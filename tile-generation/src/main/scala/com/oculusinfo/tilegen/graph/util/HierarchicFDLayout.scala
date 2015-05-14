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

package com.oculusinfo.tilegen.graph.util

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._

/**
 *  Hierarchical Force-Directed layout algorithm
 *  
 *  sc = spark context
 *  maxIterations = max iterations to use for force-directed layout algorithm. Default = 500
 *  partitions = The number of partitions into which to read the raw data. Default = 0 (automatically chosen by Spark)
 *  consolidationPartitions = The number of partitions for data processing. Default= 0 (chosen based on input partitions)
 *	sourceDir = The source directory where to find clustered graph data
 * 	delimiter = Delimiter for the source graph data. Default is comma-delimited
 *  layoutDimensions = Total desired width and height of the node layout region. Default is (256.0, 256.0)
 *  borderPercent = Percent of parent bounding box to leave as whitespace between neighbouring communities during initial layout.  Default = 2 %
 *	bUseEdgeWeights = Use edge weights (if available) as part of attraction force calculation. Default = false.
 *  nodeAreaPercent = Used for hierarchical levels > 0 to determine the area of all community 'circles' within the boundingBox vs whitespace. Default is 20 percent
 *  gravity = strength of gravity force to use to prevent outer nodes from spreading out too far.  Force-directed layout only.  Default = 0.0 (no gravity)
 *  isolatedDegreeThres = degree threshold used to define 'leaf communities'.  Such leaf communities are automatically laid out in an outer radial/spiral pattern.  Default = 0
 *  communitySizeThres = community size threshold used to exclude communities with < communitySizeThres nodes from layout, in order to speed up layout of very large parent communities.
 *  					 Only used for hierarchy level > 0.  Default = 0
 *  
 **/ 
class HierarchicFDLayout extends Serializable {

