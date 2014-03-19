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

  /** Return the default value to be used in unoccupied bins, before conversion */
  def defaultBinValue: PT

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
  def defaultBinValue: Double = 0.0
  def convert (value: Double): JavaDouble = new JavaDouble(value)
  def getSerializer: TileSerializer[JavaDouble] = new DoubleAvroSerializer(CodecFactory.bzip2Codec())
}

class CompatibilityDoubleBinDescriptor extends StandardDoubleBinDescriptor {
  override def getSerializer: TileSerializer[JavaDouble] =
    new BackwardCompatibilitySerializer()
}

class MinimumDoubleBinDescriptor extends StandardDoubleBinDescriptor {
  override def aggregateBins (a: Double, b: Double): Double = a min b
}

class MaximumDoubleBinDescriptor extends StandardDoubleBinDescriptor {
  override def aggregateBins (a: Double, b: Double): Double = a max b
}

class LogDoubleBinDescriptor(logBase: Double = math.exp(1.0)) extends StandardDoubleBinDescriptor {
  override def aggregateBins (a: Double, b: Double): Double = math.log(math.pow(logBase, a) + math.pow(logBase, b))/math.log(logBase)
}

class StandardDoubleArrayBinDescriptor extends BinDescriptor[Seq[Double], JavaList[JavaDouble]] {
  private val _emptyList = new ArrayList[JavaDouble]()

  def aggregateBins (a: Seq[Double], b: Seq[Double]): Seq[Double] = {
    val alen = a.length
    val blen = b.length
    val maxlen = alen max blen
    (Range(0, maxlen).map(n => {
      if (n < alen && n < blen) a(n) + b(n)
      else if (n < alen) a(n)
      else b(n)
    }))
  }
  def min (a: JavaList[JavaDouble], b: JavaList[JavaDouble]): JavaList[JavaDouble] = {
    val alen = a.size
    val blen = b.size
    val maxlen = alen max blen
    Range(0, maxlen).map(n => {
      if (n < alen && n < blen) a.get(n).doubleValue min b.get(n).doubleValue
      else if (n < alen) a.get(n).doubleValue
      else b.get(n).doubleValue
    }).map(new JavaDouble(_)).toList.asJava
  }
  def max (a: JavaList[JavaDouble], b: JavaList[JavaDouble]): JavaList[JavaDouble] = {
    val alen = a.size
    val blen = b.size
    val maxlen = alen max blen
    Range(0, maxlen).map(n => {
      if (n < alen && n < blen) a.get(n).doubleValue max b.get(n).doubleValue
      else if (n < alen) a.get(n).doubleValue
      else b.get(n).doubleValue
    }).map(new JavaDouble(_)).toList.asJava
  }
  def defaultMin: JavaList[JavaDouble] = _emptyList
  def defaultMax: JavaList[JavaDouble] = _emptyList
  def defaultBinValue: Seq[Double] = Seq[Double]()
  def convert (value: Seq[Double]): JavaList[JavaDouble] =
    value.map(v => new JavaDouble(v)).toList.asJava
  def getSerializer: TileSerializer[JavaList[JavaDouble]] = new DoubleArrayAvroSerializer(CodecFactory.bzip2Codec())
}

class StringScoreBinDescriptor extends BinDescriptor[Map[String, Double],
                                                     JavaList[Pair[String, JavaDouble]]] {
  private val _emptyList = new ArrayList[Pair[String, JavaDouble]]()

  def aggregateBins (a: Map[String, Double], b: Map[String, Double]): Map[String, Double] =
    (a.keySet union b.keySet).map(key => {
      val aVal = a.getOrElse(key, 0.0)
      val bVal = b.getOrElse(key, 0.0)
      key -> (aVal + bVal)
    }).toMap

  def min (a: JavaList[Pair[String, JavaDouble]],
           b: JavaList[Pair[String, JavaDouble]]): JavaList[Pair[String, JavaDouble]] = {
    val aMap = a.asScala.map(p => p.getFirst() -> p.getSecond().doubleValue).toMap
    val bMap = a.asScala.map(p => p.getFirst() -> p.getSecond().doubleValue).toMap
    convert((aMap.keySet union bMap.keySet).map(key => {
      val aVal = aMap.getOrElse(key, Double.MaxValue)
      val bVal = bMap.getOrElse(key, Double.MaxValue)
      key -> (aVal min bVal)
    }).toMap)
  }

  def max (a: JavaList[Pair[String, JavaDouble]],
           b: JavaList[Pair[String, JavaDouble]]): JavaList[Pair[String, JavaDouble]] = {
    val aMap = a.asScala.map(p => p.getFirst() -> p.getSecond().doubleValue).toMap
    val bMap = a.asScala.map(p => p.getFirst() -> p.getSecond().doubleValue).toMap
    convert((aMap.keySet union bMap.keySet).map(key => {
      val aVal = aMap.getOrElse(key, Double.MinValue)
      val bVal = bMap.getOrElse(key, Double.MinValue)
      key -> (aVal max bVal)
    }).toMap)
  }

  def defaultMin: JavaList[Pair[String, JavaDouble]] = _emptyList
  def defaultMax: JavaList[Pair[String, JavaDouble]] = _emptyList
  def defaultBinValue: Map[String, Double] = Map[String, Double]()
  def convert (value: Map[String, Double]): JavaList[Pair[String, JavaDouble]] =
    value
      .mapValues(d => new JavaDouble(d))
      .map(p => new Pair[String, JavaDouble](p._1, p._2))
      .toList.sortBy(_.getSecond).asJava
  def getSerializer: TileSerializer[JavaList[Pair[String, JavaDouble]]] = 
    new StringDoublePairArrayAvroSerializer(CodecFactory.bzip2Codec())
}
