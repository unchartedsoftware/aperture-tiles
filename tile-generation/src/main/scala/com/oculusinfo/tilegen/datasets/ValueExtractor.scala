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


import java.util.Arrays
import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Float => JavaFloat}
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}


import org.json.JSONArray

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

import org.apache.avro.file.CodecFactory

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.{PairAvroSerializer, PairArrayAvroSerializer, PrimitiveArrayAvroSerializer, PrimitiveAvroSerializer}
import com.oculusinfo.binning.io.serialization.TileSerializerFactory
import com.oculusinfo.binning.io.serialization.DefaultTileSerializerFactoryProvider
import com.oculusinfo.binning.util.TypeDescriptor

import com.oculusinfo.factory.util.Pair
import com.oculusinfo.factory.ConfigurableFactory
import com.oculusinfo.factory.ConfigurationException
import com.oculusinfo.factory.UberFactory
import com.oculusinfo.factory.properties._
import com.oculusinfo.factory.providers.FactoryProvider
import com.oculusinfo.factory.providers.AbstractFactoryProvider
import com.oculusinfo.factory.providers.StandardUberFactoryProvider

import com.oculusinfo.tilegen.tiling.analytics._
import com.oculusinfo.tilegen.util._



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

	/** Transformed the pyramid name by inserting the name of the valuer
		* Use the tiling task parameter "name" to generate customized tiling.
		* The string "{v}" will be replaced by the ValueExtractor.name value
		* For example: "MyTilingName.{v}" -> "MyTilingName.count"
		*/
	def getTransformedName(inputName : String) : String = {
		inputName.replace("{v}", name)
	}
}

/**
 * General constructors and properties for default value extractor factories
 */
object ValueExtractorFactory {
	val SERIALIZATION_FRAMEWORK = new StringProperty("framework", "The serialization framework to use by default when no specific serializer is mandated.", "avro",
		Array("avro", "kryo"))
	private[datasets] val FIELD_PROPERTY =
		new StringProperty("field", "The field used by this value extractor", "INVALID FIELD")
	private[datasets] val FIELDS_PROPERTY =
		new ListProperty[String](new StringProperty("fields", "The fields used by this value extractor", ""),
		                         "fields", "The fields used by this value extractor")

	/**
	 * The default value extractor type to use when tiling - a count.
	 */
	val defaultFactory = "count"
	/**
	 * A set of providers for value extractor factories, to be used when constructing tile tasks
	 * to construct the relevant value extractor for the current task.
	 */
	val subFactoryProviders = MutableMap[Any, FactoryProvider[ValueExtractor[_, _]]]()
	subFactoryProviders(FactoryKey(CountValueExtractorFactory.NAME,          classOf[CountValueExtractorFactory]))          = CountValueExtractorFactory.provider
	subFactoryProviders(FactoryKey(FieldValueExtractorFactory.NAME,          classOf[FieldValueExtractorFactory]))          = FieldValueExtractorFactory.provider
	subFactoryProviders(FactoryKey(MultiFieldValueExtractorFactory.NAME,     classOf[MultiFieldValueExtractorFactory]))     = MultiFieldValueExtractorFactory.provider
	subFactoryProviders(FactoryKey(SeriesValueExtractorFactory.NAME,         classOf[SeriesValueExtractorFactory]))         = SeriesValueExtractorFactory.provider
	subFactoryProviders(FactoryKey(IndirectSeriesValueExtractorFactory.NAME, classOf[IndirectSeriesValueExtractorFactory])) = IndirectSeriesValueExtractorFactory.provider
	subFactoryProviders(FactoryKey(StringValueExtractorFactory.NAME,         classOf[StringValueExtractorFactory]))         = StringValueExtractorFactory.provider
	subFactoryProviders(FactoryKey(SubstringValueExtractorFactory.NAME,      classOf[SubstringValueExtractorFactory]))      = SubstringValueExtractorFactory.provider

	/**
	 * Add a ValueExtractor sub-factory provider to the list of all possible such providers.
	 *
	 * This will replace a previous provider of the same key
	 */
	def addSubFactoryProvider (identityKey: Any, provider: FactoryProvider[ValueExtractor[_, _]]): Unit =
		subFactoryProviders(identityKey) = provider
	def getSubFactoryProviders = subFactoryProviders.values.toSet




