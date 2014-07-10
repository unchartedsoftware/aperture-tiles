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

package com.oculusinfo.tilegen.tiling



import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

import org.apache.spark.SparkContext
import org.apache.spark.Accumulator
import org.apache.spark.AccumulatorParam

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.metadata.PyramidMetaData
import com.oculusinfo.binning.util.Pair


/**
 * This class describes any the intermediate and final results of any
 * analysis over tiles, bins, or the raw data from which they are
 * derived.
 * 
 * This type is broken out into two sub-types - a processing and a
 * results type.  There are two driving cases behind this.  [1] Tnhe
 * first case is where the result type is complex, and can be
 * represented more succinctly and efficiently durring processing
 * (such as, for instance, using an array instead of a list, or an
 * native integer instead of a JavaInt).  [2] The second case is
 * analytics like mean, or standard deviation, where different values
 * are needed at processing than are written out (total number and
 * running sum vs. actual mean, for instance).
 * 
 * @param PROCESSING_TYPE An intermediate type used to store data
 *                        needed to produce the result type when the
 *                        analytic is complete.
 * @param RESULT_TYPE The final type of value to be written out as the
 *                    results of this analytic.
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

trait BinningAnalytic[PROCESSING_TYPE, RESULT_TYPE] extends Analytic[PROCESSING_TYPE] {
	/**
	 * Finish off a processing value, converting it to a result value
	 */
	def finish (value: PROCESSING_TYPE): RESULT_TYPE
}

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
	 * Convert a value to a property map, indexed by our name
	 */
	def toMap (value: T): Map[String, String] =
		Map(name -> valueToString(value))

	override def toString = "["+name+"]"
}



/** Analytic value combining two other analytic values */
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
	override def toMap (value: (T1, T2)): Map[String, String] =
		val1.toMap(value._1) ++ val2.toMap(value._2)
	override def toString = "["+val1+" + "+val2+"]"
}

