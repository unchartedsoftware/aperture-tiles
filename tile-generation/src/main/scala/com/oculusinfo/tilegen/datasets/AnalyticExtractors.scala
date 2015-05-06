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
import com.oculusinfo.factory.providers.FactoryProvider
import com.oculusinfo.factory.providers.AbstractFactoryProvider
import com.oculusinfo.factory.EmptyFactory


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

case class FieldList (fields: Seq[String]) {}
class FieldListFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ConfigurableFactory[FieldList](classOf[FieldList], parent, path)
{
	import AnalyticFactory._

	addProperty(FIELDS_PROPERTY)

	override protected def create: FieldList = {
		FieldList(getPropertyValue(FIELDS_PROPERTY))
	}
}

abstract class AnalyticFactory (name: String, parent: ConfigurableFactory[_], path: JavaList[String])
		extends ConfigurableFactory[AnalysisDescription[_, _]] (name, classOf[AnalysisDescription[_, _]], parent, path)
{
	addChildFactory(new FieldListFactory(this, List[String]().asJava))
}

class ParameterlessAnalyticFactory (name: String, parent: ConfigurableFactory[_], path: JavaList[String])
		extends AnalyticFactory(name, parent, path)
{
	import AnalyticFactory._
	addProperty(ANALYTIC_TYPE_PROPERTY)

	override protected def create: AnalysisDescription[_, _] = {
		val factoryTypeName = getPropertyValue(ANALYTIC_TYPE_PROPERTY)
		Class.forName(factoryTypeName).newInstance().asInstanceOf[AnalysisDescription[_, _]]
	}
}

abstract class AnalyticFactoryProvider extends AbstractFactoryProvider[AnalysisDescription[_, _]] {}

class DefaultAnalyticFactoryProvider extends AnalyticFactoryProvider {
	def createFactory(name: String,
	                  parent: ConfigurableFactory[_],
	                  path: JavaList[String]): ConfigurableFactory[AnalysisDescription[_, _]] = {
		new ParameterlessAnalyticFactory(name, parent, path)
	}
}


/**
 * A factory to produce analytic extractors
 */
object AnalyticExtractorFactory {
	val ANALYTIC_FACTORY_PROPERTY = new StringProperty("factory",
	                                                   "A factory provider type with a no-arg constructor.  The factory "+
		                                                   "should inherit from AnalyticFactoryProvider.",
	                                                   classOf[DefaultAnalyticFactoryProvider].getName)
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
		val tileAnalytics = {
			val tileAnalyticConfig = getPropertyValue(TILE_ANALYTIC_PROPERTY)

			(0 until tileAnalyticConfig.length()).map(n =>
				{
					// Get the name of a provider of the factory type we need
					val typeSource = new EmptyFactory(null, List[String]().asJava, ANALYTIC_FACTORY_PROPERTY)
					typeSource.readConfiguration(tileAnalyticConfig.getJSONObject(n))
					val providerType = typeSource.getPropertyValue(ANALYTIC_FACTORY_PROPERTY)

					// Create our analytic factory
					val provider = Class.forName(providerType).newInstance().asInstanceOf[FactoryProvider[AnalysisDescription[_, _]]]
					val factory = provider.createFactory(null, List[String]().asJava)
					factory.readConfiguration(tileAnalyticConfig.getJSONObject(n))

					// Create our analytic
					factory.produce(classOf[AnalysisDescription[_, _]]).asInstanceOf[AnalysisDescription[TileData[_], _]]
				}
			)
		}
		val dataAnalytics = {
			val dataAnalyticConfig = getPropertyValue(DATA_ANALYTICS_PROPERTY)

			(0 until dataAnalyticConfig.length()).map(n =>
				{
					// Get the name of a provider of the factory type we need
					val typeSource = new EmptyFactory(null, List[String]().asJava, ANALYTIC_FACTORY_PROPERTY)
					typeSource.readConfiguration(dataAnalyticConfig.getJSONObject(n))
					val providerType = typeSource.getPropertyValue(ANALYTIC_FACTORY_PROPERTY)

					// Create our analytic factory
					val provider = Class.forName(providerType).newInstance().asInstanceOf[FactoryProvider[AnalysisDescription[_, _]]]
					val factory = provider.createFactory(null, List[String]().asJava)
					factory.readConfiguration(dataAnalyticConfig.getJSONObject(n))

					// Create our analytic
					val analytic = factory.produce(classOf[AnalysisDescription[_, _]]).asInstanceOf[AnalysisDescription[Seq[Any], _]]
					// Find the fields it needs
					val fields = factory.produce(classOf[FieldList]).fields
					// return them both
					(analytic, fields)
				}
			)
		}
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
	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit =
		base.addAccumulator(sc, name, test)
	def accumulatedResults = base.accumulatedResults
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
