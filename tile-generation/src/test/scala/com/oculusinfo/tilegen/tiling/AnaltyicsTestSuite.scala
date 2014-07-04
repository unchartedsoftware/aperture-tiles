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
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}

import org.scalatest.FunSuite

import com.oculusinfo.binning.util.Pair



class AnalyticsTestSuite extends FunSuite {
	def toJava (l: List[Double]) = l.map(new JavaDouble(_)).asJava

	def assertSeqsEqual[T] (a: Seq[T], b: Seq[T]): Unit = {
		assert(a.size === b.size)
		for (n <- 0 until a.size) assert(a(n) === b(n))
	}

	def assertListsEqual[T] (a: JavaList[T], b: JavaList[T]): Unit = {
		assert(a.size === b.size)
		for (n <- 0 until a.size) assert(a.get(n) === b.get(n))
	}

	test("Standard Double Analytic") {
		val analytic = new SumDoubleAnalytic
		assert(0.0 === analytic.defaultProcessedValue)
		assert(0.0 === analytic.defaultUnprocessedValue)
		assert(3.0 === analytic.aggregate(1.0, 2.0))
	}
	
	test("Standard Double Binning Analytic") {
		val analytic = new SumDoubleAnalytic with StandardDoubleBinningAnalytic

		assert(4.0 === analytic.finish(4.0).doubleValue)
	}

	test("Standard Double Tile Analytic") {
		val analytic = new SumDoubleAnalytic with TileAnalytic[Double] {
			def name = "test"
		}

		assert("1.3" === analytic.valueToString(1.3))
	}

	test("Minimum Double Analytic") {
		val analytic = new MinimumDoubleAnalytic
		assert(1.0 === analytic.aggregate(new JavaDouble(1.0),
		                                  new JavaDouble(2.0)).doubleValue)
	}

	test("Maximum Double Analytic") {
		val analytic = new MaximumDoubleAnalytic
		assert(2.0 === analytic.aggregate(new JavaDouble(1.0),
		                                  new JavaDouble(2.0)).doubleValue)
	}

	test("Standard Double Array Analytic") {
		val aBase = List(1.0, 2.0, 3.0, 4.0)
		val a = toJava(aBase)
		val bBase = List(5.0, 4.0, 3.0, 2.0, 1.0)
		val b = toJava(bBase)

		val analytic = new SumDoubleArrayAnalytic
		assertSeqsEqual(analytic.aggregate(aBase, bBase),
		                List(6.0, 6.0, 6.0, 6.0, 1.0))
	}
	
	test("Standard Double Array Tile Analytic") {
		val aBase = List(1.0, 2.0, 3.0, 4.0)
		val a = toJava(aBase)
		val bBase = List(5.0, 4.0, 3.0, 2.0, 1.0)
		val b = toJava(bBase)

		val analytic = new SumDoubleArrayAnalytic with StandardDoubleArrayTileAnalytic {
			def name="test"
		}
		assert("[4.1,3.2,2.3,1.4]" === analytic.valueToString(List(4.1, 3.2, 2.3, 1.4)))
	}

	test("Minimum Double Array Analytic") {
		val a = List(1.0, 2.0, 3.0, 4.0)
		val b = List(5.0, 4.0, 3.0, 2.0, 1.0)

		val analytic = new MinimumDoubleArrayAnalytic
		assert(analytic.aggregate(analytic.defaultUnprocessedValue, analytic.aggregate(a, b)) ===
			       Seq(1.0, 2.0, 3.0, 2.0, 1.0))
	}

	test("Maximum Double Array Analytic") {
		val a = List(1.0, 2.0, 3.0, 4.0)
		val b = List(5.0, 4.0, 3.0, 2.0, 1.0)

		val analytic = new MaximumDoubleArrayAnalytic
		assert(analytic.aggregate(analytic.defaultUnprocessedValue, analytic.aggregate(a, b)) ===
			       Seq(5.0, 4.0, 3.0, 4.0, 1.0))
	}

	test("String Score Analytic") {
		val a = Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
		val b = Map("a" -> 5.0, "b" -> 4.0, "c" -> 3.0, "d" -> 2.0, "e" -> 1.0)

		val analytic = new StringScoreAnalytic
		assert(Map("a" -> 6.0, "b" -> 6.0, "c" -> 6.0, "d" -> 6.0, "e" -> 1.0) ===
			       analytic.aggregate(a, b))
	}

	test("String Score Tile Analytic") {
		val a = Map("a" -> 1.0, "b" -> 2.0, "c" -> 3.0, "d" -> 4.0)
		val b = Map("a" -> 5.0, "b" -> 4.0, "c" -> 3.0, "d" -> 2.0, "e" -> 1.0)

		val analytic = new StringScoreAnalytic with StandardStringScoreTileAnalytic {
			def name = "test"
		}
		println("value: \""+analytic.valueToString(a)+"\"")
		assert("[\"a\":1.0,\"b\":2.0,\"c\":3.0,\"d\":4.0]" === analytic.valueToString(a))
	}
}
