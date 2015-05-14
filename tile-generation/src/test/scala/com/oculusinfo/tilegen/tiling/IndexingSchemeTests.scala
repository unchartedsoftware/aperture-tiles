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
package com.oculusinfo.tilegen.tiling

import org.scalatest.FunSuite



class IndexingSchemeTests extends FunSuite {
	test("Test null conversion") {
		val converter = new Object with NumberConverter
		intercept[IllegalArgumentException] {
			converter.asDouble(null)
			fail
		}
	}

	test("Test cartesian indices on non-doubles") {
		val scheme = new CartesianSchemaIndexScheme
		assert(1.0 === scheme.toCartesian(Seq[Any](1, 2))._1)
		assert(2.0 === scheme.toCartesian(Seq[Any](1, 2L))._2)
		assert(3.5 === scheme.toCartesian(Seq[Any](3.5f, 3.4f))._1)
		assert(6.0 === scheme.toCartesian(Seq[Any](1, 6.toByte))._2)
		assert(7.0 === scheme.toCartesian(Seq[Any](7.toShort, 8.toShort))._1)
		assert(456789.0 === scheme.toCartesian(Seq[Any](1, new java.util.Date(456789L)))._2)
	}
}
