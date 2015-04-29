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

package com.oculusinfo.tilegen.graph.analytics


import java.util.{List => JavaList}

import org.json.{JSONArray, JSONObject}

import scala.collection.JavaConverters._

import org.apache.spark.SparkContext

import com.oculusinfo.binning.TileIndex
//import com.oculusinfo.tilegen.graph.analytics.GraphAnalyticsRecord
import com.oculusinfo.tilegen.tiling.analytics.TileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescriptionTileWrapper


class GraphMaxRecordAnalytic extends TileAnalytic[List[GraphAnalyticsRecord]] {
	def name: String = "maximum"

	def aggregate (a: List[GraphAnalyticsRecord],
	               b: List[GraphAnalyticsRecord]): List[GraphAnalyticsRecord] =
		List(GraphAnalyticsRecord.maxOfRecords((a ++ b).toArray :_*))

	override def storableValue (value: List[GraphAnalyticsRecord], location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val result = new JSONObject()
		val subRes = new JSONArray()
		value.foreach(gar => subRes.put(gar.toString))
		result.put(name, subRes)
		Some(result)
	}

	def defaultProcessedValue: List[GraphAnalyticsRecord] = List[GraphAnalyticsRecord]()
	def defaultUnprocessedValue: List[GraphAnalyticsRecord] = List[GraphAnalyticsRecord]()
}

class GraphMinRecordAnalytic extends TileAnalytic[List[GraphAnalyticsRecord]] {
	def name: String = "minimum"

	def aggregate (a: List[GraphAnalyticsRecord],
	               b: List[GraphAnalyticsRecord]): List[GraphAnalyticsRecord] =
		List(GraphAnalyticsRecord.minOfRecords((a ++ b).toArray :_*))
		
	override def storableValue (value: List[GraphAnalyticsRecord], location: TileAnalytic.Locations.Value): Option[JSONObject] = {
		val result = new JSONObject()
		val subRes = new JSONArray()
		value.foreach(gar => subRes.put(gar.toString))
		result.put(name, subRes)
		Some(result)
	}		

	def defaultProcessedValue: List[GraphAnalyticsRecord] = List[GraphAnalyticsRecord]()
	def defaultUnprocessedValue: List[GraphAnalyticsRecord] = List[GraphAnalyticsRecord]()
}


object GraphListAnalysis {
	val convertFcn: JavaList[GraphAnalyticsRecord] => List[GraphAnalyticsRecord] =
		a => a.asScala.toList
}
class GraphListAnalysis(analytic: TileAnalytic[List[GraphAnalyticsRecord]])
		extends AnalysisDescriptionTileWrapper[JavaList[GraphAnalyticsRecord],
		                                       List[GraphAnalyticsRecord]](GraphListAnalysis.convertFcn, analytic)
{
}
