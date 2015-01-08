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



import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Float => JavaFloat}
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}

import com.oculusinfo.factory.{ConfigurationProperty, UberFactory, ConfigurableFactory}
import com.oculusinfo.factory.properties._

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}
import org.apache.avro.file.CodecFactory
import com.oculusinfo.binning.{TilePyramid, TileData}
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.PairArrayAvroSerializer
import com.oculusinfo.binning.util.Pair
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescriptionTileWrapper
import com.oculusinfo.tilegen.tiling.analytics.ArrayBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.ArrayTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.CategoryValueAnalytic
import com.oculusinfo.tilegen.tiling.analytics.CategoryValueBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.CustomGlobalMetadata
import com.oculusinfo.tilegen.tiling.analytics.NumericMaxAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMaxBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMaxTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMinAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMinBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMinTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericSumAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericSumBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericSumTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMeanBinningAnalytic
import com.oculusinfo.tilegen.tiling.analytics.StringScoreBinningAnalytic
import com.oculusinfo.tilegen.util._
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer




/**
 * A class that encapsulates and describes extraction of values from schema RDDs, as well as the helper classes that
 * are needed to understand them
 *
 * @tparam PT The type of value to be extracted from records
 * @tparam BT The type of value into which the extracted values will be transformed when placed in tiles.
 */
abstract class ValueExtractor[PT: ClassTag, BT] extends Serializable {
	/** The name of the value type, used in default naming of tile pyramids. */
	def name: String

	/** The fields needed to calculate the value to be used for binning. */
	def fields: Seq[String]

	/**
	 * Convert a sequence of values, one for each of the fields listed by the fields method, into a processable
	 * value for binning
	 */
	def convert: Seq[Any] => PT

	/** The binning analytic needed to aggregate values, and transform them into their binnable form */
	def binningAnalytic: BinningAnalytic[PT, BT]

	/** The serializer needed to write tiles of the type described by this value extractor */
	def serializer: TileSerializer[BT]
}

/**
 * General constructors and properties for default value extractor factories
 */
object ValueExtractorFactory2 {
	private[datasets] val FIELD_PROPERTY =
		new StringProperty("field", "The field used by this value extractor", "")
	private[datasets] val FIELDS_PROPERTY =
		new ListProperty[String](new StringProperty("fields", "The fields used by this value extractor", ""),
		                         "fields", "The fields used by this value extractor")

	val defaultFactory = "count"

	/** Default function to use when creating child factories */
	def createChildren (parent: ConfigurableFactory[_], path: JavaList[String]):
	JavaList[ConfigurableFactory[_ <: ValueExtractor[_, _]]] =
		Seq[ConfigurableFactory[_ <: ValueExtractor[_, _]]](
			new CountValueExtractorFactory2(parent, path),
			new FieldValueExtractorFactory2(parent, path),
			new MeanValueExtractorFactory2(parent, path),
			new MultiFieldValueExtractorFactory2(parent, path),
			new SeriesValueExtractorFactory2(parent, path),
			new IndirectSeriesValueExtractorFactory2(parent, path),
			new StringValueExtractorFactory2(parent, path),
			new SubstringValueExtractorFactory2(parent, path)
		).asJava


	/** Create an un-named uber-factory for value extractors */
	def apply (parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultType: String = defaultFactory,
	           childProviders: (ConfigurableFactory[_],
			           JavaList[String]) => JavaList[ConfigurableFactory[_ <: ValueExtractor[_, _]]] = createChildren):
	ConfigurableFactory[ValueExtractor[_, _]] =
		new UberFactory[ValueExtractor[_, _]](classOf[ValueExtractor[_, _]], parent, path, true,
		                                      createChildren(parent, path), defaultType)

