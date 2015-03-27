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



import java.util.{Calendar, Properties, TimeZone}

import com.oculusinfo.tilegen.util.PropertiesWrapper
import org.apache.log4j.{Level, LogManager}
import org.apache.spark.SharedSparkContext
import org.apache.spark.sql._
import org.scalatest.FunSuite



/**
 * Created by nkronenfeld on 12/17/2014.
 */
class CSVReaderTestSuite extends FunSuite with SharedSparkContext {
	private def createReader = {
		val sqlc = new SQLContext(sc)
		val data = sc.parallelize(1 to 100).map(n =>
			{
				val boolString     = (0 == (n%2)).toString
				val byteString     = n.toByte.toString
				val shortString    = n.toShort.toString
				val intString      = n.toInt.toString
				val longString     = n.toLong.toString
				val floatString    = n.toFloat.toString
				val doubleString   = n.toDouble.toString
				val strString      = "abc"+n
				val ipv4String     = "192.168.0."+n
				val dateString     = "%02d:%02d:%02d".format(n%12, n%60, n%60)
				val propertyString = "a=aval;b=bval;n="+n
				Array(boolString, byteString, shortString, intString, longString, floatString,
				      doubleString, strString, ipv4String, dateString, propertyString).mkString(",")
			}
		)
		val configuration = new Properties()

		configuration.setProperty("oculus.binning.parsing.separator",        ",")
		configuration.setProperty("oculus.binning.parsing.bool.index",       "0")
		configuration.setProperty("oculus.binning.parsing.bool.fieldType",   "boolean")
		configuration.setProperty("oculus.binning.parsing.byte.index",       "1")
		configuration.setProperty("oculus.binning.parsing.byte.fieldType",   "byte")
		configuration.setProperty("oculus.binning.parsing.short.index",      "2")
		configuration.setProperty("oculus.binning.parsing.short.fieldType",  "short")
		configuration.setProperty("oculus.binning.parsing.int.index",        "3")
		configuration.setProperty("oculus.binning.parsing.int.fieldType",    "int")
		configuration.setProperty("oculus.binning.parsing.long.index",       "4")
		configuration.setProperty("oculus.binning.parsing.long.fieldType",   "long")
		configuration.setProperty("oculus.binning.parsing.float.index",      "5")
		configuration.setProperty("oculus.binning.parsing.float.fieldType",  "float")
		configuration.setProperty("oculus.binning.parsing.double.index",     "6")
		configuration.setProperty("oculus.binning.parsing.double.fieldType", "double")
		configuration.setProperty("oculus.binning.parsing.str.index",        "7")
		configuration.setProperty("oculus.binning.parsing.str.fieldType",    "string")
		configuration.setProperty("oculus.binning.parsing.ip.index",         "8")
		configuration.setProperty("oculus.binning.parsing.ip.fieldType",     "ipv4")
		configuration.setProperty("oculus.binning.parsing.date.index",       "9")
		configuration.setProperty("oculus.binning.parsing.date.fieldType",   "date")
		configuration.setProperty("oculus.binning.parsing.date.dateFormat",  "HH:mm:ss")
		configuration.setProperty("oculus.binning.parsing.prop.index",       "10")
		configuration.setProperty("oculus.binning.parsing.prop.fieldType",   "propertyMap")
		configuration.setProperty("oculus.binning.parsing.prop.property",               "n")
		configuration.setProperty("oculus.binning.parsing.prop.propertyType",           "int")
		configuration.setProperty("oculus.binning.parsing.prop.propertySeparator",      ";")
		configuration.setProperty("oculus.binning.parsing.prop.propertyValueSeparator", "=")

		new CSVReader(sqlc, data, new PropertiesWrapper(configuration))
	}

	test("Test CSV => schema conversion") {
		val reader = createReader
		val fields = reader.schema.fields
		assert(11 === fields.size)

		assert(("bool",   BooleanType)         === (fields( 0).name, fields( 0).dataType))
		assert(("byte",   ByteType)            === (fields( 1).name, fields( 1).dataType))
		assert(("short",  ShortType)           === (fields( 2).name, fields( 2).dataType))
		assert(("int",    IntegerType)         === (fields( 3).name, fields( 3).dataType))
		assert(("long",   LongType)            === (fields( 4).name, fields( 4).dataType))
		assert(("float",  FloatType)           === (fields( 5).name, fields( 5).dataType))
		assert(("double", DoubleType)          === (fields( 6).name, fields( 6).dataType))
		assert(("str",    StringType)          === (fields( 7).name, fields( 7).dataType))
		assert(("ip",     ArrayType(ByteType)) === (fields( 8).name, fields( 8).dataType))
		assert(("date",   LongType)       === (fields( 9).name, fields( 9).dataType))
		assert(("prop",   IntegerType)         === (fields(10).name, fields(10).dataType))
	}

