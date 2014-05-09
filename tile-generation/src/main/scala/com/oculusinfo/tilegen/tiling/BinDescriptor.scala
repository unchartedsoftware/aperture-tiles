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



import scala.collection.JavaConverters._


import java.lang.{Double => JavaDouble}
import java.lang.{Math => JavaMath}
import java.util.{List => JavaList}
import java.util.{Map => JavaMap}
import java.util.ArrayList

import org.apache.avro.file.CodecFactory

import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer
import com.oculusinfo.binning.io.serialization.impl.StringDoublePairArrayAvroSerializer
import com.oculusinfo.binning.util.Pair



/**
 * A complete description of how to treat a given type of bin
 *
 * @param PT Processing type - the type of object used when processing the data
 * @param BT Bin type - the type of object used when binning the data
 */
trait BinDescriptor[PT, BT] extends Serializable {
	/** Compose two bin values into one */
	def aggregateBins (a: PT, b: PT): PT
	/** Determine the minimum of two bin values */
	def min (a: BT, b: BT): BT
	/**
	 * Return the value used as the beginning of a fold operation over bins to
	 * find the minimum bin value.
	 */
	def defaultMin: BT
	/** Determine the maximum of two bin values */
	def max (a: BT, b: BT): BT
	/**
	 * Return the value used as the beginning of a fold operation over bins to
	 * find the maximum bin value.
	 */
	def defaultMax: BT

	/**
	 * Convert a bin value to a string.  Used when writing out the minimum and 
	 * maximum fields in the metadata of a tile tree
	 */
	def binToString (value: BT): String = value.toString

	/**
	 * Convert a bin value that has been written to a string back into a bin 
	 * value.  Used when updating the minimum and maximum fields in the 
	 * metadata of a tile tree.
	 */
	def stringToBin (value: String): BT

	/**
     * Return the default value to be used in bins known to have no value, 
	 * before conversion.
	 */
	def defaultProcessedBinValue: PT

	/**
     * Returns the default value to be used in bins not known to be empty, to 
     * which data can be aggregated.  Again, value is before conversion.
	 */
	def defaultUnprocessedBinValue: PT

	/** Convert the processing value to a binning value */
	def convert (value: PT): BT

	/** Get the type of serializer to be used for tiles with this kind of bin */
	def getSerializer: TileSerializer[BT]
}

class StandardDoubleBinDescriptor extends BinDescriptor[Double, JavaDouble] {
	val javaZero = new JavaDouble(0.0)
	def aggregateBins (a: Double, b: Double): Double = a + b
	def min (a: JavaDouble, b: JavaDouble): JavaDouble = JavaMath.min(a, b)
	def defaultMin: JavaDouble = JavaDouble.MAX_VALUE
	def max (a: JavaDouble, b: JavaDouble): JavaDouble = JavaMath.max(a, b)
	def defaultMax: JavaDouble = JavaDouble.MIN_VALUE
	def defaultProcessedBinValue: Double = 0.0
	def defaultUnprocessedBinValue: Double = 0.0
	def stringToBin (value: String): JavaDouble = convert(value.toDouble)
	def convert (value: Double): JavaDouble = new JavaDouble(value)
	def getSerializer: TileSerializer[JavaDouble] = new DoubleAvroSerializer(CodecFactory.bzip2Codec())
}

class CompatibilityDoubleBinDescriptor extends StandardDoubleBinDescriptor {
	override def getSerializer: TileSerializer[JavaDouble] =
		new BackwardCompatibilitySerializer()
}

class MinimumDoubleBinDescriptor extends StandardDoubleBinDescriptor {
	override def aggregateBins (a: Double, b: Double): Double = a min b
	override def defaultUnprocessedBinValue: Double = Double.MaxValue
}

class MaximumDoubleBinDescriptor extends StandardDoubleBinDescriptor {
	override def aggregateBins (a: Double, b: Double): Double = a max b
	override def defaultUnprocessedBinValue: Double = Double.MinValue
}

class LogDoubleBinDescriptor(logBase: Double = math.exp(1.0)) extends StandardDoubleBinDescriptor {
	override def aggregateBins (a: Double, b: Double): Double =
		math.log(math.pow(logBase, a) + math.pow(logBase, b))/math.log(logBase)
	override def defaultUnprocessedBinValue: Double = Double.NegativeInfinity
}

class StandardDoubleArrayBinDescriptor extends BinDescriptor[Seq[Double], JavaList[JavaDouble]] {
	private val _emptyList = new ArrayList[JavaDouble]()