	/** Create a named uber-factory for value extractors */
	def named (name: String, parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultType: String = defaultFactory,
	           childProviders: (ConfigurableFactory[_],
			           JavaList[String]) => JavaList[ConfigurableFactory[_ <: ValueExtractor[_, _]]] = createChildren):
	ConfigurableFactory[ValueExtractor[_, _]] =
		new UberFactory[ValueExtractor[_, _]](name, classOf[ValueExtractor[_, _]], parent, path, true,
		                                      createChildren(parent, path), defaultType)
}
abstract class ValueExtractorFactory2 (name: String, parent: ConfigurableFactory[_], path: JavaList[String])
		extends NumericallyConfigurableFactory[ValueExtractor[_,_]](name, classOf[ValueExtractor[_,_]], parent, path)
{
	/** Get a property value, but only if it is present */
	def optionalGet[T] (property: ConfigurationProperty[T]): Option[T] = {
		if (hasPropertyValue(property)) {
			Some(getPropertyValue(property))
		} else {
			None
		}
	}
}

class CountValueExtractorFactory2 (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory2("count", parent, path)
{
	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		new CountValueExtractor2[T, JT]()(tag, numeric, conversion)
	}
}

class CountValueExtractor2[T: ClassTag, JT] ()(implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[T, JT] with Serializable
{
	def name = "count"
	def fields = Seq[String]()
	def convert = (s: Seq[Any]) => numeric.fromDouble(1.0)
	def binningAnalytic = new NumericSumBinningAnalytic[T, JT]()
	def serializer = new PrimitiveAvroSerializer(conversion.toClass, CodecFactory.bzip2Codec())
}

class FieldValueExtractorFactory2 (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory2("field", parent, path)
{
	addProperty(ValueExtractorFactory2.FIELD_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val field = getPropertyValue(ValueExtractorFactory2.FIELD_PROPERTY)
		new FieldValueExtractor2[T, JT](field)(tag, numeric, conversion)
	}
}

class FieldValueExtractor2[T: ClassTag, JT] (field: String)
                                            (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[T, JT] with Serializable {
	def name = field
	def fields = Seq(field)
	override def convert: (Seq[Any]) => T = s => s(0).asInstanceOf[T]
	override def binningAnalytic: BinningAnalytic[T, JT] = new NumericSumBinningAnalytic[T, JT]()
	override def serializer: TileSerializer[JT] =
		new PrimitiveAvroSerializer(conversion.toClass, CodecFactory.bzip2Codec())
}

object MeanValueExtractorFactory2 {
	private[datasets] val EMPTY_VALUE_PROPERTY =
		new DoubleProperty("empty", "The value to be used in bins without enough data for validity", 0.0)
	private[datasets] val MIN_COUNT_PROPERTY =
		new IntegerProperty("minCount", "The minimum number of records in a bin for the bin to be considered valid", 0)
}

class MeanValueExtractorFactory2 (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory2("mean", parent, path)
{
	addProperty(ValueExtractorFactory2.FIELD_PROPERTY)
	addProperty(MeanValueExtractorFactory2.EMPTY_VALUE_PROPERTY)
	addProperty(MeanValueExtractorFactory2.MIN_COUNT_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val field = getPropertyValue(ValueExtractorFactory2.FIELD_PROPERTY)
		val emptyValue = optionalGet(MeanValueExtractorFactory2.EMPTY_VALUE_PROPERTY)
		val minCount = optionalGet(MeanValueExtractorFactory2.MIN_COUNT_PROPERTY)

		new MeanValueExtractor2[T](field, emptyValue, minCount.map(_.intValue()))(numeric)
	}
}
class MeanValueExtractor2[T: ClassTag] (field: String, emptyValue: Option[JavaDouble], minCount: Option[Int])
                                       (implicit numeric: ExtendedNumeric[T])
		extends ValueExtractor[(T, Int), JavaDouble] with Serializable {
	def name = field
	def fields = Seq(field)
	override def convert: (Seq[Any]) => (T, Int) = s => (s(0).asInstanceOf[T], 1)
	override def binningAnalytic: BinningAnalytic[(T, Int), JavaDouble] = new NumericMeanBinningAnalytic[T]()
	override def serializer: TileSerializer[JavaDouble] =
		new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())
}

class SeriesValueExtractorFactory2 (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory2("series", parent, path)
{
	addProperty(ValueExtractorFactory2.FIELDS_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		def fields = getPropertyValue(ValueExtractorFactory2.FIELDS_PROPERTY).asScala.toArray
		new SeriesValueExtractor2[T, JT](fields)(numeric, conversion)
	}
}
class SeriesValueExtractor2[T: ClassTag, JT] (_fields: Array[String])
                                             (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Seq[T], JavaList[JT]] with Serializable {
	def name = "series"
	def fields = _fields
	override def convert: (Seq[Any]) => Seq[T] =
		s => s.map(v => Try(v.asInstanceOf[T]).getOrElse(numeric.fromInt(0)))
	override def binningAnalytic: BinningAnalytic[Seq[T], JavaList[JT]] =
		new ArrayBinningAnalytic[T, JT](new NumericSumBinningAnalytic())
	override def serializer: TileSerializer[JavaList[JT]] =
		new PrimitiveArrayAvroSerializer(conversion.toClass, CodecFactory.bzip2Codec())
}

object IndirectSeriesValueExtractor2 {
	val KEY_PROPERTY = new StringProperty("key",
	                                   "The field in which to find the key of a given record for the indirect series",
	                                   "")
	val VALUE_PROPERTY = new StringProperty("value",
	                                     "The field in which to find the value of a given record for the indirect series",
	                                     "")
	val VALID_KEYS_PROPERTY = new ListProperty(KEY_PROPERTY,
	                                           "validKeys",
	                                           "A list of the valid values that may be found in a records key property; all other values will be ignored.")
}
class IndirectSeriesValueExtractorFactory2 (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory2("indirectSeries", parent, path)
{
	addProperty(IndirectSeriesValueExtractor2.KEY_PROPERTY)
	addProperty(IndirectSeriesValueExtractor2.VALUE_PROPERTY)
	addProperty(IndirectSeriesValueExtractor2.VALID_KEYS_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		import IndirectSeriesValueExtractor2._
		def keyField = getPropertyValue(KEY_PROPERTY)
		def valueField = getPropertyValue(VALUE_PROPERTY)
		def validKeys = getPropertyValue(VALID_KEYS_PROPERTY).asScala.toArray
		new IndirectSeriesValueExtractor2[T, JT](keyField, valueField, validKeys)(numeric, conversion)
	}
}
class IndirectSeriesValueExtractor2[T: ClassTag, JT] (keyField: String, valueField: String, validKeys: Seq[String])
                                                     (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Seq[T], JavaList[JT]]
{
	def name = "indirectSeries"
	def fields = Seq(keyField, valueField)
	override def convert: (Seq[Any]) => Seq[T] =
		s => {
			val key = s(0).toString
			val value = s(1).asInstanceOf[T]
			validKeys.map(k => if (k == key) value else numeric.fromInt(0))
		}
	def binningAnalytic: BinningAnalytic[Seq[T], JavaList[JT]] =
		new ArrayBinningAnalytic[T, JT](new NumericSumBinningAnalytic())
	def serializer: TileSerializer[JavaList[JT]] =
		new PrimitiveArrayAvroSerializer(conversion.toClass, CodecFactory.bzip2Codec())
}

class MultiFieldValueExtractorFactory2 (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory2("fieldMap", parent, path)
{
	addProperty(ValueExtractorFactory2.FIELDS_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val fields = getPropertyValue(ValueExtractorFactory2.FIELDS_PROPERTY).asScala.toArray
		new MultiFieldValueExtractor2[T, JT](fields)(numeric, conversion)
	}
}
class MultiFieldValueExtractor2[T: ClassTag, JT] (_fields: Array[String])
                                                 (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Seq[T], JavaList[Pair[String, JT]]] with Serializable {
	def name = "fieldMap"
	def fields = _fields
	override def convert: (Seq[Any]) => Seq[T] =
		s => s.map(v => Try(v.asInstanceOf[T]).getOrElse(numeric.fromInt(0)))
	def binningAnalytic: BinningAnalytic[Seq[T], JavaList[Pair[String, JT]]] =
		new CategoryValueBinningAnalytic[T, JT](_fields, new NumericSumBinningAnalytic())
	def serializer: TileSerializer[JavaList[Pair[String, JT]]] =
		new PairArrayAvroSerializer(classOf[String], conversion.toClass, CodecFactory.bzip2Codec())
}

object StringScoreBinningAnalyticFactory2 {
	val AGGREGATION_LIMIT_PROPERTY = new IntegerProperty("aggregationLimit",
	                                                     "The maximum number of elements to keep internally when calculating bins",
	                                                     100)
	val BIN_LIMIT_PROPERTY = new IntegerProperty("binSize",
	                                             "The maximum number of entries to write to the tiles in a given bin",
	                                             10)
	val ORDER_PROPERTY = new StringProperty("ordering",
	                                        "How to order elements.  Possible values are: \"alpha\" for "+
			                                        "alphanumeric ordering of strings, \"reverse-alpha\" "+
			                                        "similarly, \"high\" for ordering by score from high to "+
			                                        "low, \"low\" for ordering by score from low to high, "+
			                                        "and \"random\" or \"none\" for no ordering.",
	                                        "high", Array("low", "high", "alpha", "reverse-alpha", "none"))

	protected def getOrder[T] (orderDescription: String)(implicit numeric: ExtendedNumeric[T]):
	Option[((String, T), (String, T)) => Boolean] =
		orderDescription match {
			case Some("alpha") =>
				Some((a: (String, T), b: (String, T)) =>
					     a._1.compareTo(b._1)>0
				)
			case Some("reverse-alpha") =>
				Some((a: (String, T), b: (String, T)) =>
					     a._1.compareTo(b._1)>0
				)
			case Some("high") =>
				Some((a: (String, T), b: (String, T)) =>
					     numeric.gt(a._2, b._2)
				)
			case Some("low") =>
				Some((a: (String, T), b: (String, T)) =>
					     numeric.lt(a._2, b._2)
				)
			case _ => None
		}
	def addProperties (factory: ValueExtractorFactory2): Unit = {
		factory.addProperty(AGGREGATION_LIMIT_PROPERTY)
		factory.addProperty(BIN_LIMIT_PROPERTY)
		factory.addProperty(ORDER_PROPERTY)
	}
	def getBinningAnalytic[T, JT] (factory: ValueExtractorFactory2)
	                              (implicit numeric: ExtendedNumeric[T],
	                               conversion: TypeConversion[T, JT]): BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]] = {
		val aggregationLimit = factory.optionalGet(AGGREGATION_LIMIT_PROPERTY).map(_.intValue())
		val binLimit = factory.optionalGet(BIN_LIMIT_PROPERTY).map(_.intValue())
		val ordering = getOrder(factory.getPropertyValue(ORDER_PROPERTY))
		new StringScoreBinningAnalytic[T, JT](new NumericSumBinningAnalytic(), aggregationLimit, ordering, binLimit)
	}
}
class StringValueExtractorFactory2 (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory2("string", parent, path)
{
	addProperty(ValueExtractorFactory2.FIELD_PROPERTY)
	StringScoreBinningAnalyticFactory2.addProperties(this)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val field = getPropertyValue(ValueExtractorFactory2.FIELD_PROPERTY)
		val binningAnalytic = StringScoreBinningAnalyticFactory2.getBinningAnalytic[T, JT](this)
		new StringValueExtractor2[T, JT](field, binningAnalytic)(numeric, conversion)
	}
}
class StringValueExtractor2[T: ClassTag, JT] (field: String,
                                              _binningAnalytic: BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]])
                                             (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Map[String, T], JavaList[Pair[String, JT]]]
{
	def name = field
	def fields = Seq(field)
	override def convert: (Seq[Any]) => Map[String, T] =
		s => Map(s(0).toString -> numeric.fromInt(1))
	def binningAnalytic: BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]] = _binningAnalytic
	def serializer: TileSerializer[JavaList[Pair[String, JT]]] =
		new PairArrayAvroSerializer(classOf[String], conversion.toClass, CodecFactory.bzip2Codec())
}

