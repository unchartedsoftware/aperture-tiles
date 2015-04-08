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
import java.util.{List => JavaList}
import com.oculusinfo.factory.ConfigurableFactory
import com.oculusinfo.factory.properties.{IntegerProperty, DoubleProperty, StringProperty}
import com.oculusinfo.factory.util.Pair
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme._
import com.oculusinfo.tilegen.util.{NumericallyConfigurableFactory, ExtendedNumeric, TypeConversion}
import org.json.JSONObject
import scala.reflect.ClassTag
import com.oculusinfo.tilegen.util.OptionsFactoryMixin


object NumericAnalyticFactory {
	val AGGREGATION_TYPE = new StringProperty("aggregation", "The type of analytic to use", "sum",
	                                          Array[String]("sum", "min", "max", "mean", "stats"))
	val MIN_COUNT = new IntegerProperty("mincount", "The minimum number of data points a bin must have to be considered for statistical binning", 1)
	val EMPTY_MEAN = new DoubleProperty("emptymean", "The value to use as a bin's mean when it doesn't have enough data", JavaDouble.NaN)
	val EMPTY_DEV = new DoubleProperty("emptydev", "The value to use as a bin's standard deviation when it doesn't have enough data", JavaDouble.NaN)
}
class NumericBinningAnalyticFactory (name: String,
                                     parent: ConfigurableFactory[_],
                                     path: JavaList[String])
		extends NumericallyConfigurableFactory[BinningAnalytic[_, _]](name, classOf[BinningAnalytic[_, _]], parent, path, true)
{
	import NumericAnalyticFactory._
	addProperty(AGGREGATION_TYPE)
	addProperty(EMPTY_MEAN)
	addProperty(EMPTY_DEV)
	addProperty(MIN_COUNT)



	def this (parent: ConfigurableFactory[_], path: JavaList[String]) = this(null, parent, path)


	/**
	 * This function serves the purpose of the {@link ConfigurableFactory#create} function in normal factories.
	 * It includes generic numeric types to allow factories to create objects generified with the appropriate generic
	 * numeric.
	 * @param tag A ClassTag of the scala numeric base type
	 * @param numeric The scala extended numeric object that represents the type to use
	 * @param conversion A conversion object between the scala and java numeric types
	 * @tparam ST The scala extended numeric type to use
	 * @tparam JT The java numeric type to use
	 * @return The factory to be returned by ConfigurableFactory.create.
	 */
	override protected def typedCreate[ST, JT](tag: ClassTag[ST], numeric: ExtendedNumeric[ST], conversion: TypeConversion[ST, JT]): BinningAnalytic[_, _] = {
		getPropertyValue(AGGREGATION_TYPE).toLowerCase match {
			case "sum" =>     new NumericSumBinningAnalytic[ST, JT]()(numeric, conversion)
			case "min" =>     new NumericMinBinningAnalytic[ST, JT]()(numeric, conversion)
			case "minimum" => new NumericMinBinningAnalytic[ST, JT]()(numeric, conversion)
			case "max" =>     new NumericMaxBinningAnalytic[ST, JT]()(numeric, conversion)
			case "maximum" => new NumericMaxBinningAnalytic[ST, JT]()(numeric, conversion)
			case "mean" =>    new NumericMeanBinningAnalytic[ST](getPropertyValue(EMPTY_MEAN), getPropertyValue(MIN_COUNT))(numeric)
			case "average" => new NumericMeanBinningAnalytic[ST](getPropertyValue(EMPTY_MEAN), getPropertyValue(MIN_COUNT))(numeric)
			case "stats" =>   new NumericStatsBinningAnalytic[ST]((getPropertyValue(EMPTY_MEAN), getPropertyValue(EMPTY_DEV)),
			                                                      getPropertyValue(MIN_COUNT))(numeric)
		}
	}
}

