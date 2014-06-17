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
import scala.reflect.ClassTag
import scala.util.Try

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.storage.StorageLevel

import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.tilegen.datasets.CartesianIndexExtractor
import com.oculusinfo.tilegen.datasets.CSVDataset
import com.oculusinfo.tilegen.datasets.CSVDatasetBase
import com.oculusinfo.tilegen.datasets.CSVDataSource
import com.oculusinfo.tilegen.datasets.CSVFieldExtractor
import com.oculusinfo.tilegen.datasets.CSVIndexExtractor
import com.oculusinfo.tilegen.datasets.CSVRecordPropertiesWrapper
import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.datasets.GraphRecordParser
import com.oculusinfo.tilegen.datasets.LineSegmentIndexExtractor
import com.oculusinfo.tilegen.datasets.StaticProcessingStrategy
import com.oculusinfo.tilegen.spark.GeneralSparkConnector
import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import com.oculusinfo.tilegen.tiling.HBaseTileIO
import com.oculusinfo.tilegen.tiling.LocalTileIO
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.RDDLineBinner
import com.oculusinfo.tilegen.tiling.SqliteTileIO
import com.oculusinfo.tilegen.tiling.TileIO
import com.oculusinfo.tilegen.util.PropertiesWrapper


/**
 * This application handles reading in a graph dataset from a CSV file, and generating
 * tiles for the graph's nodes or edges.
 * 
 * NOTE:  It is expected that the 1st column of the CSV graph data will contain either the keyword "node"
 * for data lines representing a graph's node/vertex, or the keyword "edge" for data lines representing
 * a graph's edge
 * 
 * The following properties control how the application runs: 
 * (See code comments in CSVDataset.scala for more info and settings)
 * 
 * 
 *  oculus.binning.graph.data
 *      The type of graph data to use for tile generation.  Set to "nodes" to generate tiles of a graph's
 *      nodes [default], or set to "edges" to generate tiles of a graph's edges.
 *      
 *  oculus.binning.line.level.threshold
 *  	Level threshold to determine whether to use 'point' vs 'tile' based line segment binning
 *   	Levels above this thres use tile-based binning. Default = 4.
 *    
 *  oculus.binning.line.min.bins
 *  	Min line segment length (in bins) for a given level. Shorter line segments will be discarded.
 *   	Default = 2.
 *   
 *  oculus.binning.line.max.bins
 *  	Max line segment length (in bins) for a given level. Longer line segments will be discarded.
 *   	Default = 1024.                      
 *      
 *  oculus.binning.hierarchical.clusters
 *  	To configure tile generation of hierarchical clustered data.  Set to false [default] for 'regular'
 *   	tile generation (ie non-clustered data).  If set to true then one needs to assign different source 
 *    	'cluster levels' using the oculus.binning.source.levels.<order> property for each desired set of 
 *     	tile levels to be generated.  In this case, the property oculus.binning.source.location is not used.
 *     
 *  oculus.binning.source.levels.<order>
 *  	This is only required if oculus.binning.hierarchical.clusters=true.  This property is used to assign
 *   	A given hierarchy "level" of clustered data to a given set of tile levels.  E.g., clustered data assigned
 *     	to oculus.binning.source.levels.0 will be used to generate tiles for all tiles given in the set 
 *     	oculus.binning.levels.0, and so on for other level 'orders'.
 *      
 *  -----    
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


class CSVGraphProcessingStrategy[IT: ClassTag] (sc: SparkContext,
                                                cacheRaw: Boolean,
                                                cacheFilterable: Boolean,
                                                cacheProcessed: Boolean,
                                                properties: CSVRecordPropertiesWrapper,
                                                indexer: CSVIndexExtractor[IT])
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

	private lazy val filterableData: RDD[(String, List[Double])] = {
		val localProperties = properties
		val data = rawData.mapPartitions(iter =>
			{
				val parser = new GraphRecordParser(localProperties)
				// Parse the records from the raw data, parsing all fields
				// The funny end syntax tells scala to treat fields as a varargs
				parser.parseGraphRecords(iter, localProperties.fields:_*)
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
		val localZVar = properties.getString("oculus.binning.valueField",
											"The field to use for the value to tile",
											Some("count"))
		rawData2 = {
			val source = new CSVDataSource(properties)
			val data = source.getData(sc);
			if (cacheRaw)
				data.persist(StorageLevel.MEMORY_AND_DISK)
			data
		}

		val data = rawData2.mapPartitions(iter =>
			{
				val parser = new GraphRecordParser(localProperties)
				// Determine which fields we need
				val fields = if ("count" == localZVar) {
					localIndexer.fields
				} else {
					localIndexer.fields :+ localZVar
				}

				// Parse the records from the raw data
				parser.parseGraphRecords(iter, fields:_*)
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
						 extractor.getFieldValue(localZVar)(t))
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

		data
	}
}


/**
 * Handles basic RDD's using a ProcessingStrategy. 
 */
