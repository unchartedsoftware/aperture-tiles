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



import java.util.{List => JavaList}

import scala.collection.JavaConverters._

import org.apache.spark.SparkContext

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.twitter.binning.TwitterDemoTopicRecord

import com.oculusinfo.tilegen.tiling.TileAnalytic
import com.oculusinfo.tilegen.tiling.AnalysisDescriptionTileWrapper



class TwitterMaxRecordAnalytic extends TileAnalytic[List[TwitterDemoTopicRecord]] {
	def name: String = "maximum"

	override def valueToString (value: List[TwitterDemoTopicRecord]): String =
		value.mkString(":")

	def aggregate (a: List[TwitterDemoTopicRecord],
	               b: List[TwitterDemoTopicRecord]): List[TwitterDemoTopicRecord] =
		List(TwitterDemoTopicRecord.maxOfRecords((a ++ b).toArray :_*))
		
	def scaleValue (a: List[TwitterDemoTopicRecord], scale: Double): List[TwitterDemoTopicRecord] = throw new UnsupportedOperationException("Not Supported")
	def defaultProcessedValue: List[TwitterDemoTopicRecord] = List[TwitterDemoTopicRecord]()
	def defaultUnprocessedValue: List[TwitterDemoTopicRecord] = List[TwitterDemoTopicRecord]()
}

class TwitterMinRecordAnalytic extends TileAnalytic[List[TwitterDemoTopicRecord]] {
	def name: String = "minimum"

	override def valueToString (value: List[TwitterDemoTopicRecord]): String =
		value.mkString(":")

	def aggregate (a: List[TwitterDemoTopicRecord],
	               b: List[TwitterDemoTopicRecord]): List[TwitterDemoTopicRecord] =
		List(TwitterDemoTopicRecord.minOfRecords((a ++ b).toArray :_*))
		
	def scaleValue (a: List[TwitterDemoTopicRecord], scale: Double): List[TwitterDemoTopicRecord] = throw new UnsupportedOperationException("Not Supported")
	def defaultProcessedValue: List[TwitterDemoTopicRecord] = List[TwitterDemoTopicRecord]()
	def defaultUnprocessedValue: List[TwitterDemoTopicRecord] = List[TwitterDemoTopicRecord]()
}


object TwitterTopicListAnalysis {
	val convertFcn: JavaList[TwitterDemoTopicRecord] => List[TwitterDemoTopicRecord] =
		a => a.asScala.toList
}
class TwitterTopicListAnalysis
	(sc: SparkContext,
	 analytic: TileAnalytic[List[TwitterDemoTopicRecord]],
	 globalMetaData: Map[String, TileIndex => Boolean])
		extends AnalysisDescriptionTileWrapper
	[JavaList[TwitterDemoTopicRecord],
	 List[TwitterDemoTopicRecord]] (sc,
	                            TwitterTopicListAnalysis.convertFcn,
	                            analytic,
	                            globalMetaData)
{
}
