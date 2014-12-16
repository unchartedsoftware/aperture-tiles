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
 * FITNESS FOR A`IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tilegen.datasets


import org.apache.spark.sql.catalyst.types.ArrayType
import org.scalatest.FunSuite

import org.apache.spark.sql.{Row, StructField, StructType}
import org.apache.spark.sql.{BooleanType, StringType}
import org.apache.spark.sql.{ByteType, ShortType, IntegerType, LongType}
import org.apache.spark.sql.{FloatType, DoubleType}
import org.apache.spark.sql.catalyst.expressions.GenericRow

import scala.collection.mutable.ArrayBuffer


/**
 * Created by nkronenfeld on 12/16/2014.
 */
class SchemaTypeUtilitiesTestSuite extends FunSuite {
	import SchemaTypeUtilities._

	test("Test escaped split") {
		val split1 = unescapedSplit("abc.de\\.f.\\.\\.\\..gh\\\\\\..i", '.', '\\').toList
		assert(List("abc", "de.f", "...", "gh\\.", "i") === split1)
		val split2 = unescapedSplit(".abc.def.ghi.", '.').toList
		assert(List("", "abc", "def", "ghi", "") === split2)
		val split3 = unescapedSplit("a`|b``a|b`|c`|b|c``d``c|d.e\\d", '|', '`').toList
		assert(List("a|b`a", "b|c|b", "c`d`c", "d.e\\d") === split3)
	}

	test("Test full field parsing") {
		def toEqualTestable(input: Array[(String, Array[Int])]) =
			input.map(p => (p._1, p._2.toList)).toList

		assert(List(("a", List[Int]()), ("b", List[Int](1)), ("c", List[Int](2, 3))) ===
			       toEqualTestable(parseColumnSpec("a.b[1].c[ 2][3 ]")))
	}

	test("Calculate field extractor for flat rows") {
		val r = row(true, 1, 1L, 1.0f, 1.0, "1")
		val schema = structSchema(schemaField("bool", BooleanType), schemaField("int", IntegerType),
		                          schemaField("long", LongType), schemaField("float", FloatType),
		                          schemaField("double", DoubleType), schemaField("string", StringType))
		assert(true === calculateExtractor("bool", schema)(r))
		assert(1    === calculateExtractor("int", schema)(r))
		assert(1L   === calculateExtractor("long", schema)(r))
		assert(1.0f === calculateExtractor("float", schema)(r))
		assert(1.0  === calculateExtractor("double", schema)(r))
		assert("1"  === calculateExtractor("string", schema)(r))
	}


	test("Calculate field extractor for struct rows") {
		val r = row(1, row(2, row(3, 4)))
		val schema = structSchema(schemaField("a1", IntegerType),
		                          schemaField("a2", structSchema(schemaField("b1", IntegerType),
		                                                         schemaField("b2", structSchema(schemaField("c1", IntegerType),
		                                                                                        schemaField("c2", IntegerType))))))

		assert(1 === calculateExtractor("a1", schema)(r))
		assert(2 === calculateExtractor("a2.b1", schema)(r))
		assert(3 === calculateExtractor("a2.b2.c1", schema)(r))
		assert(4 === calculateExtractor("a2.b2.c2", schema)(r))
	}


	test("Calculate field extractor for array rows") {
		val r = row(1, array(2, 3), array(array(4, 5), array(6, 7)))
		val schema = structSchema(schemaField("a1", IntegerType),
		                          schemaField("a2", arraySchema(IntegerType)),
		                          schemaField("a3", arraySchema(arraySchema(IntegerType))))

		assert(1 === calculateExtractor("a1",       schema)(r))
		assert(2 === calculateExtractor("a2[0]",    schema)(r))
		assert(3 === calculateExtractor("a2[1]",    schema)(r))
		assert(4 === calculateExtractor("a3[0][0]", schema)(r))
		assert(5 === calculateExtractor("a3[0][1]", schema)(r))
		assert(6 === calculateExtractor("a3[1][0]", schema)(r))
		assert(7 === calculateExtractor("a3[1][1]", schema)(r))
	}

	test("Calculate field extractor for complex nested array/struct rows") {
		val r = row(1,
		            array(array(row(array( 2,  3),  4), row(array( 5,  6),  7)),
		                  array(row(array( 8,  9), 10), row(array(11, 12), 13))),
		            "14")
		val schema = structSchema(schemaField("a1", IntegerType),
		                          schemaField("a2", arraySchema(arraySchema(structSchema(schemaField("b1", arraySchema(IntegerType)),
		                                                                                 schemaField("b2", IntegerType))))),
		                          schemaField("a3", StringType))

		assert(  1  === calculateExtractor("a1",             schema)(r))
		assert(  2  === calculateExtractor("a2[0][0].b1[0]", schema)(r))
		assert(  3  === calculateExtractor("a2[0][0].b1[1]", schema)(r))
		assert(  4  === calculateExtractor("a2[0][0].b2",    schema)(r))
		assert(  5  === calculateExtractor("a2[0][1].b1[0]", schema)(r))
		assert(  6  === calculateExtractor("a2[0][1].b1[1]", schema)(r))
		assert(  7  === calculateExtractor("a2[0][1].b2",    schema)(r))
		assert(  8  === calculateExtractor("a2[1][0].b1[0]", schema)(r))
		assert(  9  === calculateExtractor("a2[1][0].b1[1]", schema)(r))
		assert( 10  === calculateExtractor("a2[1][0].b2",    schema)(r))
		assert( 11  === calculateExtractor("a2[1][1].b1[0]", schema)(r))
		assert( 12  === calculateExtractor("a2[1][1].b1[1]", schema)(r))
		assert( 13  === calculateExtractor("a2[1][1].b2",    schema)(r))
		assert("14" === calculateExtractor("a3",             schema)(r))
	}
}