	/** Create a standard value extractor uber-factory provider */
	def provider(name: String = null,
	             defaultProvider: String = defaultFactory,
	             subFactoryProviders: Set[FactoryProvider[ValueExtractor[_, _]]] = getSubFactoryProviders) =
		new StandardUberFactoryProvider[ValueExtractor[_, _]](subFactoryProviders.asJava) {
			override def createFactory(name: String, parent: ConfigurableFactory[_], path: JavaList[String]):
					ConfigurableFactory[_ <: ValueExtractor[_, _]] =
				new UberFactory[ValueExtractor[_, _]](name, classOf[ValueExtractor[_, _]], parent, path,
				                                      createChildren(parent, path), defaultProvider)
		}

	/** Short-hand for accessing the standard value extractor uber-factory easily. */
	def apply (parent: ConfigurableFactory[_], path: JavaList[String]) =
		provider().createFactory(parent, path)

	def apply (parent: ConfigurableFactory[_], path: JavaList[String], defaultProvider: String) =
		provider(defaultProvider = defaultProvider).createFactory(parent, path)

	def apply (parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultProvider: String,
	           subFactoryProviders: Set[FactoryProvider[ValueExtractor[_, _]]]) =
		provider(defaultProvider = defaultProvider,
		         subFactoryProviders = subFactoryProviders).createFactory(parent, path)

	def apply (name: String, parent: ConfigurableFactory[_], path: JavaList[String]) =
		provider(name).createFactory(name, parent, path)

	def apply (name: String, parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultProvider: String) =
		provider(name, defaultProvider).createFactory(name, parent, path)

	def apply (name: String, parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultProvider: String,
	           subFactoryProviders: Set[FactoryProvider[ValueExtractor[_, _]]]) =
		provider(name, defaultProvider, subFactoryProviders).createFactory(name, parent, path)

	/** Helper method for quick and easy construction of factory providers for sub-factories. */
	def subFactoryProvider (ctor: (ConfigurableFactory[_], JavaList[String]) => ValueExtractorFactory) =
		new AbstractFactoryProvider[ValueExtractor[_, _]] {
			override def createFactory(name: String,
			                           parent: ConfigurableFactory[_],
			                           path: JavaList[String]): ConfigurableFactory[_ <: ValueExtractor[_, _]] =
				// Name is ignored, since these are sub-factories
				ctor(parent, path)
		}
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
	import ValueExtractorFactory._

	protected val serializerFactory: ConfigurableFactory[TileSerializer[_]] = {
		val path = List("serializer").asJava
		new TileSerializerFactory(this, path, DefaultTileSerializerFactoryProvider.values.map(_.createFactory(this, path)).toList.asJava)
	}
	addProperty(SERIALIZATION_FRAMEWORK, Arrays.asList("serializer"))
	addChildFactory(serializerFactory)

	/**
	 * All this method does is check the return type of a serializer
	 * programatically, As such, it supercedes the warnings hidden here.
	 *
	 * It will only work, of course, if the serializer is set up correctly
	 * (i.e., without lying about its type_, and if the pass-ed class and
	 * expandedClass actually match; mis-use will, of course, cause errors.
	 */
	def checkBinClass[T] (serializer: TileSerializer[_], expectedBinClass: Class[T],
	                      expandedExpectedBinClass: TypeDescriptor): TileSerializer[T] = {
		if (null == serializer) {
			throw new ConfigurationException("No serializer given for renderer")
		}
		if (!expandedExpectedBinClass.equals(serializer.getBinTypeDescription())) {
			throw new ConfigurationException("Serialization type does not match rendering type.  Serialization class was "+serializer.getBinTypeDescription()+", renderer type was "+expandedExpectedBinClass)
		}
		serializer.asInstanceOf[TileSerializer[T]]
	}

	def getDefaultSerializerType (baseType: String, expectedPrimitiveCLass: Class[_]*): String = {
		val framework = getPropertyValue(SERIALIZATION_FRAMEWORK).toLowerCase()
		val suffix = if ("avro" == framework) "-a"
		else if ("kryo" == framework) "-k"
		else ""
		baseType.format(expectedPrimitiveCLass.map(_.getSimpleName.toLowerCase()):_*) + suffix
	}
}

