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



import java.text.SimpleDateFormat
import java.util.TimeZone

import scala.util.{Try, Success, Failure}

import com.oculusinfo.tilegen.util.PropertiesWrapper



object CSVRecordParser {
	def fromProperties (properties: CSVRecordPropertiesWrapper): CSVRecordParser = {
		val parserType = properties.getString(
			"oculus.binning.parsing.type", "", Some("standard")
		)

		parserType match {
			case "graph" =>
				new GraphRecordParser(properties)
			case _ =>
				new CSVRecordParser(properties)
		}
	}
}

/**
 * A simple parser that splits a record according to a given separator
 */
class CSVRecordParser (properties: CSVRecordPropertiesWrapper) {

	// A quick couple of inline functions to make our lives easier
	// Split a string (but do so efficiently if the separator is a single character)
	def splitString(input: String, separator: String): Array[String] =
		if (1 == separator.length) input.split(separator.charAt(0))
		else input.split(separator)

	def getFieldType(field: String, suffix: String = "fieldType"): String = {
		properties.getString("oculus.binning.parsing." + field + "." + suffix,
		                     "",
		                     Some(if ("constant" == field || "zero" == field) "constant"
		                          else ""))
	}

	// Convert a string to a double value according to field semantics
	def parseValue (value: String, field: String, parseType: String,
	                dateFormats: Map[String, SimpleDateFormat]): Any = {
		if ("int" == parseType) {
			value.toInt.toDouble
		} else if ("long" == parseType) {
			value.toLong.toDouble
		} else if ("date" == parseType) {
			dateFormats(field).parse(value).getTime()
		} else if ("ipv4" == parseType) {
			value.split("\\.").map(_.toLong).foldLeft(0L)((a, b) => (256L * a + b)).toDouble
		} else if ("propertyMap" == parseType) {
			val property = properties.getStringOption(
				"oculus.binning.parsing." + field + ".property", ""
			).get
			val propType = getFieldType(field, "propertyType")
			val propSep = properties.getStringOption(
				"oculus.binning.parsing." + field + ".propertySeparator", ""
			).get
			val valueSep = properties.getStringOption(
				"oculus.binning.parsing." + field + ".propertyValueSeparator", ""
			).get

			val kvPairs = splitString(value, propSep)
			val propPairs = kvPairs.map(splitString(_, valueSep))

			val propValue = propPairs.filter(kv => property.trim == kv(0).trim).map(kv =>
				if (kv.size > 1) kv(1) else "").takeRight(1)(0)
			parseValue(propValue, field, propType, dateFormats)
		} else if ("string" == parseType || "substring" == parseType) {
			value
		} else {
			value.toDouble
		}
	}

	def getRecordFilter: Array[String] => Boolean = fields => true

	def parseRecords (raw: Iterator[String], variables: String*):
			Iterator[(String, Try[List[Any]])] =
	{
		// This method generally is only called on workers, therefore
		// properties can't really be documented here.

		// Get any potential record filter we must use
		val filter = getRecordFilter

		// Get some simple parsing info we'll need
		val separator = properties.getString(
			"oculus.binning.parsing.separator", "", Some("\t"))

		val dateFormats = properties.fields.filter(field =>
			"date" == properties.getString(
				"oculus.binning.parsing." + field + ".fieldType", "", Some("")
			)
		).map(field =>
			{
				val format = new SimpleDateFormat(
					properties.getString("oculus.binning.parsing." + field + ".dateFormat",
					                     "", Some("yyMMddHHmm")
					)
				)
				format.setTimeZone(TimeZone.getTimeZone("GMT"))
				(field -> format)
			}
		).toMap

		raw.map(s =>
			{
				val columns = splitString(s, separator)

				(s, Try(
					 if (filter(columns)) {
						 properties.fields.toList.map(field =>
							 {
								 val fieldType = getFieldType(field)
								 var value = if ("constant" == fieldType
									                 || "zero" == fieldType) 0.0
								 else {
									 val fieldIndex = properties.getIntOption(
										 "oculus.binning.parsing." + field + ".index",
										 ""
									 ).get
									 parseValue(columns(fieldIndex.toInt),
									            field, fieldType, dateFormats)
								 }
								 val fieldScaling = properties.getString(
									 "oculus.binning.parsing." + field + ".fieldScaling",
									 "", Some("")
								 )
								 if ("log" == fieldScaling) {
									 val base = properties.getDouble(
										 "oculus.binning.parsing." + field + ".fieldBase",
										 "", Some(math.exp(1.0))
									 )
									 value = math.log(value.asInstanceOf[Double]) / math.log(base)
								 }
								 value
							 }
						 )
					 } else {
						 List[Double]()
					 }
				 )
				)
			}
		)
	}
}

/**
 * Custom record parser for use with graph data				//TODO -- should this be moved to CSVGraphBinner.scala?
 */
class GraphRecordParser (properties: CSVRecordPropertiesWrapper)
		extends CSVRecordParser(properties)
{
	override def getRecordFilter: Array[String] => Boolean = {
		// Only parse lines that have the first column equal to the graph field id.
		val graphDataType =
			properties.getString("oculus.binning.graph.data", "", Some("nodes"))
		val graphFieldId =
			if ("nodes" == graphDataType) "node"
			else "edge"

		fields: Array[String] => graphFieldId == fields(0)
	}
}
