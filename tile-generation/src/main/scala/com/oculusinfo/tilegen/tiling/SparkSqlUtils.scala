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
package com.oculusinfo.tilegen.tiling

import org.apache.spark.sql.{SchemaRDD, SQLContext}
import org.apache.spark.sql.catalyst.analysis.{Star, UnresolvedAttribute}
import org.apache.spark.sql.catalyst.expressions.{Expression, GenericRow, Row}
import org.apache.spark.sql.catalyst.types.{DataType, StructField, StructType}

/**
 * Utility functions for Spark SQL
 */
object SparkSqlUtils {

  /**
   * Converts a colspec into a spark sql expression.
   */
  def colSpecToExpression(sqlc: SQLContext, name: String) = {
    import sqlc._

    // Get an index from a col spec
    def getIndex (name: String): (String, Int) = {
      if (name.endsWith ("]") && name.contains ("[") ) {
        val start = name.indexOf ("[")
        val base = name.substring (0, start)
        val index = name.substring (start + 1, name.length - 1).toInt
        (base, index)
      } else {
        (name, - 1)
      }
    }

    val parts = name.split ("\\.")
    val primaryParts = getIndex (parts.take (1) (0) )
    val primary: Expression =
      if (- 1 == primaryParts._2) UnresolvedAttribute (primaryParts._1)
      else Symbol (primaryParts._1).getItem (primaryParts._2)
    parts.drop (1).foldLeft (primary) ((p, n) => {
      val nParts = getIndex (n)
      if (- 1 == nParts._2) p.getField (nParts._1)
      else p.getField (nParts._1).getItem (nParts._2)
    })
  }

  /**
   * Extends a schema with a new variable, and itializes that variables value for each
   * row using a caller supplied function.
   */
  def addVariable (sqlc: SQLContext, base: SchemaRDD, variables: Array[String],
                   fcn: Seq[Any] => Any, name: String, dataType: DataType): SchemaRDD = {
    val baseSchema = base.schema
    val baseLen = baseSchema.fields.size
    val selectFields = Star (None) +: variables.map(colSpecToExpression(sqlc, _))

    val newData = base.select (selectFields: _*) map { row =>
      val originalRow = row.take (baseLen)
      val parameters = row.drop (baseLen)
      val newVal = fcn (parameters)
      new GenericRow ((originalRow :+ newVal).toArray).asInstanceOf[Row]
    }

    val newSchema = new StructType (baseSchema.fields :+ StructField (name, dataType, true) )
    sqlc.applySchema (newData, newSchema)
  }

}