object CountValueExtractorFactory {
	private[datasets] val NAME = "count"
	def provider = ValueExtractorFactory.subFactoryProvider((parent, path) =>
		new CountValueExtractorFactory(parent, path))
}
/**
 * A constructor for CountValueExtractor2 value extractors.  All arguments are pass-throughs to
 * @see CountValueExtractor2
 */
class CountValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory(CountValueExtractorFactory.NAME, parent, path)
{
	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("%s", conversion.toClass))
		val serializer: TileSerializer[JT] = checkBinClass(produce(classOf[TileSerializer[_]]), conversion.toClass, new TypeDescriptor(conversion.toClass))
		new CountValueExtractor[T, JT](serializer)(tag, numeric, conversion)
	}
}

/**
 * A value extractor that just uses a record count as the record value (so each record has a value of '1').
 * @tparam T The numeric type to use for the count when processing
 * @tparam JT The numeric type to use for the count when writing tiles (generally a Java version of T)
 */
class CountValueExtractor[T: ClassTag, JT] (_serializer: TileSerializer[JT])(implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
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
	def serializer = _serializer
}

object FieldValueExtractorFactory {
	private[datasets] val NAME = "field"
	def provider = ValueExtractorFactory.subFactoryProvider((parent, path) =>
		new FieldValueExtractorFactory(parent, path))
}
/**
 * A constructor for FieldValueExtractor2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see FieldValueExtractor2
 */
class FieldValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory(FieldValueExtractorFactory.NAME, parent, path)
{
	import FieldValueExtractorFactory._

	addProperty(ValueExtractorFactory.FIELD_PROPERTY)
	addChildFactory(new NumericBinningAnalyticFactory(this, List[String]().asJava))

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val field = getPropertyValue(ValueExtractorFactory.FIELD_PROPERTY)
		// Guaranteed the same numeric type because the numeric type of the analytic will be determined by the exact
		// same property by which our numeric type is determined.
		val analytic = produce(classOf[BinningAnalytic[T, JT]])

		if (analytic.isInstanceOf[NumericMeanBinningAnalytic[_]]) {
			serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("%s", classOf[JavaDouble]))
			val serializer = checkBinClass(produce(classOf[TileSerializer[_]]), classOf[JavaDouble], new TypeDescriptor(classOf[JavaDouble]))
			new MeanValueExtractor[T](field, analytic.asInstanceOf[NumericMeanBinningAnalytic[T]], serializer)(tag, numeric)
		} else if (analytic.isInstanceOf[NumericStatsBinningAnalytic[_]]) {
			serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("(%s, %s)", classOf[JavaDouble], classOf[JavaDouble]))
			val serializer = checkBinClass(produce(classOf[TileSerializer[_]]),
			                               classOf[Pair[JavaDouble, JavaDouble]], new TypeDescriptor(classOf[Pair[JavaDouble, JavaDouble]],
			                                                                                         new TypeDescriptor(classOf[JavaDouble]),
			                                                                                         new TypeDescriptor(classOf[JavaDouble])))
			new StatsValueExtractor[T](field, analytic.asInstanceOf[NumericStatsBinningAnalytic[T]], serializer)(tag, numeric)
		} else {
			serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("%s", conversion.toClass))
			val serializer = checkBinClass(produce(classOf[TileSerializer[_]]), conversion.toClass, new TypeDescriptor(conversion.toClass))
			new FieldValueExtractor[T, JT](field, analytic, serializer)(tag, numeric, conversion)
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
class FieldValueExtractor[T: ClassTag, JT] (field: String, _binningAnalytic: BinningAnalytic[T, JT],
                                            _serializer: TileSerializer[JT])
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
	override def serializer: TileSerializer[JT] = _serializer
}

/**
 * A value extractor that uses the mean of the (numeric) value of a single field as the value of each record.
 * @param field The field whose value is used as the record's value
 * @param analytic The mean binning analytic used for aggregation by this extractor
 * @tparam T The numeric type expected for the field in question.  Bins are always written as Java Doubles
 */