object SubstringValueExtractorFactory2 {
	val PARSING_DELIMITER_PROPERTY = new StringProperty("parsingDelimiter",
	                                                    "A delimiter to split the value of the field of interest into parts",
	                                                    ",")
	val AGGREGATION_DELIMITER_PROPERTY = new StringProperty("aggregationDelimiter",
	                                                        "A delimiter to use when recombining the relevant split values of the field of interest",
	                                                        ",")
	val INDICES_PROPERTY = new ListProperty(new PairProperty[JavaInt, JavaInt](
		new IntegerProperty("key", "Start index of relevant substrings", 0),
		new IntegerProperty("value", "End index of relevant substrings", 1),
		"bounds", "relevant substring bounds", new Pair(0, 1)),
	                                        "indices",
	                                        "The bounds of relevant substring groups, where groups are delimited by the parsing delimiter");
}
class SubstringValueExtractorFactory2 (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory2("string", parent, path)
{
	addProperty(ValueExtractorFactory2.FIELD_PROPERTY)
	addProperty(SubstringValueExtractorFactory2.PARSING_DELIMITER_PROPERTY)
	addProperty(SubstringValueExtractorFactory2.AGGREGATION_DELIMITER_PROPERTY)
	addProperty(SubstringValueExtractorFactory2.INDICES_PROPERTY)
	StringScoreBinningAnalyticFactory2.addProperties(this)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		import SubstringValueExtractorFactory2._
		val field = getPropertyValue(ValueExtractorFactory2.FIELD_PROPERTY)
		val parsingDelimiter = getPropertyValue(PARSING_DELIMITER_PROPERTY)
		val aggregationDelimiter = getPropertyValue(AGGREGATION_DELIMITER_PROPERTY)
		val indices = getPropertyValue(INDICES_PROPERTY).asScala.toSeq.map(p =>
			(p.getFirst.intValue, p.getSecond.intValue)
		)
		val binningAnalytic = StringScoreBinningAnalyticFactory2.getBinningAnalytic[T, JT](this)
		new SubstringValueExtractor2[T, JT](field, parsingDelimiter, aggregationDelimiter, indices, binningAnalytic)(numeric, conversion)
	}
}
class SubstringValueExtractor2[T: ClassTag, JT] (field: String,
                                                 parsingDelimiter: String,
                                                 aggregationDelimiter: String,
                                                 indices: Seq[(Int, Int)],
                                                 _binningAnalytic: BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]])
                                                (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Map[String, T], JavaList[Pair[String, JT]]]
{
	def name = field
	def fields = Seq(field)
	override def convert: (Seq[Any]) => Map[String, T] =
		s => {
			val value = s(0).toString
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
			Map(entry -> numeric.fromInt(1))

	}
	override def binningAnalytic: BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]] = _binningAnalytic
	override def serializer: TileSerializer[JavaList[Pair[String, JT]]] =
		new PairArrayAvroSerializer(classOf[String], conversion.toClass, CodecFactory.bzip2Codec())
}



