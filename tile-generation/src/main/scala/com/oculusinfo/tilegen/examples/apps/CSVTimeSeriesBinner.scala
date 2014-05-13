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


import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}
import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.spark.GeneralSparkConnector
import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.HBaseTileIO
import com.oculusinfo.tilegen.tiling.LocalTileIO
import com.oculusinfo.tilegen.util.PropertiesWrapper
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.tilegen.tiling.TileIO
import com.oculusinfo.tilegen.datasets.CSVDataset
import com.oculusinfo.tilegen.datasets.CSVRecordPropertiesWrapper
import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.tilegen.spark.DoubleMaxAccumulatorParam
import com.oculusinfo.tilegen.spark.DoubleMinAccumulatorParam
import com.oculusinfo.tilegen.tiling.FieldExtractor
import com.oculusinfo.tilegen.tiling.CategoryValueBinDescriptor
import com.oculusinfo.binning.util.Pair
import com.oculusinfo.tilegen.datasets.StaticProcessingStrategy
import com.oculusinfo.tilegen.datasets.CSVDataSource
import com.oculusinfo.tilegen.datasets.CSVRecordParser
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.tilegen.datasets.ProcessingStrategy
import org.apache.spark.streaming.dstream.DStream
import com.oculusinfo.tilegen.tiling.RecordParser
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.spark.storage.StorageLevel
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import com.oculusinfo.tilegen.tiling.IndexScheme


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


class CSVTimeSeriesFieldExtractor (properties: CSVRecordPropertiesWrapper) extends FieldExtractor[List[Double]] {
	var lineCount = 0;
	def getLineCount: Int = {
		val loc = lineCount;
		lineCount = lineCount + 1
		loc
	}
	
	def getValidFieldList: List[String] = List()
	def isValidField (field: String): Boolean = true
	def isConstantField (field: String): Boolean = {
		val getFieldType =
			(field: String) => properties.getString("oculus.binning.parsing."+field+".fieldType",
			                                        "The type of the "+field+" field",
			                                        Some(if ("constant" == field || "zero" == field) "constant"
			                                             else ""))

		val fieldType = getFieldType(field)
		("constant" == fieldType || "zero" == fieldType)
	}

	def getFieldValue (field: String)(record: List[Double]) : Try[Double] =
		if ("count" == field) Try(1.0)
		else if ("zero" == field) Try(0.0)
		else Try(record(properties.fieldIndices(field)))
	
	def getDateFieldValue (field: String)(record: List[Double]): Try[Date] =
		Try(new Date(record(properties.fieldIndices(field)).toLong))

	override def getTilePyramid (xField: String, minX: Double, maxX: Double,
	                             yField: String, minY: Double, maxY: Double): TilePyramid = {
		val projection = properties.getString("oculus.binning.projection",
		                                      "The type of tile pyramid to use",
		                                      Some("EPSG:4326"))
		if ("EPSG:900913" == projection) {
			new WebMercatorTilePyramid()
		} else {
			val autoBounds = properties.getBoolean("oculus.binning.projection.autobounds",
			                                       "Whether to calculate pyramid bounds "+
				                                       "automatically or not",
			                                       Some(true))
			if (autoBounds) {
				new AOITilePyramid(minX, minY, maxX, maxY)
			} else {
				val minXp = properties.getDoubleOption("oculus.binning.projection.minx",
				                                       "The minimum x value to use for the tile pyramid").get
				val maxXp = properties.getDoubleOption("oculus.binning.projection.maxx",
				                                       "The maximum x value to use for the tile pyramid").get
				val minYp = properties.getDoubleOption("oculus.binning.projection.miny",
				                                       "The minimum y value to use for the tile pyramid").get
				val maxYp = properties.getDoubleOption("oculus.binning.projection.maxy",
				                                       "The maximum y value to use for the tile pyramid").get
				new AOITilePyramid(minXp, minYp, maxXp, maxYp)
			}
		}
	}
}


object TimeSeriesDataset {
}