class MeanValueExtractor[T: ClassTag] (field: String, analytic: NumericMeanBinningAnalytic[T],
                                       _serializer: TileSerializer[JavaDouble])
                        (implicit numeric: ExtendedNumeric[T])
		extends ValueExtractor[(T, Int), JavaDouble] with Serializable {
	def name = field
	def fields = Seq(field)
	override def convert: (Seq[Any]) => (T, Int) = s => (s(0).asInstanceOf[T], 1)
	override def binningAnalytic: BinningAnalytic[(T, Int), JavaDouble] = analytic
	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaDouble], _]] = {
		val convertFcn: JavaDouble => T = bt => {
      if (null == bt) {
        numeric.fromDouble(analytic.finish(analytic.defaultProcessedValue))
      } else {
        numeric.fromDouble(bt.doubleValue())
      }
    }
		Seq(new AnalysisDescriptionTileWrapper[JavaDouble, T](convertFcn, new NumericMinTileAnalytic[T]()),
		    new AnalysisDescriptionTileWrapper[JavaDouble, T](convertFcn, new NumericMaxTileAnalytic[T]()))
	}
	override def serializer: TileSerializer[JavaDouble] = _serializer
}
/**
 * A value extractor that uses the mean and standard deviation of the (numeric) value of a single field
 * as the value of each record.
 * @param field The field whose value is used as the record's value
 * @param analytic The stats binning analytic used for aggregation by this extractor
 * @tparam T The numeric type expected for the field in question.  Bins are always written as pairs of Java Doubles
 */
class StatsValueExtractor[T: ClassTag] (field: String, analytic: NumericStatsBinningAnalytic[T],
                                        _serializer: TileSerializer[Pair[JavaDouble, JavaDouble]])
                         (implicit numeric: ExtendedNumeric[T])
		extends ValueExtractor[(T, T, Int), Pair[JavaDouble, JavaDouble]] with Serializable
{
	override def name = field
	override def fields = Seq(field)
	override def convert: (Seq[Any]) => (T, T, Int) = s => {
		val v = s(0).asInstanceOf[T]
		(v, numeric.times(v, v), 1)
	}
	override def binningAnalytic: BinningAnalytic[(T, T, Int), Pair[JavaDouble, JavaDouble]] = analytic
	override def getTileAnalytics: Seq[AnalysisDescription[TileData[Pair[JavaDouble, JavaDouble]], _]] = {
		val convertMean:  Pair[JavaDouble, JavaDouble] => T = bt => numeric.fromDouble(bt.getFirst.doubleValue())
		val convertSigma: Pair[JavaDouble, JavaDouble] => T = bt => numeric.fromDouble(bt.getSecond.doubleValue())
		Seq(new AnalysisDescriptionTileWrapper[Pair[JavaDouble, JavaDouble], T](convertMean, new NumericMinTileAnalytic[T](Some("minMean"))),
		    new AnalysisDescriptionTileWrapper[Pair[JavaDouble, JavaDouble], T](convertMean, new NumericMaxTileAnalytic[T](Some("maxMean"))),
		    new AnalysisDescriptionTileWrapper[Pair[JavaDouble, JavaDouble], T](convertSigma, new NumericMinTileAnalytic[T](Some("minStdDev"))),
		    new AnalysisDescriptionTileWrapper[Pair[JavaDouble, JavaDouble], T](convertSigma, new NumericMaxTileAnalytic[T](Some("maxStdDev"))))
	}
	override def serializer: TileSerializer[Pair[JavaDouble, JavaDouble]] = _serializer
}

object SeriesValueExtractorFactory {
	private[datasets] val NAME = "series"
	def provider = ValueExtractorFactory.subFactoryProvider((parent, path) =>
		new SeriesValueExtractorFactory(parent, path))
}
/**
 * A constructor for SeriesValueExtractor2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see SeriesValueExtractor2
 */
class SeriesValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory(SeriesValueExtractorFactory.NAME, parent, path)
{
	addProperty(ValueExtractorFactory.FIELDS_PROPERTY)
	addChildFactory(new NumericBinningAnalyticFactory(this, List[String]().asJava))

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val fields = getPropertyValue(ValueExtractorFactory.FIELDS_PROPERTY).asScala.toArray
		val elementAnalytic = produce(classOf[BinningAnalytic[T, JT]])
		serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("[%s]", conversion.toClass))
		val serializer = checkBinClass(produce(classOf[TileSerializer[_]]), classOf[JavaList[JT]],
		                               new TypeDescriptor(classOf[JavaList[JT]], new TypeDescriptor(conversion.toClass)))
		new SeriesValueExtractor[T, JT](fields, elementAnalytic, serializer)(tag, numeric, conversion)
	}
}

