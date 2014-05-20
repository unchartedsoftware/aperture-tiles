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
import com.oculusinfo.tilegen.datasets.CSVRecordPropertiesWrapper
import com.oculusinfo.tilegen.datasets.CSVIndexExtractor
import org.apache.spark.SparkContext
import com.oculusinfo.tilegen.datasets.CSVDatasetBase
import org.apache.spark.api.java.JavaRDD
import com.oculusinfo.tilegen.datasets.StaticProcessingStrategy
import org.apache.spark.storage.StorageLevel
import com.oculusinfo.tilegen.datasets.CSVRecordParser
import com.oculusinfo.tilegen.datasets.CSVDataSource
import com.oculusinfo.tilegen.datasets.CSVFieldExtractor
import scala.util.Try
import java.util.Date
import com.oculusinfo.tilegen.tiling.IndexScheme
import com.oculusinfo.tilegen.datasets.TimeRangeCSVIndexExtractor
import java.text.SimpleDateFormat
import com.oculusinfo.tilegen.datasets.TimeRangeCartesianIndexExtractor
import com.oculusinfo.tilegen.datasets.TimeRangeCSVIndexExtractor
import java.util.TimeZone
import com.oculusinfo.tilegen.tiling.TileMetaData
import com.oculusinfo.tilegen.tiling.LevelMinMaxAccumulableParam
import org.apache.spark.Accumulable
import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.binning.TilePyramid


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


class CSVTimeRangeProcessingStrategy[IT: ClassManifest] (sc: SparkContext,
                                   cacheRaw: Boolean,
                                   cacheFilterable: Boolean,
                                   cacheProcessed: Boolean,
                                   properties: CSVRecordPropertiesWrapper,
                                   indexer: TimeRangeCSVIndexExtractor[IT])
		extends StaticProcessingStrategy[IT, Double](sc) {
	// This is a weird initialization problem that requires some
	// documentation to explain.
	// What we really want here is for rawData to be initialized in the
	// getData method, below.  However, this method is called from
	// StaticProcessingStrategy.rdd, which is called during the our
	// parent's <init> call - which happens before we event get to this
	// line.  So when we get here, if we assigned rawData directly in
	// getData, this line below, having to assign rawData some value,
	// would overwrite it.
	// We could just say rawData = rawData (that does work, I checked),
	// but that seemed semantically too confusing to abide. So instead,
	// getData sets rawData2, which can the be assigned to rawData before
	// it gets written in its own initialization (since initialization
	// lines are run in order).
	private var rawData: RDD[String] = rawData2
	private var rawData2: RDD[String] = null
	private var timeRanges = List()
	
	def getTimeRanges() = {
		timeRanges
	}

	private lazy val filterableData: RDD[(String, List[Double])] = {
		val localProperties = properties
		val data = rawData.mapPartitions(iter =>
			{
				val parser = new CSVRecordParser(localProperties)
				// Parse the records from the raw data, parsing all fields
				// The funny end syntax tells scala to treat fields as a varargs
				parser.parseRecords(iter, localProperties.fields:_*)
					.filter(_._2.isSuccess).map{case (record, fields) => (record, fields.get)}
			}
		)
		if (cacheFilterable)
			data.persist(StorageLevel.MEMORY_AND_DISK)
		data
	}

	def getRawData = rawData
	def getFilterableData = filterableData

	def getData: RDD[(IT, Double)] = {
		val localProperties = properties
		val localIndexer = indexer

		val pyramidNameFormat = localProperties.getString("oculus.binning.pyramidNameFormat", "The date format of the output pyramids", Some(""))
		
		rawData2 = {
			val source = new CSVDataSource(properties)
			val data = source.getData(sc);
			if (cacheRaw)
				data.persist(StorageLevel.MEMORY_AND_DISK)
			data
		}

		val data = rawData2.mapPartitions(iter =>
			{
				val parser = new CSVRecordParser(localProperties)
				// Determine which fields we need
				val fields = localIndexer.fields

				// Parse the records from the raw data
				parser.parseRecords(iter, fields:_*)
					.map(_._2) // We don't need the original record (in _1)
			}
		).filter(r =>
			// Filter out unsuccessful parsings
			r.isSuccess
		).map(_.get).mapPartitions(iter =>
			{
				val extractor = new CSVFieldExtractor(localProperties)

				iter.map(t =>
					{
						// Determine our index value
						val indexValue = Try(
							{
								val indexFields = localIndexer.fields
								val fieldValues = indexFields.map(field =>
									(field -> extractor.getFieldValue(field)(t))
								).map{case (k, v) => (k, v.get)}.toMap
								localIndexer.calculateIndex(fieldValues)
							}
						)

						// Determine and add in our binnable value
						(indexValue,
						 extractor.getFieldValue("count")(t))
					}
				)
			}
		).filter(record =>
			record._1.isSuccess && record._2.isSuccess
		).map(record =>
			(record._1.get, record._2.get)
		)
		
		if (cacheProcessed)
			data.persist(StorageLevel.MEMORY_AND_DISK)

//		//get the unique time ranges
//		val timeRangeNames = data.map(record => {
//			record._1
//		}).map(localIndexer.timeIndexScheme.extractTime(_)).distinct.map(time => {
//			val dateFormat = new SimpleDateFormat(pyramidNameFormat)
//			dateFormat.format(new Date(Math.round(time)))
//		}).collect()
//		
//		timeRanges ++ timeRangeNames
			
		data
	}
}