class CSVGraphDataset[IT: ClassTag](indexer:  CSVIndexExtractor[IT],
                                    properties: CSVRecordPropertiesWrapper,
                                    tileWidth: Int,
                                    tileHeight: Int)
		extends CSVDatasetBase[IT](indexer, properties, tileWidth, tileHeight) {
	// Just some Filter type aliases from Queries.scala
	import com.oculusinfo.tilegen.datasets.FilterAware._

	type STRATEGY_TYPE = CSVGraphProcessingStrategy[IT]
	protected var strategy: STRATEGY_TYPE = null

	def getRawData: RDD[String] = strategy.getRawData

	def getRawFilteredData (filterFcn: Filter):	RDD[String] = {
		strategy.getFilterableData
			.filter{ case (record, fields) => filterFcn(fields)}
			.map(_._1)
	}
	def getRawFilteredJavaData(filterFcn: Filter): JavaRDD[String] =
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
		initialize(new CSVGraphProcessingStrategy[IT](sc, cacheRaw, cacheFilterable, cacheProcessed, properties, indexer))	
}



object CSVGraphBinner {
	
	private var _graphDataType = "nodes"
	private var _lineLevelThres = 4		// [level] threshold to determine whether to use 'point' vs 'tile' 
										// based line segment binning.  Levels above this thres use tile-based binning.
	private var _lineMinBins = 2		// [bins] min line segment length for a given level.
	private var _lineMaxBins = 1024		// [bins] max line segment length for a given level.
	
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

	def createIndexExtractor (properties: PropertiesWrapper): CSVIndexExtractor[_] = {
					
		_graphDataType = properties.getString("oculus.binning.graph.data",
		                                     "The type of graph data to tile (nodes or edges). "+
			                                     "Default is nodes.",
		                                     Some("nodes"))
		                                     
		// NOTE!  currently, indexType is assumed to be cartesian for graph data
//		val indexType = properties.getString("oculus.binning.index.type",
//		                                     "The type of index to use in the data.  Currently "+
//			                                     "suppoted options are cartesian (the default) "+
//			                                     "and ipv4.",
//		                                     Some("cartesian"))
			
		_graphDataType match {
			case "nodes" => {
				val xVar = properties.getString("oculus.binning.xField",
				                                "The field to use for the X axis of tiles produced",
				                                Some(CSVDatasetBase.ZERO_STR))
				val yVar = properties.getString("oculus.binning.yField",
				                                "The field to use for the Y axis of tiles produced",
				                                Some(CSVDatasetBase.ZERO_STR))
				new CartesianIndexExtractor(xVar, yVar)
			}
			case "edges" => {
				// edges require two cartesian endpoints 
				val xVar1 = properties.getString("oculus.binning.xField",
				                                "The field to use for the X axis for edge start pt",
				                                Some(CSVDatasetBase.ZERO_STR))
				val yVar1 = properties.getString("oculus.binning.yField",
				                                "The field to use for the Y axis for edge start pt",
				                                Some(CSVDatasetBase.ZERO_STR))
				val xVar2 = properties.getString("oculus.binning.xField2",
				                                "The field to use for the X axis for edge end pt",
				                                Some(CSVDatasetBase.ZERO_STR))
				val yVar2 = properties.getString("oculus.binning.yField2",
				                                "The field to use for the Y axis for edge end pt",
				                                Some(CSVDatasetBase.ZERO_STR))				                                
				new LineSegmentIndexExtractor(xVar1, yVar1, xVar2, yVar2)
			}			
		}
	}	
	
