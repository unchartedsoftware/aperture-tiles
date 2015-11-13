/*
 * Copyright (c) 2015 Uncharted Software Inc.
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


import grizzled.slf4j.Logging
import org.apache.spark.Accumulator

import scala.collection.mutable.{Map => MutableMap}
import scala.util.Try

import scala.reflect.ClassTag

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.impl.SparseTileData
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.TileData.StorageType
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic



object UniversalBinner {
	/**
	 * Get the number of partitions to use when operating on a data set.
	 *
	 * @param dataSet the dataSet for which to determine the number of
	 *                partitions.
	 * @param minPartitions The minimum number of partitions into which to split the data.  If none,
	 *                      the number of partitions will not be increased.
	 * @param maxPartitions The maximum number of partitions into which to split the data.  If none,
	 *                      the number of partitions will not be decreased.
	 * @return The number of partitions that should be used for this dataset.
	 */
	def getNumSplits[T: ClassTag](dataSet: RDD[T],
	                              minPartitions: Option[Int],
	                              maxPartitions: Option[Int]): Int =
		dataSet.partitions.size
			.max(minPartitions.getOrElse(0))
			.min(maxPartitions.getOrElse(Int.MaxValue))

	/**
	 * Optionally aggregate two optional values
	 *
	 * @param aggFcn An aggregation function for combining two Ts
	 * @param value1 The first value to aggregate
	 * @param value2 The second value to aggregate
	 * @tparam T The type of value to aggregate
	 */
	def optAggregate[T](aggFcn: Option[(T, T) => T],
	                    value1: Option[T], value2: Option[T]): Option[T] =
		aggFcn.map(fcn => (value1 ++ value2).reduceLeftOption(fcn)).getOrElse(None)

	/**
	 * Add two maps together, aggregating entries with the same key in the two maps according to
	 * a given aggregator function.
	 *
	 * @param aggFcn The aggregation function for adding values together
	 * @param map1 The first of the two maps
	 * @param map2 The second of the two maps
	 * @tparam K The class type of the map keys
	 * @tparam V The class type of the map values
	 */
	def aggregateMaps[K, V](aggFcn: (V, V) => V, map1: MutableMap[K, V], map2: MutableMap[K, V]): MutableMap[K, V] = {
		if (map1.size > map2.size) {
			map2.keys.foreach { key =>
				map1(key) = map1.get(key).map(value => aggFcn(value, map2(key))).getOrElse(map2(key))
			}
			map1
		} else {
			map1.keys.foreach { key =>
				map2(key) = map2.get(key).map { value => aggFcn(value, map1(key))}.getOrElse(map1(key))
			}
			map2
		}
	}

	//	def oldAggregateMaps[K, V](aggFcn: (V, V) => V, map1: MutableMap[K, V], map2: MutableMap[K, V]): MutableMap[K, V] = {
	//		(map1.toSeq ++ map2.toSeq).groupBy(_._1).map { case (k, v) => (k, v.map(_._2).reduce(aggFcn)) }
	//	}
}



class UniversalBinner extends Logging {
	import UniversalBinner._

	/** Helper function to mimic RDDBinner interface */
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
		info("Binning data")
		info("\tConsolidation partitions: "+consolidationPartitions)
		info("\tWrite location: "+writeLocation)
		info("\tTile io type: "+tileIO.getClass.getName)
		info("\tlevel sets: "+levelSets.map(_.mkString("[", ", ", "]")).mkString("[", ", ", "]"))
		info("\tX Bins: "+xBins)
		info("\tY Bins: "+yBins)
		info("\tName: "+name)
		info("\tDescription: "+description)

