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
import java.util.{List => JavaList}

import com.oculusinfo.factory.properties.DoubleProperty
import com.oculusinfo.factory.properties.ListProperty
import com.oculusinfo.factory.properties.StringProperty
import com.oculusinfo.factory.providers.FactoryProvider
import com.oculusinfo.factory.providers.AbstractFactoryProvider
import com.oculusinfo.factory.providers.StandardUberFactoryProvider
import org.json.JSONObject

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}

import com.oculusinfo.binning.impl.{AOITilePyramid, WebMercatorTilePyramid}
import com.oculusinfo.factory.{UberFactory, ConfigurableFactory}

import scala.reflect.ClassTag

import com.oculusinfo.binning.{TilePyramidFactory, TilePyramid, TileData}

import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription
import com.oculusinfo.tilegen.tiling._
import com.oculusinfo.tilegen.tiling.analytics.IPv4Analytics
import com.oculusinfo.tilegen.util.{KeyValueArgumentSource, PropertiesWrapper}


/**
 * A class that encapsulates and describes extraction of indices from schema RDDs, as well as the helper classes that
 * are needed to understand them
 */
abstract class IndexExtractor () {
	/** The name of the index type, used in default naming of tile pyramids. */
	def name: String = fields.mkString(".")

	/** A description of the index type, to be inserted into tile pyramid metadata .*/
	def description: String = "Cartesian index scheme"

	/** A list of the fields needed by the index extractor to determine the index of any individual record. */
	def fields: Seq[String]

	/** The index scheme to be used when binning to understand the indices this extractor extracts from records. */
	def indexScheme: IndexScheme[Seq[Any]]

	// List any tile analytics automatically associated with this index extractor
	def getTileAnalytics[BT]: Seq[AnalysisDescription[TileData[BT], _]] = Seq()

	/** Transformed the pyramid name by inserting the field names of the indexer
		* Use the tiling task parameter "name" to generate customized tiling.
		* The string "{i}" will be replaced by the IndexExtractor.name value
		* For example: "MyTilingName.{i}" -> "MyTilingName.x.y"
		* The string "{i0}", "{i1}"... will be replaced by the IndexExtractor.fields[i] value
		* For example: "MyTilingName.{i0}-{i1}" -> "MyTilingName.x-y"
		*/
	def getTransformedName(inputName : String) : String = {
		var TransformedName = inputName.replace("{i}", name)

		for (i <- 0 to fields.length -1) {
			TransformedName = TransformedName.replace("{i" + i + "}", fields(i))
		}
		
		TransformedName
	}
}


/**
 * General constructors and properties for default index extractor factories
 */
object IndexExtractorFactory {
	private[datasets] val FIELDS_PROPERTY =
		new ListProperty(new StringProperty("field", "The fields used by this index extractor", ""),
		                 "field",
		                 "The fields that the index extractor will pull from a data record to construct that record's index")

	/**
	 * The default index type to use when tiling - a cartesian index.
	 */
	val defaultFactory = "cartesian"
	/**
	 * A set of providers for index extractor factories, to be used when constructing tile tasks
	 * to construct the relevant index extractor for the current task.
	 */
	val subFactoryProviders = MutableMap[Any, FactoryProvider[IndexExtractor]]()
	subFactoryProviders(FactoryKey(CartesianIndexExtractorFactory.NAME,   classOf[CartesianIndexExtractorFactory]))   = CartesianIndexExtractorFactory.provider
	subFactoryProviders(FactoryKey(IPv4IndexExtractorFactory.NAME,        classOf[IPv4IndexExtractorFactory]))        = IPv4IndexExtractorFactory.provider
	subFactoryProviders(FactoryKey(TimeRangeIndexExtractorFactory.NAME,   classOf[TimeRangeIndexExtractorFactory]))   = TimeRangeIndexExtractorFactory.provider

	/**
	 * Add an IndexExtractor sub-factory provider to the list of all possible such providers.
	 *
	 * This will replace a previous provider of the same key
	 */
	def addSubFactoryProvider (identityKey: Any, provider: FactoryProvider[IndexExtractor]): Unit =
		subFactoryProviders(identityKey, provider)
	private def getSubFactoryProviders = subFactoryProviders.values.toSet



