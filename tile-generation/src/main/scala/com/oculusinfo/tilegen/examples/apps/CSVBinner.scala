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
import scala.collection.JavaConverters._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.spark.GeneralSparkConnector
import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.HBaseTileIO
import com.oculusinfo.tilegen.tiling.LocalTileIO
import com.oculusinfo.tilegen.util.PropertiesWrapper
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.tilegen.tiling.TileIO
import com.oculusinfo.tilegen.tiling.SqliteTileIO
import scala.collection.immutable.List



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
	def getTileIO(properties: PropertiesWrapper): TileIO = {
		properties.getString("oculus.tileio.type",
		                     "Where to put tiles",
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
			case "sqlite" => {
				val path =
					properties.getString("oculus.tileio.sqlite.path",
					                     "The path to the database",
					                     Some(""))
				new SqliteTileIO(path)
				
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
	
	def processDataset[IT: ClassManifest,
	                   PT: ClassManifest, 
	                   BT] (dataset: Dataset[IT, PT, BT],
	                        tileIO: TileIO): Unit = {
		val binner = new RDDBinner
		binner.debug = true
		dataset.getLevels.map(levels =>
			{
				val procFcn: RDD[(IT, PT)] => Unit =
					rdd =>
				{
					val tiles = binner.processDataByLevel(rdd,
					                                      dataset.getIndexScheme,
					                                      dataset.getBinDescriptor,
					                                      dataset.getTilePyramid,
					                                      levels,
					                                      (dataset.getNumXBins max dataset.getNumYBins),
					                                      dataset.getConsolidationPartitions,
					                                      dataset.isDensityStrip)
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
	def processDatasetGeneric[IT, PT, BT] (dataset: Dataset[IT, PT, BT],
	                                       tileIO: TileIO): Unit =
		processDataset(dataset, tileIO)(dataset.indexTypeManifest, dataset.binTypeManifest)

	
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
		
		val startIdx = argIdx;
		
		val sensors = List(
				"others",
				"android",
				"iphone"
		)

		val connectorProperties = new PropertiesWrapper(defProps)
		val connector = connectorProperties.getSparkConnector()
		val sc = connector.getSparkContext("Pyramid Binning")

		sensors.foreach{sensor =>
			defProps.setProperty("oculus.tileio.sqlite.path", "c:/projects/insight/aperture-tiles/tile-generation/temp/" + sensor + ".db")
			println("--- opening db @: " + defProps.getProperty("oculus.tileio.sqlite.path"));
		
		val defaultProperties = new PropertiesWrapper(defProps)
		val tileIO = getTileIO(defaultProperties)

		val categories = List(
				"2014-03-03",
				"2014-03-04",
				"2014-03-05",
				"2014-03-06",
				"2014-03-07",
				"2014-03-08",
				"2014-03-09",
				"2014-03-10",
				"2014-03-11",
				"2014-03-12",
				"2014-03-13",
				"2014-03-14",
				"2014-03-15",
				"2014-03-16",
				"2014-03-17",
				"2014-03-18",
				"2014-03-19",
				"2014-03-20",
				"2014-03-21",
				"2014-03-22",
				"2014-03-23",
				"2014-03-24",
				"2014-03-25",
				"2014-03-26",
				"2014-03-27",
				"2014-03-28",
				"2014-03-29",
				"2014-03-30",
				"2014-03-31",
				"2014-04-01"
		)
		
		// Run for each real properties file
		val startTime = System.currentTimeMillis()
		argIdx = startIdx;
		while (argIdx < args.size) {
			categories.foreach{category =>
				val fileStartTime = System.currentTimeMillis()
				val props = new Properties(defProps)
				val propStream = new FileInputStream(args(argIdx))
				props.load(propStream)
				propStream.close()
	
				props.setProperty("oculus.binning.name", category)
				props.setProperty("oculus.binning.source.location", props.getProperty("oculus.binning.source.location") + sensor + "/" + sensor + "_" + category + "-00-00-00.csv")
				
				processDatasetGeneric(DatasetFactory.createDataset(sc, props, false, false, true, 128, 128), tileIO)
	
				val fileEndTime = System.currentTimeMillis()
				println("Finished binning "+args(argIdx)+" in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")
			}
			argIdx = argIdx + 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
		}
	}
}
