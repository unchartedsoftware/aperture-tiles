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

package com.oculusinfo.tilegen.util



import org.scalatest.FunSuite

import com.oculusinfo.tilegen.util.ExtendedNumeric._

import ExtendedNumeric._



class ExtendedNumericsTestSuite extends FunSuite {
	test("Integer Numerics") {
		orderingTest[Int]
		opsTest[Int]
		stringConversion[Int]
	}

	test("Long Numerics") {
		orderingTest[Long]
		opsTest[Long]
		stringConversion[Long]
	}

	test("Float Numerics") {
		orderingTest[Float]
		opsTest[Float]
		stringConversion[Float]
	}

	test("Double Numerics") {
		orderingTest[Double]
		opsTest[Double]
		stringConversion[Double]
	}



	def orderingTest[T] (implicit extnum: ExtendedNumeric[T]): Unit = {
		val two = extnum.plus(extnum.one, extnum.one)
		val three = extnum.plus(two, extnum.one)

		assert(true === extnum.gt(three, two))
		assert(false === extnum.gt(two, three))
		assert(false === extnum.gt(three, three))
		assert(true === extnum.gteq(three, three))
	}
	
	def opsTest[T] (implicit extnum: ExtendedNumeric[T]): Unit = {
		import extnum.mkExtendedNumericOps
		
		val two = extnum.one + extnum.one
		val three = two + extnum.one
		
		assert(true === (three > two))
		assert(false === (two > three))
		assert(false === (three > three))
		assert(true === (three >= three))
	}

	def stringConversion[T] (implicit extnum: ExtendedNumeric[T]): Unit = {
		val twoA = extnum.plus(extnum.one, extnum.one)
		val twoB = extnum.fromString("2")

		assert(twoA === twoB)
		assert(twoA.getClass === twoB.getClass)
	}
}
