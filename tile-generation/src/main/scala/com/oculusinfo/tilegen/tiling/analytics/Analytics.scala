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

package com.oculusinfo.tilegen.tiling.analytics



import java.lang.{Double => JavaDouble}
import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.util.{List => JavaList}

import com.oculusinfo.binning.util.JsonUtilities
import org.json.{JSONArray, JSONObject}

import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag

import org.apache.spark.SparkContext
import org.apache.spark.Accumulator
import org.apache.spark.AccumulatorParam

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.metadata.PyramidMetaData
import scala.util.Try



/**
 * An Analytic is basically an aggregation function, to describe how to
 * aggregate the results of some analysis across bins, tiles, or anything
 * else.
 *
 * It also encapsulates some simple knowledge of default values.
 *
 * @tparam T The type of value this analytic processes or aggregates.
 */
trait Analytic[T] extends Serializable
{
	/**
	 * Used to combine two values during processing. No assumptions
	 * should be made regarding the cardinality of either
	 * value - i.e., don't assume that the first is the aggregate, and
	 * the second, the new value, or anything like that.  Either or
	 * both may be a primary or aggregate value.
	 */
	def aggregate (a: T, b: T): T

	/**
	 * The default processing value to use for an analytic group known
	 * to have no value.
	 */
	def defaultProcessedValue: T

	/**
	 * The default processing value to use for an analytic group whose
	 * value is unknown, so as to initialize it for aggregation with
	 * any known values.
	 */
	def defaultUnprocessedValue: T
}

/**
 * BinningAnalytic extends Analytic specifically for binning in tiles; it
 * allows a two-stage processing of values, with a processing type that is
 * aggregated, and converted to a result type just before writing out a final
 * value.
 *
 * There are two driving cases behind breaking the calculation into processing
 * and final components.  [1] The first case is where the result type is
 * complex, and can be represented more succinctly and efficiently durring
 * processing (such as, for instance, using an array instead of a list, or an
 * native integer instead of a JavaInt).  [2] The second case is analytics
 * (like mean or standard deviation) where different values are needed at
 * processing than are written out (for example, when calculating a mean, one
 * needs to record total number and running sum, but only writes a single
 * number)
 *
 * @tparam PROCESSING_TYPE An intermediate type used to store data
 *                         needed to produce the result type when the
 *                         analytic is complete.
 * @tparam RESULT_TYPE The final type of value to be written out as the
 *                     results of this analytic.
 */
trait BinningAnalytic[PROCESSING_TYPE, RESULT_TYPE] extends Analytic[PROCESSING_TYPE] {
	/**
	 * Finish off a processing value, converting it to a result value
	 */
	def finish (value: PROCESSING_TYPE): RESULT_TYPE
}

/**
 * A TileAnalytic extends Analytic with a few simple pices that allow values to
 * be written to metadata, both on each individual tile, and globally.
 *
 * Note that, while binning analytics have a final form to which values must be
 * converted, tile analytics all convert their values to strings for writing to
 * metadata, and therefore don't need an explicit final form type; this
 * conversion is inherent in the valueToString method.
 *
 * @tparam T The type of value this analytic processes or aggregates.
 */
trait TileAnalytic[T] extends Analytic[T] {
	/**
	 * This is ignored if the analytic is the main value used for
	 * tiling; otherwise, it will be used to label the results in
	 * tables or metadata where the result value is written.
	 */
	def name: String

	/**
	 * Convert the analytic value to a JSON representation, that can be written into tile or pyramid metadata by
	 * simple string conversion.  This method should capture everything about the analytic, including its name.
	 *
	 * @param value The collected value to be stored
	 * @param location Where the value is to be stored.  Ordinarily, most implementations will ignore this, but they may
	 *                 use it to specify only writing to pyramid metadata, or only writing to tile metadata
	 * @return The same value, converted to JSON, and wrapped in Some, or None if nothing is to be stored
	 */
	def storableValue (value: T, location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val result = new JSONObject()
		result.put(name, value)
		Some(result)
	}

	override def toString = "["+name+"]"
}
object TileAnalytic {
	/** An enum of possible locations to which to store TileAnalytic data */
	object Locations extends Enumeration {val Tile, Pyramid = Value}

	private[analytics] def topLevelMap (value: JSONObject): Map[String, String] =
		JSONObject.getNames(value).map(key => (key, value.get(key).toString)).toMap
}



/**
 * An analytic that combines two other analytics into one.
 *
 * @see CompositeAnalysisDescription
 */
