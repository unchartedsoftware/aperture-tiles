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

package com.oculusinfo.tilegen.graph.analytics

import com.oculusinfo.tilegen.util.PropertiesWrapper
import com.oculusinfo.tilegen.tiling.TileIO
import com.oculusinfo.tilegen.tiling.HBaseTileIO
import com.oculusinfo.tilegen.tiling.SqliteTileIO
import com.oculusinfo.tilegen.tiling.LocalTileIO
import scala.reflect.ClassTag
import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.tiling.RDDBinner
import org.apache.spark.rdd.RDD
import java.util.Properties
import java.io.FileInputStream
import scala.util.Try
import com.oculusinfo.tilegen.datasets.DatasetFactory


/**
 * This application handles reading in a graph dataset from a CSV file, and generating
 * tile analytics for the graph's nodes and communities.
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
 *  oculus.tileio.type
 *      The way in which tiles are written - either hbase (to write to hbase,
 *      see hbase. properties above to specify where) or file  to write to the
 *      local file system
 *      Default is hbase
 *
 */



object GraphAnalyticsBinner {
	private var _graphDataType = "nodes"
		
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
	
	def processDataset[IT: ClassTag,
	                   PT: ClassTag,
	                   DT: ClassTag,
	                   AT: ClassTag,
	                   BT] (dataset: Dataset[IT, PT, DT, AT, BT],
	                        tileIO: TileIO): Unit = {

		if (_graphDataType == "edges") {
			throw new IllegalArgumentException("oculus.binning.graph.data must =nodes for this application")
//			val binner = new RDDLineBinner(_lineMinBins, _lineMaxBins, _bDrawLineEnds)
//			binner.debug = true
//			dataset.getLevels.map(levels =>
//				{
//					val procFcn: RDD[(IT, PT, Option[DT])] => Unit =
//						rdd =>
//					{
//						val bUsePointBinner = (levels.max <= _lineLevelThres)	// use point-based vs tile-based line-segment binning?
//						
//						val tiles = binner.processDataByLevel(rdd,
//						                                      dataset.getIndexScheme,
//						                                      dataset.getBinningAnalytic,
//						                                      dataset.getTileAnalytics,
//						                                      dataset.getDataAnalytics,
//						                                      dataset.getTilePyramid,
//						                                      levels,
//						                                      dataset.getNumXBins,
//						                                      dataset.getNumYBins,
//						                                      dataset.getConsolidationPartitions,
//						                                      dataset.isDensityStrip,
//						                                      bUsePointBinner,
//						                                      _bLinesAsArcs)
//						tileIO.writeTileSet(dataset.getTilePyramid,
//						                    dataset.getName,
//						                    tiles,
//						                    dataset.getValueScheme,
//						                    dataset.getTileAnalytics,
//						                    dataset.getDataAnalytics,
//						                    dataset.getName,
//						                    dataset.getDescription)
//					}
//					dataset.process(procFcn, None)
//				}
//			)
		}
		else  {//if (_graphDataType == "nodes")
			val binner = new RDDBinner
			binner.debug = true
			dataset.getLevels.map(levels =>
				{
					val procFcn: RDD[(IT, PT, Option[DT])] => Unit =
						rdd =>
					{
						val tiles = binner.processDataByLevel(rdd,
						                                      dataset.getIndexScheme,
						                                      dataset.getBinningAnalytic,
						                                      dataset.getTileAnalytics,
						                                      dataset.getDataAnalytics,
						                                      dataset.getTilePyramid,
						                                      levels,
						                                      dataset.getNumXBins,
						                                      dataset.getNumYBins,
						                                      dataset.getConsolidationPartitions,
						                                      dataset.isDensityStrip)
						tileIO.writeTileSet(dataset.getTilePyramid,
						                    dataset.getName,
						                    tiles,
						                    dataset.getValueScheme,
						                    dataset.getTileAnalytics,
						                    dataset.getDataAnalytics,
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
	def processDatasetGeneric[IT, PT, DT, AT, BT] (dataset: Dataset[IT, PT, DT, AT, BT],
	                                               tileIO: TileIO): Unit =
		processDataset(dataset, tileIO)(dataset.indexTypeTag,
		                                dataset.binTypeTag,
		                                dataset.dataAnalysisTypeTag,
		                                dataset.tileAnalysisTypeTag)


	def main (args: Array[String]): Unit = {
		if (args.size<1) {
			println("Usage:")
			println("\tGraphAnalyticsBinner [-d default_properties_file] job_properties_file_1 job_properties_file_2 ...")
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
		defProps.setProperty("oculus.binning.index.type", "graph")

		val defaultProperties = new PropertiesWrapper(defProps)
		val connector = defaultProperties.getSparkConnector()
		val sc = connector.getSparkContext("Pyramid Binning")
		val tileIO = TileIO.fromArguments(defaultProperties)

		// Run for each real properties file
		val startTime = System.currentTimeMillis()
		while (argIdx < args.size) {
			val fileStartTime = System.currentTimeMillis()
			val props = new Properties(defProps)
			val propStream = new FileInputStream(args(argIdx))
			props.load(propStream)
			propStream.close()
			
			
			// init type of graph tile generation job (nodes or edges) 
			_graphDataType = Try(props.getProperty("oculus.binning.graph.data",
		                                      "The type of graph data to tile (nodes or edges). "+
			                                      "Default is nodes.")).getOrElse("nodes")

			// check if hierarchical mode is enabled
			var valTemp = props.getProperty("oculus.binning.hierarchical.clusters","false");
			var hierarchicalClusters = if (valTemp=="true") true else false
			
			if (!hierarchicalClusters) {
				// If the user hasn't explicitly set us not to cache, cache processed data to make
				// multiple runs more efficient
				if (!props.stringPropertyNames.contains("oculus.binning.caching.processed"))
					props.setProperty("oculus.binning.caching.processed", "true")

				// regular tile generation
				processDatasetGeneric(DatasetFactory.createDataset(sc, props), tileIO)
			}
			else {
				// hierarchical-based tile generation
				var nn = 0;
				var levelsList:scala.collection.mutable.MutableList[String] =
					scala.collection.mutable.MutableList()
				var sourcesList:scala.collection.mutable.MutableList[String] =
					scala.collection.mutable.MutableList()
				do {
					// Loop through all "sets" of tile generation levels.
					// (For each set, we will generate tiles based on a
					// given subset of hierarchically-clustered data)
					valTemp = props.getProperty("oculus.binning.levels."+nn)
					
					if (valTemp != null) {
						levelsList += valTemp	// save tile gen level set in a list
							// ... and temporarily overwrite tiling levels as empty
							// in preparation for hierarchical tile gen
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
					// If the user hasn't explicitly set us not to cache, cache processed data to make
					// multiple runs more efficient
					if (!props.stringPropertyNames.contains("oculus.binning.caching.processed"))
						props.setProperty("oculus.binning.caching.processed", "true")
					// perform tile generation
					processDatasetGeneric(DatasetFactory.createDataset(sc, props),
					                      tileIO)
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
