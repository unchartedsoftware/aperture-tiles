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

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.TimeZone

import com.oculusinfo.tilegen.util.KeyValueArgumentSource
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._

/**
 * A class that allows reading a schema file and a CSV file as a SchemaRDD.
 *
 * This may eventually become obsolete - Spark may be writing a similar thing.
 *
 * The schema file is, essentially, a big properties file, with the following properties:
 *
 * <dl>
 *   <dt> oculus.binning.parsing.separator </dt>
 *   <dd> The character or string to use as a separator between columns. Default is a tab </dd>
 *   <dt> oculus.binning.parsing.&lt;field&gt;.index </dt>
 *   <dd> The column number of the described field. This field is mandatory for every field type to be used. </dd>
 *   <dt> oculus.binning.parsing.&lt;field&gt;.fieldType </dt>
 *   <dd> The type of value expected in the column specified by oculus.binning.parsing.&lt;field&gt;.index.  Default
 *        is to treat the column as containing real, double-precision values.  Other possible types are:
 *     <dl>
 *       <dt> constant or zero </dt>
 *       <dd> Treat the column as containing 0.0 (the column doesn't actually have to exist) </dd>
 *       <dt> boolean </dt>
 *       <dd> Treat the column as containing boolean values (true/false, yes/no) </dd>
 *       <dt> byte </dt>
 *       <dd> Treat the column as containing bytes </dd>
 *       <dt> shoft </dt>
 *       <dd> Treat the column as containing short integers </dd>
 *       <dt> int </dt>
 *       <dd> Treat the column as containing integers </dd>
 *       <dt> long </dt>
 *       <dd> Treat the column as containing double-precision integers </dd>
 *       <dt> float </dt>
 *       <dd> Treat the column as containing floating-point numbers </dd>
 *       <dt> double (the default) </dt>
 *       <dd> Treat the column as containing double-precision floating-point numbers </dd>
 *       <dt> ipv4 </dt>
 *       <dd> Treat the column as an IP address.  It will be treated as a  4-digit base 256 number, and just turned
 *            into a double </dd>
 *       <dt> date </dt>
 *       <dd> Treat the column as containing a date.  The date will be parsed and transformed into milliseconds
 *            since the standard java start date (using SimpleDateFormatter). Default format is yyMMddHHmm, but 
 *            this can be overridden using the oculus.binning.parsing.&lt;field&gt;.dateFormat. </dd>
 *       <dt> propertyMap </dt>
 *       <dd> Treat the column as a property map.  Further information is then needed to get the specific property.
 *            All four of the following properties must be present to read the property.
 *         <dl>
 *           <dt> oculus.binning.parsing.&lt;field&gt;.property </dt>
 *           <dd> The name of the property to read </dd>
 *           <dt> oculus.binning.parsing.&lt;field&gt;.propertyType </dt>
 *           <dd> equivalent to fieldType </dd>
 *           <dt> oculus.binning.parsing.&lt;field&gt;.propertySeparator </dt>
 *           <dd> The character or string to use to separate one property from the next </dd>
 *           <dt> oculus.binning.parsing.&lt;field&gt;.propertyValueSeparator </dt>
 *           <dd> The character or string used to separate a property key from its value </dd>
 *         </dl>
 *       </dd>
 *     </dl>
 *   </dd>
 *   <dt> oculus.binning.parsing.&lt;field&gt;.fieldScaling </dt>
 *   <dd> How the field values should be scaled.  Default is to leave values as they are.  Other possibilities are:
 *     <dl>
 *       <dt> log </dt>
 *       <dd> Take the log of the value.  The base of the logarithm is taken from
 *            <code>oculus.binning.parsing,%lt;field&gt;.fieldBase</code>. </dd>
 *     </dl>
 *   </dd>
 * </dl>
 *
 * Created by nkronenfeld on 12/16/2014.
 */
class CSVReader (val sqlc: SQLContext, data: RDD[String], configuration: KeyValueArgumentSource) {
	import SchemaTypeUtilities._

	// alternate single-file constructor
	def this (sqlc: SQLContext, file: String, configuration: KeyValueArgumentSource) =
		this(sqlc, sqlc.sparkContext.textFile(file), configuration)

	def this (sqlc: SQLContext, files: Array[String], configuration: KeyValueArgumentSource) =
		this(sqlc,
		     files.map(sqlc.sparkContext.textFile(_)).fold(sqlc.sparkContext.emptyRDD[String])(_ union _),
		     configuration)


	/**
	 * Get the wrapped CSV RDD as a SchemaRDD, parsed and typed.
	 */
	def asSchemaRDD = _parsed

