/**
 * Copyright (c) 2013 Oculus Info Inc.
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
 
package com.oculusinfo.twitter.tilegen



import java.lang.{Integer => JavaInt}
import java.util.{List => JavaList}
import java.util.ArrayList

import scala.collection.JavaConverters._

import com.oculusinfo.binning.io.TileSerializer

import com.oculusinfo.tilegen.tiling.BinDescriptor

import com.oculusinfo.twitter.binning.TwitterDemoRecord
import com.oculusinfo.twitter.binning.TwitterDemoAvroSerializer


class TwitterDemoBinDescriptor
extends BinDescriptor[Map[String, TwitterDemoRecord], JavaList[TwitterDemoRecord]] {
  import TwitterDemoRecord._


  def aggregateBins (a: Map[String, TwitterDemoRecord],
		     b: Map[String, TwitterDemoRecord]): Map[String, TwitterDemoRecord] = {
    a.keySet.union(b.keySet).map(tag => {
      val aVal = a.get(tag)
      val bVal = b.get(tag)
      if (aVal.isEmpty) {
	(tag -> bVal.get)
      } else if (bVal.isEmpty) {
	(tag -> aVal.get)
      } else {
	(tag -> addRecords(aVal.get, bVal.get))
      }
    }).toMap
  }

  def defaultMin: JavaList[TwitterDemoRecord] = new ArrayList()
  def min (a: JavaList[TwitterDemoRecord],
	   b: JavaList[TwitterDemoRecord]): JavaList[TwitterDemoRecord] = {
    val both = a.asScala.toList ++ b.asScala.toList
    val min = minOfRecords(both.toArray : _*)
    List(min).asJava
  }

  def defaultMax: JavaList[TwitterDemoRecord] = new ArrayList()
  def max (a: JavaList[TwitterDemoRecord],
	   b: JavaList[TwitterDemoRecord]): JavaList[TwitterDemoRecord] = {
    val both = a.asScala.toList ++ b.asScala.toList
    val max = maxOfRecords(both.toArray : _*)
    List(max).asJava
  }

  def defaultBinValue: JavaList[TwitterDemoRecord] = new ArrayList()

  def convert (value: Map[String, TwitterDemoRecord]): JavaList[TwitterDemoRecord] =
    value.values.toList.sortBy(-_.getCount()).slice(0, 10).asJava

  def getSerializer: TileSerializer[JavaList[TwitterDemoRecord]] = new TwitterDemoAvroSerializer
}