// /////////////////////////////////////////////////////////////////////////////
// Everything below this line refers to the old, defunct CSV-based dataset
//
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
			.construct(field.getOrElse(fields.getOrElse("")), properties)
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

/**
 * A factory to construct a value extractor, with some general mixin functions 
 * we use a lot in value extractor factories
 */
trait ValueExtractorFactory {
	/** Get the stated type of a field specified by a set of properties */
	def getFieldType (field: String, properties: PropertiesWrapper): String =
		properties.getString("oculus.binning.parsing."+field+".fieldType",
		                     "The type of the "+field+" field",
		                     Some(if ("constant" == field || "zero" == field) "constant"
		                          else "double")).toLowerCase

	def getFieldAggregation (field: String, properties: PropertiesWrapper): String =
		properties.getString("oculus.binning.parsing." + field + ".fieldAggregation",
		                     "The way to aggregate the value field when binning",
		                     Some("add")).toLowerCase


	/** Get the stated sub-type of a property in a field specified by a set of properties */
	def getPropertyType (field: String, properties: PropertiesWrapper): String =
		properties.getString("oculus.binning.parsing."+field+".propertyType",
		                     "The type of the "+field+" field",
		                     Some(if ("constant" == field || "zero" == field) "constant"
		                          else "")).toLowerCase

