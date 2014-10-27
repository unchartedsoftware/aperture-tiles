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
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}
import org.apache.avro.file.CodecFactory
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.StringDoublePairArrayAvroSerializer
import com.oculusinfo.binning.util.Pair
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescriptionTileWrapper
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.CategoryValueAnalytic
import com.oculusinfo.tilegen.tiling.analytics.CategoryValueBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.CustomGlobalMetadata
import com.oculusinfo.tilegen.tiling.analytics.MinimumDoubleArrayTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.MaximumDoubleArrayTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMaxAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMinAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericSumAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMeanAnalytic
import com.oculusinfo.tilegen.tiling.analytics.StandardDoubleArrayBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.StandardStringScoreBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.SumDoubleArrayAnalytic
import com.oculusinfo.tilegen.util.PropertiesWrapper




trait ValueDescription[BT] {
	// Get a serializer that can write tiles of our type.
	def getSerializer: TileSerializer[BT]
}
object CSVValueExtractor {
	val standardFactories = Array[ValueExtractorFactory](
		new FieldValueExtractorFactory,
		new MeanFieldValueExtractorFactory,
		new StringValueExtractorFactory,
		new SubstringValueExtractorFactory,
		new IndirectSeriesValueExtractorFactory,
		new SeriesValueExtractorFactory
	)

	def fromProperties (properties: PropertiesWrapper,
	                    factories: Array[ValueExtractorFactory]): CSVValueExtractor[_, _] = {
		var field =
			properties.getStringOption("oculus.binning.valueField",
			                           "The single field to use for the value to tile. "
				                           +"This will override oculus.binning.valueFields, "
				                           +"if present.")
		val fields =
			properties.getStringOption("oculus.binning.valueFields",
			                           "Multiple fields to use for the values of a tile.")

		factories
			.find(_.handles(field, fields, properties))
			.getOrElse(new DefaultValueExtractorFactory)
			.constructValueExtractor(field.getOrElse(fields.getOrElse("")), properties)
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
	def calculateValue (fieldValues: Map[String, Any]): PT

	def getBinningAnalytic: BinningAnalytic[PT, BT]

	def getTileAnalytics: Seq[AnalysisDescription[TileData[BT], _]]

	def getDataAnalytics: Seq[AnalysisDescription[(_, PT), _]]
}



trait ValueExtractorFactory {
	def getFieldType (field: String, properties: PropertiesWrapper): String =
		properties.getString("oculus.binning.parsing."+field+".fieldType",
		                     "The type of the "+field+" field",
		                     Some(if ("constant" == field || "zero" == field) "constant"
		                          else "double")).toLowerCase

	def getPropertyType (field: String, properties: PropertiesWrapper): String =
		properties.getString("oculus.binning.parsing."+field+".propertyType",
		                     "The type of the "+field+" field",
		                     Some(if ("constant" == field || "zero" == field) "constant"
		                          else "")).toLowerCase

	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean
	def constructValueExtractor (field: String, properties: PropertiesWrapper): CSVValueExtractor[_, _]
}



class DefaultValueExtractorFactory extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean = true
	def constructValueExtractor (field: String, properties: PropertiesWrapper) =
		new CountValueExtractor
}

class CountValueExtractor extends CSVValueExtractor[Double, JavaDouble] {
	def name: String = "count"
	def description: String = "A count of relevant records"
	def fields: Array[String] = Array[String]()
	def calculateValue (fieldValues: Map[String, Any]): Double = 1.0
	def getSerializer: TileSerializer[JavaDouble] =
		new DoubleAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Double, JavaDouble] = new NumericSumAnalytic[Double, JavaDouble]()

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaDouble], _]] = {
		val convertFcn: JavaDouble => Double = bt => bt.asInstanceOf[Double]
		Seq(new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMinAnalytic[Double, JavaDouble]()),
		    new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMaxAnalytic[Double, JavaDouble]()))
	}


	def getDataAnalytics: Seq[AnalysisDescription[(_, Double), _]] =
		Seq[AnalysisDescription[(_, Double), _]]()
}



