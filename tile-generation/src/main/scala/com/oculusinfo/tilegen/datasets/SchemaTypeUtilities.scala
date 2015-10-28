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

import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag

import org.apache.spark.sql.catalyst.expressions.{Expression}
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._



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
	def schemaField (name: String, dataType: DataType, nullable: Boolean = true, metadata: Metadata = Metadata.empty) =
		new StructField(name, dataType, nullable, metadata)

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
	def structSchema (fields: StructField*) = new StructType(fields.toArray)

	// Convenience functions for easy row construction
	/** Construct a row */
	def row (values: Any*) = Row(values:_*)
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
			testSchema.fields.zipWithIndex.filter(p => p._1.name.equalsIgnoreCase(symbol)).map(_._2).head
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
	 * Get the type of the point in a schema specified by a hierarchical index
	 */
	private[datasets] def getType (indexPath: Array[Int], schema: StructType): DataType = {
		if (0 == indexPath.length) schema
		else if (1 == indexPath.length) schema.fields(indexPath(0)).dataType
		else {
			val currentType = schema.fields(indexPath(0)).dataType
			currentType match {
				case struct: StructType => getType(indexPath.drop(1), struct)
				case array: ArrayType => getType(indexPath.drop(1), array)
			}
		}
	}
	private[datasets] def getType (indexPath: Array[Int], schema: ArrayType): DataType = {
		if (0 == indexPath.length) schema
		else if (1 == indexPath.length) schema.elementType
		else {
			val elementType = schema.elementType
			elementType match {
				case struct: StructType => getType(indexPath.drop(1), struct)
				case array: ArrayType => getType(indexPath.drop(1), array)
			}
		}
	}

	/**
	 * Calculate a function to extract a single, typed field from a DataFrame.  calculateExtractor does not in itself
	 * extract these values, it just returns a function that does.
	 *
	 * @param columnSpec A specification of the field of interest
	 * @param schema The schema of the RDD from which to pull the column
	 * @return A function to pull the field of interest out of a row of the given schema
	 */
	def calculateExtractor(columnSpec: String, schema: StructType): Row => Any = {
		val fieldSpec = parseColumnSpec(columnSpec)
		val indices = fieldSpecToIndices(fieldSpec, schema)

		r: Row => getElement(indices, r)
	}

	/**
	 * A combination of the simpler calculateExtractor and calculateConverter, so the result is guaranteed to be
	 * a particular type.
	 *
	 * @param columnSpec A specification of the field of interest
	 * @param schema The schema of the RDD from which to pull the column
	 * @param targetType The type to which to convert the result within the returned function
	 * @return A function to pull the field of interest out of a row of the given schema
	 */
	def calculateExtractor (columnSpec: String, schema: StructType, targetType: DataType): Row => Any = {
		val sourceType = getColumnType(columnSpec, schema)
		val converter = calculateConverter(sourceType, targetType)
		val baseExtractor = calculateExtractor(columnSpec, schema)

		row => converter(baseExtractor(row))
	}

	/**
	 * A combination of the simpler calculateExtractor and calculatePrimitiveConverter, so the result is guaranteed to
	 * be a particular primitive type, cast correctly.
	 *
	 * @param columnSpec A specification of the field of interest
	 * @param schema The schema of the RDD from which to pull the column
	 * @param targetType The class to which to convert.  This must be one of those types listed by _dataTypesOfClasses
	 * @return A function to pull the field of interest out of a row of the given schema
	 */
	def calculatePrimitiveExtractor[T] (columnSpec: String, schema: StructType, targetType: Class[T]): Row => T = {
		val sourceType = getColumnType(columnSpec, schema)
		val converter = calculatePrimitiveConverter(sourceType, targetType)
		val baseExtractor = calculateExtractor(columnSpec, schema)

		row => converter(baseExtractor(row))
	}

	/** A simple trait to allow creation of typed numeric functions, solely for use passing in to withNumeric */
	trait FunctionCreator {
		def createFunction[T: Numeric] (t: T): T
	}
	/** Act on a value as the appropriate type of numeric, in a type-safe manner. */
	def withNumeric[T] (value: Any, functionCreator: FunctionCreator): Any = {
		value match {
			case b: Byte =>      functionCreator.createFunction[Byte](b)
			case s: Short =>     functionCreator.createFunction[Short](s)
			case i: Int =>       functionCreator.createFunction[Int](i)
			case l: Long =>      functionCreator.createFunction[Long](l)
			case f: Float =>     functionCreator.createFunction[Float](f)
			case d: Double =>    functionCreator.createFunction[Double](d)
			case t: Timestamp => functionCreator.createFunction[Long](t.getTime)
		}
	}

	/**
	 * Figure out from a schema the type of a particular column.
	 *
	 * @param columnSpec A specification of the field of interest
	 * @param schema The schema of the RDD from which to pull the column
	 * @return The data type of the given column in the given schema
	 */
	def getColumnType (columnSpec: String, schema: StructType): DataType = {
		val fieldSpec = parseColumnSpec(columnSpec)
		val indices = fieldSpecToIndices(fieldSpec, schema)

		getType(indices, schema)
	}

	/**
	 * Calculate a function to convert between two types, as could be specified by a schema
	 * @param from The type from which to convert
	 * @param to The type to which to convert
	 * @return A function that will convert as stated.  Behavior of this function is undetermined when passed a value not of the proper input type.
	 */
	def calculateConverter (from: DataType, to: DataType): Any => Any = {
		val equals = (from == to)
		if (from == to) {
			(v: Any) => v
		} else if (StringType == to) {
			(v: Any) => v.toString
		} else if (conversions.contains((from, to))) {
			conversions((from, to))
		} else if (to.isInstanceOf[StructType] && from.isInstanceOf[StructType]) {
			val fromStruct = from.asInstanceOf[StructType]
			val toStruct = to.asInstanceOf[StructType]

			val commonFields = toStruct.fieldNames.filter(name => fromStruct.fieldNames.contains(name))
			val fromFields = fromStruct.fields.zipWithIndex
					.filter(fi => commonFields.contains(fi._1.name))
					.map(fi => (fi._1.name, fi)).toMap
			val toFields = toStruct.fields.zipWithIndex
				.filter(fi => commonFields.contains(fi._1.name))
				.map(fi => (fi._1.name, fi)).toMap
			val toSize = toStruct.fields.size

			val fieldConverters = commonFields.map(field =>
				{
					val iF = fromFields(field)._2
					val typeF = fromFields(field)._1.dataType

					val iT = toFields(field)._2
					val typeT = toFields(field)._1.dataType

					val converter = calculateConverter(typeF, typeT)

					(iT, (field, iF, converter))
				}
			).toMap

			input: Any => {
				val row = input.asInstanceOf[Row]
				val values = (1 to toSize).map(toIndex =>
					{
						if (fieldConverters.contains(toIndex)) {
							val (field, fromIndex, converter) = fieldConverters(toIndex)
							converter(row(fromIndex))
						} else {
							null
						}
					}
				)
				Row(values:_*)
			}
		} else if (to.isInstanceOf[ArrayType] && from.isInstanceOf[ArrayType]) {
			val fromArray = from.asInstanceOf[ArrayType]
			val fromType = fromArray.elementType
			val toArray = to.asInstanceOf[ArrayType]
			val toType = toArray.elementType

			val conversion = calculateConverter(fromType, toType)

			input: Any => input.asInstanceOf[ArrayBuffer[Any]].map(conversion)
		} else {
			throw new IllegalArgumentException("Cannot convert from " + from + " to " + to)
		}
	}

	/**
	 * Calculate a function to convert from a given existing data type to a concrete, but primitive type.
	 *
	 * @param from The Spark-SQL data type from which to convert
	 * @param to The class to which to convert.  This must be one of those types listed by _dataTypesOfClasses
	 * @return A function to convert from the from type into the to type.
	 */
	def calculatePrimitiveConverter[T] (from: DataType, to: Class[T]): Any => T = {
		val toType = _dataTypesOfClasses(to)
		input: Any => (calculateConverter(from, toType)(input)).asInstanceOf[T]
	}

	/**
	 * Take an existing DataFrame, and add a new column to it.
	 * @param base The existing DataFrame
	 * @param columnName The name of the column to add
	 * @param columnType the type of the column to add
	 * @param columnFcn A function mapping the values of the base data specified by inputColumns onto an output value,
	 *                  which had darn well better be of the right type.
	 * @param inputColumns The input columns needed to calculate the output column; their extracted values become the
	 *                     inputs to columnFcn
	 * @return A new DataFrame with the named added value.
	 */
	def addColumn (base: DataFrame, columnName: String, columnType: DataType,
	               columnFcn: Array[Any] => Any, inputColumns: String*): DataFrame = {
		val columns = inputColumns.map(new Column(_))
		val newColumn = inputColumns.length match {
			case  0 =>
				callUDF(() => columnFcn(Array[Any]()),
				        columnType)
			case  1 =>
				callUDF((a: Any) => columnFcn(Array[Any](a)),
				        columnType,
				        columns(0))
			case  2 =>
				callUDF((a: Any, b: Any) => columnFcn(Array[Any](a, b)),
				        columnType,
				        columns(0), columns(1))
			case  3 =>
				callUDF((a: Any, b: Any, c: Any) => columnFcn(Array[Any](a, b, c)),
				        columnType,
				        columns(0), columns(1), columns(2))
			case  4 =>
				callUDF((a: Any, b: Any, c: Any, d: Any) => columnFcn(Array[Any](a, b, c, d)),
				        columnType,
				        columns(0), columns(1), columns(2), columns(3))
			case  5 =>
				callUDF((a: Any, b: Any, c: Any, d: Any, e: Any) => columnFcn(Array[Any](a, b, c, d, e)),
				        columnType,
				        columns(0), columns(1), columns(2), columns(3), columns(4))
			case  6 =>
				callUDF((a: Any, b: Any, c: Any, d: Any, e: Any, f: Any) =>
					columnFcn(Array[Any](a, b, c, d, e, f)),
				        columnType,
				        columns(0), columns(1), columns(2), columns(3), columns(4),
				        columns(5))
			case  7 =>
				callUDF((a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any) =>
					columnFcn(Array[Any](a, b, c, d, e, f, g)),
				        columnType,
				        columns(0), columns(1), columns(2), columns(3), columns(4),
				        columns(5), columns(6))
			case  8 =>
				callUDF((a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any) =>
					columnFcn(Array[Any](a, b, c, d, e, f, g, h)),
				        columnType,
				        columns(0), columns(1), columns(2), columns(3), columns(4),
				        columns(5), columns(6), columns(7))
			case  9 =>
				callUDF((a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any) =>
					columnFcn(Array[Any](a, b, c, d, e, f, g, h, i)),
				        columnType,
				        columns(0), columns(1), columns(2), columns(3), columns(4),
				        columns(5), columns(6), columns(7), columns(8))
			case 10 =>
				callUDF((a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any, h: Any, i: Any, j: Any) =>
					columnFcn(Array[Any](a, b, c, d, e, f, g, h, i, j)),
				        columnType,
				        columns(0), columns(1), columns(2), columns(3), columns(4),
				        columns(5), columns(6), columns(7), columns(8), columns(9))
		}
		base.withColumn(columnName, newColumn)
	}


	/**
	 * Append a new column of primitives to the data.
	 * The data type of the column must be one of those types listed by _dataTypesOfClasses
	 * @param dataFrame The data to append to
	 * @param newColumn The column of primitives to append
	 * @param newColumnName The name of the new column
	 * @return The data frame with the new column added
	 */
	def zip[T: ClassTag] (dataFrame: DataFrame,
		                    newColumn: RDD[T],
		                    newColumnName: String): DataFrame = {
		// Append the new column to the data
		val dfWithNewColumn = dataFrame.rdd.zip(newColumn).map{ case (row, rddElement) =>
			Row((row.toSeq :+ rddElement):_*)
		}
		// Append the new column to the schema
		val sparkSqlDataType = _dataTypesOfClasses(scala.reflect.classTag[T].runtimeClass)
		val newSchema = structSchema((dataFrame.schema.fields.toSeq :+ schemaField(newColumnName, sparkSqlDataType)):_*)

		// Combine the data with the schema
		dataFrame.sqlContext.createDataFrame(dfWithNewColumn, newSchema)
	}

	/**
	 * Remove the specified columns from the data frame
	 * @param base The existing DataFrame
	 * @param columnNames The names of the columns to remove from the DataFrame
	 * @return A new DataFrame with the named added value.
	 */
	def removeColumns(base: DataFrame, columnNames: Set[String]): DataFrame = {

		val schemaFields = base.schema.fields
		var columnFilter: Seq[Column] = Seq()

		// Iterate the fields in the schema and construct a filter
		// containing only the fields that are NOT in the set of
		// field names to remove
		for (field <- schemaFields)
			if (!columnNames.contains(field.name))
					columnFilter = columnFilter :+ new Column(field.name)

		// Select only the columns specified in the filter
		base.select(columnFilter: _*)
	}

	private val conversions: MutableMap[(DataType, DataType), Any => Any] = MutableMap()
	private def addConverter (from: DataType, to: DataType, conversion: Any => Any): Unit = {
		conversions((from, to)) = conversion
	}
	addConverter(BooleanType, ByteType,    (b: Any) => if (b.asInstanceOf[Boolean]) 1.toByte else 0.toByte)
	addConverter(BooleanType, ShortType,   (b: Any) => if (b.asInstanceOf[Boolean]) 1.toShort else 0.toShort)
	addConverter(BooleanType, IntegerType, (b: Any) => if (b.asInstanceOf[Boolean]) 1 else 0)
	addConverter(BooleanType, LongType,    (b: Any) => if (b.asInstanceOf[Boolean]) 1L else 0L)
	addConverter(BooleanType, FloatType,   (b: Any) => if (b.asInstanceOf[Boolean]) 1.0f else 0.0f)
	addConverter(BooleanType, DoubleType,  (b: Any) => if (b.asInstanceOf[Boolean]) 1.0 else 0.0)
	addConverter(StringType,  BooleanType, (s: Any) => s.asInstanceOf[String].toBoolean)
	addConverter(StringType,  ByteType,    (s: Any) => s.asInstanceOf[String].toByte)
	addConverter(StringType,  ShortType,   (s: Any) => s.asInstanceOf[String].toShort)
	addConverter(StringType,  IntegerType, (s: Any) => s.asInstanceOf[String].toInt)
	addConverter(StringType,  LongType,    (s: Any) => s.asInstanceOf[String].toLong)
	addConverter(StringType,  FloatType,   (s: Any) => s.asInstanceOf[String].toFloat)
	addConverter(StringType,  DoubleType,  (s: Any) => s.asInstanceOf[String].toDouble)
	addConverter(ByteType,    BooleanType, (b: Any) => b.asInstanceOf[Byte] != 0.toByte)
	addConverter(ByteType,    ShortType,   (b: Any) => b.asInstanceOf[Byte].toShort)
	addConverter(ByteType,    IntegerType, (b: Any) => b.asInstanceOf[Byte].toInt)
	addConverter(ByteType,    LongType,    (b: Any) => b.asInstanceOf[Byte].toLong)
	addConverter(ByteType,    FloatType,   (b: Any) => b.asInstanceOf[Byte].toFloat)
	addConverter(ByteType,    DoubleType,  (b: Any) => b.asInstanceOf[Byte].toDouble)
	addConverter(ShortType,   BooleanType, (b: Any) => b.asInstanceOf[Short] != 0.toShort)
	addConverter(ShortType,   ByteType,    (b: Any) => b.asInstanceOf[Short].toByte)
	addConverter(ShortType,   IntegerType, (b: Any) => b.asInstanceOf[Short].toInt)
	addConverter(ShortType,   LongType,    (b: Any) => b.asInstanceOf[Short].toLong)
	addConverter(ShortType,   FloatType,   (b: Any) => b.asInstanceOf[Short].toFloat)
	addConverter(ShortType,   DoubleType,  (b: Any) => b.asInstanceOf[Short].toDouble)
	addConverter(IntegerType, BooleanType, (b: Any) => b.asInstanceOf[Int] != 0)
	addConverter(IntegerType, ByteType,    (b: Any) => b.asInstanceOf[Int].toByte)
	addConverter(IntegerType, ShortType,   (b: Any) => b.asInstanceOf[Int].toShort)
	addConverter(IntegerType, LongType,    (b: Any) => b.asInstanceOf[Int].toLong)
	addConverter(IntegerType, FloatType,   (b: Any) => b.asInstanceOf[Int].toFloat)
	addConverter(IntegerType, DoubleType,  (b: Any) => b.asInstanceOf[Int].toDouble)
	addConverter(LongType,    BooleanType, (b: Any) => b.asInstanceOf[Long] != 0L)
	addConverter(LongType,    ByteType,    (b: Any) => b.asInstanceOf[Long].toByte)
	addConverter(LongType,    ShortType,   (b: Any) => b.asInstanceOf[Long].toShort)
	addConverter(LongType,    IntegerType, (b: Any) => b.asInstanceOf[Long].toInt)
	addConverter(LongType,    FloatType,   (b: Any) => b.asInstanceOf[Long].toFloat)
	addConverter(LongType,    DoubleType,  (b: Any) => b.asInstanceOf[Long].toDouble)
	addConverter(FloatType,   BooleanType, (b: Any) => b.asInstanceOf[Float] != 0.toFloat)
	addConverter(FloatType,   ByteType,    (b: Any) => b.asInstanceOf[Float].toByte)
	addConverter(FloatType,   ShortType,   (b: Any) => b.asInstanceOf[Float].toShort)
	addConverter(FloatType,   IntegerType, (b: Any) => b.asInstanceOf[Float].toInt)
	addConverter(FloatType,   LongType,    (b: Any) => b.asInstanceOf[Float].toLong)
	addConverter(FloatType,   DoubleType,  (b: Any) => b.asInstanceOf[Float].toDouble)
	addConverter(DoubleType,  BooleanType, (b: Any) => b.asInstanceOf[Double] != 0.0)
	addConverter(DoubleType,  ByteType,    (b: Any) => b.asInstanceOf[Double].toByte)
	addConverter(DoubleType,  ShortType,   (b: Any) => b.asInstanceOf[Double].toShort)
	addConverter(DoubleType,  IntegerType, (b: Any) => b.asInstanceOf[Double].toInt)
	addConverter(DoubleType,  LongType,    (b: Any) => b.asInstanceOf[Double].toLong)
	addConverter(DoubleType,  FloatType,   (b: Any) => b.asInstanceOf[Double].toFloat)

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
