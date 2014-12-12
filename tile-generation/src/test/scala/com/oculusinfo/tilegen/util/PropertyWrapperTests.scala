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



import java.util.Properties

import org.scalatest.FunSuite



class PropertyWrapperTestSuite extends FunSuite {
	test("string sequence") {
		val props = new Properties()
		props.setProperty("b.0", "0")

		props.setProperty("c.0", "a")
		props.setProperty("c.1", "b")

		props.setProperty("d.0", "x")
		props.setProperty("d.1", "y")
		props.setProperty("d.2", "z")

		props.setProperty("e.0", "i")
		props.setProperty("e.1", "j")
		props.setProperty("e.2", "k")
		props.setProperty("e.3", "l")

		val wrappedProps = new PropertiesWrapper(props)
		assert(List[String]() === wrappedProps.getStringPropSeq("a", "").toList)
		assert(List[String]("0") === wrappedProps.getStringPropSeq("b", "").toList)
		assert(List[String]("a", "b") === wrappedProps.getStringPropSeq("c", "").toList)
		assert(List[String]("x", "y", "z") === wrappedProps.getStringPropSeq("d", "").toList)
		assert(List[String]("i", "j", "k", "l") === wrappedProps.getStringPropSeq("e", "").toList)
	}

	test("create connector with spark and akka properties") {
		val props = new Properties()
		props.setProperty("spark.some.property", "someValue")
		props.setProperty("akka.some.property", "someOtherValue")

		val wrappedProps = new PropertiesWrapper(props)
		val connector = wrappedProps.getSparkConnector()
		assertResult(Some("someValue")) {
			connector.getConfProperty("spark.some.property")
		}
		assertResult(Some("someOtherValue")) {
			connector.getConfProperty("akka.some.property")
		}
	}

	test("create connector with ignored properties") {
		val props = new Properties()
		props.setProperty("bad.some.property", "someValue")

		val wrappedProps = new PropertiesWrapper(props)
		val connector = wrappedProps.getSparkConnector()
		assertResult(None) {
			connector.getConfProperty("bad.some.property")
		}
	}
}