class FieldValueExtractorFactory  extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean =
		field match {
			case Some(fieldName) => {
				def matches (fieldType: String): Boolean =
					fieldType match {
						case "int" => true
						case "long" => true
						case "date" => true
						case "double" => true
						case "propertyMap" => matches(getPropertyType(fieldName, properties))
						case _ => false
					}
				matches(getFieldType(fieldName, properties))
			}
			case _ => false
		}

	def constructValueExtractor (field: String, properties: PropertiesWrapper) = {
		val fieldAggregation =
			properties.getString("oculus.binning.parsing." + field
				                     + ".fieldAggregation",
			                     "The way to aggregate the value field when binning",
			                     Some("add"))

		val binningAnalytic =
			if ("min" == fieldAggregation)
				new NumericMinAnalytic[Double, JavaDouble]()
			else if ("max" == fieldAggregation)
				new NumericMaxAnalytic[Double, JavaDouble]()
			else
				new NumericSumAnalytic[Double, JavaDouble]()

		new FieldValueExtractor(field, binningAnalytic);
	}
}

class FieldValueExtractor (fieldName: String,
                           binningAnalytic: BinningAnalytic[Double, JavaDouble])
		extends CSVValueExtractor[Double, JavaDouble]
{
	def name: String = fieldName
	def description: String = "The aggregate value of field "+fieldName
	def fields: Array[String] = Array(fieldName)
	def calculateValue (fieldValues: Map[String, Any]): Double =
		Try(fieldValues.get(fieldName).get.asInstanceOf[Double])
			.getOrElse(binningAnalytic.defaultUnprocessedValue)
	def getSerializer: TileSerializer[JavaDouble] =
		new DoubleAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Double, JavaDouble] = binningAnalytic

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaDouble], _]] = {
		val convertFcn: JavaDouble => Double = bt => bt.asInstanceOf[Double]
		Seq(new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMinAnalytic[Double, JavaDouble]()),
		    new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMaxAnalytic[Double, JavaDouble]()))
	}


	def getDataAnalytics: Seq[AnalysisDescription[(_, Double), _]] =
		Seq[AnalysisDescription[(_, Double), _]]()
}



class MeanFieldValueExtractorFactory extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean =
		field match {
			case Some(fieldName) => {
				def matches (fieldType: String): Boolean =
					fieldType match {
						case "mean" => true
						case "average" => true
						case _ => false
					}
				matches(getFieldType(fieldName, properties))
			}
			case _ => false
		}

	def constructValueExtractor (field: String, properties: PropertiesWrapper) = {
		val emptyValue = properties.getDoubleOption(
			"oculus.binning.parsing."+field+".emptyValue",
			"The value to use for bins where there aren't enough data points to give a "+
				"valid average")
		val minCount = properties.getIntOption(
			"oculus.binning.parsing."+field+".minCount",
			"The minimum number of data points allowed to have a valid mean for this field")
		new MeanValueExtractor(field, emptyValue, minCount)
	}
}

class MeanValueExtractor (fieldName: String, emptyValue: Option[Double], minCount: Option[Int])
		extends CSVValueExtractor[(Double, Int), JavaDouble]
{
	private val binningAnalytic =
		if (emptyValue.isDefined && minCount.isDefined) {
			new NumericMeanAnalytic[Double](emptyValue.get, minCount.get)
		} else if (emptyValue.isDefined) {
			new NumericMeanAnalytic[Double](emptyValue = emptyValue.get)
		} else if (minCount.isDefined) {
			new NumericMeanAnalytic[Double](minCount = minCount.get)
		} else {
			new NumericMeanAnalytic[Double]()
		}
	def name: String = fieldName
	def description: String = "The mean value of field "+fieldName
	def fields: Array[String] = Array(fieldName)
	def calculateValue (fieldValues: Map[String, Any]): (Double, Int) =
		Try((fieldValues.get(fieldName).get.asInstanceOf[Double], 1))
			.getOrElse(binningAnalytic.defaultUnprocessedValue)
	def getSerializer: TileSerializer[JavaDouble] =
		new DoubleAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[(Double, Int), JavaDouble] = binningAnalytic

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaDouble], _]] = {
		val convertFcn: JavaDouble => Double = bt => bt.asInstanceOf[Double]
		Seq(new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMinAnalytic[Double, JavaDouble]()),
		    new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMaxAnalytic[Double, JavaDouble]()))
	}


	def getDataAnalytics: Seq[AnalysisDescription[(_, (Double, Int)), _]] =
		Seq[AnalysisDescription[(_, (Double, Int)), _]]()
}