class TimeSeriesDataset (rawProperties: Properties,
                               tileWidth: Int,
                               tileHeight: Int)
		extends Dataset[(Double, Double), List[Double], JavaList[Pair[String, JavaDouble]]] {
	def manifest = implicitly[ClassManifest[Double]]

	private val properties = new CSVRecordPropertiesWrapper(rawProperties)

	private val description = properties.getStringOption("oculus.binning.description",
	                                                     "The description to put in the tile metadata")
	private val xVar = properties.getStringOption("oculus.binning.xField",
	                                              "The field to use for the X axis of tiles produced").get
	private val yVar = properties.getString("oculus.binning.yField",
	                                        "The field to use for the Y axis of tiles produced",
	                                        Some("zero"))
	private val zVar = properties.getString("oculus.binning.valueField",
	                                        "The field to use for the value to tile",
	                                        Some("count"))
	private val dateVar = properties.getStringOption("oculus.binning.dateField",
											"The field to use for category names").get
	
	private val dateVarFormat = properties.getStringOption("oculus.binning.dateFormat",
											"The format that category names should be displayed as").get
	
	private val levels = properties.getStringPropSeq("oculus.binning.levels",
	                                                 "The levels to bin").map(lvlString =>
		{
			lvlString.split(',').map(levelRange =>
				{
					val extrema = levelRange.split('-')

					if (0 == extrema.size) Seq[Int]()
					if (1 == extrema.size) Seq[Int](extrema(0).toInt)
					else Range(extrema(0).toInt, extrema(1).toInt+1).toSeq
				}
			).fold(Seq[Int]())(_ ++ _)
		}
	)

	private val consolidationPartitions =
		properties.getIntOption("oculus.binning.consolidationPartitions",
		                        "The number of partitions into which to consolidate data when done")


	//////////////////////////////////////////////////////////////////////////////
	// Section: Dataset implementation
	//
	def getName = {
		val name = properties.getString("oculus.binning.name",
		                                "The name of the tileset",
		                                Some("unknown"))
		val prefix = properties.getStringOption("oculus.binning.prefix",
		                                        "A prefix to add to the tile pyramid ID")
		val pyramidName = if (prefix.isDefined) prefix.get+"."+name
		else name

		pyramidName+"."+xVar+"."+yVar+(if ("count".equals(zVar)) "" else "."+zVar)
	}

	def getDescription =
		description.getOrElse(
			"Binned "+getName+" data showing "+xVar+" vs. "+yVar)

	def getLevels = levels

	private def getAxisBounds (): (Double, Double, Double, Double) = {
		val coordinates = transformRDD(_.map(_._1))

		// Figure out our axis bounds
		val minXAccum = coordinates.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam)
		val maxXAccum = coordinates.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam)
		val minYAccum = coordinates.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam)
		val maxYAccum = coordinates.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam)

		properties.setDistributedComputation(true)
		coordinates.foreach(p =>
			{
				val (x, y) = p
				minXAccum += x
				maxXAccum += x
				minYAccum += y
				maxYAccum += y
			}
		)
		properties.setDistributedComputation(false)

		val minX = minXAccum.value
		val maxX = maxXAccum.value
		val minY = minYAccum.value
		val maxY = maxYAccum.value

		// Include a fraction of a bin extra in the bounds, so the max goes on the
		// right side of the last tile, rather than forming an extra tile.
		val maxLevel = {
			if (levels.isEmpty) 18
			else levels.map(_.reduce(_ max _)).reduce(_ max _)
		}
		val epsilon = (1.0/(1 << maxLevel))
		val adjustedMaxX = maxX+(maxX-minX)*epsilon/(tileWidth*tileWidth)
		val adjustedMaxY = maxY+(maxY-minY)*epsilon/(tileHeight*tileHeight)
		if (_debug) {
			println(("\n\n\nGot bounds: %.4f to %.4f (%.4f) x, "+
				         "%.4f to %.4f (%.4f) y").format(minX, maxX, adjustedMaxX, minY, maxY, adjustedMaxY))
		}

		(minX, adjustedMaxX, minY, adjustedMaxY)
	}

	private lazy val axisBounds = getAxisBounds()

	def getTilePyramid = {
		val extractor = new CSVTimeSeriesFieldExtractor(properties)
		val autoBounds = properties.getBoolean("oculus.binning.projection.autobounds",
		                                       "If true, calculate tile pyramid bounds automatically; "+
			                                       "if false, use values given by properties",
		                                       Some(true)).get
		val (minX, maxX, minY, maxY) =
			if (autoBounds) {
				axisBounds
			} else {
				(0.0, 0.0, 0.0, 0.0)
			}

		extractor.getTilePyramid("", minX, maxX, "", minY, maxY)
	}

	override def getNumXBins = tileWidth
	override def getNumYBins = tileHeight
	override def getConsolidationPartitions: Option[Int] = consolidationPartitions

	def getCategories: List[String] = {
		println("--- categories are: ")
		categories.foreach(str => println("\t" + str))
		categories
	}
	
	def getBinDescriptor: BinDescriptor[List[Double], JavaList[Pair[String, JavaDouble]]] = {
		new CategoryValueBinDescriptor(getCategories)
	}

	override def isDensityStrip = yVar == "zero"

	abstract class StaticCategoryListProcessingStrategy[IT: ClassManifest, PT: ClassManifest] (sc: SparkContext, cache: Boolean)
			extends ProcessingStrategy[IT, PT] {
		private var res: (List[String], RDD[(IT, PT)]) = null;
		private val catsAndRecords = {
			if (res == null) {
				res = getData
			}
			res
		}
		
		private val rdd = catsAndRecords._2
		val categories = catsAndRecords._1

		/**
		 * Returns a list of categories and the dataset of (lat, lon, category values list),
		 * where category values list is a list of values, one for each category
		 */
		protected def getData: (List[String], RDD[(IT, PT)])
	
		final def process[OUTPUT] (fcn: RDD[(IT, PT)] => OUTPUT,
		                           completionCallback: Option[OUTPUT => Unit] = None): Unit = {
			val result = fcn(rdd)
			completionCallback.map(_(result))
		}
	
		final def transformRDD[OUTPUT_TYPE: ClassManifest]
			(fcn: RDD[(IT, PT)] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
			fcn(rdd)
	
		final def transformDStream[OUTPUT_TYPE: ClassManifest]
			(fcn: RDD[(IT, PT)] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
			throw new Exception("Attempt to call DStream transform on RDD processor")
		
	}

		
	class CSVTimeSeriesStaticProcessingStrategy (sc: SparkContext, cache: Boolean)
			extends StaticCategoryListProcessingStrategy[(Double, Double), List[Double]](sc, cache) {
		
		private def getExpandedValueList(value: Double, index: Int, size: Int): List[Double] = {
			Range(0, size).map(i => {
				if (i == index)
					value
				else
					0.0
			}).toList
		}
		
		protected def getData: (List[String], RDD[((Double, Double), List[Double])]) = {
			val source = new CSVDataSource(properties)
			val parser = new CSVRecordParser(properties)
			val extractor = new CSVTimeSeriesFieldExtractor(properties)

			val localXVar = xVar
			val localYVar = yVar
			val localZVar = zVar
			val localDateVar = dateVar
			val localDateVarFormat = dateVarFormat
			
			val index = 0

			val rawData = source.getData(sc)

			val categoryData = rawData.mapPartitions(iter =>
				// Parse the records from the raw data
				parser.parseRecords(iter, localXVar, localYVar).map(_._2)
			).filter(r =>
				// Filter out unsuccessful parsings
				r.isSuccess
			).map(_.get).mapPartitions(iter => 
				iter.map(t => extractor.getDateFieldValue(localDateVar)(t))
			).filter(record =>
				record.isSuccess
			).map(record => {
				val formatter = new SimpleDateFormat(localDateVarFormat)
				formatter.format(record.get)
			})
			val categories = categoryData.distinct().collect.sortBy(name => {
				val lc = extractor.getLineCount
				if (lc % 100000 == 0)
					println("1: " + extractor.hashCode() + " got line: " + lc)
				
				val formatter = new SimpleDateFormat(localDateVarFormat)
				formatter.parse(name).getTime()
			}).toList
			categories.foreach(str => println("category: " + str))
			
			
			
			val recordData = rawData.mapPartitions(iter =>
				// Parse the records from the raw data
				parser.parseRecords(iter, localXVar, localYVar).map(_._2)
			).filter(r =>
				// Filter out unsuccessful parsings
				r.isSuccess
			).map(_.get).mapPartitions(iter => 
				iter.map(t => (extractor.getFieldValue(localXVar)(t),
				               extractor.getFieldValue(localYVar)(t),
				               extractor.getFieldValue(localZVar)(t),
				               extractor.getDateFieldValue(localDateVar)(t))
			).filter(record =>
				record._1.isSuccess && record._2.isSuccess && record._3.isSuccess && record._4.isSuccess)
			).map(record => {
				val lc = extractor.getLineCount
				if (lc % 100000 == 0)
					println("2: " + extractor.hashCode() + " got line: " + lc)
				val formatter = new SimpleDateFormat(localDateVarFormat)
				val valueList: List[Double] = Range(0, categories.length).map(i => {
					if (i == categories.indexOf(formatter.format(record._4.get)))
						record._3.get
					else
						0.0
				}).toList
				((record._1.get, record._2.get), valueList)
			})

			println("--- finished running the parser getData: " + recordData.count)
			
			recordData.persist(StorageLevel.MEMORY_AND_DISK)
//			recordData.cache
			
//			val locData = recordData.collect
//			val rdd2 = sc.parallelize(locData)
//
//			println("--- finished running the parser getData: " + recordData.count)

			(categories, recordData)
		}
	}

	type STRATEGY_TYPE = ProcessingStrategy[(Double, Double), List[Double]]
	protected var strategy: STRATEGY_TYPE = null
	
	def initialize (sc: SparkContext, cache: Boolean): Unit = {
		val strat = new CSVTimeSeriesStaticProcessingStrategy(sc, cache)
		categories = strat.categories
		initialize(strat)
	}
	
	var categories: List[String] = null
}


object CSVTimeSeriesBinner {
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
			case _ => {
				val extension =
					properties.getString("oculus.tileio.file.extension",
					                     "The extension with which to write tiles",
					                     Some("avro"))
				new LocalTileIO(extension)
			}
		}
	}
	
	
	
	def processDataset[PT: ClassManifest, BT] (dataset: Dataset[(Double, Double), PT, BT], tileIO: TileIO): Unit = {
		val binner = new RDDBinner
		binner.debug = true
		dataset.getLevels.map(levels =>
			{
				println("\n\n");
				
				val procFcn: RDD[((Double, Double), PT)] => Unit = 
					rdd =>
				{
					if (rdd.count > 0) {
						val stime = System.currentTimeMillis()
						println("processing level: " + levels)
						val tiles = binner.processDataByLevel(rdd,
															  new CartesianIndexScheme,
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

						//grab the final processing time and print out some time stats
						val ftime = System.currentTimeMillis()
						val timeInfo = new StringBuilder().append("Levels ").append(levels).append(" for job ").append(dataset.getName).append(" finished\n")
							.append("  Processing time: ").append((ftime - stime)).append("ms\n")
						println(timeInfo)
					} else {
						println("No data to process")
					}
				}
				dataset.process(procFcn, None)
			}
		)
	}

	/**
	 * This function is simply for pulling out the generic params from the DatasetFactory,
	 * so that they can be used as params for other types.
	 */
	def processDatasetGeneric[PT, BT] (dataset: Dataset[(Double, Double), PT, BT], tileIO: TileIO): Unit =
		processDataset(dataset, tileIO)(dataset.binTypeManifest)

		
	def createDataset (sc: SparkContext,
	                   dataDescription: Properties,
	                   cache: Boolean,
	                   tileWidth: Int = 256,
	                   tileHeight: Int = 256): Dataset[(Double, Double), _, _] = {
		val dataset = new TimeSeriesDataset(dataDescription, tileWidth, tileHeight)
		dataset.initialize(sc, cache)
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

			processDatasetGeneric(createDataset(sc, props, true, 1, 1), tileIO)

			val fileEndTime = System.currentTimeMillis()
			println("Finished binning "+args(argIdx)+" in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")
			argIdx = argIdx + 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
	}
}
