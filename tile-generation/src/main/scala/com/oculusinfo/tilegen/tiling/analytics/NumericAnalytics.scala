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
 * This class is, for the moment, both a tile and a bin analytic; this means it 
 * needs the initialization parameters of both.  I've tried to make this as easy 
 * as possible, but I may revisit this at some point and break them appart.
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 */
class NumericSumAnalytic[T, JT] (analyticName: String = "sum")(implicit numeric: SimpleNumeric[T],
                                                               converter: ScalaJavaTypePair[T, JT])
		extends Analytic[T]
		with BinningAnalytic[T, JT]
		with TileAnalytic[T]
{
	def name = analyticName
	def aggregate (a: T, b: T): T = numeric.plus(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.zero
	def finish (value: T): JT = converter.asJava(value)
}

/**
 * The simplest of numeric analytics, this takes in numbers and spits out their 
 * maximum.
 * 
 * This class is, for the moment, both a tile and a bin analytic; this means it 
 * needs the initialization parameters of both.  I've tried to make this as easy 
 * as possible, but I may revisit this at some point and break them appart.
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 */
class NumericMaxAnalytic[T, JT] (analyticName: String = "maximum")(implicit numeric: SimpleNumeric[T],
                                                                   converter: ScalaJavaTypePair[T, JT])
		extends Analytic[T]
		with BinningAnalytic[T, JT]
		with TileAnalytic[T]
{
	def name = analyticName
	def aggregate (a: T, b: T): T =
		if (numeric.isNaN(a)) b
		else if (numeric.isNaN(b)) a
		else numeric.max(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.minValue
	def finish (value: T): JT = converter.asJava(value)
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
 * This class is, for the moment, both a tile and a bin analytic; this means it 
 * needs the initialization parameters of both.  I've tried to make this as easy 
 * as possible, but I may revisit this at some point and break them appart.
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMaxWithPayloadAnalytic[T, JT <: Serializable, PT <: Serializable] (analyticName: String = "maximum")(
	implicit numeric: SimpleNumeric[T],
	converter: ScalaJavaTypePair[T, JT])
		extends Analytic[(T, PT)]
		with BinningAnalytic[(T, PT), Pair[JT, PT]]
		with TileAnalytic[(T, PT)]
{
	def name = analyticName
	def aggregate (a: (T, PT), b: (T, PT)): (T, PT) =
		if (numeric.isNaN(a._1)) b
		else if (numeric.isNaN(b._1) || numeric.gt(a._1, b._1)) a
		else b
	def defaultProcessedValue: (T, PT) = (numeric.zero, null.asInstanceOf[PT])
	def defaultUnprocessedValue: (T, PT) = (numeric.zero, null.asInstanceOf[PT])
	def finish (value: (T, PT)): Pair[JT, PT] = new Pair[JT, PT](converter.asJava(value._1), value._2)
}

/**
 * The simplest of numeric analytics, this takes in numbers and spits out their 
 * minimum.
 * 
 * This class is, for the moment, both a tile and a bin analytic; this means it 
 * needs the initialization parameters of both.  I've tried to make this as easy 
 * as possible, but I may revisit this at some point and break them appart.
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 */
class NumericMinAnalytic[T, JT] (analyticName: String = "minimum")(implicit numeric: SimpleNumeric[T],
                                                                   converter: ScalaJavaTypePair[T, JT])
		extends Analytic[T]
		with BinningAnalytic[T, JT]
		with TileAnalytic[T]
{
	def name = analyticName
	def aggregate (a: T, b: T): T =
		if (numeric.isNaN(a)) b
		else if (numeric.isNaN(b)) a
		else numeric.min(a, b)
	def defaultProcessedValue: T = numeric.zero
	def defaultUnprocessedValue: T = numeric.maxValue
	def finish (value: T): JT = converter.asJava(value)
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
 * This class is, for the moment, both a tile and a bin analytic; this means it 
 * needs the initialization parameters of both.  I've tried to make this as easy 
 * as possible, but I may revisit this at some point and break them appart.
 * 
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam JT The numeric type to which to convert the data when writing to 
 *            bins (typically a java version of the same numeric type).
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMinWithPayloadAnalytic[T, JT <: Serializable, PT <: Serializable] (analyticName: String = "minimum")(
	implicit numeric: SimpleNumeric[T],
	converter: ScalaJavaTypePair[T, JT])
		extends Analytic[(T, PT)]
		with BinningAnalytic[(T, PT), Pair[JT, PT]]
		with TileAnalytic[(T, PT)]
{
	def name = analyticName
	def aggregate (a: (T, PT), b: (T, PT)): (T, PT) =
		if (numeric.isNaN(a._1)) b
		else if (numeric.isNaN(b._1) || numeric.lt(a._1, b._1)) a
		else b
	def defaultProcessedValue: (T, PT) = (numeric.zero, null.asInstanceOf[PT])
	def defaultUnprocessedValue: (T, PT) = (numeric.zero, null.asInstanceOf[PT])
	def finish (value: (T, PT)): Pair[JT, PT] = new Pair[JT, PT](converter.asJava(value._1), value._2)
}

/**
 * This analytic calculates the average (mean) value of some quantity across a 
 * data set.
 * 
 * This class is, for the moment, both a tile and a bin analytic; this means it 
 * needs the initialization parameters of both.  I've tried to make this as easy 
 * as possible, but I may revisit this at some point and break them appart.
 * 
 * As a bin analytic, the mean is calculated and inserted in the bin.  As a tile 
 * analytic, both mean and total count are inserted into any relevant metadata.
 * 
 * @tparam T The numeric type of the raw data.  The output will be a Java 
 *           Double, no matter what numeric input type is given.
 */
class NumericMeanAnalytic[T] (emptyValue: Double = JavaDouble.NaN,
                              minCount: Int = 1,
                              analyticName: String = "")(implicit numeric: SimpleNumeric[T])
		extends Analytic[(T, Int)]
		with BinningAnalytic[(T, Int), JavaDouble]
		with TileAnalytic[(T, Int)]
{
	private def statName (stat: String): String =
		if (null == analyticName || analyticName.isEmpty) stat
		else stat+" "+analyticName
	def name = statName("mean")
	def aggregate (a: (T, Int), b: (T, Int)): (T, Int) =
		(numeric.plus(a._1, b._1), (a._2 + b._2))
	def defaultProcessedValue: (T, Int) = (numeric.zero, 0)
	def defaultUnprocessedValue: (T, Int) = (numeric.zero, 0)
	def finish (value: (T, Int)): JavaDouble = {
		val (total, count) = value
		if (count < minCount) emptyValue
		else Double.box(numeric.toDouble(total) / count)
	}
	override def valueToString (value: (T, Int)): String = finish(value).toString
	override def toMap (value: (T, Int)): Map[String, Any] =
		Map(statName("count") -> value._2, statName("mean") -> finish(value))
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
class NumericStatsAnalysis[T] (emptyValue: (Double, Double) = (JavaDouble.NaN, JavaDouble.NaN),
                               minCount: Int = 1,
                               analyticName: String = "")(implicit numeric: SimpleNumeric[T])
		extends Analytic[(T, T, Int)]
		with BinningAnalytic[(T, T, Int), Pair[JavaDouble, JavaDouble]]
		with TileAnalytic[(T, T, Int)]
{
	private def statName (stat: String): String =
		if (null == analyticName || analyticName.isEmpty) stat
		else stat+" "+analyticName
	def name = statName("stats")
	def aggregate (a: (T, T, Int), b: (T, T, Int)): (T, T, Int) =
		(numeric.plus(a._1, b._1), numeric.plus(a._2, b._2), (a._3+b._3))
	def defaultProcessedValue: (T, T, Int) = (numeric.zero, numeric.zero, 0)
	def defaultUnprocessedValue: (T, T, Int) = (numeric.zero, numeric.zero, 0)
	private def calculate (value: (T, T, Int)): (Double, Double) = {
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
	def finish (value: (T, T, Int)): Pair[JavaDouble, JavaDouble] = {
		val (mean, stddev) = calculate(value)
		new Pair[JavaDouble, JavaDouble](Double.box(mean), Double.box(stddev))
	}
	override def toMap (value: (T, T, Int)): Map[String, Any] = {
		val (mean, stddev) = calculate(value)
		Map(statName("count") -> value._3,
		    statName("mean") -> mean,
		    statName("stddev") -> stddev)
	}
}
