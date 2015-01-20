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

	def getTileAnalytics: Seq[AnalysisDescription[TileData[BT], _]]
}

/**
 * General constructors and properties for default value extractor factories
 */
object ValueExtractorFactory {
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
			new CountValueExtractorFactory(parent, path),
			new FieldValueExtractorFactory(parent, path),
			new MultiFieldValueExtractorFactory(parent, path),
			new SeriesValueExtractorFactory(parent, path),
			new IndirectSeriesValueExtractorFactory(parent, path),
			new StringValueExtractorFactory(parent, path),
			new SubstringValueExtractorFactory(parent, path)
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

/**
 * Root class for most specific value extractor factories, this mostly just mixes in the necessary helper traits.
 * Non-numeric value extractors may want to inherit from ConfigurableFactory directly.
 *
 * Constructor arguments are pass-throughs to the super-class
 *
 * @param name The name of this specific value extractor factory; this will be what must be specified as the value
 *             type in configuration files.
 */
abstract class ValueExtractorFactory (name: String, parent: ConfigurableFactory[_], path: JavaList[String])
		extends NumericallyConfigurableFactory[ValueExtractor[_,_]](name, classOf[ValueExtractor[_,_]], parent, path)
		with OptionsFactoryMixin[ValueExtractor[_, _]]
{
}

/**
 * A constructor for CountValueExtractor2 value extractors.  All arguments are pass-throughs to
 * @see CountValueExtractor2
 */
class CountValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory("count", parent, path)
{
	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		new CountValueExtractor[T, JT]()(tag, numeric, conversion)
	}
}

/**
 * A value extractor that just uses a record count as the record value (so each record has a value of '1').
 * @tparam T The numeric type to use for the count when processing
 * @tparam JT The numeric type to use for the count when writing tiles (generally a Java version of T)
 */
