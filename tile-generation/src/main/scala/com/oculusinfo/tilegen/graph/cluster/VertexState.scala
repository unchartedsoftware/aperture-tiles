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

/**
 * Louvain vertex state
 * Contains all information needed for louvain community detection
 * 
 * Code adapted from Sotera's graphX implementation of the distributed Louvain modularity algorithm
 * https://github.com/Sotera/spark-distributed-louvain-modularity
 */
class VertexState extends Serializable{

	var community = -1L
	var communitySigmaTot = 0L
	var internalWeight = 0L  	// self edges
	var nodeWeight = 0L  		// out degree
	var internalNodes = 1L	// number of internal nodes (unweighted)
	var nodeDegree = 0		// out degree (unweighted)
	var extraAttributes = ""	// extra node attributes
	var changed = false
	
	override def toString(): String = {
		"community:"+community+",communitySigmaTot:"+communitySigmaTot+
		",internalWeight:"+internalWeight+
		",internalNodes:"+internalNodes+
		",nodeWeight:"+nodeWeight+
		",nodeDegree:"+nodeDegree+
		",extraAttributes:"+extraAttributes
	}
}
