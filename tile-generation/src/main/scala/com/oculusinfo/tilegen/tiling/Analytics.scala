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
import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.util.{List => JavaList}

import scala.collection.JavaConverters._
import scala.collection.mutable.MutableList
import scala.collection.mutable.{Map => MutableMap}
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
 * An Analytic is basically an aggregation function, to describe how to 
 * aggregate the results of some analysis across bins, tiles, or anything 
 * else.
 * 
 * It also encapsulates some simple knowledge of default values.
 * 
 * @param T The type of value this analytic processes or aggregates.
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
 * @param PROCESSING_TYPE An intermediate type used to store data
 *                        needed to produce the result type when the
 *                        analytic is complete.
 * @param RESULT_TYPE The final type of value to be written out as the
 *                    results of this analytic.
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
	def toMap (value: T): Map[String, Object] =
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
	override def toMap (value: (T1, T2)): Map[String, Object] =
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
 * @param RT The raw type of data on which this analysis takes place
 * @parma AT The type of data 
 */
trait AnalysisDescription[RT, AT] {
	val analysisTypeTag: ClassTag[AT]
	def convert: RT => AT
	def analytic: TileAnalytic[AT]
	// Add a data point to appropriate accumulators
	def accumulate (tile: TileIndex, data: AT): Unit
	// Get accumulatoed metadata info in a form that can be applied
	// directly to metadata.
	def toMap: Map[String, Object]
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

case class MetaDataAccumulatorInfo[AT] (name: String,
                                         test: TileIndex => Boolean,
                                         accumulator: Accumulator[AT]) {}

/**
 * A standard analysis description parent class for descriptions of a single, 
 * monolithic analysis.
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

	def toMap: Map[String, Object] = accumulatorInfos.map(info =>
		analytic.toMap(info._2.accumulator.value).map{case (k, v) => (info._2.name+"."+k, v)}
	).reduceOption(_ ++ _).getOrElse(Map[String, String]())

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
	def toMap: Map[String, Object] = analysis1.toMap ++ analysis2.toMap
	def applyTo (metaData: PyramidMetaData): Unit = {
		analysis1.applyTo(metaData)
		analysis2.applyTo(metaData)
	}

	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit = {
		analysis1.addAccumulator(sc, name, test)
		analysis2.addAccumulator(sc, name, test)
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



/**
 * A standard analytic to calculate the mean of a value.
 * 
 * This doesn't currently exist in simple Analytic form since the finish function 
 * is an integral part of the process.  If/When someone implements a TileAnalytic
 * using mean, it is suggested they derive directly from this BinningAnalytic, and
 * map the output of finish into metadata rather than the raw (Double,Int) value.
 */
class MeanDoubleBinningAnalytic
		extends Analytic[(Double, Int)]
		with BinningAnalytic[(Double, Int), JavaDouble]
{
	def aggregate (a: (Double, Int), b: (Double, Int)): (Double, Int) =
		(a._1 + b._1, a._2 + b._2)
	def defaultProcessedValue: (Double, Int) = (0.0, 0)
	def defaultUnprocessedValue: (Double, Int) = (0.0, 0)
	def finish (value: (Double, Int)): JavaDouble = {
		val (total, count) = value
		if (0 == count) {
			JavaDouble.NaN
		} else {
			new JavaDouble(total/count)
		}
	}
}



class SumIntAnalytic extends Analytic[Int] {
	def aggregate (a: Int, b: Int): Int = a + b
	def defaultProcessedValue: Int = 0
	def defaultUnprocessedValue: Int = 0
}

class MinimumIntAnalytic extends Analytic[Int] {
	def aggregate (a: Int, b: Int): Int = a min b
	def defaultProcessedValue: Int = 0
	def defaultUnprocessedValue: Int = Int.MaxValue
}

class MinimumIntTileAnalytic extends MinimumIntAnalytic with TileAnalytic[Int] {
	def name = "minimum"
}

class MaximumIntAnalytic extends Analytic[Int] {
	def aggregate (a: Int, b: Int): Int = a max b
	def defaultProcessedValue: Int = 0
	def defaultUnprocessedValue: Int = Int.MinValue
}

class MaximumIntTileAnalytic extends MinimumIntAnalytic with TileAnalytic[Int] {
	def name = "maximum"
}

trait StandardIntBinningAnalytic extends BinningAnalytic[Int, JavaInt] {
	def finish (value: Int): JavaInt = new JavaInt(value)
}




class SumLongAnalytic extends Analytic[Long] {
	def aggregate (a: Long, b: Long): Long = a + b
	def defaultProcessedValue: Long = 0L
	def defaultUnprocessedValue: Long = 0L
}

class MinimumLongAnalytic extends Analytic[Long] {
	def aggregate (a: Long, b: Long): Long = a min b
	def defaultProcessedValue: Long = 0L
	def defaultUnprocessedValue: Long = Long.MaxValue
}

class MinimumLongTileAnalytic extends MinimumLongAnalytic with TileAnalytic[Long] {
	def name = "minimum"
}

class MaximumLongAnalytic extends Analytic[Long] {
	def aggregate (a: Long, b: Long): Long = a max b
	def defaultProcessedValue: Long = 0L
	def defaultUnprocessedValue: Long = Long.MinValue
}

class MaximumLongTileAnalytic extends MinimumLongAnalytic with TileAnalytic[Long] {
	def name = "maximum"
}

trait StandardLongBinningAnalytic extends BinningAnalytic[Long, JavaLong] {
	def finish (value: Long): JavaLong = new JavaLong(value)
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

class MinimumDoubleArrayTileAnalytic
		extends MinimumDoubleArrayAnalytic
		with StandardDoubleArrayTileAnalytic
{
	def name = "minimums"
}

class MaximumDoubleArrayAnalytic extends StandardDoubleArrayAnalytic {
	def aggregateElements (a: Double, b: Double): Double = a max b
}

class MaximumDoubleArrayTileAnalytic
		extends MaximumDoubleArrayAnalytic
		with StandardDoubleArrayTileAnalytic
{
	def name = "maximums"
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
		).getOrElse(sorted).toMap
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
object IPv4Analytics extends Serializable {
	import IPv4ZCurveIndexScheme._

