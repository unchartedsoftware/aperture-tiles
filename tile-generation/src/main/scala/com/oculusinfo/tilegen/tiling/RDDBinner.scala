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



import com.oculusinfo.binning.TileData.StorageType


import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.Try



import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel



import com.oculusinfo.binning._
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.impl.SparseTileData
import com.oculusinfo.binning.io.serialization.TileSerializer

import com.oculusinfo.tilegen.tiling.analytics.{TileAnalytic, AnalysisDescription, BinningAnalytic}


/**
 * This class is the basis of all (or, at least, nearly all) of the
 * other binning classes.  This takes an RDD of data and transforms it
 * into a pyramid of tiles.
 */
@deprecated("Use UniversalBinner", "0.7") class RDDBinner {
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
	 * @tparam RT The raw data type
	 * @tparam IT The index type, used as input into the pyramid transformation
	 *            that determins in which tile and bin any given piece of data
	 *            falls
	 * @tparam PT The processing bin type, to be used as the values in resulting
	 *            tiles
	 * @tparam DT The data analytic type, used to attach analytic values to tiles
	 *            calculated from raw data
	 * @param data The raw data to be tiled
	 * @param indexFcn A function to transform a raw data record into an index
	 *                 (see parameter IT)
	 * @param valueFcn A function to transform a raw data record into a value
	 *                 to be binned (see parameter RT)
	 * @param dataAnalytics An optional transformation from a raw data record
	 *                      into an aggregable analytic value.  If None, this
	 *                      should cause no extra processing.  If multiple
	 *                      analytics are desired, use ComposedTileAnalytic
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
	 * @tparam RT The raw input record type
	 * @tparam IT The coordinate type
	 * @tparam PT The processing bin type
	 * @tparam AT The tile analytic type
	 * @tparam DT The data anlytic type
	 * @tparam BT The output bin type
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
		tileType: Option[StorageType],
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
				                               consolidationPartitions,
				                               tileType)
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
	 * @param consolidationPartitions The number of partitions to use when grouping values in the same bin or the same
	 *                                tile.  None to use the default determined by Spark.
	 * @param tileType A specification of how data should be stored.  If None, a heuristic will be used that will use
	 *                 the optimal type for a double-valued tile, and isn't too bad for smaller-valued types.  For
	 *                 significantly larger-valued types, Some(Sparse) would probably work best.
	 *
	 * @tparam IT the index type, convertable to a cartesian pair with the
	 *            coordinateFromIndex function
	 * @tparam PT The bin type, when processing and aggregating
	 * @tparam AT The type of tile-level analytic to calculate for each tile.
	 * @tparam DT The type of raw data-level analytic that already has been
	 *            calculated for each tile.
	 * @tparam BT The final bin type, ready for writing to tiles
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
		 consolidationPartitions: Option[Int] = None,
		 tileType: Option[StorageType] = None): RDD[TileData[BT]] =
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
		            mapOverLevels, consolidationPartitions, tileType)
	}



	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles.
	 *
	 * @param data The data to be processed
	 * @param binAnalytic A description of how raw values are to be aggregated into bin values
	 * @param tileAnalytics An optional description of extra analytics to be run on complete tiles
	 * @param dataAnalytics An optional description of extra analytics to be run on the raw data
	 * @param indexToTiles A function that spreads a data point out over the tiles and bins of interest
	 * @param consolidationPartitions The number of partitions to use when grouping values in the same bin or the same
	 *                                tile.  None to use the default determined by Spark.
	 * @param tileType A specification of how data should be stored.  If None, a heuristic will be used that will use
	 *                 the optimal type for a double-valued tile, and isn't too bad for smaller-valued types.  For
	 *                 significantly larger-valued types, Some(Sparse) would probably work best.
	 *
	 * @tparam IT The index type, convertable to tile and bin
	 * @tparam PT The bin type, when processing and aggregating
	 * @tparam BT The final bin type, ready for writing to tiles
	 */
	def processData[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 indexToTiles: IT => TraversableOnce[(TileIndex, BinIndex)],
		 consolidationPartitions: Option[Int] = None,
		 tileType: Option[StorageType] = None): RDD[TileData[BT]] =
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
		consolidate(partitionBins, binAnalytic, tileAnalytics, dataAnalytics,
		            metaData, consolidationPartitions, tileType)
	}

	/**
	 * Process a simplified input dataset to run any raw data-based analysis
	 *
	 * @tparam IT The index type of the data set
	 * @tparam DT The type of data analytic used
	 * @param data The data to process
	 * @param indexToTiles A function that spreads a data point out over the
	 *                     tiles and bins of interest
	 * @param dataAnalytics An optional transformation from a raw data record
	 *                      into an aggregable analytic value.  If None, this
	 *                      should cause no extra processing.  If multiple
	 *                      analytics are desired, use ComposedTileAnalytic
	 */
	def processMetaData[IT: ClassTag, PT: ClassTag, DT: ClassTag]
		(data: RDD[(IT, PT, Option[DT])],
		 indexToTiles: IT => TraversableOnce[(TileIndex, BinIndex)],
		 dataAnalytics: Option[AnalysisDescription[_, DT]]):
			Option[RDD[(TileIndex, DT)]] =
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
		)
	}

	private def consolidate[PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[((TileIndex, BinIndex), PT)],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 tileMetaData: Option[RDD[(TileIndex, DT)]],
		 consolidationPartitions: Option[Int],
		 tileType: Option[StorageType]): RDD[TileData[BT]] =
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
		                              Option[DT]))] = {
			val env = SparkEnv.get
			val conf = SparkEnv.get.conf

			data.reduceByKey(binAnalytic.aggregate(_, _),
			                 getNumSplits(consolidationPartitions, data))
				.map(p => (p._1._1, (Some((p._1._2, p._2)), None)))
		}
		// Now the metadata half (in a way that should take no work if there is no metadata)
		val metaData: Option[RDD[(TileIndex, (Option[(BinIndex, PT)],
		                                      Option[DT]))]] =
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

				val definedTileData = tileData.filter(_._1.isDefined)

				// Create our tile
				// Use the type passed in; if no type is passed in, use dense if more than half full.
				val typeToUse = tileType.getOrElse(
					if (definedTileData.size > xLimit*yLimit/2) StorageType.Dense
					else StorageType.Sparse
				)
				val defaultBinValue =
					binAnalytic.finish(binAnalytic.defaultProcessedValue)
				val tile: TileData[BT] = typeToUse match {
					case StorageType.Dense => new DenseTileData[BT](index, defaultBinValue)
					case StorageType.Sparse => new SparseTileData[BT](index, defaultBinValue)
				}

				// Put the proper value into each bin
				definedTileData.foreach(p =>
					{
						val bin = p._1.get._1
						val value = p._1.get._2
						tile.setBin(bin.getX(), bin.getY(), binAnalytic.finish(value))
					}
				)

				// Add in any pre-calculated metadata
				tileData.filter(_._2.isDefined).foreach(p =>
					{
						val analyticValue = p._2.get
						dataAnalytics.map(da => AnalysisDescription.record(analyticValue, da, tile))
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
						AnalysisDescription.record(analyticValue, ta, tile)
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
