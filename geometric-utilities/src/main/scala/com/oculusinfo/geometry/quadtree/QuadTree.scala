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

package com.oculusinfo.geometry.quadtree


/**
 *  Quadtree Decomposition Algorithm
 *  
 *  Adapted from the 'FFDLayouter' code in ApertureJS
 *  
 */
class QuadTree(box: (Double, Double, Double, Double)) {	// bounding box of entire quadtree (x,y of lower left corner, width, height)
	
	private var _root: QuadNode = new QuadNode(box)
	
	def getRoot(): QuadNode = {
		_root
	}
	
	def insert(x: Double, y: Double, id: Long, size: Double) = {
		insertIntoQuadNode(_root, new QuadNodeData(x, y, id, size))
	}
	
	def insertIntoQuadNode(qn: QuadNode, data: QuadNodeData): Unit = {
		
		if (qn == null) {
			return
		}
		
		qn.incrementChildren();
		
		// case 1: leaf node, no data
		// 	 just add the values and get out
		if (qn.getNumChildren() == 1) {
			qn.setData(data)
			qn.setCenterOfMass((data.getX, data.getY))
			qn.setSize(data.getSize)
			return
		}
	
		// case 2: check that two nodes aren't located in exact same location
		// (to prevent quadtree with infinite depth)
		if ((qn.getData != null) && (qn.getNumChildren == 2)) {
			val x1 = qn.getData.getX
			val y1 = qn.getData.getY
			if ((x1 == data.getX) && (y1 == data.getY)) {
				qn.setNumChildren(1)	// force two nodes at same location to be considered as one node
				return
			}		
		}		
				
		// move the center of mass by scaling old value by (n-1)/n and adding the 1/n new contribution
		val scale = 1.0/qn.getNumChildren
		val newX = qn.getCenterOfMass._1 + scale*(data.getX - qn.getCenterOfMass._1)
		val newY = qn.getCenterOfMass._2 + scale*(data.getY - qn.getCenterOfMass._2)
		qn.setCenterOfMass((newX, newY))
		// and use same technique for updating the average size (ie node radius) of the current quad node
		val newSize = qn.getSize + scale*(data.getSize - qn.getSize)
		qn.setSize(newSize)
		
		// case 3: current leaf needs to become internal, and four new child quad nodes need to be created
		if (qn.getData != null) {
			val rect = qn.getBounds
			val halfwidth = rect._3/2
			val halfheight = rect._4/2
			qn.setNW( new QuadNode(rect._1, rect._2+halfheight, halfwidth, halfheight))
			qn.setNE( new QuadNode(rect._1+halfwidth, rect._2+halfheight, halfwidth, halfheight))
			qn.setSW( new QuadNode(rect._1, rect._2, halfwidth, halfheight))
			qn.setSE( new QuadNode(rect._1+halfwidth, rect._2, halfwidth, halfheight))
			val oldNodeData = qn.getData()
			qn.setData(null)
			insertIntoContainingChildQuandrant(qn, oldNodeData)
		}

		// case 4: internal node, has more than one child but no nodedata
		//    just push into the proper subquadrant (which already exists)
		insertIntoContainingChildQuandrant(qn, data)
	}
	
	private def insertIntoContainingChildQuandrant(qn: QuadNode, qnData: QuadNodeData) = {
		var childRecursionQuad: QuadNode = null
		
		if (qn.getNW.isPointInBounds(qnData.getX, qnData.getY)) {
			childRecursionQuad = qn.getNW
		} else if (qn.getNE.isPointInBounds(qnData.getX, qnData.getY)) {
			childRecursionQuad = qn.getNE
		} else if (qn.getSW.isPointInBounds(qnData.getX, qnData.getY)) {
			childRecursionQuad = qn.getSW
		} else if (qn.getSE.isPointInBounds(qnData.getX, qnData.getY)) {
			childRecursionQuad = qn.getSE
		}
		insertIntoQuadNode(childRecursionQuad, qnData);
	}
}