class CountValueExtractor[T: ClassTag, JT] ()(implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[T, JT] with Serializable
{
	def name = "count"
	def fields = Seq[String]()
	def convert = (s: Seq[Any]) => numeric.fromDouble(1.0)
	def binningAnalytic = new NumericSumBinningAnalytic[T, JT]()
	def getTileAnalytics: Seq[AnalysisDescription[TileData[JT], _]] = {
		Seq(new AnalysisDescriptionTileWrapper[JT, T](conversion.backwards(_), new NumericMinTileAnalytic[T]()),
		    new AnalysisDescriptionTileWrapper[JT, T](conversion.backwards(_), new NumericMaxTileAnalytic[T]()))
	}
	def serializer = new PrimitiveAvroSerializer(conversion.toClass, CodecFactory.bzip2Codec())
}

object FieldValueExtractorFactory {
	private[datasets] val FIELD_AGGREGATION_PROPERTY = new StringProperty("aggregation",
	                                                                      "The way to aggregate the value field when binning",
	                                                                      "add")
	private[datasets] val EMPTY_VALUE_PROPERTY =
		new DoubleProperty("empty", "The value to be used in bins without enough data for validity", 0.0)
	private[datasets] val MIN_COUNT_PROPERTY =
		new IntegerProperty("minCount", "The minimum number of records in a bin for the bin to be considered valid", 0)
}
/**
 * A constructor for FieldValueExtractor2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see FieldValueExtractor2
 */
class FieldValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory("field", parent, path)
{
	import FieldValueExtractorFactory._

	addProperty(ValueExtractorFactory.FIELD_PROPERTY)
	addProperty(FIELD_AGGREGATION_PROPERTY)
	addProperty(EMPTY_VALUE_PROPERTY)
	addProperty(MIN_COUNT_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val field = getPropertyValue(ValueExtractorFactory.FIELD_PROPERTY)
		val analyticType = getPropertyValue(FieldValueExtractorFactory.FIELD_AGGREGATION_PROPERTY).toLowerCase()

		def createFieldExtractor (analytic: BinningAnalytic[T, JT]): ValueExtractor[_, _] =
			new FieldValueExtractor[T, JT](field, analytic)(tag, numeric, conversion)

		def createMeanExtractor (): ValueExtractor[_, _] = {
			val emptyValue = optionalGet(EMPTY_VALUE_PROPERTY)
			val minCount = optionalGet(MIN_COUNT_PROPERTY).map(_.intValue())

			new MeanValueExtractor[T](field, emptyValue, minCount)(tag, numeric)
		}

		analyticType match {
			case "min" =>     createFieldExtractor(new NumericMinBinningAnalytic[T, JT]()(numeric, conversion))
			case "minimum" => createFieldExtractor(new NumericMinBinningAnalytic[T, JT]()(numeric, conversion))
			case "max" =>     createFieldExtractor(new NumericMaxBinningAnalytic[T, JT]()(numeric, conversion))
			case "maximum" => createFieldExtractor(new NumericMaxBinningAnalytic[T, JT]()(numeric, conversion))
			case "mean" =>    createMeanExtractor()
			case "average" => createMeanExtractor()
			case _ =>         createFieldExtractor(new NumericSumBinningAnalytic[T, JT]()(numeric, conversion))
		}
	}
}

/**
 * A value extractor that uses the sum of the (numeric) value of a single field as the value of each record.
 * @param field The field whose value is used as the record's value
 * @param _binningAnalytic The analytic used to aggregate the field value
 * @tparam T The numeric type expected for the field in question
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class FieldValueExtractor[T: ClassTag, JT] (field: String, _binningAnalytic: BinningAnalytic[T, JT])
                          (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[T, JT] with Serializable {
	def name = field
	def fields = Seq(field)
	override def convert: (Seq[Any]) => T = s => s(0).asInstanceOf[T]
	override def binningAnalytic: BinningAnalytic[T, JT] = _binningAnalytic
	def getTileAnalytics: Seq[AnalysisDescription[TileData[JT], _]] = {
		Seq(new AnalysisDescriptionTileWrapper[JT, T](conversion.backwards(_), new NumericMinTileAnalytic[T]()),
		    new AnalysisDescriptionTileWrapper[JT, T](conversion.backwards(_), new NumericMaxTileAnalytic[T]()))
	}
	override def serializer: TileSerializer[JT] =
		new PrimitiveAvroSerializer(conversion.toClass, CodecFactory.bzip2Codec())
}

/**
 * A value extractor that uses the mean of the (numeric) value of a single field as the value of each record.
 * @param field The field whose value is used as the record's value
 * @param emptyValue The value to use for bins without enough valid data
 * @param minCount The minimum number of records allowed in a bin before it is considered valid.
 * @tparam T The numeric type expected for the field in question.  Bins are always written as Java Doubles
 */
class MeanValueExtractor[T: ClassTag] (field: String, emptyValue: Option[JavaDouble], minCount: Option[Int])
                         (implicit numeric: ExtendedNumeric[T])
		extends ValueExtractor[(T, Int), JavaDouble] with Serializable {
	def name = field
	def fields = Seq(field)
	override def convert: (Seq[Any]) => (T, Int) = s => (s(0).asInstanceOf[T], 1)
	override def binningAnalytic: BinningAnalytic[(T, Int), JavaDouble] = new NumericMeanBinningAnalytic[T]()
	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaDouble], _]] = {
		val convertFcn: JavaDouble => T = bt => numeric.fromDouble(bt.doubleValue())
		Seq(new AnalysisDescriptionTileWrapper[JavaDouble, T](convertFcn, new NumericMinTileAnalytic[T]()),
		    new AnalysisDescriptionTileWrapper[JavaDouble, T](convertFcn, new NumericMaxTileAnalytic[T]()))
	}
	override def serializer: TileSerializer[JavaDouble] =
		new PrimitiveAvroSerializer(classOf[JavaDouble], CodecFactory.bzip2Codec())
}

/**
 * A constructor for SeriesValueExtractor2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see SeriesValueExtractor2
 */
class SeriesValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory("series", parent, path)
{
	addProperty(ValueExtractorFactory.FIELDS_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		def fields = getPropertyValue(ValueExtractorFactory.FIELDS_PROPERTY).asScala.toArray
		new SeriesValueExtractor[T, JT](fields)(tag, numeric, conversion)
	}
}

/**
 * A value extractor that sets as the value for each record a (dense) array of the values of various fields in the
 * record.
 * @param _fields The record fields whose values should be used as the record's value
 * @tparam T The numeric type expected for the fields in question
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class SeriesValueExtractor[T: ClassTag, JT] (_fields: Array[String])
                           (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Seq[T], JavaList[JT]] with Serializable {
	def name = "series"
	def fields = _fields
	override def convert: (Seq[Any]) => Seq[T] =
		s => s.map(v => Try(v.asInstanceOf[T]).getOrElse(numeric.fromInt(0)))
	override def binningAnalytic: BinningAnalytic[Seq[T], JavaList[JT]] =
		new ArrayBinningAnalytic[T, JT](new NumericSumBinningAnalytic())
	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[JT]], _]] = {
		val convertFcn: JavaList[JT] => Seq[T] = bt => bt.asScala.map(conversion.backwards(_))
		Seq(new AnalysisDescriptionTileWrapper(convertFcn, new ArrayTileAnalytic[T](new NumericMinTileAnalytic())),
		    new AnalysisDescriptionTileWrapper(convertFcn, new ArrayTileAnalytic[T](new NumericMaxTileAnalytic())),
		    new CustomGlobalMetadata(Map[String, Object]("variables" -> fields.toSeq.asJava)))
	}
	override def serializer: TileSerializer[JavaList[JT]] =
		new PrimitiveArrayAvroSerializer(conversion.toClass, CodecFactory.bzip2Codec())
}

object IndirectSeriesValueExtractor {
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

/**
 * A constructor for IndirectSeriesValueExtractor2 value extractors.  All arguments are pass-throughs to the
 * super-class's constructor.
 *
 * @see IndirectSeriesValueExtractor
 */
class IndirectSeriesValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory("indirectSeries", parent, path)
{
	addProperty(IndirectSeriesValueExtractor.KEY_PROPERTY)
	addProperty(IndirectSeriesValueExtractor.VALUE_PROPERTY)
	addProperty(IndirectSeriesValueExtractor.VALID_KEYS_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		import IndirectSeriesValueExtractor._
		def keyField = getPropertyValue(KEY_PROPERTY)
		def valueField = getPropertyValue(VALUE_PROPERTY)
		def validKeys = getPropertyValue(VALID_KEYS_PROPERTY).asScala.toArray
		new IndirectSeriesValueExtractor[T, JT](keyField, valueField, validKeys)(tag, numeric, conversion)
	}
}

/**
 * A value extractor that stores a (dense) array of values, summed across records, where for each record, a single,
 * named value in that array is non-zero.
 *
 * @param keyField The field from which the name of the non-zero entry for each record is taken
 * @param valueField The field from which the value of the non-zero entry for each record is taken
 * @param validKeys The list of valid entry names
 * @tparam T The numeric type expected for the fields in question
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class IndirectSeriesValueExtractor[T: ClassTag, JT] (keyField: String, valueField: String, validKeys: Seq[String])
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
	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[JT]], _]] = {
		val convertFcn: JavaList[JT] => Seq[T] = bt =>
		for (b <- bt.asScala) yield conversion.backwards(b)
		Seq(new AnalysisDescriptionTileWrapper(convertFcn, new ArrayTileAnalytic[T](new NumericMinTileAnalytic())),
		    new AnalysisDescriptionTileWrapper(convertFcn, new ArrayTileAnalytic[T](new NumericMaxTileAnalytic())))
	}
	def serializer: TileSerializer[JavaList[JT]] =
		new PrimitiveArrayAvroSerializer(conversion.toClass, CodecFactory.bzip2Codec())
}

/**
 * A constructor for MultiFieldValueExtractor2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see MultiFieldValueExtractor
 */
class MultiFieldValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory("fieldMap", parent, path)
{
	addProperty(ValueExtractorFactory.FIELDS_PROPERTY)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val fields = getPropertyValue(ValueExtractorFactory.FIELDS_PROPERTY).asScala.toArray
		new MultiFieldValueExtractor[T, JT](fields)(tag, numeric, conversion)
	}
}

/**
 * A value extractor that sets as the value for each record a set of values of various fields in the
 * record.  This set is kept as a dense array during processing, but is written out as a sparse array.
 * @param _fields The record fields whose values should be used as the record's value
 * @tparam T The numeric type expected for the fields in question
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class MultiFieldValueExtractor[T: ClassTag, JT] (_fields: Array[String])
                               (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Seq[T], JavaList[Pair[String, JT]]] with Serializable {
	def name = "fieldMap"
	def fields = _fields
	override def convert: (Seq[Any]) => Seq[T] =
		s => s.map(v => Try(v.asInstanceOf[T]).getOrElse(numeric.fromInt(0)))
	def binningAnalytic: BinningAnalytic[Seq[T], JavaList[Pair[String, JT]]] =
		new CategoryValueBinningAnalytic[T, JT](_fields, new NumericSumBinningAnalytic())
	override def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[Pair[String, JT]]], _]] =
		Seq()
	def serializer: TileSerializer[JavaList[Pair[String, JT]]] =
		new PairArrayAvroSerializer(classOf[String], conversion.toClass, CodecFactory.bzip2Codec())
}

object StringScoreBinningAnalyticFactory {
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
	                                        "none", Array("low", "high", "alpha", "reverse-alpha", "none"))

	protected def getOrder[T] (orderDescription: Option[String])(implicit numeric: ExtendedNumeric[T]):
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
	def addProperties (factory: ValueExtractorFactory): Unit = {
		factory.addProperty(AGGREGATION_LIMIT_PROPERTY)
		factory.addProperty(BIN_LIMIT_PROPERTY)
		factory.addProperty(ORDER_PROPERTY)
	}
	def getBinningAnalytic[T, JT] (factory: ValueExtractorFactory)
	                      (implicit numeric: ExtendedNumeric[T],
	                       conversion: TypeConversion[T, JT]): BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]] = {
		val aggregationLimit = factory.optionalGet(AGGREGATION_LIMIT_PROPERTY).map(_.intValue())
		val binLimit = factory.optionalGet(BIN_LIMIT_PROPERTY).map(_.intValue())
		val ordering = getOrder(factory.optionalGet(ORDER_PROPERTY))
		new StringScoreBinningAnalytic[T, JT](new NumericSumBinningAnalytic(), aggregationLimit, ordering, binLimit)
	}
}
/**
 * A constructor for SubstringValueExtrator2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see SubstringValueExtractor2
 */
class StringValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory("string", parent, path)
{
	addProperty(ValueExtractorFactory.FIELD_PROPERTY)
	StringScoreBinningAnalyticFactory.addProperties(this)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val field = getPropertyValue(ValueExtractorFactory.FIELD_PROPERTY)
		val binningAnalytic = StringScoreBinningAnalyticFactory.getBinningAnalytic[T, JT](this)(numeric, conversion)
		new StringValueExtractor[T, JT](field, binningAnalytic)(tag, numeric, conversion)
	}
}
/**
 * A value extractor that sets as the value for each record the count of instances of a given string value in the
 * specified field.
 *
 * @param field The record fields whose values should be used as the record's value
 * @tparam T The numeric type expected for the fields in question
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class StringValueExtractor[T: ClassTag, JT] (field: String,
                                              _binningAnalytic: BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]])
                           (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Map[String, T], JavaList[Pair[String, JT]]]
{
	def name = field
	def fields = Seq(field)
	override def convert: (Seq[Any]) => Map[String, T] =
		s => Map(s(0).toString -> numeric.fromInt(1))
	def binningAnalytic: BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]] = _binningAnalytic
	override def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[Pair[String, JT]]], _]] =
		Seq()
	def serializer: TileSerializer[JavaList[Pair[String, JT]]] =
		new PairArrayAvroSerializer(classOf[String], conversion.toClass, CodecFactory.bzip2Codec())
}

object SubstringValueExtractorFactory {
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

/**
 * A constructor for SubstringValueExtrator2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see SubstringValueExtractor2
 */
class SubstringValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory("string", parent, path)
{
	addProperty(ValueExtractorFactory.FIELD_PROPERTY)
	addProperty(SubstringValueExtractorFactory.PARSING_DELIMITER_PROPERTY)
	addProperty(SubstringValueExtractorFactory.AGGREGATION_DELIMITER_PROPERTY)
	addProperty(SubstringValueExtractorFactory.INDICES_PROPERTY)
	StringScoreBinningAnalyticFactory.addProperties(this)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		import SubstringValueExtractorFactory._
		val field = getPropertyValue(ValueExtractorFactory.FIELD_PROPERTY)
		val parsingDelimiter = getPropertyValue(PARSING_DELIMITER_PROPERTY)
		val aggregationDelimiter = getPropertyValue(AGGREGATION_DELIMITER_PROPERTY)
		val indices = getPropertyValue(INDICES_PROPERTY).asScala.toSeq.map(p =>
			(p.getFirst.intValue, p.getSecond.intValue)
		)
		val binningAnalytic = StringScoreBinningAnalyticFactory.getBinningAnalytic[T, JT](this)(numeric, conversion)
		new SubstringValueExtractor[T, JT](field, parsingDelimiter, aggregationDelimiter, indices, binningAnalytic)(tag, numeric, conversion)
	}
}

/**
 * A value extractor that counts the values of various substrings of a single field
 * @param field The field from which to get keys to count
 * @param parsingDelimiter A delimiter to use when spliting the value of the counted field into keys
 * @param aggregationDelimiter A delimiter to use when recombining selected pieces of the counted field
 * @param indices The indices of the sub-pieces of the counted field to use.
 * @tparam T The numeric type to use for the counts when processing
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class SubstringValueExtractor[T: ClassTag, JT] (field: String,
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
	override def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[Pair[String, JT]]], _]] =
		Seq()
	override def serializer: TileSerializer[JavaList[Pair[String, JT]]] =
		new PairArrayAvroSerializer(classOf[String], conversion.toClass, CodecFactory.bzip2Codec())
}
