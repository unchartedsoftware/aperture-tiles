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

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.collection.mutable.{Map => MutableMap}

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.DensityStripData
import com.oculusinfo.binning.TileData

import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.spark.GeneralSparkConnector
import com.oculusinfo.tilegen.spark.SparkConnector
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
	 */
	def processDataByLevel[PT: ClassManifest, BT] (data: RDD[(Double, Double, PT)],
	                                               binDesc: BinDescriptor[PT, BT],
	                                               tileScheme: TilePyramid,
	                                               levels: Seq[Int],
	                                               bins: Int = 256,
	                                               consolidationPartitions: Option[Int] = None,
	                                               isDensityStrip: Boolean = false):
			RDD[TileData[BT]] = {
		val mapOverLevels: (Double, Double, PT) => TraversableOnce[((TileIndex, BinIndex), PT)] =
			(x, y, value) => {
				levels.map(level =>
					{
						val tile = tileScheme.rootToTile(x, y, level, bins)
						val bin = tileScheme.rootToBin(x, y, tile)
						((tile, bin), value)
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
	 */
	def processData[PT: ClassManifest, BT] (data: RDD[(Double, Double, PT)],
	                                        binDesc: BinDescriptor[PT, BT],
	                                        datumToTiles: (Double, Double, PT) => TraversableOnce[((TileIndex, BinIndex), PT)],
	                                        bins: Int = 256,
	                                        consolidationPartitions: Option[Int] = None,
	                                        isDensityStrip: Boolean = false):
			RDD[TileData[BT]] = {
		// Combine two tiles, assuming they have the same tile index
		val combineTiles: (TileData[PT], TileData[PT]) => TileData[PT] = (a, b) => {
			val xLimit = a.getDefinition().getXBins()
			val yLimit = a.getDefinition().getYBins()

			val c = new TileData[PT](a.getDefinition())
			for (x <- 0 until xLimit) {
				for (y <- 0 until yLimit) {
					c.setBin(x, y, binDesc.aggregateBins(a.getBin(x, y),
					                                     b.getBin(x, y)))
				}
			}
			c
		}

		val convertAndClean: TileData[PT] => TileData[BT] = input => {
			val index = input.getDefinition()
			val xLimit = index.getXBins()
			val yLimit = index.getYBins()
			val defaultRawBin = binDesc.defaultUnprocessedBinValue
			val defaultCookedBin = binDesc.defaultProcessedBinValue

			val output = new TileData[BT](index)
			for (x <- 0 until xLimit) {
				for (y <- 0 until yLimit) {
					var inputBin = input.getBin(x, y)
					if (inputBin == defaultRawBin)
						inputBin = defaultCookedBin
					output.setBin(x, y, binDesc.convert(inputBin))
				}
			}
			output
		}


		// Go through each partition, transforming it directly to tiles of the
		// processing type
		val tiledByPartition = data.mapPartitions(iter =>
			{
				val partitionResults = MutableMap[TileIndex, TileData[PT]]()

				iter.foreach(record =>
					{
						datumToTiles(record._1, record._2, record._3).foreach(tbv =>
							{
								val tileIndex = tbv._1._1
								val bin = tbv._1._2
								val value = tbv._2
								val defaultRawBin = binDesc.defaultUnprocessedBinValue

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
								val newBinValue = binDesc.aggregateBins(oldBinValue, value)
								tile.setBin(bin.getX(), bin.getY(), newBinValue)
							}
						)
					}
				)

				partitionResults.iterator
			}
		)

		if (consolidationPartitions.isEmpty) {
			// Combine any tiles that happen to show up in multiple partitions
			tiledByPartition.reduceByKey(combineTiles(_, _))
			// Transform to our bin type, and put in the proper zeros
				.map(indexAndTile => convertAndClean(indexAndTile._2))
		} else {
			// Combine any tiles that happen to show up in multiple partitions
			tiledByPartition.reduceByKey(combineTiles(_, _), consolidationPartitions.get)
			// Transform to our bin type, and put in the proper zeros
				.map(indexAndTile => convertAndClean(indexAndTile._2))
		}
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
	
	def processDataset[BT: ClassManifest, PT] (dataset: Dataset[BT, PT], tileIO: TileIO): Unit = {
		val binner = new SortedBinner
		binner.debug = true
		dataset.getLevels.map(levels =>
			{
				val procFcn: RDD[(Double, Double, BT)] => Unit = rdd =>
				{
					val bins = (dataset.getNumXBins max dataset.getNumYBins)
					val tiles = binner.processDataByLevel(rdd,
					                                      dataset.getBinDescriptor,
					                                      dataset.getTilePyramid,
					                                      levels,
					                                      bins,
					                                      dataset.getConsolidationPartitions)
					tileIO.writeTileSet(dataset.getTilePyramid,
					                    dataset.getName,
					                    tiles,
					                    dataset.getBinDescriptor,
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
	def processDatasetGeneric[BT, PT] (dataset: Dataset[BT, PT], tileIO: TileIO): Unit =
		processDataset(dataset, tileIO)(dataset.binTypeManifest)


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
		val sc = connector.getSparkContext("Pyramid Binning")
		val tileIO = getTileIO(defaultProperties)

		// Run for each real properties file
		val startTime = System.currentTimeMillis()
		while (argIdx < args.size) {
			val fileStartTime = System.currentTimeMillis()
			val props = new Properties(defProps)
			val propStream = new FileInputStream(args(argIdx))
			props.load(propStream)
			propStream.close()

			processDatasetGeneric(DatasetFactory.createDataset(sc, props, true), tileIO)

			val fileEndTime = System.currentTimeMillis()
			println("Finished binning "+args(argIdx)+" in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")

			argIdx = argIdx + 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
	}
}