class ComposedTileAnalytic[T1, T2]
	(val1: TileAnalytic[T1],
	 val2: TileAnalytic[T2])
		extends TileAnalytic[(T1, T2)]
{
	def name: String = val1.name+","+val2.name
	def aggregate (a: (T1, T2), b: (T1, T2)): (T1, T2) =
		(val1.aggregate(a._1, b._1), val2.aggregate(a._2, b._2))
	def defaultProcessedValue: (T1, T2) =
		(val1.defaultProcessedValue, val2.defaultProcessedValue)
	def defaultUnprocessedValue: (T1, T2) =
		(val1.defaultUnprocessedValue, val2.defaultUnprocessedValue)
	override def storableValue (value: (T1, T2), location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		// We want to combine the results of both our components into a single return value.
		val v1 = val1.storableValue(value._1, location)
		val v2 = val2.storableValue(value._2, location)
		val k1 = v1.map(JSONObject.getNames(_)).getOrElse(Array[String]())
		val k2 = v2.map(JSONObject.getNames(_)).getOrElse(Array[String]())
		val keys = k1.toSet ++ k2.toSet
		val result = new JSONObject()
		keys.foreach{key =>
			val value1 = v1.map(json => if (json.has(key)) Some(json.get(key)) else None).getOrElse(None)
			val value2 = v2.map(json => if (json.has(key)) Some(json.get(key)) else None).getOrElse(None)
			// Where keys don't interfere, use the key as is
			if (value1.isDefined && value2.isDefined) {
				// sadly, keys are interfering; preface them with the analytic name
				result.put(val1.name+"."+key, value1.get)
				result.put(val2.name+"."+key, value2.get)
			} else if (value1.isDefined) {
				result.put(key, value1.get)
			} else if (value2.isDefined) {
				result.put(key, value2.get)
			}
		}
		Some(result)
	}
	override def toString = "["+val1+" + "+val2+"]"
}



/**
 * An accumulator that accumulates a TileAnalytic across multiple tiles
 *
 * @param analytic An analytic defining an aggregation function to be used to
 *                 accumulate values
 *
 * @tparam T The type of value to be accumulated
 */
class AnalyticAccumulatorParam[T] (analytic: Analytic[T]) extends AccumulatorParam[T] {
	// AccumulatorParam implementation
	def zero (initialValue: T): T = analytic.defaultUnprocessedValue
	def addInPlace (a: T, b: T): T = analytic.aggregate(a, b)
}




object AnalysisDescription {
	def getStandardLevelMetadataMap (name: String, min: Int, max: Int):
			Map[String, TileIndex => Boolean] =
	{
		getGlobalOnlyMetadataMap(name) ++
		(for (level <- min to max) yield
			 (level+"."+name -> ((t: TileIndex) => level == t.getLevel)))
	}

	def getGlobalOnlyMetadataMap (name: String) :
			Map[String, TileIndex => Boolean] =
		Map("global."+name -> ((t: TileIndex) => true))


	/*
	 * Take the old metadata value and overlay the new one on top of it.
	 */
	private def combineMetaData (origValue: Any, newValue: Any): Any =
		Try{
			// Convert both inputs to Json, and overlay them
			val origJson = origValue match {
				case json: JSONObject => JsonUtilities.deepClone(json)
				case null => new JSONObject("{}")
				case _ => new JSONObject(origValue.toString())
			}
			val newJson = newValue match {
				case json: JSONObject => json
				case null => new JSONObject("{}")
				case _ => new JSONObject(newValue.toString())
			}
			JsonUtilities.overlayInPlace(origJson, newJson)
		}.getOrElse(
			// Default to simple replacement if there is any failure in overlaying.
			newValue
		)

	// Apply accumulated metadata info to actual global metadata for a pyramid
	def record[T] (analysis: AnalysisDescription[_, T], metaData: PyramidMetaData): Unit = {
		metaData.setCustomMetaData(analysis.accumulatedResults);
	}

	// Apply accumulated metadata info to an actual tile
	def record[T] (value: T, analysis: AnalysisDescription[_, T], tile: TileData[_]): Unit = {
		analysis.analytic.storableValue(value, TileAnalytic.Locations.Tile).foreach { json =>
			TileAnalytic.topLevelMap(json).foreach{case (key, value) =>
				tile.setMetaData(key, combineMetaData(tile.getMetaData(key), value))
			}
		}
	}
}
/**
 * An AnalysisDescription describes an analysis that needs to be executed
 * during the tiling process.  Both tile and data analytics use this class
 * as their basic description.
 *
 * There are three main parts to an analysis description:
 * <ol>
 * <li>The conversion function, convert, which extracts the needed values for
 * analysis from the raw data</li>
 * <li>The analytic, which describes how analytic values are treated</li>
 * <li>Accumulator-related methods, which describe how analyses are aggregated
 * across the data set.</li>
 * </ol>
 *
 * @tparam RT The raw type of data on which this analysis takes place
 * @tparam AT The type of data collected by this analysis, to be aggregated and
 *            stored.  In an AnalyticDescription, the result of analyzing a
 *            single record of type RT is a single record of type AT, and this
 *            analysis is captured by the convert method.
 */
