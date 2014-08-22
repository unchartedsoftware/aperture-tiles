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
 *	borderOffset = (CURRENTLY NOT IN USE) percent of boundingBox width and height to leave as whitespace when laying out leaf nodes.  Default is 5 percent
 *	numNodesThres = (CURRENTLY NOT IN USE) threshold used to determine when to layout underlying communities within a single force-directed layout task.  Default is 1000 nodes
 *  nodeAreaPercent = Used for hierchical levels > 0 to determine the area of all node 'circles' within the boundingBox vs whitespace. Default is 20 percent
 *  gravity = strength of gravity force to use to prevent outer nodes from spreading out too far.  Force-directed layout only.  Default = 0.0 (no gravity)
 * **/ 
class HierarchicFDLayout extends Serializable {

	def determineLayout(sc: SparkContext, 
						maxIterations: Int = 500, 
						maxHierarchyLevel: Int, 
						partitions: Int = 0,
						consolidationPartitions: Int = 0,
						sourceDir: String,
						delimiter: String = ",",
						layoutDimensions: (Double, Double) = (256.0, 256.0),
						//borderOffset: Int = 0,
						//numNodesThres: Int = 1000
						nodeAreaPercent: Int = 20,
						bUseEdgeWeights: Boolean = false,
						gravity: Double = 0.0,
						outputDir: String
						) = {	
		
		//TODO -- this class assumes edge weights are Longs.  If this becomes an issue for some datasets, then change expected edge weights to Doubles? 
		//TODO -- numNodesThres not currently used for FD hierarchical layout (could add it in later?)
		val borderOffset = 0	//TODO -- borderOffset not currently used for FD hierarchical layout (could add it in later?)
		
		if (maxHierarchyLevel < 0) throw new IllegalArgumentException("maxLevel parameter must be >= 0") 
		
		val forceDirectedLayouter = new ForceDirected()	//force-directed layout scheme

		// init results for 'parent group' rectangle with group ID 0   (rectangle format is bottem-left corner, width, height of rectangle) 		
		//var localLastLevelLayout = Seq(0L -> (0.0,0.0,layoutDimensions._1,layoutDimensions._2))
		var lastLevelLayout = sc.parallelize(Seq(0L -> (0.0,0.0,layoutDimensions._1,layoutDimensions._2)))
		
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
			val edges = gparser.parseEdgeData(sc, rawData, partitions, delimiter, 1, 2, 3)
		
			// parse node data ... and re-format as (parent community ID, nodeID, internal number of nodes)
			val parsedNodeData = gparser.parseNodeData(sc, rawData, partitions, delimiter, 1, 2, 3, 4)
			val nodes = if (level == maxHierarchyLevel) {
				parsedNodeData.map(node => (0L, (node._1, node._2._2)))	// force parentGroupID = 0L for top level group		
			}
			else {
				parsedNodeData.map(node => (node._2._1, (node._1, node._2._2)))	
			}				
					
			// join nodes with parent rectangle, and store as (node ID, parent rect, numInternalNodes)
			val nodesWithRectangles = nodes.join(lastLevelLayout)
										   .map(n => { 
										  	   val id = n._2._1._1
										  	   val numInternalNodes = n._2._1._2
										  	   //val parentId = n._1
										  	   val parentRect = n._2._2
										  	   (id, (parentRect, numInternalNodes))
										   })						   
										  
			val graph = Graph(nodesWithRectangles, edges)	// create graph with parent rectangle as Vertex attribute

			// find all intra-community edges and store with parent rectangle as map key
			val edgesByRect = graph.triplets.flatMap(et => {
				val srcParentRect = et.srcAttr._1	// parent rect for edge's source node
				val dstParentRect = et.dstAttr._1	// parent rect for edge's destination node
				
				if (srcParentRect == dstParentRect) {
					// this is an INTRA-community edge (so save result with parent community ID as key)
					Iterator( (srcParentRect, (et.srcId, et.dstId, et.attr)) )
				}
				else {
					// this is an INTER-community edge (so disregard for force-directed layout of leaf communities)
					Iterator.empty
				}		
			})
		
			val groupedEdges = if (consolidationPartitions==0) {	// group intra-community edges by parent rectangle
				edgesByRect.groupByKey()
			} else {
				edgesByRect.groupByKey(consolidationPartitions)
			}
		
			// now re-map nodes by (parent rect, (node ID, numInternalNodes)) and group by parent rectangle
			val groupedNodes = if (consolidationPartitions==0) {	
				nodesWithRectangles.map(n => (n._2._1, (n._1, n._2._2))).groupByKey()
			} else {
				nodesWithRectangles.map(n => (n._2._1, (n._1, n._2._2))).groupByKey(consolidationPartitions)
			}
			
			//join raw nodes with intra-community edges (key is parent rectangle)
			val joinedData = groupedNodes.leftOuterJoin(groupedEdges).map({case (parentRect, (nodeData, edgesOption)) =>
				// create a dummy edge for any communities without intra-cluster edges
				// (ie for leaf communities containing only 1 node)
				val edgeResults = edgesOption.getOrElse(Iterable( (-1L, -1L, 0L) ))
				(parentRect, (nodeData, edgeResults))
			})
		
			// perform force-directed layout algorithm on all nodes and edges in a given parent rectangle
			val bUseNodeSizes = (level > 0)
		    val g = if (level > 0) gravity else 0
			val currAreaPercent =  if (level > 0) Math.min(nodeAreaPercent + (level-1)*5, 80)	// use more area for communities at higher hierarchical levels	// TODO -- test this further!
								   else 20

			val nodeDataAll = joinedData.flatMap(p => {
				val parentRectangle = p._1
				val communityNodes = p._2._1		// List of raw node IDs and internal number of nodes for a given community (Long, Long)
				val communityEdges = p._2._2 
				val coords = forceDirectedLayouter.run(communityNodes, 
													   communityEdges, 
													   parentRectangle, 
													   borderOffset, 
													   maxIterations,
													   bUseEdgeWeights,
													   bUseNodeSizes,
													   currAreaPercent,
													   g)
													  								   				
				// calc circle coords of parent community for saving results
				val parentRadius = Math.max(parentRectangle._3, parentRectangle._4)*1.41421	// 1.41421 = sqrt(2)
				val parentCircle = (parentRectangle._1 + 0.5*parentRectangle._3, parentRectangle._2 + 0.5*parentRectangle._4, parentRadius)
				
				val nodeData = coords.map(i => {	// append parent community info onto end of record for each node (so can save parent community info too)
					val (id, x, y, radius, numInternalNodes) = i
					(id, ((x, y, radius, numInternalNodes), parentCircle))
				})
				nodeData
			})
			
			val graphForThisLevel = Graph(nodeDataAll, edges)	// create a graph of the layout results for this level
			
			saveLayoutResults(graphForThisLevel, outputDir, level)	// save layout results for this hierarchical level
			
			if (level > 0) {
				val levelLayout = nodeDataAll.map(data => {
					// convert x,y coords and community radius of this community to a square bounding box for next hierarchical level
					val dataCircle = (data._1, data._2._1._1, data._2._1._2, data._2._1._3)
					val rect = circleToRectangle(dataCircle)	
					rect
				})
				
				//localLastLevelLayout = levelLayout.collect
				levelLayout.cache
				levelLayout.count
				lastLevelLayout.unpersist(blocking=false)
				lastLevelLayout = levelLayout
			}
						
			level -= 1
		}								
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
	