	/** Get a standard codec factory from a set of properties */
	def getCodecFactory (properties: PropertiesWrapper): CodecFactory =
		properties.getString("oculus.binning.serialization.codecfactory",
		                     "The standard codec factory to use when serializing this "+
			                     "data set.  Possible values are null (no compression), "+
			                     "deflate, snappy, and bzip2. Deflate takes an extra "+
			                     "parameter of compressionLevel, specified by "+
			                     "oculus.binning.serialization.codecfactory.deflatelevel. "+
			                     "Only null and bzip2 support splitting of files by "+
			                     "HDFS, though bzip2 is slow.  Default is bzip2.",
		                     Some("bzip2")) match {
			case "null" => CodecFactory.nullCodec()
			case "deflate" => CodecFactory.deflateCodec(
				properties.getInt("oculus.binning.serialization.codecfactory.deflateLevel",
				                  "The level of deflation to be performed.  Values should be "+
					                  "between 1 and 9.  Default is 4.",
				                  Some(4))
			)
			case "snappy" => CodecFactory.snappyCodec()
			case _ => CodecFactory.bzip2Codec()
		}

	/**
	 * Indicates if this factory handles the case of the given field or fields 
	 * in the given property set
	 */
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean

	/**
	 * Actually construct the value extractor
	 */
	def construct (field: String, properties: PropertiesWrapper): CSVValueExtractor[_, _]
}