object NumericTileAnalyticFactory {
	val ANALYTIC_NAME = new StringProperty("name", "The name of the tile analytic", "sum")
	
}
class NumericTileAnalyticFactory (name: String = null,
                                  parent: ConfigurableFactory[_],
                                  path: JavaList[String])
		extends NumericallyConfigurableFactory[TileAnalytic[_]](name, classOf[TileAnalytic[_]], parent, path, true)
		with OptionsFactoryMixin[TileAnalytic[_]]
{
	import NumericAnalyticFactory._
	import NumericTileAnalyticFactory._
	addProperty(AGGREGATION_TYPE)
	addProperty(ANALYTIC_NAME)
	addProperty(EMPTY_MEAN)
	addProperty(EMPTY_DEV)
	addProperty(MIN_COUNT)

	/**
	 * This function serves the purpose of the {@link ConfigurableFactory#create} function in normal factories.
	 * It includes generic numeric types to allow factories to create objects generified with the appropriate generic
	 * numeric.
	 * @param tag A ClassTag of the scala numeric base type
	 * @param numeric The scala extended numeric object that represents the type to use
	 * @param conversion A conversion object between the scala and java numeric types
	 * @tparam ST The scala extended numeric type to use
	 * @tparam JT The java numeric type to use
	 * @return The factory to be returned by ConfigurableFactory.create.
	 */
	override protected def typedCreate[ST, JT](tag: ClassTag[ST], numeric: ExtendedNumeric[ST], conversion: TypeConversion[ST, JT]): TileAnalytic[_] = {
		val name = optionalGet(ANALYTIC_NAME)
		getPropertyValue(AGGREGATION_TYPE) match {
			case "sum"     => new NumericSumTileAnalytic[ST](name)(numeric)
			case "min"     => new NumericMinTileAnalytic[ST](name)(numeric)
			case "minimum" => new NumericMinTileAnalytic[ST](name)(numeric)
			case "max"     => new NumericMaxTileAnalytic[ST](name)(numeric)
			case "maximum" => new NumericMaxTileAnalytic[ST](name)(numeric)
			case "mean"    => new NumericMeanTileAnalytic[ST](getPropertyValue(EMPTY_MEAN), getPropertyValue(MIN_COUNT), name)(numeric)
			case "average" => new NumericMeanTileAnalytic[ST](getPropertyValue(EMPTY_MEAN), getPropertyValue(MIN_COUNT), name)(numeric)
			case "stats"   => new NumericStatsTileAnalytic[ST]((getPropertyValue(EMPTY_MEAN), getPropertyValue(EMPTY_DEV)),
			                                                   getPropertyValue(MIN_COUNT), name)(numeric)
		}
	}
}
/**
 * An trait that allows users of a class to retrieve its analytic type
 */
trait NumericType[T] {
	val numericType: ExtendedNumeric[T]
}

/**
 * The simplest of numeric analytics, this takes in numbers and spits out their
 * sum.
 *
 * @tparam T The numeric type of data of which to sum.
 */
