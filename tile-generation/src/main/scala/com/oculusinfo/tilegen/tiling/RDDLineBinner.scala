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
import com.oculusinfo.binning.TileAndBinIndices


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
class RDDLineBinner(minBins: Int = 2,
                    maxBins: Int = 1024) extends Serializable {	//1<<10 = 1024  256*4 = 4 tile widths
	var debug: Boolean = true
	
	protected def getMinBins = minBins
	protected def getMaxBins = maxBins	

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
	                                          isDensityStrip: Boolean = false,
	                                          usePointBinner: Boolean = true,
	                                          linesAsArcs: Boolean = false):
	        RDD[TileData[BT]] = {
				
		val tileBinToUniBin = {
			if (bins == 256) 
				(TileIndex.tileBinIndexToUniversalBinIndex256)_ 	// use this version if bins == 256
			else 
				(TileIndex.tileBinIndexToUniversalBinIndex)_
		}
		
		val minPts = getMinBins
		val maxPts = getMaxBins
				
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
						if (points < minPts || points > maxPts) {
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

		processData(data, binDesc, mapOverLevels, bins, consolidationPartitions, isDensityStrip, usePointBinner, linesAsArcs)
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
	                                   isDensityStrip: Boolean = false,
	                                   usePointBinner: Boolean = true,
	                                   linesAsArcs: Boolean = false):
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
		if (usePointBinner) {
			consolidateByPoints(partitionBins, binDesc, consolidationPartitions, isDensityStrip, bins, linesAsArcs)
		}
		else {
			consolidateByTiles(partitionBins, binDesc, consolidationPartitions, isDensityStrip, bins, linesAsArcs)
		}
	}



	private def consolidateByPoints[PT: ClassTag, BT] (data: RDD[((BinIndex, BinIndex, TileIndex), PT)],
	                                           binDesc: BinDescriptor[PT, BT],
	                                           consolidationPartitions: Option[Int],
	                                           isDensityStrip: Boolean,
	                                           bins: Int = 256,
	                                           linesAsArcs: Boolean = false):
			RDD[TileData[BT]] = {
		
	    val uniBinToTileBin = {
			if (bins == 256) 
				(TileIndex.universalBinIndexToTileBinIndex256)_ 	// use this version if bins == 256
			else 
				(TileIndex.universalBinIndexToTileBinIndex)_
		}
	    val calcLinePixels = {
	    	if (linesAsArcs)
	    		endpointsToArcBins
	    	else
	    		endpointsToLineBins
	    }	    
		
		val densityStripLocal = isDensityStrip
		
		// Do reduceByKey to account for duplicate lines at a given level (not really necessary, but might speed things up?)
		//val reduced1 = data.reduceByKey(binDesc.aggregateBins(_, _),
		//                               getNumSplits(consolidationPartitions, data))                              
		// Draw lines (based on endpoint bins), and convert all results from universal bins to tile,bin coords
		//val reducedData = reduced1.flatMap(p => {		//need flatMap here, else result is an RDD IndexedSeq
		val reducedData = data.flatMap(p => {
			calcLinePixels(p._1._1, p._1._2).map(bin => {
				val tb = uniBinToTileBin(p._1._3, bin)
				((tb.getTile(), tb.getBin()), p._2)
			})		
		})
		// Rest of process is same as regular RDDBinner (reduceByKey, convert to (tile,(bin,value)), groupByKey, and create tiled results)
		val reducedFinal = reducedData.reduceByKey(binDesc.aggregateBins(_, _),
		                               getNumSplits(consolidationPartitions, reducedData))				                               
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

	

	private def consolidateByTiles[PT: ClassTag, BT] (data: RDD[((BinIndex, BinIndex, TileIndex), PT)],
	                                           binDesc: BinDescriptor[PT, BT],
	                                           consolidationPartitions: Option[Int],
	                                           isDensityStrip: Boolean,
	                                           bins: Int = 256,
	                                           linesAsArcs: Boolean = false):
			RDD[TileData[BT]] = {
		
	    val uniBinToTileBin = {
			if (bins == 256) 
				(TileIndex.universalBinIndexToTileBinIndex256)_ 	// use this version if bins == 256
			else 
				(TileIndex.universalBinIndexToTileBinIndex)_
		}	    
	    val calcLinePixels = {
	    	if (linesAsArcs)
	    		endpointsToArcBins
	    	else
	    		endpointsToLineBins
	    }
		
		val densityStripLocal = isDensityStrip
		
		// Do reduceByKey to account for duplicate lines at a given level (not really necessary, but might speed things up?)
		//val reduced1 = data.reduceByKey(binDesc.aggregateBins(_, _),
		//                               getNumSplits(consolidationPartitions, data))                              
		// Draw lines (based on endpoint bins), and convert all results from universal bins to tile,bin coords
		//val reduced2 = reduced1.flatMap(p => {		//need flatMap here, else result is an RDD IndexedSeq
		
		val reducedData = data.flatMap(p => {		
			universalBinsToTiles(p._1._3, calcLinePixels(p._1._1, p._1._2), uniBinToTileBin).map(tile =>
								(tile, (p._1._1, p._1._2, p._2))
							)
		})
		
		
		// Consolidate segments for each tile index, and draw a tile data based on
		// the consolidated results
		reducedData.groupByKey(getNumSplits(consolidationPartitions, reducedData)).map(t =>
			{				
				val index = t._1
				val segments = t._2
				val xLimit = index.getXBins()
				val yLimit = index.getYBins()
				val tile = if (densityStripLocal) new DensityStripData[BT](index)
				else new TileData[BT](index)
				
				val binValues = Array.ofDim[PT](xLimit, yLimit)	//init 2D array of of type PT
				for (x <- 0 until xLimit) {		// fill 2D array with default bin values
					for (y <- 0 until yLimit) {
						binValues(x)(y) = binDesc.defaultProcessedBinValue
					}
				}
									
				segments.foreach(segment =>
					{
						// get all universal bins in line, discard ones not in current tile, 
						// and convert bins to 'regular' tile/bin units
						universalBinsToBins(index, calcLinePixels(segment._1, segment._2), uniBinToTileBin).foreach(bin =>
							{
								val x = bin.getX()
								val y = bin.getY()
								binValues(x)(y) =  binDesc.aggregateBins(binValues(x)(y), segment._3)
							}
						)
					}
				)
				
				// convert aggregated bin values from type PT to BT, and save tile results
				for (x <- 0 until xLimit) {
					for (y <- 0 until yLimit) {
						tile.setBin(x, y, binDesc.convert(binValues(x)(y)))
					}
				}
				tile
			}
		)
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
			var (x0, y0, x1, y1) = (start.getX(), start.getY(), end.getX(), end.getY())
			var steep = math.abs(y1 - y0) > math.abs(x1 - x0)
			
			var tmpInt = 0
			if (steep) {
				tmpInt = y0		//swap x0, y0
				y0 = x0
				x0 = tmpInt
				tmpInt = y1		//swap x1, y1
				y1 = x1
				x1 = tmpInt
			}
			if (x0 > x1) {
				tmpInt = x1		//swap x0, x1
				x1 = x0
				x0 = tmpInt
				tmpInt = y0		//swap y0, y1
				y0 = y1
				y1 = tmpInt
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

	/**
	 * Determine all bins that are required to draw an arc between two endpoint bins.
	 *
	 * Bresenham's Midpoint circle algorithm is used for filling in the intermediate pixels in an arc.
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
	protected val endpointsToArcBins: (BinIndex, BinIndex) => IndexedSeq[BinIndex] =
		(start, end) => {

			var (x0, y0, x1, y1) = (start.getX(), start.getY(), end.getX(), end.getY())

			//---- Find centre of circle to be used to draw the arc
			val dx = x1-x0
			val dy = y1-y0		
			val len = Math.sqrt(dx*dx + dy*dy)	//length between endpoints
			val r = len.toInt	// set radius of circle = len for now  (TODO -- could make this a tunable parameter?)
			
			val theta1 = 0.5*Math.PI - Math.asin(len/(2.0*r)); // angle from each endpoint to circle's centre (centre and endpoints form an isosceles triangle)
			val angleTemp = Math.atan2(dy, dx)-theta1;
			val xC = x0 + (r*Math.cos(angleTemp)).toInt		   // co-ords for circle's centre
			val yC = y0 + (r*Math.sin(angleTemp)).toInt
			//val xC_2 = x0 + (r*Math.cos(Math.atan2(dy, dx)+theta1)).toInt	//Note: 2nd possiblility for circle's centre 
			//val yC_2 = y0 + (r*Math.cos(Math.atan2(dy, dx)+theta1)).toInt	//(corresponds to CCW arc, so not needed in this case)
					
			//---- Use Midpoint Circle algorithm to draw the arc
			var f = 1-r
			var ddF_x = 1
			var ddF_y = -2*r
			var x = 0
			var y = r
			
			// angles for start and end points of arc
			var startRad = Math.atan2(y0-yC, x0-xC)
			var stopRad = Math.atan2(y1-yC, x1-xC)
			
			val bWrapNeg = (stopRad-startRad > Math.PI)
			if (bWrapNeg) startRad += 2.0*Math.PI	//note: this assumes using CW arcs only!
													//(otherwise would need to check if stopRad is negative here as well)
		
			val arcBins = scala.collection.mutable.ArrayBuffer[BinIndex]()
			
			// calc points for the four vertices of circle		
			saveArcPoint((xC, yC+r))			
			saveArcPoint((xC, yC-r))			
			saveArcPoint((xC+r, yC))
			saveArcPoint((xC-r, yC))
			
			while (x < y-1) {
				if (f >= 0) {
					y = y - 1
					ddF_y = ddF_y + 2
					f = f + ddF_y
				}
				x = x + 1
				ddF_x = ddF_x + 2
				f = f + ddF_x

				// TODO -- optimize this section (only need to do atan2 call once per loop iteration and then scale 
				// angles below by multiples of pi/2 rads as needed)
				saveArcPoint((xC+x, yC+y))			
				saveArcPoint((xC-x, yC+y))			
				saveArcPoint((xC+x, yC-y))
				saveArcPoint((xC-x, yC-y))
				
				if (x!=y) {
					saveArcPoint((xC+y, yC+x))
					saveArcPoint((xC-y, yC+x))
					saveArcPoint((xC+y, yC-x))
					saveArcPoint((xC-y, yC-x))
				}
			}
			
			def saveArcPoint(point: (Int, Int)) = {
				
				var newAngle = Math.atan2(point._2-yC, point._1-xC)
				if (bWrapNeg && newAngle < 0.0)
					newAngle += 2.0*Math.PI
				
				if (newAngle <= startRad && newAngle >= stopRad)
					arcBins += new BinIndex(point._1, point._2)
			}
			
			arcBins.toIndexedSeq	
		}
					
	/**
	 * Determine all tiles required to draw a line between two endpoint bins.
	 *
	 * @param baseTile
	 *        A sample tile specifying level and number of bins of all required
	 *        results.
	 * @return All tiles falling on the direct line (in universal bin
	 *         coordinates) between the two endpoint bins
	 */
//	protected val universalBinsToTiles:
//			(TileIndex, IndexedSeq[BinIndex]) => Traversable[TileIndex] =
//		(baseTile, bins) => {
//			bins.map(ubin =>
//				{
//					val tb = TileIndex.universalBinIndexToTileBinIndex(baseTile, ubin)
//					tb.getTile()
//				}
//			).toSet
//			// transform to set to remove duplicates
//		}
		
	protected def universalBinsToTiles(baseTile: TileIndex, bins: IndexedSeq[BinIndex], 
									   uniBinToTB: (TileIndex, BinIndex) => TileAndBinIndices): Traversable[TileIndex] = {
		bins.map(ubin =>
			{
				val tb = uniBinToTB(baseTile, ubin)
				tb.getTile()
			}
		).toSet	// transform to set to remove duplicates			
	}	
	/**
	 * Determine all bins within a given tile that are required to draw a line
	 * between two endpoint bins.
	 *
	 * @param tile
	 *        The tile of interest
	 * @return All bins, in tile bin coordinates, in the given tile, falling on
	 *         the direct line (in universal bin coordinates) between the two
	 *         endoint bins.
	 */
//	protected val universalBinsToBins:
//			(TileIndex, IndexedSeq[BinIndex]) => IndexedSeq[BinIndex] =
//		(tile, bins) => {
//			bins.map(ubin =>
//				TileIndex.universalBinIndexToTileBinIndex(tile, ubin)
//			).filter(_.getTile().equals(tile)).map(_.getBin())
//		}
		
	protected def universalBinsToBins(tile: TileIndex, bins: IndexedSeq[BinIndex],
									   uniBinToTB: (TileIndex, BinIndex) => TileAndBinIndices): IndexedSeq[BinIndex] = {
		bins.map(ubin =>
			uniBinToTB(tile, ubin)
		).filter(_.getTile().equals(tile)).map(_.getBin())
	}			
		
}
