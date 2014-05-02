/*
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

import org.apache.avro.file.CodecFactory

import com.oculusinfo.binning.io.serialization.TileSerializer

import com.oculusinfo.tilegen.tiling.BinDescriptor

import com.oculusinfo.twitter.binning.TwitterDemoTopicRecord
import com.oculusinfo.twitter.binning.TwitterTopicAvroSerializer


class TwitterTopicBinDescriptor
extends BinDescriptor[Map[String, TwitterDemoTopicRecord], JavaList[TwitterDemoTopicRecord]] {
  import TwitterDemoTopicRecord._


  def aggregateBins (a: Map[String, TwitterDemoTopicRecord],
		     b: Map[String, TwitterDemoTopicRecord]): Map[String, TwitterDemoTopicRecord] = {
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

  def defaultMin: JavaList[TwitterDemoTopicRecord] = new ArrayList()
  def min (a: JavaList[TwitterDemoTopicRecord],
	   b: JavaList[TwitterDemoTopicRecord]): JavaList[TwitterDemoTopicRecord] = {
    val both = a.asScala.toList ++ b.asScala.toList
    val min = minOfRecords(both.toArray : _*)
    List(min).asJava
  }

  def defaultMax: JavaList[TwitterDemoTopicRecord] = new ArrayList()
  def max (a: JavaList[TwitterDemoTopicRecord],
	   b: JavaList[TwitterDemoTopicRecord]): JavaList[TwitterDemoTopicRecord] = {
    val both = a.asScala.toList ++ b.asScala.toList
    val max = maxOfRecords(both.toArray : _*)
    List(max).asJava
  }


  override def binToString (value: JavaList[TwitterDemoTopicRecord]): String =
    value.asScala.mkString(";")

  def stringToBin (value: String): JavaList[TwitterDemoTopicRecord] = {
	  var input = value
	  val result = new java.util.ArrayList[TwitterDemoTopicRecord]()
	  while (input.length>0) {
		  val nextRecord = TwitterDemoTopicRecord.fromString(input)
		  result.add(nextRecord)
		  val nextRecordString = nextRecord.toString
		  input = input.substring(nextRecordString.length())
		  if (input.startsWith(";"))
			  input = input.substring(1)
	  }
	  result
  }

  def defaultProcessedBinValue: Map[String, TwitterDemoTopicRecord] = Map[String, TwitterDemoTopicRecord]()
  def defaultUnprocessedBinValue: Map[String, TwitterDemoTopicRecord] = Map[String, TwitterDemoTopicRecord]()

  def convert (value: Map[String, TwitterDemoTopicRecord]): JavaList[TwitterDemoTopicRecord] =
    value.values.toList.sortBy(-_.getCountMonthly()).slice(0, 10).asJava

  def getSerializer: TileSerializer[JavaList[TwitterDemoTopicRecord]] = new TwitterTopicAvroSerializer(CodecFactory.bzip2Codec())
}
