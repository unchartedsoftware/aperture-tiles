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

import org.apache.spark.SharedSparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.ArrayType
import org.scalatest.FunSuite

import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StructField, StructType}
import org.apache.spark.sql.types.{BooleanType, StringType, TimestampType}
import org.apache.spark.sql.types.{ByteType, ShortType, IntegerType, LongType}
import org.apache.spark.sql.types.{FloatType, DoubleType}

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

		val data = List(Row(1, 1.1, "1.11"),
		                Row(2, 2.2, "2.22"),
		                Row(3, 3.3, "3.33"),
		                Row(4, 4.4, "4.44"),
		                Row(5, 5.5, "5.55"),
		                Row(6, 6.6, "6.66"),
		                Row(7, 7.7, "7.77"),
		                Row(8, 8.8, "8.88"),
		                Row(9, 9.9, "9.99"))

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

		val iToLp = calculatePrimitiveConverter(IntegerType, classOf[Long])
		val aAsLp: List[Long] = data.map(r => iToLp(aExtractor(r))).toList
		assert(List(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L) === aAsLp)

		val iToD = calculateConverter(IntegerType, DoubleType)
		val aAsD = data.map(r => iToD(aExtractor(r))).toList
		assert(List(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0) === aAsD)
		// Similar to Longs, above.
		mappedInstanceCheck(aAsD, classOf[Double], classOf[JavaDouble])

		val iToDp = calculatePrimitiveConverter(IntegerType, classOf[Double])
		val aAsDp: List[Double] = data.map(r => iToDp(aExtractor(r))).toList
		assert(List(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0) === aAsDp)

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

		val dToSp = calculatePrimitiveConverter(DoubleType, classOf[String])
		val bAsSp = data.map(r => dToSp(bExtractor(r))).toList
		assert(List("1.1", "2.2", "3.3", "4.4", "5.5", "6.6", "7.7", "8.8", "9.9") === bAsSp)
	}

	test("Check conversion boxing") {
		val schema = structSchema(schemaField("a", IntegerType),
		                          schemaField("b", DoubleType),
		                          schemaField("c", StringType))

		val data = List(Row(1, 1.1, "1.11"),
		                Row(2, 2.2, "2.22"),
		                Row(3, 3.3, "3.33"),
		                Row(4, 4.4, "4.44"),
		                Row(5, 5.5, "5.55"),
		                Row(6, 6.6, "6.66"),
		                Row(7, 7.7, "7.77"),
		                Row(8, 8.8, "8.88"),
		                Row(9, 9.9, "9.99"))

		val aExtractor = calculateExtractor("a", schema)
		val iToL = calculateConverter(IntegerType, LongType)

		val results = data.map(row => iToL(aExtractor(row)).asInstanceOf[Long]+4)
		assert(List(5, 6, 7, 8, 9, 10, 11, 12, 13) === results)

		val iToLp = calculatePrimitiveConverter(IntegerType, classOf[Long])
		val resultsP = data.map(row => iToLp(aExtractor(row))+4)
		assert(List(5, 6, 7, 8, 9, 10, 11, 12, 13) === resultsP)
	}

	test("Numeric Function Passing") {
		val schema = structSchema(schemaField("a", IntegerType),
		                          schemaField("b", DoubleType),
		                          schemaField("c", StringType))

		val data = List(Row(1, 1.1, "1.11"),
		                Row(2, 2.2, "2.22"),
		                Row(3, 3.3, "3.33"),
		                Row(4, 4.4, "4.44"),
		                Row(5, 5.5, "5.55"),
		                Row(6, 6.6, "6.66"),
		                Row(7, 7.7, "7.77"),
		                Row(8, 8.8, "8.88"),
		                Row(9, 9.9, "9.99"))

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


class SchemaTypeUtilitiesSparkTestSuite extends FunSuite with SharedSparkContext {
	import SchemaTypeUtilities._

	test("Programatic column addition") {
		val localSqlc = sqlc
		import localSqlc._

		val jsonData = sc.parallelize(1 to 10).map(n => "{\"a\": %d, \"b\": %d, \"c\": %d}".format(n, n*n, 11-n))
		val data = localSqlc.jsonRDD(jsonData)

		assert(3 === data.schema.fields.size)
		assert("a" == data.schema.fields(0).name)
		assert(LongType == data.schema.fields(0).dataType)
		assert("b" == data.schema.fields(1).name)
		assert(LongType == data.schema.fields(1).dataType)
		assert("c" == data.schema.fields(2).name)
		assert(LongType == data.schema.fields(2).dataType)

		val converter = calculatePrimitiveConverter(LongType, classOf[Double])
		val augmentFcn: Array[Any] => Any = row =>
			{
				val c = converter(row(0))
				val a = converter(row(1))
				(1.0+a)*c
			}
		val augmentedData = addColumn(data, "d", DoubleType, augmentFcn, "c", "a")
		assert(4 === augmentedData.schema.fields.size)
		assert("a" == augmentedData.schema.fields(0).name)
		assert(LongType == augmentedData.schema.fields(0).dataType)
		assert("b" == augmentedData.schema.fields(1).name)
		assert(LongType == augmentedData.schema.fields(1).dataType)
		assert("c" == augmentedData.schema.fields(2).name)
		assert(LongType == augmentedData.schema.fields(2).dataType)
		assert("d" == augmentedData.schema.fields(3).name)
		assert(DoubleType == augmentedData.schema.fields(3).dataType)

		val collectedData = augmentedData.collect

		assert(10 === collectedData.size)
		for (i <- 1 to 10) {
			val row = collectedData(i-1)
			assert(4 === row.size)
			val a = row(0).asInstanceOf[Long]
			val b = row(1).asInstanceOf[Long]
			val c = row(2).asInstanceOf[Long]
			val d = row(3).asInstanceOf[Double]

			assert(i === a)
			assert(b === a * a)
			assert(c === 11 - a)
			assert(d === (1.0 + a) * c)
		}
	}

	test("Column removal") {

		val localSqlc = sqlc
		import localSqlc._

		// Generate test data
		val jsonData = sc.parallelize(1 to 10).map(n => "{\"a\": %d, \"b\": %d, \"c\": %d}".format(n, n*n, 11-n))
		val data = localSqlc.jsonRDD(jsonData)

		// Remove columns
		val resultDf = removeColumns(data, Set("a", "c"))

		// Check the output
		assert(resultDf.schema.fieldNames.length == 1)
		assert(resultDf.schema.fieldNames(0) == "b")
		var counter = 1
		resultDf.collect().foreach { row =>
			assert(row.getLong(0) == counter*counter)
			counter += 1
		}
	}

	test("Zip") {
		val localSqlc = sqlc
		import localSqlc._

		// Generate test data
		val jsonData = sc.parallelize(1 to 10).map(n => "{\"a\": %d, \"b\": %d, \"c\": %d}".format(n, n*n, 11-n))
		val data = localSqlc.jsonRDD(jsonData)

		// Add a string column
		val stringRdd : RDD[String] = sc.parallelize(1 to 10).map(n => n.toString)
		val resultDf1 = zip[String](data, stringRdd, "d")

		// Add an integer column
		val intRdd : RDD[Integer] = sc.parallelize(1 to 10).map(n => 2 * n)
		val resultDf2 = zip[Integer](resultDf1, intRdd, "e")

		// Add a double column
		val doubleRdd : RDD[Double] = sc.parallelize(1 to 10).map(n => n * 0.1)
		val resultDf3 = zip[Double](resultDf2, doubleRdd, "f")

		val resultDf = resultDf3

		// Check the schema
		assert(resultDf.schema.fieldNames.length == 6)
		var counter = 0
		for (c <- 'a' to 'f') {
			assert(resultDf.schema.fieldNames(counter) == c.toString)
			counter += 1
		}

		// Check the data
		counter = 1
		resultDf.collect().foreach { row =>
			assert(row.getLong(0) == counter)
			assert(row.getLong(1) == counter*counter)
			assert(row.getLong(2) == 11-counter)
			assert(row.getString(3) == counter.toString)
			assert(row.getInt(4) == 2 * counter)
			assert((row.getDouble(5) - (counter * 0.1)).abs < 0.000000001)
			counter += 1
		}
	}

}
