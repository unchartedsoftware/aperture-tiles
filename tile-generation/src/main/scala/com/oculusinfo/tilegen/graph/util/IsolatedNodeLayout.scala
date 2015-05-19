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

/**
 *  Functions to layout isolated nodes/communities in a graph (i.e. nodes with degree = 0)
 *  
 **/ 

class IsolatedNodeLayout {
	
	/**
	 * 	calcSpiralCoords
	 *  
	 *  Function to layout isolated and/or leaf communities in a spiral pattern
	 *
	 *  - nodes = List of isolated nodes to layout (node ID, numInternalNodes, degree, metadata) 
	 *  - boundingBox = bottem-left corner, width, height of bounding region for layout of nodes
	 *  - nodeAreaNorm = normalization factor used to determine areas of communities within bounding box
	 *  - centralCommunityArea = area of large central 'connected' community (ie area to leave empty in centre of spiral)
	 *  - borderPercent = Percent of parent bounding box to leave as whitespace between neighbouring communities.  Default = 2 %
	 *  
	 *  - Format of output array is (node ID, x, y, radius, numInternalNodes, degree, metaData)
	 **/	
	def calcSpiralCoords(nodes: Iterable[(Long, Long, Int, String)],
	                     boundingBox: (Double, Double, Double, Double),
	                     nodeAreaNorm: Double,
	                     centralCommunityArea: Double,
	                     borderPercent: Double = 2.0,
	                     bSortByDegree: Boolean = false): (Array[(Long, Double, Double, Double, Long, Int, String)], Double) = {
		
		val (xC, yC) = (0.0, 0.0) //(boundingBox._1 + boundingBox._3/2, boundingBox._2 + boundingBox._4/2)	// centre of bounding box
		val boundingBoxArea = boundingBox._3 * boundingBox._4
		
		if (borderPercent < 0.0 || borderPercent > 10.0) throw new IllegalArgumentException("borderPercent must be between 0 and 10")
		val border = borderPercent*0.01*Math.min(boundingBox._3, boundingBox._4)
		
		// Store community results in array with initial coords as xc,yc for now.
		// Array is sorted by community radius (or degree) -- smallest to largest
		var nodeCoords = nodes.map(n =>
			{
				val (id, numInternalNodes, degree, metaData) = n
				val nodeArea = nodeAreaNorm * boundingBoxArea * numInternalNodes
				val nodeRadius = Math.sqrt(nodeArea * 0.31831)	//0.31831 = 1/pi
				                          (id, xC, yC, nodeRadius, numInternalNodes, degree, metaData)
			}
		).toList.sortBy(row => if (bSortByDegree) row._6 else row._4).toArray

		val numNodes = nodeCoords.size
		
		//---- layout centre of spiral
		var n = numNodes-1
		val r0 = if (centralCommunityArea > 0.0) {
			// use central connected community as centre of spiral
			Math.sqrt(centralCommunityArea * 0.31831)	//0.31831 = 1/pi
		}
		else {
			// no central connected community here, so use largest isolated community as centre of spiral instead
			n -= 1
			nodeCoords(n+1)._4
		}
		
		//init spiral layout variables
		var r_prev = 0.0
		var r_delta = 0.0
		var Q_now_sum = 0.0
		var Q = 0.0		// current spiral angle	(in radians)
		var rQ = r0		// current spiral radius
		
		//---- layout 2nd community (to right of spiral centre)
		if (n >= 0) {
			val r_curr = nodeCoords(n)._4	// radius of current community
			
			rQ = rQ + r_curr + border 	// update spiral radius
			val x = rQ * Math.cos(Q);	// get centre coords of current community and save results
			val y = rQ * Math.sin(Q);
			nodeCoords(n) = (nodeCoords(n)._1, x + xC, y + yC, r_curr, nodeCoords(n)._5, nodeCoords(n)._6, nodeCoords(n)._7)
			
			r_prev = r_curr				//save current community radius for next iteration
			r_delta = 2*r_curr + border	//rate of r change per 2*pi radians (determines how tight or wide the spiral is)
			n -= 1
		}
		
		//---- layout rest of isolated communities
		while (n >= 0) {
			val r_curr = nodeCoords(n)._4	// radius of current community
			val d_arc = r_prev + r_curr + border	// distance between neighouring nodes in spiral
			val Q_curr = Math.acos((2*rQ*rQ - d_arc*d_arc)/(2*rQ*rQ)) 	// use cosine law to get spiral angle between neighbouring nodes
			Q += Q_curr
			Q_now_sum += Q_curr
			
			//rQ += r_delta*Q_curr/(2.0*Math.PI)	// increase r_delta over 2*pi rads
			if (Q_now_sum >= Math.PI) {
				rQ += r_delta*Q_curr/Math.PI		// increase r_delta over pi rads (produces a slightly tighter spiral)
			}
			
			val x = rQ * Math.cos(Q);	// get centre coords of current community and save results
			val y = rQ * Math.sin(Q);
			nodeCoords(n) = (nodeCoords(n)._1, x + xC, y + yC, r_curr, nodeCoords(n)._5, nodeCoords(n)._6, nodeCoords(n)._7)
			
			if (Q_now_sum > 2.0*Math.PI) {   // reset r_delta every 2*pi radians (for next level of spiral)
				Q_now_sum = 0.0
				r_delta = 2.0*r_curr + border	//rate of r change per 2*pi radians
			}
			
			r_prev = r_curr			//save current community radius for next iteration
			n -= 1
		}
		
		//---- Do final scaling of XY co-ordinates to fit within bounding area
		var maxDist = Double.MinValue
		for (n <- 0 until numNodes) {
			val xDist = xC - nodeCoords(n)._2	// node distance to centre
			val yDist = yC - nodeCoords(n)._3
			// calc distance plus node radius (to ensure all of a given node's circle fits into the bounding area)
			val dist = Math.sqrt(xDist*xDist + yDist*yDist) + nodeCoords(n)._4
			maxDist = Math.max(maxDist, dist)
		}
		
		val scaleFactor = 0.5*Math.min(boundingBox._3, boundingBox._4) / maxDist
		
		for (n <- 0 until numNodes) {
			val (id, x, y, radius, numInternalNodes, degree, metaData) = nodeCoords(n)
			// scale community radii too if scaleFactor < 1, so scaling doesn't cause communities to overlap
			val scaledRadius = if (scaleFactor < 1.0) radius*scaleFactor else radius
			nodeCoords(n) = (id, x*scaleFactor + boundingBox._1 + 0.5*boundingBox._3, y*scaleFactor + boundingBox._2 + 0.5*boundingBox._4, scaledRadius, numInternalNodes, degree, metaData)
		}
		
		val scaledCentralArea = centralCommunityArea*scaleFactor*scaleFactor	// also scale centralCommunityArea accordingly (ie x square of scaleFactor)
		
		(nodeCoords, scaledCentralArea)
	}
}