	def processDataset[IT: ClassTag,
	                   PT: ClassTag, 
	                   BT] (dataset: Dataset[IT, PT, BT],
	                        tileIO: TileIO): Unit = {

		if (_graphDataType == "edges") {
			val binner = new RDDLineBinner(_lineMinBins, _lineMaxBins)
			binner.debug = true
			dataset.getLevels.map(levels =>
				{
					val procFcn: RDD[(IT, PT)] => Unit =
						rdd =>
					{
						val bUsePointBinner = (levels.max <= _lineLevelThres)	// use point-based vs tile-based line-segment binning?
						
						val tiles = binner.processDataByLevel(rdd,
						                                      dataset.getIndexScheme,
						                                      dataset.getBinDescriptor,
						                                      dataset.getTilePyramid,
						                                      levels,
						                                      (dataset.getNumXBins max dataset.getNumYBins),
						                                      dataset.getConsolidationPartitions,
						                                      dataset.isDensityStrip,
						                                      bUsePointBinner)
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
		else  {//if (_graphDataType == "nodes")
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
	}

	/**
	 * This function is simply for pulling out the generic params from the DatasetFactory,
	 * so that they can be used as params for other types.
	 */
	def processDatasetGeneric[IT, PT, BT] (dataset: Dataset[IT, PT, BT],
	                                       tileIO: TileIO): Unit =
		processDataset(dataset, tileIO)(dataset.indexTypeManifest, dataset.binTypeManifest)

		
	private def getDataset[T: ClassTag] (indexer: CSVIndexExtractor[T],
	                                     properties: CSVRecordPropertiesWrapper,
	                                     tileWidth: Int,
	                                     tileHeight: Int): CSVGraphDataset[T] =
		new CSVGraphDataset(indexer, properties, tileWidth, tileHeight)		

	
	private def getDatasetGeneric[T] (indexer: CSVIndexExtractor[T],
	                                  properties: CSVRecordPropertiesWrapper,
	                                  tileWidth: Int,
	                                  tileHeight: Int): CSVGraphDataset[T] =
		getDataset(indexer, properties, tileWidth, tileHeight)(indexer.indexTypeManifest)		

		
	def createDataset(sc: SparkContext,
	                   dataDescription: Properties,
	                   cacheRaw: Boolean,
	                   cacheFilterable: Boolean,
	                   cacheProcessed: Boolean,
	                   tileWidth: Int = 256,
	                   tileHeight: Int = 256): Dataset[_, _, _] = {
		// Wrap parameters more usefully
		val properties = new CSVRecordPropertiesWrapper(dataDescription)

		// Determine indexing information
		val indexer = createIndexExtractor(properties)
		
		// init parameters for binning graph edges (note, not used for binning graph's nodes) 
		_lineLevelThres = properties.getInt("oculus.binning.line.level.threshold",
									"Level threshold to determine whether to use 'point' vs 'tile'"+
									" based line segment binning (levels above this thres use tile-based binning)",
									Some(4))
									
		_lineMinBins = properties.getInt("oculus.binning.line.min.bins",
									"Min line segment length (in bins) for a given level.  Shorter line segments will be discarded",
									Some(2))
									
		_lineMaxBins = properties.getInt("oculus.binning.line.max.bins",
									"Max line segment length (in bins) for a given level.  Longer line segments will be discarded",
									Some(1024))							

		val dataset:CSVGraphDataset[_] = getDatasetGeneric(indexer, properties, tileWidth, tileHeight)
		dataset.initialize(sc, cacheRaw, cacheFilterable, cacheProcessed)
		dataset
	}
	
	def main (args: Array[String]): Unit = {
		if (args.size<1) {
			println("Usage:")
			println("\tCSVGraphBinner [-d default_properties_file] job_properties_file_1 job_properties_file_2 ...")
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
			
			// check if hierarchical mode is enabled
			var valTemp = props.getProperty("oculus.binning.hierarchical.clusters","false");
			var hierarchicalClusters = if (valTemp=="true") true else false
			
			if (!hierarchicalClusters) {
				// regular tile generation
				processDatasetGeneric(createDataset(sc, props, false, false, true), tileIO)			
			}
			else {
				// hierarchical-based tile generation
				var nn = 0;
				var levelsList:scala.collection.mutable.MutableList[String] = scala.collection.mutable.MutableList()
				var sourcesList:scala.collection.mutable.MutableList[String] = scala.collection.mutable.MutableList()
				do {
					// Loop through all "sets" of tile generation levels.  
					// (For each set, we will generate tiles based on a
					// given subset of hierarchically-clustered data)
					valTemp = props.getProperty("oculus.binning.levels."+nn)
					
					if (valTemp != null) {
						levelsList += valTemp	// save tile gen level set in a list
						//... and temporarily overwrite tiling levels as empty in preparation for hierarchical tile gen
						props.setProperty("oculus.binning.levels."+nn, "")	
						
						// get clustered data for this hierarchical level and save to list
						var sourceTemp = props.getProperty("oculus.binning.source.levels."+nn)
						if (sourceTemp == null) {
							throw new Exception("Source data not defined for hierarchical level oculus.binning.source.levels."+nn)
						}
						else {
							sourcesList += sourceTemp
						}
						nn+=1
					}
					
				} while (valTemp != null)
				
				// Loop through all hierarchical levels, and perform tile generation
				var m = 0
				for (m <- 0 until nn) {
					// set tile gen level(s)
					props.setProperty("oculus.binning.levels."+m, levelsList(m))
					// set raw data source
					props.setProperty("oculus.binning.source.location", sourcesList(m))
					// perform tile generation
					processDatasetGeneric(createDataset(sc, props, false, false, true), tileIO)
					// reset tile gen levels for next loop iteration
					props.setProperty("oculus.binning.levels."+m, "")
				}	
								    				
			}

			val fileEndTime = System.currentTimeMillis()
			println("Finished binning "+args(argIdx)+" in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")

			argIdx = argIdx + 1
		}
		val endTime = System.currentTimeMillis()
		println("Finished binning all sets in "+((endTime-startTime)/60000.0)+" minutes")
	}
}
