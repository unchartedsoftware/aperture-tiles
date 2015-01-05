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

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import scala.reflect.ClassTag
import scala.util.Try

import org.apache.spark.Accumulable
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.tilegen.datasets.CSVDatasetBase
import com.oculusinfo.tilegen.datasets.CSVDataSource
import com.oculusinfo.tilegen.datasets.CSVFieldExtractor
import com.oculusinfo.tilegen.datasets.CSVIndexExtractor
import com.oculusinfo.tilegen.datasets.CSVRecordParser
import com.oculusinfo.tilegen.datasets.CSVRecordPropertiesWrapper
import com.oculusinfo.tilegen.datasets.CSVDataset
import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.datasets.StaticProcessingStrategy
import com.oculusinfo.tilegen.datasets.TimeRangeCartesianIndexExtractor
import com.oculusinfo.tilegen.datasets.TimeRangeCSVIndexExtractor
import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import com.oculusinfo.tilegen.tiling.HBaseTileIO
import com.oculusinfo.tilegen.tiling.IndexScheme
import com.oculusinfo.tilegen.tiling.LocalTileIO
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.SqliteTileIO
import com.oculusinfo.tilegen.tiling.TileIO
import com.oculusinfo.tilegen.tiling.TimeIndexScheme
import com.oculusinfo.tilegen.util.PropertiesWrapper


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
	
	
	def processDataset[IT: ClassTag,
	                   PT: ClassTag,
	                   DT: ClassTag,
	                   AT: ClassTag,
	                   BT] (sc: SparkContext,
	                        dataset: Dataset[IT, PT, DT, AT, BT],
	                        tileIO: TileIO,
	                        properties: CSVRecordPropertiesWrapper): Unit = {
		val binner = new RDDBinner
		binner.debug = true

		val tileAnalytics = dataset.getTileAnalytics
		val dataAnalytics = dataset.getDataAnalytics

		tileAnalytics.map(_.addGlobalAccumulator(sc))
		dataAnalytics.map(_.addGlobalAccumulator(sc))

		dataset.getLevels.map(levels =>
			{
				// Add level accumulators for all analytics for these levels (for now at least)
				tileAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)
				dataAnalytics.map(analytic =>
					levels.map(level => analytic.addLevelAccumulator(sc, level))
				)

				val localIndexer: TimeRangeCSVIndexExtractor[IT] =
					dataset
						.asInstanceOf[CSVDataset[IT, PT, DT, AT, BT]]
						.getIndexer
						.asInstanceOf[TimeRangeCSVIndexExtractor[IT]]

				val procFcn: RDD[(IT, PT, Option[DT])] => Unit = rdd =>
				{
					// get the unique time ranges
					val timeRanges = rdd.map(record =>
						{
							val scheme: TimeIndexScheme[IT] = localIndexer.timeIndexScheme
							scheme.extractTime(record._1)
						}
					).distinct.collect()

					val levelRange = timeRanges.map(startTime =>
						{
							val timeRangeRdd = rdd.filter(record =>
								{
									val curTime =
										localIndexer.timeIndexScheme.extractTime(record._1)
									(curTime >= startTime &&
										 curTime < (startTime + localIndexer.msPerTimeRange))
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
							                                      dataset.getIndexScheme,
							                                      dataset.getBinningAnalytic,
							                                      tileAnalytics,
							                                      dataAnalytics,
							                                      dataset.getTilePyramid,
							                                      levels,
							                                      dataset.getNumXBins,
							                                      dataset.getNumYBins,
							                                      dataset.getConsolidationPartitions)
							// TODO: This doesn't actually write the tiles, does it?
							// I think we need to write them to do anyting.
							val rangeMD = tileIO.readMetaData(name).get
							rangeMD.getValidZoomLevels.asScala.map(_.intValue).toSet
						}
					).reduce(_ ++ _)
					
					writeBaseMetaData(tileIO,
					                  dataset.getTilePyramid,
					                  dataset.getName,
					                  levelRange,
					                  tileAnalytics,
					                  dataAnalytics,
					                  dataset.getNumXBins,
					                  dataset.getNumYBins,
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
	                                               tileIO: TileIO,
	                                               properties: CSVRecordPropertiesWrapper):
			Unit =
		processDataset(sc, dataset, tileIO, properties)(dataset.indexTypeTag,
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
		defProps.setProperty("oculus.binning.index.type", "timerange")
		
		val startIdx = argIdx;
		
		val connectorProperties = new PropertiesWrapper(defProps)
		val connector = connectorProperties.getSparkConnector()
		val sc = connector.createContext(Some("Pyramid Binning"))

		val defaultProperties = new PropertiesWrapper(defProps)
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

			processDatasetGeneric(sc, DatasetFactory.createDataset(sc, props),
			                      tileIO, new CSVRecordPropertiesWrapper(props))
			argIdx += 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
	}
}
