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



import java.lang.{Double => JavaDouble}
import java.io.File
import java.io.PrintWriter
import java.util.Properties

import scala.util.{Try, Success, Failure}

import org.json.JSONObject

import org.scalatest.FunSuite

import org.apache.spark.SharedSparkContext

import com.oculusinfo.binning.TileData

import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription


class CSVDatasetSearchTestSuite extends FunSuite with SharedSparkContext {
	var dataset: CSVDataset[(Double, Double), Double, Int, Int, JavaDouble] = null;


	def unwrapTry[T] (attempt: Try[T]): T = {
		attempt match {
			case Success(t) => t
			case Failure(e) => throw e
		}
	}

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
			val csvProps = new CSVRecordPropertiesWrapper(readProps);
			val csvIndexer = new CartesianIndexExtractor("x", "y")
			val csvValuer = new CountValueExtractor
			val dataAnalytics:
					Option[AnalysisDescription[((Double, Double), Double), Int]] = None
			val tileAnalytics:
					Option[AnalysisDescription[TileData[JavaDouble], Int]] = None
			val levels = Seq(Seq(0))

			// Put our dataset together
			dataset = new CSVDataset(csvIndexer, csvValuer, dataAnalytics, tileAnalytics,
			                         1, 1, levels, csvProps)
			dataset.initialize(sc, false, true, false)

			test()
		} catch {
			case e: Throwable => {
				dataFile.delete()
				throw e
			}
		}
	}

	test("Test simple filtering") {
		val filter = dataset.getFieldDoubleValueFilterFunction("b", 3, 7)
		val results = dataset.getRawFilteredData(filter).collect().toSet

		assert(5 === results.size)
		assert(results.contains(" 2,  3,  8,  0,  9"))
		assert(results.contains("10,  4,  7,  7, 11"))
		assert(results.contains(" 5,  5,  2, 12,  3"))
		assert(results.contains(" 9,  6,  1,  1,  4"))
		assert(results.contains(" 3,  7,  4,  5,  5"))
	}

	test("Test and filters") {
		val bFilter = dataset.getFieldDoubleValueFilterFunction("b", 3, 7)
		val cFilter = dataset.getFieldDoubleValueFilterFunction("c", 2, 7)
		val dFilter = dataset.getFieldDoubleValueFilterFunction("d", 1, 8)
		val filter = FilterFunctions.and(bFilter, cFilter, dFilter)
		val results = dataset.getRawFilteredData(filter).collect().toSet

		assert(2 === results.size)
		assert(results.contains("10,  4,  7,  7, 11"))
		assert(results.contains(" 3,  7,  4,  5,  5"))
	}

	test("Test or filters") {
		val bFilter = dataset.getFieldDoubleValueFilterFunction("b", 8, 9)
		val cFilter = dataset.getFieldDoubleValueFilterFunction("c", 6, 7)
		val filter = FilterFunctions.or(bFilter, cFilter)
		val results = dataset.getRawFilteredData(filter).collect().toSet

		assert(3 === results.size)
		assert(results.contains(" 0,  8,  6, 10,  7"))
		assert(results.contains(" 6,  9, 10,  4, 12"))
		assert(results.contains("10,  4,  7,  7, 11"))
	}

	test("Test simple query parsing") {
		val query = "{\"b\":{\"min\":3, \"max\":7}}"
		val filter = unwrapTry(FilterFunctions.parseQuery(new JSONObject(query), dataset))
		val results = dataset.getRawFilteredData(filter).collect().toSet

		assert(5 === results.size)
		assert(results.contains(" 2,  3,  8,  0,  9"))
		assert(results.contains("10,  4,  7,  7, 11"))
		assert(results.contains(" 5,  5,  2, 12,  3"))
		assert(results.contains(" 9,  6,  1,  1,  4"))
		assert(results.contains(" 3,  7,  4,  5,  5"))
	}

	test("Test query or parsing") {
		val query = "{\"or\":[{\"b\":{\"min\":8,\"max\":9}},{\"c\":{\"min\":6,\"max\":7}}]}"
		val filter = unwrapTry(FilterFunctions.parseQuery(new JSONObject(query), dataset))
		val results = dataset.getRawFilteredData(filter).collect().toSet

		assert(3 === results.size)
		assert(results.contains(" 0,  8,  6, 10,  7"))
		assert(results.contains(" 6,  9, 10,  4, 12"))
		assert(results.contains("10,  4,  7,  7, 11"))
	}

	test("Test query and parsing") {
		val query = "{\"and\":[{\"b\":{\"min\":3,\"max\":7}},{\"c\":{\"min\":2,\"max\":7}}]}"
		val filter = unwrapTry(FilterFunctions.parseQuery(new JSONObject(query), dataset))
		val results = dataset.getRawFilteredData(filter).collect().toSet

		assert(3 === results.size)
		assert(results.contains("10,  4,  7,  7, 11"))
		assert(results.contains(" 5,  5,  2, 12,  3"))
		assert(results.contains(" 3,  7,  4,  5,  5"))
	}

	test("Test complex combination query") {
		val query =
			(""+
				 "{\n"+
				 "  \"or\": [\n"+
				 "    {\"a\": {\"min\":2, \"max\":4}},\n"+
				 "    {\"and\": [\n"+
				 "      {\"b\": {\"min\": 3, \"max\": 7}},\n"+
				 "      {\"c\": {\"min\": 2, \"max\": 7}}\n"+
				 "    ]}\n"+
				 "  ]\n"+
				 "}")

		val filter = unwrapTry(FilterFunctions.parseQuery(new JSONObject(query), dataset))
		val results = dataset.getRawFilteredData(filter).collect().toSet

		assert(5 === results.size)
		assert(results.contains(" 2,  3,  8,  0,  9"))
		assert(results.contains(" 3,  7,  4,  5,  5"))
		assert(results.contains(" 4, 11,  3,  3,  8"))
		assert(results.contains(" 5,  5,  2, 12,  3"))
		assert(results.contains("10,  4,  7,  7, 11"))
	}
}