/**
 * An accumulator that accumulates a TileAnalytic across multiple tiles
 * 
 * @param analytic
 * @param filter A filter to be used with this accumulator to define in which 
 *               tiles it is insterested
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
}



trait AnalysisDescription[RT, AT] {
	val analysisTypeTag: ClassTag[AT]
	def convert: RT => AT
	def analytic: TileAnalytic[AT]
	// Add a data point to appropriate accumulators
	def accumulate (tile: TileIndex, data: AT): Unit
	// Get accumulatoed metadata info in a form that can be applied
	// directly to metadata.
	def toMap: Map[String, String]
	// Apply accumulated metadata info to actual metadata
	def applyTo (metaData: PyramidMetaData): Unit
	// Reset metadata accumulators
	def resetAccumulators (sc: SparkContext): Unit
	def copy: AnalysisDescription[RT, AT]
}

class MetaDataAccumulatorInfo[AT] (@transient sc: SparkContext,
                                   val name: String,
                                   val test: TileIndex => Boolean,
                                   newAccum: SparkContext => Accumulator[AT])
		extends Serializable
{
	private var privateAccumulator: Accumulator[AT] = newAccum(sc)
	def reset (sc: SparkContext): Unit =
		privateAccumulator = newAccum(sc)
	def accumulator = privateAccumulator
}

class MonolithicAnalysisDescription[RT, AT: ClassTag]
	(@transient sc: SparkContext,
	 convertParam: RT => AT,
	 analyticParam: TileAnalytic[AT],
	 globalMetaData: Map[String, TileIndex => Boolean])
		extends AnalysisDescription[RT, AT]
		with Serializable
{
	val analysisTypeTag = implicitly[ClassTag[AT]]

	private val accumulatorInfos = globalMetaData.map{case (name, test) =>
		new MetaDataAccumulatorInfo(sc, name, test,
		                            (lsc: SparkContext) => lsc.accumulator(
			                            analytic.defaultUnprocessedValue
		                            )(new AnalyticAccumulatorParam(analytic)))
	}


	def convert = convertParam

	def analytic = analyticParam

	def accumulate (tile: TileIndex, data: AT): Unit =
		accumulatorInfos.foreach(info =>
			if (info.test(tile))
				info.accumulator += data
		)

	def toMap: Map[String, String] = accumulatorInfos.map(info =>
		analytic.toMap(info.accumulator.value).map{case (k, v) => (info.name+"."+k, v)}
	).reduceOption(_ ++ _).getOrElse(Map[String, String]())

	def applyTo (metaData: PyramidMetaData): Unit = {
		toMap.foreach{case (key, value) =>
			{
				metaData.setCustomMetaData(value, key.split("\\.") :_*)
			}
		}
	}

	def resetAccumulators (sc: SparkContext): Unit = accumulatorInfos.map(_.reset(sc))

	def copy = new MonolithicAnalysisDescription(sc, convert, analytic, globalMetaData)

	override def toString = analyticParam.toString
}

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
	def toMap: Map[String, String] = analysis1.toMap ++ analysis2.toMap
	def applyTo (metaData: PyramidMetaData): Unit = {
		analysis1.applyTo(metaData)
		analysis2.applyTo(metaData)
	}
	def resetAccumulators (sc: SparkContext): Unit = {
		analysis1.resetAccumulators(sc)
		analysis2.resetAccumulators(sc)
	}
	def copy = new CompositeAnalysisDescription(analysis1, analysis2)
	override def toString = "["+analysis1+","+analysis2+"]"
}

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
	(sc: SparkContext,
	 convert: RT => AT,
	 analytic: TileAnalytic[AT],
	 globalMetaData: Map[String, TileIndex => Boolean])
		extends MonolithicAnalysisDescription[TileData[RT], AT](
	sc,
	AnalysisDescriptionTileWrapper.acrossTile(convert, analytic),
	analytic,
	globalMetaData)
{
}



// /////////////////////////////////////////////////////////////////////////////
// Some standard analytic functions
//
class SumDoubleAnalytic extends Analytic[Double] {
	def aggregate (a: Double, b: Double): Double = a + b
	def defaultProcessedValue: Double = 0.0
	def defaultUnprocessedValue: Double = 0.0
}

class MinimumDoubleAnalytic extends Analytic[Double] {
	def aggregate (a: Double, b: Double): Double = a min b
	def defaultProcessedValue: Double = 0.0
	def defaultUnprocessedValue: Double = Double.MaxValue
}
class MinimumDoubleTileAnalytic extends MinimumDoubleAnalytic with TileAnalytic[Double] {
	def name = "minimum"
}

class MaximumDoubleAnalytic extends Analytic[Double] {
	def aggregate (a: Double, b: Double): Double = a max b
	def defaultProcessedValue: Double = 0.0
	def defaultUnprocessedValue: Double = Double.MinValue
}
class MaximumDoubleTileAnalytic extends MaximumDoubleAnalytic with TileAnalytic[Double] {
	def name = "maximum"
}

class SumLogDoubleAnalytic(base: Double = math.exp(1.0)) extends Analytic[Double] {
	def aggregate (a: Double, b: Double): Double =
		math.log(math.pow(base, a) + math.pow(base, b))/math.log(base)
	def defaultProcessedValue: Double = 0.0
	def defaultUnprocessedValue: Double = Double.NegativeInfinity
}

trait StandardDoubleBinningAnalytic extends BinningAnalytic[Double, JavaDouble] {
	def finish (value: Double): JavaDouble = new JavaDouble(value)
}

abstract class StandardDoubleArrayAnalytic extends Analytic[Seq[Double]] {
	def aggregateElements (a: Double, b: Double): Double

	def aggregate (a: Seq[Double], b: Seq[Double]): Seq[Double] = {
		val alen = a.length
		val blen = b.length
		val len = alen max blen
		Range(0, len).map(n =>
			{
				if (n < alen && n < blen) aggregateElements(a(n), b(n))
				else if (n < alen) a(n)
				else b(n)
			}
		)
	}
	def defaultProcessedValue: Seq[Double] = Seq[Double]()
	def defaultUnprocessedValue: Seq[Double] = Seq[Double]()
}
trait StandardDoubleArrayTileAnalytic extends TileAnalytic[Seq[Double]] {
	override def valueToString (value: Seq[Double]): String = value.mkString("[", ",", "]")
}

class SumDoubleArrayAnalytic extends StandardDoubleArrayAnalytic {
	def aggregateElements (a: Double, b: Double): Double = a + b
}
class MinimumDoubleArrayAnalytic extends StandardDoubleArrayAnalytic {
	def aggregateElements (a: Double, b: Double): Double = a min b
}
class MaximumDoubleArrayAnalytic extends StandardDoubleArrayAnalytic {
	def aggregateElements (a: Double, b: Double): Double = a max b
}

trait StandardDoubleArrayBinningAnalytic extends BinningAnalytic[Seq[Double],
                                                                 JavaList[JavaDouble]] {
	def finish (value: Seq[Double]): JavaList[JavaDouble] =
		value.map(v => new JavaDouble(v)).asJava
}

/**
 * Standard string score ordering
 * 
 * @param aggregationLimit A pair whose first element is the number of elements 
 *                         to keep when aggregating, and whose second element 
 *                         is a sorting function by which to sort them.
 */
class StringScoreAnalytic
	(aggregationLimit: Option[Int] = None,
	 order: Option[((String, Double), (String, Double)) => Boolean] = None)
		extends Analytic[Map[String, Double]]
{
	def aggregate (a: Map[String, Double],
	               b: Map[String, Double]): Map[String, Double] = {
		val combination =
			(a.keySet union b.keySet).map(key =>
				key -> (a.getOrElse(key, 0.0) + b.getOrElse(key, 0.0))
			)
		val sorted: Iterable[(String, Double)] = order.map(fcn =>
			combination.toList.sortWith(fcn)
		).getOrElse(combination)

		aggregationLimit.map(limit =>
			sorted.take(limit)
		).getOrElse(sorted)
			.toMap
	}
	def defaultProcessedValue: Map[String, Double] = Map[String, Double]()
	def defaultUnprocessedValue: Map[String, Double] = Map[String, Double]()
}