trait TimeRangeDataset[IT] {
	def getTimeRangeIndexer: TimeRangeCSVIndexExtractor[IT]
}


/**
 * Handles basic RDD's using a ProcessingStrategy. 
 */
class CSVTimeRangeDataset[IT: ClassManifest] (indexer: TimeRangeCSVIndexExtractor[IT],
                                     properties: CSVRecordPropertiesWrapper,
                                     tileWidth: Int,
                                     tileHeight: Int)
		extends CSVDatasetBase[IT](indexer, properties, tileWidth, tileHeight) with TimeRangeDataset[IT] {
	// Just some Filter type aliases from Queries.scala
	import com.oculusinfo.tilegen.datasets.FilterAware._

	def getTimeRangeIndexer: TimeRangeCSVIndexExtractor[IT] = indexer

	type STRATEGY_TYPE = CSVTimeRangeProcessingStrategy[IT]
	protected var strategy: STRATEGY_TYPE = null

	def getRawData: RDD[String] = strategy.getRawData

	def getRawFilteredData (filterFcn: Filter):	RDD[String] = {
		strategy.getFilterableData
			.filter{ case (record, fields) => filterFcn(fields)}
			.map(_._1)
	}
	def getRawFilteredJavaData (filterFcn: Filter): JavaRDD[String] =
		JavaRDD.fromRDD(getRawFilteredData(filterFcn))

	def getFieldFilterFunction (field: String, min: Double, max: Double): Filter = {
		val localProperties = properties
		new FilterFunction with Serializable {
			def apply (valueList: List[Double]): Boolean = {
				val index = localProperties.fieldIndices(field)
				val value = valueList(index)
				min <= value && value <= max
			}
			override def toString: String = "%s Range[%.4f, %.4f]".format(field, min, max)
		}
	}

	def initialize (sc: SparkContext,
	                cacheRaw: Boolean,
	                cacheFilterable: Boolean,
	                cacheProcessed: Boolean): Unit =
		initialize(new CSVTimeRangeProcessingStrategy[IT](sc, cacheRaw, cacheFilterable, cacheProcessed, properties, indexer))
	
}




object CSVTimeRangeBinner {
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
	
