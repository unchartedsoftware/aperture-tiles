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


import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Float => JavaFloat}
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}
import scala.collection.JavaConverters._
import org.scalatest.FunSuite
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.DenseTileData
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme.ipArrayToString
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme.longToIPArray
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme
import com.oculusinfo.binning.util.JSONUtilitiesTests
import org.json.JSONObject



class NumericAnalyticsTestSuite extends FunSuite {
	import TileAnalytic.Locations._
	def toJava (l: List[Double]) = l.map(new JavaDouble(_)).asJava

	def assertSeqsEqual[T] (a: Seq[T], b: Seq[T]): Unit = {
		assert(a.size === b.size)
		for (n <- 0 until a.size) assert(a(n) === b(n))
	}

	def assertListsEqual[T] (a: JavaList[T], b: JavaList[T]): Unit = {
		assert(a.size === b.size)
		for (n <- 0 until a.size) assert(a.get(n) === b.get(n))
	}

	// Full tests on each type for the summation analytic
	test("Standard Integer Analytic") {
		// check base
		val analytic = new NumericSumTileAnalytic[Int]()
		assert(0 === analytic.defaultProcessedValue)
		assert(0 === analytic.defaultUnprocessedValue)
		assert(3 === analytic.aggregate(1, 2))
		assert(analytic.aggregate(1, 2).isInstanceOf[Int])

		// Check tile analytic output
		JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"sum": 4}"""),
		                                   analytic.storableValue(4, Tile).get)
	}

	test("Standard Long Analytic") {
		// check base
		val analytic = new NumericSumTileAnalytic[Long]()
		assert(0l === analytic.defaultProcessedValue)
		assert(0l === analytic.defaultUnprocessedValue)
		assert(3l === analytic.aggregate(1l, 2l))
		assert(analytic.aggregate(1L, 2L).isInstanceOf[Long])

		// Check tile analytic output
		JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"sum": 4444444444}"""),
		                                   analytic.storableValue(4444444444L, Tile).get)
	}

	test("Standard Float Analytic") {
		// check base
		val analytic = new NumericSumTileAnalytic[Float]()
		assert(0.0f === analytic.defaultProcessedValue)
		assert(0.0f === analytic.defaultUnprocessedValue)
		assert(3.0f === analytic.aggregate(1.0f, 2.0f))
		assert(analytic.aggregate(1.0f, 2.0f).isInstanceOf[Float])

		// Check tile analytic output
		val expected = new JSONObject()
		expected.put("sum", Float.box(4.2f))
		JSONUtilitiesTests.assertJsonEqual(expected, analytic.storableValue(4.2f, Tile).get)
	}

	test("Standard Double Analytic") {
		// check base
		val analytic = new NumericSumTileAnalytic[Double]()
		assert(0.0 === analytic.defaultProcessedValue)
		assert(0.0 === analytic.defaultUnprocessedValue)
		assert(3.0 === analytic.aggregate(1.0, 2.0))
		assert(analytic.aggregate(1.0, 2.0).isInstanceOf[Double])
		assert(analytic.aggregate(1.0f, 2.0f).isInstanceOf[Double])
		assert(analytic.aggregate(1L, 2L).isInstanceOf[Double])
		assert(analytic.aggregate(1, 2).isInstanceOf[Double])

		// Check tile analytic output
		JSONUtilitiesTests.assertJsonEqual(new JSONObject("""{"sum": 4.3}"""),
		                                   analytic.storableValue(4.3, Tile).get)
	}

	// Having testing the summation analytic fully for each type, we just do
	// type checking on non-Int types for other analytics
	test("Minimum Int Analytic") {
		val analytic = new NumericMinAnalytic[Int]()
		assert(1 === analytic.aggregate(1, 2))
		assert(1 === analytic.aggregate(2, 1))

		assert(analytic.aggregate(1, 2).isInstanceOf[Int])
	}

	test("Minimum Double Analytic ignores NaN") {
		val sampleTile = new DenseTileData[JavaDouble](new TileIndex(0, 0, 0, 4, 4), JavaDouble.NaN)
		sampleTile.setBin(0, 0, 1.0)
		sampleTile.setBin(1, 1, 2.0)
		sampleTile.setBin(2, 2, 3.0)
		sampleTile.setBin(3, 3, 4.0)

		val minConvert = AnalysisDescriptionTileWrapper.acrossTile((d: JavaDouble) => d.doubleValue,
		                                                           new NumericMinTileAnalytic[Double]())
		assert(1.0 === minConvert(sampleTile))
	}

	//	test("Minimum Double with Payload analytic") {
	//		val analytic = new NumericMinWithPayloadAnalytic[Double, JavaDouble, String]()
	//		assert((1.0, "a") === analytic.aggregate((1.0, "a"), (2.0, "b")))
	//		assert((1.0, "a") === analytic.aggregate((2.0, "b"), (1.0, "a")))
	//		assert((1.0, "a") === analytic.aggregate((1.0, "a"), (JavaDouble.NaN, "b")))
	//		assert((1.0, "a") === analytic.aggregate((JavaDouble.NaN, "b"), (1.0, "a")))
	//	}

	test("Minimum Long Analytic") {
		assert(new NumericMinAnalytic[Long]().aggregate(1L, 2L).isInstanceOf[Long])
	}

	test("Minimum Float Analytic") {
		assert(new NumericMinAnalytic[Float]().aggregate(1.1f, 2.2f).isInstanceOf[Float])
	}

	test("Minimum Double Analytic") {
		assert(new NumericMinAnalytic[Double]().aggregate(1.2, 2.4).isInstanceOf[Double])
	}

	test("Maximum Int Analytic") {
		val analytic = new NumericMaxAnalytic[Int]()
		assert(2 === analytic.aggregate(1, 2))
		assert(2 === analytic.aggregate(2, 1))

		assert(analytic.aggregate(1, 2).isInstanceOf[Int])
	}

	test("Maximum Double Analytic ignores NaN") {
		val sampleTile = new DenseTileData[JavaDouble](new TileIndex(0, 0, 0, 4, 4), JavaDouble.NaN)
		sampleTile.setBin(0, 0, 1.0)
		sampleTile.setBin(1, 1, 2.0)
		sampleTile.setBin(2, 2, 3.0)
		sampleTile.setBin(3, 3, 4.0)
		val maxConvert = AnalysisDescriptionTileWrapper.acrossTile((d: JavaDouble) => d.doubleValue,
		                                                           new NumericMaxTileAnalytic[Double]())
		assert(4.0 === maxConvert(sampleTile))
	}

	//	test("Maximum Double with Payload analytic") {
	//		val analytic = new NumericMaxWithPayloadAnalyticddd[Double, JavaDouble, String]()
	//		assert((1.0, "a") === analytic.aggregate((1.0, "a"), (0.0, "b")))
	//		assert((1.0, "a") === analytic.aggregate((0.0, "b"), (1.0, "a")))
	//		assert((1.0, "a") === analytic.aggregate((1.0, "a"), (JavaDouble.NaN, "b")))
	//		assert((1.0, "a") === analytic.aggregate((JavaDouble.NaN, "b"), (1.0, "a")))
	//	}

	test("Maximum Long Analytic") {
		assert(new NumericMaxAnalytic[Long]().aggregate(1L, 2L).isInstanceOf[Long])
	}

	test("Maximum Float Analytic") {
		assert(new NumericMaxAnalytic[Float]().aggregate(1.1f, 2.2f).isInstanceOf[Float])
	}

	test("Maximum Double Analytic") {
		assert(new NumericMaxAnalytic[Double]().aggregate(1.2, 2.4).isInstanceOf[Double])
	}


	test("Standard Mean Int Binning Analytic") {
		// Test normal values
		val analytic = new NumericMeanBinningAnalytic[Int]()
		assert((0, 0) === analytic.defaultProcessedValue)
		assert((0, 0) === analytic.defaultUnprocessedValue)
		assert((3, 5) === analytic.aggregate((2, 2), (1, 3)))
		assert(analytic.aggregate((2, 1), (1, 2))._1.isInstanceOf[Int])
		assert(0.6 === analytic.finish(3, 5))
		assert(JavaDouble.isNaN(analytic.finish((0, 0))))

		// Test default values
		val analyticDef1 = new NumericMeanBinningAnalytic[Int](emptyValue=1)
		assert(1 == analyticDef1.finish((3, 0)))
		val analyticDef2 = new NumericMeanBinningAnalytic[Int](emptyValue=3)
		assert(3 == analyticDef2.finish((1, 0)))

		// Test minimum count
		val analyticCount1 = new NumericMeanBinningAnalytic[Int](minCount=4)
		assert(analyticCount1.finish((3, 3)).isNaN)
		assert(1.25 === analyticCount1.finish((5, 4)))
	}

	test("Standard Mean Long Binning Analytic") {
		// Test normal values
		val analytic = new NumericMeanAnalytic[Long]()
		assert(analytic.aggregate((2L, 1), (1L, 2))._1.isInstanceOf[Long])
	}

	test("Standard Mean Float Binning Analytic") {
		// Test normal values
		val analytic = new NumericMeanAnalytic[Float]()
		assert(analytic.aggregate((2.0f, 1), (1.0f, 2))._1.isInstanceOf[Float])
	}

	test("Standard Mean Double Binning Analytic") {
		// Test normal values
		val analytic = new NumericMeanAnalytic[Double]()
		assert(analytic.aggregate((2.0, 1), (1.0, 2))._1.isInstanceOf[Double])
	}
}