	private val EPSILON = 1E-10
	def getCIDRBlock (pyramid: TilePyramid)(tile: TileData[_]): String = {
		// Figure out the IP address of our corners
		val index = tile.getDefinition()
		val bounds = pyramid.getTileBounds(index)
		val llAddr = ipArrayToLong(reverse(bounds.getMinX(), bounds.getMinY()))
		val urAddr = ipArrayToLong(reverse(bounds.getMaxX()-EPSILON, bounds.getMaxY()-EPSILON))

		// Figure out how many significant bits they have in common
		val significant = 0xffffffffL & ~(llAddr ^ urAddr)
		// That is the number of blocks
		val block = 32 - 
			(for (i <- 0 to 32) yield (i, ((1L << i) & significant) != 0))
				.find(_._2)
				.getOrElse((32, false))._1
		// And apply that to either to get the common address
		val addr = longToIPArray(llAddr & significant)
		ipArrayToString(addr)+"/"+block
	}

	def getIPAddress (pyramid: TilePyramid, max: Boolean)(tile: TileData[_]): Long = {
		val index = tile.getDefinition()
		val bounds = pyramid.getTileBounds(index)
		if (max) {
			ipArrayToLong(reverse(bounds.getMaxX()-EPSILON, bounds.getMaxY()-EPSILON))
		} else {
			ipArrayToLong(reverse(bounds.getMinX(), bounds.getMinY()))
		}
	}

	/**
	 * Get an analysis description for an analysis that stores the CIDR block 
	 * of an IPv4-indexed tile, with an arbitrary tile pyramid.
	 */
	def getCIDRBlockAnalysis[BT] (pyramid: TilePyramid = getDefaultIPPyramid):
			AnalysisDescription[TileData[BT], String] =
		new TileOnlyMonolithicAnalysisDescription[TileData[BT], String](
			getCIDRBlock(pyramid),
			new StringAnalytic("CIDR Block"))


	def getMinIPAddressAnalysis[BT] (pyramid: TilePyramid = getDefaultIPPyramid):
			AnalysisDescription[TileData[BT], Long] =
		new MonolithicAnalysisDescription[TileData[BT], Long](
			getIPAddress(pyramid, false),
			new TileAnalytic[Long] {
				def name = "Minimum IP Address"
				def aggregate (a: Long, b: Long): Long = a min b
				def defaultProcessedValue: Long = 0L
				def defaultUnprocessedValue: Long = 0xffffffffL
				override def valueToString (value: Long): String =
					ipArrayToString(longToIPArray(value))
			})

	def getMaxIPAddressAnalysis[BT] (pyramid: TilePyramid = getDefaultIPPyramid):
			AnalysisDescription[TileData[BT], Long] =
		new MonolithicAnalysisDescription[TileData[BT], Long](
			getIPAddress(pyramid, true),
			new TileAnalytic[Long] {
				def name = "Maximum IP Address"
				def aggregate (a: Long, b: Long): Long = a max b
				def defaultProcessedValue: Long = 0xffffffffL
				def defaultUnprocessedValue: Long = 0L
				override def valueToString (value: Long): String =
					ipArrayToString(longToIPArray(value))
			})
}

class StringAnalytic (analyticName: String) extends TileAnalytic[String] {
	def name = analyticName
	def aggregate (a: String, b: String): String = a+b
	def defaultProcessedValue: String = ""
	def defaultUnprocessedValue: String = ""
}


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
	override def toMap (value: String): Map[String, Object] = Map[String, String]()
}
/**
 * A very simply tile analytic that just writes custom metadata directly to the tile set 
 * metadata, and no where else.
 */
class CustomGlobalMetadata[T] (customData: Map[String, Object])
		extends AnalysisDescription[T, String] with Serializable
{
	val analysisTypeTag = implicitly[ClassTag[String]]
	def convert: T => String = (raw: T) => ""
	def analytic: TileAnalytic[String] = new CustomMetadataAnalytic
	def accumulate (tile: TileIndex, data: String): Unit = {}
	// This is used to apply the analytic to tiles; we don't want anything to happen there
	def toMap: Map[String, Object] = Map[String, Object]()
	// This is used to apply the analytic to metadata; here's where we want stuff used.
	def applyTo (metaData: PyramidMetaData): Unit =
		customData.map{case (key, value) =>
			metaData.setCustomMetaData(value, key)
		}
	// Global metadata needs no accumulators - it doesn't actually have any data.
	def addAccumulator (sc: SparkContext, name: String, test: (TileIndex) => Boolean): Unit = {}
}
