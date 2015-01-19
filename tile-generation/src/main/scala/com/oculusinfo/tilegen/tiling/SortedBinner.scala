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



import java.io.FileInputStream
import java.util.Properties

import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.TileData

import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.PropertiesWrapper



/**
 * This class takes a data set that is pre-sorted in Z-curve order, and 
 * transforms it into a pyramid of tiles
 */
class SortedBinner {
	var debug: Boolean = true


	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles on the given levels.
	 *
	 * @param data The data to be processed
	 * @param binAnalytic A description of how raw values are aggregated into 
	 *                    bin values
	 * @param tileAnalytics A description of analytics that can be run on each
	 *                      tile, and how to aggregate them
	 * @param dataAnalytics A description of analytics that can be run on the
	 *                      raw data, and recorded (in the aggregate) on each
	 *                      tile.
	 * @param tileScheme A description of how raw values are transformed to bin
	 *                   coordinates
	 * @param levels A list of levels on which to create tiles
	 * @param xBins The number of bins along the horizontal axis of each tile
	 * @param yBins The number of bins along the vertical axis of each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 */
	def processDataByLevel[IT: ClassTag, PT: ClassTag, DT: ClassTag, AT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 indexScheme: IndexScheme[IT],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 tileScheme: TilePyramid,
		 levels: Seq[Int],
		 xBins: Int = 256,
		 yBins: Int = 256,
		 consolidationPartitions: Option[Int] = None):
			RDD[TileData[BT]] =
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
		            mapOverLevels, xBins, yBins, consolidationPartitions)
	}



	/**
	 * Process a simplified input dataset minimally - transform an RDD of raw,
	 * but minimal, data into an RDD of tiles.
	 * 
	 * @param data The data to be processed
	 * @param binDesc A description of how raw values are to be aggregated into
	 *                bin values
	 * @param indexToTiles A function that spreads a data point out over the
	 *                     tiles and bins of interest
	 * @param levels A list of levels on which to create tiles
	 * @param xBins The number of bins along the horizontal axis of each tile
	 * @param yBins The number of bins along the vertical axis of each tile
	 * @param consolidationPartitions The number of partitions to use when
	 *                                grouping values in the same bin or the same
	 *                                tile.  None to use the default determined
	 *                                by Spark.
	 */
	def processData[IT: ClassTag, PT: ClassTag, AT: ClassTag, DT: ClassTag, BT]
		(data: RDD[(IT, PT, Option[DT])],
		 binAnalytic: BinningAnalytic[PT, BT],
		 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
		 dataAnalytics: Option[AnalysisDescription[_, DT]],
		 indexToTiles: IT => TraversableOnce[(TileIndex, BinIndex)],
		 xBins: Int = 256,
		 yBins: Int = 256,
		 consolidationPartitions: Option[Int] = None):
			RDD[TileData[BT]] =
	{
		// Combine two tiles, assuming they have the same tile index
		val combineTiles: (TileData[PT], TileData[PT]) => TileData[PT] = (a, b) => {
			val xLimit = a.getDefinition().getXBins()
			val yLimit = a.getDefinition().getYBins()

			val c = new TileData[PT](a.getDefinition())
			for (x <- 0 until xLimit) {
				for (y <- 0 until yLimit) {
					c.setBin(x, y, binAnalytic.aggregate(a.getBin(x, y),
					                                     b.getBin(x, y)))
				}
			}
			c
		}

		val metaData = processMetaData(data, indexToTiles, dataAnalytics)

		// Go through each partition, transforming it directly to tiles of the
		// processing type
		val tiledByPartition = data.mapPartitions(iter =>
			{
				val partitionResults = MutableMap[TileIndex, TileData[PT]]()

				iter.foreach(record =>
					{
						indexToTiles(record._1).foreach(tb =>
							{
								val tileIndex = tb._1
								val bin = tb._2
								val value = record._2
								val defaultRawBin = binAnalytic.defaultUnprocessedValue

								if (!partitionResults.contains(tileIndex)) {
									// New tile - make a new, blank tile slate
									// with which to start
									val xLimit = tileIndex.getXBins()
									val yLimit = tileIndex.getYBins()
									val tile = new TileData[PT](tileIndex)
									for (x <- 0 until xLimit) {
										for (y <- 0 until yLimit) {
											tile.setBin(x, y, defaultRawBin)
										}
									}
									partitionResults(tileIndex) = tile
								}

								val tile = partitionResults(tileIndex)
								val oldBinValue = tile.getBin(bin.getX(), bin.getY())
								val newBinValue = binAnalytic.aggregate(oldBinValue, value)
								tile.setBin(bin.getX(), bin.getY(), newBinValue)
							}
						)
					}
				)

				partitionResults.iterator
			}
		)

		// Combine any tiles that happen to show up in multiple partitions
		val partitions = consolidationPartitions.getOrElse(tiledByPartition.partitions.size)
		val processTypeTiles = tiledByPartition.reduceByKey(combineTiles(_, _))

		// In our last step, we need to do several things simultaneously, so that the
		// TileData object is treated as immutable.
		//   (1) We need to combine metadata with the proper tile
		//   (2) We need to transform to our proper bin type
		//   (3) We need to add in any tile analytics
		//
		// The first, because it requires a join with another RDD, must come first - we
		// need to transform each RDD into a common form, and then essentially joni the
		// two.  It's not precisely a join, because a join only joins keys which have
		// values in each input set, whereas we want every value in the tile set,
		// associated with values (if they exist) in the metadata set.
		//
		// cf stands for 'common form'
		val cfTiles: RDD[(TileIndex, (Option[TileData[PT]],
		                              Option[Map[String, Any]]))] =
			processTypeTiles.map{case (index, tile) => (index, (Some(tile), None))}
		val cfMetaData: Option[RDD[(TileIndex, (Option[TileData[PT]],
		                                        Option[Map[String, Any]]))]] =
			metaData.map(_.map{case (index, datum) => (index, (None, Some(datum)))})

		val tiles =
			if (cfMetaData.isDefined)
				// Just take the simple union
				(cfTiles union cfMetaData.get).groupByKey(partitions)
			else cfTiles.map{case (key, value) => (key, Seq(value))}

		// Now that we've consolidated our data into a single RDD< execute our
		// needed three steps to create a final tile.
		tiles.map(t =>
			{
				val index = t._1
				val tileData = t._2
				val xLimit = index.getXBins()
				val yLimit = index.getYBins()
				val defaultRawBin = binAnalytic.defaultUnprocessedValue
				val defaultCookedBin = binAnalytic.defaultProcessedValue

				val output = new TileData[BT](index)

				val withTiles = tileData.filter(_._1.isDefined)
				if (withTiles.isEmpty) {
					val defaultBin = binAnalytic.finish(defaultCookedBin)
					// No tile data; default the whole tile
					for (x <- 0 until xLimit; y <- 0 until yLimit) {
						output.setBin(x, y, defaultBin)
					}
				} else {
					// Copy in our tile data
					withTiles.foreach(p =>
						{
							val input = p._1.get
							for (x <- 0 until xLimit; y <- 0 until yLimit) {
								var inputBin =input.getBin(x, y)
								if (inputBin == defaultRawBin)
									inputBin = defaultCookedBin
								output.setBin(x, y, binAnalytic.finish(inputBin))
							}
						}
					)
				}

				// Copy in any pre-calculated raw-data analytics
				tileData.filter(_._2.isDefined).foreach(p =>
					{
						val metaData = p._2.get
						metaData.map{
							case (key, value) => output.setMetaData(key, value)
						}
					}
				)

				// Calculate and add in any tile-level metadata we've been told to
				// calculate
				tileAnalytics.map(ta =>
					{
						// Figure out the value for this tile
						val analyticValue = ta.convert(output)
						// Add t into any appropriate accumulators
						ta.accumulate(output.getDefinition(), analyticValue)
						// And store it in the tile's metadata
						ta.analytic.toMap(analyticValue).map{case (key, value) =>
							output.setMetaData(key, value)
						}
					}
				)

				output
			}
		)
	}


	def processMetaData[IT: ClassTag, PT: ClassTag, DT: ClassTag]
		(data: RDD[(IT, PT, Option[DT])],
		 indexToTiles: IT => TraversableOnce[(TileIndex, BinIndex)],
		 dataAnalytics: Option[AnalysisDescription[_, DT]]):
			Option[RDD[(TileIndex, Map[String, Any])]] =
	{
		dataAnalytics.map(da =>
			{
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
			}
		)
	}
}


