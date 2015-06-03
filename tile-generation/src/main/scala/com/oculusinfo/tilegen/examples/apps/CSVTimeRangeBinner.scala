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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.TimeZone

import com.oculusinfo.tilegen.datasets.{CSVReader, CSVDataSource, TimeRangeCartesianIndexExtractor, TilingTask}
import org.apache.spark.sql.SQLContext

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.UniversalBinner
import com.oculusinfo.tilegen.tiling.TileIO
import com.oculusinfo.tilegen.util.{KeyValueArgumentSource, PropertiesWrapper}


/**
 * The CSVTimeRangeBinner is meant to read in a csv file and parse it similarily to
 * the CSVBinner, however this one is capable of using a date column to split the
 * produced pyramids into a set of pyramids that represent discrete time ranges.
 */

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
 *      local file system, or sqlite to write to a simple database.
 *      Default is hbase
 *
 *	oculus.tileio.sqlite.path
 * 		If 'oculus.tileio.type=sqlite' then the database can be found using this
 *   	file location.
 *
 *	oculus.binning.pyramidNameFormat
 * 		Each pyramid will use have a name that represents its date. The name format
 *   	is a SimpleDateFormat string that will transform the pyramid's date.
 *
 *	oculus.binning.timeField
 * 		Specifies which field the indexer should use to find the time field data.
 *   	eg.
 *    	if:		oculus.binning.timeField=time
 *   	then:	oculus.binning.parsing.time.index=0
 *     			oculus.binning.parsing.time.fieldType=date
 *       		oculus.binning.parsing.time.dateFormat=yyyy-MM-dd HH:mm:ss
 *
 *
 * 	oculus.binning.timeRange.dateFormat
 *  	The SimpleDateFormat string to interpret oculus.binning.timeRange.startDate
 *
 * 	oculus.binning.timeRange.startDate
 *  	This is the earliest date that should be considered valid data from the source.
 *   	Any data found prior to the startDate will be dropped.
 *
 *  oculus.binning.timeRange.secondsPerRange
 *  	The number of seconds in each time range. Each time range will be a multiple of
 *   	secondsPerRange seconds after oculus.binning.timeRange.startDate.
 *
 *  oculus.binning.tileWidth
 *  oculus.binning.tileHeight
 *  	Specifies the tile width and height. Default is 256.
 */


object CSVTimeRangeBinner {
	def writeBaseMetaData[BT, AT, DT](tileIO: TileIO,
	                                  pyramider: TilePyramid,
	                                  baseLocation: String,
	                                  levels: Set[Int],
	                                  tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
	                                  dataAnalytics: Option[AnalysisDescription[_, DT]],
	                                  tileSizeX: Int,
	                                  tileSizeY: Int,
	                                  name: String,
	                                  description: String): Unit = {
		val metaData = tileIO.combineMetaData(pyramider,
		                                      baseLocation,
		                                      levels,
		                                      tileAnalytics,
		                                      dataAnalytics,
		                                      tileSizeX,
		                                      tileSizeY,
		                                      name,
		                                      description)
		tileIO.writeMetaData(baseLocation, metaData)
	}


