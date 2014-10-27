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

import com.oculusinfo.binning.util.Pair


class NumericSumAnalytic[T, JT](analyticName: String = "sum")(implicit numeric: SimpleNumeric[T],
                                                              converter: ScalaJavaTypePair[T, JT])
		extends Analytic[T] with BinningAnalytic[T, JT] with TileAnalytic[T]
{
	def aggregate (a: T, b: T): T = numeric.plus(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.zero
	def finish (value: T): JT = converter.asJava(value)
	def name = analyticName
}

class NumericMaxAnalytic[T, JT](analyticName: String = "max")(implicit numeric: SimpleNumeric[T],
                                                              converter: ScalaJavaTypePair[T, JT])
		extends Analytic[T] with BinningAnalytic[T, JT] with TileAnalytic[T]
{
	def aggregate (a: T, b: T): T = numeric.max(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.minValue
	def finish (value: T): JT = converter.asJava(value)
	def name = analyticName
}

class NumericMinAnalytic[T, JT](analyticName: String = "min")(implicit numeric: SimpleNumeric[T],
                                                              converter: ScalaJavaTypePair[T, JT])
		extends Analytic[T] with BinningAnalytic[T, JT] with TileAnalytic[T]
{
	def aggregate (a: T, b: T): T = numeric.min(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.maxValue
	def finish (value: T): JT = converter.asJava(value)
	def name = analyticName
}

class NumericMeanAnalytic[T, JT](analyticName: String = "mean")(implicit numeric: SimpleNumeric[T],
                                                                converter: ScalaJavaTypePair[T, JT])
		extends Analytic[(T, Int)] with BinningAnalytic[(T, Int), JT] with TileAnalytic[(T, Int)]
{
	def aggregate (a: (T, Int), b: (T, Int)): (T, Int) =
		(numeric.plus(a._1, b._1), (a._2 + b._2))
	def defaultProcessedValue: (T, Int) = (numeric.zero, 0)
	def defaultUnprocessedValue: (T, Int) = (numeric.zero, 0)
	def finish (value: (T, Int)): JT = converter.asJava(numeric.div(value._1, numeric.fromInt(value._2)))
	override def valueToString (value: (T, Int)): String = finish(value).toString
	def name = analyticName
}

class NumericStatsAnalysis[T, JT <: Serializable](implicit numeric: SimpleNumeric[T],
                                  converter: ScalaJavaTypePair[T, JT])
		extends Analytic[(T, T, Int)] with BinningAnalytic[(T, T, Int), Pair[JT, JT]] with TileAnalytic[(T, T, Int)]
{
	def aggregate (a: (T, T, Int), b: (T, T, Int)): (T, T, Int) =
		(numeric.plus(a._1, b._1), numeric.plus(a._2, b._2), (a._3+b._3))
	def defaultProcessedValue: (T, T, Int) = (numeric.zero, numeric.zero, 0)
	def defaultUnprocessedValue: (T, T, Int) = (numeric.zero, numeric.zero, 0)
	private def calculate (value: (T, T, Int)): (T, T) = {
		val mean = numeric.div(value._1, numeric.fromInt(value._3))
		val stddev = numeric.minus(numeric.div(value._2, numeric.fromInt(value._3)),
		                           numeric.times(mean, mean))
		(mean, stddev)
	}
	def finish (value: (T, T, Int)): Pair[JT, JT] = {
		val (mean, stddev) = calculate(value)
		new Pair[JT, JT](converter.asJava(mean), converter.asJava(stddev))
	}
	override def toMap (value: (T, T, Int)): Map[String, Any] = {
		val (mean, stddev) = calculate(value)
		Map("mean" -> mean, "stddev" -> stddev)
	}
	def name = "stats"
}
