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



import java.util.Properties

import org.scalatest.FunSuite




class CSVDatasetPropertiesTestSuite extends FunSuite {
	test("Test properties wrapper") {
		val props = new Properties()
		props.setProperty("oculus.binning.source.location", "hdfs://localhost/data-location")
		props.setProperty("oculus.binning.source.partitions", "13")
		props.setProperty("oculus.binning.name", "sample name")
		props.setProperty("oculus.binning.parsing.a.index", "0")
		props.setProperty("oculus.binning.parsing.a.fieldType", "Int")
		props.setProperty("oculus.binning.parsing.e.index", "3")
		props.setProperty("oculus.binning.parsing.e.fieldType", "Double")
		props.setProperty("oculus.binning.parsing.d.index", "7")
		props.setProperty("oculus.binning.parsing.d.fieldType", "Date")
		props.setProperty("oculus.binning.parsing.d.dateFormat", "yyyy-MM-dd HH:mm:ss")
		props.setProperty("oculus.binning.parsing.c.index", "5")
		props.setProperty("oculus.binning.parsing.c.fieldType", "Long")
		props.setProperty("oculus.binning.parsing.separator", "\t")

		val CSVProps = new CSVRecordPropertiesWrapper(props)
		assert(List("a", "e", "c", "d") === CSVProps.fields.toList)
		assert(0 === CSVProps.fieldIndices("a"))
		assert(1 === CSVProps.fieldIndices("e"))
		assert(2 === CSVProps.fieldIndices("c"))
		assert(3 === CSVProps.fieldIndices("d"))
	}

	test("Test mean type extraction") {
		val props = new Properties()
		props.setProperty("oculus.binning.source.location", "hdfs://localhost/data-location")
		props.setProperty("oculus.binning.source.partitions", "13")
		props.setProperty("oculus.binning.name", "sample name")
		props.setProperty("oculus.binning.parsing.a.index", "0")
		props.setProperty("oculus.binning.parsing.a.fieldType", "double")
		props.setProperty("oculus.binning.parsing.a.fieldAggregation", "meAn")
		props.setProperty("oculus.binning.parsing.separator", "\t")
		props.setProperty("oculus.binning.parsing.b.index", "1")
		props.setProperty("oculus.binning.parsing.b.fieldType", "float")
		props.setProperty("oculus.binning.parsing.b.fieldAggregation", "aVerage")
		props.setProperty("oculus.binning.parsing.c.index", "2")
		props.setProperty("oculus.binning.parsing.c.fieldType", "Median")

		// Make sure that both 'mean' and 'average' field types use our 
		// MeanValueExtractor
		props.setProperty("oculus.binning.valueField", "a")
		val CSVPropsA = new CSVRecordPropertiesWrapper(props)
		assert(CSVValueExtractor.fromProperties(CSVPropsA,
		                                        CSVValueExtractor.standardFactories)
			       .isInstanceOf[MeanValueExtractor[_]])

		props.setProperty("oculus.binning.valueField", "b")
		val CSVPropsB = new CSVRecordPropertiesWrapper(props)
		assert(CSVValueExtractor.fromProperties(CSVPropsB,
		                                        CSVValueExtractor.standardFactories)
			       .isInstanceOf[MeanValueExtractor[_]])

		// Make sure something else (say, for the sake of argument, 'median')
		// does not.
		props.setProperty("oculus.binning.valueField", "c")
		val CSVPropsC = new CSVRecordPropertiesWrapper(props)
		assert(!CSVValueExtractor.fromProperties(CSVPropsC,
		                                          CSVValueExtractor.standardFactories)
			         .isInstanceOf[MeanValueExtractor[_]])
	}
}

