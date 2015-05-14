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



import java.util.{List => JavaList}
import scala.collection.JavaConverters._
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList



/**
 * A generalized array analytic.  It takes an element analytic - the analytic
 * one would use if one were interested in a single element of the same types,
 * rather than an array - and uses it to process an array of the same element
 * types.
 *
 * @tparam PT The processing type of the analytic, used for intermediate
 *            processing of the values during binning.  See PROCESSING_TYPE in
 *            {@link BinningAnalytic}
 */
class ArrayAnalytic[PT] (elementAnalytic: Analytic[PT]) extends Analytic[Seq[PT]] {
	def aggregate (a: Seq[PT], b: Seq[PT]): Seq[PT] = {
		val alen = a.length
		val blen = b.length
		val len = alen max blen
		Range(0, len).map(n =>
			{
				if (n < alen && n < blen) elementAnalytic.aggregate(a(n), b(n))
				else if (n < alen) a(n)
				else b(n)
			}
		)
	}
	def defaultProcessedValue: Seq[PT] = Seq[PT]()
	def defaultUnprocessedValue: Seq[PT] = Seq[PT]()
}
/**
 * A generalized array analytic.  It takes an element analytic - the analytic
 * one would use if one were interested in a single element of the same types,
 * rather than an array - and uses it to process an array of the same element
 * types.
 *
 * @tparam PT The processing type of the analytic, used for intermediate
 *            processing of the values during binning.  See PROCESSING_TYPE in
 *            {@link BinningAnalytic}
 * @tparam RT The result type of the analytic, the one actually written to
 *            tiles (if this is used as a binning analytic).  See RESULT_TYPE
 *            in {@link BinningAnalytic}.  Note that for tile analyics, this
 *            conversion still takes place, it is simply taken further.
 */
class ArrayBinningAnalytic[PT, RT] (elementAnalytic: BinningAnalytic[PT, RT])
		extends ArrayAnalytic[PT](elementAnalytic)
		with BinningAnalytic[Seq[PT], JavaList[RT]]
{
	def finish (value: Seq[PT]): JavaList[RT] = {
		val result = new ArrayList[RT](value.size)
		value.foreach(elt => result.add(elementAnalytic.finish(elt)))
		result
	}
}
class ArrayTileAnalytic[PT] (elementAnalytic: TileAnalytic[PT],
                             analyticName: Option[String] = None)
		extends ArrayAnalytic[PT](elementAnalytic)
		with TileAnalytic[Seq[PT]]
{
	def name = analyticName.getOrElse(elementAnalytic.name+" array")
	override def storableValue (value: Seq[PT], location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val outputValues = new JSONArray()
		value.foreach{p =>
			val esv = elementAnalytic.storableValue(p, location)
			if (esv.isEmpty) outputValues.put(null.asInstanceOf[Object])
			else esv.foreach(e =>
				if (e.length() > 1) outputValues.put(e)
				else if (e.length() == 1) outputValues.put(e.get(JSONObject.getNames(e)(0)))
			)
		}
		if (outputValues.length()>0) {
			val result = new JSONObject()
			result.put(name, outputValues)
			Some(result)
		} else None
	}
}