		val startTime = System.currentTimeMillis()

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
				val levelEndTime = System.currentTimeMillis()
				info("Finished binning levels ["+levels.mkString(", ")+"] of data set "
					     + name + " in " + ((levelEndTime-levelStartTime)/60000.0) + " minutes")
			}
		)

		bareData.unpersist(false)

		val endTime = System.currentTimeMillis()
		info("Finished binning data set " + name + " into "
			     + levelSets.map(_.size).reduce(_+_)
			     + " levels (" + levelSets.map(_.mkString(",")).mkString(";") + ") in "
			     + ((endTime-startTime)/60000.0) + " minutes")
	}

	/** Helper function to mimic RDDBinner interface */
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
		processData[IT, PT, AT, DT, BT](data, binAnalytic, tileAnalytics, dataAnalytics,
		                                StandardBinningFunctions.locateIndexOverLevels(indexScheme, tileScheme, xBins, yBins)(levels),
		                                StandardBinningFunctions.populateTileIdentity,
		                                BinningParameters(true, xBins, yBins, consolidationPartitions, consolidationPartitions, tileType))
	}

	/**
	 * @param data The data to tile
	 * @param binAnalytic The aggregation function to use to tile the data
	 * @param tileAnalytics Optional analytics to apply to each produced tile
	 * @param dataAnalytics Optional analytics to apply to each raw data point, and bin along with
	 *                      the tiles.
	 * @param locateIndexFcn: A function that takes in input index, and indicates which tile(s) it
	 *                        is on.  The array of bin indices indicates precisely where on the
	 *                        tiles is indicated, in universal bin coordinates.
	 * @param populateTileFcn A function that takes the precise specification of the input location
	 *                        and value in a single tile, and outputs all the located values in
	 *                        that tile.  The input bins are in universal bin coordinates, as in
	 *                        locateIndexFcn, while the output bins are bins for the specific tile
	 *                        in question.  Note that this function should return an empty map if
	 *                        given empty inputs.
	 * @param parameters General binning parameters affecting how this tiling will be done.
	 */
	def processData[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 locateIndexFcn: IT => Traversable[(TileIndex, Array[BinIndex])],
		 populateTileFcn: (TileIndex, Array[BinIndex], PT) => MutableMap[BinIndex, PT],
		 parameters: BinningParameters = new BinningParameters()): RDD[TileData[BT]] =
	{
		// Convert raw indices into tiles and bins
		val consolidatedByPartition: RDD[(TileIndex, Array[BinIndex], PT, Option[DT])] =
			data.flatMap { record =>
				val indices: Traversable[(TileIndex, Array[BinIndex])] = locateIndexFcn(record._1)
				val value: PT = record._2
				val analyticValue: Option[DT] = record._3
				indices.map(index => (index._1, index._2, value, analyticValue))
			}

		// Combine all information from a single tile
		val createCombiner: ((TileIndex, Array[BinIndex], PT, Option[DT])) => (MutableMap[BinIndex, PT], Option[DT]) =
			c => {
				val (tile, bins, value, analyticValue) = c

				// Accumulate data analytic metadata
				analyticValue.foreach(av =>
					dataAnalytics.foreach(analytic =>
						analytic.accumulate(tile, av)
					)
				)

				(populateTileFcn(tile, bins, value), analyticValue)
			}
		val mergeValue: ((MutableMap[BinIndex, PT], Option[DT]),
		                 (TileIndex, Array[BinIndex], PT, Option[DT])) => (MutableMap[BinIndex, PT], Option[DT]) =
			(aggregateValue, recordValue) => {
				val (binValues, curAnalyticValue) = aggregateValue
				val (tile, bins, value, newAnalyticValue) = recordValue
				val binAggregator = binAnalytic.aggregate(_, _)
				val analyticAggregator = dataAnalytics.map(analytic => analytic.analytic.aggregate(_, _))

        // Accumulate data analytic metadata
				newAnalyticValue.foreach(av => dataAnalytics.foreach(analytic => analytic.accumulate(tile, av)))

				(aggregateMaps(binAggregator, binValues, populateTileFcn(tile, bins, value)),
				 optAggregate(analyticAggregator, curAnalyticValue, newAnalyticValue))
			}
		val mergeCombiners: ((MutableMap[BinIndex, PT], Option[DT]),
		                     (MutableMap[BinIndex, PT], Option[DT])) => (MutableMap[BinIndex, PT], Option[DT]) =
			(tileValues1, tileValues2) => {
				val (binValues1, analyticValue1) = tileValues1
				val (binValues2, analyticValue2) = tileValues2
				val binAggregator = binAnalytic.aggregate(_, _)
				val analyticAggregator = dataAnalytics.map(analytic => analytic.analytic.aggregate(_, _))
				(aggregateMaps(binAggregator, binValues1, binValues2),
				 optAggregate(analyticAggregator, analyticValue1, analyticValue2))
			}
		val a = consolidatedByPartition.map{case (tile, bins, value, analyticValue) =>
			(tile, (tile, bins, value, analyticValue))
		}
		val tileInfos = a.combineByKey[(MutableMap[BinIndex, PT], Option[DT])](createCombiner, mergeValue, mergeCombiners)

		// Now, go through those results and convert to tiles.
		tileInfos.map{tileInfo =>
			val index = tileInfo._1
			val binValues = tileInfo._2._1
			val analyticValue = tileInfo._2._2

			// Determine if we need a dense or sparse tile
			val numValues = binValues.size
			val xLimit = index.getXBins
			val yLimit = index.getYBins
			val typeToUse = parameters.tileType.getOrElse(
				if (numValues > xLimit*yLimit/2) StorageType.Dense
				else StorageType.Sparse
			)

			// Create our tile
			val defaultBinValue = binAnalytic.finish(binAnalytic.defaultProcessedValue)
			val tile: TileData[BT] = typeToUse match {
				case StorageType.Dense => new DenseTileData[BT](index, defaultBinValue)
				case StorageType.Sparse => new SparseTileData[BT](index, defaultBinValue)
			}

			// Populate our tile with basic bin data
			binValues.foreach{case (bin, value) =>
				tile.setBin(bin.getX, bin.getY, binAnalytic.finish(value))
			}

			// Add in data analytics
			dataAnalytics.foreach(da =>
				analyticValue.foreach(a =>
					AnalysisDescription.record(a, da, tile)
				)
			)

			// Add in tile analytics
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
	}
}

/**
 * A simple parameter class to encapsulate the various parameters used by a binning job
 *
 * @param debug Whether or not to output debug information about the job
 * @param xBins The number of bins per generated tile in the horizontal direction
 * @param yBins the number of bins per generated tile in the vertical direction
 * @param minPartitions The minimum number of partitions to use during reduce operations
 * @param maxPartitions The maximum number of partitions to use during reduce operations
 * @param tileType The type of tile to generate (dense or sparse); None for a fairly good
 *                 heuristic to decide on a tile-by-tile basis.
 */
case class BinningParameters (debug: Boolean = true,
                              xBins: Int = 256, yBins: Int = 256,
                              minPartitions: Option[Int] = None,
                              maxPartitions: Option[Int] = None,
                              tileType: Option[StorageType] = None)
