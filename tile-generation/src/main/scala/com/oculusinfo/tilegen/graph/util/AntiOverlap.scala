/*
 * Copyright (c) 2015 Oculus Info Inc.
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

import com.oculusinfo.geometry.quadtree.QuadNode

/**
 *  A Graph Layout algorithm that prevents nodes/communities from overlapping using an iterative repulsion calculation. N
 *  Nodes that do not overlap each other are not altered.
 *  
 *  Designed to be used in conjunction with ForceDirected layout algorithm 
 *  (ie use ForceDirected algo first to get general layout, 
 *  and then use AntiOverlap to fine-tune the positions of any overlapping nodes)
 *  
 **/ 

class AntiOverlap {

  	val QT_NODE_THRES = 20		// num of nodes threshold for whether or not to use quadtree decomposition
	val QT_THETA = 1.0			// theta value for quadtree decomposition
								// (>= 0; lower value gives more accurate repulsion force results, but is less efficient)
	var _bNodesOverlapping = false	// boolean for whether community circles overlap or not
	
	/**
	 * Run the AntiOverlap graph layout algorithm
	 * 
	 * @param 	nodeCoords
	 *        	Array of input node/community locations. Format of array is (node ID, x, y, radius, numInternalNodes, metaData)
     * @param 	parentID
	 *        	ID of parent community. Node having ID == parentID is kept in a constant location (ie in the centre of the bounding area)
	 * @param 	maxIterations
	 *        	Max number of iterations to run AntiOverlap algorithm
	 * @param 	dampen
	 * 		  	Factor used to control how much to dampen, or slow down, the anti-collision repulsion force. 
	 * 		  	0 to 1; lower number == slower, but more precise nodes movements
     * @param 	maxDelta
	 * 		  	Optional threshold to determine when algorithm has sufficiently converged (if maximum node displacement < maxDelta). Default = 0.0
	 *      	meaning algorithm will simply continue to run for maxIterations              
	 *                                                     
	 * @return 	Array of final node/community locations. Format of output array is (node ID, x, y, radius, numInternalNodes, metaData)
	 * 
	 */
	def run(nodeCoords: Array[(Long, Double, Double, Double, Long, Int, String)],
	        parentID: Long,
	        maxIterations: Int = 250,
	        dampen: Float = 0.5f,
	        maxDelta: Double = 0.0): Array[(Long, Double, Double, Double, Long, Int, String)] = {
  	  
  	  
  	    val numNodes = nodeCoords.size
  	    val bUseQTDecomp = numNodes > QT_NODE_THRES
		var bDone = false
		var iterations = 1
		println("Starting Anti-Overlap layout algorithm on " + numNodes + " nodes")
		
		while (!bDone) {
			
			_bNodesOverlapping = false
			
			// init array of node displacements for this iteration
			var deltaXY = Array.fill[(Double, Double)](numNodes)((0.0,0.0))
			
			//---- Calc repulsion displacements between all overlapping nodes
			if (bUseQTDecomp) {
				// Use Quadtree Decomposition for repulsion displacement calculation
				val qt = ForceDirected.createQuadTree(nodeCoords, numNodes)
				
				for (n1 <- 0 until numNodes) {
					val (x,y,r) = (nodeCoords(n1)._2, nodeCoords(n1)._3, nodeCoords(n1)._4)
					val qtDeltaXY = calcQTOverlapRepulsion(n1, x, y, r, qt.getRoot, QT_THETA)
					
					deltaXY(n1) = (deltaXY(n1)._1 + qtDeltaXY._1, deltaXY(n1)._2 + qtDeltaXY._2)
				}
			}
			else {
				// Use regular repulsion force calculation instead
				for (n1 <- 0 until numNodes) {
					for (n2 <- 0 until numNodes) {
						if (n1 != n2 && (nodeCoords(n1)._1 != parentID)) {
							// get x, y coords and radii of target and repulsor nodes
							val xyr_target = (nodeCoords(n1)._2, nodeCoords(n1)._3, nodeCoords(n1)._4)
							val xyr_repulsor = (nodeCoords(n2)._2, nodeCoords(n2)._3, nodeCoords(n2)._4)
							
							val nodeDeltaXY = calcOverlapRepulsion(xyr_target, xyr_repulsor)
							deltaXY(n1) = (deltaXY(n1)._1 + nodeDeltaXY._1, deltaXY(n1)._2 + nodeDeltaXY._2)
						}
					}
				}
			}
			
			//---- apply anti-overlap repulsion
			var maxDX = Double.MinValue
			var maxDY = Double.MinValue
			for (n1 <- 0 until numNodes) {
			    val dX = deltaXY(n1)._1 * dampen	// dampen displacement results
			    val dY = deltaXY(n1)._2 * dampen		    
				maxDX = Math.max(dX, maxDX)			// calc max of all displacements
				maxDY = Math.max(dY, maxDY)			  
			  
				// save new node coord locations
				nodeCoords(n1) = (nodeCoords(n1)._1, nodeCoords(n1)._2 + dX, nodeCoords(n1)._3 + dY, nodeCoords(n1)._4, nodeCoords(n1)._5, nodeCoords(n1)._6, nodeCoords(n1)._7)		
			}
			
			if ((!_bNodesOverlapping) || (iterations >= maxIterations) || (Math.max(maxDX,maxDY) < maxDelta)) {
				bDone = true
			}
			else {
				iterations += 1
			}

		}
  	      
  		nodeCoords
  	}

