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



import java.io.{Serializable => JavaSerializable}
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}

import org.apache.avro.util.Utf8

import scala.collection.JavaConverters._

import com.oculusinfo.binning.util.Pair



/**
 * Standard string score ordering
 * 
 * @baseAnalytic An analytic used to aggregate scores
 * @param aggregationLimit An optional number of elements to keep when 
 *                         aggregating.  If None, all elements are kept.
 * @param order An optional function to specify the order of values.  If not 
 *              given, the order will be random.
 * @tparam The type of the score value
 */
class StringScoreAnalytic[T]
	(baseAnalytic: Analytic[T],
	 aggregationLimit: Option[Int] = None,
	 order: Option[((String, T), (String, T)) => Boolean] = None)
		extends Analytic[Map[String, T]]
{
	def aggregate (a: Map[String, T], b: Map[String, T]): Map[String, T] = {
		val combination =
			(a.keySet union b.keySet).map(key =>
				// Should always have one or the other, since we took the union
				// of keysets, so the error case where we try to reduce an
				// empty list shouldn't happen
				key -> List(a.get(key), b.get(key)).flatten.reduce(baseAnalytic.aggregate(_, _))
			)
		val sorted: Iterable[(String, T)] = order.map(fcn =>
			combination.toList.sortWith(fcn)
		).getOrElse(combination)

		aggregationLimit.map(limit =>
			sorted.take(limit)
		).getOrElse(sorted).toMap
	}
	def defaultProcessedValue: Map[String, T] = Map[String, T]()
	def defaultUnprocessedValue: Map[String, T] = Map[String, T]()
}

/**
 * Extends the standard string score analytic into a binning analytic.
 * 
 * @param baseAnalytic See StringScoreAnalytic
 * @param aggregationLimit See StringScoreAnalytic
 * @param order See StringScoreAnalytic
 * @param storageLimit An optional maximum number of entries to store in each
 *                     tile bin. If not given, all values are stored.
 * @tparam T See StringScoreAnalytic
 * @tparam JT The type as which the score is to be written to bins.
 */
class StringScoreBinningAnalytic[T, JT <: JavaSerializable]
	(baseAnalytic: BinningAnalytic[T, JT],
	 aggregationLimit: Option[Int] = None,
	 order: Option[((String, T), (String, T)) => Boolean] = None,
	 storageLimit: Option[Int] = None)
		extends StringScoreAnalytic[T](baseAnalytic, aggregationLimit, order)
		with BinningAnalytic[Map[String, T],
		                     JavaList[Pair[String, JT]]]
{
	def finish (value: Map[String, T]): JavaList[Pair[String, JT]] = {
		val valueSeq =
			order
				.map(fcn => value.toSeq.sortWith(fcn))
				.getOrElse(value.toSeq)
				.map(p => new Pair[String, JT](p._1, baseAnalytic.finish(p._2)))
		storageLimit
			.map(valueSeq.take(_))
			.getOrElse(valueSeq)
			.asJava
	}
}

/**
 * Extends the standard string score analytic into a tile analytic.
 * 
 * @param analyticName The name by which the analytic value should be known in 
 *                     metadata
 * @param baseAnalytic See StringScoreAnalytic
 * @param aggregationLimit See StringScoreAnalytic
 * @param order See StringScoreAnalytic
 * @tparam T See StringScoreAnalytic
 */
class StringScoreTileAnalytic[T] (analyticName: Option[String],
                                  baseAnalytic: TileAnalytic[T],
                                  aggregationLimit: Option[Int] = None,
                                  order: Option[((String, T), (String, T)) => Boolean] = None)
		extends StringScoreAnalytic[T](baseAnalytic, aggregationLimit, order)
		with TileAnalytic[Map[String, T]]
{
	def name = analyticName.getOrElse(baseAnalytic.name)
	override def valueToString (value: Map[String, T]): String =
		value.map(p => "\""+p._1+"\":"+baseAnalytic.valueToString(p._2)).mkString("[", ",", "]")
	override def toMap (value: Map[String, T]): Map[String, Any] =
		value.map{case (k1, v1) =>
			baseAnalytic.toMap(v1).map{case (k2, v2) => (k1+"."+k2, v2)}
		}.flatten.toMap
}

/**
 * Similar to a StringScoreAnalytic, but this analytic tracks fixed, rather 
 * than arbitrary, categories.
 * 
 * @param categoryNames The names of the fixed categories this analytic will 
 *                      track
 * @param baseAnalytic An analytic used to process the scores associated with 
 *                     each category.
 * @tparam T The type of the score associated with each category
 */
class CategoryValueAnalytic[T] (categoryNames: Seq[String], baseAnalytic: Analytic[T])
		extends Analytic[Seq[T]]
{
	def aggregate (a: Seq[T], b: Seq[T]): Seq[T] =
		a.zipAll(b, baseAnalytic.defaultUnprocessedValue, baseAnalytic.defaultUnprocessedValue)
			.map{case (aa, bb) => baseAnalytic.aggregate(aa, bb)}
	def defaultProcessedValue: Seq[T] = Seq[T]()
	def defaultUnprocessedValue: Seq[T] = Seq[T]()
}
/**
 * Extends the standard category value analytic into a binning analytic.
 * 
 * @param categoryNames {@see CategoryValueAnalytic}
 * @param baseAnalytic {@see CategoryValueAnalytic}
 * @tparam T {@see CategoryValueAnalytic}
 * @tparam JT The type as which the score is to be written to bins.
 */
class CategoryValueBinningAnalytic[T, JT <: JavaSerializable] (categoryNames: Seq[String], baseAnalytic: BinningAnalytic[T, JT])
		extends CategoryValueAnalytic[T](categoryNames, baseAnalytic)
		with BinningAnalytic[Seq[T], JavaList[Pair[String, JT]]]
{
	def finish (value: Seq[T]): JavaList[Pair[String, JT]] =
		categoryNames.zip(value).map{case (c, v) =>
			new Pair[String, JT](c, baseAnalytic.finish(v))
		}.toSeq.asJava
}

class CategoryValueTileAnalytic[T] (analyticName: Option[String],
                                    categoryNames: Seq[String],
                                    baseAnalytic: TileAnalytic[T])
		extends CategoryValueAnalytic[T](categoryNames, baseAnalytic)
		with TileAnalytic[Seq[T]]
{
	def name = analyticName.getOrElse(baseAnalytic.name)
	override def valueToString (value: Seq[T]): String =
		value.map(baseAnalytic.valueToString(_)).mkString("[", ",", "]")
	override def toMap (value: Seq[T]): Map[String, Any] =
		categoryNames.zip(value).map{case (k1, v1) =>
			baseAnalytic.toMap(v1).map{case (k2, v2) => (k1+"."+k2, v2)}
		}.flatten.toMap
}


class StringAnalytic (analyticName: String) extends TileAnalytic[String] {
	def name = analyticName
	def aggregate (a: String, b: String): String = a+b
	def defaultProcessedValue: String = ""
	def defaultUnprocessedValue: String = ""
}
