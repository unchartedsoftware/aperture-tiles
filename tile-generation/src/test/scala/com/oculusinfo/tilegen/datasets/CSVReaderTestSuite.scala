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
import org.apache.spark.sql.types._

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
		configuration.setProperty("oculus.binning.parsing.cbool.index",       "0")
		configuration.setProperty("oculus.binning.parsing.cbool.fieldType",   "boolean")
		configuration.setProperty("oculus.binning.parsing.cbyte.index",       "1")
		configuration.setProperty("oculus.binning.parsing.cbyte.fieldType",   "byte")
		configuration.setProperty("oculus.binning.parsing.cshort.index",      "2")
		configuration.setProperty("oculus.binning.parsing.cshort.fieldType",  "short")
		configuration.setProperty("oculus.binning.parsing.cint.index",        "3")
		configuration.setProperty("oculus.binning.parsing.cint.fieldType",    "int")
		configuration.setProperty("oculus.binning.parsing.clong.index",       "4")
		configuration.setProperty("oculus.binning.parsing.clong.fieldType",   "long")
		configuration.setProperty("oculus.binning.parsing.cfloat.index",      "5")
		configuration.setProperty("oculus.binning.parsing.cfloat.fieldType",  "float")
		configuration.setProperty("oculus.binning.parsing.cdouble.index",     "6")
		configuration.setProperty("oculus.binning.parsing.cdouble.fieldType", "double")
		configuration.setProperty("oculus.binning.parsing.cstr.index",        "7")
		configuration.setProperty("oculus.binning.parsing.cstr.fieldType",    "string")
		configuration.setProperty("oculus.binning.parsing.cip.index",         "8")
		configuration.setProperty("oculus.binning.parsing.cip.fieldType",     "ipv4")
		configuration.setProperty("oculus.binning.parsing.cdate.index",       "9")
		configuration.setProperty("oculus.binning.parsing.cdate.fieldType",   "date")
		configuration.setProperty("oculus.binning.parsing.cdate.dateFormat",  "HH:mm:ss")
		configuration.setProperty("oculus.binning.parsing.cprop.index",       "10")
		configuration.setProperty("oculus.binning.parsing.cprop.fieldType",   "propertyMap")
		configuration.setProperty("oculus.binning.parsing.cprop.property",               "n")
		configuration.setProperty("oculus.binning.parsing.cprop.propertyType",           "int")
		configuration.setProperty("oculus.binning.parsing.cprop.propertySeparator",      ";")
		configuration.setProperty("oculus.binning.parsing.cprop.propertyValueSeparator", "=")

		new CSVReader(sqlc, data, new PropertiesWrapper(configuration))
	}

	test("Test CSV => schema conversion") {
		val reader = createReader
		val fields = reader.schema.fields
		assert(11 === fields.size)

		assert(("cbool",   BooleanType)         === (fields( 0).name, fields( 0).dataType))
		assert(("cbyte",   ByteType)            === (fields( 1).name, fields( 1).dataType))
		assert(("cshort",  ShortType)           === (fields( 2).name, fields( 2).dataType))
		assert(("cint",    IntegerType)         === (fields( 3).name, fields( 3).dataType))
		assert(("clong",   LongType)            === (fields( 4).name, fields( 4).dataType))
		assert(("cfloat",  FloatType)           === (fields( 5).name, fields( 5).dataType))
		assert(("cdouble", DoubleType)          === (fields( 6).name, fields( 6).dataType))
		assert(("cstr",    StringType)          === (fields( 7).name, fields( 7).dataType))
		assert(("cip",     ArrayType(ByteType)) === (fields( 8).name, fields( 8).dataType))
		assert(("cdate",   TimestampType)       === (fields( 9).name, fields( 9).dataType))
		assert(("cprop",   IntegerType)         === (fields(10).name, fields(10).dataType))
	}

	test("Test CSV => Data conversion") {
		LogManager.getRootLogger.setLevel(Level.WARN)
		val reader = createReader
		import reader.sqlc._

		val booleans = reader.asDataFrame.selectExpr("cbool").map(_(0).asInstanceOf[Boolean]).collect.toList
		booleans.grouped(2).foreach(values => assert(List(false, true) === values))

		val bytes = reader.asDataFrame.selectExpr("cbyte").map(_(0).asInstanceOf[Byte]).collect.toList
		bytes.zipWithIndex.foreach(values => assert((1+values._2).toByte === values._1))

		val shorts = reader.asDataFrame.selectExpr("cshort").map(_(0).asInstanceOf[Short]).collect.toList
		shorts.zipWithIndex.foreach(values => assert((1+values._2).toShort=== values._1))

		val ints = reader.asDataFrame.selectExpr("cint").map(_(0).asInstanceOf[Int]).collect.toList
		ints.zipWithIndex.foreach(values => assert((1+values._2).toInt=== values._1))

		val longs = reader.asDataFrame.selectExpr("clong").map(_(0).asInstanceOf[Long]).collect.toList
		longs.zipWithIndex.foreach(values => assert((1+values._2).toLong === values._1))

		val floats = reader.asDataFrame.selectExpr("cfloat").map(_(0).asInstanceOf[Float]).collect.toList
		floats.zipWithIndex.foreach(values => assert((1+values._2).toFloat === values._1))

		val doubles = reader.asDataFrame.selectExpr("cdouble").map(_(0).asInstanceOf[Double]).collect.toList
		doubles.zipWithIndex.foreach(values => assert((1+values._2).toDouble === values._1))

		val strings = reader.asDataFrame.selectExpr("cstr").map(_(0).asInstanceOf[String]).collect.toList
		strings.zipWithIndex.foreach(values => assert("abc%d".format(1+values._2) == values._1))

		val ips = reader.asDataFrame.selectExpr("cip").map(_(0).asInstanceOf[Seq[Byte]]).collect.toList
		ips.zipWithIndex.foreach(values =>
			{
				assert(4 === values._1.size)
				assert(192.toByte === values._1(0))
				assert(168.toByte === values._1(1))
				assert(0.toByte   === values._1(2))
				assert((values._2+1).toByte === values._1(3))
			}
		)

		val dates = reader.asDataFrame.selectExpr("cdate").map(_(0).asInstanceOf[java.util.Date]).collect.toList
		dates.zipWithIndex.foreach(values =>
			{
				val date = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
				date.setTime(values._1)

				val n = values._2+1
				assert((n%12) === date.get(Calendar.HOUR_OF_DAY))
				assert((n%60) === date.get(Calendar.MINUTE))
				assert((n%60) === date.get(Calendar.SECOND))
			}
		)

		val props = reader.asDataFrame.selectExpr("cprop").map(_(0).asInstanceOf[Int]).collect.toList
		props.zipWithIndex.foreach(values => assert((1+values._2).toInt=== values._1))
	}

	test("CSV parsing key case sensitivity") {
		val configuration = new Properties()
		configuration.setProperty("oculus.binning.parsing.caseSensitiveKey.index",     "0")
		configuration.setProperty("oculus.binning.parsing.caseSensitiveKey.fieldType", "string")
		val data = sc.parallelize(List("test result"))
		val reader = new CSVReader(sqlc, data, new PropertiesWrapper(configuration))
		import reader.sqlc._

		val result = reader.asDataFrame.select(new Column("caseSensitiveKey")).map(_(0).asInstanceOf[String]).first()
		assertResult("test result")(result)

		intercept[Exception] {
			reader.asDataFrame.select(new Column("CaseSensitiveKey")).map(_(0).asInstanceOf[String]).first()
		}

		intercept[Exception] {
			reader.asDataFrame.select(new Column("casesensitivekey")).map(_(0).asInstanceOf[String]).first()
		}
	}

	test("CSV parse fieldType case insensitivity") {
		val configuration = new Properties()
		configuration.setProperty("oculus.binning.parsing.test.index",     "0")
		configuration.setProperty("oculus.binning.parsing.test.fieldType", "String")
		val data = sc.parallelize(List("test result"))
		val reader = new CSVReader(sqlc, data, new PropertiesWrapper(configuration))
		import reader.sqlc._

		val result = reader.asDataFrame.select(new Column("test")).map(_(0).asInstanceOf[String]).first()
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

		val result = reader.asDataFrame.select(new Column("testMap")).map(_(0).asInstanceOf[String]).first()
		assertResult("test value")(result)

		intercept[Exception] {
			failReader.asDataFrame.select(new Column("testMap")).map(_(0).asInstanceOf[String]).first()
		}
	}

  test("CSV parse string enclosed in quotes") {
    val configuration = new Properties()
    configuration.setProperty("oculus.binning.parsing.test.index",     "0")
    configuration.setProperty("oculus.binning.parsing.test.fieldType", "String")
    configuration.setProperty("oculus.binning.parsing.quotechar", "'")
    val data = sc.parallelize(List("'test result'"))
    val reader = new CSVReader(sqlc, data, new PropertiesWrapper(configuration))
    import reader.sqlc._

    val result = reader.asDataFrame.select(new Column("test")).map(_(0).asInstanceOf[String]).first()
    assertResult("test result")(result)
  }

  test("CSV parse string enclosed in quotes with separator in text") {
    val configuration = new Properties()
    configuration.setProperty("oculus.binning.parsing.test.index",     "0")
    configuration.setProperty("oculus.binning.parsing.test.fieldType", "String")

    configuration.setProperty("oculus.binning.parsing.separator", ",")
    configuration.setProperty("oculus.binning.parsing.quotechar", "'")
    val data = sc.parallelize(List("'One, field'"))
    val reader = new CSVReader(sqlc, data, new PropertiesWrapper(configuration))
    import reader.sqlc._

    val result = reader.asDataFrame.select(new Column("test")).map(_(0).asInstanceOf[String]).first()
    assertResult("One, field")(result)
  }
}
