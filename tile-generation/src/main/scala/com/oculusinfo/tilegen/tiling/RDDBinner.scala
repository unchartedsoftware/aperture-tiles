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



import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}



import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel



import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.BinIterator
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.DensityStripData
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.serialization.TileSerializer



trait IndexScheme[T] {
	def toCartesian (t: T): (Double, Double)
}

class CartesianIndexScheme extends IndexScheme[(Double, Double)] with Serializable {
	def toCartesian (coords: (Double, Double)): (Double, Double) = coords
}

class IPv4ZCurveIndexScheme extends IndexScheme[Array[Byte]] with Serializable {
	def toCartesian (ipAddress: Array[Byte]): (Double, Double) = {
		def getXDigit (byte: Byte): Long =
			(((byte & 0x40) >> 3) |
				 ((byte & 0x10) >> 2) |
				 ((byte & 0x04) >> 1) |
				 ((byte & 0x01))).toLong

		def getYDigit (byte: Byte): Long =
			(((byte & 0x80) >> 4) |
				 ((byte & 0x20) >> 3) |
				 ((byte & 0x08) >> 2) |
				 ((byte & 0x02) >> 1)).toLong

		ipAddress.map(byte => (getXDigit(byte), getYDigit(byte)))
			.foldLeft((0.0, 0.0))((a, b) =>
			(16.0*a._1+b._1, 16.0*a._2+b._2)
		)
	}
}

/**
 * This class is the basis of all (or, at least, nearly all) of the
 * other binning classes.  This takes an RDD of data and transforms it
 * into a pyramid of tiles.
 *
 * @param tileScheme the type of tile pyramid this binner can bin.
 */
class RDDBinner {
	var debug: Boolean = true
	

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
		val mapOverLevels: IT => TraversableOnce[(TileIndex, BinIndex)] =
			index => {
				val (x, y) = indexScheme.toCartesian(index)
				levels.map(level =>
					{
						val tile = tileScheme.rootToTile(x, y, level, bins)
						val bin = tileScheme.rootToBin(x, y, tile)
						(tile, bin)
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
	                                   indexToTiles: IT => TraversableOnce[(TileIndex, BinIndex)],
	                                   bins: Int = 256,
	                                   consolidationPartitions: Option[Int] = None,
	                                   isDensityStrip: Boolean = false):
			RDD[TileData[BT]] = {
		// We first bin data in each partition into its associated bins
		val partitionBins = data.mapPartitions(iter =>
			{
				val partitionResults: MutableMap[(TileIndex, BinIndex), PT] =
					MutableMap[(TileIndex, BinIndex), PT]()

				// Map each data point in this partition into its bins
				iter.flatMap(record => indexToTiles(record._1).map(tbi => (tbi, record._2)))
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
		consolidate(partitionBins, binDesc, consolidationPartitions, isDensityStrip)
	}



	private def consolidate[PT: ClassTag, BT] (data: RDD[((TileIndex, BinIndex), PT)],
	                                           binDesc: BinDescriptor[PT, BT],
	                                           consolidationPartitions: Option[Int],
	                                           isDensityStrip: Boolean):
			RDD[TileData[BT]] = {
		val densityStripLocal = isDensityStrip
		val reduced = data.reduceByKey(binDesc.aggregateBins(_, _),
		                               getNumSplits(consolidationPartitions, data)).map(p =>
			(p._1._1, (p._1._2, p._2))
		)
		val result = reduced
			.groupByKey(getNumSplits(consolidationPartitions, reduced))
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
}