object SortedBinnerTest {
	def getTileIO(properties: PropertiesWrapper): TileIO = {
		properties.getString("oculus.tileio.type",
		                     "Where to write tiles",
		                     Some("hbase")) match {
			case "hbase" => {
				val quorum = properties.getStringOption("hbase.zookeeper.quorum",
				                                        "The HBase zookeeper quorum").get
				val port = properties.getString("hbase.zookeeper.port",
				                                "The HBase zookeeper port",
				                                Some("2181"))
				val master = properties.getStringOption("hbase.master",
				                                        "The HBase master").get
				new HBaseTileIO(quorum, port, master)
			}
			case _ => {
				val extension =
					properties.getString("oculus.tileio.file.extension",
					                     "The extension with which to write tiles",
					                     Some("avro"))
				new LocalTileIO(extension)
			}
		}
	}
	
	def processDataset[IT: ClassTag,
	                   PT: ClassTag,
	                   DT: ClassTag,
	                   AT: ClassTag,
	                   BT] (dataset: Dataset[IT, PT, DT, AT, BT],
	                        tileIO: TileIO): Unit = {
		val binner = new SortedBinner
		binner.debug = true
		dataset.getLevels.map(levels =>
			{
				val procFcn: RDD[(IT, PT, Option[DT])] => Unit = rdd =>
				{
					val tiles = binner.processDataByLevel(
						rdd,
						dataset.getIndexScheme,
						dataset.getBinningAnalytic,
						dataset.getTileAnalytics,
						dataset.getDataAnalytics,
						dataset.getTilePyramid,
						levels,
						dataset.getNumXBins,
						dataset.getNumYBins,
						dataset.getConsolidationPartitions)
					tileIO.writeTileSet(dataset.getTilePyramid,
					                    dataset.getName,
					                    tiles,
					                    dataset.getTileSerializer,
					                    dataset.getTileAnalytics,
					                    dataset.getDataAnalytics,
					                    dataset.getName,
					                    dataset.getDescription)
				}
				dataset.process(procFcn, None)
			}
		)
	}
	