class DefaultValueExtractorFactory extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean = true
	def construct (field: String, properties: PropertiesWrapper) =
		new CountValueExtractor
}

class CountValueExtractor extends CSVValueExtractor[Double, JavaDouble] {
	def name: String = "count"
	def description: String = "A count of relevant records"
	def fields: Array[String] = Array[String]()
	def calculateValue (fieldValues: Map[String, Any]): Double = 1.0
	def getSerializer: TileSerializer[JavaDouble] =
		new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Double, JavaDouble] = new NumericSumBinningAnalytic[Double, JavaDouble]()

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaDouble], _]] = {
		val convertFcn: JavaDouble => Double = bt => bt.asInstanceOf[Double]
		Seq(new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMinTileAnalytic[Double]()),
		    new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMaxTileAnalytic[Double]()))
	}


	def getDataAnalytics: Seq[AnalysisDescription[(_, Double), _]] =
		Seq[AnalysisDescription[(_, Double), _]]()
}



class FieldValueExtractorFactory  extends ValueExtractorFactory {
	protected def checkTypeValidity (fieldName: String, properties: PropertiesWrapper): Boolean = {
		// We break the type match out as a separate function so we can
		// call it recursively in the case of a property map input
		def matches (fieldType: String): Boolean =
			fieldType match {
				case "int" => true
				case "long" => true
				case "date" => true
				case "float" => true
				case "double" => true
				case "propertyMap" =>
					matches(getPropertyType(fieldName, properties))
				case _ => false
			}
		matches(getFieldType(fieldName, properties))
	}

	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean =
		field match {
			case Some(fieldName) => {
				// We match field aggregation of min, max, or add, and the
				// above field types
				(getFieldAggregation(fieldName, properties) match {
					 case "add" => true
					 case "min" => true
					 case "minimum" => true
					 case "max" => true
					 case "maximum" => true
					 case _ => false
				 }) && checkTypeValidity(fieldName, properties)
			}
			case _ => false
		}

	def construct (field: String, properties: PropertiesWrapper): CSVValueExtractor[_, _] = {
		val fieldAggregation = getFieldAggregation(field, properties)
		val fieldType = getFieldType(field, properties)
		val codecFactory = getCodecFactory(properties)

		def constructBinningAnalytic[T, JT] ()(implicit numeric: ExtendedNumeric[T],
		                                       converter: TypeConversion[T, JT]) =
			if ("min" == fieldAggregation || "minimum" == fieldAggregation)
				new NumericMinBinningAnalytic[T, JT]
			else if ("max" == fieldAggregation || "maximum" == fieldAggregation)
				new NumericMaxBinningAnalytic[T, JT]
			else
				new NumericSumBinningAnalytic[T, JT]

		fieldType match {
			case "int" =>
				new FieldValueExtractor[Int, JavaInt](
					field,
					constructBinningAnalytic[Int, JavaInt](),
					new PrimitiveAvroSerializer(classOf[JavaInt], codecFactory))
			case "long" =>
				new FieldValueExtractor[Long, JavaLong](
					field,
					constructBinningAnalytic[Long, JavaLong](),
					new PrimitiveAvroSerializer(classOf[JavaLong], codecFactory))
			case "float" =>
				new FieldValueExtractor[Float, JavaFloat](
					field,
					constructBinningAnalytic[Float, JavaFloat](),
					new PrimitiveAvroSerializer(classOf[JavaFloat], codecFactory))
			case "double" =>
				new FieldValueExtractor[Double, JavaDouble](
					field,
					constructBinningAnalytic[Double, JavaDouble](),
					new PrimitiveAvroSerializer(classOf[JavaDouble], codecFactory))
		}
	}
}

