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
import org.json.{JSONArray, JSONObject}

import scala.collection.JavaConverters._

import com.oculusinfo.factory.util.Pair



/**
 * Standard string score ordering
 *
 * @param baseAnalytic An analytic used to aggregate scores
 * @param aggregationLimit An optional number of elements to keep when
 *                         aggregating.  If None, all elements are kept.
 * @param order An optional function to specify the order of values.  If not
 *              given, the order will be random.
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
class StringScoreBinningAnalytic[T, JT]
	(baseAnalytic: BinningAnalytic[T, JT],
	 aggregationLimit: Option[Int] = None,
	 order: Option[((String, T), (String, T)) => Boolean] = None,
	 storageLimit: Option[Int] = None)
		extends StringScoreAnalytic[T](baseAnalytic, aggregationLimit, order)
		with BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]]
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
 * Extends the standard string score analytic into a tile analytic with scores keyed by string.
 *
 * @param analyticName The name by which the analytic value should be known in metadata
 * @param baseAnalytic See StringScoreAnalytic
 * @param stringName The name by which the string in each entry is known in the metadata to which this analytic is written
 * @param scoreName The name by which the score in each entry is known in the metadata to which this analytic is written
 * @param aggregationLimit See StringScoreAnalytic
 * @param order See StringScoreAnalytic
 * @tparam T See StringScoreAnalytic
 */
import TileAnalytic.Locations
class StringScoreTileAnalytic[T] (analyticName: Option[String],
                                  baseAnalytic: TileAnalytic[T],
                                  stringName: String = "string",
                                  scoreName: String = "score",
                                  writeLocations: Set[TileAnalytic.Locations.Value] = TileAnalytic.Locations.values,
                                  aggregationLimit: Option[Int] = None,
                                  order: Option[((String, T), (String, T)) => Boolean] = None,
                                  storageLimit: Option[Int] = None)
		extends StringScoreAnalytic[T](baseAnalytic, aggregationLimit, order)
		with TileAnalytic[Map[String, T]]
{
	def foo = TileAnalytic.Locations.values
	def name = analyticName.getOrElse(baseAnalytic.name)
	override def storableValue (value: Map[String, T], location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		if (writeLocations.contains(location)) {
			val allValues = order.map(sorter=>value.toList.sortWith(sorter)).getOrElse(value.toList)
      val topValues = storageLimit.map(limit => allValues.take(limit)).getOrElse(allValues)
			val subRes = new JSONArray()
      topValues.foreach { case (key, value) =>
				baseAnalytic.storableValue(value, location).foreach{bsv =>
					val entry = new JSONObject()
					if (bsv.length() > 1) entry.put(scoreName, bsv)
					else if (bsv.length == 1) entry.put(scoreName, bsv.get(JSONObject.getNames(bsv)(0)))
					if (entry.length() > 0) {
						entry.put(stringName, key)
						subRes.put(entry)
					}
				}
			}
			if (subRes.length() > 0) {
				val result = new JSONObject()
				result.put(name, subRes)
				Some(result)
			} else None
		} else None
	}
}

/**
 * Extends the standard string score analytic into a tile analytic with the top scoring strings, in order
 *
 * @param analyticName The name by which the analytic value should be known in metadata
 * @param baseAnalytic See StringScoreAnalytic
 * @param aggregationLimit See StringScoreAnalytic
 * @param order See StringScoreAnalytic
 * @tparam T See StringScoreAnalytic
 */
class OrderedStringTileAnalytic[T] (analyticName: Option[String],
                                    baseAnalytic: TileAnalytic[T],
                                    aggregationLimit: Option[Int] = None,
                                    order: Option[((String, T), (String, T)) => Boolean] = None)
		extends StringScoreAnalytic[T](baseAnalytic, aggregationLimit, order)
		with TileAnalytic[Map[String, T]]
{
	def name = analyticName.getOrElse(baseAnalytic.name)
	override def storableValue (value: Map[String, T], location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val values = order.map(sorter=>value.toList.sortWith(sorter)).getOrElse(value.toList)
		val outputValues = new JSONArray()
		values.foreach { case (key, value) => outputValues.put(key)}
		if (outputValues.length()>0) {
			val result = new JSONObject()
			result.put(name, outputValues)
			Some(result)
		}
		else None
	}
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
class CategoryValueBinningAnalytic[T, JT] (categoryNames: Seq[String], baseAnalytic: BinningAnalytic[T, JT])
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
	override def storableValue (value: Seq[T], location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val outputValues = new JSONArray()
		value.map(t => baseAnalytic.storableValue(t, location).map(bt => outputValues.put(bt)))
		if (outputValues.length()>0) {
			val result = new JSONObject()
			result.put(name, outputValues)
			Some(result)
		} else None
	}
}


class StringAnalytic (analyticName: String) extends TileAnalytic[String] {
	def name = analyticName
	def aggregate (a: String, b: String): String = a+b
	def defaultProcessedValue: String = ""
	def defaultUnprocessedValue: String = ""
}