class StringValueExtractorFactory extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean =
		field.isDefined && "string" == getFieldType(field.get, properties)

	protected def getAggregationLimit (field: String, properties: PropertiesWrapper): Option[Int] =
		properties.getIntOption(
			"oculus.binning.parsing."+field+".limit.aggregation",
			"The maximum number of elements to keep internally when "+
				"calculating bins")

	protected def getBinLimit (field: String, properties: PropertiesWrapper): Option[Int] =
		properties.getIntOption(
			"oculus.binning.parsing."+field+".limit.bins",
			"The maximum number of entries to write to the tiles in a given bin")

	protected def getOrder (field: String, properties: PropertiesWrapper):
			Option[((String, Double), (String, Double)) => Boolean] =
		properties.getStringOption(
			"oculus.binning.parsing."+field+".order",
			"How to order elements.  Possible values are: \"alpha\" for "+
				"alphanumeric ordering of strings, \"reverse-alpha\" "+
				"similarly, \"high\" for ordering by score from high to "+
				"low, \"low\" for ordering by score from low to high, "+
				"and \"random\" or \"none\" for no ordering.") match {
			case Some("alpha") =>
				Some((a: (String, Double), b: (String, Double)) =>
					a._1.compareTo(b._1)>0
				)
			case Some("reverse-alpha") =>
				Some((a: (String, Double), b: (String, Double)) =>
					a._1.compareTo(b._1)>0
				)
			case Some("high") =>
				Some((a: (String, Double), b: (String, Double)) =>
					a._2 > b._2
				)
			case Some("low") =>
				Some((a: (String, Double), b: (String, Double)) =>
					a._2 < b._2
				)
			case _ => None
		}

	def constructValueExtractor (field: String, properties: PropertiesWrapper): CSVValueExtractor[_, _] = {
		val aggregationLimit = getAggregationLimit(field, properties)
		val binLimit = getBinLimit(field, properties)
		val order = getOrder(field, properties)

		val analytic = new StandardStringScoreBinningAnalytic(aggregationLimit, order, binLimit)

		new StringValueExtractor(field, analytic)
	}
}

class StringValueExtractor (fieldName: String,
                            binningAnalytic: BinningAnalytic[Map[String, Double], JavaList[Pair[String, JavaDouble]]])
		extends CSVValueExtractor[Map[String, Double], JavaList[Pair[String, JavaDouble]]]
{
	def name: String = fieldName
	def description: String = "The most common values of "+fieldName
	def fields: Array[String] = Array(fieldName)
	def calculateValue (fieldValues: Map[String, Any]): Map[String, Double] =
		Map(fieldValues.get(fieldName).toString -> 1.0)
	def getSerializer: TileSerializer[JavaList[Pair[String, JavaDouble]]] =
		new StringDoublePairArrayAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Map[String, Double], JavaList[Pair[String, JavaDouble]]] =
		binningAnalytic

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[Pair[String, JavaDouble]]], _]] =
		Seq[AnalysisDescription[TileData[JavaList[Pair[String, JavaDouble]]], _]]()

	def getDataAnalytics: Seq[AnalysisDescription[(_, Map[String, Double]), _]] =
		Seq[AnalysisDescription[(_, Map[String, Double]), _]]()
}



class SubstringValueExtractorFactory extends StringValueExtractorFactory {
	override def handles (field: Option[String], fields: Option[String],
	                      properties: PropertiesWrapper): Boolean =
		field.isDefined && "substring" == getFieldType(field.get, properties)

