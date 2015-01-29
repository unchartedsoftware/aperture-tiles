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

import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._


// Class for inserting edge info into appropriate GraphCommunity objects.  Used for Graph Analytics.

class EdgeMatcher extends Serializable {
	
	/**
	 * 	matchEdgesWithCommunities
	 *  
	 *  Function for inserting edge info into appropriate GraphCommunity object
	 *
	 *  - communities = RDD of node communities for a given hierarchical level. 
	 *  				Expected format is (node ID, GraphCommunity obj)) 
	 *  - edges 	  = RDD of edges for a given hierarchical level.  
	 *  				Expected format is graphx.Edge[(weight, bIsInterCommunityEdge)] 
	 *      
	 *  Output format is RDD with ((x-coord,y-coord), GraphCommunity)    
	 **/
	def matchEdgesWithCommunities(communities: RDD[(Long, GraphCommunity)],
	                              edges: RDD[Edge[(Long, Boolean)]]):
			RDD[((Double, Double), GraphCommunity)] = {
		
		val graph = Graph(communities, edges)	// create graph of communities and edges
		
		// map across all edge triplets...
		val edgesTwice = graph.triplets.flatMap(et =>
			{
				val srcID = et.srcAttr.getID
				val srcCoords = ((et.srcAttr.getCoords.getFirst).toDouble, (et.srcAttr.getCoords.getSecond).toDouble)

				val dstID = et.dstAttr.getID
				val dstCoords = ((et.dstAttr.getCoords.getFirst).toDouble, (et.dstAttr.getCoords.getSecond).toDouble)

				val (weight, isInterEdge) = et.attr

				// Need to store info for each edge twice in preparation for join operation below
				// (i.e., store with each of source and dest node ID's as key)
				Iterator((srcID, (dstID, dstCoords, weight, isInterEdge)), (dstID, (srcID, srcCoords, weight, isInterEdge)))
			}
		)

		val groupedEdges = edgesTwice.groupByKey	// group edges by key nodeID

		val communitiesWithEdgeInfo = communities.leftOuterJoin(groupedEdges).map{case (parentID, (community, edgesOption)) =>
			// create a dummy edge to account for any communities without edges,
			// and/or if edge data wasn't included with this graph analytics tile-gen job
			val edgeResults = edgesOption.getOrElse(Iterable((-1L, (0.0, 0.0), -1L, false)))

			var currentCommunity = community
			val xy = ((community.getCoords.getFirst).toDouble, (community.getCoords.getSecond).toDouble)

			// add edges to this GraphCommunity
			edgeResults.foreach(e =>
				{
					val (dstID, (dstX, dstY), weight, isInterEdge) = e
					if ((dstID != -1L) && (dstID != currentCommunity.getID)) {
						val gEdge = new GraphEdge(dstID, dstX, dstY, weight)
						if (isInterEdge) {
							currentCommunity.addInterEdgeToCommunity(gEdge)
						}
						else {
							currentCommunity.addIntraEdgeToCommunity(gEdge)
						}
					}
				}
			)

			(xy, currentCommunity)
		}

		communitiesWithEdgeInfo
	}
}
