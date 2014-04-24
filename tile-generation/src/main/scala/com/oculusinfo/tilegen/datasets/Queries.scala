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

package com.oculusinfo.tilegen.datasets



import org.json.JSONObject

import com.oculusinfo.tilegen.tiling.ValueOrException



object FilterFunctions {
	def and (operands: (ValueOrException[List[Double]] => Boolean)*) = {
		val result: ValueOrException[List[Double]] => Boolean = value =>
		operands.map(_(value)).reduce(_ && _)

		result;
	}

	def or (operands: (ValueOrException[List[Double]] => Boolean)*) = {
		val result: ValueOrException[List[Double]] => Boolean = value =>
		operands.map(_(value)).reduce(_ || _)

		result;
	}

	def parseQuery (query: JSONObject, dataset: CSVDataset): ValueOrException[List[Double]] => Boolean = {
		val names = query.names()
		if (names.length != 1) throw new IllegalArgumentException("Bad query: Need exactly one key")
		val name = names.getString(0)

		name match {
			case "or" => {
				val subs = query.getJSONArray(name)
				or(Range(0, subs.length()).map(i => subs.getJSONObject(i)).map(parseQuery(_, dataset)):_*)
			}
			case "and" => {
				val subs = query.getJSONArray(name)
				and(Range(0, subs.length()).map(i => subs.getJSONObject(i)).map(parseQuery(_, dataset)):_*)
			}
			case _ => {
				val range = query.getJSONObject(name)
				val min = range.getDouble("min")
				val max = range.getDouble("max")
				dataset.getFieldFilterFunction(name, min, max)
			}
		}
	}
}