	def determineLayout(sc: SparkContext,
	                    maxIterations: Int = 500,
	                    maxHierarchyLevel: Int,
	                    partitions: Int = 0,
	                    consolidationPartitions: Int = 0,
	                    sourceDir: String,
	                    delimiter: String = ",",
	                    layoutDimensions: (Double, Double) = (256.0, 256.0),
	                    borderPercent: Double = 2.0,
	                    nodeAreaPercent: Int = 30,
	                    bUseEdgeWeights: Boolean = false,
	                    gravity: Double = 0.0,
	                    isolatedDegreeThres: Int = 0,
	                    communitySizeThres: Int = 0,
	                    outputDir: String) = {
		
		//TODO -- this class assumes edge weights are Longs.  If this becomes an issue for some datasets, then change expected edge weights to Doubles?
	  		
		if (maxHierarchyLevel < 0) throw new IllegalArgumentException("maxLevel parameter must be >= 0")
		if (nodeAreaPercent < 10 || nodeAreaPercent > 90) throw new IllegalArgumentException("nodeAreaPercent parameter must be between 10 and 90")
		
		val forceDirectedLayouter = new ForceDirected()	//force-directed layout scheme
		
		var levelStats = new Array[(Long, Long, Double, Double, Double, Double, Int)](maxHierarchyLevel+1)	// (numNodes, numEdges, minR, maxR, minParentR, maxParentR, min Recommended Zoom Level)
		
		//Array of RDDs for storing all node results.  Format is (id, (x, y, radius, parentID, numInternalNodes, metaData))
		//var nodeResultsAllLevels = new Array[(RDD[(Long, (Double, Double, Double, Long, Long, String))])](maxHierarchyLevel+1)
		//Array of RDDs for storing all edge results.
		//var edgeResultsAllLevels = new Array[(RDD[Edge[Long]])](maxHierarchyLevel+1)

		// init results for 'parent group' rectangle with group ID -1 (because top hierarchical communities don't have valid parents)
		//(rectangle format is bottem-left corner, width, height of rectangle)

		//var localLastLevelLayout = Seq(-1L -> (0.0,0.0,layoutDimensions._1,layoutDimensions._2))
		var lastLevelLayout = sc.parallelize(Seq(-1L -> (0.0,0.0,layoutDimensions._1,layoutDimensions._2)))
		
		var level = maxHierarchyLevel
		while (level >= 0) {
			println("Starting Force Directed Layout for hierarchy level " + level)

			//val lastLevelLayout = sc.parallelize(localLastLevelLayout)
			
			// For each hierarchical level > 0, get community ID's, community degree (num outgoing edges),
			// and num internal nodes, and the parent community ID.
			// Group by parent community, and do Group-in-Box layout once for each parent community.
			// Then consolidate results and save in format (community id, rectangle in 'global coordinates')
			
			// parse edge data
			val gparser = new GraphCSVParser
			val rawData = if (partitions <= 0) {
				sc.textFile( sourceDir + "/level_" + level)
			} else {
				sc.textFile( sourceDir + "/level_" + level, partitions)
			}
			val edges0 = gparser.parseEdgeData(sc, rawData, partitions, delimiter, 1, 2, 3)
			
			// parse node data ... format is (nodeID, parent community ID, internal number of nodes, degree, metadata)
			val parsedNodeData0 =  if (level == maxHierarchyLevel) {
				val ndata = gparser.parseNodeData(sc, rawData, partitions, delimiter, 1, 2, 3, 4)
				//for the top hierarachy level, force the 'parentID' = to the largest community,
				// so the largest community will be placed in the centre of the graph layout
				//(and reset the 'lastLevelLayout' variable accordingly)
				val topParentID = ndata.map(n => (n._1, n._2._2)).top(1)(Ordering.by(_._2))(0)._1
				lastLevelLayout = sc.parallelize(Seq(topParentID -> (0.0,0.0,layoutDimensions._1,layoutDimensions._2)))
				
				ndata.map(node => (node._1, (topParentID, node._2._2, node._2._3, node._2._4)))	// force parentID = topParentID for top level group
			}
			else {
				gparser.parseNodeData(sc, rawData, partitions, delimiter, 1, 2, 3, 4)
			}
			
			// now create graph of parsed nodes and edges for this hierarchy, and discard any nodes/communities that ==null or are too small
			val graph = Graph(parsedNodeData0, edges0).subgraph(vpred = (id, attr) => {
				if ((attr != null) && (attr._2 > communitySizeThres || level == 0)) true else false
			})	
			val parsedNodeData = graph.vertices
			val edges = graph.edges
			edges.cache
			
			// find all intra-community edges and store with parent ID as map key
			val edgesByParent = graph.triplets.flatMap(et =>
				{
					val srcParentID = et.srcAttr._1	// parent ID for edge's source node
					val dstParentID = et.dstAttr._1	// parent ID for edge's destination node

					if (srcParentID == dstParentID) {
						// this is an INTRA-community edge (so save result with parent community ID as key)
						Iterator( (srcParentID, (et.srcId, et.dstId, et.attr)) )
					}
					else {
						// this is an INTER-community edge (so disregard for force-directed layout of leaf communities)
						Iterator.empty
					}
				}
			)
			
			val groupedEdges = if (consolidationPartitions==0) {	// group intra-community edges by parent ID
				edgesByParent.groupByKey()
			} else {
				edgesByParent.groupByKey(consolidationPartitions)
			}
			
			// now re-map nodes by (parent ID, (node ID, numInternalNodes, degree, metaData)) and group by parent rectangle
			val groupedNodes = if (consolidationPartitions==0) {
				parsedNodeData.map(n => (n._2._1, (n._1, n._2._2, n._2._3, n._2._4))).groupByKey()
			} else {
				parsedNodeData.map(n => (n._2._1, (n._1, n._2._2, n._2._3, n._2._4))).groupByKey(consolidationPartitions)
			}
			
			//join raw nodes with intra-community edges (key is parent ID), AND join with lastLevelLayout so have access to parent rectangle coords too
			val joinedData = groupedNodes.leftOuterJoin(groupedEdges).map{case (parentID, (nodeData, edgesOption)) =>
				// create a dummy edge for any communities without intra-cluster edges
				// (ie for leaf communities containing only 1 node)
				val edgeResults = edgesOption.getOrElse(Iterable( (-1L, -1L, 0L) ))
				(parentID, (nodeData, edgeResults))
			}.join(lastLevelLayout)
			
			val bUseNodeSizes = true //(level > 0)
			val g = if (level > 0) gravity else 0
			//val currAreaPercent = Math.max(nodeAreaPercent - (maxHierarchyLevel-level)*5, 10)	// use less area for communities at lower hierarchical levels

			// perform force-directed layout algorithm on all nodes and edges in a given parent community
			// note: format for nodeDataAll is (id, (x, y, radius, parentID, parentX, parentY, parentR, numInternalNodes, degree, metaData))
			val nodeDataAll = joinedData.flatMap(p =>
				{
					val parentID = p._1
					val parentRectangle = p._2._2
					// List of (node IDs, numInternalNodes, degree, node metaData) for a given community
					val communityNodes = p._2._1._1
					// List of edges (srcID, dstID, weight)
					val communityEdges = p._2._1._2
					// Note, 'nodesWithCoords' result is an array of format (ID, x, y, radius, numInternalNodes, degree, metaData)
					val nodesWithCoords = forceDirectedLayouter.run(communityNodes,
					                                                communityEdges,
					                                                parentID,
					                                                parentRectangle,
					                                                level,
					                                                borderPercent,
					                                                maxIterations,
					                                                bUseEdgeWeights,
					                                                bUseNodeSizes,
					                                                nodeAreaPercent,
					                                                g,
					                                                isolatedDegreeThres)

					// calc circle coords of parent community for saving results
					// centre of parent circle
					val (parentX, parentY) = (parentRectangle._1 + 0.5*parentRectangle._3, parentRectangle._2 + 0.5*parentRectangle._4)
					// radius of parent circle
					val parentR = Math.sqrt(Math.pow(parentX - parentRectangle._1, 2.0) + Math.pow(parentY - parentRectangle._2, 2.0))

					val nodeData = nodesWithCoords.map(i =>
						{	// add parent ID onto each record
							val (id, x, y, radius, numInternalNodes, degree, metaData) = i
							(id, (x, y, radius, parentID, parentX, parentY, parentR, numInternalNodes, degree, metaData))
						}
					)
					nodeData
				}
			)
			nodeDataAll.cache

			//			nodeResultsAllLevels(level) = nodeDataAll	// store node and edge results for this hierarchy level
			//			nodeResultsAllLevels(level).cache
			//			edgeResultsAllLevels(level) = edges
			//			edgeResultsAllLevels(level).cache

			val graphForThisLevel = Graph(nodeDataAll, edges)	// create a graph of the layout results for this level

			levelStats(level) = calcLayoutStats(graphForThisLevel.vertices.count,	// calc some overall stats about layout for this level
			                                    graphForThisLevel.edges.count,
			                                    graphForThisLevel.vertices.map(n => try { n._2._3 } catch { case _: Throwable => { 0.0 }} ),	// get community radii
			                                    graphForThisLevel.vertices.map(n => try { n._2._7 } catch { case _: Throwable => { 0.0 }} ),	// get parent radii
			                                    Math.min(layoutDimensions._1, layoutDimensions._2),
			                                    level == maxHierarchyLevel)

			// save layout results for this hierarchical level
			saveLayoutResults(graphForThisLevel, outputDir, level, level == maxHierarchyLevel)
			
			if (level > 0) {
				val levelLayout = nodeDataAll.map(data =>
					{
						// convert x,y coords and community radius of this community to a square bounding box for next hierarchical level
						val dataCircle = (data._1, data._2._1, data._2._2, data._2._3)
						val rect = circleToRectangle(dataCircle)
						rect
					}
				)
				
				//localLastLevelLayout = levelLayout.collect
				levelLayout.cache
				levelLayout.count
				lastLevelLayout.unpersist(blocking=false)
				lastLevelLayout = levelLayout
			}
			nodeDataAll.unpersist(blocking=false)
			edges.unpersist(blocking=false)
			level -= 1
		}
		
		saveLayoutStats(sc, levelStats, outputDir)	// save layout stats for all hierarchical levels
		
		//---- For each hierarchy level, append the raw coords for the 'primary node' of each community
		//		val rawNodeCoords = nodeResultsAllLevels(0).map(n => (n._1, (n._2._1, n._2._2)))	//store (id (x,y)) of all raw nodes
		//		rawNodeCoords.cache
		//
		//		level = maxHierarchyLevel
		//		while (level >= 0) {
		//
		//			val finalNodeData = if (level == maxHierarchyLevel) {
		//
		//				nodeResultsAllLevels(level).map(n => {
		//					// parent coords are not applicable for top level of hierarchy so save as in 0,0
		//					val (id, (x, y, r, parentId, numInternalNodes, metaData)) = n
		//					(id, (x, y, r, parentId, 0.0, 0.0, numInternalNodes, metaData))
		//				})
		//			}
		//			else {
		//
		//				// reformat node data for this level so parentId is key, and join with raw node coords
		//				val nodesXY = nodeResultsAllLevels(level).map(n => {
		//					val (id, (x, y, r, parentId, numInternalNodes, metaData)) = n
		//					(parentId, (id, x, y, r, numInternalNodes, metaData))
		//				}).join(rawNodeCoords)
		//
		//				nodesXY.map(n => {	// re-map data so nodeID is key
		//					val (parentId, ((id, x, y, r, numInternalNodes, metaData), (parentX, parentY))) = n
		//					(id, (x, y, r, parentId, parentX, parentY, numInternalNodes, metaData))
		//				})
		//			}
		//
		//			val graphForThisLevel = Graph(finalNodeData, edgeResultsAllLevels(level))	// create a graph of the layout results for this level
		//			saveLayoutResults(graphForThisLevel, outputDir, level, level == maxHierarchyLevel)	// save layout results for this hierarchical level
		//
		//			nodeResultsAllLevels(level).unpersist(blocking=false)
		//			edgeResultsAllLevels(level).unpersist(blocking=false)
		//
		//			level -= 1
		//		}
		//
		//		rawNodeCoords.unpersist(blocking=false)
	}