	private def createIndexExtractor(properties: CSVRecordPropertiesWrapper): TimeRangeCSVIndexExtractor[_] = {
			val xVar = properties.getString("oculus.binning.xField",
			                                "The field to use for the X axis of tiles produced",
			                                Some(CSVDatasetBase.ZERO_STR))
			val yVar = properties.getString("oculus.binning.yField",
			                                "The field to use for the Y axis of tiles produced",
			                                Some(CSVDatasetBase.ZERO_STR))
			val timeVar = properties.getString("oculus.binning.timeField",
			                                "The field to use for the time axis of tiles produced",
			                                Some(CSVDatasetBase.ZERO_STR))
			                                
            val startDateFormat = new SimpleDateFormat(properties.getString("oculus.binning.timeRange.dateFormat",
	                                			"The parsing format to use for 'oculus.binning.timeRange.startDate'",
	                                			Some("yyMMddHHmm")))
	                                			
			val startDate = startDateFormat.parse(properties.getString("oculus.binning.timeRange.startDate",
												"The initial date to base the time ranges on.",
												Some(""))).getTime()
												
			val secsPerRange = properties.getDouble("oculus.binning.timeRange.secondsPerRange",
												"The number of seconds each range should represent",
												Some(60 * 60 * 24))
			new TimeRangeCartesianIndexExtractor(timeVar, xVar, yVar, startDate, secsPerRange)
	}
	
