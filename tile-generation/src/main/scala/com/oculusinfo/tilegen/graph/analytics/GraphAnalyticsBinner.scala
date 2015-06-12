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

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.tilegen.util.{KeyValueArgumentSource, PropertiesWrapper}
import com.oculusinfo.tilegen.tiling.TileIO
import org.apache.avro.file.CodecFactory
import scala.reflect.ClassTag
import com.oculusinfo.tilegen.tiling.UniversalBinner
import org.apache.spark.rdd.RDD
import java.util.Properties
import java.io.FileInputStream
import scala.util.Try
	import org.apache.spark.SparkContext
import java.util.{List => JavaList}
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.CompositeAnalysisDescription
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import org.apache.spark.graphx._
import scala.collection.JavaConverters._

/**
 * This application handles reading in a graph dataset from a CSV file, and generating
 * pre-tile analytics for the graph's nodes and communities.
 *
 * NOTE:  It is expected that the 1st column of the CSV graph data will contain either the keyword "node"
 * for data lines representing a graph's node/vertex, or the keyword "edge" for data lines representing
 * a graph's edge
 *
 * The following properties control how the application runs:
 * (See code comments in TilingTask.scala for more info and settings)
 *
 *
 *  oculus.binning.hierarchical.clusters
 *  	To configure tile generation of hierarchical clustered data.  Set to false [default] for 'regular'
 *   	tile generation (ie non-clustered data).  If set to true then one needs to assign different source
 *    	'cluster levels' using the oculus.binning.source.levels.<order> property for each desired set of
 *     	tile levels to be generated.  In this case, the property oculus.binning.source.location is not used.
 *
 *  oculus.binning.hierarchical.maxlevel
 *  	The highest hierarchy level used for tile generation (ie for a dataset having 0,1,...maxlevel hierarchies)
 *
 *  oculus.binning.source.levels.<order>
 *  	This is only required if oculus.binning.hierarchical.clusters=true.  This property is used to assign
 *   	A given hierarchy "level" of clustered data to a given set of tile levels.  E.g., clustered data assigned
 *     	to oculus.binning.source.levels.0 will be used to generate tiles for all tiles given in the
 *     	oculus.binning.levels.0 set of zoom levels, and so on for other level 'orders'.
 *
 *  -----
 *
 *  Parameters for parsing graph community information [required]
 *
 *  oculus.binning.graph.x.index
 *      The column number of X axis coord of each graph community/node (Double)
 *
 *  oculus.binning.graph.y.index
 *      The column number of Y axis coord of each graph community/node (Double)
 *
 *  oculus.binning.graph.id.index
 *      The column number of Long ID of each graph community/node (Long)
 *
 *  oculus.binning.graph.r.index
 *      The column number of the radius of each graph community/node (Double)
 *
 *  oculus.binning.graph.numnodes.index
 *      The column number of the number of raw nodes in each graph community (Long)
 *
 *  oculus.binning.graph.degree.index
 *      The column number of the degree of each graph community/node (Int)
 *
 *  oculus.binning.graph.metadata.index
 *      The column number of the metadata of each graph community/node (comma delimited string)
 *
 *  oculus.binning.graph.parentID.index
 *      The column number of Long ID of a given parent community (Long)
 *
 *  oculus.binning.graph.parentR.index
 *      The column number of the radius of a given parent community (Double)
 *
 *  oculus.binning.graph.parentX.index
 *      The column number of X coordinate of a given parent community (Double)
 *
 *  oculus.binning.graph.parentY.index
 *      The column number of Y coordinate of a given parent community (Double)
 *
 *  oculus.binning.graph.maxcommunities
 *  	The max number of communities to store per tile (ranked by community size). Default is 25.
 *
 *  -----
 *
 *  Parameters for parsing graph edge information [optional].
 *  	  NOTE: If edge parameters are not specified then analytics will ONLY be calculated
 *        for the graph's communities/nodes (NOT edges).
 *
 *  oculus.binning.graph.edge.srcID.index
 *  	The column number of source ID of each graph edge (Long)
 *
 *  oculus.binning.graph.edges.dstID.index
 *  	The column number of destination ID of each graph edge (Long)
 *
 *  oculus.binning.graph.edges.weight.index
 *  	The column number of the weight of each graph edge (Long).
 *   	Default is all edges are given a weiight of 1 (unweighted).
 *
 *  oculus.binning.graph.edges.type.index
 *      The column number of a boolean specifying each edge as an inter-community edge (=1) or an intra-community edge (=0)
 *
 *  oculus.binning.graph.maxedges
 *  	The max number of both inter-community and intra-community edges to
 *   	store per community (ranked by weight). Default is 10.
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
	private var _hierlevel = 0

	//------------------
	def importAndProcessData (sc: SparkContext,
	                          dataDescription: Properties,
	                          tileIO: TileIO,
	                          hierarchyLevel: Int = 0) = {

		// Wrap parameters more usefully
		val properties = new PropertiesWrapper(dataDescription)

		val source = properties.getString("oculus.binning.source.location", "The hdfs file name from which to get the CSV data")
		val partitions = properties.getInt("oculus.binning.source.partitions",
		                                   "The number of partitions to use when reducing data, if needed", Some(0))
		//val consolidationPartitions = properties.getIntOption("oculus.binning.consolidationPartitions",
		//								   "The number of partitions into which to consolidate data when done")
		val pyramidName = properties.getString("oculus.binning.name","The name of the tileset",Some("unknown"))
		val pyramidDescription = properties.getString("oculus.binning.description", "The description to put in the tile metadata",Some(""))

		val levelSets = properties.getStringPropSeq("oculus.binning.levels",		// parse zoom level sets
		                                            "The levels to bin").map(lvlString =>
			{
				lvlString.split(',').map(levelRange =>
					{
						val extrema = levelRange.split('-')

						if ((0 == extrema.size) || (levelRange==""))
							Seq[Int]()
						else if (1 == extrema.size)
							Seq[Int](extrema(0).toInt)
						else
							Range(extrema(0).toInt, extrema(1).toInt+1).toSeq
					}
				).fold(Seq[Int]())(_ ++ _)
			}
		).filter(levelSeq =>
			levelSeq != Seq[Int]()	// discard empty entries
		)
		val levelBounds = levelSets.map(_.map(a => (a, a))
			                                .reduce((a, b) => (a._1 min b._1, a._2 max b._2)))
			.reduce((a, b) => (a._1 min b._1, a._2 max b._2))

		val rawData = if (0 == partitions) {	// read in raw data
			sc.textFile(source)
		} else {
			sc.textFile(source, partitions)
		}

		val minAnalysis:
				AnalysisDescription[TileData[JavaList[GraphAnalyticsRecord]],
				                    List[GraphAnalyticsRecord]] =
			new GraphListAnalysis(new GraphMinRecordAnalytic)

		val maxAnalysis:
				AnalysisDescription[TileData[JavaList[GraphAnalyticsRecord]],
				                    List[GraphAnalyticsRecord]] =
			new GraphListAnalysis(new GraphMaxRecordAnalytic)

		val tileAnalytics: Option[AnalysisDescription[TileData[JavaList[GraphAnalyticsRecord]],
		                                              (List[GraphAnalyticsRecord],
		                                               List[GraphAnalyticsRecord])]] =
			Some(new CompositeAnalysisDescription(minAnalysis, maxAnalysis))

		//		val tileAnalytics: Option[AnalysisDescription[TileData[JavaList[GraphAnalyticsRecord]], JavaList[GraphAnalyticsRecord]]] = None
		val dataAnalytics: Option[AnalysisDescription[((Double, Double), GraphAnalyticsRecord),
		                                              Int]] = None
		// process data
		genericProcessData(sc, rawData, levelSets, tileIO, tileAnalytics, dataAnalytics, pyramidName, pyramidDescription, properties, hierarchyLevel)

	}

	//------------
	private def genericProcessData[AT, DT]
		(sc: SparkContext,
		 rawData: RDD[String],
		 levelSets: Seq[Seq[Int]],
		 tileIO: TileIO,
		 tileAnalytics: Option[AnalysisDescription[TileData[JavaList[GraphAnalyticsRecord]], AT]],
		 dataAnalytics: Option[AnalysisDescription[((Double, Double), GraphAnalyticsRecord), DT]],
		 pyramidName: String,
		 pyramidDescription: String,
		 properties: KeyValueArgumentSource,
		 hierarchyLevel: Int = 0) =
	{
		val tileAnalyticsTag: ClassTag[AT] = tileAnalytics.map(_.analysisTypeTag).getOrElse(ClassTag.apply(classOf[Int]))
		val dataAnalyticsTag: ClassTag[DT] = dataAnalytics.map(_.analysisTypeTag).getOrElse(ClassTag.apply(classOf[Int]))

		processData(sc, rawData, levelSets, tileIO, tileAnalytics, dataAnalytics, pyramidName, pyramidDescription, properties, hierarchyLevel)(tileAnalyticsTag, dataAnalyticsTag)

	}

	//------------
	private def processData[AT: ClassTag, DT: ClassTag]
		(sc: SparkContext,
		 rawData: RDD[String],
		 levelSets: Seq[Seq[Int]],
		 tileIO: TileIO,
		 tileAnalytics: Option[AnalysisDescription[TileData[JavaList[GraphAnalyticsRecord]], AT]],
		 dataAnalytics: Option[AnalysisDescription[((Double, Double), GraphAnalyticsRecord), DT]],
		 pyramidName: String,
		 pyramidDescription: String,
		 properties: KeyValueArgumentSource,
		 hierarchyLevel: Int = 0) =
	{
		val recordParser = new GraphAnalyticsRecordParser(hierarchyLevel, properties)
		val edgeMatcher = new EdgeMatcher

		// parse edge data
		val edgeData = rawData.flatMap(line => recordParser.getEdges(line))

		// parse node/community data
		val nodeData = rawData.flatMap(line => recordParser.getNodes(line))

		// match edges with corresponding graph communities
		val nodesWithEdges = edgeMatcher.matchEdgesWithCommunities(nodeData, edgeData)

		//convert parsed graph communities into GraphAnalyticsRecord objects for processing with RDDBinner
		val data = nodesWithEdges.map(record => {
			                              val (xy, community) = record
			                              val graphRecord = new GraphAnalyticsRecord(1, List(community).asJava)
			                              (xy, graphRecord, dataAnalytics.map(_.convert((xy, graphRecord))))
		                              })
		data.cache

		val binner = new UniversalBinner
		val tilePyramid = getTilePyramid(properties)

		println("\tTile analytics: "+tileAnalytics)
		println("\tData analytics: "+dataAnalytics)

		tileAnalytics.map(_.addGlobalAccumulator(sc))
		dataAnalytics.map(_.addGlobalAccumulator(sc))

		levelSets.foreach(levelSet =>
			{
				// Add level accumulators for all analytics for these levels (for now at least)
				tileAnalytics.map(analytic =>
					levelSet.map(level => analytic.addLevelAccumulator(sc, level))
				)
				dataAnalytics.map(analytic =>
					levelSet.map(level => analytic.addLevelAccumulator(sc, level))
				)

				println()
				println()
				println()
				println("Starting binning levels "+levelSet.mkString("[", ",", "]"))
				val startTime = System.currentTimeMillis
				val tiles = binner.processDataByLevel(data,
				                                      new CartesianIndexScheme,
				                                      new GraphBinningAnalytic,
				                                      tileAnalytics,
				                                      dataAnalytics,
				                                      tilePyramid,
				                                      levelSet,
				                                      xBins=1,
				                                      yBins=1)
				tileIO.writeTileSet(tilePyramid,
				                    pyramidName,
				                    tiles,
				                    new GraphAnalyticsAvroSerializer(CodecFactory.bzip2Codec()),
				                    tileAnalytics,
				                    dataAnalytics,
				                    pyramidName,
				                    pyramidDescription)
				val endTime = System.currentTimeMillis()
				println("Finished binning levels "+levelSet.mkString("[", ",", "]"))
				println("\telapsed time: "+((endTime-startTime)/60000.0)+" minutes")
				println()
			}
		)
	}

	//----------------
	def getTilePyramid(properties: KeyValueArgumentSource): TilePyramid = {
		val autoBounds = properties.getBoolean("oculus.binning.projection.autobounds",
		                                       "If true, calculate tile pyramid bounds automatically",
		                                       Some(false))
		if (autoBounds) {
			throw new Exception("oculus.binning.projection.autobounds = true currently not supported")
		}
		//TODO -- add in autobounds checking to this application (ie when/if we modify the app to use 'proper' tile-gen Data Analytics features)

		val projection = properties.getString("oculus.binning.projection.type",
		                                      "The type of tile pyramid to use",
		                                      Some("areaofinterest"))
		if ("webmercator" == projection) {
			new WebMercatorTilePyramid()
		} else {
			//		if (autoBounds) {
			//			new AOITilePyramid(minX, minY, maxX, maxY)
			//		} else {
			val minXp = properties.getDoubleOption("oculus.binning.projection.minX",
			                                       "The minimum x value to use for "+
				                                       "the tile pyramid").get
			val maxXp = properties.getDoubleOption("oculus.binning.projection.maxX",
			                                       "The maximum x value to use for "+
				                                       "the tile pyramid").get
			val minYp = properties.getDoubleOption("oculus.binning.projection.minY",
			                                       "The minimum y value to use for "+
				                                       "the tile pyramid").get
			val maxYp = properties.getDoubleOption("oculus.binning.projection.maxY",
			                                       "The maximum y value to use for "+
				                                       "the tile pyramid").get
			new AOITilePyramid(minXp, minYp, maxXp, maxYp)
			//		}
		}
	}

	//----------------
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
		//defProps.setProperty("oculus.binning.index.type", "graph")

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

			// check if hierarchical mode is enabled
			var valTemp = props.getProperty("oculus.binning.hierarchical.clusters","false");
			var hierarchicalClusters = if (valTemp=="true") true else false

			if (!hierarchicalClusters) {
				// If the user hasn't explicitly set us not to cache, cache processed data to make
				// multiple runs more efficient
				if (!props.stringPropertyNames.contains("oculus.binning.caching.processed"))
					props.setProperty("oculus.binning.caching.processed", "true")

				// regular tile generation
				importAndProcessData(sc, props, tileIO, _hierlevel)
			}
			else {
				// hierarchical-based tile generation

				// The highest hierarchy level used for tile generation
				var currentHierLevel = Try(props.getProperty("oculus.binning.hierarchical.maxlevel",
				                                             "The highest hierarchy level used for tile generation").toInt).getOrElse(0)

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
					importAndProcessData(sc, props, tileIO, currentHierLevel)

					// reset tile gen levels for next loop iteration
					props.setProperty("oculus.binning.levels."+m, "")

					currentHierLevel = currentHierLevel-1  //Math.max(currentHierLevel-1, 0)
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