	test("Test CSV => Data conversion") {
		LogManager.getRootLogger.setLevel(Level.WARN)
		val reader = createReader
		import reader.sqlc._

		val booleans = reader.asSchemaRDD.select('bool).map(_(0).asInstanceOf[Boolean]).collect.toList
		booleans.grouped(2).foreach(values => assert(List(false, true) === values))

		val bytes = reader.asSchemaRDD.select('byte).map(_(0).asInstanceOf[Byte]).collect.toList
		bytes.zipWithIndex.foreach(values => assert((1+values._2).toByte === values._1))

		val shorts = reader.asSchemaRDD.select('short).map(_(0).asInstanceOf[Short]).collect.toList
		shorts.zipWithIndex.foreach(values => assert((1+values._2).toShort=== values._1))

		val ints = reader.asSchemaRDD.select('int).map(_(0).asInstanceOf[Int]).collect.toList
		ints.zipWithIndex.foreach(values => assert((1+values._2).toInt=== values._1))

		val longs = reader.asSchemaRDD.select('long).map(_(0).asInstanceOf[Long]).collect.toList
		longs.zipWithIndex.foreach(values => assert((1+values._2).toLong === values._1))

		val floats = reader.asSchemaRDD.select('float).map(_(0).asInstanceOf[Float]).collect.toList
		floats.zipWithIndex.foreach(values => assert((1+values._2).toFloat === values._1))

		val doubles = reader.asSchemaRDD.select('double).map(_(0).asInstanceOf[Double]).collect.toList
		doubles.zipWithIndex.foreach(values => assert((1+values._2).toDouble === values._1))

		val strings = reader.asSchemaRDD.select('str).map(_(0).asInstanceOf[String]).collect.toList
		strings.zipWithIndex.foreach(values => assert("abc%d".format(1+values._2) == values._1))

		val ips = reader.asSchemaRDD.select('ip).map(_(0).asInstanceOf[Seq[Byte]]).collect.toList
		ips.zipWithIndex.foreach(values =>
			{
				assert(4 === values._1.size)
				assert(192.toByte === values._1(0))
				assert(168.toByte === values._1(1))
				assert(0.toByte   === values._1(2))
				assert((values._2+1).toByte === values._1(3))
			}
		)

		val dates = reader.asSchemaRDD.select('date).map(_(0).asInstanceOf[Long]).collect.toList
		dates.zipWithIndex.foreach(values =>
			{
				// val date: Timestamp = values._1
				val date = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
				date.setTimeInMillis(values._1)

				val n = values._2+1
				assert((n%12) === date.get(Calendar.HOUR_OF_DAY))
				assert((n%60) === date.get(Calendar.MINUTE))
				assert((n%60) === date.get(Calendar.SECOND))
			}
		)

		val props = reader.asSchemaRDD.select('prop).map(_(0).asInstanceOf[Int]).collect.toList
		props.zipWithIndex.foreach(values => assert((1+values._2).toInt=== values._1))
	}

	test("CSV parsing key case sensitivity") {
		val configuration = new Properties()
		configuration.setProperty("oculus.binning.parsing.caseSensitiveKey.index",     "0")
		configuration.setProperty("oculus.binning.parsing.caseSensitiveKey.fieldType", "string")
		val data = sc.parallelize(List("test result"))
		val reader = new CSVReader(sqlc, data, new PropertiesWrapper(configuration))
		import reader.sqlc._

		val result = reader.asSchemaRDD.select('caseSensitiveKey).map(_(0).asInstanceOf[String]).first()
		assertResult("test result")(result)

		intercept[Exception] {
			reader.asSchemaRDD.select('CaseSensitiveKey).map(_(0).asInstanceOf[String]).first()
		}

		intercept[Exception] {
			reader.asSchemaRDD.select('casesensitivekey).map(_(0).asInstanceOf[String]).first()
		}
	}

	test("CSV parse fieldType case insensitivity") {
		val configuration = new Properties()
		configuration.setProperty("oculus.binning.parsing.test.index",     "0")
		configuration.setProperty("oculus.binning.parsing.test.fieldType", "String")
		val data = sc.parallelize(List("test result"))
		val reader = new CSVReader(sqlc, data, new PropertiesWrapper(configuration))
		import reader.sqlc._

		val result = reader.asSchemaRDD.select('test).map(_(0).asInstanceOf[String]).first()
		assertResult("test result")(result)
	}

	test("CSV parse propertyMap case sensitivity") {
		val configuration = new Properties()
		configuration.setProperty("oculus.binning.parsing.testMap.index",                  "0")
		configuration.setProperty("oculus.binning.parsing.testMap.fieldType",              "proPertymaP")
		configuration.setProperty("oculus.binning.parsing.testMap.property",               "caseSensitiveKey")
		configuration.setProperty("oculus.binning.parsing.testMap.propertyType",           "StrIng")
		configuration.setProperty("oculus.binning.parsing.testMap.propertySeparator",      ";")
		configuration.setProperty("oculus.binning.parsing.testMap.propertyValueSeparator", "=")

		val data = sc.parallelize(List("caseSensitiveKey=test value"))
		val reader = new CSVReader(sqlc, data, new PropertiesWrapper(configuration))

		val failData = sc.parallelize(List("casesensitivekey=test value"))
		val failReader = new CSVReader(sqlc, failData, new PropertiesWrapper(configuration))

		import reader.sqlc._

		val result = reader.asSchemaRDD.select('testMap).map(_(0).asInstanceOf[String]).first()
		assertResult("test value")(result)

		intercept[Exception] {
			failReader.asSchemaRDD.select('testMap).map(_(0).asInstanceOf[String]).first()
		}
	}
}
