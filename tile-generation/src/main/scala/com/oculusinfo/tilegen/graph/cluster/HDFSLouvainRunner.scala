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

package com.oculusinfo.tilegen.graph.cluster

import org.apache.spark.SparkContext
import org.apache.spark.graphx._
import scala.Array.canBuildFrom

/**
 * Execute the louvain algorithim and save the vertices and edges in hdfs at each level.
 * Can also save locally if in local mode.
 * 
 * See LouvainHarness for algorithm details
 * 
 * Code adapted from Sotera's graphX implementation of the distributed Louvain modularity algorithm
 * https://github.com/Sotera/spark-distributed-louvain-modularity
 */
class HDFSLouvainRunner(minProgressFactor:Double,progressCounter:Int,outputdir:String) extends LouvainHarness(minProgressFactor:Double,progressCounter:Int){

	var qValues = Array[(Int,Double)]()
	
	override def saveLevel(sc:SparkContext,level:Int,q:Double,graph:Graph[VertexState,Long]) = {
		//graph.vertices.saveAsTextFile(outputdir+"/level_"+level+"_vertices")
		//graph.edges.saveAsTextFile(outputdir+"/level_"+level+"_edges")

		// re-format results into tab-delimited strings for saving to text file
		val resultsNodes = graph.vertices.map(node =>
			{
				val (id, state) = node
				val parentId = state.community			// ID of parent community
				val internalNodes = state.internalNodes	// number of raw internal nodes in this community
				val nodeDegree = state.nodeDegree		// number of inter-community edges (unweighted)
				val extraAttributes = state.extraAttributes	// additional node attributes (labels, etc.)
					//val nodeWeight = state.nodeWeight		// weighted inter-community edges
					//val internalWeight = state.internalWeight	// weighted self-edges (ie intra-community edges)
					//val sigmaTot = state.communitySigmaTot	// analogous to modularity for this community
					
				("node\t" + id + "\t" + parentId + "\t" + internalNodes + "\t" + nodeDegree + "\t" + extraAttributes)
			}
		)

		val resultsEdges = graph.edges.map(et =>
			{
				val srcID = et.srcId
				val dstID = et.dstId
				//val srcCoords = et.srcAttr
				//val dstCoords = et.dstAttr
				//("edge\t" + srcID + "\t" + srcCoords._1 + "\t" + srcCoords._2 + "\t" + dstID + "\t" + dstCoords._1 + "\t" + dstCoords._2 + "\t" + et.attr)
				("edge\t" + srcID + "\t" + dstID + "\t" + et.attr)
			}
		)
		
		val resultsAll = resultsNodes.union(resultsEdges)	// put both node and edge results into one RDD
		
		resultsAll.saveAsTextFile(outputdir+"/level_"+level)	// save results to a separate sub-dir for each level
		
		// save the q values at each level
		qValues = qValues :+ ((level,q))
		println(s"qValue: $q")
		sc.parallelize(qValues, 1).saveAsTextFile(outputdir+"/level_"+level+"_qvalues")
	}
}