	//----------------------
	// For a node location, take the x,y coords and radius, and convert to a bounding box (square) contained
	// within the circle (square diagonal == circle diameter).  To be used as a bounding box for the FD layout of the next hierarchical level communities
	private def circlesToRectangles(nodeCoords: Array[(Long, Double, Double, Double)]): Iterable[(Long, (Double, Double, Double, Double))] = {
		val squares = nodeCoords.map(n => {
			                             circleToRectangle(n)
		                             })
		squares
	}
	
	private def circleToRectangle(nodeCoords: (Long, Double, Double, Double)): (Long, (Double, Double, Double, Double)) = {
		val (id, x, y, r) = nodeCoords
		// calc coords of bounding box with same centre as the circle, and width = height = sqrt(2)*r
		val rSqrt2 = r*0.70711	// 0.70711 = 1/sqrt(2)
		val squareCoords = (x - rSqrt2, y - rSqrt2, 2.0*rSqrt2, 2.0*rSqrt2)	// (x,y of left-bottem corner, width, height)
		(id, squareCoords)
	}
	
	private def calcLayoutStats(numNodes: Long,
	                            numEdges: Long,
	                            radii: RDD[Double],
	                            parentRadii: RDD[Double],
	                            totalLayoutLength: Double,
	                            bMaxHierarchyLevel: Boolean): (Long, Long, Double, Double, Double, Double, Int) = {
		
		val maxR = radii.reduce(_ max _)	// calc min and max radii
		val minR = radii.reduce(_ min _)
		val maxParentR = parentRadii.reduce(_ max _)	// calc min and max parent radii
		val minParentR = parentRadii.reduce(_ min _)
		
		val minRecommendedZoomLevel = if (bMaxHierarchyLevel) {
			0
		}
		else {
			// use max parent radius to give a min recommended zoom level for this hierarchy
			// (ideally want parent radius to correspond to approx 1 tile length)
			(Math.round(Math.log(totalLayoutLength/maxParentR)*1.4427)).toInt	// 1.4427 = 1/log(2), so equation = log2(layoutlength/maxParentR)
		}

		//output format is (numNodes, numEdges, minR, maxR, minParentR, maxParentR, min Recommended Zoom Level)
		(numNodes, numEdges, minR, maxR, minParentR, maxParentR, minRecommendedZoomLevel)
	}
	
