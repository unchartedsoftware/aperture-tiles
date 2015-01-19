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

import com.oculusinfo.factory.properties.{DoubleProperty, StringProperty, ListProperty}
import org.json.JSONObject

import scala.collection.JavaConverters._

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
}


/**
 * General constructors and properties for default index extractor factories
 */
object IndexExtractorFactory {
	private[datasets] val FIELDS_PROPERTY =
		new ListProperty(new StringProperty("field", "The fields used by this index extractor", ""),
		                 "field",
		                 "The fields that the index extractor will pull from a data record to construct that record's index")

	val defaultFactory = "cartesian"

	/** Default function to use when creating child factories */
	def createChildren (parent: ConfigurableFactory[_], path: JavaList[String]):
			JavaList[ConfigurableFactory[_ <: IndexExtractor]] =
		Seq[ConfigurableFactory[_ <: IndexExtractor]](
			new CartesianIndexExtractorFactory(parent, path),
			new LineSegmentIndexExtractorFactory(parent, path),
			new IP4VIndexExtractorFactory(parent, path),
			new TimeRangeIndexExtractorFactory(parent, path)).asJava


	/** Create an un-named uber-factory for index extractors */
	def apply (parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultType: String = defaultFactory,
	           childProviders: (ConfigurableFactory[_],
	                            JavaList[String]) => JavaList[ConfigurableFactory[_ <: IndexExtractor]] = createChildren):
			ConfigurableFactory[IndexExtractor] =
		new UberFactory[IndexExtractor](classOf[IndexExtractor], parent, path, true,
		                                createChildren(parent, path), defaultType)

	/** Create a named uber-factory for index extractors */
	def named (name: String, parent: ConfigurableFactory[_], path: JavaList[String],
	           defaultType: String = defaultFactory,
	           childProviders: (ConfigurableFactory[_],
	                            JavaList[String]) => JavaList[ConfigurableFactory[_ <: IndexExtractor]] = createChildren):
			ConfigurableFactory[IndexExtractor] =
		new UberFactory[IndexExtractor](name, classOf[IndexExtractor], parent, path, true,
		                                createChildren(parent, path), defaultType)
}

/**
 * A superclass for all index extractor factories; all it really does is add in the pyramid factory.
 *
 * All parameters are pass-throughs to {@link ConfigurableFactory}.
 */
abstract class IndexExtractorFactory (name: String, parent: ConfigurableFactory[_], path: JavaList[String])
		extends ConfigurableFactory[IndexExtractor](name, classOf[IndexExtractor], parent, path)
{
}

/** A constructor for a standard cartesian index extractor */
class CartesianIndexExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends IndexExtractorFactory("cartesian", parent, path)
{
	// Initialize needed properties
	addProperty(IndexExtractorFactory.FIELDS_PROPERTY)

	override protected def create(): IndexExtractor = {
		val fields = getPropertyValue(IndexExtractorFactory.FIELDS_PROPERTY).asScala
		val xField = fields(0)
		val yField = if (fields.size < 2) "zero" else fields(1)
		new CartesianSchemaIndexExtractor(xField, yField)
	}
}
/**
 * A simple index extractor that extracts cartesian coordinates from the data, based on fields specified in
 * the configuration
 * @param xField The field from which to get the X coordinate of a given record
 * @param yField The field from which to get the Y coordinate of a given record
 */
class CartesianSchemaIndexExtractor (xField: String, yField: String) extends IndexExtractor() {
	private def checkForZero (field: String): String = if ("zero" == field) "0" else field
	@transient lazy private val _fields = Seq(checkForZero(xField), checkForZero(yField))
	def fields = _fields
	def indexScheme: IndexScheme[Seq[Any]] = new CartesianSchemaIndexScheme
}

/** A constructor for a standard line segment index extractor */
class LineSegmentIndexExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends IndexExtractorFactory("segment", parent, path)
{
	// Initialize needed properties
	addProperty(IndexExtractorFactory.FIELDS_PROPERTY)

	override protected def create (): IndexExtractor = {
		val fields = getPropertyValue(IndexExtractorFactory.FIELDS_PROPERTY).asScala
		val x1Field = fields(0)
		val y1Field = if (fields.size < 4) "zero" else fields(1)
		val x2Field = if (fields.size < 4) fields(1) else fields(2)
		val y2Field = if (fields.size < 4) "zero" else fields(3)
		new LineSegmentSchemaIndexExtractor(x1Field, y1Field, x2Field, y2Field)
	}
}

/**
 * An index extractor that extracts from a record the description of a line segment
 * @param x1Var The field from which to get the X coordinate of the first point of the described line segment from a given record.
 * @param y1Var The field from which to get the Y coordinate of the first point of the described line segment from a given record.
 * @param x2Var The field from which to get the X coordinate of the second point of the described line segment from a given record.
 * @param y2Var The field from which to get the Y coordinate of the second point of the described line segment from a given record.
 */
class LineSegmentSchemaIndexExtractor (x1Var: String, y1Var: String, x2Var: String, y2Var: String)
		extends IndexExtractor()
{
	@transient lazy private val _fields = Seq(x1Var, y1Var, x2Var, y2Var)
	def fields = _fields
	def indexScheme: IndexScheme[Seq[Any]] = new CartesianSchemaIndexScheme
}

/** A constructor for a standard ip address index extractor */
class IP4VIndexExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends IndexExtractorFactory("ipv4", parent, path)
{
	// Initialize needed properties
	addProperty(IndexExtractorFactory.FIELDS_PROPERTY)

	override protected def create (): IndexExtractor = {
		val fields = getPropertyValue(IndexExtractorFactory.FIELDS_PROPERTY).asScala
		val ipField = fields(0)
		new IPv4SchemaIndexExtractor(ipField)
	}
}

/**
 * An index extractor that extracts an IP address (v4) from a record
 * @param ipField The field from which to get the IPv4 address of a given record
 */
class IPv4SchemaIndexExtractor (ipField: String) extends IndexExtractor() {
	@transient lazy private val _fields = Seq(ipField)
	def fields = _fields
	def indexScheme = new IPv4ZCurveSchemaIndexScheme
	override def getTileAnalytics[BT]: Seq[AnalysisDescription[TileData[BT], _]] =
		Seq(IPv4Analytics.getCIDRBlockAnalysis[BT](),
		    IPv4Analytics.getMinIPAddressAnalysis[BT](),
		    IPv4Analytics.getMaxIPAddressAnalysis[BT]())
}

object TimeRangeIndexExtractorFactory {
	private[datasets] val START_DATE_PROPERTY =
		new DoubleProperty("start-date", "The start of the first time period, for binned times", 0.0)
	private[datasets] val SECONDS_PER_PERIOD_PROPERTY =
		new DoubleProperty("period-length", "The length of each time period, in seconds", 60.0*60.0*24.0)
}

/** A constructor for a standard time range/cartesian point index extractor */
class TimeRangeIndexExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends IndexExtractorFactory("timerange", parent, path)
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
		new TimeRangeCartesianSchemaIndexExtractor(timeField, xField, yField, start, period)
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
class TimeRangeCartesianSchemaIndexExtractor (timeVar: String, xVar: String, yVar: String,
                                              startDate: Double, val secsPerPeriod: Double)
		extends IndexExtractor()
{
	@transient lazy private val _scheme = new TimeRangeCartesianSchemaIndexScheme(startDate, secsPerPeriod)
	@transient lazy private val _fields = Seq(timeVar, xVar, yVar)
	def fields = _fields
	def indexScheme = _scheme
}