class StandardStringScoreBinningAnalytic
	(aggregationLimit: Option[Int] = None,
	 order: Option[((String, Double), (String, Double)) => Boolean] = None,
	 storageLimit: Option[Int] = None)
		extends StringScoreAnalytic(aggregationLimit, order)
		with BinningAnalytic[Map[String, Double],
		                     JavaList[Pair[String, JavaDouble]]]
{
	def finish (value: Map[String, Double]): JavaList[Pair[String, JavaDouble]] = {
		val valueSeq =
			order
				.map(fcn => value.toSeq.sortWith(fcn))
				.getOrElse(value.toSeq)
				.map(p => new Pair(p._1, new JavaDouble(p._2)))
		storageLimit
			.map(valueSeq.take(_))
			.getOrElse(valueSeq)
			.asJava
	}
}

trait StandardStringScoreTileAnalytic extends TileAnalytic[Map[String, Double]] {
	override def valueToString (value: Map[String, Double]): String = value.map(p => "\""+p._1+"\":"+p._2).mkString("[", ",", "]")
}

class CategoryValueAnalytic(categoryNames: Seq[String])
		extends Analytic[Seq[Double]]
{
	def aggregate (a: Seq[Double], b: Seq[Double]): Seq[Double] =
		Range(0, a.length max b.length).map(i =>
			{
				def getOrElse(value: Seq[Double], index: Int, default: Double): Double =
					value.applyOrElse(index, (n: Int) => default)
				getOrElse(a, i, 0.0) + getOrElse(b, i, 0.0)
			}
		)
	def defaultProcessedValue: Seq[Double] = Seq[Double]()
	def defaultUnprocessedValue: Seq[Double] = Seq[Double]()
}
class CategoryValueBinningAnalytic(categoryNames: Seq[String])
		extends CategoryValueAnalytic(categoryNames)
		with BinningAnalytic[Seq[Double],
		                     JavaList[Pair[String, JavaDouble]]]
{
	def finish (value: Seq[Double]): JavaList[Pair[String, JavaDouble]] = {
		Range(0, value.length min categoryNames.length).map(i =>
			new Pair[String, JavaDouble](categoryNames(i), value(i))
		).toSeq.asJava
	}
}

/**
 * This analytic stores the CIDR block represented by a given tile.
 */
object IPv4CIDRBlockAnalysis extends Serializable {
	def convertParam (pyramid: TilePyramid)(tile: TileData[_]): String = {
		val index = tile.getDefinition()
		// The CIDR block level is just our zoom level.
		val level = index.getLevel()
		// Figure out the IP address of our lower left corner
		val bounds = pyramid.getTileBounds(index)
		val x = bounds.getMinX().toLong
		val y = bounds.getMinY().toLong
		val yExpand = Range(0, 16).map(i => ((y >> i) & 0x1L) << (2*i+1)).reduce(_ + _)
		val xExpand = Range(0, 16).map(i => ((x >> i) & 0x1L) << (2*i  )).reduce(_ + _)
		val ipAddress = xExpand + yExpand
		// Determine the CIDR block from the IP address and level
		val blockAddress = ((ipAddress >> (32-level)) & 0xffffffffL) << (32-level)
		(    (0xff & (blockAddress >> 24))+"."+
			 (0xff & (blockAddress >> 16))+"."+
			 (0xff & (blockAddress >> 8))+"."+
			 (0xff & blockAddress)+"/"+level)
	}

	/**
	 * Get an analysis description for an analysis that stores the CIDR block 
	 * of an IPv4-indexed tile, using the standard IPv4 tile pyramid.
	 */
	def getDescription[BT] (sc: SparkContext): AnalysisDescription[TileData[BT], String] =
		getDescription(sc, new AOITilePyramid(0, 0, 0xffffL.toDouble, 0xffffL.toDouble))

	/**
	 * Get an analysis description for an analysis that stores the CIDR block 
	 * of an IPv4-indexed tile, with an arbitrary tile pyramid.
	 */
	def getDescription[BT] (sc: SparkContext, pyramid: TilePyramid): AnalysisDescription[TileData[BT], String] =
		new MonolithicAnalysisDescription[TileData[BT], String](
			sc,
			convertParam(pyramid),
			new StringAnalytic("CIDR Block"),
			// No accumulators are appropriate for CIDR block metadata
			Map[String, TileIndex => Boolean]())
}


class StringAnalytic (analyticName: String) extends TileAnalytic[String] {
	def name = analyticName
	def aggregate (a: String, b: String): String = a+b
	def defaultProcessedValue: String = ""
	def defaultUnprocessedValue: String = ""
}