	override def constructValueExtractor (field: String, properties: PropertiesWrapper):
			CSVValueExtractor[_, _] =
	{
		val aggregationLimit = getAggregationLimit(field, properties)
		val binLimit = getBinLimit(field, properties)
		val order = getOrder(field, properties)

		val parseDelimiter = properties.getString(
			"oculus.binning.parsing."+field+".substring.delimiter",
			"The delimiter by which to split the field's value into "+
				"substrings.  This is a regular expression, not a straight "+
				"string.")
		val aggregateDelimiter = properties.getString(
			"oculus.binning.aggregation."+field+".substring.delimiter",
			"The delimiter with which to reassemble multiple substring "+
				"values when creating a bin value.  This is a straight "+
				"string, not a regular expression, hence its separation "+
				"from oculus.binning.parsing."+field+".substring.delimiter")
		val indexSpec = properties.getString(
			"oculus.binning.parsing."+field+".substring.entry",
			"The indices of the desired substring within the substrings of "+
				"the field value.  Negative values indicate place from the "+
				"right-hand-side, rather than the left.  Multiple indices "+
				"can be specified, using a comma as a separator, while ranges "+
				"can be specified using a colon separator.  Open-ended ranges "+
				"can be specified using the range separator with nothing on one side.")
		val indices: Seq[(Int, Int)] = indexSpec.split(",").map(_.trim).flatMap(indexRange =>
			{
				val separator = ":"
				if (indexRange.startsWith(separator)) {
					Seq[(Int, Int)]((0, indexRange.substring(1).trim.toInt))
				} else if (indexRange.endsWith(separator)) {
					Seq[(Int, Int)]((indexRange.substring(0, indexRange.length-1).trim.toInt, -1))
				} else if (indexRange.contains(separator)) {
					val extrema = indexRange.split(':')
					Seq[(Int, Int)]((extrema(0).toInt, extrema(1).toInt))
				} else if ("" == indexRange) {
					Seq[(Int, Int)]()
				} else {
					val value = indexRange.toInt
					Seq[(Int, Int)]((value, value))
				}
			}
		).toSeq


		val analytic = new StandardStringScoreBinningAnalytic(aggregationLimit, order, binLimit)

		new SubstringValueExtractor(field, parseDelimiter, aggregateDelimiter, indices, analytic)
	}
}

class SubstringValueExtractor (fieldName: String,
                               parsingDelimiter: String,
                               aggregationDelimiter: String,
                               indices: Seq[(Int, Int)],
                               binningAnalytic: BinningAnalytic[Map[String, Double], JavaList[Pair[String, JavaDouble]]])
		extends CSVValueExtractor[Map[String, Double], JavaList[Pair[String, JavaDouble]]]
{
	def name: String = fieldName
	def description: String = "The most common values of "+fieldName
	def fields: Array[String] = Array(fieldName)
	def calculateValue (fieldValues: Map[String, Any]): Map[String, Double] = {
		val value = fieldValues(fieldName).toString
		val subValues = value.split(parsingDelimiter)
		val len = subValues.length
		val entryIndices = indices.flatMap(extrema =>
			{
				val (start, end) = extrema
				val modStart = if (start < 0) len+start else start
				val modEnd = if (end < 0) len+end else end

				modStart to modEnd
			}
		).toSet
		val entry = Range(0, len)
			.filter(entryIndices.contains(_))
			.map(subValues(_))
			.mkString(aggregationDelimiter)
		Map(entry -> 1.0)
	}
	def getSerializer: TileSerializer[JavaList[Pair[String, JavaDouble]]] =
		new StringDoublePairArrayAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Map[String, Double], JavaList[Pair[String, JavaDouble]]] =
		binningAnalytic

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[Pair[String, JavaDouble]]], _]] =
		Seq[AnalysisDescription[TileData[JavaList[Pair[String, JavaDouble]]], _]]()

	def getDataAnalytics: Seq[AnalysisDescription[(_, Map[String, Double]), _]] =
		Seq[AnalysisDescription[(_, Map[String, Double]), _]]()
}



class MultiFieldValueExtractorFactory extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean =
		fields.isDefined

	def constructValueExtractor (fields: String, properties: PropertiesWrapper) = {
		val fieldNames = fields.split(",")
		new MultiFieldValueExtractor(fieldNames)
	}
}

class MultiFieldValueExtractor (fieldNames: Array[String])
		extends CSVValueExtractor[Seq[Double], JavaList[Pair[String, JavaDouble]]]
{
	def name: String = "field map: "+fieldNames.mkString(",")
	def description: String = "The aggregate value map of the fields "+fieldNames.mkString(",")
	def fields = fieldNames
	def calculateValue (fieldValues: Map[String, Any]): Seq[Double] =
		fieldNames.map(field => Try(fieldValues(field).asInstanceOf[Double]).getOrElse(0.0))
	def getSerializer =
		new StringDoublePairArrayAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Seq[Double], JavaList[Pair[String, JavaDouble]]] =
		new CategoryValueBinningAnalytic(fieldNames)

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[Pair[String, JavaDouble]]], _]] =
		Seq[AnalysisDescription[TileData[JavaList[Pair[String, JavaDouble]]], _]]()

	def getDataAnalytics: Seq[AnalysisDescription[(_, Seq[Double]), _]] =
		Seq[AnalysisDescription[(_, Seq[Double]), _]]()
}