/**
 * A value extractor that sets as the value for each record a (dense) array of the values of various fields in the
 * record.
 * @param _fields The record fields whose values should be used as the record's value
 * @param elementAnalytic The binning analytic used to aggregate individual series value entries
 * @tparam T The numeric type expected for the fields in question
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class SeriesValueExtractor[T: ClassTag, JT] (_fields: Array[String], elementAnalytic: BinningAnalytic[T, JT],
                                             _serializer: TileSerializer[JavaList[JT]])
                          (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Seq[T], JavaList[JT]] with Serializable {
	def name = "series"
	def fields = _fields
	override def convert: (Seq[Any]) => Seq[T] =
		s => s.map(v => Try(v.asInstanceOf[T]).getOrElse(numeric.fromInt(0)))
	override def binningAnalytic: BinningAnalytic[Seq[T], JavaList[JT]] =
		new ArrayBinningAnalytic[T, JT](elementAnalytic)
	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[JT]], _]] = {
		val convertFcn: JavaList[JT] => Seq[T] = bt => bt.asScala.map(conversion.backwards(_))
		val fieldNames = {
			val names = new JSONArray()
			fields.foreach(names.put(_))
			names
		}
		Seq(new AnalysisDescriptionTileWrapper(convertFcn, new ArrayTileAnalytic[T](new NumericMinTileAnalytic())),
		    new AnalysisDescriptionTileWrapper(convertFcn, new ArrayTileAnalytic[T](new NumericMaxTileAnalytic())),
		    new CustomGlobalMetadata(Map[String, String]("variables" -> fieldNames.toString)))
	}
	override def serializer: TileSerializer[JavaList[JT]] = _serializer
}

object IndirectSeriesValueExtractorFactory {
	private[datasets] val NAME = "indirectSeries"
	val KEY_PROPERTY = new StringProperty("key",
	                                      "The field in which to find the key of a given record for the indirect series",
	                                      "")
	val VALUE_PROPERTY = new StringProperty("value",
	                                        "The field in which to find the value of a given record for the indirect series",
	                                        "")
	val VALID_KEYS_PROPERTY = new ListProperty(KEY_PROPERTY,
	                                           "validKeys",
	                                           "A list of the valid values that may be found in a records key property; all other values will be ignored.")

	def provider = ValueExtractorFactory.subFactoryProvider((parent, path) =>
		new IndirectSeriesValueExtractorFactory(parent, path))
}

/**
 * A constructor for IndirectSeriesValueExtractor2 value extractors.  All arguments are pass-throughs to the
 * super-class's constructor.
 *
 * @see IndirectSeriesValueExtractor
 */
class IndirectSeriesValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory(IndirectSeriesValueExtractorFactory.NAME, parent, path)
{
	addProperty(IndirectSeriesValueExtractorFactory.KEY_PROPERTY)
	addProperty(IndirectSeriesValueExtractorFactory.VALUE_PROPERTY)
	addProperty(IndirectSeriesValueExtractorFactory.VALID_KEYS_PROPERTY)
	addChildFactory(new NumericBinningAnalyticFactory(this, List[String]().asJava))

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		import IndirectSeriesValueExtractorFactory._
		val keyField = getPropertyValue(KEY_PROPERTY)
		val valueField = getPropertyValue(VALUE_PROPERTY)
		val validKeys = getPropertyValue(VALID_KEYS_PROPERTY).asScala.toArray
		val elementAnalytic = produce(classOf[BinningAnalytic[T, JT]])
		serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("[%s]", conversion.toClass))
		val serializer = checkBinClass(produce(classOf[TileSerializer[_]]), classOf[JavaList[JT]],
		                               new TypeDescriptor(classOf[JavaList[JT]], new TypeDescriptor(conversion.toClass)))

		new IndirectSeriesValueExtractor[T, JT](keyField, valueField, validKeys, elementAnalytic, serializer)(tag, numeric, conversion)
	}
}

