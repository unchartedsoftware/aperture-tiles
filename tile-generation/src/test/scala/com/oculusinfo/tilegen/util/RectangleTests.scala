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

import org.scalatest.FunSuite



class RectangleTestSuite extends FunSuite {
	test("Union") {
		val r1 = new Rectangle[Int](0, 1, 3, 4)
		val r2 = new Rectangle[Int](2, 3, 1, 2)

		val union = r1 union r2
		assert(union.minX === 0)
		assert(union.maxX === 3)
		assert(union.minY === 1)
		assert(union.maxY === 4)
	}

	test("Equality") {
		val r1 = new Rectangle[Int](0, 1, 0, 1)
		val r2 = new Rectangle[Int](0, 1, 0, 1)
		val r3 = new Rectangle[Int](0, 1, 0, 2)

		assert(r1 === r2)
		assert(!(r1 == r3))
	}

	test("Point containment") {
		val r = new Rectangle[Int](0, 10, 0, 10)

		assert(r.contains(0, 0))
		assert(r.contains(9, 9))
		assert(!r.contains(0, -1))
		assert(!r.contains(-1, 0))
		assert(!r.contains(9, 10))
		assert(!r.contains(10, 9))
	}


	test("Rectangle containment") {
		val r = new Rectangle[Int](0, 10, 0, 10)

		assert(r.contains(new Rectangle[Int](0, 1, 0, 1)))
		assert(r.contains(new Rectangle[Int](9, 10, 9, 10)))
		assert(r.contains(new Rectangle[Int](0, 10, 0, 10)))
		assert(!r.contains(new Rectangle[Int](-1, 10, 0, 10)))
		assert(!r.contains(new Rectangle[Int](0, 11, 0, 10)))
		assert(!r.contains(new Rectangle[Int](0, 10, -1, 10)))
		assert(!r.contains(new Rectangle[Int](0, 10, 0, 11)))
	}

	test("fromJava") {
		assert(Rectangle.fromJava(new Rectangle2D.Double(1.0, 1.0, 1.0, 1.0)) ===
			       new Rectangle[Double](1.0, 2.0, 1.0, 2.0))
		assert(Rectangle.fromJava(new Rectangle2D.Double(-1.5, -1.5, 0.5, 0.5)) ===
			       new Rectangle[Double](-1.5, -1.0, -1.5, -1.0))
	}
}
