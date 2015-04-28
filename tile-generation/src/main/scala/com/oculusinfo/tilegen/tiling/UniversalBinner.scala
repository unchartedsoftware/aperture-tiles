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
	def getNumSplits[T: ClassTag] (dataSet: RDD[T],
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
	def optAggregate[T] (aggFcn: Option[(T, T) => T],
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
	def aggregateMaps[K, V] (aggFcn: (V, V) => V, map1: Map[K, V], map2: Map[K, V]): Map[K, V] =
		(map1.toSeq ++ map2.toSeq).groupBy(_._1).map{case (k, v) => (k, v.map(_._2).reduce(aggFcn))}
}



class UniversalBinner {
	import UniversalBinner._


	private def log (msg: String) =
		println(msg)

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
		log("Binning data")
		log("\tConsolidation partitions: "+consolidationPartitions)
		log("\tWrite location: "+writeLocation)
		log("\tTile io type: "+tileIO.getClass.getName)
		log("\tlevel sets: "+levelSets.map(_.mkString("[", ", ", "]"))
			    .mkString("[", ", ", "]"))
		log("\tX Bins: "+xBins)
		log("\tY Bins: "+yBins)
		log("\tName: "+name)
		log("\tDescription: "+description)

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
				log("Finished binning levels ["+levels.mkString(", ")+"] of data set "
					    + name + " in " + ((levelEndTime-levelStartTime)/60000.0) + " minutes")
			}
		)

		bareData.unpersist(false)

		val endTime = System.currentTimeMillis()
		log("Finished binning data set " + name + " into "
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
		                                StandardBinningFunctions.locateIndexIdentity(indexScheme, tileScheme, levels, xBins, yBins),
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
		 populateTileFcn: (TileIndex, Array[BinIndex], PT) => Map[BinIndex, PT],
		 parameters: BinningParameters = new BinningParameters()): RDD[TileData[BT]] =
	{
		// If asked to run in debug mode, keep some stats on how much aggregation is going on in
		// this stage.
		val aggregationTracker = if (parameters.debug) Some(data.context.accumulator(0)) else None

		// First, within each partition, group data by tile
		val consolidatedByPartition: RDD[(TileIndex, Array[BinIndex], PT, Option[DT])] =
			data.mapPartitions{iter =>
				val partitionResults = MutableMap[(TileIndex, Array[BinIndex]), (PT, Option[DT])]()

				// Map each input record in this partition into tile coordinates, ...
				iter.flatMap(record =>
					locateIndexFcn(record._1).map(index => (index, (record._2, record._3)))
				).foreach{case (key, newValue) =>
						// ... and consolidate identical input records in this partition.
						if (partitionResults.contains(key)) {
							val oldValue = partitionResults(key)
							val analyticAggregator =
								dataAnalytics.map(analytic => analytic.analytic.aggregate(_, _))
							partitionResults(key) = (binAnalytic.aggregate(newValue._1, oldValue._1),
							                         optAggregate(analyticAggregator,
							                                      newValue._2, oldValue._2))
							aggregationTracker.foreach(_ += 1)
						} else {
							partitionResults(key) = newValue
						}
				}
				partitionResults.iterator.map(results => (results._1._1, results._1._2, results._2._1, results._2._2))
			}

		// TODO: If this works, look at using MutableMaps instead of Maps as the first output
		// value, and adding in place.
		// TODO: If that works, look into getting rid of the mutable map in the previous step
		// Combine all information from a single tile
		val createCombiner: ((TileIndex, Array[BinIndex], PT, Option[DT])) => (Map[BinIndex, PT], Option[DT]) =
			c => {
				val (tile, bins, value, analyticValue) = c
				(populateTileFcn(tile, bins, value), analyticValue)
			}
		val mergeValue: ((Map[BinIndex, PT], Option[DT]),
		                 (TileIndex, Array[BinIndex], PT, Option[DT])) => (Map[BinIndex, PT], Option[DT]) =
			(aggregateValue, recordValue) => {
				val (binValues, curAnalyticValue) = aggregateValue
				val (tile, bins, value, newAnalyticValue) = recordValue
				val binAggregator = binAnalytic.aggregate(_, _)
				val analyticAggregator = dataAnalytics.map(analytic => analytic.analytic.aggregate(_, _))
				(aggregateMaps(binAggregator, binValues, populateTileFcn(tile, bins, value)),
				 optAggregate(analyticAggregator, curAnalyticValue, newAnalyticValue))
			}
		val mergeCombiners: ((Map[BinIndex, PT], Option[DT]),
		                     (Map[BinIndex, PT], Option[DT])) => (Map[BinIndex, PT], Option[DT]) =
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
		val tileInfos = a.combineByKey[(Map[BinIndex, PT], Option[DT])](createCombiner, mergeValue, mergeCombiners)

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
 * A repository of standard index location and tile population functions, for use with the 
 * UniversalBinner
 */
object StandardBinningFunctions {
	/**
	 * Simple function to spread an input point over several levels of tile pyramid.
	 */
	def locateIndexIdentity[T](indexScheme: IndexScheme[T], pyramid: TilePyramid,
	                           levels: Traversable[Int], xBins: Int = 256, yBins: Int = 256)
			: T => Traversable[(TileIndex, Array[BinIndex])] =
		index => {
			val (x, y) = indexScheme.toCartesian(index)
			levels.map{level =>
				val tile = pyramid.rootToTile(x, y, level, xBins, yBins)
				val bin = pyramid.rootToBin(x, y, tile)
				(tile, Array(bin))
			}
		}

	/**
	 * Simple function to spread an input point over several levels of tile pyramid, ignoring 
	 * points that are out of bounds
	 */
	def locateBoundedIndex[T](indexScheme: IndexScheme[T], pyramid: TilePyramid,
	                          levels: Traversable[Int], xBins: Int = 256, yBins: Int = 256)
			: T => Traversable[(TileIndex, Array[BinIndex])] = {
		val bounds = pyramid.getTileBounds(new TileIndex(0, 0, 0))
		val (minX, minY, maxX, maxY) = (bounds.getMinX, bounds.getMinY,
		                                bounds.getMaxX, bounds.getMaxY)

		index => {
			val (x, y) = indexScheme.toCartesian(index)
			if (minX <= x && x < maxX && minY <= y && y < maxY) {
				levels.map{level =>
					val tile = pyramid.rootToTile(x, y, level, xBins, yBins)
					val bin = pyramid.rootToBin(x, y, tile)
					(tile, Array(bin))
				}
			} else {
				Traversable()
			}
		}
	}




	/**
	 * Simple function to spread an input lines over several levels of tile pyramid.
	 * 
	 * @param indexScheme The scheme for interpretting input indices
	 * @param pyramid The tile pyramid for projecting interpretted indices into tile space.
	 * @param levels The levels at which to tile
	 * @param minBins The minimum length of a segment, in bins, below which it is not drawn, or None 
	 *                to have no minimum segment length
	 * @param maxBins The maximum length of a segment, in bins, above which it is not drawn, or None 
	 *                to have no minimum segment length
	 * @param xBins The number of bins into which each tile is broken in the horizontal direction
	 * @param yBins the number of bins into which each tile is broken in the vertical direction
	 * @return a traversable over the tiles this line crosses, each associated with the overall 
	 *         endpoints of this line, in universal bin coordinates.
	 */
	def locateLine[T](indexScheme: IndexScheme[T], pyramid: TilePyramid, levels: Traversable[Int],
	                  minBins: Option[Int], maxBins: Option[Int],
	                  xBins: Int = 256, yBins: Int = 256)
			: T => Traversable[(TileIndex, Array[BinIndex])] = {
		val bounds = pyramid.getTileBounds(new TileIndex(0, 0, 0))
		val (minX, minY, maxX, maxY) = (bounds.getMinX, bounds.getMinY,
		                                bounds.getMaxX, bounds.getMaxY)

		index => {
			val (x1, y1, x2, y2) = indexScheme.toCartesianEndpoints(index)
			if (minX <= x1 && x1 <= maxX &&
				    minY <= y1 && y1 <= maxY &&
				    minX <= x2 && x2 <= maxX &&
				    minY < y2 && y2 <= maxY) {
				levels.flatMap{level =>
					val tile1 = pyramid.rootToTile(x1, y1, level, xBins, yBins)
					val tileBin1 = pyramid.rootToBin(x1, y1, tile1)
					val uniBin1 = TileIndex.tileBinIndexToUniversalBinIndex(tile1, tileBin1)

					val tile2 = pyramid.rootToTile(x2, y2, level, xBins, yBins)
					val tileBin2 = pyramid.rootToBin(x2, y2, tile2)
					val uniBin2 = TileIndex.tileBinIndexToUniversalBinIndex(tile2, tileBin2)

					val length = (math.abs(uniBin1.getX - uniBin2.getX) max
						              math.abs(uniBin1.getY - uniBin2.getY))

					if (minBins.map(_ <= length).getOrElse(true) &&
						    maxBins.map(_ > length).getOrElse(true)) {
						// Fill in somewhere around here.
						linearTiles(uniBin1, uniBin2, tile1).map(tile => (tile, Array(uniBin1, uniBin2)))
					} else {
						Traversable()
					}
				}
			} else {
				println(minX)
				println(x1)
				println(x2)
				println(maxX)
				println(minY)
				println(y1)
				println(y2)
				println(maxY)
				println((minX <= x1 && x1 < maxX))
				println((minY <= y1 && y1 < maxY))
				println((minX <= x2 && x2 < maxX))
				println((minY < y2 && y2 < maxY))
				Traversable()
			}
		}
	}

	/**
	 * Simple population function that just takes input points and outputs them, as is, in the 
	 * correct coordinate system.
	 */
	def populateTileIdentity[T]: (TileIndex, Array[BinIndex], T) => Map[BinIndex, T] =
		(tile, bins, value) => bins.map(bin => (TileIndex.universalBinIndexToTileBinIndex(tile, bin).getBin, value)).toMap

	/**
	 * Line segment population function
	 * 
	 * Takes endpoints of line segments, and populates the tiles with the points appropriate to that tile
	 */
	def populateTileWithLineSegments[T]: (TileIndex, Array[BinIndex], T) => Map[BinIndex, T] =
		(tile, bins, value) => {
			linearBinsForTile(bins(0), bins(1), tile).map(bin => (bin, value)).toMap
		}



	/*
	 * Re-order coords of two endpoints for efficient implementation of Bresenham's line algorithm  
	 */ 
	private def  initializeBresenham (start: BinIndex, end: BinIndex)
			: (Boolean, Int, Int, Int, Int) = {
		val xs = start.getX()
		val xe = end.getX()
		val ys = start.getY()
		val ye = end.getY()
		val steep = (math.abs(ye - ys) > math.abs(xe - xs))

		if (steep) {
			if (ys > ye) {
				(steep, ye, xe, ys, xs)
			} else {
				(steep, ys, xs, ye, xe)
			}
		} else {
			if (xs > xe) {
				(steep, xe, ye, xs, ys)
			} else {
				(steep, xs, ys, xe, ye)
			}
		}
	}

	/**
	 * Compute the intermediate points between two endpoints using Bresneham's algorithm
	 * 
	 * @param start The start bin, in unviersal bin coordinates, of the segment
	 * @param end The end bin, in universal bin coordinates, of the segment
	 * @return Each bin in the segment, in universal bin coordinates
	 */
	def linearUniversalBins (start: BinIndex, end: BinIndex): Traversable[BinIndex] = {
		val (steep, x0, y0, x1, y1) = initializeBresenham(start, end)

		val deltax = x1-x0
		val deltay = math.abs(y1-y0)
		var error = deltax>>1
		var y = y0
		val ystep = if (y0 < y1) 1 else -1

		// x1+1 needed here so that "end" bin is included in Sequence
		Iterable.range(x0, x1+1).map{x =>
			val ourY = y
			error = error - deltay
			if (error < 0) {
				y = y + ystep
				error = error + deltax
			}

			if (steep) new BinIndex(ourY, x)
			else new BinIndex(x, ourY)
		}
	}

	/**
	 * Compute the tiles between two endpoints, using a modified version of Bresneham's 
	 * algorithm, in a way that should be completely self-consistent with a Bresneham-based bin
	 * extraction function.
	 * 
	 * @param start The start bin, in unviersal bin coordinates, of the segment
	 * @param end The end bin, in universal bin coordinates, of the segment
	 * @param sample A sample tile, indicating the level and tile size of the desired output tiles
	 * @return Each tile in the segment, in universal bin coordinates
	 */
	def linearTiles (start: BinIndex, end: BinIndex, sample: TileIndex)
			: Traversable[TileIndex] = {
		val (steep, x0, y0, x1, y1) = initializeBresenham(start, end)
		val (xSize, ySize) =
			if (steep) (sample.getYBins, sample.getXBins)
			else (sample.getXBins, sample.getYBins)
		val level = sample.getLevel

		val deltax: Long = x1 - x0
		val deltay: Long = math.abs(y1 - y0)
		val baseError: Long = deltax >> 1
		val ystep = if (y0 < y1) 1 else -1

		// Function to convert from universal bin to tile quickly and easily
		def binToTile (x: Int, y: Int) =
			if (steep) TileIndex.universalBinIndexToTileBinIndex(sample, new BinIndex(y, x)).getTile
			else TileIndex.universalBinIndexToTileBinIndex(sample, new BinIndex(x, y)).getTile

		// Find nth bin from scratch
		def tileX (x: Int) = {
			val dx = x - x0
			val e = baseError - deltay * dx
			val y = if (e < 0) {
				val factor = math.ceil(-e.toDouble / deltax).toInt
				y0 + factor * ystep
			} else {
				y0
			}
			binToTile(x, y)
		}
		
		// Determine the start of the range of internal tiles
		val t0 = (x0 + xSize - (x0 % xSize))/xSize
		val tn = (x1 - (x1 %xSize))/xSize

		// Determine the end of the range of internal tiles
		val x11 = x1 - (x1%xSize)
		val t1 = x11 / xSize

		// Determine first and last tiles
		val tile0 = binToTile(x0, y0)
		val tile0a = tileX(t0 * xSize - 1)
		val tile1a = tileX(t1 * xSize)
		val tile1 = binToTile(x1, y1)
		val initialTiles = if (tile0 == tile0a || t0 > t1) Traversable(tile0) else Traversable(tile0, tile0a)
		val finalTiles = if (tile1 == tile1a || t0 > t1) Traversable(tile1) else Traversable(tile1a, tile1)


		initialTiles ++ Iterable.range(t0, tn).flatMap{t =>
			val startTile = tileX(t * xSize)
			val endTile = tileX((t + 1) * xSize - 1)

			if (startTile == endTile) Traversable(startTile) else Traversable(startTile, endTile)
		} ++ finalTiles
	}

	/**
	 * Compute all the bins on a single tile that are on the line between two given endpoints, 
	 * using a modified version of Bresneham's algorithm, and in a way that guarantees 
	 * consistency between this and a total-line Bresneham-based line-drawing function.
	 * 
	 * @param start The start bin, in unviersal bin coordinates, of the segment
	 * @param end The end bin, in universal bin coordinates, of the segment
	 * @param tile The tile whose bins are desired
	 * @return Each bin in the given tile on this line, in tile coordinates.
	 */
	def linearBinsForTile (start: BinIndex, end: BinIndex, tile: TileIndex): Traversable[BinIndex] = {
		val (steep, x0, y0, x1, y1) = initializeBresenham(start, end)

		val deltax: Long = x1 - x0
		val deltay: Long = math.abs(y1 - y0)
		var error: Long = deltax >> 1
		var y = y0
		val ystep = if (y0 < y1) 1 else -1

		// Figure out the bounds of this tile in our x direction
		val tileMin = TileIndex.tileBinIndexToUniversalBinIndex(tile, new BinIndex(0, 0))
		val tileMax = TileIndex.tileBinIndexToUniversalBinIndex(tile, new BinIndex(tile.getXBins-1, tile.getYBins-1))
		val (minX, maxX, minY, maxY) =
			if (steep) (tileMin.getY, tileMax.getY, tileMin.getX, tileMax.getX)
			else (tileMin.getX, tileMax.getX, tileMin.getY, tileMax.getY)

		val xx0 = x0 max minX
		val xx1 = (x1 min maxX) + 1

		// Offset to our start location
		val startOffset = xx0 - x0
		error = error - startOffset * deltay
		if (error < 0) {
			val factor = math.ceil(-error.toDouble / deltax).toInt
			error = error + factor * deltax
			y = y + factor*ystep
		}

		// Get the universal bin of our lower left corner (for offsets)
		var baseX = if (steep) tileMax.getY else tileMin.getX
		var baseY = if (steep) tileMin.getX else tileMax.getY

		// And iterate over our range
		Iterable.range(xx0, xx1).flatMap{x =>
			val curY = y
			error = error - deltay
			if (error < 0) {
				y = y + ystep
				error = error + deltax
			}

			if (minY <= curY && curY <= maxY)
				Some(if (steep) TileIndex.universalBinIndexToTileBinIndex(tile, new BinIndex(curY, x)).getBin
				     else TileIndex.universalBinIndexToTileBinIndex(tile, new BinIndex(x, curY)).getBin)
			else None
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
