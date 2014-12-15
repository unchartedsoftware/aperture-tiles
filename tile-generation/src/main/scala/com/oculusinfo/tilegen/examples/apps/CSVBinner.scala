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

package com.oculusinfo.tilegen.examples.apps



import java.io.FileInputStream
import java.util.Properties

import com.oculusinfo.tilegen.datasets.{Dataset, DatasetFactory}
import com.oculusinfo.tilegen.tiling.{RDDBinner, TileIO}
import com.oculusinfo.tilegen.util.PropertiesWrapper
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag



/*
 * The following properties control how the application runs:
 * 
 *  hbase.zookeeper.quorum
 *      If tiles are written to hbase, the zookeeper quorum location needed to
 *      connect to hbase.
 * 
 *  hbase.zookeeper.port
 *      If tiles are written to hbase, the port through which to connect to
 *      zookeeper.  Defaults to 2181
 * 
 *  hbase.master
 *      If tiles are written to hbase, the location of the hbase master to
 *      which to write them
 *
 * 
 *  spark
 *      The location of the spark master.
 *      Defaults to "localhost"
 *
 *  sparkhome
 *      The file system location of Spark in the remote location (and,
 *      necessarily, on the local machine too)
 *      Defaults to "/srv/software/spark-0.7.2"
 * 
 *  user
 *      A user name to stick in the job title so people know who is running the
 *      job
 *
 *
 * 
 *  oculus.tileio.type
 *      The way in which tiles are written - either hbase (to write to hbase,
 *      see hbase. properties above to specify where) or file  to write to the
 *      local file system
 *      Default is hbase
 *
 */








object CSVBinner {

	def processDataset[IT: ClassTag,
	                   PT: ClassTag,
	                   DT: ClassTag,
	                   AT: ClassTag,
	                   BT] (sc: SparkContext,
	                        dataset: Dataset[IT, PT, DT, AT, BT],
	                        tileIO: TileIO): Unit = {
		val binner = new RDDBinner
		binner.debug = true

		val tileAnalytics = dataset.getTileAnalytics
		val dataAnalytics = dataset.getDataAnalytics

		println("Tiling dataset "+dataset.getName)
		println("\tTile analytics: "+tileAnalytics)
		println("\tData analytics: "+dataAnalytics)

		tileAnalytics.map(_.addGlobalAccumulator(sc))
		dataAnalytics.map(_.addGlobalAccumulator(sc))

		dataset.getLevels.map(levels =>
			{
				println("\tProcessing levels "+levels)

				// Add level accumulators for all analytics for these levels (for now at least)
				tileAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)
				dataAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)

				val procFcn: RDD[(IT, PT, Option[DT])] => Unit =
					rdd =>
				{
					val tiles = binner.processDataByLevel(rdd,
					                                      dataset.getIndexScheme,
					                                      dataset.getBinningAnalytic,
					                                      tileAnalytics,
					                                      dataAnalytics,
					                                      dataset.getTilePyramid,
					                                      levels,
					                                      dataset.getNumXBins,
					                                      dataset.getNumYBins,
					                                      dataset.getConsolidationPartitions)

					tileIO.writeTileSet(dataset.getTilePyramid,
					                    dataset.getName,
					                    tiles,
					                    dataset.getTileSerializer,
					                    tileAnalytics, dataAnalytics,
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
	def processDatasetGeneric[IT, PT, DT, AT, BT] (sc: SparkContext,
	                                               dataset: Dataset[IT, PT, DT, AT, BT],
	                                               tileIO: TileIO): Unit =
		processDataset(sc, dataset, tileIO)(dataset.indexTypeTag,
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
		val tileIO = TileIO.fromArguments(defaultProperties)

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
			processDatasetGeneric(sc, DatasetFactory.createDataset(sc, props), tileIO)

			val fileEndTime = System.currentTimeMillis()
			println("Finished binning "+args(argIdx)+" in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")

			argIdx = argIdx + 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
	}
}