	private def saveLayoutResults(graphWithCoords: Graph[(Double, Double, Double, Long, Double, Double, Double, Long, Int, String), Long],
	                              outputDir: String,
	                              level: Int, bIsMaxLevel: Boolean) =	{
		
		// re-format results into tab-delimited strings for saving to text file
		val resultsNodes = graphWithCoords.vertices.map(node =>
			{
				try {
					val (id, (x, y, radius, parentID, parentX, parentY, parentR, numInternalNodes, degree, metaData)) = node

					("node\t" + id + "\t" + x + "\t" + y + "\t" + radius + "\t" + parentID + "\t" + parentX + "\t" + parentY + "\t" + parentR + "\t" + numInternalNodes + "\t" + degree + "\t" + metaData)
				}
				catch {
					case _: Throwable => null
				}
			}
		).filter(line => line != null)

		val resultsEdges = graphWithCoords.triplets.map(et =>
			{
				try {
					val srcID = et.srcId
					val dstID = et.dstId
					// nodeAttributes are of format ((x, y, radius, numInternalNodes), parentCircle)
					val srcCoords = (et.srcAttr._1, et.srcAttr._2)
					val dstCoords = (et.dstAttr._1, et.dstAttr._2)
					// is this an inter-community edge (same parentID for src and dst)
					val interCommunityEdge = if ((et.srcAttr._4 != et.dstAttr._4) || bIsMaxLevel) 1 else 0

					("edge\t" + srcID + "\t" + srcCoords._1 + "\t" + srcCoords._2 + "\t" + dstID + "\t" + dstCoords._1 + "\t" + dstCoords._2 + "\t" + et.attr + "\t" + interCommunityEdge)
				}
				catch {
					case _: Throwable => null
				}
			}
		).filter(line => line != null)

		val resultsAll = resultsNodes.union(resultsEdges)	// put both node and edge results into one RDD

		resultsAll.saveAsTextFile(outputDir+"/level_"+level)	// save results to outputDir + "level_#"
	}


	private def saveLayoutStats(sc: SparkContext, stats: Array[(Long, Long, Double, Double, Double, Double, Int)], outputDir: String) = {

		// re-format results into strings for saving to text file
		var level = stats.size-1
		var statsStrings = new Array[(String)](stats.size)
		while (level >= 0) {
			val (numNodes, numEdges, minR, maxR, minParentR, maxParentR, minRecommendedZoomLevel) = stats(level)

			statsStrings(level) = ("hierarchical level: " + level + ", min recommended zoom level: " + minRecommendedZoomLevel
				                       + ", nodes: " + numNodes
				                       + ", edges: " + numEdges
				                       + ", min radius: " + minR
				                       + ", max radius: " + maxR
				                       + ", min parent radius: " + minParentR
				                       + ", max parent radius: " + maxParentR)

			level -= 1
		}

		sc.parallelize(statsStrings, 1).saveAsTextFile(outputDir+"/stats")
	}
}
