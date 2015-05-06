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

import scala.collection.TraversableOnce
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.Try

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import com.oculusinfo.binning._
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.binning.impl.SparseTileData
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.tilegen.tiling.analytics.{TileAnalytic, AnalysisDescription, BinningAnalytic}
import com.oculusinfo.tilegen.util.EndPointsToLine


class LineSegmentIndexScheme extends IndexScheme[(Double, Double, Double, Double)] with Serializable {
	def toCartesianEndpoints (coords: (Double, Double, Double, Double)): (Double, Double, Double, Double) = coords
	def toCartesian (coords: (Double, Double, Double, Double)): (Double, Double) = (coords._1, coords._2)
}


/**
 * This takes an RDD of line segment data (ie pairs of endpoints) and transforms it
 * into a pyramid of tiles.  minBins and maxBins define the min and max valid rane for
 * line segments that are included in the binning process
 */
object RDDLineBinner {

	def getNumSplits[T: ClassTag] (requestedPartitions: Option[Int], dataSet: RDD[T]): Int = {
		val curSize = dataSet.partitions.size
		val result = curSize max requestedPartitions.getOrElse(0)
		result
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
	protected def universalBinsToTiles[PT](baseTile: TileIndex, bins: IndexedSeq[(BinIndex, PT)],
	                                       uniBinToTB: (TileIndex, BinIndex) => TileAndBinIndices):
			Traversable[TileIndex] =
	{
		bins.map(b =>
			{
				val ubin = b._1
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
	protected def universalBinsToBins[PT](tile: TileIndex, bins: IndexedSeq[(BinIndex, PT)],
	                                      uniBinToTB: (TileIndex, BinIndex) => TileAndBinIndices):
			IndexedSeq[(BinIndex, PT)] =
	{
		//get tile/bin and line-scale pairs
		val tb_scale = bins.map(b => {
			                        val (ubin, value) = b
			                        val tb = uniBinToTB(tile, ubin)
			                        (tb, value)
		                        })

		// filter results so only ones for current tile remain
		tb_scale.filter(_._1.getTile().equals(tile))
			.map(b => {
				     val bin = b._1.getBin	//bin result
				     val value = b._2		// and corresponding line scale value for this bin
				     (bin, value)
			     })
	}
}



class RDDLineBinner(minBins: Int = 2,
                    maxBins: Int = 1024,		// 1<<10 = 1024  256*4 = 4 tile widths
                    bDrawLineEnds: Boolean = false) {
	var debug: Boolean = true

	def transformData[RT: ClassTag, IT: ClassTag, PT: ClassTag, DT: ClassTag]
		(data: RDD[RT],
		 indexFcn: RT => Try[IT],
		 valueFcn: RT => Try[PT],
		 dataAnalytics: Option[AnalysisDescription[RT, DT]] = None):
			RDD[(IT, PT, Option[DT])] =
	{
		// Process the data to remove all but the minimal portion we need for
		// tiling - x coordinate, y coordinate, and bin value
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
	 * @tparam AT The type of tile analytic to apply to tiles
	 * @tparam DT The type of data analytic to apply to raw data
	 * @tparam BT The output bin type
	 */
	def binAndWriteData[RT: ClassTag, IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT] (
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
		calcLinePixels: (BinIndex, BinIndex, PT) => IndexedSeq[(BinIndex, PT)] =
			new EndPointsToLine().endpointsToLineBins,
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
				                               tileType,
				                               calcLinePixels)
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
	 * @param calcLinePixels A function used to rasterize the line/arc between
	 *                       two end points.  Defaults to a line based implementation.
	 * @param usePointBinner Indicates whether the lines will be consolidate by point
	 *                       or by tile.  Defaults to using point based consolidation.
	 * @param linesAsArcs Indicates whether the endpoints have lines drawn between them,
	 *                    or arcs.  Defaults to lines.
	 * @tparam IT the index type, convertible to a cartesian pair with the coordinateFromIndex function
	 * @tparam PT The bin type, when processing and aggregating
	 * @tparam AT The type of tile-level analytic to calculate for each tile.
	 * @tparam DT The type of raw data-level analytic that already has been calculated for each tile.
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
		 tileType: Option[StorageType] = None,
		 calcLinePixels: (BinIndex, BinIndex, PT) => IndexedSeq[(BinIndex, PT)]	=
			 new EndPointsToLine().endpointsToLineBins,
		 usePointBinner: Boolean = true,
		 linesAsArcs: Boolean = false,
		 drawDirectedArcs: Boolean = false):
			RDD[TileData[BT]] =
	{
		val tileBinToUniBin = (TileIndex.tileBinIndexToUniversalBinIndex)_

		val localMinBins = minBins
		val localMaxBins = maxBins
		val localDrawLineEnds = bDrawLineEnds

		val mapOverLevels: IT => TraversableOnce[(BinIndex, BinIndex, TileIndex)] =
			//BinIndex == universal bin indices in this case
			index => {
				val (x1, y1, x2, y2) = indexScheme.toCartesianEndpoints(index)
				levels.map(level =>
					{
						if (tileScheme.getProjection.equals("EPSG:900913") &&
							(y1 > 85.05115 || y1 < -85.05115 || y2 > 85.05115 || y2 < -85.05115)) {
							// If using mercator projection then exclude line segments if
							// either endpoint is near the poles (latitude > 85.05115 or < -85.05115 degrees)
							(null, null, null)
						}
						else {
							// find 'universal' bins for both endpoints
							val tile1 = tileScheme.rootToTile(x1, y1, level, xBins, yBins)
							val tileBin1 = tileScheme.rootToBin(x1, y1, tile1)
							val unvBin1 = tileBinToUniBin(tile1, tileBin1)

							val tile2 = tileScheme.rootToTile(x2, y2, level, xBins, yBins)
							val tileBin2 = tileScheme.rootToBin(x2, y2, tile2)
							val unvBin2 = tileBinToUniBin(tile2, tileBin2)

							// check if line length is within valid range
							val points =
								(math.abs(unvBin1.getX() - unvBin2.getX()) max
									math.abs(unvBin1.getY() - unvBin2.getY()))
							if (points < localMinBins || (!localDrawLineEnds && points > localMaxBins)) {
								// line segment either too short or too long (so
								// disregard)
								(null, null, null)
							} else {
								// return endpoints as pair of universal bins per
								// level (note, we need one of the tile indices here
								// to keep level info for this line segment)
								if (linesAsArcs && drawDirectedArcs) {
									(unvBin1, unvBin2, tile1)
								}
								else {
									// If not drawing directed CW arcs, then use convention
									// of endpoint with min X value listed first
									if (unvBin1.getX() < unvBin2.getX())
										(unvBin1, unvBin2, tile1)
									else
										(unvBin2, unvBin1, tile2)
								}
							}
						}
					}
				)
			}

		processData(data, binAnalytic, tileAnalytics, dataAnalytics,
		            mapOverLevels, xBins, yBins, consolidationPartitions, tileType, calcLinePixels,
		            usePointBinner, linesAsArcs)
	}



	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles.
	 *
	 * @param data The data to be processed
	 * @param binAnalytic A description of how raw values are aggregated into
	 *                    bin values
	 * @param tileAnalytics A description of analytics that can be run on each
	 *                      tile, and how to aggregate them
	 * @param dataAnalytics A description of analytics that can be run on the
	 *                      raw data, and recorded (in the aggregate) on each
	 *                      tile
	 * @param indexToUniversalBins A function that spreads a data point out over the tiles and bins of interest
	 * @param xBins The number of bins along the horizontal axis of each tile
	 * @param yBins The number of bins along the vertical axis of each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 * @param tileType A specification of how data should be stored.  If None, a heuristic will be used that will use
	 *                 the optimal type for a double-valued tile, and isn't too bad for smaller-valued types.  For
	 *                 significantly larger-valued types, Some(Sparse) would probably work best.
	 * @param calcLinePixels A function used to rasterize the line/arc between
	 *                       two end points.  Defaults to a line based implementation.
	 * @param usePointBinner Indicates whether the lines will be consolidate by point
	 *                       or by tile.  Defaults to using point based consolidation.
	 * @param linesAsArcs Indicates whether the endpoints have lines drawn between them,
	 *                    or arcs.  Defaults to lines.
	 * @tparam IT The index type, convertable to tile and bin
	 * @tparam PT The bin type, when processing and aggregating
	 * @tparam AT The type of tile-level analytic to calculate for each tile.
	 * @tparam DT The type of raw data-level analytic that already has been calculated for each tile.
	 * @tparam BT The final bin type, ready for writing to tiles
	 */
	def processData[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 indexToUniversalBins: IT => TraversableOnce[(BinIndex, BinIndex, TileIndex)],
		 xBins: Int = 256,
		 yBins: Int = 256,
		 consolidationPartitions: Option[Int] = None,
		 tileType: Option[StorageType] = None,
		 calcLinePixels: (BinIndex, BinIndex, PT) => IndexedSeq[(BinIndex, PT)] =
			 new EndPointsToLine().endpointsToLineBins,
		 usePointBinner: Boolean = true,
		 linesAsArcs: Boolean = false): RDD[TileData[BT]] =
	{
		val metaData = processMetaData(data, indexToUniversalBins, dataAnalytics)

		// We first bin data in each partition into its associated bins
		val partitionBins = data.mapPartitions(iter =>
			{
				val partitionResults: MutableMap[(BinIndex, BinIndex, TileIndex), PT] =
					MutableMap[(BinIndex, BinIndex, TileIndex), PT]()

				// Map each data point in this partition into its bins
				iter.flatMap(record =>
					indexToUniversalBins(record._1)
						.filter(_._1 != null)
						.map(tbi => (tbi, record._2))
				).foreach(tbv =>
					// And combine bins within this partition
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

		val uniBinToTileBin = {
			if (linesAsArcs)
				(TileIndex.universalBinIndexToTileBinIndexClipped)_	// need to clip arc pts that go outside valid tile/bin bounds
			else
				(TileIndex.universalBinIndexToTileBinIndex)_
		}

		// Now, combine by-partition bins into global bins, and turn them into tiles.
		if (usePointBinner) {
			consolidateByPoints(partitionBins, binAnalytic, tileAnalytics, dataAnalytics,
			                    metaData, consolidationPartitions, tileType,
			                    xBins, yBins, uniBinToTileBin, calcLinePixels)
		} else {
			consolidateByTiles(partitionBins, binAnalytic, tileAnalytics, dataAnalytics,
			                   metaData, consolidationPartitions,
			                   xBins, yBins, uniBinToTileBin, calcLinePixels)
		}
	}



	/**
	 * Process a simplified input dataset to run any raw data-based analysis
	 *
	 * @tparam IT The index type of the data set
	 * @tparam PT The type of data to be processed into tiles
	 * @tparam DT The type of data analytic used
	 * @param data The data to process
	 * @param indexToUniversalBins A function that spreads a data point out over
	 *                             area of interest
	 * @param dataAnalytics An optional transformation from a raw data record
	 *                      into an aggregable analytic value.  If None, this
	 *                      should cause no extra processing. If multiple
	 *                      analytics are desired, use ComposedTileAnalytic
	 */
	def processMetaData[IT: ClassTag, PT: ClassTag, DT: ClassTag]
		(data: RDD[(IT, PT, Option[DT])],
		 indexToUniversalBins: IT => TraversableOnce[(BinIndex, BinIndex, TileIndex)],
		 dataAnalytics: Option[AnalysisDescription[_, DT]]):
			Option[RDD[(TileIndex, DT)]] =
	{
		dataAnalytics.map{da =>
			data.mapPartitions{iter =>
				val partitionResults = MutableMap[TileIndex, DT]()
				iter.foreach{record =>
					indexToUniversalBins(record._1).foreach{indices =>
						val tile = indices._3
						val value = record._3
						value.foreach{v =>
							partitionResults(tile) =
								if (partitionResults.contains(tile)) {
									da.analytic.aggregate(partitionResults(tile), v)
								} else {
									v
								}
							da.accumulate(tile, v)
						}
					}
				}

				partitionResults.iterator
			}.reduceByKey(da.analytic.aggregate(_, _))
		}
	}



	private def consolidateByPoints[PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[((BinIndex, BinIndex, TileIndex), PT)],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 tileMetaData: Option[RDD[(TileIndex, DT)]],
		 consolidationPartitions: Option[Int],
		 tileType: Option[StorageType],
		 xBins: Int = 256,
		 yBins: Int = 256,
		 uniBinToTileBin: (TileIndex, BinIndex) => TileAndBinIndices,
		 calcLinePixels: (BinIndex, BinIndex, PT) => IndexedSeq[(BinIndex, PT)]):
			RDD[TileData[BT]] =
	{
		// Do reduceByKey to account for duplicate lines at a given level
		// (not really necessary, but might speed things up?)
		// val reduced1 = data.reduceByKey(binAnalytic.aggregate(_, _),
		//                                 getNumSplits(consolidationPartitions, data))

		// We need to consolidate both metadata and binning data, so our result
		// has two slots, and each half populates one of them.
		//
		// We need to do this right away because the tiles should be immutable
		// once created
		//
		// For the same reason, we'll have to run the tile analytic when we
		// create the tile, too.
		//
		// First, the binning data half.

		//     Draw lines (based on endpoint bins), and convert all results from
		//     universal bins to tile,bin coords
		val expanded = data.flatMap(p =>
			{
				val ((lineStart, lineEnd, tile), procValue) = p
				calcLinePixels(lineStart, lineEnd, procValue).map(b =>
					{
						val (bin, scaledValue) = b
						val tb = uniBinToTileBin(tile, bin)
						((tb.getTile(), tb.getBin()), scaledValue)
					}
				)
			}
		)

		//     Rest of process is same as regular RDDBinner (reduceByKey, convert
		//     to (tile,(bin,value)), groupByKey, and create tiled results)
		val reduced: RDD[(TileIndex, (Option[(BinIndex, PT)],
		                              Option[DT]))] =
			expanded.reduceByKey(binAnalytic.aggregate(_, _),
			                     RDDLineBinner.getNumSplits(consolidationPartitions, expanded)
			).map(p => (p._1._1, (Some((p._1._2, p._2)), None)))

		// Now the metadata half (in a way that should take no work if there is no metadata)
		val metaData: Option[RDD[(TileIndex, (Option[(BinIndex, PT)],
		                                      Option[DT]))]] =
			tileMetaData.map(
				_.map{case (index, metaData) => (index, (None, Some(metaData))) }
			)

		// Get the combination of the two sets, again in a way that does
		// no extra work if there is no metadata
		val toTile =
			if (metaData.isDefined) reduced union metaData.get
			else reduced



		toTile
			.groupByKey(RDDLineBinner.getNumSplits(consolidationPartitions, toTile))
			.map(t =>
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

				// Put the proper value in each bin
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

				// Calculate and add in any tile-level metadata we've been told to calcualte
				tileAnalytics.map(ta =>
					{
						// Figure out the value for this tile
						val analyticValue = ta.convert(tile)
						// Add it into any appropriate accumulators
						ta.accumulate(index, analyticValue)
						// And store it in the tile's metadata
						AnalysisDescription.record(analyticValue, ta, tile)
					}
				)

				tile
			}
		)
	}



	private def consolidateByTiles[PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[((BinIndex, BinIndex, TileIndex), PT)],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 tileMetaData: Option[RDD[(TileIndex, DT)]],
		 consolidationPartitions: Option[Int],
		 xBins: Int = 256,
		 yBins: Int = 256,
		 uniBinToTileBin: (TileIndex, BinIndex) => TileAndBinIndices,
		 calcLinePixels: (BinIndex, BinIndex, PT) => IndexedSeq[(BinIndex, PT)]):
			RDD[TileData[BT]] = {


		// Do reduceByKey to account for duplicate lines at a given level (not
		// really necessary, but might speed things up?)
		// val reduced1 = data.reduceByKey(binDesc.aggregateBins(_, _),
		//                               getNumSplits(consolidationPartitions, data))

		// We need to consolidate (join) both metadata and segment data, so our
		// result has two slots, and each half populates one of them.
		//
		// We need to do this before we actually create tiles, because the
		// tiles should be immutable, once created.
		//
		// For the same reason, we'll have to run the tile analytic when we
		// create the tile, too.
		//
		// First, the segment data half

		// Draw lines (based on endpoint bins), and convert all results from
		// universal bins to tile,bin coords
		// Need flatMap here, else result is an RDD IndexedSeq
		// val reduced2 = reduced1.flatMap(p => {

		val segmentsByTile: RDD[(TileIndex, (Option[(BinIndex, BinIndex, PT)],
		                                     Option[DT]))] =
			data.flatMap(p =>
				{
					val ((lineStart, lineEnd, tile), procType) = p

					RDDLineBinner.universalBinsToTiles(tile,
					                                   calcLinePixels(lineStart, lineEnd, procType),
					                                   uniBinToTileBin).map(tile =>
						(tile, (Some((lineStart, lineEnd, procType)), None))
					)
				}
			)


		// Now, the metadata half (in a way that should take no work if there
		// is no metadata)
		val metaData: Option[RDD[(TileIndex, (Option[(BinIndex, BinIndex, PT)],
		                                      Option[DT]))]] =
			tileMetaData.map(_.map{case (index, metaData) => (index, (None, Some(metaData)))})

		// Get the combination of the two sets, again in a way that does no
		// extra work if there is no metadata
		//
		// Note that we don't do a simple join because we want all entries from
		// the segmentsByTile dataset, whether or not they have a corresponding
		// entry in the metadata dataset
		val toTile =
			if (metaData.isDefined) segmentsByTile union metaData.get
			else segmentsByTile

		// Consolidate segments for each tile index, and any associated metadata,
		// and draw a tile data based on the consolidated results
		val partitions = RDDLineBinner.getNumSplits(consolidationPartitions, segmentsByTile)
		toTile
			.groupByKey(partitions)
			.map(t =>
			{
				val index = t._1
				val tileData = t._2
				val xLimit = index.getXBins()
				val yLimit = index.getYBins()

				// Create and default our tile as an array, first, for efficiency.
				//init 2D array of of type PT
				val binValues = Array.ofDim[PT](xLimit, yLimit)
				val defaultRawValue = binAnalytic.defaultUnprocessedValue
				val defaultCookedValue = binAnalytic.defaultProcessedValue
				for (x <- 0 until xLimit) {
					for (y <- 0 until yLimit) {
						binValues(x)(y) = defaultRawValue
					}
				}

				// Determine proper bin values
				tileData.filter(_._1.isDefined).foreach(p =>
					{
						val segment = p._1.get
						val (lineStart, lineEnd, procValue) = segment
						// get all universal bins in line, discard ones not in current tile,
						// and convert bins to 'regular' tile/bin units
						RDDLineBinner.universalBinsToBins(index,
						                                  calcLinePixels(lineStart, lineEnd, procValue),
						                                  uniBinToTileBin).foreach(b =>
							{
								val (bin, scaledValue) = b
								val x = bin.getX()
								val y = bin.getY()
								binValues(x)(y) =  binAnalytic.aggregate(binValues(x)(y), scaledValue)
							}
						)
					}
				)

				// convert aggregated bin values from type PT to BT, and save tile results
				// Create our tile
				val tile = new DenseTileData[BT](index)

				for (x <- 0 until xLimit) {
					for (y <- 0 until yLimit) {
						val value =
							if (binValues(x)(y) == defaultRawValue) defaultCookedValue
							else binValues(x)(y)
						tile.setBin(x, y, binAnalytic.finish(value))
					}
				}

				// Add in any pre-calculated metadata
				tileData.filter(_._2.isDefined).foreach(p =>
					{
						val analyticValue = p._2.get
						dataAnalytics.map(da => AnalysisDescription.record(analyticValue, da, tile))
					}
				)
				// Calculate and add in any tile-level metadata we've been told
				// to calculate
				tileAnalytics.map(ta =>
					{
						// Figure out the value for this tile
						val analyticValue = ta.convert(tile)
						// Add it into any appropriate accumulators
						ta.accumulate(index, analyticValue)
						// And store it in the tile's metadata
						AnalysisDescription.record(analyticValue, ta, tile)
					}
				)
				tile
			}
		)
	}
}
