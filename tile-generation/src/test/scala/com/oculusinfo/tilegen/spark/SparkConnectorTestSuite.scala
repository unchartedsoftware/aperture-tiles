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
package com.oculusinfo.tilegen.spark



import org.scalatest._


class SparkConnectorTestSuite extends FunSuite {

	test("Create connector with arguments") {
		val connector = new SparkConnector(Map("spark.app.name" -> "someName", "spark.home" -> "opt/spark"))
		assertResult(Some("someName")) {
			connector.getConfProperty("spark.app.name")
		}
		assertResult(Some("opt/spark")) {
			connector.getConfProperty("spark.home")
		}
	}

	test("add conf property to connector") {
		val connector = new SparkConnector(Map("spark.app.name" -> "someName"))
		connector.setConfProperty("spark.home", "opt/spark")
		assertResult(Some("opt/spark")) {
			connector.getConfProperty("spark.home")
		}
	}

	test("get non-existent conf property from connector") {
		val connector = new SparkConnector(Map("spark.app.name" -> "someName", "spark.home" -> "opt/spark"))
		assertResult(None) {
			connector.getConfProperty("spark.missing.parm")
		}
	}

	test("create context with job name override") {
		val connector = new SparkConnector(Map("spark.app.name" -> "someJob", "spark.master" -> "local", "spark.driver.allowMultipleContexts" -> "true"))
		connector.createContext(Some("someOtherJob"))
		assertResult(Some("someOtherJob")) {
			connector.getConfProperty("spark.app.name")
		}
	}

}
