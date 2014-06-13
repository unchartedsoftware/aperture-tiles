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



import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}

import scala.reflect.ClassTag

import org.apache.avro.file.CodecFactory;

import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.StringDoublePairArrayAvroSerializer
import com.oculusinfo.binning.util.Pair

import com.oculusinfo.tilegen.tiling.BinningAnalytic
import com.oculusinfo.tilegen.tiling.CategoryValueAnalytic
import com.oculusinfo.tilegen.tiling.CategoryValueBinningAnalytic
import com.oculusinfo.tilegen.tiling.StandardDoubleBinningAnalytic
import com.oculusinfo.tilegen.tiling.SumLogDoubleAnalytic
import com.oculusinfo.tilegen.tiling.MinimumDoubleAnalytic
import com.oculusinfo.tilegen.tiling.MaximumDoubleAnalytic
import com.oculusinfo.tilegen.tiling.SumDoubleAnalytic
import com.oculusinfo.tilegen.tiling.SumDoubleArrayAnalytic
import com.oculusinfo.tilegen.tiling.StandardDoubleArrayBinningAnalytic
import com.oculusinfo.tilegen.util.PropertiesWrapper




trait ValueDescription[BT] {
	// Get a serializer that can write tiles of our type.
	def getSerializer: TileSerializer[BT]
}
object CSVValueExtractor {
	def fromProperties (properties: PropertiesWrapper): CSVValueExtractor[_, _] = {
		var field =
			properties.getStringOption("oculus.binning.valueField",
			                           "The single field to use for the value to tile. "
				                           +"This will override oculus.binning.valueFields, "
				                           +"if present.")
		val fields =
			properties.getStringOption("oculus.binning.valueFields",
			                           "Multiple fields to use for the values of a tile.")

		if (field.isEmpty && fields.isDefined) {
			// No single field defined, but multiple ones - use the mutiples.
			val fieldNames = fields.get.split(",")
			new MultiFieldValueExtractor(fieldNames)
		} else {
			// Single field; figure out what type.
			field match {
				case None | Some("count") => {
					new CountValueExtractor
				}
				case _ => {
					val fieldAggregation =
						properties.getString("oculus.binning.parsing." + field.get
							                     + ".fieldAggregation",
						                     "The way to aggregate the value field when binning",
						                     Some("add"))

					val binningAnalytic = if ("log" == fieldAggregation) {
						val base =
							properties.getDouble("oculus.binning.parsing." + field.get
								                     + ".fieldBase",
							                     "The base to use when taking value the "+
								                     "logarithm of values.  Default is e.",
							                     Some(math.exp(1.0)))
						new SumLogDoubleAnalytic(base) with StandardDoubleBinningAnalytic
					} else if ("min" == fieldAggregation)
						new MinimumDoubleAnalytic with StandardDoubleBinningAnalytic
					else if ("max" == fieldAggregation)
						new MaximumDoubleAnalytic with StandardDoubleBinningAnalytic
					else
						new SumDoubleAnalytic with StandardDoubleBinningAnalytic

					new FieldValueExtractor(field.get, binningAnalytic);
				}
			}
		}
	}
}
abstract class CSVValueExtractor[PT: ClassTag, BT]
		extends ValueDescription[BT]
		with Serializable
{
	val valueTypeTag = implicitly[ClassTag[PT]]

	// The name of the value type- usually refering to the fields it uses - for
	// use in table naming.
	def name: String

	// A description of the value
	def description: String

	// The fields this extractor needs
	def fields: Array[String]

	// Get the value from the field values
	def calculateValue (fieldValues: Map[String, Double]): PT

	def getBinningAnalytic: BinningAnalytic[PT, BT]
}

class CountValueExtractor extends CSVValueExtractor[Double, JavaDouble] {
	def name: String = "count"
	def description: String = "A count of relevant records"
	def fields: Array[String] = Array[String]()
	def calculateValue (fieldValues: Map[String, Double]): Double = 1.0
	def getSerializer: TileSerializer[JavaDouble] =
		new DoubleAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Double, JavaDouble] =
		new SumDoubleAnalytic with StandardDoubleBinningAnalytic
}

class FieldValueExtractor (fieldName: String,
                           binningAnalytic: BinningAnalytic[Double, JavaDouble])
		extends CSVValueExtractor[Double, JavaDouble] {
	def name: String = fieldName
	def description: String = "The aggregate value of field "+fieldName
	def fields: Array[String] = Array(fieldName)
	def calculateValue (fieldValues: Map[String, Double]): Double =
		fieldValues.get(fieldName).getOrElse(binningAnalytic.defaultUnprocessedValue)
	def getSerializer: TileSerializer[JavaDouble] =
		new DoubleAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Double, JavaDouble] = binningAnalytic
}

class MultiFieldValueExtractor (fieldNames: Array[String])
		extends CSVValueExtractor[Seq[Double], JavaList[Pair[String, JavaDouble]]]
{
	def name: String = "field map: "+fieldNames.mkString(",")
	def description: String = "The aggregate value map of the fields "+fieldNames.mkString(",")
	def fields = fieldNames
	def calculateValue (fieldValues: Map[String, Double]): Seq[Double] =
		fieldNames.map(field => fieldValues(field))
	def getSerializer =
		new StringDoublePairArrayAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Seq[Double], JavaList[Pair[String, JavaDouble]]] =
		new CategoryValueBinningAnalytic(fieldNames)
}

class SeriesValueExtractor (fieldNames: Array[String])
		extends CSVValueExtractor[Seq[Double], JavaList[JavaDouble]]
{
	def name: String = "series: "+fieldNames.mkString(",")
	def description: String = "The series of the fields "+fieldNames.mkString(",")
	def fields = fieldNames
	def calculateValue (fieldValues: Map[String, Double]): Seq[Double] =
		fieldNames.map(field => fieldValues(field))
	def getSerializer =
		new DoubleArrayAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Seq[Double], JavaList[JavaDouble]] =
		new SumDoubleArrayAnalytic with StandardDoubleArrayBinningAnalytic
}
