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



import org.scalatest.FunSuite
import org.scalatest.Ignore



@Ignore
class SimpleNumericSpeedTestSuite extends FunSuite {
	// Full tests on each type for the summation analytic
	test("Test integer speed") {
		val sn = SimpleNumeric.IntIsSimpleNumeric
		val N = 100000000

		val addStart = System.currentTimeMillis
		var resAdd = 0
		for (i <- 1 to N) {
			resAdd = sn.plus(resAdd, 1)
		}
		val addTime = System.currentTimeMillis-addStart

		var resSub = 0
		val subStart = System.currentTimeMillis
		for (n <- 1 to N) {
			resSub = sn.minus(resSub, 1)
		}
		val subTime = System.currentTimeMillis-subStart

		var resMin = Int.MaxValue
		val minStart = System.currentTimeMillis
		for (n <- 1 to N) {
			resMin = sn.min(resMin, n)
		}
		val minTime = System.currentTimeMillis-minStart

		var resMax = 0
		val maxStart = System.currentTimeMillis
		for (n <- 1 to N) {
			resMax = sn.max(resMax, n)
		}
		val maxTime = System.currentTimeMillis-maxStart

		println("Integer times for "+N+" iterations:")
		println("\taddition:       "+addTime+" ("+resAdd+")")
		println("\tsubtraction:    "+subTime+" ("+resSub+")")
		println("\tminimum:        "+minTime+" ("+resMin+")")
		println("\tmaximum:        "+maxTime+" ("+resMax+")")
	}
}

