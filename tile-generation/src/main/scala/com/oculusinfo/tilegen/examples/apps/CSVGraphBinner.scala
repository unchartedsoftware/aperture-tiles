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

import com.oculusinfo.binning.BinIndex
import com.oculusinfo.tilegen.datasets.{CSVReader, CSVDataSource, TilingTask}
import com.oculusinfo.tilegen.tiling.{UniversalBinner, RDDLineBinner, TileIO}
import com.oculusinfo.tilegen.util.{EndPointsToLine, PropertiesWrapper}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext

import scala.reflect.ClassTag
import scala.util.Try


/**
 * This application handles reading in a graph from a CSV file, and generating
 * tiles for the graph's nodes or edges.
 *
 * NOTE:  It is expected that the 1st column of the CSV graph data will contain either the keyword "node"
 * for data lines representing a graph's node/vertex, or the keyword "edge" for data lines representing
 * a graph's edge
 *
 * The following properties control how the application runs:
 * (See code comments in TilingTask.scala for more info and settings)
 *
 *
 *  oculus.binning.graph.data
 *      The type of graph data to use for tile generation.  Set to "nodes" to generate tiles of a graph's
 *      nodes [default], or set to "edges" to generate tiles of a graph's edges.
 *
 *  oculus.binning.graph.edges.type.index
 *      The column number of a boolean specifying each edge as an inter-community edge (=1) or an intra-community edge (=0)
 *
 *  oculus.binning.graph.edges.type
 *      The type of edges to use for tile generation (for hierarchically clustered data only).  Set to "all"
 *      to generate tiles using all edges in a graph [default].  Set to "inter" to only use inter-community
 *      edges, or set to "intra" use to intra-community edges.  For the "inter" or "intra" switch, the
 *      'edges.type.index' column (above) is used to determine which raw edges to use for tile generation.
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
 *  oculus.binning.line.drawends
 *  	Boolean = false by default. If = true, then any line segments longer than max.bins will have
 *   	only pixels within 1 tile-length of an endpoint drawn instead of the whole line segment
 *    	being discarded.  Also, line segments will be faded out as they get farther away from an endpoint
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



object CSVGraphBinner {
	private var _graphDataType = "nodes"
	private var _lineLevelThres = 4		// [level] threshold to determine whether to use 'point' vs 'tile'
			// based line segment binning.  Levels above this thres use tile-based binning.
	private var _lineMinBins = 2		// [bins] min line segment length for a given level.
	private var _lineMaxBins = 1024		// [bins] max line segment length for a given level.
	private var _bDrawLineEnds = false	// [Boolean] switch to draw just the ends of very long line segments
	private var _bLinesAsArcs = false	// [Boolean] switch to draw line segments as straight lines (default) or as clock-wise arcs.
	private var _bDrawDirectedArcs = false	// [Boolean] switch to draw directed arcs (direction is inferred by clock-wise curve of arc)

	def processTask[PT: ClassTag,
	                   DT: ClassTag,
	                   AT: ClassTag,
	                   BT] (sc: SparkContext,
	                        task: TilingTask[PT, DT, AT, BT],
	                        tileIO: TileIO): Unit = {
		val tileAnalytics = task.getTileAnalytics
		val dataAnalytics = task.getDataAnalytics

		// Add global accumulators to analytics
		tileAnalytics.map(_.addGlobalAccumulator(sc))
		dataAnalytics.map(_.addGlobalAccumulator(sc))

		if (_graphDataType == "edges") {
			val binner = new RDDLineBinner(_lineMinBins, _lineMaxBins, _bDrawLineEnds)

			val lenThres = if (_bDrawLineEnds) _lineMaxBins else Int.MaxValue
			val lineDrawer = new EndPointsToLine(lenThres, task.getNumXBins, task.getNumYBins)

			binner.debug = true
			task.getLevels.map(levels =>
				{
					// Add level accumulators for all analytics for these levels (for now at least)
					tileAnalytics.map(analytic =>
						levels.map(level => analytic.addLevelAccumulator(sc, level))
					)
					dataAnalytics.map(analytic =>
						levels.map(level => analytic.addLevelAccumulator(sc, level))
					)

					val procFcn: RDD[(Seq[Any], PT, Option[DT])] => Unit =
						rdd =>
					{

						// Assign generic line drawing function for either drawing straight lines or arcs
						// (to be passed into processDataByLevel func)
						val calcLinePixels = if (task.binTypeTag == scala.reflect.classTag[Double])	{
							// PT is type Double, so assign line drawing func (using hard-casting)
							val lineDrawFcn = if (_bLinesAsArcs)
								lineDrawer.endpointsToArcBins
							else
								lineDrawer.endpointsToLineBins
							lineDrawFcn.asInstanceOf[(BinIndex, BinIndex, PT) => IndexedSeq[(BinIndex, PT)]]
						}
						else {
							// PT is not type Double, so don't draw lines.  Simply pass-through endpoints.
							val passTroughFcn: (BinIndex, BinIndex, PT) => IndexedSeq[(BinIndex, PT)] =
								(start, end, PT) => {
									IndexedSeq((start, PT), (end, PT))
								}
							passTroughFcn
						}

						val bUsePointBinner = (levels.max <= _lineLevelThres)	// use point-based vs tile-based line-segment binning?

						val tiles = binner.processDataByLevel(rdd,
						                                      task.getIndexScheme,
						                                      task.getBinningAnalytic,
						                                      tileAnalytics,
						                                      dataAnalytics,
						                                      task.getTilePyramid,
						                                      levels,
						                                      task.getNumXBins,
						                                      task.getNumYBins,
						                                      task.getConsolidationPartitions,
						                                      task.getTileType,
						                                      calcLinePixels,
						                                      bUsePointBinner,
						                                      _bLinesAsArcs,
										 					  _bDrawDirectedArcs)
						tileIO.writeTileSet(task.getTilePyramid,
						                    task.getName,
						                    tiles,
						                    task.getTileSerializer,
						                    tileAnalytics,
						                    dataAnalytics,
						                    task.getName,
						                    task.getDescription)
					}
					task.process(procFcn, None)
				}
			)
		}
		else  {//if (_graphDataType == "nodes")
			val binner = new UniversalBinner
			task.getLevels.map(levels =>
				{
					// Add level accumulators for all analytics for these levels (for now at least)
					tileAnalytics.map(analytic =>
						levels.map(level => analytic.addLevelAccumulator(sc, level))
					)
					dataAnalytics.map(analytic =>
						levels.map(level => analytic.addLevelAccumulator(sc, level))
					)

					val procFcn: RDD[(Seq[Any], PT, Option[DT])] => Unit =
						rdd =>
					{
						val tiles = binner.processDataByLevel(rdd,
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
						tileIO.writeTileSet(task.getTilePyramid,
						                    task.getName,
						                    tiles,
						                    task.getTileSerializer,
						                    tileAnalytics,
						                    dataAnalytics,
						                    task.getName,
						                    task.getDescription)
					}
					task.process(procFcn, None)
				}
			)
		}
	}

	/**
	 * This function is simply for pulling out the generic params from the the TilingTask,
	 * so that they can be used as params for other types.
	 */
	def processTaskGeneric[PT, DT, AT, BT] (sc: SparkContext,
	                                        task: TilingTask[PT, DT, AT, BT],
	                                        tileIO: TileIO): Unit =
		processTask(sc, task, tileIO)(task.binTypeTag,
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
			println("\tCSVGraphBinner [-d default_properties_file] job_properties_file_1 job_properties_file_2 ...")
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

		val defaultProperties = new PropertiesWrapper(defProps)
		val connector = defaultProperties.getSparkConnector()
		val sc = connector.createContext(Some("Pyramid Binning"))
		val sqlc = new SQLContext(sc)
		val tileIO = TileIO.fromArguments(defaultProperties)

		// Run for each real properties file
		val startTime = System.currentTimeMillis()
		while (argIdx < args.size) {
			val fileStartTime = System.currentTimeMillis()
			val props = readFile(args(argIdx), new Properties(defProps))

			// init type of graph tile generation job (nodes or edges)
			_graphDataType = Try(props.getProperty("oculus.binning.graph.data",
			                                       "The type of graph data to tile (nodes or edges). "+
				                                       "Default is nodes.")).getOrElse("nodes")
			// Set binning index type accordingly
		    if (_graphDataType == "edges") {
		    	props.setProperty("oculus.binning.index.type", "segment")
		    }
		    else { //_graphDataType="nodes"
		    	props.setProperty("oculus.binning.index.type", "cartesian")
		    }

			// init parameters for binning graph edges (note, not used for
			// binning graph's nodes)
			// Level threshold to determine whether to use 'point' vs 'tile'
			// based line segment binning (levels above this thres use
			// tile-based binning
			_lineLevelThres = Try(props.getProperty("oculus.binning.line.level.threshold").toInt).getOrElse(4)

			// Min line segment length (in bins) for a given level.  Shorter
			// line segments will be discarded
			_lineMinBins = Try(props.getProperty("oculus.binning.line.min.bins").toInt).getOrElse(2)

			// Max line segment length (in bins) for a given level.  Longer
			// line segments will be discarded
			_lineMaxBins = Try(props.getProperty("oculus.binning.line.max.bins").toInt).getOrElse(1024)

			// Boolean to draw just the ends of very long line segments (instead of just discarding the whole line)
			_bDrawLineEnds = Try(props.getProperty("oculus.binning.line.drawends").toBoolean).getOrElse(false)

			// Draw line segments as straight lines (default) or as clock-wise arcs.
			_bLinesAsArcs = Try(props.getProperty("oculus.binning.line.style.arcs").toBoolean).getOrElse(false)

			// Draw directed arcs instead of undirected (direction is inferred by clockwise curve of arc)
			_bDrawDirectedArcs = Try(props.getProperty("oculus.binning.line.directed.arcs").toBoolean).getOrElse(false)

			// check if hierarchical mode is enabled
			var valTemp = props.getProperty("oculus.binning.hierarchical.clusters","false");
			var hierarchicalClusters = if (valTemp=="true") true else false

			if (!hierarchicalClusters) {
				// Read our CSV data
				val wrappedProps = new PropertiesWrapper(props)
				val source = new CSVDataSource(wrappedProps)
				// Read the CSV into a schema file
				val reader = new CSVReader(sqlc, source.getData(sc), wrappedProps)
				// Register it as a table
				val table = "table"+argIdx
				reader.asDataFrame.registerTempTable(table)

				val cache = wrappedProps.getBoolean(
					"oculus.binning.caching.processed",
					"Cache the data, in a parsed and processed form, if true",
					Some(true))
				if (cache) sqlc.cacheTable(table)

				// regular tile generation
				processTaskGeneric(sc, TilingTask(sqlc, table, props), tileIO)
			} else {
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

					// Read our CSV data
					val wrappedProps = new PropertiesWrapper(props)
					val source = new CSVDataSource(wrappedProps)
					// Read the CSV into a schema file
					val reader = new CSVReader(sqlc, source.getData(sc), wrappedProps)
					// Register it as a table (include hierarchy level in table name too)
					val table = "table"+argIdx+m
					reader.asDataFrame.registerTempTable(table)

					val cache = wrappedProps.getBoolean(
						"oculus.binning.caching.processed",
						"Cache the data, in a parsed and processed form, if true",
						Some(true))
					if (cache) sqlc.cacheTable(table)

					// perform tile generation
					processTaskGeneric(sc, TilingTask(sqlc, table, props), tileIO)

					if (cache) sqlc.uncacheTable(table)

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