/**
 * A value extractor that stores a (dense) array of values, summed across records, where for each record, a single,
 * named value in that array is non-zero.
 *
 * @param keyField The field from which the name of the non-zero entry for each record is taken
 * @param valueField The field from which the value of the non-zero entry for each record is taken
 * @param validKeys The list of valid entry names
 * @param elementAnalytic The binning analytic used to aggregate individual series value entries
 * @tparam T The numeric type expected for the fields in question
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class IndirectSeriesValueExtractor[T: ClassTag, JT] (keyField: String, valueField: String, validKeys: Seq[String],
                                                     elementAnalytic: BinningAnalytic[T, JT],
                                                     _serializer: TileSerializer[JavaList[JT]])
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
		new ArrayBinningAnalytic[T, JT](elementAnalytic)
	def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[JT]], _]] = {
		val convertFcn: JavaList[JT] => Seq[T] = bt =>
		for (b <- bt.asScala) yield conversion.backwards(b)
		Seq(new AnalysisDescriptionTileWrapper(convertFcn, new ArrayTileAnalytic[T](new NumericMinTileAnalytic())),
		    new AnalysisDescriptionTileWrapper(convertFcn, new ArrayTileAnalytic[T](new NumericMaxTileAnalytic())))
	}
	def serializer: TileSerializer[JavaList[JT]] = _serializer
}

object MultiFieldValueExtractorFactory {
	private[datasets] val NAME = "fieldMap"
	def provider = ValueExtractorFactory.subFactoryProvider((parent, path) =>
		new MultiFieldValueExtractorFactory(parent, path))
}
/**
 * A constructor for MultiFieldValueExtractor2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see MultiFieldValueExtractor
 */
class MultiFieldValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory(MultiFieldValueExtractorFactory.NAME, parent, path)
{
	addProperty(ValueExtractorFactory.FIELDS_PROPERTY)
	addChildFactory(new NumericBinningAnalyticFactory(this, List[String]().asJava))

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val fields = getPropertyValue(ValueExtractorFactory.FIELDS_PROPERTY).asScala.toArray
		val analytic = produce(classOf[BinningAnalytic[T, JT]])
		serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("[(%s, %s)]", classOf[String], conversion.toClass))
		val serializer = checkBinClass(produce(classOf[TileSerializer[_]]),
		                               classOf[JavaList[Pair[String, JT]]],
		                               new TypeDescriptor(classOf[JavaList[Pair[String, JT]]],
		                                                  new TypeDescriptor(classOf[Pair[String, JT]],
		                                                                     new TypeDescriptor(classOf[String]),
		                                                                     new TypeDescriptor(conversion.toClass))))

		new MultiFieldValueExtractor[T, JT](fields, analytic, serializer)(tag, numeric, conversion)
	}
}

/**
 * A value extractor that sets as the value for each record a set of values of various fields in the
 * record.  This set is kept as a dense array during processing, but is written out as a sparse array.
 * @param _fields The record fields whose values should be used as the record's value
 * @param elementAnalytic The binning analytic used to aggregate individual field value entries
 * @tparam T The numeric type expected for the fields in question
 * @tparam JT The numeric type to use when writing tiles (generally a Java version of T)
 */
