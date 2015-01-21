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

//import org.apache.spark.SparkContext
//import org.apache.spark.SparkContext._
//import org.apache.spark.rdd.RDD
//import org.apache.spark.graphx._

/**
 *  A treemap space-filling graph layout algorithm.
 *  Based on the 'Group-in-a-Box' paper by Rodrigues et al, and the 'Fitted Rectangles'
 *  paper by Chaturvedi et al, but uses a spiral layout of community rectangles
 */ 
class GroupInBox extends Serializable {

	def run(groups: Iterable[(Long, Long, Int)],
	        parentRect: (Double, Double, Double, Double),
	        numNodesThres: Int = 1000): List[(Long, (Double, Double, Double, Double))] = {
		
		//format of groups is assumed to be (groupID, numInternalNodes, community degree))
		val totalNumNodes = groups.map(_._2).reduce(_ + _)	// total number of internal nodes in all groups
		if (totalNumNodes <= 0)	throw new IllegalArgumentException("totalNumNodes must be > 0, but = " + totalNumNodes)
		
		val bUseWeightedConnectedness = true	//TODO -- could expose an API parameter for this?
			//Note: weighted connectedness sorts communities using a weighted combination of community degree (num of outgoing edeges)
			// and community size (num of internal nodes).  This helps ensure the largest communities are placed near the centre of the screen
			// If set to false, then communities are sorted by num of outgoing edges only
		
		val sortedGroups =
			(if (bUseWeightedConnectedness) {
				 // find max internalNodes for all communities
				 val invMaxInternalNodes = 1.0/(groups.map(n => n._2).max.toDouble)
				 groups.map(n =>
					 (n._1, n._2, n._3*n._2*invMaxInternalNodes)
				 )
			 }
			 else {
				 groups.map(n => (n._1, n._2, (n._3).toDouble))

			 }).toList.sortBy(_._3).reverse	//sort by descending community degree
		
		val numGroups = sortedGroups.size
		var alpha = 1.0	// starting alpha 	//TODO -- could expose this as an API parameter?
		val screenW = parentRect._3
		val screenH = parentRect._4
		val screenArea = screenH * screenW
		val aspRatio = screenW / screenH;
		val minAspRatio = 0.25 //; %1/6;    %1/4	//TODO -- could expose this as an API parameter? (also a parameter controlling the width of successive 'spiral levels'?)
		                       // note: lower value for minAspRatio allows for better space-filling, but the potential trade-off is narrower rectangles
		
		var rects = new Array[(Double, Double, Double, Double)](numGroups)	// init array of rectangle results
		
		var nPrev = 0
		var n = 0
		var bDone = false
		var bDoExpansion = false
		var bNextSpiralLevel = false
		var currLabel = 0 	// 0 = center, 1 = H_D, 2 = V_R, 3 = H_U, 4 = V_L
		var labelToResume = 0
		var regionsTried = Array(0,0,0,0)
		var H_D = Array(0.0,0.0,0.0,0.0)	// represents unfilled lower horizontal rectangle region
		var H_U = Array(0.0,0.0,0.0,0.0)	// represents unfilled upper horizontal rectangle region
		var V_R = Array(0.0,0.0,0.0,0.0)	// represents unfilled right vertical rectangle region
		var V_L = Array(0.0,0.0,0.0,0.0)	// represents unfilled left vertical rectangle region
		var currRect = Array(0.0,0.0,0.0,0.0)	// current rectangle results
		var centreArea = Array(0.0,0.0,0.0,0.0)
		var centreAreaNext = Array(0.0,0.0,0.0,0.0)
		
		if (numGroups == 1) {	// if only 1 group to layout
			rects(0) = parentRect
			bDone = true
		}
		else if (totalNumNodes < numNodesThres) {
			for (n <- 0 until numGroups) {	// size of groups is below threshold, so simply set all rectangles = parent rectangle
				rects(n) = parentRect
			}
			bDone = true
		}
		else {
			println("Starting Group-In-Box layout on " + numGroups + " communities...")
		}
		
		while (!bDone) {
			var bSaveRect = false
			
			val internalNodes = sortedGroups(n)._2		//number of internal nodes for current community
			val communityArea = alpha * screenArea * internalNodes / totalNumNodes;	// rectangle area for current community
			
			if (currLabel == 0) {
				// place first group
				val h = Math.sqrt(communityArea / aspRatio);      // keep same aspect ratio for center group as 'screen' aspect ratio
				val w = communityArea/h;
				currRect = Array(screenW*0.5 - w*0.5, screenH*0.5-h*0.5, w, h);
				centreArea = currRect;
				H_D = Array((screenW-w)*0.5, (screenH-h)*0.25, screenW*0.25 + w*0.75, (screenH-h)*0.25);
				H_U = Array((screenW-w)*0.25, (screenH+h)*0.5, H_D(2), H_D(3));
				V_L = Array(H_U(0), H_D(1), (screenW-w)*0.25, centreArea(3)+H_D(3));
				V_R = Array(centreArea(0)+centreArea(2), centreArea(1), (screenW-w)*0.25, centreArea(3)+H_U(3));
				centreAreaNext = Array(V_L(0), V_L(1), V_L(2)+H_D(2), V_L(3)+H_U(3));  // for next level of spiral
				
				bSaveRect = true;
				currLabel = 1;
			}
			
			if ((currLabel == 1) && !bSaveRect) {
				// place next group in H_D rect
				var h = Math.sqrt(communityArea / aspRatio);
				if (h > H_D(3))
					h = H_D(3);
				else
					h = Math.min(H_D(3), Math.sqrt(communityArea / minAspRatio));
				
				val w = communityArea/h;
				
				if (w <= H_D(2)) {
					currRect = Array(H_D(0), H_D(1)+H_D(3)-h, w, h);      // rectangle for this group
					H_D(0) = H_D(0) + w;  //H2(1) = H2(1) + w;       // re-size H_D for next iteration
					H_D(2) = H_D(2) - w;
					bSaveRect = true;
				}
				else {
					regionsTried(0) = 1
					if (regionsTried(0)+regionsTried(1)+regionsTried(2)+regionsTried(3) == 4) {
						bNextSpiralLevel = true;
					}
					else {
						currLabel = 2;
						if (labelToResume != 0) labelToResume = 1
					}
				}
			}
			
			if ((currLabel == 2) && !bSaveRect) {
				// place next group in V_R rect
				var w = Math.sqrt(communityArea / aspRatio);
				if (w > V_R(2))
					w = V_R(2);
				else
					w = Math.min(V_R(2), Math.sqrt(communityArea / minAspRatio));
				
				val h = communityArea / w;
				
				if (h <= V_R(3)) {
					currRect = Array(V_R(0), V_R(1), w, h)  // rectangle for this group
					V_R(1) = V_R(1) + h;                    // re-size V_R for next iteration
					V_R(3) = V_R(3) - h;
					bSaveRect = true
				}
				else {
					regionsTried(1) = 1
					if (regionsTried(0)+regionsTried(1)+regionsTried(2)+regionsTried(3) == 4) {
						bNextSpiralLevel = true
					}
					else {
						currLabel = 3;
						if (labelToResume != 0) labelToResume = 2
					}
				}
			}
			
			if ((currLabel == 3) && !bSaveRect) {
				// place next group in H_U rect
				var h = Math.sqrt(communityArea / aspRatio);
				if (h > H_U(3))
					h = H_U(3)
				else
					h = Math.min(H_U(3), Math.sqrt(communityArea / minAspRatio))
				
				val w = communityArea/h;
				
				if (w <= H_U(2)) {
					currRect = Array(H_U(0)+H_U(2)-w, H_U(1), w, h)  // rectangle for this group
					H_U(2) = H_U(2) - w;  //H2(1) = H2(1) + w;       // re-size H_D for next iteration
					bSaveRect = true;
				}
				else {
					regionsTried(2) = 1
					if (regionsTried(0)+regionsTried(1)+regionsTried(2)+regionsTried(3) == 4) {
						bNextSpiralLevel = true;
					}
					else {
						currLabel = 4;
						if (labelToResume != 0) labelToResume = 3
					}
				}
			}
			
			if ((currLabel == 4) && !bSaveRect) {
				// place next group in V_L rect
				var w = Math.sqrt(communityArea / aspRatio);
				if (w > V_L(2))
					w = V_L(2)
				else
					w = Math.min(V_L(2), Math.sqrt(communityArea / minAspRatio));
				
				val h = communityArea/w
				
				if (h <= V_L(3)) {
					currRect = Array(V_L(0)+V_L(2)-w, V_L(1)+V_L(3)-h, w, h)  // rectangle for this group
					V_L(3) = V_L(3) - h                                       // re-size V_R for next iteration
					bSaveRect = true
				}
				else {
					regionsTried(3) = 1
					if (regionsTried(0)+regionsTried(1)+regionsTried(2)+regionsTried(3) == 4) {
						bNextSpiralLevel = true;
					}
					else {
						currLabel = 1;
						if (labelToResume != 0) labelToResume = 4
					}
				}
			}
			
			if (bSaveRect) {
				// save results for this rectangle (while accounting for 'global' location of parent rectangle)
				rects(n) = (currRect(0)+parentRect._1, currRect(1)+parentRect._2, currRect(2), currRect(3))
				bNextSpiralLevel = false
				regionsTried = Array(0,0,0,0)
				
				if (labelToResume != 0) {
					currLabel = labelToResume
					labelToResume = 0
				}
				if (n == numGroups-1) {
					bDone = true	// finished!
					bDoExpansion = true
					println("Group-In-Box layout finished with alpha = " + alpha)
				}
				//println(n)
				n = n + 1
			}
			else if (bNextSpiralLevel) {
				if (n == nPrev) {
					// no progression since started this spiral level, therefore reset and start again with a lower alpha value
					n = 0;
					alpha = alpha*0.9;
					println("Group-In-Box ran out of space, resetting with alpha = " + alpha + " and retrying...")
					bDone = false;
					
					currLabel = 0
					labelToResume = 0
					H_D = Array(0.0, 0.0, 0.0, 0.0)
					H_U = Array(0.0, 0.0, 0.0, 0.0)
					V_R = Array(0.0, 0.0, 0.0, 0.0)
					V_L = Array(0.0, 0.0, 0.0, 0.0)
				}
				else {
					//println("Ok!  Starting next level of spiral...")
					bDone = false
					
					currLabel = 1
					labelToResume = 0
					
					centreArea = centreAreaNext;
					H_D = Array(centreArea(0), (screenH-centreArea(3))*0.25, screenW*0.25 + centreArea(2)*0.75, (screenH-centreArea(3))*0.25)
					H_U = Array((screenW-centreArea(2))*0.25, centreArea(1)+centreArea(3), screenW*0.25 + centreArea(2)*0.75, (screenH-centreArea(3))*0.25)
					V_L = Array(H_U(0), H_D(1), (screenW-centreArea(2))*0.25, centreArea(3)+H_D(3))
					V_R = Array(centreArea(0)+centreArea(2), centreArea(1), (screenW-centreArea(2))*0.25, centreArea(3)+H_U(3))
					centreAreaNext = Array(V_L(0), V_L(1), V_L(2)+H_D(2), V_L(3)+H_U(3)) // for next level of spiral
					nPrev = n
				}
			}
		}
		
		// ---- Expand rectangles out to better fit parent layout (to reduce amount of wasted space)
		if (bDoExpansion && (centreAreaNext(2) > 0.0) && (centreAreaNext(3) > 0.0)) {
			val expandFactor = Math.min(screenW/centreAreaNext(2), screenH/centreAreaNext(3))
			if (expandFactor > 1.0) {
				val widthTmp = screenW/2 + parentRect._1
				val heightTmp = screenH/2 + parentRect._2
				for (n <- 0 until numGroups) {
					val x = (rects(n)._1 - widthTmp)*expandFactor + widthTmp
					val y = (rects(n)._2 - heightTmp)*expandFactor + heightTmp
					val w = rects(n)._3*expandFactor
					val h = rects(n)._4*expandFactor
					rects(n) = (x, y, w, h)
					if ((rects(n)._3 > parentRect._3) || (rects(n)._4 > parentRect._4)) {
						throw new RuntimeException ("Internal rectangle size is bigger than parent rectangle.")
					}
				}
			}
		}
		
		sortedGroups.map(p => p._1).zip(rects)	//result format is List[(groupID, rectangle)]
	}
}
