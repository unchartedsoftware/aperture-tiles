/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oculusinfo.tilegen.tiling



import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

import scala.collection.TraversableOnce
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.BinIterator
import com.oculusinfo.binning.DensityStripData
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.serialization.TileSerializer


class LineSegmentIndexScheme extends IndexScheme[(Double, Double, Double, Double)] with Serializable { 
	def toCartesianEndpoints (coords: (Double, Double, Double, Double)): (Double, Double, Double, Double) = coords
	def toCartesian (coords: (Double, Double, Double, Double)): (Double, Double) = (coords._1, coords._2)
}


/**
 * This takes an RDD of line segment data (ie pairs of endpoints) and transforms it
 * into a pyramid of tiles.  minBins and maxBins define the min and max valid rane for
 * line segments that are included in the binning process
 * 
 *
 * @param tileScheme the type of tile pyramid this binner can bin.
 */
class RDDLineBinner(minBins: Int = 4,	//was 1<<1
                    maxBins: Int = 1024) extends Serializable {	//1<<10 = 1024  256*4 = 4 tile widths
	var debug: Boolean = true
	
	private val _minBins = minBins
	private val _maxBins = maxBins	

	/**
	 * Fully process a dataset of input records into output tiles written out
	 * somewhere
	 * 
	 * @param RT The raw input record type
	 * @param IT The coordinate type
	 * @param PT The processing bin type
	 * @param BT The output bin type
	 */
	def binAndWriteData[RT: ClassTag, IT: ClassTag, PT: ClassTag, BT] (
		data: RDD[RT],
		indexFcn: RT => Try[IT],
		valueFcn: RT => Try[PT],
		indexScheme: IndexScheme[IT],
		binDesc: BinDescriptor[PT, BT],
		tileScheme: TilePyramid,
		consolidationPartitions: Option[Int],
		writeLocation: String,
		tileIO: TileIO,
		levelSets: Seq[Seq[Int]],
		bins: Int = 256,
		name: String = "unknown",
		description: String = "unknown") =
	{
		if (debug) {
			println("Binning data")
			println("\tConsolidation partitions: "+consolidationPartitions)
			println("\tWrite location: "+writeLocation)
			println("\tTile io type: "+tileIO.getClass.getName)
			println("\tlevel sets: "+levelSets.map(_.mkString("[", ", ", "]"))
				        .mkString("[", ", ", "]"))
			println("\tBins: "+bins)
			println("\tName: "+name)
			println("\tDescription: "+description)
		}

		val startTime = System.currentTimeMillis()

		// Process the data to remove all but the minimal portion we need for
		// tiling - x coordinate, y coordinate, and bin value
		val bareData = data.mapPartitions(iter =>
			{
				println("Initial partition processing")
				iter.map(i => (indexFcn(i), valueFcn(i)))
			}
		).filter(record => record._1.isSuccess && record._2.isSuccess)
			.map(record =>(record._1.get, record._2.get))

		// Cache this, we'll use it at least once for each level set
		bareData.persist(StorageLevel.MEMORY_AND_DISK)

		levelSets.foreach(levels =>
			{
				val levelStartTime = System.currentTimeMillis()
				// For each level set, process the bare data into tiles...
				var tiles = processDataByLevel(bareData,
				                               indexScheme,
				                               binDesc,
				                               tileScheme,
				                               levels,
				                               bins,
				                               consolidationPartitions)
				// ... and write them out.
				tileIO.writeTileSet(tileScheme, writeLocation, tiles,
				                    binDesc, name, description)
				if (debug) {
					val levelEndTime = System.currentTimeMillis()
					println("Finished binning levels ["+levels.mkString(", ")+"] of data set "
						        + name + " in " + ((levelEndTime-levelStartTime)/60000.0) + " minutes")
				}
			}
		)

		bareData.unpersist(false)

		if (debug) {
			val endTime = System.currentTimeMillis()
			println("Finished binning data set " + name + " into "
				        + levelSets.map(_.size).reduce(_+_)
				        + " levels (" + levelSets.map(_.mkString(",")).mkString(";") + ") in "
				        + ((endTime-startTime)/60000.0) + " minutes")
		}
	}



	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles on the given levels.
	 *
	 * @param data The data to be processed
	 * @param indexScheme A conversion scheme for converting from the index type 
	 *        to one we can use.
	 * @param binDesc A description of how raw values are to be aggregated into
	 *                bin values
	 * @param tileScheme A description of how raw values are transformed to bin
	 *                   coordinates
	 * @param levels A list of levels on which to create tiles
	 * @param bins The number of bins per coordinate on each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 * 
	 * @param IT the index type, convertable to a cartesian pair with the 
     *           coordinateFromIndex function
	 * @param PT The bin type, when processing and aggregating
     * @param BT The final bin type, ready for writing to tiles
	 */
	def processDataByLevel[IT: ClassTag,
	                       PT: ClassTag, BT] (data: RDD[(IT, PT)],
	                                          indexScheme: IndexScheme[IT],
	                                          binDesc: BinDescriptor[PT, BT],
	                                          tileScheme: TilePyramid,
	                                          levels: Seq[Int],
	                                          bins: Int = 256,
	                                          consolidationPartitions: Option[Int] = None,
	                                          isDensityStrip: Boolean = false):
	        RDD[TileData[BT]] = {
				
		val tileBinToUniBin = {
			if (bins == 256) 
				(TileIndex.tileBinIndexToUniversalBinIndex256)_ 	// use this version if bins == 256
			else 
				(TileIndex.tileBinIndexToUniversalBinIndex)_
		}
				
		val mapOverLevels: IT => TraversableOnce[(BinIndex, BinIndex, TileIndex)] =	//BinIndex == universal bin indices in this case
			index => {
				val (x1, y1, x2, y2) = indexScheme.toCartesianEndpoints(index)	
				levels.map(level =>
					{
						// find 'universal' bins for both endpoints 
						val tile1 = tileScheme.rootToTile(x1, y1, level, bins)
						val tileBin1 = tileScheme.rootToBin(x1, y1, tile1)
						val unvBin1 = tileBinToUniBin(tile1, tileBin1)
						
						val tile2 = tileScheme.rootToTile(x2, y2, level, bins)
						val tileBin2 = tileScheme.rootToBin(x2, y2, tile2)
						val unvBin2 = tileBinToUniBin(tile2, tileBin2)
						
						// check if line length is within valid range 
						val points = math.abs(unvBin1.getX()-unvBin2.getX()) max math.abs(unvBin1.getY()-unvBin2.getY())
						if (points < _minBins || points > _maxBins) {
							(null, null, null)	// line segment either too short or too long (so disregard)
						} else {
							// return endpoints as pair of universal bins per level
							if (unvBin1.getX() < unvBin2.getX())
								(unvBin1, unvBin2, tile1)	// use convention of endpoint with min X value is listed first
							else
								(unvBin2, unvBin1, tile2)	//note, we need one of the tile indices here to keep level info for this line segment	
						}
					}
				)
			}

		processData(data, binDesc, mapOverLevels, bins, consolidationPartitions, isDensityStrip)
	}



	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles.
	 * 
	 * @param data The data to be processed
	 * @param binDesc A description of how raw values are to be aggregated into
	 *                bin values
	 * @param datumToTiles A function that spreads a data point out over the
	 *                     tiles and bins of interest
	 * @param levels A list of levels on which to create tiles
	 * @param bins The number of bins per coordinate on each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 * 
	 * @param IT The index type, convertable to tile and bin
	 * @param PT The bin type, when processing and aggregating
     * @param BT The final bin type, ready for writing to tiles
	 */
	def processData[IT: ClassTag,
	                PT: ClassTag, BT] (data: RDD[(IT, PT)],
	                                   binDesc: BinDescriptor[PT, BT],
	                                   indexToUniversalBins: IT => TraversableOnce[(BinIndex, BinIndex, TileIndex)],
	                                   bins: Int = 256,
	                                   consolidationPartitions: Option[Int] = None,
	                                   isDensityStrip: Boolean = false):
			RDD[TileData[BT]] = {
		// We first bin data in each partition into its associated bins
		val partitionBins = data.mapPartitions(iter =>
			{
				val partitionResults: MutableMap[(BinIndex, BinIndex, TileIndex), PT] =
					MutableMap[(BinIndex, BinIndex, TileIndex), PT]()

				// Map each data point in this partition into its bins
				iter.flatMap(record => indexToUniversalBins(record._1).filter(_._1 != null)
					.map(tbi => (tbi, record._2)))
					// And combine bins within this partition
					.foreach(tbv =>
					{
						val key = tbv._1
						val value = tbv._2
						if (partitionResults.contains(key)) {
							partitionResults(key) = binDesc.aggregateBins(partitionResults(key), value)
						} else {
							partitionResults(key) = value
						}
					}
				)

				partitionResults.iterator
			}
		)

		// Now, combine by-partition bins into global bins, and turn them into tiles.
		consolidate(partitionBins, binDesc, consolidationPartitions, isDensityStrip, bins)
	}



	private def consolidate[PT: ClassTag, BT] (data: RDD[((BinIndex, BinIndex, TileIndex), PT)],
	                                           binDesc: BinDescriptor[PT, BT],
	                                           consolidationPartitions: Option[Int],
	                                           isDensityStrip: Boolean,
	                                           bins: Int = 256):
			RDD[TileData[BT]] = {
		
	    val uniBinToTileBin = {
			if (bins == 256) 
				(TileIndex.universalBinIndexToTileBinIndex256)_ 	// use this version if bins == 256
			else 
				(TileIndex.universalBinIndexToTileBinIndex)_
		}
		
		val densityStripLocal = isDensityStrip
		
		// Do reduceByKey to account for duplicate lines at a given level (not really necessary, but might speed things up?)
		//val reduced1 = data.reduceByKey(binDesc.aggregateBins(_, _),
		//                               getNumSplits(consolidationPartitions, data))                              
		// Draw lines (based on endpoint bins), and convert all results from universal bins to tile,bin coords
		//val reduced2 = reduced1.flatMap(p => {		//need flatMap here, else result is an RDD IndexedSeq
		val reduced2 = data.flatMap(p => {
			endpointsToLineBins(p._1._1, p._1._2).map(bin => {
				val tb = uniBinToTileBin(p._1._3, bin)
				((tb.getTile(), tb.getBin()), p._2)
			})		
		})
		// Rest of process is same as regular RDDBinner (reduceByKey, convert to (tile,(bin,value)), groupByKey, and create tiled results)
		val reducedFinal = reduced2.reduceByKey(binDesc.aggregateBins(_, _),
		                               getNumSplits(consolidationPartitions, reduced2))				                               
		                           .map(p => (p._1._1, (p._1._2, p._2)))
			
		val result = reducedFinal
			.groupByKey(getNumSplits(consolidationPartitions, reducedFinal))
			.map(t =>
			{
				val index = t._1
				val bins = t._2
				val xLimit = index.getXBins()
				val yLimit = index.getYBins()
				val tile = if (densityStripLocal) new DensityStripData[BT](index)
				else new TileData[BT](index)
				val defaultBinValue = binDesc.convert(binDesc.defaultProcessedBinValue)

				for (x <- 0 until xLimit) {
					for (y <- 0 until yLimit) {
						tile.setBin(x, y, defaultBinValue)
					}
				}

				bins.foreach(p =>
					{
						val bin = p._1
						val value = p._2
						tile.setBin(bin.getX(), bin.getY(), binDesc.convert(value))
					}
				)

				tile
			}
		)

		result
	}


	def getNumSplits[T: ClassTag] (requestedPartitions: Option[Int], dataSet: RDD[T]): Int = {
		val curSize = dataSet.partitions.size
		val result = curSize max requestedPartitions.getOrElse(0)
		result
	}
	
	
	/**
	 * Determine all bins that are required to draw a line two endpoint bins.
	 *
	 * Bresenham's algorithm for filling in the intermediate pixels in a line.
	 *
	 * From wikipedia
	 * 
	 * @param start
	 *        The start bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @param end
	 *        The end bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @return All bins, in universal bin coordinates, falling on the direct
	 *         line between the two endoint bins.
	 */
	protected val endpointsToLineBins: (BinIndex, BinIndex) => IndexedSeq[BinIndex] =
		(start, end) => {

			// Bresenham's algorithm
			var steep = math.abs(end.getY() - start.getY()) > math.abs(end.getX() - start.getX())

			var (x0, y0, x1, y1) =
				if (steep) {
					if (start.getY() > end.getY()) {
						(end.getY(), end.getX(), start.getY(), start.getX())
					} else {
						(start.getY(), start.getX(), end.getY(), end.getX())
					}
				} else {
					if (start.getX() > end.getX()) {
						(end.getX(), end.getY(), start.getX(), start.getY())
					} else {
						(start.getX(), start.getY(), end.getX(), end.getY())
					}
				}

			val deltax = x1-x0
			val deltay = math.abs(y1-y0)
			var error = deltax>>1
			var y = y0
			val ystep = if (y0 < y1) 1 else -1

			Range(x0, x1+1).map(x =>		//x1+1 needed here so that "end" bin is included in Sequence
				{
					val ourY = y
					error = error - deltay
					if (error < 0) {
						y = y + ystep
						error = error + deltax
					}

					if (steep) new BinIndex(ourY, x)
					else new BinIndex(x, ourY)
				}
			)
		}
	
}