class MultiFieldValueExtractor[T: ClassTag, JT] (_fields: Array[String], elementAnalytic: BinningAnalytic[T, JT],
                                                 _serializer: TileSerializer[JavaList[Pair[String, JT]]])
                              (implicit numeric: ExtendedNumeric[T], conversion: TypeConversion[T, JT])
		extends ValueExtractor[Seq[T], JavaList[Pair[String, JT]]] with Serializable {
	def name = "fieldMap"
	def fields = _fields
	override def convert: (Seq[Any]) => Seq[T] =
		s => s.map(v => Try(v.asInstanceOf[T]).getOrElse(numeric.fromInt(0)))
	def binningAnalytic: BinningAnalytic[Seq[T], JavaList[Pair[String, JT]]] =
		new CategoryValueBinningAnalytic[T, JT](_fields, elementAnalytic)
	override def getTileAnalytics: Seq[AnalysisDescription[TileData[JavaList[Pair[String, JT]]], _]] =
		Seq()
	def serializer: TileSerializer[JavaList[Pair[String, JT]]] = _serializer
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
		factory.addChildFactory(new NumericBinningAnalyticFactory(factory, List[String]().asJava))
	}
	def getBinningAnalytic[T, JT] (factory: ValueExtractorFactory)
	                      (implicit numeric: ExtendedNumeric[T],
	                       conversion: TypeConversion[T, JT]): BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]] = {
		val aggregationLimit = factory.optionalGet(AGGREGATION_LIMIT_PROPERTY).map(_.intValue())
		val binLimit = factory.optionalGet(BIN_LIMIT_PROPERTY).map(_.intValue())
		val ordering = getOrder(factory.optionalGet(ORDER_PROPERTY))
		val elementAnalytic = factory.produce(classOf[BinningAnalytic[T, JT]])
		new StringScoreBinningAnalytic[T, JT](elementAnalytic, aggregationLimit, ordering, binLimit)
	}
}
object StringValueExtractorFactory {
	private[datasets] val NAME = "string"
	def provider = ValueExtractorFactory.subFactoryProvider((parent, path) =>
		new StringValueExtractorFactory(parent, path))
}
/**
 * A constructor for SubstringValueExtrator2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see SubstringValueExtractor2
 */
class StringValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory(StringValueExtractorFactory.NAME, parent, path)
{
	addProperty(ValueExtractorFactory.FIELD_PROPERTY)
	StringScoreBinningAnalyticFactory.addProperties(this)

	override protected def typedCreate[T, JT] (tag: ClassTag[T],
	                                           numeric: ExtendedNumeric[T],
	                                           conversion: TypeConversion[T, JT]): ValueExtractor[_, _] = {
		val field = getPropertyValue(ValueExtractorFactory.FIELD_PROPERTY)
		val binningAnalytic = StringScoreBinningAnalyticFactory.getBinningAnalytic[T, JT](this)(numeric, conversion)
		serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("[(%s, %s)]", classOf[String], conversion.toClass))
		val serializer = checkBinClass(produce(classOf[TileSerializer[_]]),
		                               classOf[JavaList[Pair[String, JT]]],
		                               new TypeDescriptor(classOf[JavaList[Pair[String, JT]]],
		                                                  new TypeDescriptor(classOf[Pair[String, JT]],
		                                                                     new TypeDescriptor(classOf[String]),
		                                                                     new TypeDescriptor(conversion.toClass))))

		new StringValueExtractor[T, JT](field, binningAnalytic, serializer)(tag, numeric, conversion)
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
                                             _binningAnalytic: BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]],
                                             _serializer: TileSerializer[JavaList[Pair[String, JT]]])
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
	def serializer: TileSerializer[JavaList[Pair[String, JT]]] = _serializer
}

object SubstringValueExtractorFactory {
	private[datasets] val NAME = "substring"
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

	def provider = ValueExtractorFactory.subFactoryProvider((parent, path) =>
		new SubstringValueExtractorFactory(parent, path))
}

/**
 * A constructor for SubstringValueExtrator2 value extractors.  All arguments are pass-throughs to the super-class's
 * constructor.
 *
 * @see SubstringValueExtractor2
 */
class SubstringValueExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ValueExtractorFactory(SubstringValueExtractorFactory.NAME, parent, path)
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
		serializerFactory.setDefaultValue(UberFactory.FACTORY_TYPE, getDefaultSerializerType("[(%s, %s)]", classOf[String], conversion.toClass))
		val serializer = checkBinClass(produce(classOf[TileSerializer[_]]),
		                               classOf[JavaList[Pair[String, JT]]],
		                               new TypeDescriptor(classOf[JavaList[Pair[String, JT]]],
		                                                  new TypeDescriptor(classOf[Pair[String, JT]],
		                                                                     new TypeDescriptor(classOf[String]),
		                                                                     new TypeDescriptor(conversion.toClass))))

		new SubstringValueExtractor[T, JT](field, parsingDelimiter, aggregationDelimiter, indices, binningAnalytic, serializer)(tag, numeric, conversion)
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
                                                _binningAnalytic: BinningAnalytic[Map[String, T], JavaList[Pair[String, JT]]],
                                                _serializer: TileSerializer[JavaList[Pair[String, JT]]])
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
	override def serializer: TileSerializer[JavaList[Pair[String, JT]]] = _serializer
}