	def aggregateBins (a: Seq[Double], b: Seq[Double]): Seq[Double] = {
		val alen = a.length
		val blen = b.length
		val maxlen = alen max blen
		(Range(0, maxlen).map(n =>
			 {
				 if (n < alen && n < blen) a(n) + b(n)
				 else if (n < alen) a(n)
				 else b(n)
			 }
		 ))
	}
	def min (a: JavaList[JavaDouble], b: JavaList[JavaDouble]): JavaList[JavaDouble] = {
		val alen = a.size
		val blen = b.size
		val maxlen = alen max blen
		Range(0, maxlen).map(n =>
			{
				if (n < alen && n < blen) a.get(n).doubleValue min b.get(n).doubleValue
				else if (n < alen) a.get(n).doubleValue
				else b.get(n).doubleValue
			}
		).map(new JavaDouble(_)).asJava
	}
	def max (a: JavaList[JavaDouble], b: JavaList[JavaDouble]): JavaList[JavaDouble] = {
		val alen = a.size
		val blen = b.size
		val maxlen = alen max blen
		Range(0, maxlen).map(n =>
			{
				if (n < alen && n < blen) a.get(n).doubleValue max b.get(n).doubleValue
				else if (n < alen) a.get(n).doubleValue
				else b.get(n).doubleValue
			}
		).map(new JavaDouble(_)).asJava
	}
	def defaultMin: JavaList[JavaDouble] = _emptyList
	def defaultMax: JavaList[JavaDouble] = _emptyList

	override def binToString (value: JavaList[JavaDouble]): String =
		value.asScala.mkString(",")
	def stringToBin (value: String): JavaList[JavaDouble] =
		convert(value.split(",").map(_.toDouble).toSeq)
	def defaultProcessedBinValue: Seq[Double] = Seq[Double]()
	def defaultUnprocessedBinValue: Seq[Double] = Seq[Double]()
	def convert (value: Seq[Double]): JavaList[JavaDouble] =
		value.map(v => new JavaDouble(v)).asJava
	def getSerializer: TileSerializer[JavaList[JavaDouble]] = new DoubleArrayAvroSerializer(CodecFactory.bzip2Codec())
}

class StringScoreBinDescriptor extends BinDescriptor[Map[String, Double],
                                                     JavaList[Pair[String, JavaDouble]]] {
	private val _emptyList = new ArrayList[Pair[String, JavaDouble]]()

	def aggregateBins (a: Map[String, Double], b: Map[String, Double]): Map[String, Double] =
		(a.keySet union b.keySet).map(key =>
			{
				val aVal = a.getOrElse(key, 0.0)
				val bVal = b.getOrElse(key, 0.0)
				key -> (aVal + bVal)
			}
		).toMap

	def min (a: JavaList[Pair[String, JavaDouble]],
	         b: JavaList[Pair[String, JavaDouble]]): JavaList[Pair[String, JavaDouble]] = {
		val aMap = a.asScala.map(p => p.getFirst() -> p.getSecond().doubleValue).toMap
		val bMap = b.asScala.map(p => p.getFirst() -> p.getSecond().doubleValue).toMap
		convert((aMap.keySet union bMap.keySet).map(key =>
			        {
				        val aVal = aMap.getOrElse(key, Double.MaxValue)
				        val bVal = bMap.getOrElse(key, Double.MaxValue)
				        key -> (aVal min bVal)
			        }
		        ).toMap)
	}

	def max (a: JavaList[Pair[String, JavaDouble]],
	         b: JavaList[Pair[String, JavaDouble]]): JavaList[Pair[String, JavaDouble]] = {
		val aMap = a.asScala.map(p => p.getFirst() -> p.getSecond().doubleValue).toMap
		val bMap = b.asScala.map(p => p.getFirst() -> p.getSecond().doubleValue).toMap
		convert((aMap.keySet union bMap.keySet).map(key =>
			        {
				        val aVal = aMap.getOrElse(key, Double.MinValue)
				        val bVal = bMap.getOrElse(key, Double.MinValue)
				        key -> (aVal max bVal)
			        }
		        ).toMap)
	}

	override def binToString (value: JavaList[Pair[String, JavaDouble]]): String = {
		value.asScala.map(pair =>
			"\""+pair.getFirst.replace("\"", "\\\"") + "\":" + pair.getSecond).mkString(",")
	}
	def stringToBin (value: String): JavaList[Pair[String, JavaDouble]] = {
		def inner (value: String): List[(String, Double)] = {
			if (value.isEmpty) List[(String, Double)]()
			else {
				val firstQuote = value.indexOf("\"")
				var secondQuote = value.indexOf("\"", firstQuote+1)
				while (secondQuote > 0 && '\\' == value.charAt(secondQuote-1))
					secondQuote = value.indexOf("\"", secondQuote+1)
				val colon = value.indexOf(":", secondQuote)
				val comma = value.indexOf(",", colon)
				if (comma > 0)
					((value.substring(firstQuote+1, secondQuote),
					  (value.substring(colon+1, comma).toDouble)) +:
						 inner(value.substring(comma+1)))
				else
					List((value.substring(firstQuote+1, secondQuote),
					      value.substring(colon+1).toDouble))
			}
		}
		inner(value).map(p => new Pair[String, JavaDouble](p._1, new JavaDouble(p._2))).asJava
	}

	def defaultMin: JavaList[Pair[String, JavaDouble]] = _emptyList
	def defaultMax: JavaList[Pair[String, JavaDouble]] = _emptyList
	def defaultProcessedBinValue: Map[String, Double] = Map[String, Double]()
	def defaultUnprocessedBinValue: Map[String, Double] = Map[String, Double]()
	def convert (value: Map[String, Double]): JavaList[Pair[String, JavaDouble]] = {
		value
			.mapValues(d => new JavaDouble(d))
			.map(p => new Pair[String, JavaDouble](p._1, p._2))
			.toList.sortBy(_.getSecond).asJava
	}
	def getSerializer: TileSerializer[JavaList[Pair[String, JavaDouble]]] =
		new StringDoublePairArrayAvroSerializer(CodecFactory.bzip2Codec())
}

