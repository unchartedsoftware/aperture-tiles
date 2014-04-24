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

import org.scalatest.FunSuite
import org.apache.spark.SharedSparkContext
import java.io.File
import java.io.PrintWriter
import java.util.Properties

class CSVDatasetSearchTestSuite extends FunSuite with SharedSparkContext {
	var dataset: CSVDataset = null;

	
	override def withFixture (test: NoArgTest) = {
		// Create a 5-column dataset, with the ability to search on any column
		// Create our raw data
		val dataFile = File.createTempFile("live-search-test", ".csv")
		val writer = new PrintWriter(dataFile);
		List(" 0,  8,  6, 10,  7",
		     " 1, 12, 11,  6, 10",
		     " 2,  3,  8,  0,  9",
		     " 3,  7,  4,  5,  5",
		     " 4, 11,  3,  3,  8",
		     " 5,  5,  2, 12,  3",
		     " 6,  9, 10,  4, 12",
		     " 7,  1,  9,  2,  6",
		     " 8, 10, 12,  9,  0",
		     " 9,  6,  1,  1,  4",
		     "10,  4,  7,  7, 11",
		     "11,  0,  0,  8,  2",
		     "12,  2,  5, 11,  1").map(line => writer.write(line+"\n"))
		writer.close();

		try {
			// Put together the data description
			val readProps = new Properties()
			readProps.setProperty("oculus.binning.source.location.0", dataFile.getAbsolutePath())
			readProps.setProperty("oculus.binning.projection.minx", "0.0")
			readProps.setProperty("oculus.binning.projection.maxx", "12.00001")
			readProps.setProperty("oculus.binning.projection.miny", "0.0")
			readProps.setProperty("oculus.binning.projection.maxy", "12.00001")
			readProps.setProperty("oculus.binning.parsing.separator", ",")
			readProps.setProperty("oculus.binning.parsing.a.index", "0")
			readProps.setProperty("oculus.binning.parsing.b.index", "1")
			readProps.setProperty("oculus.binning.parsing.c.index", "2")
			readProps.setProperty("oculus.binning.parsing.d.index", "3")
			readProps.setProperty("oculus.binning.parsing.e.index", "4")

			// Put our dataset together
			dataset = new CSVDataset(readProps, 1, 1);
			dataset.initialize(sc, true, false)

			test()
		} catch {
			case e: Throwable => {
				dataFile.delete()
				throw e
			}
		}
	}

	test("Test simple filtering") {
		val filter = dataset.getFieldFilterFunction("b", 3, 7)
		val results = dataset.getFilteredRawData(filter).collect().toSet

		assert(5 === results.size)
		assert(results.contains(" 2,  3,  8,  0,  9"))
		assert(results.contains("10,  4,  7,  7, 11"))
		assert(results.contains(" 5,  5,  2, 12,  3"))
		assert(results.contains(" 9,  6,  1,  1,  4"))
		assert(results.contains(" 3,  7,  4,  5,  5"))
	}

	test("Test and filters") {
		val bFilter = dataset.getFieldFilterFunction("b", 3, 7)
		val cFilter = dataset.getFieldFilterFunction("c", 2, 7)
		val dFilter = dataset.getFieldFilterFunction("d", 1, 8)
		val filter = LogicalFilterFunctions.and(bFilter, cFilter, dFilter)
		val results = dataset.getFilteredRawData(filter).collect().toSet

		assert(2 === results.size)
		assert(results.contains("10,  4,  7,  7, 11"))
		assert(results.contains(" 3,  7,  4,  5,  5"))
	}

	test("Test or filters") {
		val bFilter = dataset.getFieldFilterFunction("b", 8, 9)
		val cFilter = dataset.getFieldFilterFunction("c", 6, 7)
		val filter = LogicalFilterFunctions.or(bFilter, cFilter)
		val results = dataset.getFilteredRawData(filter).collect().toSet

		assert(3 === results.size)
		assert(results.contains(" 0,  8,  6, 10,  7"))
		assert(results.contains(" 6,  9, 10,  4, 12"))
		assert(results.contains("10,  4,  7,  7, 11"))
	}
}
