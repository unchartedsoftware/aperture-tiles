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

import com.oculusinfo.binning.util.Pair



/**
 * The simplest of numeric analytics, this takes in numbers and spits out their 
 * sum.
 * 
 * @tparam T The numeric type of data of which to sum.
 */
class NumericSumAnalytic[T] (implicit numeric: SimpleNumeric[T]) extends Analytic[T] {
	def aggregate (a: T, b: T): T = numeric.plus(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.zero
}
/**
 * {@see NumericSumAnalytic}
 * 
 * @tparam T The numeric type of data of which to sum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 */
class NumericSumBinningAnalytic[T, JT] (implicit numeric: SimpleNumeric[T],
                                        converter: ScalaJavaTypePair[T, JT])
		extends NumericSumAnalytic[T]
		with BinningAnalytic[T, JT]
{
	def finish (value: T): JT = converter.asJava(value)
}
/**
 * {@see NumericSumAnalytic}
 * 
 * @tparam T The numeric type of data of which to sum.
 */
class NumericSumTileAnalytic[T] (analyticName: String = "sum")(implicit numeric: SimpleNumeric[T])
		extends NumericSumAnalytic[T]
		with TileAnalytic[T]
{
	def name = analyticName
}



/**
 * The simplest of numeric analytics, this takes in numbers and spits out their 
 * maximum.
 * 
 * @tparam T The numeric type of data of which to take the maximum.
 */
class NumericMaxAnalytic[T] (implicit numeric: SimpleNumeric[T]) extends Analytic[T] {
	def aggregate (a: T, b: T): T =
		if (numeric.isNaN(a)) b
		else if (numeric.isNaN(b)) a
		else numeric.max(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.minValue
}
/**
 * {@see NumericMaxAnalytic}
 * 
 * @tparam T The numeric type of data of which to take the maximum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 */
class NumericMaxBinningAnalytic[T, JT] (implicit numeric: SimpleNumeric[T],
                                        converter: ScalaJavaTypePair[T, JT])
		extends NumericMaxAnalytic[T]
		with BinningAnalytic[T, JT]
{
	def finish (value: T): JT = converter.asJava(value)
}
/**
 * {@see NumericMaxAnalytic}
 * 
 * @tparam T The numeric type of data of which to take the maximum.
 */
class NumericMaxTileAnalytic[T] (analyticName: String = "maximum")(implicit numeric: SimpleNumeric[T])
		extends NumericMaxAnalytic[T]
		with TileAnalytic[T]
{
	def name = analyticName
}



/**
 * This class takes in numbers and some piece of data associate with the 
 * number (the "payload"), and spits out the maximum of its input numbers, as 
 * well as the payload associated with that maximum.
 * 
 * For example, the payload could be the location of the datum, and the result
 * would be the maximum value of the data in the data set, as well as the 
 * location at which that maximum occured.
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMaxWithPayloadAnalytic[T, PT <: Serializable] (implicit numeric: SimpleNumeric[T])
		extends Analytic[(T, PT)]
{
	def aggregate (a: (T, PT), b: (T, PT)): (T, PT) =
		if (numeric.isNaN(a._1)) b
		else if (numeric.isNaN(b._1) || numeric.gt(a._1, b._1)) a
		else b
	def defaultProcessedValue: (T, PT) = (numeric.zero, null.asInstanceOf[PT])
	def defaultUnprocessedValue: (T, PT) = (numeric.zero, null.asInstanceOf[PT])
}
/**
 * {@see NumericMaxWithPayloadAnalytic}
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMaxWithPayloadBinningAnalytic[T,
                                           JT <: Serializable,
                                           PT <: Serializable](implicit numeric: SimpleNumeric[T],
                                                               converter: ScalaJavaTypePair[T, JT])
		extends NumericMaxWithPayloadAnalytic[T, PT]
		with BinningAnalytic[(T, PT), Pair[JT, PT]]
{
	def finish (value: (T, PT)): Pair[JT, PT] = new Pair[JT, PT](converter.asJava(value._1), value._2)
}
/**
 * {@see NumericMaxWithPayloadAnalytic}
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMaxWithPayloadTileAnalytic[T, PT <: Serializable] (
	analyticName: String = "maximum")(
	implicit numeric: SimpleNumeric[T])
		extends NumericMaxWithPayloadAnalytic[T, PT]
		with TileAnalytic[(T, PT)]
{
	def name = analyticName
}



/**
 * The simplest of numeric analytics, this takes in numbers and spits out their 
 * minimum.
 * 
 * @tparam T The numeric type of data of which to take the minimum.
 */
class NumericMinAnalytic[T] (implicit numeric: SimpleNumeric[T]) extends Analytic[T] {
	def aggregate (a: T, b: T): T =
		if (numeric.isNaN(a)) b
		else if (numeric.isNaN(b)) a
		else numeric.min(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.maxValue
}
/**
 * {@see NumericMinAnalytic}
 * 
 * @tparam T The numeric type of data of which to take the minimum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 */
class NumericMinBinningAnalytic[T, JT] (implicit numeric: SimpleNumeric[T],
                                        converter: ScalaJavaTypePair[T, JT])
		extends NumericMinAnalytic[T]
		with BinningAnalytic[T, JT]
{
	def finish (value: T): JT = converter.asJava(value)
}
/**
 * {@see NumericMinAnalytic}
 * 
 * @tparam T The numeric type of data of which to take the minimum.
 */
class NumericMinTileAnalytic[T] (analyticName: String = "minimum")(implicit numeric: SimpleNumeric[T])
		extends NumericMinAnalytic[T]
		with TileAnalytic[T]
{
	def name = analyticName
}



/**
 * This class takes in numbers and some piece of data associate with the 
 * number (the "payload"), and spits out the minimum of its input numbers, as 
 * well as the payload associated with that minimum.
 * 
 * For example, the payload could be the location of the datum, and the result
 * would be the minimum value of the data in the data set, as well as the 
 * location at which that minimum occured.
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMinWithPayloadAnalytic[T, PT <: Serializable] (implicit numeric: SimpleNumeric[T])
		extends Analytic[(T, PT)]
{
	def aggregate (a: (T, PT), b: (T, PT)): (T, PT) =
		if (numeric.isNaN(a._1)) b
		else if (numeric.isNaN(b._1) || numeric.lt(a._1, b._1)) a
		else b
	def defaultProcessedValue: (T, PT) = (numeric.zero, null.asInstanceOf[PT])
	def defaultUnprocessedValue: (T, PT) = (numeric.zero, null.asInstanceOf[PT])
}
/**
 * {@see NumericMinWithPayloadAnalytic}
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMinWithPayloadBinningAnalytic[T, JT <: Serializable, PT <: Serializable] (
	implicit numeric: SimpleNumeric[T],	converter: ScalaJavaTypePair[T, JT])
		extends NumericMinWithPayloadAnalytic[T, PT]
		with BinningAnalytic[(T, PT), Pair[JT, PT]]
{
	def finish (value: (T, PT)): Pair[JT, PT] = new Pair[JT, PT](converter.asJava(value._1), value._2)
}
/**
 * {@see NumericMinWithPayloadAnalytic}
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMinWithPayloadTileAnalytic[T, PT <: Serializable] (
	analyticName: String = "minimum")(implicit numeric: SimpleNumeric[T])
		extends NumericMinWithPayloadAnalytic[T, PT]
		with TileAnalytic[(T, PT)]
{
	def name = analyticName
}



/**
 * This analytic calculates the average (mean) value of some quantity across a 
 * data set.
 * 
 * @tparam T The numeric type of the raw data.  The output will be a Java 
 *           Double, no matter what numeric input type is given.
 */
class NumericMeanAnalytic[T] (emptyValue: Double = JavaDouble.NaN,
                              minCount: Int = 1)(implicit numeric: SimpleNumeric[T])
		extends Analytic[(T, Int)]
{
	def aggregate (a: (T, Int), b: (T, Int)): (T, Int) =
		(numeric.plus(a._1, b._1), (a._2 + b._2))
	def defaultProcessedValue: (T, Int) = (numeric.zero, 0)
	def defaultUnprocessedValue: (T, Int) = (numeric.zero, 0)
	protected def calculate (value: (T, Int)): Double = {
		val (total, count) = value
		if (count < minCount) emptyValue
		else numeric.toDouble(total)/count
	}
}
/**
 * {@see NumericMeanAnalytic}
 * 
 * @tparam T The numeric type of the raw data.  The output will be a Java 
 *           Double, no matter what numeric input type is given.
 */
class NumericMeanBinningAnalytic[T] (emptyValue: Double = JavaDouble.NaN,
                                     minCount: Int = 1)(implicit numeric: SimpleNumeric[T])
		extends NumericMeanAnalytic[T](emptyValue, minCount)
		with BinningAnalytic[(T, Int), JavaDouble]
{
	def finish (value: (T, Int)): JavaDouble = Double.box(calculate(value))
}
/**
 * {@see NumericMeanAnalytic}
 * 
 * Both the mean and the total count are inserted into any relevant metadata.
 * 
 * @tparam T The numeric type of the raw data.  The output will be a Java 
 *           Double, no matter what numeric input type is given.
 */
class NumericMeanTileAnalytic[T] (emptyValue: Double = JavaDouble.NaN,
                                  minCount: Int = 1,
                                  analyticName: String = "")(implicit numeric: SimpleNumeric[T])
		extends NumericMeanAnalytic[T](emptyValue, minCount)
		with TileAnalytic[(T, Int)]
{
	private def statName (stat: String): String =
		if (null == analyticName || analyticName.isEmpty) stat
		else stat+" "+analyticName
	def name = statName("mean")
	override def valueToString (value: (T, Int)): String = calculate(value).toString
	override def toMap (value: (T, Int)): Map[String, Any] =
		Map(statName("count") -> value._2, statName("mean") -> calculate(value))
}



/**
 * This analytic calculates the average (mean) value and standard deviation of 
 * some quantity across a data set.
 * 
 * This class is, for the moment, both a tile and a bin analytic; this means it 
 * needs the initialization parameters of both.  I've tried to make this as easy 
 * as possible, but I may revisit this at some point and break them appart.
 * 
 * As a bin analytic, the mean and standard deviation are calculated and 
 * inserted in the bin as a pair.  As a tile analytic, mean, standard deviation,
 * and total count are inserted into any relevant metadata.
 * 
 * @tparam T The numeric type of the raw data.  The output will be a pair of 
 *           Java Doubles, no matter what numeric input type is given.
 */
class NumericStatsAnalytic[T] (emptyValue: (Double, Double) = (JavaDouble.NaN, JavaDouble.NaN),
                               minCount: Int = 1)(implicit numeric: SimpleNumeric[T])
		extends Analytic[(T, T, Int)]
{
	def aggregate (a: (T, T, Int), b: (T, T, Int)): (T, T, Int) =
		(numeric.plus(a._1, b._1), numeric.plus(a._2, b._2), (a._3+b._3))
	def defaultProcessedValue: (T, T, Int) = (numeric.zero, numeric.zero, 0)
	def defaultUnprocessedValue: (T, T, Int) = (numeric.zero, numeric.zero, 0)
	protected def calculate (value: (T, T, Int)): (Double, Double) = {
		val count = value._3
		val sumX = numeric.toDouble(value._1)
		val sumX2 = numeric.toDouble(value._2)
		if (count < minCount) emptyValue
		else {
			val mean = sumX/count
			val stddev = sumX2/count - mean*mean
			(mean, stddev)
		}
	}
}
class NumericStatsBinningAnalytic[T] (emptyValue: (Double, Double) = (JavaDouble.NaN, JavaDouble.NaN),
                                      minCount: Int = 1)(implicit numeric: SimpleNumeric[T])
		extends NumericStatsAnalytic[T]
		with BinningAnalytic[(T, T, Int), Pair[JavaDouble, JavaDouble]]
{
	def finish (value: (T, T, Int)): Pair[JavaDouble, JavaDouble] = {
		val (mean, stddev) = calculate(value)
		new Pair[JavaDouble, JavaDouble](Double.box(mean), Double.box(stddev))
	}
}
class NumericStatstileAnalytic[T] (emptyValue: (Double, Double) = (JavaDouble.NaN, JavaDouble.NaN),
                                   minCount: Int = 1,
                                   analyticName: String = "")(implicit numeric: SimpleNumeric[T])
		extends NumericStatsAnalytic[T]
		with TileAnalytic[(T, T, Int)]
{
	private def statName (stat: String): String =
		if (null == analyticName || analyticName.isEmpty) stat
		else stat+" "+analyticName
	def name = statName("stats")
	override def valueToString (value: (T, T, Int)): String = {
		val (mean, stddev) = calculate(value)
		mean.toString+","+stddev.toString
	}
	override def toMap (value: (T, T, Int)): Map[String, Any] = {
		val (mean, stddev) = calculate(value)
		Map(statName("count") -> value._3,
		    statName("mean") -> mean,
		    statName("stddev") -> stddev)
	}
}
