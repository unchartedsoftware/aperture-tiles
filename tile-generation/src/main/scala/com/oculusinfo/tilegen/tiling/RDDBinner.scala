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

import scala.collection.JavaConverters._


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
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.serialization.TileSerializer

import com.oculusinfo.tilegen.datasets.ValueDescription
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic


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
	 * Transform an arbitrary dataset into one that can be used for binning.
	 * Also sets up data-based analytics to be attached to tiles
	 *
	 * Note that this method only sets up transformations; nothing it does 
	 * actually causes any work to be done directly, so the accumulators passed 
	 * in will not be populated when this method is complete.  They will be 
	 * populated once the data actually has been used.
	 * 
	 * @param RT The raw data type
	 * @param IT The index type, used as input into the pyramid transformation 
	 *           that determins in which tile and bin any given piece of data 
	 *           falls
	 * @param PT The processing bin type, to be used as the values in resulting 
	 *           tiles
	 * @param DT The data analytic type, used to attach analytic values to tiles
	 *           calculated from raw data
	 * @param data The raw data to be tiled
	 * @param indexFcn A function to transform a raw data record into an index 
	 *                 (see parameter IT)
	 * @param valueFcn A function to transform a raw data record into a value 
	 *                 to be binned (see parameter RT)
	 * @param dataAnalytic An optional transformation from a raw data record 
	 *                     into an aggregable analytic value.  If None, this 
	 *                     should cause no extra processing.  If multiple 
	 *                     analytics are desired, use ComposedTileAnalytic
	 */
	def transformData[RT: ClassTag, IT: ClassTag, PT: ClassTag, DT: ClassTag]
		(data: RDD[RT],
		 indexFcn: RT => Try[IT],
		 valueFcn: RT => Try[PT],
		 dataAnalytics: Option[AnalysisDescription[RT, DT]] = None):
			RDD[(IT, PT, Option[DT])] =
	{
		// Process the data to remove all but the minimal portion we need for
		// tiling - index, value, and analytics
		data.mapPartitions(iter =>
			iter.map(i => (indexFcn(i), valueFcn(i), dataAnalytics.map(_.convert(i))))
		).filter(record => record._1.isSuccess && record._2.isSuccess)
			.map(record =>(record._1.get, record._2.get, record._3))
	}


	/**
	 * Fully process a dataset of input records into output tiles written out
	 * somewhere
	 * 
	 * @param RT The raw input record type
	 * @param IT The coordinate type
	 * @param PT The processing bin type
	 * @param AT The tile analytic type
	 * @param DT The data anlytic type
	 * @param BT The output bin type
	 */
	def binAndWriteData[RT: ClassTag, IT: ClassTag, PT: ClassTag,
	                    AT: ClassTag, DT: ClassTag, BT] (
		data: RDD[RT],
		indexFcn: RT => Try[IT],
		valueFcn: RT => Try[PT],
		indexScheme: IndexScheme[IT],
		binAnalytic: BinningAnalytic[PT, BT],
		tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		dataAnalytics: Option[AnalysisDescription[RT, DT]],
		serializer: TileSerializer[BT],
		tileScheme: TilePyramid,
		consolidationPartitions: Option[Int],
		writeLocation: String,
		tileIO: TileIO,
		levelSets: Seq[Seq[Int]],
		xBins: Int = 256,
		yBins: Int = 256,
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
			println("\tX Bins: "+xBins)
			println("\tY Bins: "+yBins)
			println("\tName: "+name)
			println("\tDescription: "+description)
		}

		val startTime = System.currentTimeMillis()

		val bareData = transformData(data, indexFcn, valueFcn, dataAnalytics)

		// Cache this, we'll use it at least once for each level set
		bareData.persist(StorageLevel.MEMORY_AND_DISK)

		levelSets.foreach(levels =>
			{
				val levelStartTime = System.currentTimeMillis()
				// For each level set, process the bare data into tiles...
				var tiles = processDataByLevel(bareData,
				                               indexScheme,
				                               binAnalytic,
				                               tileAnalytics,
				                               dataAnalytics,
				                               tileScheme,
				                               levels,
				                               xBins,
				                               yBins,
				                               consolidationPartitions)
				// ... and write them out.
				tileIO.writeTileSet(tileScheme, writeLocation, tiles,
				                    serializer, tileAnalytics, dataAnalytics,
				                    name, description)
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
	 * @param binAnalytic A description of how raw values are aggregated into 
	 *                    bin values
	 * @param tileAnalytics A description of analytics that can be run on each 
	 *                      tile, and how to aggregate them
	 * @param dataAnalytics A description of analytics that can be run on the
	 *                      raw data, and recorded (in the aggregate) on each
	 *                      tile
	 * @param tileScheme A description of how raw values are transformed to bin
	 *                   coordinates
	 * @param levels A list of levels on which to create tiles
	 * @param xBins The number of bins along the horizontal axis of each tile
	 * @param yBins The number of bins along the vertical axis of each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 * 
	 * @param IT the index type, convertable to a cartesian pair with the 
	 *           coordinateFromIndex function
	 * @param PT The bin type, when processing and aggregating
	 * @param AT The type of tile-level analytic to calculate for each tile.
	 * @param DT The type of raw data-level analytic that already has been 
	 *           calculated for each tile.
	 * @param BT The final bin type, ready for writing to tiles
	 */
	def processDataByLevel[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 indexScheme: IndexScheme[IT],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 tileScheme: TilePyramid,
		 levels: Seq[Int],
		 xBins: Int = 256,
		 yBins: Int = 256,
		 consolidationPartitions: Option[Int] = None): RDD[TileData[BT]] =
	{
		val mapOverLevels: IT => TraversableOnce[(TileIndex, BinIndex)] =
			index => {
				val (x, y) = indexScheme.toCartesian(index)
				levels.map(level =>
					{
						val tile = tileScheme.rootToTile(x, y, level, xBins, yBins)
						val bin = tileScheme.rootToBin(x, y, tile)
						(tile, bin)
					}
				)
			}

		processData(data, binAnalytic, tileAnalytics, dataAnalytics,
		            mapOverLevels, consolidationPartitions)
	}



	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles.
	 * 
	 * @param data The data to be processed
	 * @param binAnalytic A description of how raw values are to be aggregated into
	 *                bin values
	 * @param indexToTiles A function that spreads a data point out over the
	 *                     tiles and bins of interest
	 * @param xBins The number of bins along the horizontal axis of each tile
	 * @param yBins The number of bins along the vertical axis of each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 * 
	 * @param IT The index type, convertable to tile and bin
	 * @param PT The bin type, when processing and aggregating
	 * @param BT The final bin type, ready for writing to tiles
	 */
	def processData[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 indexToTiles: IT => TraversableOnce[(TileIndex, BinIndex)],
		 consolidationPartitions: Option[Int] = None): RDD[TileData[BT]] =
	{
		// Determine metadata
		val metaData = processMetaData(data, indexToTiles, dataAnalytics)

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
							partitionResults(key) = binAnalytic.aggregate(partitionResults(key), value)
						} else {
							partitionResults(key) = value
						}
					}
				)

				partitionResults.iterator
			}
		)

		// Now, combine by-partition bins into global bins, and turn them into tiles.
		consolidate(partitionBins, binAnalytic, tileAnalytics,
		            metaData, consolidationPartitions)
	}

	/**
	 * Process a simplified input dataset to run any raw data-based analysis
	 * 
	 * @param IT The index type of the data set
	 * @param DT The type of data analytic used
	 * @param data The data to process
	 * @param indexToTiles A function that spreads a data point out over the
	 *                     tiles and bins of interest
	 * @param dataAnalytic An optional transformation from a raw data record 
	 *                     into an aggregable analytic value.  If None, this 
	 *                     should cause no extra processing.  If multiple 
	 *                     analytics are desired, use ComposedTileAnalytic
	 */
	def processMetaData[IT: ClassTag, PT: ClassTag, DT: ClassTag]
		(data: RDD[(IT, PT, Option[DT])],
		 indexToTiles: IT => TraversableOnce[(TileIndex, BinIndex)],
		 dataAnalytics: Option[AnalysisDescription[_, DT]]):
			Option[RDD[(TileIndex, Map[String, Any])]] =
	{
		dataAnalytics.map(da =>
			data.mapPartitions(iter =>
				{
					val partitionResults = MutableMap[TileIndex, DT]()
					iter.foreach(record =>
						indexToTiles(record._1).map(tbi => (tbi._1, record._3))
							.foreach{
							case (tile, value) =>
								value.foreach(v =>
									{
										partitionResults(tile) =
											if (partitionResults.contains(tile)) {
												da.analytic.aggregate(partitionResults(tile), v)
											} else {
												v
											}
										da.accumulate(tile, v)
									}
								)
						}
					)
					partitionResults.iterator
				}
			).reduceByKey(da.analytic.aggregate(_, _))
				.map{case (tile, value) =>
					(tile, da.analytic.toMap(value))
			}
		)
	}

	private def consolidate[PT: ClassTag, AT: ClassTag, BT]
		(data: RDD[((TileIndex, BinIndex), PT)],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 tileMetaData: Option[RDD[(TileIndex, Map[String, Any])]],
		 consolidationPartitions: Option[Int]): RDD[TileData[BT]] =
	{
		// We need to consolidate both metadata and binning data, so our result
		// has two slots, and each half populates one of them.
		//
		// We need to do this right away because the tiles should be immutable
		// once created
		//
		// For the same reason, we'll have to run the tile analytic when we
		// create the tile, too.
		//
		// First the binning data half
		val reduced: RDD[(TileIndex, (Option[(BinIndex, PT)],
		                              Option[Map[String, Any]]))] = {
			val env = SparkEnv.get
			val conf = SparkEnv.get.conf
			
			data.reduceByKey(binAnalytic.aggregate(_, _),
			                 getNumSplits(consolidationPartitions, data))
				.map(p => (p._1._1, (Some((p._1._2, p._2)), None)))
		}
		// Now the metadata half (in a way that should take no work if there is no metadata)
		val metaData: Option[RDD[(TileIndex, (Option[(BinIndex, PT)],
		                                      Option[Map[String, Any]]))]] =
			tileMetaData.map(
				_.map{case (index, metaData) => (index, (None, Some(metaData))) }
			)

		// Get the combination of the two sets, again in a way that does
		// no extra work if there is no metadata
		val toTile =
			(if (metaData.isDefined)
				 // Just take the simple union
			    (reduced union metaData.get)
			 else reduced
			).groupByKey(getNumSplits(consolidationPartitions, reduced))


		toTile.map(t =>
			{
				val index = t._1
				val tileData = t._2
				val xLimit = index.getXBins()
				val yLimit = index.getYBins()

				// Create our tile
				val tile = new TileData[BT](index)

				// Put the proper default in all bins
				val defaultBinValue =
					binAnalytic.finish(binAnalytic.defaultProcessedValue)
				for (x <- 0 until xLimit) {
					for (y <- 0 until yLimit) {
						tile.setBin(x, y, defaultBinValue)
					}
				}

				// Put the proper value into each bin
				tileData.filter(_._1.isDefined).foreach(p =>
					{
						val bin = p._1.get._1
						val value = p._1.get._2
						tile.setBin(bin.getX(), bin.getY(), binAnalytic.finish(value))
					}
				)

				// Add in any pre-calculated metadata
				tileData.filter(_._2.isDefined).foreach(p =>
					{
						val metaData = p._2.get
						metaData.map{
							case (key, value) =>
								tile.setMetaData(key, value)
						}
					}
				)

				// Calculate and add in an tile-level metadata we've been told to calculate
				tileAnalytics.map(ta =>
					{
						// Figure out the value for this tile
						val analyticValue = ta.convert(tile)
						// Add it into any appropriate accumulators
						ta.accumulate(tile.getDefinition(), analyticValue)
						// And store it in the tile's metadata
						ta.analytic.toMap(analyticValue).map{case (key, value) =>
							tile.setMetaData(key, value)
						}
					}
				)

				tile
			}
		)
	}

	/**
	 * Get the number of partitions to use when operating on a data set.
	 * 
	 * @param requestedPartitions An optional override of the default number of 
	 *                            partitions
	 * @param dataSet the dataSet for which to determine the number of 
	 *                partitions.
	 * @return The number of partitions that should be used for this dataset.
	 */
	def getNumSplits[T: ClassTag] (requestedPartitions: Option[Int], dataSet: RDD[T]): Int =
		requestedPartitions.getOrElse(dataSet.partitions.size)
}