/**
 * A BinDescriptor that takes a list of category names, and then translates a list of doubles (which
 * corresponds to the categories in the same order) to a list of the pairs (category name, value).  
 */
class CategoryValueBinDescriptor(categoryNames: List[String]) extends BinDescriptor[List[Double],
                                                     JavaList[Pair[String, JavaDouble]]] {
	private val _emptyList = new ArrayList[Pair[String, JavaDouble]]()

	def aggregateBins (a: List[Double], b: List[Double]): List[Double] =
		Range(0, a.length max b.length).map(i => {
			val av = if (i < a.length) a(i) else 0
			val bv = if (i < b.length) b(i) else 0
			av+bv
		}).toList

	def min (a: JavaList[Pair[String, JavaDouble]],
	         b: JavaList[Pair[String, JavaDouble]]): JavaList[Pair[String, JavaDouble]] = {
		val as = a.asScala
		val bs = b.asScala
		Range(0, as.length max bs.length).map(i => {
			val key = if (i < as.length) as(i).getFirst() else bs(i).getFirst()
			val av: JavaDouble = if (i < as.length) as(i).getSecond() else JavaDouble.MAX_VALUE
			val bv: JavaDouble = if (i < bs.length) bs(i).getSecond() else JavaDouble.MAX_VALUE
			new Pair[String, JavaDouble](key, java.lang.Math.min(av, bv))
		}).toList.asJava
	}

	def max (a: JavaList[Pair[String, JavaDouble]],
	         b: JavaList[Pair[String, JavaDouble]]): JavaList[Pair[String, JavaDouble]] = {
		val as = a.asScala
		val bs = b.asScala
		Range(0, as.length max bs.length).map(i => {
			val key = if (i < as.length) as(i).getFirst() else bs(i).getFirst()
			val av: JavaDouble = if (i < as.length) as(i).getSecond() else JavaDouble.MIN_VALUE
			val bv: JavaDouble = if (i < bs.length) bs(i).getSecond() else JavaDouble.MIN_VALUE
			new Pair[String, JavaDouble](key, java.lang.Math.max(av, bv))
		}).toList.asJava
	}

	override def binToString (value: JavaList[Pair[String, JavaDouble]]): String = {
		value.asScala.map(pair =>
			"\""+pair.getFirst.replace("\"", "\\\"") + "\":" + pair.getSecond).mkString(",")
	}
	def stringToBin (value: String): JavaList[Pair[String, JavaDouble]] = {
		def inner (value: String): List[(String, Double)] = {
			if (value.isEmpty) List[(String, Double)]()
			else {
				val firstQuote = value.indexOf("\"")
				var secondQuote = value.indexOf("\"", firstQuote+1)
				while (secondQuote > 0 && '\\' == value.charAt(secondQuote-1))
					secondQuote = value.indexOf("\"", secondQuote+1)
				val colon = value.indexOf(":", secondQuote)
				val comma = value.indexOf(",", colon)
				if (comma > 0)
					((value.substring(firstQuote+1, secondQuote),
					  (value.substring(colon+1, comma).toDouble)) +:
						 inner(value.substring(comma+1)))
				else
					List((value.substring(firstQuote+1, secondQuote),
					      value.substring(colon+1).toDouble))
			}
		}
		inner(value).map(p => new Pair[String, JavaDouble](p._1, new JavaDouble(p._2))).asJava
	}

	def defaultMin: JavaList[Pair[String, JavaDouble]] = _emptyList
	def defaultMax: JavaList[Pair[String, JavaDouble]] = _emptyList
	def defaultProcessedBinValue: List[Double] = List[Double]()
	def defaultUnprocessedBinValue: List[Double] = List[Double]() 
	def defaultBinValue: List[Double] = List[Double]()
	def convert (value: List[Double]): JavaList[Pair[String, JavaDouble]] = {
		Range(0, value.length min categoryNames.length).map(i => {
			new Pair[String, JavaDouble](categoryNames(i), value(i))
		}).toList.asJava
	}
	def getSerializer: TileSerializer[JavaList[Pair[String, JavaDouble]]] =
		new StringDoublePairArrayAvroSerializer(CodecFactory.bzip2Codec())
}