class FieldValueExtractor[T: ClassTag, JT] (
	fieldName: String, binningAnalytic: BinningAnalytic[T, JT], serializer: TileSerializer[JT])(
	implicit numeric: ExtendedNumeric[T], converter: TypeConversion[T, JT])
		extends CSVValueExtractor[T, JT]
{
	def name: String = fieldName
	def description: String = "The aggregate value of field "+fieldName
	def fields: Array[String] = Array(fieldName)
	def calculateValue (fieldValues: Map[String, Any]): T =
		Try(fieldValues.get(fieldName).get.asInstanceOf[T])
			.getOrElse(binningAnalytic.defaultUnprocessedValue)
	def getSerializer: TileSerializer[JT] = serializer
	def getBinningAnalytic: BinningAnalytic[T, JT] = binningAnalytic

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JT], _]] = {
		val convertFcn: JT => T = bt => converter.backwards(bt)
		Seq(new AnalysisDescriptionTileWrapper[JT, T](convertFcn,
		                                              new NumericMinTileAnalytic[T]()),
		    new AnalysisDescriptionTileWrapper[JT, T](convertFcn,
		                                              new NumericMaxTileAnalytic[T]()))
	}


	def getDataAnalytics: Seq[AnalysisDescription[(_, T), _]] =
		Seq[AnalysisDescription[(_, T), _]]()
}



class MeanFieldValueExtractorFactory extends  FieldValueExtractorFactory {
	override def handles (field: Option[String], fields: Option[String],
	                      properties: PropertiesWrapper): Boolean =
		field match {
			case Some(fieldName) => {
				(getFieldAggregation(fieldName, properties) match {
					 case "mean" => true
					 case "average" => true
					 case _ => false
				 }) && checkTypeValidity(fieldName, properties)
			}
			case _ => false
		}

	override def construct (field: String, properties: PropertiesWrapper): CSVValueExtractor[_, _] = {
		val fieldType = getFieldType(field, properties)
		val emptyValue = properties.getDoubleOption(
			"oculus.binning.parsing."+field+".emptyValue",
			"The value to use for bins where there aren't enough data points to give a "+
				"valid average").map(Double.box(_))
		val minCount = properties.getIntOption(
			"oculus.binning.parsing."+field+".minCount",
			"The minimum number of data points allowed to have a valid mean for this field")

		fieldType match {
			case "int" => new MeanValueExtractor[Int](field, emptyValue, minCount)
			case "long" => new MeanValueExtractor[Long](field, emptyValue, minCount)
			case "float" => new MeanValueExtractor[Float](field, emptyValue, minCount)
			// Default is Double
			case _ => new MeanValueExtractor[Double](field, emptyValue, minCount)
		}
	}
}

