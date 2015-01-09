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

import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag

import org.apache.spark.SparkContext
import org.apache.spark.Accumulator
import org.apache.spark.AccumulatorParam

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.metadata.PyramidMetaData



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
	 * Convert an analytic value to a string representation thereof.
	 * This is a one-way conversion, and is used to store values in
	 * tile metadata.
	 */
	def valueToString (value: T): String = value.toString

	/**
	 * Convert a value to a property map to be inserted into metadata.
	 * 
	 * The default behavior is simply to map the analytic name to the value.
	 * 
	 * Overriding this will the other functions in this trait to be made 
	 * irrelevant; we should probably break it up into a TileAnalytic with 
	 * only this function (unimplemented), and a StandardTileAnalytic with 
	 * the rest
	 */
	def toMap (value: T): Map[String, Any] =
		Map(name -> valueToString(value))

	override def toString = "["+name+"]"
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
	override def toMap (value: (T1, T2)): Map[String, Any] =
		val1.toMap(value._1) ++ val2.toMap(value._2)
	override def toString = "["+val1+" + "+val2+"]"
}



/**
 * An accumulator that accumulates a TileAnalytic across multiple tiles
 * 
 * @param analytic An analytic defining an aggregation function to be used to 
 *                 accumulate values
 * @param filter A filter to be used with this accumulator to define in which 
 *               tiles it is insterested
 * 
 * @tparam T The type of value to be accumulated
 */
class AnalyticAccumulatorParam[T] (analytic: Analytic[T]) extends AccumulatorParam[T] {
	// AccumulatorParam implementation
	def zero (initialValue: T): T = analytic.defaultUnprocessedValue
	def addInPlace (a: T, b: T): T = analytic.aggregate(a, b)
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
}
trait AnalysisDescription[RT, AT] {
	val analysisTypeTag: ClassTag[AT]
	def convert: RT => AT
	def analytic: TileAnalytic[AT]
	// Add a data point to appropriate accumulators
	def accumulate (tile: TileIndex, data: AT): Unit
	// Get accumulatoed metadata info in a form that can be applied
	// directly to metadata.
	def toMap: Map[String, Any]
	// Apply accumulated metadata info to actual metadata
	def applyTo (metaData: PyramidMetaData): Unit

	// Deal with accumulators
	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit
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
		with Serializable
{
	val analysisTypeTag = implicitly[ClassTag[AT]]

	def convert = convertParam

	def analytic = analyticParam

	def accumulate (tile: TileIndex, data: AT): Unit =
		accumulatorInfos.foreach(info =>
			if (info._2.test(tile))
				info._2.accumulator += data
		)

	def toMap: Map[String, Any] = accumulatorInfos.map(info =>
		analytic.toMap(info._2.accumulator.value).map{case (k, v) => (info._2.name+"."+k, v)}
	).reduceOption(_ ++ _).getOrElse(Map[String, Any]())

	def applyTo (metaData: PyramidMetaData): Unit = {
		toMap.foreach{case (key, value) =>
			{
				metaData.setCustomMetaData(value, key.split("\\.") :_*)
			}
		}
	}

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
		with Serializable
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
	def toMap: Map[String, Any] = analysis1.toMap ++ analysis2.toMap
	def applyTo (metaData: PyramidMetaData): Unit = {
		analysis1.applyTo(metaData)
		analysis2.applyTo(metaData)
	}

	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit = {
		analysis1.addAccumulator(sc, name, test)
		analysis2.addAccumulator(sc, name, test)
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
	override def toMap (value: String): Map[String, Any] = Map[String, String]()
}
/**
 * A very simply tile analytic that just writes custom metadata directly to the tile set 
 * metadata, and no where else.
 * 
 * @tparam T The raw data type of input records.  Nothing in this analytic uses 
 *           this type, it just must match the dataset.
 */
class CustomGlobalMetadata[T] (customData: Map[String, Object])
		extends AnalysisDescription[T, String] with Serializable
{
	val analysisTypeTag = implicitly[ClassTag[String]]
	def convert: T => String = (raw: T) => ""
	def analytic: TileAnalytic[String] = new CustomMetadataAnalytic
	def accumulate (tile: TileIndex, data: String): Unit = {}
	// This is used to apply the analytic to tiles; we don't want anything to happen there
	def toMap: Map[String, Any] = Map[String, Any]()
	// This is used to apply the analytic to metadata; here's where we want stuff used.
	def applyTo (metaData: PyramidMetaData): Unit =
		customData.map{case (key, value) =>
			metaData.setCustomMetaData(value, key)
		}
	// Global metadata needs no accumulators - it doesn't actually have any data.
	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit = {}
}
