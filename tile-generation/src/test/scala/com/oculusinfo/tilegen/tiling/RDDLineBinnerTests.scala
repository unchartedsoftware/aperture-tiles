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

package com.oculusinfo.tilegen.tiling



import scala.collection.JavaConverters._
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite

import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.BinIndex


class RDDLineBinnerTestSuite extends FunSuite with SharedSparkContext {
	def wikipediaGetPoints (start: BinIndex, end: BinIndex): (Boolean, Int, Int, Int, Int) = {
		// The un-scala-like version from wikipedia
		var (x0, y0, x1, y1) = (start.getX(), start.getY(), end.getX(), end.getY())
		var steep = math.abs(y1 - y0) > math.abs(x1 - x0)

		var tmpInt = 0
		if (steep) {
			tmpInt = y0		//swap x0, y0
			y0 = x0
			x0 = tmpInt
			tmpInt = y1		//swap x1, y1
			y1 = x1
			x1 = tmpInt
		}
		if (x0 > x1) {
			tmpInt = x1		//swap x0, x1
			x1 = x0
			x0 = tmpInt
			tmpInt = y0		//swap y0, y1
			y0 = y1
			y1 = tmpInt
		}
		(steep, x0, y0, x1, y1)
	}
	
	/**
	 * Re-order coords of two endpoints for efficient implementation of Bresenham's line algorithm  
	 */	
	def getPoints (start: BinIndex, end: BinIndex): (Boolean, Int, Int, Int, Int) = {
		val xs = start.getX()
		val xe = end.getX()
		val ys = start.getY()
		val ye = end.getY()
		val steep = (math.abs(ye - ys) > math.abs(xe - xs))

		if (steep) {
			if (ys > ye) {
				(steep, ye, xe, ys, xs)
			} else {
				(steep, ys, xs, ye, xe)
			}
		} else {
			if (xs > xe) {
				(steep, xe, ye, xs, ys)
			} else {
				(steep, xs, ys, xe, ye)
			}
		}
	}	

	test("Bresenham Alternatives") {
		// Make sure our scala-like version matches the unscala-like one from wikipedia.
		for (w <- 0 to 10;
		     x <- 0 to 10;
		     y <- 0 to 10;
		     z <- 0 to 10) {
			val b1 = new BinIndex(w, x)
			val b2 = new BinIndex(y, z)
			assert(getPoints(b1, b2) === wikipediaGetPoints(b1, b2))
		}
	}
}