	def schema = _schema

	// Get some simple parsing info we'll need
	private val _separator = configuration.getString("oculus.binning.parsing.separator",
	                                                 "The separator to use between fields in the input data",
	                                                 Some("\t"))

	private lazy val _parsed: SchemaRDD = {
		val separator = _separator
		val parsers = _parsers
		val N = _fields
		val rowRDD: RDD[Row] = data.map(record =>
			{
				val fields = record.split(separator)
				val values = (0 until N).map(n => parsers(n)(fields(n)))
				row(values:_*)
			}
		)
		sqlc.applySchema(rowRDD, _schema)
	}

	// _schema: the schema of our CSV file, as specified by our configuration
	// _indices: The column index of each field in the schema, in order
	// _parsers: A parser of each field in the schema, in order, from a string to the desired type.
	// _fields: the number of parsed fields
	// Do not calculate these lazily - we want errors as soon as the reader is made, for ease of debugging.
	private val (_schema, _indices, _parsers, _fields) = {
		// A quick internal function to get a field or field property type.
		def getFieldType(field: String, suffix: String = "fieldType"): String =
			configuration.getString("oculus.binning.parsing." + field + "." + suffix, "You should never see this.",
			                        Some(if ("constant" == field || "zero" == field) "constant" else ""))

		// A quick internal function to parse a string type into a data type and a parser function
		def getParseFunction (fieldName: String, stringType: String): (DataType, String => Any) =
			stringType match {
				case "boolean" => (BooleanType, s => {
					                   val ss = s.toLowerCase.trim
					                   s == "yes" || s == "true" || s == "1"
				                   })
				case "byte" => (ByteType, s => s.trim.toShort.toByte)
				case "short" => (ShortType, s => s.trim.toShort)
				case "int" => (IntegerType, s => s.trim.toInt)
				case "long" => (LongType, s => s.trim.toLong)
				case "float" => (FloatType, s => s.trim.toFloat)
				case "double" => (DoubleType, s => s.trim.toDouble)
				case "string" => (StringType, s => s)
				case "ipv4" => (ArrayType(ByteType), s => {
					                s.trim.split("\\.").map(_.trim.toShort.toByte).toSeq
				                })
				case "date" => {
					val format = new SimpleDateFormat(
						configuration.getString("oculus.binning.parsing." + fieldName + ".dateFormat",
						                        "The date format of the "+fieldName+" field",
						                        Some("yyMMddHHmm")))
					format.setTimeZone(TimeZone.getTimeZone("GMT"))

					(TimestampType, s => new Timestamp(format.parse(s.trim).getTime()))
				}
				case "propertymap" => {
					val property = configuration.getString(
						"oculus.binning.parsing." + fieldName + ".property",
						"Property name for the property of interest in the "+fieldName+" field",
						None).toLowerCase.trim
					val propertyType = getFieldType(fieldName, "propertyType")
					val propSep = configuration.getString(
						"oculus.binning.parsing." + fieldName + ".propertySeparator",
						"The field separator between properties in the "+fieldName+" field",
						None)
					val valueSep = configuration.getString(
						"oculus.binning.parsing." + fieldName + ".propertyValueSeparator",
						"The separator between keys and values of properties in the "+fieldName+" field",
						None)
					val (eltType, eltParser) = getParseFunction(fieldName, propertyType)

					(eltType, s => {
						 val rawValue = s.split(propSep)
							 .map(_.split(valueSep))
							 .filter(kv => property == kv(0).trim.toLowerCase)
							 .map(kv => if (kv.size > 1) kv(1) else "")
							 .takeRight(1)(0)
						 eltParser(rawValue)
					 })
				}
			}

		// Finally done helper functions.
		// Get our field-by-field information
		val fieldByField = configuration.properties.keys
			.filter(_.startsWith("oculus.binning.parsing."))
			.filter(_.endsWith(".index"))
			.map(indexProperty =>
			{
				val fieldName = indexProperty.substring("oculus.binning.parsing.".length,
				                                        indexProperty.length - ".index".length)
				val fieldIndex = configuration.getInt(indexProperty, "The column number of the " + fieldName + " field")
				val fieldType = getFieldType(fieldName).toLowerCase.trim
				val (dataType, parser) = getParseFunction(fieldName, fieldType)

				(schemaField(fieldName, dataType), fieldIndex, parser)
			}
		).toSeq.sortBy(_._2)

		(structSchema(fieldByField.map(_._1):_*), fieldByField.map(_._2), fieldByField.map(_._3), fieldByField.size)
	}
}