class MeanValueExtractor[T] (
	fieldName: String, emptyValue: Option[JavaDouble], minCount: Option[Int])(
	implicit numeric: ExtendedNumeric[T])
		extends CSVValueExtractor[(T, Int), JavaDouble]
{
	private val binningAnalytic =
		if (emptyValue.isDefined && minCount.isDefined) {
			new NumericMeanBinningAnalytic[T](emptyValue.get, minCount.get)
		} else if (emptyValue.isDefined) {
			new NumericMeanBinningAnalytic[T](emptyValue = emptyValue.get)
		} else if (minCount.isDefined) {
			new NumericMeanBinningAnalytic[T](minCount = minCount.get)
		} else {
			new NumericMeanBinningAnalytic[T]()
		}
	def name: String = fieldName
	def description: String = "The mean value of field "+fieldName
	def fields: Array[String] = Array(fieldName)
	def calculateValue (fieldValues: Map[String, Any]): (T, Int) =
		Try((fieldValues.get(fieldName).get.asInstanceOf[T], 1))
			.getOrElse(binningAnalytic.defaultUnprocessedValue)
	def getSerializer: TileSerializer[JavaDouble] =
		new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[(T, Int), JavaDouble] = binningAnalytic

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaDouble], _]] = {
		val convertFcn: JavaDouble => Double = bt => bt.asInstanceOf[Double]
		Seq(new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMinTileAnalytic[Double]()),
		    new AnalysisDescriptionTileWrapper[JavaDouble, Double](convertFcn,
		                                                           new NumericMaxTileAnalytic[Double]()))
	}


	def getDataAnalytics: Seq[AnalysisDescription[(_, (T, Int)), _]] =
		Seq[AnalysisDescription[(_, (T, Int)), _]]()
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

	def construct (field: String, properties: PropertiesWrapper): CSVValueExtractor[_, _] = {
		val aggregationLimit = getAggregationLimit(field, properties)
		val binLimit = getBinLimit(field, properties)
		val order = getOrder(field, properties)

		val analytic = new StringScoreBinningAnalytic[Double, JavaDouble](new NumericSumBinningAnalytic(), aggregationLimit, order, binLimit)

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
		new PairArrayAvroSerializer(classOf[String], classOf[JavaDouble], CodecFactory.bzip2Codec())
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

	override def construct (field: String, properties: PropertiesWrapper):
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


		val analytic = new StringScoreBinningAnalytic[Double, JavaDouble](new NumericSumBinningAnalytic(), aggregationLimit, order, binLimit)

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
		new PairArrayAvroSerializer(classOf[String], classOf[JavaDouble], CodecFactory.bzip2Codec())
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

	def construct (fields: String, properties: PropertiesWrapper) = {
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
		new PairArrayAvroSerializer(classOf[String], classOf[JavaDouble], CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Seq[Double], JavaList[Pair[String, JavaDouble]]] =
		new CategoryValueBinningAnalytic[Double, JavaDouble](fieldNames, new NumericSumBinningAnalytic())

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[Pair[String, JavaDouble]]], _]] =
		Seq[AnalysisDescription[TileData[JavaList[Pair[String, JavaDouble]]], _]]()

	def getDataAnalytics: Seq[AnalysisDescription[(_, Seq[Double]), _]] =
		Seq[AnalysisDescription[(_, Seq[Double]), _]]()
}



class SeriesValueExtractorFactory extends ValueExtractorFactory {
	def handles (field: Option[String], fields: Option[String],
	             properties: PropertiesWrapper): Boolean =
		fields.isDefined

	def construct (fields: String, properties: PropertiesWrapper) = {
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
		new PrimitiveArrayAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Seq[Double], JavaList[JavaDouble]] =
		new ArrayBinningAnalytic[Double, JavaDouble](new NumericSumBinningAnalytic())

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[JavaDouble]], _]] = {
		val convertFcn: JavaList[JavaDouble] => Seq[Double] = bt => {
			for (b <- bt.asScala) yield b.asInstanceOf[Double]
		}
		Seq(new AnalysisDescriptionTileWrapper(convertFcn,
		                                       new ArrayTileAnalytic[Double](new NumericMinTileAnalytic())),
		    new AnalysisDescriptionTileWrapper(convertFcn,
		                                       new ArrayTileAnalytic[Double](new NumericMaxTileAnalytic())),
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

	def construct (fields: String, properties: PropertiesWrapper) = {
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
		new PrimitiveArrayAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())
	def getBinningAnalytic: BinningAnalytic[Seq[Double], JavaList[JavaDouble]] =
		new ArrayBinningAnalytic[Double, JavaDouble](new NumericSumBinningAnalytic())

	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[JavaDouble]], _]] = {
		val convertFcn: JavaList[JavaDouble] => Seq[Double] = bt => {
			for (b <- bt.asScala) yield b.asInstanceOf[Double]
		}
		Seq(new AnalysisDescriptionTileWrapper(convertFcn,
		                                       new ArrayTileAnalytic[Double](new NumericMinTileAnalytic())),
		    new AnalysisDescriptionTileWrapper(convertFcn,
		                                       new ArrayTileAnalytic[Double](new NumericMaxTileAnalytic())))
	}

	def getDataAnalytics: Seq[AnalysisDescription[(_, Seq[Double]), _]] =
		Seq[AnalysisDescription[(_, Seq[Double]), _]]()
}
