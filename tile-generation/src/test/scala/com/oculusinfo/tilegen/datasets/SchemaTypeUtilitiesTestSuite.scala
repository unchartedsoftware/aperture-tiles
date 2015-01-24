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


import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Double => JavaDouble}

import org.apache.spark.sql.catalyst.types.ArrayType
import org.scalatest.FunSuite

import org.apache.spark.sql.{Row, StructField, StructType}
import org.apache.spark.sql.{BooleanType, StringType, TimestampType}
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

	test("Field type determination") {
		val schema = structSchema(schemaField("a", IntegerType),
		                          schemaField("b", FloatType),
		                          schemaField("c", arraySchema(DoubleType)),
		                          schemaField("d", arraySchema(structSchema(schemaField("e", StringType),
		                                                                    schemaField("f", TimestampType)))),
		                          schemaField("g", structSchema(schemaField("h", BooleanType),
		                                                        schemaField("i", ByteType))))

		assert(IntegerType === getColumnType("a", schema))
		assert(FloatType === getColumnType("b", schema))
		assert(getColumnType("c", schema).isInstanceOf[ArrayType])
		assert(DoubleType === getColumnType("c[0]", schema))
		assert(DoubleType === getColumnType("c[1]", schema))
		assert(getColumnType("d", schema).isInstanceOf[ArrayType])
		assert(getColumnType("d[0]", schema).isInstanceOf[StructType])
		assert(StringType === getColumnType("d[1].e", schema))
		assert(TimestampType === getColumnType("d[1].f", schema))
		assert(getColumnType("g", schema).isInstanceOf[StructType])
		assert(BooleanType === getColumnType("g.h", schema))
		assert(ByteType === getColumnType("g.i", schema))
	}

	test("Primitive type conversion - sample conversions") {
		val schema = structSchema(schemaField("a", IntegerType),
		                          schemaField("b", DoubleType),
		                          schemaField("c", StringType))

		val data = List(new GenericRow(Array(1, 1.1, "1.11")),
		                new GenericRow(Array(2, 2.2, "2.22")),
		                new GenericRow(Array(3, 3.3, "3.33")),
		                new GenericRow(Array(4, 4.4, "4.44")),
		                new GenericRow(Array(5, 5.5, "5.55")),
		                new GenericRow(Array(6, 6.6, "6.66")),
		                new GenericRow(Array(7, 7.7, "7.77")),
		                new GenericRow(Array(8, 8.8, "8.88")),
		                new GenericRow(Array(9, 9.9, "9.99")))

		assert(IntegerType == getColumnType("a", schema))
		val aExtractor = calculateExtractor("a", schema)
		assert(DoubleType == getColumnType("b", schema))
		val bExtractor = calculateExtractor("b", schema)
		assert(StringType == getColumnType("c", schema))
		val cExtractor = calculateExtractor("c", schema)

		def mappedInstanceCheck (toCheck: List[Any], checkTypes: Class[_]*): Unit = {
			toCheck.foreach(item => assert(checkTypes.map(ct => ct.isInstance(item)).reduce(_ || _)))
		}

		val iToS = calculateConverter(IntegerType, StringType)
		val aAsS = data.map(r => iToS(aExtractor(r))).toList
		assert(List("1", "2", "3", "4", "5", "6", "7", "8", "9") === aAsS)
		mappedInstanceCheck(aAsS, classOf[String])

		val iToL = calculateConverter(IntegerType, LongType)
		val aAsL = data.map(r => iToL(aExtractor(r))).toList
		assert(List(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L) === aAsL)
		// Because aAsL is a list of Any, it ends up boxing the contents, so we really have to check for JavaLong
		// instead of Long here; we pass in both, just in case this changes, because either should, theoretically,
		// be acceptable.
		mappedInstanceCheck(aAsL, classOf[Long], classOf[JavaLong])

		val iToD = calculateConverter(IntegerType, DoubleType)
		val aAsD = data.map(r => iToD(aExtractor(r))).toList
		assert(List(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0) === aAsD)
		// Similar to Longs, above.
		mappedInstanceCheck(aAsD, classOf[Double], classOf[JavaDouble])

		val dToI = calculateConverter(DoubleType, IntegerType)
		val bAsI = data.map(r => {
			val d = bExtractor(r)
			val i = dToI(d)
			dToI(bExtractor(r))
		}).toList
		assert(List(1, 2, 3, 4, 5, 6, 7, 8, 9) === bAsI)
		// Similar to Longs, above.
		mappedInstanceCheck(bAsI, classOf[Int], classOf[JavaInt])

		val dToS = calculateConverter(DoubleType, StringType)
		val bAsS = data.map(r => dToS(bExtractor(r))).toList
		assert(List("1.1", "2.2", "3.3", "4.4", "5.5", "6.6", "7.7", "8.8", "9.9") === bAsS)
		mappedInstanceCheck(bAsS, classOf[String])
	}

	test("Check conversion boxing") {
		val schema = structSchema(schemaField("a", IntegerType),
		                          schemaField("b", DoubleType),
		                          schemaField("c", StringType))

		val data = List(new GenericRow(Array(1, 1.1, "1.11")),
		                new GenericRow(Array(2, 2.2, "2.22")),
		                new GenericRow(Array(3, 3.3, "3.33")),
		                new GenericRow(Array(4, 4.4, "4.44")),
		                new GenericRow(Array(5, 5.5, "5.55")),
		                new GenericRow(Array(6, 6.6, "6.66")),
		                new GenericRow(Array(7, 7.7, "7.77")),
		                new GenericRow(Array(8, 8.8, "8.88")),
		                new GenericRow(Array(9, 9.9, "9.99")))

		val aExtractor = calculateExtractor("a", schema)
		val iToL = calculateConverter(IntegerType, LongType)

		val results = data.map(row => iToL(aExtractor(row)).asInstanceOf[Long]+4)
		assert(List(5, 6, 7, 8, 9, 10, 11, 12, 13) === results)
	}

	test("Numeric Function Passing") {
		val schema = structSchema(schemaField("a", IntegerType),
		                          schemaField("b", DoubleType),
		                          schemaField("c", StringType))

		val data = List(new GenericRow(Array(1, 1.1, "1.11")),
		                new GenericRow(Array(2, 2.2, "2.22")),
		                new GenericRow(Array(3, 3.3, "3.33")),
		                new GenericRow(Array(4, 4.4, "4.44")),
		                new GenericRow(Array(5, 5.5, "5.55")),
		                new GenericRow(Array(6, 6.6, "6.66")),
		                new GenericRow(Array(7, 7.7, "7.77")),
		                new GenericRow(Array(8, 8.8, "8.88")),
		                new GenericRow(Array(9, 9.9, "9.99")))

		val aExtractor = calculateExtractor("a", schema)
		val functionCreator = new FunctionCreator {
			import Numeric.Implicits._
			override def createFunction[T: Numeric](t: T): T = {
				val four = implicitly[Numeric[T]].fromInt(4)
				t + four
			}
		}

		val results = data.map(row => withNumeric(aExtractor(row), functionCreator))
		assert(List(5, 6, 7, 8, 9, 10, 11, 12, 13) === results)
	}
}