	/** Create a standard index extractor uber-factory provider */
	def provider (name: String = null,
	              defaultProvider: String = defaultFactory,
	              subFactoryProviders: Set[FactoryProvider[IndexExtractor]] = getSubFactoryProviders) =
		new StandardUberFactoryProvider[IndexExtractor](subFactoryProviders.asJava) {
			override def createFactory(name: String,
			                           parent: ConfigurableFactory[_],
			                           path: JavaList[String]): ConfigurableFactory[_ <: IndexExtractor] =
				new UberFactory[IndexExtractor](name, classOf[IndexExtractor], parent, path, createChildren(parent, path), defaultProvider)
		}

	/** Short-hand for accessing the standard index extractor uber-factory easily. */
	def apply (parent: ConfigurableFactory[_], path: JavaList[String]) =
		provider().createFactory(parent, path)

	def apply (parent: ConfigurableFactory[_], path: JavaList[String], defaultProvider: String) =
		provider(defaultProvider = defaultProvider).createFactory(parent, path)

	def apply (parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultProvider: String,
	           subFactoryProviders: Set[FactoryProvider[IndexExtractor]]) =
		provider(defaultProvider = defaultProvider,
		         subFactoryProviders = subFactoryProviders).createFactory(parent, path)

	def apply (name: String, parent: ConfigurableFactory[_], path: JavaList[String]) =
		provider(name).createFactory(name, parent, path)

	def apply (name: String, parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultProvider: String) =
		provider(name, defaultProvider).createFactory(name, parent, path)

	def apply (name: String, parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultProvider: String,
	           subFactoryProviders: Set[FactoryProvider[IndexExtractor]]) =
		provider(name, defaultProvider, subFactoryProviders).createFactory(name, parent, path)



	/** Helper method for quick and easy construction of factory providers for sub-factories. */
	def subFactoryProvider (ctor: (ConfigurableFactory[_], JavaList[String]) => IndexExtractorFactory) =
		new AbstractFactoryProvider[IndexExtractor] {
			override def createFactory(name: String,
			                           parent: ConfigurableFactory[_],
			                           path: JavaList[String]): ConfigurableFactory[_ <: IndexExtractor] =
				// Name is ignored, because these are sub-factories
				ctor(parent, path)
		}
}

/**
 * A superclass for all index extractor factories.
 *
 * First of all, this guarantees types of all index extractor factories.
 *
 * Secondly, it makes sure factories have a factory provider - in
 *
 * All parameters are pass-throughs to {@link ConfigurableFactory}.
 */
abstract class IndexExtractorFactory (name: String, parent: ConfigurableFactory[_], path: JavaList[String])
		extends ConfigurableFactory[IndexExtractor](name, classOf[IndexExtractor], parent, path)
{
}

object CartesianIndexExtractorFactory {
	val NAME = "cartesian"
	def provider = IndexExtractorFactory.subFactoryProvider((parent, path) =>
		new CartesianIndexExtractorFactory(parent, path))
}
/** A constructor for a standard cartesian index extractor */
class CartesianIndexExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends IndexExtractorFactory(CartesianIndexExtractorFactory.NAME, parent, path)
{
	// Initialize needed properties
	addProperty(IndexExtractorFactory.FIELDS_PROPERTY)

	override protected def create(): IndexExtractor = {
		val fields = getPropertyValue(IndexExtractorFactory.FIELDS_PROPERTY).asScala
		new CartesianIndexExtractor(fields)
	}
}
/**
 * A simple index extractor that extracts cartesian coordinates from the data, based on fields specified in
 * the configuration
 * @param columns The columns from which to take the coordinates of each record
 */
class CartesianIndexExtractor (columns: Seq[String]) extends IndexExtractor() {
	def fields = columns
	def indexScheme: IndexScheme[Seq[Any]] = new CartesianSchemaIndexScheme
}


