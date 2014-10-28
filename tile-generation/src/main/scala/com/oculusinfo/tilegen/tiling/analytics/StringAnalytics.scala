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

import scala.collection.JavaConverters._

import com.oculusinfo.binning.util.Pair



/**
 * Standard string score ordering
 * 
 * @param aggregationLimit The number of elements to keep when aggregating
 * @param order An optional function to specify the order of values.  If not 
 *              given, the order will be random.
 */
class StringScoreAnalytic
	(aggregationLimit: Option[Int] = None,
	 order: Option[((String, Double), (String, Double)) => Boolean] = None)
		extends Analytic[Map[String, Double]]
{
	def aggregate (a: Map[String, Double],
	               b: Map[String, Double]): Map[String, Double] = {
		val combination =
			(a.keySet union b.keySet).map(key =>
				key -> (a.getOrElse(key, 0.0) + b.getOrElse(key, 0.0))
			)
		val sorted: Iterable[(String, Double)] = order.map(fcn =>
			combination.toList.sortWith(fcn)
		).getOrElse(combination)

		aggregationLimit.map(limit =>
			sorted.take(limit)
		).getOrElse(sorted).toMap
	}
	def defaultProcessedValue: Map[String, Double] = Map[String, Double]()
	def defaultUnprocessedValue: Map[String, Double] = Map[String, Double]()
}

/**
 * Extends the standard string score analytic into a binning analytic.
 * 
 * @param aggregationLimit See StringScoreAnalytic
 * @param order See StringScoreAnalytic
 * @param storageLimit The maximum number of entries to store in each tile bin.
 */
class StandardStringScoreBinningAnalytic
	(aggregationLimit: Option[Int] = None,
	 order: Option[((String, Double), (String, Double)) => Boolean] = None,
	 storageLimit: Option[Int] = None)
		extends StringScoreAnalytic(aggregationLimit, order)
		with BinningAnalytic[Map[String, Double],
		                     JavaList[Pair[String, JavaDouble]]]
{
	def finish (value: Map[String, Double]): JavaList[Pair[String, JavaDouble]] = {
		val valueSeq =
			order
				.map(fcn => value.toSeq.sortWith(fcn))
				.getOrElse(value.toSeq)
				.map(p => new Pair(p._1, new JavaDouble(p._2)))
		storageLimit
			.map(valueSeq.take(_))
			.getOrElse(valueSeq)
			.asJava
	}
}

trait StandardStringScoreTileAnalytic extends TileAnalytic[Map[String, Double]] {
	override def valueToString (value: Map[String, Double]): String =
		value.map(p => "\""+p._1+"\":"+p._2).mkString("[", ",", "]")
}

class CategoryValueAnalytic(categoryNames: Seq[String])
		extends Analytic[Seq[Double]]
{
	def aggregate (a: Seq[Double], b: Seq[Double]): Seq[Double] =
		Range(0, a.length max b.length).map(i =>
			{
				def getOrElse(value: Seq[Double], index: Int, default: Double): Double =
					value.applyOrElse(index, (n: Int) => default)
				getOrElse(a, i, 0.0) + getOrElse(b, i, 0.0)
			}
		)
	def defaultProcessedValue: Seq[Double] = Seq[Double]()
	def defaultUnprocessedValue: Seq[Double] = Seq[Double]()
}
class CategoryValueBinningAnalytic(categoryNames: Seq[String])
		extends CategoryValueAnalytic(categoryNames)
		with BinningAnalytic[Seq[Double],
		                     JavaList[Pair[String, JavaDouble]]]
{
	def finish (value: Seq[Double]): JavaList[Pair[String, JavaDouble]] = {
		Range(0, value.length min categoryNames.length).map(i =>
			new Pair[String, JavaDouble](categoryNames(i), value(i))
		).toSeq.asJava
	}
}

class StringAnalytic (analyticName: String) extends TileAnalytic[String] {
	def name = analyticName
	def aggregate (a: String, b: String): String = a+b
	def defaultProcessedValue: String = ""
	def defaultUnprocessedValue: String = ""
}