	private def saveLayoutResults(graphWithCoords: Graph[((Double, Double, Double, Long),(Double, Double, Double)), Long],
								outputDir: String,
								level: Int)	 {
		
		//TODO need to put in saving node metadata as well!
		
		// re-format results into tab-delimited strings for saving to text file											
		val resultsNodes = graphWithCoords.vertices.map(node => {
			val (id, ((x, y, radius, numInternalNodes), parentCircle)) = node
			
			("node\t" + id + "\t" + x + "\t" + y + "\t" + radius + "\t" + numInternalNodes + "\t" + parentCircle._1 + "\t" + parentCircle._2 + "\t" + parentCircle._3)
		})

		val resultsEdges = graphWithCoords.triplets.map(et => {
			val srcID = et.srcId
			val dstID = et.dstId
			// nodeAttributes are of format ((x, y, radius, numInternalNodes), parentCircle)
			val srcCoords = (et.srcAttr._1._1, et.srcAttr._1._2)
			val dstCoords = (et.dstAttr._1._1, et.dstAttr._1._2)
			
			("edge\t" + srcID + "\t" + srcCoords._1 + "\t" + srcCoords._2 + "\t" + dstID + "\t" + dstCoords._1 + "\t" + dstCoords._2 + "\t" + et.attr)
		})
				
		val resultsAll = resultsNodes.union(resultsEdges)	// put both node and edge results into one RDD
		
		resultsAll.saveAsTextFile(outputDir+"/level_"+level)	// save results to outputDir + "level_#"
	}

}