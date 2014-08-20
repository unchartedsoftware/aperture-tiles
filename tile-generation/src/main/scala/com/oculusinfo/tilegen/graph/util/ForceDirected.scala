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

import scala.util.Random

/**
 *  A Force-Directed graph layout algorithm (ie Fruchterman-Reingold)
 *  
 *  Converted from the 'FFDLayouter' code in ApertureJS
 *  
 *  notes:
 *  - nodes = List of (node ID, number of internal nodes) -- num of internal nodes is only applicable
 *  	for hierarchical force-directed applications (ie if each 'node' represents a community). In this
 *   	case it is recommended to set bUseNodeSizes = true
 *  - boundingBox = bottem-left corner, width, height of bounding region for layout of nodes
 *  - borderOffset = percent of boundingBox width and height to leave as whitespace when laying out nodes
 *  - maxIterations = max number of iterations to use for force-directed algorithm
 *  - bUseEdgeWeights = uses edge weights to scale the attraction forces between connected nodes
 *  - bUseNodeSizes = uses 'number of internal nodes' attribute to size each node as a circle
 *  - nodeAreaPercent = if bUseNodeSizes = true, then this parameter is used to determine the area of all node
 *  	'circles' within the boundingBox vs whitespace
 *  - gravity = strength gravity force to use to prevent outer nodes from spreading out too far.  Default = 0.0 (no gravity),
 *  			whereas gravity = 1.0 gives gravitational force on a similar scale to edge attraction forces  
 *  
 **/ 
class ForceDirected extends Serializable {