trait AnalysisDescription[RT, AT] extends Serializable {
	val analysisTypeTag: ClassTag[AT]
	def convert: RT => AT
	def analytic: TileAnalytic[AT]
	// Add a data point to appropriate accumulators
	def accumulate (tile: TileIndex, data: AT): Unit

	// Deal with accumulators
	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit
	def accumulatedResults: JSONObject

	// Standard accumulators
	def addLevelAccumulator (sc: SparkContext, level: Int): Unit =
		addAccumulator(sc, ""+level, (test: TileIndex) => (level == test.getLevel))
	def addGlobalAccumulator (sc: SparkContext): Unit =
		addAccumulator(sc, "global", (test: TileIndex) => true)
}

/**
 * A small, simple data class to store what an analysis description needs to
 * remember about a single cross-dataset accumulator of its analysis.
 *
 * @tparam AT The analysis type being accumulated
 */
case class MetaDataAccumulatorInfo[AT] (name: String,
                                        test: TileIndex => Boolean,
                                        accumulator: Accumulator[AT]) {}

/**
 * A standard analysis description parent class for descriptions of a single,
 * monolithic analysis (as opposed to a composite analysis).
 *
 * See AnalysisDescription for descriptions of the generic type parameters.
 */
class MonolithicAnalysisDescription[RT, AT: ClassTag]
	(convertParam: RT => AT,
	 analyticParam: TileAnalytic[AT])
		extends AnalysisDescription[RT, AT]
{
	val analysisTypeTag = implicitly[ClassTag[AT]]

	def convert = convertParam

	def analytic = analyticParam

	def accumulate (tile: TileIndex, data: AT): Unit =
		accumulatorInfos.foreach(info =>
			if (info._2.test(tile))
				info._2.accumulator += data
		)

	// Deal with accumulators
	protected val accumulatorInfos = MutableMap[String, MetaDataAccumulatorInfo[AT]]()
	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit =
		// Don't add anything twice
		if (!accumulatorInfos.contains(name)) {
			val defaultValue = analytic.defaultUnprocessedValue
			val accumulator = sc.accumulator(defaultValue)(new AnalyticAccumulatorParam(analytic))
			accumulatorInfos(name) =
				new MetaDataAccumulatorInfo(name, test, accumulator)
		}

	def accumulatedResults: JSONObject = {
		val result = new JSONObject
		accumulatorInfos.map{case (key, accum) =>
			analytic.storableValue(accum.accumulator.value, TileAnalytic.Locations.Pyramid).foreach(value =>
				result.put(key, value)
			)
		}
		result
	}

	override def toString = analyticParam.toString
}

/**
 * A description of a single analysis which is not aggregated into the global
 * metadata, but only exists on tiles
 *
 * See AnalysisDescription for descriptions of the generic type parameters.
 */
class TileOnlyMonolithicAnalysisDescription[RT, AT: ClassTag]
	(convertParam: RT => AT,
	 analyticParam: TileAnalytic[AT])
		extends MonolithicAnalysisDescription[RT, AT](convertParam, analyticParam)
{
	// Ignores requests to add standard accumulators
	override def addLevelAccumulator (sc: SparkContext, level: Int): Unit = {}
	override def addGlobalAccumulator (sc: SparkContext): Unit = {}
	override def accumulatedResults: JSONObject = new JSONObject
}

/**
 * A class to combine two analyses into a single analysis.
 *
 * This is needed because, for reasons of type generification, we can
 * only pass in a single tile analytic, and a single data analytic,
 * into the binning process.
 *
 * If we actually need more than one analytic, we combine them into
 * one using this class.
 *
 * See AnalysisDescription for descriptions of the generic type parameters.
 */
