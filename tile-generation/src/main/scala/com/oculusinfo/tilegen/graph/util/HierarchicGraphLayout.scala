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
//import scala.reflect.ClassTag
//import org.apache.spark.HashPartitioner
//import scala.collection.mutable.ArrayBuffer
//import scala.util.Random

/**
 *  Hierarchical graph layout algorithm
 *  
 *  sc = spark context
 *  maxIterations = max iterations to use for force-directed layout algorithm. Default = 500
 *  partitions = The number of partitions into which to read the raw data. Default = 0 (automatically chosen by Spark)
 *  consolidationPartitions = The number of partitions for data processing. Default= 0 (chosen based on input partitions)
 *	sourceDir = The source directory where to find clustered graph data
 * 	delimiter = Delimiter for the source graph data. Default is comma-delimited
 *  layoutDimensions = Total desired width and height of the node layout region. Default is (256.0, 256.0)
 *	borderOffset = percent of boundingBox width and height to leave as whitespace when laying out leaf nodes.  Default is 5 percent
 *	numNodesThres = threshold used to determine when to layout underlying communities/nodes with GIB or FD layout.  Default is 1000 nodes
 **/ 
class HierarchicGraphLayout extends Serializable {

	def determineLayout(sc: SparkContext, 
						maxIterations: Int = 500, 
						maxHierarchyLevel: Int, 
						partitions: Int = 0,
						consolidationPartitions: Int = 0,
						sourceDir: String,
						delimiter: String = ",",
						layoutDimensions: (Double, Double) = (256.0, 256.0),
						borderOffset: Int = 5,
						numNodesThres: Int = 1000): RDD[(Long, Double, Double)] = {	
		
		
		if (maxHierarchyLevel < 0) throw new IllegalArgumentException("maxLevel parameter must be >= 0") 
		
		val boxLayouter = new GroupInBox()	//group-in-a-box layout scheme
				
		// init results for 'parent group' rectangle with group ID 0   (rectangle format is bottem-left corner, width, height of rectangle) 
		var localLastLevelLayout = Seq(0L -> (0.0,0.0,layoutDimensions._1,layoutDimensions._2))  
		//var lastLevelLayout = sc.parallelize(Seq(0L -> (0.0,0.0,layoutDimensions._1,layoutDimensions._2)))
		
		var level = maxHierarchyLevel
		while (level > 0) {
			println("Starting GroupInBox Layout for hierarchy level " + level)

			val lastLevelLayout = sc.parallelize(localLastLevelLayout)
			// For each hierarchical level > 0, get community ID's, community degree (num outgoing edges), 
			// and num internal nodes, and the parent community ID.
			// Group by parent community, and do Group-in-Box layout once for each parent community.
			// Then consolidate results and save in format (community id, rectangle in 'global coordinates') 

			// parse node data ... and re-format as (parent communityID, (communityID,numInternalNodes, community degree))
			val parsedNodeData = parseNodeData(sc, sourceDir + "/level_" + level + "_vertices",	partitions, delimiter)
			val groups = if (level == maxHierarchyLevel) {
				parsedNodeData.map(node => (0L, (node._1, node._2._2, node._2._3)))	// force parentGroupID = 0L for top level group		
			}
			else {
				parsedNodeData.map(node => (node._2._1, (node._1, node._2._2, node._2._3)))	
			}		
						
			val groupsByParent = if (consolidationPartitions==0) {		// group by parent community ID
				groups.groupByKey()
			} else {
				groups.groupByKey(consolidationPartitions)
			}
			
			val joinedGroups = groupsByParent.join(lastLevelLayout)
//			joinedGroups.cache
//			joinedGroups.count
//			lastLevelLayout.unpersist(blocking=false)
			
			val levelLayout = joinedGroups.flatMap(n => {
				val data = n._2._1
				val parentRectangle = n._2._2
				//data format is (parent communityID, Iterable(communityID,numInternalNodes, community degree))				
				val rects = boxLayouter.run(data, parentRectangle, numNodesThres)
				rects
			})
			
			localLastLevelLayout = levelLayout.collect
			level -= 1
		}
				
		// Do Level 0...
		// For lowest hierarchical level, get raw node ID's, parent community ID's, 
		// and corresponding intra-community edges for each parent community
		// Group nodes and intra-community edges by parent community, and do Force-directed layout for 
		// each parent community.
		// Then consolidate results and save final results in format (node id, node location in 'global coordinates') 

		println("Starting Force Directed Layout for hierarchy level 0")
		
		val forceDirectedLayouter = new ForceDirected()	//force-directed layout scheme
		
		val lastLevelLayout = sc.parallelize(localLastLevelLayout)

		// parse edge data
		val edges = parseEdgeData(sc, sourceDir + "/level_" + level + "_edges", partitions, delimiter)
		
		// parse node data ... format is (node ID, parent community ID)
		val nodes = parseNodeData(sc, sourceDir + "/level_" + level + "_vertices", partitions, delimiter)
					.map(node => (node._1, node._2._1))
					
		// swap so parent ID is the key, and raw node ID is the value, join with parent rectangle, and store
		// as (node ID, parent rect)
		val nodesWithRectangles = nodes.map(n => (n._2, n._1))
									   .join(lastLevelLayout)
									   .map(n => { 
									  	   val id = n._2._1
									  	   //val parentId = n._1
									  	   val parentRect = n._2._2
									  	   (id, parentRect)
									   })
									   
		val graph = Graph(nodesWithRectangles, edges)	// create graph with parent rectangle as Vertex attribute

		// find all intra-community edges and store with parent rectangle as map key
		val edgesByRect = graph.triplets.flatMap(et => {
			val srcParentRect = et.srcAttr	// parent rect for edge's source node
			val dstParentRect = et.dstAttr	// parent rect for edge's destination node
			
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
		
		// now re-map nodes by (parent rect, node ID) and group by parent rectangle
		val groupedNodes = if (consolidationPartitions==0) {	
			nodesWithRectangles.map(n => (n._2, n._1)).groupByKey()
		} else {
			nodesWithRectangles.map(n => (n._2, n._1)).groupByKey(consolidationPartitions)
		}
			
		//join raw nodes with intra-community edges (key is parent rectangle)
		val joinedData = groupedNodes.leftOuterJoin(groupedEdges).map({case (parentRect, (nodeIDs, edgesOption)) =>
			// create a dummy edge for any communities without intra-cluster edges
			// (ie for leaf communities containing only 1 node)
			val edgeResults = edgesOption.getOrElse(Iterable( (-1L, -1L, 0L) ))
			(parentRect, (nodeIDs, edgeResults))
		})
		
		// perform force-directed layout algorithm on all nodes and edges in a given parent rectangle
		val finalNodeCoords = joinedData.flatMap(p => {
			val parentRectangle = p._1
			val communityNodes = p._2._1		// List of raw node IDs in a given community
			val communityEdges = p._2._2 
			val coords = forceDirectedLayouter.run(communityNodes, communityEdges, parentRectangle, borderOffset, maxIterations)
			coords
		})				
					
		finalNodeCoords	
	}

	//----------------------
	// Parse edge data for a given hierarchical level 
	//(assumes graph data has been louvain clustered using the spark-based graph clustering utility)	
	private def parseEdgeData(sc: SparkContext, edgeDir: String, partitions: Int, delimiter: String): RDD[Edge[Long]] = {
		val rawEdgeData = if (partitions <= 0) {
			sc.textFile(edgeDir)
		} else {
			sc.textFile(edgeDir, partitions)
		}
	
		val edges = rawEdgeData.map(row => {
			val row2 = row.substring(row.find("(")+1, row.find(")"))	// keep only data in between ( ) on each row
			val tokens = row2.split(delimiter).map(_.trim())		// parse using delimiter
			val len = tokens.length
			tokens.length match {
				case 2 => { new Edge(tokens(0).toLong, tokens(1).toLong, 1L) }					//unweighted edges
				case 3 => { new Edge(tokens(0).toLong, tokens(1).toLong, tokens(2).toLong) }	//weighted edges
				case _ => { throw new IllegalArgumentException("invalid input line: " + row) }
			}
		})
		edges
	}
	
	//----------------------
	// Parse node/community data for a given hierarchical level 
	//(assumes graph data has been louvain clustered using the spark-based graph clustering utility)
	private def parseNodeData(sc: SparkContext, nodeDir: String, partitions: Int, delimiter: String): RDD[(Long, (Long, Long, Int))] = {
		val rawNodeData = if (partitions <= 0) {
			sc.textFile(nodeDir)
		} else {
			sc.textFile(nodeDir, partitions)
		}
	
		val nodes = rawNodeData.map(row => {
			val row2 = row.substring(row.find("(")+1, row.find(")"))	// keep only data in between ( ) on each row
			val tokens = row2.split(delimiter)						// parse using delimiter
			if (tokens.length < 7) throw new IllegalArgumentException("invalid input line: " + row)
			val id = tokens(0).trim.toLong													// community ID
			val parentCommunity = tokens(1).substring(tokens(1).find(":")+1).trim.toLong	// ID of 'parent' community (one hierarchical level up)
			val internalNodes = tokens(4).substring(tokens(4).find(":")+1).trim.toLong		// number of internal nodes in this community		
			val nodeDegree = tokens(6).substring(tokens(6).find(":")+1).trim.toInt			// edge degree for this community		
			(id, (parentCommunity, internalNodes, nodeDegree))
		})
		nodes
	}
			
			

}