class SeriesValueExtractorFactory extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean =
		fields.isDefined

	def constructValueExtractor (fields: String, properties: PropertiesWrapper) = {
		val fieldNames = fields.split(",")
		new SeriesValueExtractor(fieldNames)
	}
}

class SeriesValueExtractor (fieldNames: Array[String])
		extends CSVValueExtractor[Seq[Double], JavaList[JavaDouble]]
{
	def name: String = "series"
	def description: String =
		("The series of the fields "+
			 (if (fieldNames.size > 3) fieldNames.take(3).mkString("(", ",", ")...")
			  else fieldNames.mkString("(", ",", ")")))
	def fields = fieldNames
	def calculateValue (fieldValues: Map[String, Any]): Seq[Double] =
		fieldNames.map(field => Try(fieldValues(field).asInstanceOf[Double]).getOrElse(0.0))
	def getSerializer =
		new DoubleArrayAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Seq[Double], JavaList[JavaDouble]] =
		new SumDoubleArrayAnalytic with StandardDoubleArrayBinningAnalytic

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[JavaDouble]], _]] = {
		val convertFcn: JavaList[JavaDouble] => Seq[Double] = bt => {
			for (b <- bt.asScala) yield b.asInstanceOf[Double]
		}
		Seq(new AnalysisDescriptionTileWrapper(convertFcn, new MinimumDoubleArrayTileAnalytic),
		    new AnalysisDescriptionTileWrapper(convertFcn, new MaximumDoubleArrayTileAnalytic),
		    new CustomGlobalMetadata(Map[String, Object]("variables" -> fields.toSeq.asJava)))
	}

	def getDataAnalytics: Seq[AnalysisDescription[(_, Seq[Double]), _]] =
		Seq[AnalysisDescription[(_, Seq[Double]), _]]()
}



class IndirectSeriesValueExtractorFactory extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean = {
		fields.map(f =>
			{
				val fieldNames = f.split(",")
				(2 == fieldNames.length &&
					 "keyname" == getFieldType(fieldNames(0), properties))
			}
		).getOrElse(false)
	}

	def constructValueExtractor (fields: String, properties: PropertiesWrapper) = {
		val fieldNames = fields.split(",")
		val validKeys = properties.getSeqPropertyNames("oculus.binning.valueField.subFields")
		new IndirectSeriesValueExtractor(fieldNames(0),
		                                 fieldNames(1),
		                                 validKeys)
	}
}

class IndirectSeriesValueExtractor (keyField: String,
                                    valueField: String,
                                    validKeys: Seq[String])
		extends CSVValueExtractor[Seq[Double], JavaList[JavaDouble]]
{
	def name: String = "IndirectSeries"
	def description: String =
		("A series of values associated with certain keys, where key and "+
			 "value each come from distinct columns.  Relevant keys are "+
			 (if (validKeys.size > 3) validKeys.take(3).mkString("(", ",", ")...")
			  else validKeys.mkString("(", ",", ")")))
	def fields = Array(keyField, valueField)
	def calculateValue (fieldValues: Map[String, Any]): Seq[Double] =
		validKeys.map(key =>
			if (fieldValues.get(keyField).map(_ == key).getOrElse(false))
				if ("count" == valueField) 1.0 else {
					val fieldValue = fieldValues(valueField)
					if (fieldValue.isInstanceOf[Double]) fieldValue.asInstanceOf[Double]
					else fieldValue.toString.toDouble
				}
				else 0.0
		)
	def getSerializer =
		new DoubleArrayAvroSerializer(CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Seq[Double], JavaList[JavaDouble]] =
		new SumDoubleArrayAnalytic with StandardDoubleArrayBinningAnalytic

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[JavaDouble]], _]] = {
		val convertFcn: JavaList[JavaDouble] => Seq[Double] = bt => {
			for (b <- bt.asScala) yield b.asInstanceOf[Double]
		}
		Seq(new AnalysisDescriptionTileWrapper(convertFcn, new MinimumDoubleArrayTileAnalytic),
		    new AnalysisDescriptionTileWrapper(convertFcn, new MaximumDoubleArrayTileAnalytic))
	}

	def getDataAnalytics: Seq[AnalysisDescription[(_, Seq[Double]), _]] =
		Seq[AnalysisDescription[(_, Seq[Double]), _]]()
}
