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

import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.apache.spark.sql.{Row, StructField, DataType, StructType, ArrayType}
import org.apache.spark.sql.{BooleanType, StringType}
import org.apache.spark.sql.{ByteType, ShortType, IntegerType, LongType}
import org.apache.spark.sql.{FloatType, DoubleType}
import org.apache.spark.sql.{TimestampType}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by nkronenfeld on 12/16/2014.
 */
object SchemaTypeUtilities {
	// Convenience functions for easy schema construction

	/** Construct a field for use in map schema */
	def schemaField (name: String, dataType: Class[_]): StructField =
		schemaField(name, _dataTypesOfClasses(dataType))
	/** Construct a field for use in map schema */
	def schemaField (name: String, dataType: Class[_], nullable: Boolean): StructField =
		schemaField(name, _dataTypesOfClasses(dataType), nullable)
	/** Construct a field for use in map schema */
	def schemaField (name: String, dataType: DataType, nullable: Boolean = true) =
		new StructField(name, dataType, nullable)

	/** Construct an array type for use in schema fields */
	def arraySchema (elementType: Class[_]): ArrayType =
		arraySchema(_dataTypesOfClasses(elementType))
	/** Construct an array type for use in schema fields */
	def arraySchema (elementType: Class[_], containsNull: Boolean): ArrayType =
		arraySchema(_dataTypesOfClasses(elementType), containsNull)
	/** Construct an array type for use in schema fields */
	def arraySchema (elementType: DataType, containsNull: Boolean = true) =
		new ArrayType(elementType, containsNull)

	/** Construct a map schema */
	def structSchema (fields: StructField*) = new StructType(fields.toSeq)

	// Convenience functions for easy row construction
	/** Construct a row */
	def row (values: Any*) = new GenericRow(values.toArray)
	/** Construct an array for use as a value in a row */
	def array (values: Any*) = ArrayBuffer[Any](values:_*)



	/**
	 * Split a string at a delimiter, allowing escaping of the delimiter character.  The delimiter character and the
	 * escape character, when escaped, will be replaced in the output by their unescaped version.
	 *
	 * @param input The string to split
	 * @param delimiter The character on which to split it
	 * @param escapeChar The escape character with which to escape non-delimiter versions of the delimiter character.
	 * @return An array of the delimited parts of the input string.
	 */
	private[datasets] def unescapedSplit(input: String, delimiter: Char, escapeChar: Char = '\\'): Array[String] = {
		val tokenizer = new StringBuilder
		val results = new ArrayBuffer[String]()
		var escaped = false
		input.foreach(c =>
			{
				if (escaped) {
					tokenizer += c
					escaped = false
				} else if (c == delimiter) {
					results += tokenizer.mkString
					tokenizer.clear()
				} else if (c == escapeChar) {
					escaped = true
				} else
					tokenizer += c
			}
		)
		results += tokenizer.mkString
		results.toArray
	}

	/**
	 * Parse a single-string specification of column (and subcolumn, etc) into a usable, separated sequence of
	 * column names, with associated array indices.
	 *
	 * @param fieldSpec A hierarchical column specification. Levels are separated by periods.  Array indices are
	 *                  enclosed in brackets (i.e., []).  Spaces in this are <em>not</em> ignored - they become part
	 *                  of the column name.  Column names are case-sensitive.  Multiple indices are specified by
	 *                  having multiple sets of brackets.  Spaces within brackets are allowed.
	 * @return An array of column specifications by level, starting with the first (outer) level.  Each column
	 *         specification contains the name of that column, followed by the index or indices of array entries of
	 *         interest within it.
	 */
	private[datasets] def parseColumnSpec(fieldSpec: String): Array[(String, Array[Int])] = {
		val fieldParts = unescapedSplit(fieldSpec, '.', '\\')
		fieldParts.map(part =>
			{
				if (part.contains("[") && part.endsWith("]")) {
					val start = part.indexOf('[')
					(part.substring(0, start), part.substring(start+1, part.length-1).split("]\\[").map(_.trim.toInt))
				} else {
					(part, Array[Int]())
				}
			}
		)
	}

	private[datasets] def fieldSpecToIndices (fieldSpec: Array[(String, Array[Int])],
	                                          schema: StructType): Array[Int] = {
		def getImmediateIndex(symbol: String, testSchema: StructType): Int =
			testSchema.fields.zipWithIndex.filter(p => p._1.name == symbol).map(_._2).head
		def getArrayType (depth: Int, arrayType: DataType): DataType =
			if (0 == depth) arrayType
			else if (1 == depth) arrayType.asInstanceOf[ArrayType].elementType
			else getArrayType(depth-1, arrayType.asInstanceOf[ArrayType].elementType)

		if (0 == fieldSpec.length) {
			Array[Int]()
		} else {
			val (field, arrayIndices) = fieldSpec(0)
			val fieldIndex = getImmediateIndex(field, schema)
			val subFieldType = getArrayType(arrayIndices.length, schema.fields(fieldIndex).dataType)
			val immediateIndices = fieldIndex +: arrayIndices
			if (1 == fieldSpec.length) {
				immediateIndices
			} else {
				val tail = fieldSpec.drop(1)
				val field = schema.fields(fieldIndex)
				immediateIndices ++ fieldSpecToIndices(tail, subFieldType.asInstanceOf[StructType])
			}
		}
	}

	/**
	 * Get an element of a hierarchical row/array, as specified by an array of indices per level
	 */
	private[datasets] def getElement (indexPath: Array[Int], row: Row): Any = {
		if (0 == indexPath.length) null
		else if (1 == indexPath.length) row(indexPath(0))
		else {
			val subRow = row(indexPath(0))
			if (subRow.isInstanceOf[Row]) {
				getElement(indexPath.drop(1), subRow.asInstanceOf[Row])
			} else {
				getElement(indexPath.drop(1), subRow.asInstanceOf[ArrayBuffer[_]])
			}
		}
	}

	/**
	 * Get an element of a hierarchical row/array, as specified by an array of indices per level
	 */
	private[datasets] def getElement (indexPath: Array[Int], array: ArrayBuffer[_]): Any = {
		if (0 == indexPath.length) null
		else if (1 == indexPath.length) array(indexPath(0))
		else {
			val element = array(indexPath(0))
			if (element.isInstanceOf[Row]) {
				getElement(indexPath.drop(1), element.asInstanceOf[Row])
			} else {
				getElement(indexPath.drop(1), element.asInstanceOf[ArrayBuffer[_]])
			}
		}
	}

	/**
	 * Calculate a function to extract a single, typed field from a SchemaRDD.  calculateExtractor does not in itself
	 * extract these values, it just returns a function that does.
	 *
	 * @param columnSpec A specification of the field of interest
	 * @return A function to pull the field of interest out of a row of the given schema
	 */
	def calculateExtractor(columnSpec: String, schema: StructType): Row => Any = {
		val fieldSpec = parseColumnSpec(columnSpec)
		val indices = fieldSpecToIndices(fieldSpec, schema)

		r: Row => getElement(indices, r)
	}

	private val _classesOfDataTypes: Map[DataType, Class[_]] =
		Map(BooleanType -> classOf[Boolean],
		    StringType -> classOf[String],
		    ByteType -> classOf[Byte],
		    ShortType -> classOf[Short],
		    IntegerType -> classOf[Integer],
		    LongType -> classOf[Long],
		    FloatType -> classOf[Float],
		    DoubleType -> classOf[Double],
		    TimestampType -> classOf[Timestamp]
		)
	private val _dataTypesOfClasses: Map[Class[_], DataType] =
		_classesOfDataTypes.map(_.swap)
}