	def writeBaseMetaData[BT](tileIO: TileIO,
							pyramider: TilePyramid,
							baseLocation: String,
							minsMaxes: Map[Int, (BT, BT)],
							tileSize: Int,
							name: String,
							description: String): Unit = {
		val metaData = tileIO.combineMetaData(pyramider, baseLocation, minsMaxes, tileSize, name, description)
		tileIO.writeMetaData(baseLocation, metaData)
	}
	
	
	def processDataset[IT: ClassManifest,
	                   PT: ClassManifest, 
	                   BT] (dataset: Dataset[IT, PT, BT] with TimeRangeDataset[IT],
	                        tileIO: TileIO,
	                        properties: CSVRecordPropertiesWrapper): Unit = {
		val binner = new RDDBinner
		binner.debug = true
		dataset.getLevels.map(levels =>
			{
				val localIndexer = dataset.getTimeRangeIndexer
				
				//local function to combine all of the min/max data
				val combineMetaData: Array[(TileMetaData, Map[Int, (BT, BT)])] => Map[Int, (BT, BT)] =
					metadatas =>
				{
					val binDesc = dataset.getBinDescriptor
					
					// Set up some accumulators to figure out needed metadata
					val minMaxAccumulable = new LevelMinMaxAccumulableParam[BT](binDesc.min,
					                                                            binDesc.defaultMin,
					                                                            binDesc.max,
					                                                            binDesc.defaultMax)
					val minMaxAccum = new Accumulable(minMaxAccumulable.zero(Map()), minMaxAccumulable)
					
					//merge the metadata together
					metadatas.foreach(metadata => {
						metadata._2.foreach(levelMinMax => {
							minMaxAccum += (levelMinMax._1 -> levelMinMax._2._1)	//add in mapping for level -> min
							minMaxAccum += (levelMinMax._1 -> levelMinMax._2._2)	//add in mapping for level -> max
						})
					})
				
					minMaxAccum.value
				}

				val procFcn: RDD[(IT, PT)] => Unit =
					rdd =>
				{
					//get the unique time ranges
					val timeRanges = rdd.map(record => {
						localIndexer.timeIndexScheme.extractTime(record._1)
					}).distinct.collect()
		
					val rangeMetadatas = timeRanges.map(startTime =>{
						val timeRangeRdd = rdd.filter(record => {
							val curTime = localIndexer.timeIndexScheme.extractTime(record._1)
							curTime >= startTime && curTime < (startTime + localIndexer.msPerTimeRange)
						})
					
						val dateFormatString = properties.getString("oculus.binning.pyramidNameFormat", "The output name format to use for each pyramid", Some(""))
						val dateFormat = new SimpleDateFormat(dateFormatString)
						dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
						val name = dateFormat.format(new Date(Math.round(startTime)))

						
						val tiles = binner.processDataByLevel(timeRangeRdd,
						                                      dataset.getIndexScheme,
						                                      dataset.getBinDescriptor,
						                                      dataset.getTilePyramid,
						                                      levels,
						                                      (dataset.getNumXBins max dataset.getNumYBins),
						                                      dataset.getConsolidationPartitions,
						                                      dataset.isDensityStrip)
						val levelMinMaxes = tileIO.writeTileSet(dataset.getTilePyramid,
						                    name,
						                    tiles,
						                    dataset.getBinDescriptor,
						                    name,
						                    dataset.getDescription)
						(tileIO.readMetaData(name).get, levelMinMaxes)
					})
					
					writeBaseMetaData(tileIO,
								dataset.getTilePyramid,
								dataset.getName,
								combineMetaData(rangeMetadatas),
								dataset.getNumXBins,
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
	def processDatasetGeneric[IT, PT, BT] (dataset: Dataset[IT, PT, BT] with TimeRangeDataset[IT],
	                                       tileIO: TileIO,
	                                       properties: CSVRecordPropertiesWrapper): Unit =
		processDataset(dataset, tileIO, properties)(dataset.indexTypeManifest, dataset.binTypeManifest)

		
	private def getDataset[T: ClassManifest] (indexer: TimeRangeCSVIndexExtractor[T],
	                                          properties: CSVRecordPropertiesWrapper,
	                                          tileWidth: Int,
	                                          tileHeight: Int): CSVTimeRangeDataset[T] =
		new CSVTimeRangeDataset(indexer, properties, tileWidth, tileHeight)

	private def getDatasetGeneric[T] (indexer: TimeRangeCSVIndexExtractor[T],
	                                  properties: CSVRecordPropertiesWrapper,
	                                  tileWidth: Int,
	                                  tileHeight: Int): CSVTimeRangeDataset[T] =
		getDataset(indexer, properties, tileWidth, tileHeight)(indexer.indexTypeManifest)

	def getTypedDataset[IT]: Dataset[IT, _, _] with TimeRangeDataset[IT] = {
		return null
	}
		
	def createDataset (sc: SparkContext,
	                   properties: CSVRecordPropertiesWrapper,
	                   cacheRaw: Boolean,
	                   cacheFilterable: Boolean,
	                   cacheProcessed: Boolean
                   ): CSVTimeRangeDataset[_] = {
		// Wrap parameters more usefully
		val tileWidth = properties.getInt("oculus.binning.tileWidth", "The number of bins wide in each tile", Some(256))
		val tileHeight = properties.getInt("oculus.binning.tileHeight", "The number of bins high in each tile", Some(256))
			
		// Determine indexing information
		val indexer = createIndexExtractor(properties)

		val dataset:CSVTimeRangeDataset[_] = getDatasetGeneric(indexer, properties, tileWidth, tileHeight)
		dataset.initialize(sc, cacheRaw, cacheFilterable, cacheProcessed)
		dataset
	}
		
	
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
		
		val connectorProperties = new PropertiesWrapper(defProps)
		val connector = connectorProperties.getSparkConnector()
		val sc = connector.getSparkContext("Pyramid Binning")

		val defaultProperties = new PropertiesWrapper(defProps)
		val tileIO = getTileIO(defaultProperties)

		// Run for each real properties file
		val startTime = System.currentTimeMillis()
		while (argIdx < args.size) {
			val fileStartTime = System.currentTimeMillis()
			val props = new Properties(defProps)
			val propStream = new FileInputStream(args(argIdx))
			props.load(propStream)
			propStream.close()

			val properties = new CSVRecordPropertiesWrapper(props)
			processDatasetGeneric(createDataset(sc, properties, false, false, true), tileIO, properties)

			argIdx += 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
	}
}