class CompositeAnalysisDescription[RT, AT1: ClassTag, AT2: ClassTag]
	(analysis1: AnalysisDescription[RT, AT1],
	 analysis2: AnalysisDescription[RT, AT2])
		extends AnalysisDescription[RT, (AT1, AT2)]
{
	val analysisTypeTag = implicitly[ClassTag[(AT1, AT2)]]

	private val composedConversion = (raw: RT) => (analysis1.convert(raw), analysis2.convert(raw))
	private val composedAnalytic = new ComposedTileAnalytic(analysis1.analytic, analysis2.analytic)

	def convert = composedConversion
	def analytic = composedAnalytic
	def accumulate (tile: TileIndex, data: (AT1, AT2)): Unit = {
		analysis1.accumulate(tile, data._1)
		analysis2.accumulate(tile, data._2)
	}

	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit = {
		analysis1.addAccumulator(sc, name, test)
		analysis2.addAccumulator(sc, name, test)
	}

	def accumulatedResults: JSONObject = {
		val res1 = analysis1.accumulatedResults
		val res2 = analysis2.accumulatedResults
		JsonUtilities.overlayInPlace(res1, res2)
	}


	// Helper functions for testing purposes only
	/** Count all composed sub-components.  For testing purposes only. */
	def countComponents: Int = {
		val count1: Int = if (analysis1.isInstanceOf[CompositeAnalysisDescription[_, _, _]])
			analysis1.asInstanceOf[CompositeAnalysisDescription[_, _, _]].countComponents
		else 1
		val count2: Int = if (analysis2.isInstanceOf[CompositeAnalysisDescription[_, _, _]])
			analysis2.asInstanceOf[CompositeAnalysisDescription[_, _, _]].countComponents
		else 1
		count1 + count2
	}

	/** Get the nth composed sub-component.  For testing purposes only. */
	def getComponent (n: Int): AnalysisDescription[RT, _] = {
		getComponentInternal(n)._1.get
	}

	protected def getComponentInternal (n: Int): (Option[AnalysisDescription[RT, _]], Int) = {
		val postFirst =
			if (analysis1.isInstanceOf[CompositeAnalysisDescription[RT, _, _]])
				analysis1.asInstanceOf[CompositeAnalysisDescription[RT, _, _]].getComponentInternal(n)
			else if (0 == n) (Some(analysis1), -1)
			else (None, n - 1)

		if (postFirst._1.isDefined) postFirst
		else {
			val n2 = postFirst._2
			val postSecond =
				if (analysis2.isInstanceOf[CompositeAnalysisDescription[RT, _, _]])
					analysis2.asInstanceOf[CompositeAnalysisDescription[RT, _, _]].getComponentInternal(n2)
				else if (0 == n2) (Some(analysis2), -1)
				else (None, n2 - 1)
			postSecond
		}
	}

	override def toString = "["+analysis1+","+analysis2+"]"
}

/**
 * A class (and companion object) to take an analysis of bin values and convert
 * it into an analysis of tiles.
 */
object AnalysisDescriptionTileWrapper {
	def acrossTile[BT, AT] (convertFcn: BT => AT,
	                        analytic: TileAnalytic[AT]): TileData[BT] => AT =
	{
		tile: TileData[BT] => {
			val index = tile.getDefinition
				(for (x <- 0 until index.getXBins;
				      y <- 0 until index.getYBins)
				 yield convertFcn(tile.getBin(x, y))
				).reduce((a, b) => analytic.aggregate(a, b))
		}
	}
}
class AnalysisDescriptionTileWrapper[RT, AT: ClassTag]
	(convert: RT => AT,
	 analytic: TileAnalytic[AT])
		extends MonolithicAnalysisDescription[TileData[RT], AT](
	AnalysisDescriptionTileWrapper.acrossTile(convert, analytic),
	analytic)
{
}



// /////////////////////////////////////////////////////////////////////////////
// Some standard analytic functions
//


/**
 * The Custom Metadata analytic is a dummy tile analytic that does not actually
 * write anything to tiles.  It's only purpose is to help
 * CustomGlobalMetadata fulfill its interface without having any side-effects.
 *
 */
class CustomMetadataAnalytic extends TileAnalytic[String]
{
	def aggregate (a: String, b: String): String = a
	def defaultProcessedValue: String = ""
	def defaultUnprocessedValue: String = ""
	def name: String = "VariableSeries"
	override def storableValue (value: String, location: TileAnalytic.Locations.Value): Option[JSONObject] =
		if (TileAnalytic.Locations.Pyramid == location) super.storableValue(value, location) else None
}
/**
 * A very simply tile analytic that just writes custom metadata directly to the tile set
 * metadata, and no where else.
 *
 * @tparam T The raw data type of input records.  Nothing in this analytic uses
 *           this type, it just must match the dataset.
 */
class CustomGlobalMetadata[T] (customData: Map[String, Object])
		extends AnalysisDescription[T, String]
{
	val analysisTypeTag = implicitly[ClassTag[String]]
	def convert: T => String = (raw: T) => ""
	def analytic: TileAnalytic[String] = new CustomMetadataAnalytic
	def accumulate (tile: TileIndex, data: String): Unit = {}

	// Global metadata needs no accumulators - it doesn't actually have any data.
	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit = {}
	def accumulatedResults: JSONObject = {
		customData.foldLeft(new JSONObject()) { (res, curr) =>
			curr match {
				case (key: String, value: JSONObject) => res.put(key, value)
				case (key: String, value: Object) if JsonUtilities.isJSON(value.toString) => res.put(key, new JSONObject(value.toString))
				case _ => res.put(curr._1, curr._2)
			}
		}
	}
}
