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

package com.oculusinfo.tilegen.spark



import org.apache.spark.AccumulableParam
import org.apache.spark.AccumulatorParam


class MinMaxAccumulableParam
		extends AccumulableParam[Map[Int, (Double, Double)], (Int, Double)]
		with Serializable {
	private val defaultValue = (Double.MaxValue, Double.MinValue)
	def addAccumulator (currentValue: Map[Int, (Double, Double)],
	                    addition: (Int, Double)): Map[Int, (Double, Double)] = {
		val level = addition._1
		val value = addition._2
		val curMinMax = currentValue.getOrElse(level, defaultValue)
		currentValue + ((level, (value min curMinMax._1, value max curMinMax._2)))
	}

	def addInPlace (a: Map[Int, (Double, Double)],
	                b: Map[Int, (Double, Double)]): Map[Int, (Double, Double)] = {
		val keys = a.keySet union b.keySet
		keys.map(key =>
			{
				val aVal = a.getOrElse(key, defaultValue)
				val bVal = b.getOrElse(key, defaultValue)

				(key -> (aVal._1 min bVal._1, aVal._2 max bVal._2))
			}
		).toMap
	}

	def zero (initialValue: Map[Int, (Double, Double)]): Map[Int, (Double, Double)] =
		Map[Int, (Double, Double)]()
}

class DoubleMinAccumulatorParam extends AccumulatorParam[Double] {
	def addInPlace(t1: Double, t2: Double): Double = t1 min t2
	def zero(initialValue: Double) = Double.MaxValue
}
class DoubleMaxAccumulatorParam extends AccumulatorParam[Double] {
	def addInPlace(t1: Double, t2: Double): Double = t1 max t2
	def zero(initialValue: Double) = Double.MinValue
}
class IntMinAccumulatorParam extends AccumulatorParam[Int] {
	def addInPlace(t1: Int, t2: Int): Int = t1 min t2
	def zero(initialValue: Int) = Int.MaxValue
}
class IntMaxAccumulatorParam extends AccumulatorParam[Int] {
	def addInPlace(t1: Int, t2: Int): Int = t1 max t2
	def zero(initialValue: Int) = Int.MinValue
}
