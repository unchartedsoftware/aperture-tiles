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
package com.oculusinfo.tilegen.util

import java.text.ParseException

import org.scalatest.FunSuite

class StringUtilitiesTestSuite extends FunSuite {
	import StringUtilities._

	test("Test hanging escape") {
		intercept[ParseException] {
			separateString("""a, b, c|""", ", ", Some("'"), Some("\\|"))
		}
		intercept[ParseException] {
			separateString("""a, b, c\""", ", ", Some("'"), Some("\\\\"))
		}
	}

	test("Test unmatched quote") {
		intercept[ParseException] {
			separateString("""a, b, 'c""", ", ", Some("'"), Some("\\|"))
		}
		intercept[ParseException] {
			separateString("""a, 'b', c'""", ", ", Some("'"), Some("\\|"))
		}
	}

	test("Test simple string") {
		assert(List("a", "b", "c") === separateString("""a, b, c""", ", ", Some("'"), Some("\\|")).toList)
	}

	test("Test no escape") {
		assert(List("a", "b\\", "c") === separateString("""a, b\, c""", ", ", Some("'"), None).toList)
	}

	test("Test no quote") {
		assert(List("a", "'b", "c'") === separateString("""a, 'b, c'""", ", ", None, Some("\\\\")).toList)
	}

	test("Test escaped separator") {
		assert(List("a", "b, c", "d") === separateString("""a, b|, c, d""", ", ", Some("'"), Some("\\|")).toList)
	}

	test("Test quoted separator") {
		assert(List("a", "b, c", "d") === separateString("""a, 'b, c', d""", ", ", Some("'"), Some("\\|")).toList)
	}

	test("Test escaped escape character") {
		assert(List("a", "b|", "c") === separateString("""a, b||, c""", ", ", Some("'"), Some("\\|")).toList)
	}

	test("Test complex expression") {
		assert(List("a, b", "c, d", "'e", "f, g", "h", "|i, j", "k") ===
			       separateString("""'a, b', 'c, d', |'e, f', 'g, h, ||'i, j', k""", ", ", Some("'"), Some("\\|")).toList)
	}

	test("Test backslash escape") {
		// Getting the regular expression correct for backslashes is a pain; this example is provided to show what
		// pattern to use
		assert(List("a", "b, c", "d") === separateString("""a, b\, c, d""", ", ", Some("'"), Some("\\\\")).toList)
		assert(List("a", "b, c", "d") === separateString("a, b\\, c, d", ", ", Some("'"), Some("\\\\")).toList)
	}
}
