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

package com.oculusinfo.tilegen.util



import java.awt.geom.Rectangle2D



/**
 * A generic rectangle class that works on any value with an implicit ordering
 */
class Rectangle [T: Ordering] (val minX: T, val maxX: T, val minY: T, val maxY: T)
		extends Serializable {
	private val ordering = implicitly[Ordering[T]]
	lazy val xRange = (minX, maxX)
	lazy val yRange = (minY, maxY)

	/**
	 * Get the smallest rectangle fully containing both this and that
	 */
	def union (that: Rectangle[T]): Rectangle[T] =
		new Rectangle[T](ordering.min(this.minX, that.minX),
		                 ordering.max(this.maxX, that.maxX),
		                 ordering.min(this.minY, that.minY),
		                 ordering.max(this.maxY, that.maxY))

	/**
	 * Determine if this rectangle contains the point specefied
	 */
	def contains (x: T, y: T): Boolean =
		(ordering.lteq(minX, x) && ordering.lt(x, maxX) &&
			 ordering.lteq(minY, y) && ordering.lt(y, maxY))

	/**
	 * Determine if this rectangle contains the rectangle specified
	 */
	def contains (that: Rectangle[T]): Boolean =
		(ordering.lteq(this.minX, that.minX) && ordering.lteq(that.maxX, this.maxX) &&
			 ordering.lteq(this.minY, that.minY) && ordering.lteq(that.maxY, this.maxY))



	// //////////////////////////////////////////////////////////////////////////
	// Section: Any overrides
	//
	override def hashCode =
		41*minX.hashCode + 43*maxX.hashCode + 47*minY.hashCode + 53*maxY.hashCode

	override def equals (other: Any) = other match {
		case that: Rectangle[_] =>
			(this.minX == that.minX &&
				 this.maxX == that.maxX &&
				 this.minY == that.minY &&
				 this.maxY == that.maxY)
		case _ => false
	}

	override def toString: String = "["+minX+", "+minY+" => "+maxX+", "+maxY+"]"
}

object Rectangle {
	/**
	 * Convert a java rectangle into a Rectangle[Double]
	 */
	def fromJava (source: Rectangle2D): Rectangle[Double] =
		new Rectangle[Double](source.getMinX(), source.getMaxX(),
		                      source.getMinY(), source.getMaxY())
}

