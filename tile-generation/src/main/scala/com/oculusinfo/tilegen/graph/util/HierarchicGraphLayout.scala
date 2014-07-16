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
 */ 
class HierarchicGraphLayout extends Serializable {

	def determineLayout(sc: SparkContext, 
						maxIterations: Int, 
						maxHierarchyLevel: Int, 
						partitions: Int = 0,
						consolidationPartitions: Int = 0,
						sourceDir: String,
						delimiter: String) = {//: VertexRDD[(Double, Double)] = {	
		
		if (maxHierarchyLevel < 0) throw new IllegalArgumentException("maxLevel parameter must be >= 0") 
		
		val boxLayouter = new GroupInBox()
		
		//start at highest level,
		//	get ID's, degrees, and internal nodes -- do GIB algo once, and store results as (id, rectangle global_coords)
		val screenW = 1024.0	// total width of node layout region (arbitrary units)
		val screenH = 1024.0	// total height of node layout region 	// TODO ... expose API parameters for this?
		//var lastLevelLayout = sc.parallelize(Seq(0L -> (0.0,0.0,screenW,screenH)))	// init results for 'parent group' rectangle with group ID 0
																					// bottem-left corner, width, heigth of rectangle 
		var localLastLevelLayout = Seq(0L -> (0.0,0.0,screenW,screenH))
		
		var totalNumNodes = 0L
		var level = maxHierarchyLevel
		while (level > 0) {
			println("Starting GroupInBox Layout for hierarchy level " + level)

			val lastLevelLayout = sc.parallelize(localLastLevelLayout)
			//do next level down ...
			// 	get ID's, degrees, internal nodes AND L+1 community level -- group by L+1 community level, do GIB algo once per
			//	L+1 community, then consolidate results and save in format (id, rectangle global_coords)
			// ...
			
			// parse node data ... and re-format as (parent communityID, (communityID,numInternalNodes, community degree))
			val parsedNodeData = parseNodeData(sc, sourceDir + "/level_" + level + "_vertices",	partitions, delimiter)
			val groups = if (level == maxHierarchyLevel) {
				parsedNodeData.map(node => (0L, (node._1, node._2._2, node._2._3)))	// force parentGroupID = 0L for top level group		
			}
			else {
				parsedNodeData.map(node => (node._2._1, (node._1, node._2._2, node._2._3)))	
			}
	
			if (level == maxHierarchyLevel) {
				// calc total number of nodes in graph (total should be the same for all levels, so only need to do once)
				totalNumNodes = groups.map(_._2._2).reduce(_ + _)	
			}			
						
			val groupsByParent = if (consolidationPartitions==0) {
				groups.groupByKey()
			} else {
				groups.groupByKey(consolidationPartitions)
			}
			
			//val countG = groupsByParent.count		//temp
			//val countL = lastLevelLayout.count	//temp
			
			val joinedGroups = groupsByParent.join(lastLevelLayout)
//			joinedGroups.cache
//			joinedGroups.count
//			lastLevelLayout.unpersist(blocking=false)
			
			//val countJ = joinedGroups.count		//temp
			
			val levelLayout = joinedGroups.flatMap(n => {
				val data = n._2._1
				val parentRectangle = n._2._2
				//data format is (parent communityID, Iterable(communityID,numInternalNodes, community degree))				
				val rects = boxLayouter.run(data, totalNumNodes, parentRectangle)
				rects
			})
			//val countL2 = levelLayout.count	//temp
			
			localLastLevelLayout = levelLayout.collect
			level -= 1
		}
				
		// TODO .... do level=0 here!!!
		//do Level 0...
		//  get ID's, degrees, internal nodes AND L+1 community level -- group by L+1 community level
		//	do Force-directed algo once per L+1 community, then consolidate results and save in final format (id, id x,y coords)
		// parse edge data
		var edges = parseEdgeData(sc, sourceDir + "/level_" + level + "_edges", partitions, delimiter)
		
		// parse node data ... and re-format as (parent communityID, (communityID, numInternalNodes, community degree))
		var nodes = parseNodeData(sc, sourceDir + "/level_" + level + "_vertices", partitions, delimiter)
						.map(node => (node._2._1, (node._1, node._2._2, node._2._3)))
						
		if ((consolidationPartitions > 0) && (consolidationPartitions != partitions)) {
			// re-partition the data into 'consolidationPartitions' partitions
			edges = edges.coalesce(consolidationPartitions, shuffle = true)	
			nodes = nodes.coalesce(consolidationPartitions, shuffle = true)			
		}
		
		println("Starting Force Directed Layout for hierarchy level 0")
		
//TODO		doForceDirectedLayout(nodes)	
		
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