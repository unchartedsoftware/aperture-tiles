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
import java.util
import java.util.{List => JavaList}
import java.util.ArrayList


import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.factory.properties.{JSONArrayProperty, StringProperty, ListProperty}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.SeqWrapper
import scala.collection.mutable.Buffer
import scala.reflect.ClassTag

import org.apache.spark.SparkContext

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.factory.ConfigurableFactory
import com.oculusinfo.tilegen.tiling.analytics._
import com.oculusinfo.tilegen.util.{TypeConversion, ExtendedNumeric, NumericallyConfigurableFactory, PropertiesWrapper}


/**
 * Small case class to allow factories to return no analytics at all with proper type tags
 */
private[datasets] class AnalysisWithTag[BT, AT: ClassTag] (val analysis: Option[AnalysisDescription[BT, AT]]) {
	val analysisTypeTag: ClassTag[AT] = implicitly[ClassTag[AT]]
}

/**
 * A factory to produce a single analytic
 */
object AnalyticFactory {
	val ANALYTIC_TYPE_PROPERTY = new StringProperty("analytic",
	                                                "The full class name of the analysis description to create.  The "+
		                                                "class named must have a no-argument constructor, and be "+
		                                                "of type AnalysisDescription[Seq[Any], _] for data "+
		                                                "analytics, or AnalysisDescription[TileData[BT], _] for "+
		                                                "tile analytics (where BT is the bin type of the tile).",
	                                                "")
	val FIELDS_PROPERTY = new ListProperty(new StringProperty("", "", ""),
	                                       "fields",
	                                       "Fields relevant to the current analytic")

}
class AnalyticFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ConfigurableFactory[(AnalysisDescription[_, _], Seq[String])] (
	classOf[(AnalysisDescription[_, _], Seq[String])], parent, path)
{
	import AnalyticFactory._

	addProperty(ANALYTIC_TYPE_PROPERTY)
	addProperty(FIELDS_PROPERTY)

	override protected def create: (AnalysisDescription[_, _], Seq[String]) = {
		val factoryTypeName = getPropertyValue(ANALYTIC_TYPE_PROPERTY)
		val analytic = Class.forName(factoryTypeName).newInstance().asInstanceOf[AnalysisDescription[_, _]]
		val fields = getPropertyValue(FIELDS_PROPERTY)

		(analytic, fields)
	}
}

/**
 * A factory to produce analytic extractors
 */
object AnalyticExtractorFactory {
	val DATA_ANALYTICS_PROPERTY = new JSONArrayProperty("data", "A list of data analytic types", "[]")
	val TILE_ANALYTIC_PROPERTY = new JSONArrayProperty("tile", "A list of tile analytic types", "[]")
}
class AnalyticExtractorFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ConfigurableFactory[AnalyticExtractor](classOf[AnalyticExtractor], parent, path, true)
{
	import AnalyticExtractorFactory._
	addProperty(DATA_ANALYTICS_PROPERTY)
	addProperty(TILE_ANALYTIC_PROPERTY)

	override protected def create(): AnalyticExtractor = {
		val tileAnalyticConfig = getPropertyValue(TILE_ANALYTIC_PROPERTY)
		val tileAnalytics =
			(0 until tileAnalyticConfig.length()).map(n =>
				{
					val factory = new AnalyticFactory(null, new util.ArrayList[String]())
					factory.readConfiguration(tileAnalyticConfig.getJSONObject(n))
					val (analytic, fields) = factory.produce(classOf[(AnalysisDescription[_, _], Seq[String])])
					analytic.asInstanceOf[AnalysisDescription[TileData[_], _]]
				}
			)
		val dataAnalyticConfig = getPropertyValue(DATA_ANALYTICS_PROPERTY)
		val dataAnalytics =
			(0 until dataAnalyticConfig.length()).map(n =>
				{
					val factory = new AnalyticFactory(null, new util.ArrayList[String]())
					factory.readConfiguration(dataAnalyticConfig.getJSONObject(n))
					val (analytic, fields) = factory.produce(classOf[(AnalysisDescription[_, _], Seq[String])])
					(analytic.asInstanceOf[AnalysisDescription[Seq[Any], _]], fields)
				}
			)
		new AnalyticExtractor(tileAnalytics, dataAnalytics)
	}
}

/**
 * A wrapper analytic to wrap around each data analytic so as to limit its input to its relevant fields
 * @param base The base analytic we wrap
 * @param startIndex The index of the first field used by our base analytic in the list of all fields used by any
 *                   data analytic
 * @param endIndex The index of the last field used by our base analytic in the list of all fields used by any data
 *                 analytic
 * @tparam AT The analytic type of both us and our base analytic
 */