object IPv4IndexExtractorFactory {
	val NAME = "ipv4"
	def provider = IndexExtractorFactory.subFactoryProvider((parent, path) =>
		new IPv4IndexExtractorFactory(parent, path))
}
/** A constructor for a standard ip address index extractor */
class IPv4IndexExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends IndexExtractorFactory(IPv4IndexExtractorFactory.NAME, parent, path)
{
	// Initialize needed properties
	addProperty(IndexExtractorFactory.FIELDS_PROPERTY)

	override protected def create (): IndexExtractor = {
		val fields = getPropertyValue(IndexExtractorFactory.FIELDS_PROPERTY).asScala
		val ipField = fields(0)
		new IPv4IndexExtractor(ipField)
	}
}

/**
 * An index extractor that extracts an IP address (v4) from a record
 * @param ipField The field from which to get the IPv4 address of a given record
 */
class IPv4IndexExtractor (ipField: String) extends IndexExtractor() {
	@transient lazy private val _fields = Seq(ipField)
	def fields = _fields
	def indexScheme = new IPv4ZCurveSchemaIndexScheme
	override def getTileAnalytics[BT]: Seq[AnalysisDescription[TileData[BT], _]] =
		Seq(IPv4Analytics.getCIDRBlockAnalysis[BT](),
		    IPv4Analytics.getMinIPAddressAnalysis[BT](),
		    IPv4Analytics.getMaxIPAddressAnalysis[BT]())
}

object TimeRangeIndexExtractorFactory {
	private[datasets] val NAME = "timerange"
	private[datasets] val START_DATE_PROPERTY =
		new DoubleProperty("start-date", "The start of the first time period, for binned times", 0.0)
	private[datasets] val SECONDS_PER_PERIOD_PROPERTY =
		new DoubleProperty("period-length", "The length of each time period, in seconds", 60.0*60.0*24.0)

	def provider = IndexExtractorFactory.subFactoryProvider((parent, path) =>
		new TimeRangeIndexExtractorFactory(parent, path))
}

/** A constructor for a standard time range/cartesian point index extractor */
class TimeRangeIndexExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends IndexExtractorFactory(TimeRangeIndexExtractorFactory.NAME, parent, path)
{
	// Initialize needed properties
	addProperty(IndexExtractorFactory.FIELDS_PROPERTY)
	addProperty(TimeRangeIndexExtractorFactory.START_DATE_PROPERTY)
	addProperty(TimeRangeIndexExtractorFactory.SECONDS_PER_PERIOD_PROPERTY)

	override protected def create (): IndexExtractor = {
		val fields = getPropertyValue(IndexExtractorFactory.FIELDS_PROPERTY).asScala
		val start = getPropertyValue(TimeRangeIndexExtractorFactory.START_DATE_PROPERTY)
		val period = getPropertyValue(TimeRangeIndexExtractorFactory.SECONDS_PER_PERIOD_PROPERTY)
		val timeField = fields(0)
		val xField = fields(1)
		val yField = if (fields.length < 3) "zero" else fields(2)
		new TimeRangeCartesianIndexExtractor(timeField, xField, yField, start, period)
	}
}

/**
 * An augmentation of the standard cartesian index extractor that also records a binned time range.
 * With the ability to do data analytics on fields not used in the index or value, this will probably become defunct.
 * @param timeVar The field from which to get the time of a given record.
 * @param xVar The field from which to get the X coordinate of a given record
 * @param yVar The field from which to get the Y coordinate of a given record
 * @param startDate The start time of the first time bin, as per standard Java date math
 * @param secsPerPeriod The size of each time bin, in seconds
 */
class TimeRangeCartesianIndexExtractor (timeVar: String, xVar: String, yVar: String,
                                        startDate: Double, val secsPerPeriod: Double)
		extends IndexExtractor()
{
	@transient lazy private val _scheme = new TimeRangeCartesianSchemaIndexScheme(startDate, secsPerPeriod)
	@transient lazy private val _fields = Seq(timeVar, xVar, yVar)
	def fields = _fields
	def indexScheme = _scheme
}