class NumericSumAnalytic[T] (implicit numeric: ExtendedNumeric[T]) extends Analytic[T] with NumericType[T] {
	val numericType = numeric
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
class NumericSumBinningAnalytic[T, JT] (implicit numeric: ExtendedNumeric[T],
                                        converter: TypeConversion[T, JT])
		extends NumericSumAnalytic[T]
		with BinningAnalytic[T, JT]
{
	def finish (value: T): JT = converter.forwards(value)
}
/**
 * {@see NumericSumAnalytic}
 *
 * @tparam T The numeric type of data of which to sum.
 */
class NumericSumTileAnalytic[T] (analyticName: Option[String] = None)(implicit numeric: ExtendedNumeric[T])
		extends NumericSumAnalytic[T]
		with TileAnalytic[T]
{
	def name = analyticName.getOrElse("sum")
}



/**
 * The simplest of numeric analytics, this takes in numbers and spits out their
 * maximum.
 *
 * @tparam T The numeric type of data of which to take the maximum.
 */
class NumericMaxAnalytic[T] (implicit numeric: ExtendedNumeric[T]) extends Analytic[T] with NumericType[T] {
	val numericType = numeric
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
class NumericMaxBinningAnalytic[T, JT] (implicit numeric: ExtendedNumeric[T],
                                        converter: TypeConversion[T, JT])
		extends NumericMaxAnalytic[T]
		with BinningAnalytic[T, JT]
{
	def finish (value: T): JT = converter.forwards(value)
}
/**
 * {@see NumericMaxAnalytic}
 *
 * @tparam T The numeric type of data of which to take the maximum.
 */
class NumericMaxTileAnalytic[T] (analyticName: Option[String] = None)(implicit numeric: ExtendedNumeric[T])
		extends NumericMaxAnalytic[T]
		with TileAnalytic[T]
{
	def name = analyticName.getOrElse("maximum")
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
class NumericMaxWithPayloadAnalytic[T, PT <: Serializable] (implicit numeric: ExtendedNumeric[T])
		extends Analytic[(T, PT)] with NumericType[T]
{
	val numericType = numeric
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
                                           PT <: Serializable](implicit numeric: ExtendedNumeric[T],
                                                               converter: TypeConversion[T, JT])
		extends NumericMaxWithPayloadAnalytic[T, PT]
		with BinningAnalytic[(T, PT), Pair[JT, PT]]
{
	def finish (value: (T, PT)): Pair[JT, PT] = new Pair[JT, PT](converter.forwards(value._1), value._2)
}
/**
 * {@see NumericMaxWithPayloadAnalytic}
 *
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMaxWithPayloadTileAnalytic[T, PT <: Serializable] (
	analyticName: Option[String] = None)(
	implicit numeric: ExtendedNumeric[T])
		extends NumericMaxWithPayloadAnalytic[T, PT]
		with TileAnalytic[(T, PT)]
{
	def name = analyticName.getOrElse("maximum")
}



/**
 * The simplest of numeric analytics, this takes in numbers and spits out their
 * minimum.
 *
 * @tparam T The numeric type of data of which to take the minimum.
 */
class NumericMinAnalytic[T] (implicit numeric: ExtendedNumeric[T]) extends Analytic[T] with NumericType[T] {
	val numericType = numeric
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
class NumericMinBinningAnalytic[T, JT] (implicit numeric: ExtendedNumeric[T],
                                        converter: TypeConversion[T, JT])
		extends NumericMinAnalytic[T]
		with BinningAnalytic[T, JT]
{
	def finish (value: T): JT = converter.forwards(value)
}
/**
 * {@see NumericMinAnalytic}
 *
 * @tparam T The numeric type of data of which to take the minimum.
 */
class NumericMinTileAnalytic[T] (analyticName: Option[String] = None)(implicit numeric: ExtendedNumeric[T])
		extends NumericMinAnalytic[T]
		with TileAnalytic[T]
{
	def name = analyticName.getOrElse("minimum")
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
class NumericMinWithPayloadAnalytic[T, PT <: Serializable] (implicit numeric: ExtendedNumeric[T])
		extends Analytic[(T, PT)] with NumericType[T]
{
	val numericType = numeric
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
	implicit numeric: ExtendedNumeric[T],	converter: TypeConversion[T, JT])
		extends NumericMinWithPayloadAnalytic[T, PT]
		with BinningAnalytic[(T, PT), Pair[JT, PT]]
{
	def finish (value: (T, PT)): Pair[JT, PT] = new Pair[JT, PT](converter.forwards(value._1), value._2)
}
/**
 * {@see NumericMinWithPayloadAnalytic}
 *
 * @tparam T The numeric type of data of which to tak ethe maximum.
 * @tparam PT The type of the payload attached to the numeric data.
 */
class NumericMinWithPayloadTileAnalytic[T, PT <: Serializable] (
	analyticName: String = "minimum")(implicit numeric: ExtendedNumeric[T])
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
                              minCount: Int = 1)(implicit numeric: ExtendedNumeric[T])
		extends Analytic[(T, Int)] with NumericType[Double]
{
	val numericType = ExtendedNumeric.ExtendedDouble
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
                                     minCount: Int = 1)(implicit numeric: ExtendedNumeric[T])
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
                                  analyticName: Option[String] = None)(implicit numeric: ExtendedNumeric[T])
		extends NumericMeanAnalytic[T](emptyValue, minCount)
		with TileAnalytic[(T, Int)]
{
	private def statName (stat: String): String = analyticName.map(stat+" "+_).getOrElse(stat)
	def name = statName("mean")
	override def storableValue (value: (T, Int), location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val result = new JSONObject()
		result.put(statName("count"), value._2)
		result.put(statName("mean"), calculate(value))
		Some(result)
	}
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
                               minCount: Int = 1)(implicit numeric: ExtendedNumeric[T])
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
                                      minCount: Int = 1)(implicit numeric: ExtendedNumeric[T])
		extends NumericStatsAnalytic[T]
		with BinningAnalytic[(T, T, Int), Pair[JavaDouble, JavaDouble]]
{
	def finish (value: (T, T, Int)): Pair[JavaDouble, JavaDouble] = {
		val (mean, stddev) = calculate(value)
		new Pair[JavaDouble, JavaDouble](Double.box(mean), Double.box(stddev))
	}
}
class NumericStatsTileAnalytic[T] (emptyValue: (Double, Double) = (JavaDouble.NaN, JavaDouble.NaN),
                                   minCount: Int = 1,
                                   analyticName: Option[String] = None)(implicit numeric: ExtendedNumeric[T])
		extends NumericStatsAnalytic[T]
		with TileAnalytic[(T, T, Int)]
{
	private def statName (stat: String): String = analyticName.map(stat+" "+_).getOrElse(stat)
	def name = statName("stats")
	override def storableValue (value: (T, T, Int), location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val result = new JSONObject()
		val (mean, stddev) = calculate(value)
		result.put(statName("count"), value._2)
		result.put(statName("mean"), mean)
		result.put(statName("stddev"), stddev)
		Some(result)
	}
}