	def processTask[PT: ClassTag,
	                DT: ClassTag,
	                AT: ClassTag,
	                BT] (sc: SparkContext,
	                     task: TilingTask[PT, DT, AT, BT],
	                     tileIO: TileIO,
	                     properties: KeyValueArgumentSource): Unit = {
		val binner = new UniversalBinner

		val tileAnalytics = task.getTileAnalytics
		val dataAnalytics = task.getDataAnalytics

		tileAnalytics.map(_.addGlobalAccumulator(sc))
		dataAnalytics.map(_.addGlobalAccumulator(sc))

		task.getLevels.map(levels =>
			{
				// Add level accumulators for all analytics for these levels (for now at least)
				tileAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)
				dataAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)

				val localIndexer: TimeRangeCartesianIndexExtractor =
					task
						.asInstanceOf[TilingTask[PT, AT, DT, BT]]
						.getIndexer
						.asInstanceOf[TimeRangeCartesianIndexExtractor]

				val procFcn: RDD[(Seq[Any], PT, Option[DT])] => Unit = rdd =>
				{
					// get the unique time ranges
					val timeRanges = rdd.map(record =>
						{
							val scheme = localIndexer.indexScheme
							scheme.extractTime(record._1)
						}
					).distinct.collect()

					val levelRange = timeRanges.map(startTime =>
						{
							val timeRangeRdd = rdd.filter(record =>
								{
									val curTime =
										localIndexer.indexScheme.extractTime(record._1)
									(curTime >= startTime &&
										 curTime < (startTime + localIndexer.indexScheme.msPerTimeRange))
								}
							)

							val dateFormatString =
								properties.getString("oculus.binning.pyramidNameFormat",
								                     "The output name format to use for "+
									                     "each pyramid",
								                     Some(""))
							val dateFormat = new SimpleDateFormat(dateFormatString)
							dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
							val name = dateFormat.format(new Date(Math.round(startTime)))


							val tiles = binner.processDataByLevel(timeRangeRdd,
							                                      task.getIndexScheme,
							                                      task.getBinningAnalytic,
							                                      tileAnalytics,
							                                      dataAnalytics,
							                                      task.getTilePyramid,
							                                      levels,
							                                      task.getNumXBins,
							                                      task.getNumYBins,
							                                      task.getConsolidationPartitions,
							                                      task.getTileType)
							// TODO: This doesn't actually write the tiles, does it?
							// I think we need to write them to do anyting.
							val rangeMD = tileIO.readMetaData(name).get
							rangeMD.getValidZoomLevels.asScala.map(_.intValue).toSet
						}
					).reduce(_ ++ _)

					writeBaseMetaData(tileIO,
					                  task.getTilePyramid,
					                  task.getName,
					                  levelRange,
					                  tileAnalytics,
					                  dataAnalytics,
					                  task.getNumXBins,
					                  task.getNumYBins,
					                  task.getName,
					                  task.getDescription)
				}
				task.process(procFcn, None)
			}
		)
	}

	/**
	 * This function is simply for pulling out the generic params from the TilingTask
	 * so that they can be used as params for other types.
	 */
	def processTaskGeneric[PT, DT, AT, BT] (sc: SparkContext,
	                                        task: TilingTask[PT, DT, AT, BT],
	                                        tileIO: TileIO,
	                                        properties: KeyValueArgumentSource):
			Unit =
		processTask(sc, task, tileIO, properties)(task.binTypeTag,
		                                          task.dataAnalysisTypeTag,
		                                          task.tileAnalysisTypeTag)



	private def readFile (file: String, props: Properties): Properties = {
		val stream = new FileInputStream(file)
		props.load(stream)
		stream.close()
		props
	}



	def main (args: Array[String]): Unit = {
		if (args.size<1) {
			println("Usage:")
			println("\tCSVTimeRangeBinner [-d default_properties_file] job_properties_file_1 job_properties_file_2 ...")
			System.exit(1)
		}

		// Read default properties
		var argIdx = 0
		var defProps = new Properties()

		while ("-d" == args(argIdx)) {
			argIdx = argIdx + 1
			readFile(args(argIdx), defProps)
			argIdx = argIdx + 1
		}
		defProps.setProperty("oculus.binning.index.type", "timerange")

		val startIdx = argIdx;

		val connectorProperties = new PropertiesWrapper(defProps)
		val connector = connectorProperties.getSparkConnector()
		val sc = connector.createContext(Some("Pyramid Binning"))
		val sqlc = new SQLContext(sc)

		val defaultProperties = new PropertiesWrapper(defProps)
		val tileIO = TileIO.fromArguments(defaultProperties)

		// Run for each real properties file
		val startTime = System.currentTimeMillis()
		while (argIdx < args.size) {
			val fileStartTime = System.currentTimeMillis()
			val rawProps = readFile(args(argIdx), new Properties(defProps))
			val props = new PropertiesWrapper(rawProps)

			// Read our CSV data
			val source = new CSVDataSource(props)
			// Read the CSV into a schema file
			val reader = new CSVReader(sqlc, source.getData(sc), props)
			// Unless the user has specifically said not to, cache processed data so as to make multiple runs
			// more efficient.
			val cache = props.getBoolean(
				"oculus.binning.caching.processed",
				"Cache the data, in a parsed and processed form, if true",
				Some(true))
			// Register it as a table
			val table = "table"+argIdx
			reader.asDataFrame.registerTempTable(table)
			if (cache) sqlc.cacheTable(table)

			// Process the data
			processTaskGeneric(sc, TilingTask(sqlc, table, rawProps), tileIO, props)

			val fileEndTime = System.currentTimeMillis()
			println("Finished binning "+args(argIdx)+" in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")

			argIdx += 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
	}
}