  	// Calculate Node to Node anti-overlap repulsion displacement
	def calcOverlapRepulsion(target: (Double, Double, Double), repulsor: (Double, Double, Double)): (Double, Double) = {
		
		//format for 'point' is assumed to be (x,y,radius)
		var xDist = target._1 - repulsor._1
		var yDist = target._2 - repulsor._2
		val r1 = target._3
		val r2 = repulsor._3
		// calc distance between two nodes (corrected for node radii)
		val dist12 = Math.sqrt(xDist*xDist + yDist*yDist)		// distance between node
		if (dist12 < r1 + r2) {
			_bNodesOverlapping = true
			var repulseDist = 0.0
			if ((xDist == 0) && (yDist == 0)) {
				xDist = r1*0.01	// force xDist and yDist to be 1% of radius so repulse calc below doesn't == 0
				yDist = r2*0.01	// TODO -- would be better to use random directions here!
			}
			else {
				repulseDist = ((r2 + r1 - dist12)/dist12)*(r2/(r2+r1));	// repulse force == proportional to ratio of community radii 
			}
			(xDist*repulseDist, yDist*repulseDist)
		}
		else {
			(0.0, 0.0)
		}
	}
	
  	// Calculate QuadTree anti-overlap repulsion displacement
	def calcQTOverlapRepulsion(index: Int, x: Double, y: Double,
	                    r: Double, qn: QuadNode, theta: Double): (Double, Double) = {
		
		if (qn == null) {
			throw new IllegalArgumentException("quadNode == null") //return (0.0, 0.0)
		}
		if (qn.getNumChildren == 0) { // nothing to compute
			return (0.0, 0.0)
		}
		
		if (qn.getNumChildren == 1) { // leaf

			val qnData = qn.getData
			if (qnData.getId == index) {
				return (0.0, 0.0)
			}
			val currentLeafPosition = qnData
			val xyDelta = calcOverlapRepulsion((x,y,r), (qnData.getX, qnData.getY, qnData.getSize))
			return xyDelta
		}
		
		if (ForceDirected.useAsPseudoNode(qn, (x,y), r, theta)) {	// consider current quadnode as a 'pseudo node'?
			val (xC, yC) = qn.getCenterOfMass									// use quadnode's Centre of Mass as repulsor's coords
			val rC = qn.getSize													// and use average radius of all underlying nodes, for this pseudo node
			val xyDelta = calcOverlapRepulsion((x,y,r), (xC, yC, rC))
			return (xyDelta._1*qn.getNumChildren, xyDelta._2*qn.getNumChildren)	// multiply repulsion results by number of child nodes
		}
		
		// failed to resolve a repulsion, so recurse into all four child quad nodes, and sum results
		val xyDeltaNW = calcQTOverlapRepulsion(index, x, y, r, qn.getNW, theta)
		val xyDeltaNE = calcQTOverlapRepulsion(index, x, y, r, qn.getNE, theta)
		val xyDeltaSW = calcQTOverlapRepulsion(index, x, y, r, qn.getSW, theta)
		val xyDeltaSE = calcQTOverlapRepulsion(index, x, y, r, qn.getSE, theta)

		(xyDeltaNW._1 + xyDeltaNE._1 + xyDeltaSW._1 + xyDeltaSE._1,
		 xyDeltaNW._2 + xyDeltaNE._2 + xyDeltaSW._2 + xyDeltaSE._2)
	}
	
	

}