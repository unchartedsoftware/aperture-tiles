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

package com.oculusinfo.tilegen.tiling.analytics


import java.lang.{Double => JavaDouble}
import scala.collection.JavaConverters._
import org.scalatest.FunSuite
import com.oculusinfo.binning.util.JSONUtilitiesTests
import org.json.JSONObject



class NumericArrayAnalyticsTestSuite extends FunSuite {
	def toJava (l: List[Double]) = l.map(new JavaDouble(_)).asJava

	def assertSeqsEqual[T] (a: Seq[T], b: Seq[T]): Unit = {
		assert(a.size === b.size)
		for (n <- 0 until a.size) assert(a(n) === b(n))
	}

	test("Standard Double Array Analytic") {
		val aBase = List(1.0, 2.0, 3.0, 4.0)
		val a = toJava(aBase)
		val bBase = List(5.0, 4.0, 3.0, 2.0, 1.0)
		val b = toJava(bBase)

		val analytic = new ArrayAnalytic(new NumericSumAnalytic[Double]())
		assertSeqsEqual(analytic.aggregate(aBase, bBase),
		                List(6.0, 6.0, 6.0, 6.0, 1.0))
	}

	test("Standard Double Array Tile Analytic") {
		val aBase = List(1.0, 2.0, 3.0, 4.0)
		val a = toJava(aBase)
		val bBase = List(5.0, 4.0, 3.0, 2.0, 1.0)
		val b = toJava(bBase)

		val analytic = new ArrayTileAnalytic[Double](new NumericSumTileAnalytic[Double](), Some("test"))
		JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"test":[4.1, 3.2, 2.3, 1.4]}"""),
		                                   analytic.storableValue(Seq(4.1, 3.2, 2.3, 1.4), TileAnalytic.Locations.Tile).get)
	}

	test("Minimum Double Array Analytic") {
		val a = List(1.0, 2.0, 3.0, 4.0)
		val b = List(5.0, 4.0, 3.0, 2.0, 1.0)

		val analytic = new ArrayAnalytic(new NumericMinAnalytic[Double]())
		assert(analytic.aggregate(analytic.defaultUnprocessedValue, analytic.aggregate(a, b)) ===
			       Seq(1.0, 2.0, 3.0, 2.0, 1.0))
	}

	test("Maximum Double Array Analytic") {
		val a = List(1.0, 2.0, 3.0, 4.0)
		val b = List(5.0, 4.0, 3.0, 2.0, 1.0)

		val analytic = new ArrayAnalytic(new NumericMaxAnalytic[Double]())
		assert(analytic.aggregate(analytic.defaultUnprocessedValue, analytic.aggregate(a, b)) ===
			       Seq(5.0, 4.0, 3.0, 4.0, 1.0))
	}
}