class DataAnalyticWrapper[AT: ClassTag] (private[datasets] var base: AnalysisDescription[Seq[Any], AT], startIndex: Int, endIndex: Int)
		extends AnalysisDescription[Seq[Any], AT]
{
	val analysisTypeTag: ClassTag[AT] = base.analysisTypeTag

	// Methods for which we actually do something to the input before passing it to our base analytic
	def convert: Seq[Any] => AT = allFields => base.convert((startIndex to endIndex).map(allFields(_)))

	// Simple pass-through methods into our base analytic
	def analytic: TileAnalytic[AT] = base.analytic
	def accumulate (tile: TileIndex, data: AT): Unit = base.accumulate(tile, data)
	def toMap: Map[String, Any] = base.toMap
	def applyTo (metaData: PyramidMetaData): Unit = base.applyTo(metaData)
	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit =
		base.addAccumulator(sc, name, test)
}
class AnalyticExtractor (_tileAnalytics: Seq[AnalysisDescription[TileData[_], _]],
                         _dataAnalytics: Seq[(AnalysisDescription[Seq[Any], _], Seq[String])]) {

	/**
	 * An internal data class to store relevant information about data analytics.  The main purpose of this class is
	 * to encapsulate field information with each data analytic, and to associate the field information wiht the
	 * analytic.
	 *
	 * @param analytic The analytic in question
	 * @param fields The names of the fields associated with this analytic
	 * @param startIndex The index of the first field relevant to this analytic in the list of all fields used by data analytics
	 * @param endIndex The index of the last field relevant to this analytic in the list of all fields used by data analytics
	 */
	private case class DataAnalyticInfo (analytic: AnalysisDescription[Seq[Any], _], fields: Seq[String],
	                                     startIndex: Int, endIndex: Int) {}
	private val dataAnalyticInfos = {
		var index = 0
		_dataAnalytics.map{case (analytic, fields) =>
			{
				val numFields = fields.size
				val start = index
				val end = start + numFields - 1
				index = index + numFields
				new DataAnalyticInfo(new DataAnalyticWrapper(analytic, start, end), fields, start, end)
			}
		}
	}

	private def consolidateAnalytics[T] (analytics: Seq[AnalysisDescription[T, _]]): AnalysisWithTag[T, _] =
		if (analytics.isEmpty) new AnalysisWithTag[T, Int](None)
		else new AnalysisWithTag(Some(analytics.reduce((a, b) => new CompositeAnalysisDescription(a, b))))

	/**
	 * Get relevant tile analytics
	 *
	 * These include externally defined default tile analytics
	 *
	 * @param defaultTileAnalytics Externally defined, default tile analytics, to incorporate into the list of specifically defined custom analytics.
	 * @return A single tile analytic composed of all that are passed in.
	 */
	def tileAnalytics[BT] (defaultTileAnalytics: Seq[AnalysisDescription[TileData[BT], _]]): AnalysisWithTag[TileData[BT], _] = {
		val allAnalytics = defaultTileAnalytics ++ _tileAnalytics
		consolidateAnalytics(allAnalytics.map(_.asInstanceOf[AnalysisDescription[TileData[BT], _]]))
	}

	/**
	 * Get relevant data analytics
	 *
	 * @return A single data analytic composed of all passed-in data analytics
	 */
	def dataAnalytics = consolidateAnalytics(dataAnalyticInfos.map(_.analytic))

	/**
	 * A list of fields used by analytics.  All these are used by data analytics, of course, not by tile analytics,
	 * which just use the finished tile as input.
	 *
	 * @return A consolidated list of all needed fields, in the order in which they are needed.
	 */
	def fields = dataAnalyticInfos.flatMap(_.fields)
}



// /////////////////////////////////////////////////////////////////////////////
// Everything below this line refers to the old, defunct CSV-based dataset
//
object CSVDataAnalyticExtractor {
	def consolidate[IT, PT] (properties: PropertiesWrapper,
	                         dataAnalytics: Seq[AnalysisDescription[(IT, PT), _]]):
			AnalysisWithTag[(IT, PT), _] = {
		// Load up any data analytics specified in the properties file, and add them to
		// the passed-in list of standard data analytics to run.
		val classLoader = classOf[AnalysisDescription[_, _]].getClassLoader
		val customAnalyses = properties.getStringPropSeq("oculus.binning.analytics.data",
		                                                 "A list of custom data analysis descriptions to "+
			                                                 "run on the data")
		val allAnalytics: Seq[AnalysisDescription[(IT, PT), _]] =
			customAnalyses.map(className =>
				classLoader.loadClass(className).newInstance
					.asInstanceOf[AnalysisDescription[(IT, PT), _]]
			) ++ dataAnalytics

		// Combine all data analytics into one, for the binner
		if (allAnalytics.isEmpty) {
			new AnalysisWithTag[(IT, PT), Int](None)
		} else {
			new AnalysisWithTag(Some(allAnalytics.reduce((a, b) =>
				                         new CompositeAnalysisDescription(a, b))))
		}
	}
}

object CSVTileAnalyticExtractor {
	def consolidate[IT, PT, BT] (properties: PropertiesWrapper,
	                             tileAnalytics: Seq[AnalysisDescription[TileData[BT], _]]):
			AnalysisWithTag[TileData[BT], _] =
	{
		// Load up any tile analytics specified in the properties file, and add them to
		// the passed-in list of standard tile analytics to run.
		val classLoader = classOf[AnalysisDescription[_, _]].getClassLoader
		val customAnalyses = properties.getStringPropSeq("oculus.binning.analytics.tile",
		                                                 "A list of custom tile analytic descriptions "+
			                                                 "to run on the tiles produced")
		val allAnalytics: Seq[AnalysisDescription[TileData[BT], _]] =
			customAnalyses.map(className =>
				classLoader.loadClass(className).newInstance
					.asInstanceOf[AnalysisDescription[TileData[BT], _]]
			) ++ tileAnalytics

		// Combine all tile analytics into one, for the binner
		if (allAnalytics.isEmpty) {
			new AnalysisWithTag[TileData[BT], Int](None)
		} else {
			new AnalysisWithTag(Some(allAnalytics.reduce((a, b) =>
				                         new CompositeAnalysisDescription(a, b))))
		}
	}
}
