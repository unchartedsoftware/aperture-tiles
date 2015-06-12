/*
 * Copyright (c) 2015 Uncharted Software Inc.
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
package com.oculusinfo.tilegen.tiling



import scala.collection.mutable.{Map => MutableMap}

import org.scalatest.FunSuite



/**
 * Test some of the functions used by the Universal Binner
 */
class BinningSupportTests extends FunSuite {
	test("Test map aggregation") {
		val aggFcn: (Int, Int) => Int = (n, m) => n + m

		{
			val a = MutableMap("a" -> 1, "b" -> 2, "c" -> 1)
			val b = MutableMap("c" -> 2, "d" -> 4)

			val abm = UniversalBinner.aggregateMaps(aggFcn, a, b)
			val ab = abm.toList.sortBy(_._1)
			// Make sure the values are correct
			assert(List(("a", 1), ("b", 2), ("c", 3), ("d", 4)) === ab)
			// Make sure the smaller map (b) was merged into the larger map (a)
			assert(abm === a)
		}

		{
			val a = MutableMap("a" -> 1, "b" -> 2, "c" -> 1)
			val b = MutableMap("c" -> 2, "d" -> 4)

			val bam = UniversalBinner.aggregateMaps(aggFcn, b, a)
			val ba = bam.toList.sortBy(_._1)
			// Make sure the values are correct
			assert(List(("a", 1), ("b", 2), ("c", 3), ("d", 4)) === ba)
			// Make sure the smaller map (b) was merged into the larger map (a)
			assert(bam === a)
		}
	}

	test("Test optional aggregation") {
		val aggFcn: (Int, Int) => Int = (n, m) => n + m

		// Make sure valid values are aggregated
		assert(7 === UniversalBinner.optAggregate(Some(aggFcn), Some(3), Some(4)).get)
		assert(4 === UniversalBinner.optAggregate(Some(aggFcn), None, Some(4)).get)
		assert(3 === UniversalBinner.optAggregate(Some(aggFcn), Some(3), None).get)
		// Make sure invalid values are not aggregated
		assert(None === UniversalBinner.optAggregate(None, Some(3), Some(4)))
		assert(None == UniversalBinner.optAggregate(Some(aggFcn), None, None))
		assert(None == UniversalBinner.optAggregate(None, None, None))
	}
}
