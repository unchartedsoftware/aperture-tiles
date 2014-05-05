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



import scala.util.{Try, Success, Failure}

import org.json.JSONObject
import org.json.JSONException

import com.oculusinfo.tilegen.tiling.ValueOrException


// There's probably a better way, but the point of having this FilterAware 
// object, and immediately importing its contents, is to have the Filter 
// type alias available everywhere in this file.
object FilterAware {
	type Filter = ValueOrException[List[Double]] => Boolean
	type FilterFunction = Function1[ValueOrException[List[Double]], Boolean]
}
import com.oculusinfo.tilegen.datasets.FilterAware._



// Encapsulate the Or function so it will work nicely with toString
class OrFunction(operands: Filter*)
		extends FilterFunction
		with Serializable {
	def apply (value: ValueOrException[List[Double]]): Boolean =
		operands.map(_(value)).reduce(_ || _)
	override def toString: String = operands.mkString("or(", ", ", ")")
}

// Encapsulates the And function we can use toString nicely on it.
class AndFunction(operands: Filter*)
		extends FilterFunction
		with Serializable {
	def apply (value: ValueOrException[List[Double]]): Boolean =
		operands.map(_(value)).reduce(_ && _)
	override def toString: String = operands.mkString("and(", ", ", ")")
}



object FilterFunctions {

	def and (operands: Filter*): Filter = new AndFunction(operands:_*)

	def or (operands: Filter*): Filter = new OrFunction(operands:_*)

	def parseQuery (query: JSONObject, dataset: CSVDataset): Try[Filter] =
		Try({
			    val names = query.names()
			    if (names.length != 1)
				    throw new IllegalArgumentException("Bad query: Need exactly one key")
			    val name = names.getString(0)

			    name match {
				    case "or" => {
					    val subs = query.getJSONArray(name)
					    or(Range(0, subs.length()).map(i =>
						       subs.getJSONObject(i)
					       ).map(parseQuery(_, dataset) match {
						             case Success(q) => q
						             case Failure(e) => throw e
					             }):_*)
				    }
				    case "and" => {
					    val subs = query.getJSONArray(name)
					    and(Range(0, subs.length()).map(i =>
						        subs.getJSONObject(i)
					        ).map(parseQuery(_, dataset) match {
						              case Success(q) => q
						              case Failure(e) => throw e
					              }):_*)
				    }
				    case _ => {
					    val range = query.getJSONObject(name)
					    val min = range.getDouble("min")
					    val max = range.getDouble("max")
					    dataset.getFieldFilterFunction(name, min, max)
				    }
			    }
		    })
}