	def run(nodes: Iterable[(Long, Long)], 
			edges: Iterable[(Long, Long, Long)],
			boundingBox: (Double, Double, Double, Double), 
			borderOffset: Int = 0, 
			maxIterations: Int = 1000,
			bUseEdgeWeights: Boolean = false,
			bUseNodeSizes: Boolean = false,
			nodeAreaPercent: Int = 20,
			gravity: Double = 0.0): Array[(Long, Double, Double, Double)] = {
		
		val numNodes = nodes.size
		if (numNodes == 0) throw new IllegalArgumentException("number of nodes must be > 0")
		
		val invTotalInternalNodes = if (bUseNodeSizes) {	// inverse of total number of internal nodes in all groups
			1.0 / (nodes.map(_._2).reduce(_ + _))
		}
		else {
			0L
		}
				
		var boundingBoxFinal = if ((!bUseNodeSizes) && (borderOffset > 0)) {
			calcFinalBoundingBox(boundingBox, borderOffset)
		}
		else {
			boundingBox
		}
		var boundingBoxArea = boundingBoxFinal._3 * boundingBoxFinal._4
		val nodeAreaFactor = nodeAreaPercent*0.01
		
		if (numNodes == 1) {
			//---- Special case: only one node to layout, so place in centre of boundingBox
			val x = boundingBox._1 + boundingBox._3*0.5	
			val y = boundingBox._2 + boundingBox._4*0.5
			val radius = if (bUseNodeSizes) {
				val wTmp = boundingBox._3*0.5
				val hTmp = boundingBox._4*0.5
				Math.sqrt(wTmp*wTmp + hTmp*hTmp)
			}
			else {
				0.0
			}
			return Array( (nodes.last._1, x, y, radius) )
		}
		if ((numNodes == 2) && !bUseNodeSizes) {
			//---- Special case: only two nodes to layout, so place bottem-left and top-right corners
			val nodeList = nodes.toList
			val x = boundingBox._1
			val y = boundingBox._2
			val x1 = boundingBox._1 + boundingBox._3
			val y1 = boundingBox._2 + boundingBox._4
			return Array( (nodeList(0)._1, x, y, 0.0), (nodeList(1)._1, x1, y1, 0.0) )
		}
		
		//Init scale factors for edge weighting (squash raw edge weights into an appropriate range for the number of nodes)
		//(ie range of sqrt(numNodes) to 1)
		var eWeightSlope = 0.0
		var eWeightOffset = 1.0
		if (bUseEdgeWeights) {
			// find the min and max edge weights
			val (maxW, minW) = edges.map(e => (e._3, e._3)).reduce((a, b) => (a._1 max b._1, a._2 min b._2))
			
			if (maxW > minW) {
				val maxWeight = Math.max(Math.sqrt(numNodes), 10.0)
				eWeightSlope = (maxWeight - 1.0) / (maxW - minW)
				eWeightOffset = 1.0 - eWeightSlope*minW
			}
		}
			
		//---- Initially assign random co-ordinates for all nodes (random number between 0 and 1) 
		// and also assign radii for each node based on internal community size (if applicable)
		
		// TODO -- try other initial layout schemes to try and reduce number of needed iterations?
		//Eg, place nodes with highest degree in centre, and others farther away ... perhaps in a spiral-like layout similar to Group-in-Box
		val randSeed = 911
		var random = new Random(randSeed)
		var nodeCoords = if (bUseNodeSizes) {
			nodes.map(n => {
				val numInternalNodes = n._2
				val nodeArea = nodeAreaFactor * boundingBoxArea * numInternalNodes * invTotalInternalNodes
				val nodeRadius = Math.sqrt(nodeArea * 0.31831)	//0.31831 = 1/pi
				(n._1, random.nextDouble, random.nextDouble, nodeRadius)
			}).toArray
		} else {
			nodes.map(n => (n._1, random.nextDouble, random.nextDouble, 0.0)).toArray
		}
		
		if (bUseNodeSizes) {
			// Shrink bounding box a bit to ensure node 'circles' don't extend beyond the edges of parent box too much
			val borderWeightFactor = 0.5 // between 0 and 1 ... 0 = no shrinking, 1 = max shrinking  
			val borderAdjust = nodeCoords.map(_._4).reduce(_ max _) * borderWeightFactor
			boundingBoxFinal = (boundingBoxFinal._1+borderAdjust, boundingBoxFinal._2+borderAdjust, boundingBoxFinal._3 - 2.0*borderAdjust, boundingBoxFinal._4 - 2.0*borderAdjust)
			boundingBoxArea = boundingBoxFinal._3 * boundingBoxFinal._4
		}
		
		// normalize starting coords so they are within the width and height of the bounding box
		for (n <- 0 until numNodes) {
			val (id,x,y, radius) = nodeCoords(n)
			nodeCoords(n) = (id, x*boundingBoxFinal._3, y*boundingBoxFinal._4, radius)		
		}
				
		//----- Init variables for controlling force-directed step-size
		val k2 = boundingBoxArea/numNodes
		val k_inv = 1.0/Math.sqrt(k2)
		val temperature0 = 0.5*Math.min(boundingBoxFinal._3, boundingBoxFinal._4)
		var temperature = temperature0
		val stepLimitFactor = 0.001	
		val alphaCool = Math.max(Math.min(1.0 + Math.log(stepLimitFactor)*4.0/maxIterations, 0.99), 0.8)	// set temperature cooling factor with respect to maxIterations (lower value == faster algorithm cooling)	
		val stepLimitSq = Math.pow(Math.min(boundingBoxFinal._3, boundingBoxFinal._4)*stepLimitFactor, 2.0)	// square of stepLimit
		var energySum = Double.MaxValue		// init high
		var progressCount = 0
		val nodeOverlapRepulsionFactor = 100.0/Math.min(boundingBoxFinal._3, boundingBoxFinal._4)	// constant used for extra strong repulsion if node 'circles' overlap

		//----- Re-format edge data to reference node array indices instead of actual node ID labels (for faster array look-ups below)
		val edgesArray = reformatEdges(edges, nodeCoords.map(n => n._1))

		val numEdges = edgesArray.length
		
		//----- Main Force-directed algorithm...
		var bDone = false
		var iterations = 1
		println("Starting Force Directed layout on " + numNodes + " nodes and " + numEdges + " edges...")
		
		while (!bDone) {
			
			// init array of node displacements for this iteration
			var deltaXY = Array.fill[(Double, Double)](numNodes)((0.0,0.0))	
			
			//---- Calc repulsion forces between all nodes
			if (bUseNodeSizes) {
				// account for node sizes, by adjusting distance between nodes by node radii
				for (n1 <- 0 until numNodes) {
					for (n2 <- 0 until numNodes) {
						if (n1 != n2) {
							val xDist = nodeCoords(n1)._2 - nodeCoords(n2)._2
							val yDist = nodeCoords(n1)._3 - nodeCoords(n2)._3
							// calc distance between two nodes (corrected for node radii)
							val dist = Math.sqrt(xDist*xDist + yDist*yDist) - nodeCoords(n1)._4 - nodeCoords(n2)._4	
							if (dist > 0.0) {
								val repulseForce = k2/(dist*dist)	// repulsion force
								deltaXY(n1) = (deltaXY(n1)._1 + xDist*repulseForce, deltaXY(n1)._2 + yDist*repulseForce)
							}
							else {
								val repulseForce = nodeOverlapRepulsionFactor*k2	// extra strong repulsion force if node circles overlap!
								deltaXY(n1) = (deltaXY(n1)._1 + xDist*repulseForce, deltaXY(n1)._2 + yDist*repulseForce)
							}
						} 
					}
				}
			}
			else {
				for (n1 <- 0 until numNodes) {
					for (n2 <- 0 until numNodes) {
						if (n1 != n2) {
							val xDist = nodeCoords(n1)._2 - nodeCoords(n2)._2
							val yDist = nodeCoords(n1)._3 - nodeCoords(n2)._3
							val normSq = xDist*xDist + yDist*yDist	// norm of distance between two nodes
							if (normSq > 0.0) {
								val repulseForce = k2/normSq	// repulsion force
								deltaXY(n1) = (deltaXY(n1)._1 + xDist*repulseForce, deltaXY(n1)._2 + yDist*repulseForce)
							}
						} 
					}
				}
			}		
			
			//---- Calc attraction forces due to all edges
			if (bUseNodeSizes) {
				// account for node sizes, by adjusting distance between nodes by node radii
				for (e1 <- 0 until numEdges) {
				    val (srcE, dstE, edgeWeight) = edgesArray(e1)	//get node indices for edge endpoints
				    
				    val xDist = nodeCoords(dstE)._2 - nodeCoords(srcE)._2
				    val yDist = nodeCoords(dstE)._3 - nodeCoords(srcE)._3
				    val dist = Math.sqrt(xDist*xDist + yDist*yDist) - nodeCoords(dstE)._4 - nodeCoords(srcE)._4
				    if (dist > 0) {	// only calc attraction force if node circles don't overlap
					    val attractForce = if (bUseEdgeWeights) {
					    	val w = eWeightSlope*edgeWeight + eWeightOffset
					    	dist * k_inv * w
					    }
					    else
					    	dist * k_inv
					    		
					    deltaXY(srcE) = (deltaXY(srcE)._1 + xDist*attractForce, deltaXY(srcE)._2 + yDist*attractForce)
					    deltaXY(dstE) = (deltaXY(dstE)._1 - xDist*attractForce, deltaXY(dstE)._2 - yDist*attractForce)			
				    }
				}
			}
			else {
				for (e1 <- 0 until numEdges) {
				    val (srcE, dstE, edgeWeight) = edgesArray(e1)	//get node indices for edge endpoints
				    
				    val xDist = nodeCoords(dstE)._2 - nodeCoords(srcE)._2
				    val yDist = nodeCoords(dstE)._3 - nodeCoords(srcE)._3
				    val dist = Math.sqrt(xDist*xDist + yDist*yDist)
				    val attractForce = if (bUseEdgeWeights) {
				    	val w = eWeightSlope*edgeWeight + eWeightOffset
				    	dist * k_inv * w
				    }
				    else
				    	dist * k_inv
				    	
				    deltaXY(srcE) = (deltaXY(srcE)._1 + xDist*attractForce, deltaXY(srcE)._2 + yDist*attractForce)
				    deltaXY(dstE) = (deltaXY(dstE)._1 - xDist*attractForce, deltaXY(dstE)._2 - yDist*attractForce)			
				}				
			}
			
			//---- Calc gravitational force for all nodes
			if (gravity > 0.0) {
				
				val xC = 0.5*boundingBoxFinal._3	// centre of bounding box (use as gravitational centre)
				val yC = 0.5*boundingBoxFinal._4
				
				if (bUseNodeSizes) {
					// account for node sizes using node radii
					for (n <- 0 until numNodes) {
						val xDist = xC - nodeCoords(n)._2	// node distance to centre
					    val yDist = yC - nodeCoords(n)._3
						val dist = Math.sqrt(xDist*xDist + yDist*yDist) - nodeCoords(n)._4
						if (dist > 0) {
							val gForce = dist * k_inv * gravity	// gravitational force for this node
							deltaXY(n) = (deltaXY(n)._1 + xDist*gForce, deltaXY(n)._2 + yDist*gForce)		
						}
					}
				}
				else {
					for (n <- 0 until numNodes) {
						val xDist = xC - nodeCoords(n)._2	// node distance to centre
					    val yDist = yC - nodeCoords(n)._3
						val dist = Math.sqrt(xDist*xDist + yDist*yDist)				
						if (dist > 0) {
							val gForce = dist * k_inv * gravity	// gravitational force for this node
							deltaXY(n) = (deltaXY(n)._1 + xDist*gForce, deltaXY(n)._2 + yDist*gForce)		
						}
					}					
				}
			}
			
			//---- Calc final displacements and save results for this iteration
			var largestStepSq = Double.MinValue // init low
			val energySum0 = energySum
			energySum = 0.0
			for (n <- 0 until numNodes) {

			    val deltaDist = Math.sqrt(deltaXY(n)._1*deltaXY(n)._1 + deltaXY(n)._2*deltaXY(n)._2);
			    if (deltaDist > temperature) {
			    	val normalizedTemp = temperature/deltaDist
			    	deltaXY(n) = (deltaXY(n)._1*normalizedTemp, deltaXY(n)._2*normalizedTemp)
			    }
			    val finalStepSq = deltaXY(n)._1*deltaXY(n)._1 + deltaXY(n)._2*deltaXY(n)._2
			    largestStepSq = Math.max(largestStepSq, finalStepSq);	// save largest step for this iteration
			    energySum += finalStepSq
			    
			    // save new node coord locations
			    nodeCoords(n) = (nodeCoords(n)._1, nodeCoords(n)._2 + deltaXY(n)._1, nodeCoords(n)._3 + deltaXY(n)._2, nodeCoords(n)._4)
			}
			
			//---- Adaptive cooling function (based on Yifan Hu approach)
			if (energySum < energySum0) {
				// system energy (movement) is decreasing, so keep temperature constant
				// or increase slightly to prevent algorithm getting stuck in a local minimum
				progressCount += 1
				if (progressCount >= 5) {
					progressCount = 0
					temperature = Math.min(temperature / alphaCool, temperature0)
				}
			}
			else {
				// system energy (movement) is increasing, so cool the temperature
				progressCount = 0
				temperature *= alphaCool
			}
			
			//---- Check if system has adequately converged
			if ((iterations >= maxIterations) || (temperature <= 0.0) || (largestStepSq <= stepLimitSq)) {
				bDone = true
				println("Finished layout algorithm in " + iterations + " iterations.")
			}			

			iterations += 1
		}
		
		//---- Do final scaling of XY co-ordinates to fit within bounding box
		var maxX = Double.MinValue
		var minX = Double.MaxValue
		var maxY = Double.MinValue
		var minY = Double.MaxValue
		
		for (n <- 0 until numNodes) {
			val (x,y) = (nodeCoords(n)._2, nodeCoords(n)._3)
			maxX = Math.max(maxX, x)
			minX = Math.min(minX, x)
			maxY = Math.max(maxY, y)
			minY = Math.min(minY, y)			
		}
		
		// TODO -- note:  with this scheme, we *could* end up with circles overlapping after final scaling if sx or sy < 1),
		// need to investigate further... 
//		if (bUseNodeSizes) {
//			// need to use the same scaleFactor for both x and y coords so node 'circles' don't get distorted
//			val scaleFactor = Math.min(boundingBoxFinal._3 / (maxX - minX),  boundingBoxFinal._4 / (maxY - minY))
//			
//			for (n <- 0 until numNodes) {
//				val (id,x,y, radius) = nodeCoords(n)
//				nodeCoords(n) = (id, (x-minX)*scaleFactor + boundingBoxFinal._1, (y-minY)*scaleFactor + boundingBoxFinal._2, radius)		
//			}	
//		}
//		else {
			val sx = boundingBoxFinal._3 / (maxX - minX)
			val sy = boundingBoxFinal._4 / (maxY - minY)
			
			for (n <- 0 until numNodes) {
				val (id,x,y, radius) = nodeCoords(n)
				nodeCoords(n) = (id, (x-minX)*sx + boundingBoxFinal._1, (y-minY)*sy + boundingBoxFinal._2, radius)		
			}		
//		}
						
		nodeCoords	// return final node coordinates
	}
	
	
	private def calcFinalBoundingBox(box: (Double, Double, Double, Double), borderOffset: Int): (Double, Double, Double, Double) = {
		//box == bottem-left corner, width, height 
		
		if (borderOffset < 0 || borderOffset > 100) throw new IllegalArgumentException("borderOffset must be >= 0 and <= 100")
		val offsetW = box._3*(borderOffset*0.01)
		val offsetH = box._4*(borderOffset*0.01)
		
		(box._1+offsetW, box._2+offsetH, box._3 - 2.0*offsetW, box._4 - 2.0*offsetH)
	}
	
	private def reformatEdges(edges: Iterable[(Long, Long, Long)], nodeIds: Array[Long]): Array[(Int, Int, Long)] = {
	
		edges.flatMap(e => {
			val srcIndx = nodeIds.indexOf(e._1)
			val dstIndx = nodeIds.indexOf(e._2)
			//val srcIndx = nodeCoords.indexOf((e._1,_:Double,_:Double))
			//val dstIndx = nodeCoords.indexOf((e._2,_:Double,_:Double))
			
			if (srcIndx == -1 || dstIndx == -1)			
				Iterator.empty 	// not a valid edge
			else
				Iterator( (srcIndx, dstIndx, e._3) )
				
		}).toArray
	}
	
}