	/**
	 * This function is simply for pulling out the generic params from the DatasetFactory,
	 * so that they can be used as params for other types.
	 */
	def processDatasetGeneric[IT, PT, DT, AT, BT] (dataset: Dataset[IT, PT, DT, AT, BT],
	                                               tileIO: TileIO): Unit =
		processDataset(dataset, tileIO)(dataset.indexTypeTag,
		                                dataset.binTypeTag,
		                                dataset.dataAnalysisTypeTag,
		                                dataset.tileAnalysisTypeTag)


	def main (args: Array[String]): Unit = {
		if (args.size<1) {
			println("Usage:")
			println("\tCSVBinner [-d default_properties_file] job_properties_file_1 job_properties_file_2 ...")
			System.exit(1)
		}

		// Read default properties
		var argIdx = 0
		var defProps = new Properties()

		while ("-d" == args(argIdx)) {
			argIdx = argIdx + 1
			val stream = new FileInputStream(args(argIdx))
			defProps.load(stream)
			stream.close()
			argIdx = argIdx + 1
		}
		val defaultProperties = new PropertiesWrapper(defProps)
		val connector = defaultProperties.getSparkConnector()
		val sc = connector.createContext(Some("Pyramid Binning"))
		val tileIO = getTileIO(defaultProperties)

		// Run for each real properties file
		val startTime = System.currentTimeMillis()
		while (argIdx < args.size) {
			val fileStartTime = System.currentTimeMillis()
			val props = new Properties(defProps)
			val propStream = new FileInputStream(args(argIdx))
			props.load(propStream)
			propStream.close()

			// If the user hasn't explicitly set us not to cache, cache processed data to make
			// multiple runs more efficient
			if (!props.stringPropertyNames.contains("oculus.binning.caching.processed"))
				props.setProperty("oculus.binning.caching.processed", "true")

			processDatasetGeneric(DatasetFactory.createDataset(sc, props), tileIO)

			val fileEndTime = System.currentTimeMillis()
			println("Finished binning "+args(argIdx)+" in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")

			argIdx = argIdx + 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
	}
}
