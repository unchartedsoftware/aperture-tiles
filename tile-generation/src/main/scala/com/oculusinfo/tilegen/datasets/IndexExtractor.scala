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

package com.oculusinfo.tilegen.datasets



import java.text.SimpleDateFormat

import scala.reflect.ClassTag

import com.oculusinfo.tilegen.tiling.IndexScheme
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme
import com.oculusinfo.tilegen.tiling.TimeRangeCartesianIndexScheme
import com.oculusinfo.tilegen.tiling.TimeIndexScheme
import com.oculusinfo.tilegen.tiling.LineSegmentIndexScheme
import com.oculusinfo.tilegen.util.PropertiesWrapper



object CSVIndexExtractor {
	def fromProperties (properties: PropertiesWrapper): CSVIndexExtractor[_] = {
		var indexType = properties.getString(
			"oculus.binning.index.type",
			"The type of index to use in the data.  Currently supported options "+
				"are cartesian (the default), graph, timerange, and ipv4.",
			Some("cartesian"))

		if ("graph" == indexType) {
			val graphDataType = properties.getString(
				"oculus.binning.graph.data",
				"The type of graph data to tile (nodes or edges). Default is nodes.",
				Some("nodes"))
			if ("nodes" == graphDataType)
				indexType = "cartesian"
		}

		indexType match {
			case "cartesian" => {
				val xVar = properties.getString("oculus.binning.xField",
				                                "The field to use for the X axis of tiles produced",
				                                Some(CSVDatasetBase.ZERO_STR))
				val yVar = properties.getString("oculus.binning.yField",
				                                "The field to use for the Y axis of tiles produced",
				                                Some(CSVDatasetBase.ZERO_STR))
				new CartesianIndexExtractor(xVar, yVar)
			}
			case "graph" => {
				// edges require two cartesian endpoints
				val xVar1 = properties.getString("oculus.binning.xField",
				                                 "The field to use for the X axis for edge start pt",
				                                 Some(CSVDatasetBase.ZERO_STR))
				val yVar1 = properties.getString("oculus.binning.yField",
				                                 "The field to use for the Y axis for edge start pt",
				                                 Some(CSVDatasetBase.ZERO_STR))
				val xVar2 = properties.getString("oculus.binning.xField2",
				                                 "The field to use for the X axis for edge end pt",
				                                 Some(CSVDatasetBase.ZERO_STR))
				val yVar2 = properties.getString("oculus.binning.yField2",
				                                 "The field to use for the Y axis for edge end pt",
				                                 Some(CSVDatasetBase.ZERO_STR))
				new LineSegmentIndexExtractor(xVar1, yVar1, xVar2, yVar2)
			}
			case "timerange" => {
				val xVar = properties.getString("oculus.binning.xField",
				                                "The field to use for the X axis of tiles produced",
				                                Some(CSVDatasetBase.ZERO_STR))
				val yVar = properties.getString("oculus.binning.yField",
				                                "The field to use for the Y axis of tiles produced",
				                                Some(CSVDatasetBase.ZERO_STR))
				val timeVar = properties.getString("oculus.binning.timeField",
				                                   "The field to use for the time axis of tiles produced",
				                                   Some(CSVDatasetBase.ZERO_STR))

				val startDateFormat = new SimpleDateFormat(
					properties.getString("oculus.binning.timeRange.dateFormat",
					                     "The parsing format to use for 'oculus.binning.timeRange.startDate'",
					                     Some("yyMMddHHmm")))

				val startDate = startDateFormat.parse(
					properties.getString("oculus.binning.timeRange.startDate",
					                     "The initial date to base the time ranges on.",
					                     Some(""))).getTime()

				val secsPerRange = properties.getDouble("oculus.binning.timeRange.secondsPerRange",
				                                        "The number of seconds each range should represent",
				                                        Some(60 * 60 * 24))
				new TimeRangeCartesianIndexExtractor(timeVar, xVar, yVar, startDate, secsPerRange)
			}
			case "ipv4" => {
				val ipVar = properties.getString("oculus.binning.ipv4Field",
				                                 "The field from which to get the ipv4 address.  "+
					                                 "Field type must be \"ipv4\".",
				                                 None)
				new IPv4IndexExtractor(ipVar)
			}
		}
	}
}
abstract class CSVIndexExtractor[T: ClassTag] extends Serializable {
	val indexTypeTag = implicitly[ClassTag[T]]

	// The fields this extractor needs
	def fields: Array[String]

	// The name of the indexing scheme - usually refering to the fields it
	// uses - for use in table naming.
	def name: String

	// A description of the data set axes
	def description: String

	// The index scheme the binner needs to know what to do with the index
	// values we generate
	def indexScheme: IndexScheme[T]
	
	// Get the index value from the field values
	def calculateIndex (fieldValues: Map[String, Double]): T

	// Indicate if the index implies a density strip
	def isDensityStrip: Boolean
}

abstract class TimeRangeCSVIndexExtractor[T: ClassTag] extends CSVIndexExtractor[T] {
	// The time based index scheme the binner needs to know what to do with the index
	// values we generate
	def timeIndexScheme: TimeIndexScheme[T]
	
	def msPerTimeRange: Double
}

class CartesianIndexExtractor(xVar: String, yVar: String)
		extends CSVIndexExtractor[(Double, Double)]
{
	private val scheme = new CartesianIndexScheme

	def fields = Array(xVar, yVar)
	def name = xVar + "." + yVar
	def description = xVar + " vs. " + yVar
	def indexScheme = scheme
	def calculateIndex (fieldValues: Map[String, Double]): (Double, Double) =
		(fieldValues(xVar), fieldValues(yVar))
	def isDensityStrip = yVar == CSVDatasetBase.ZERO_STR
}

class LineSegmentIndexExtractor
	(xVar: String, yVar: String, xVar2: String, yVar2: String)
		extends CSVIndexExtractor[(Double, Double, Double, Double)]
{
	private val scheme = new LineSegmentIndexScheme

	def fields = Array(xVar, yVar, xVar2, yVar2)
	def name =  xVar + "." + yVar  //xVar + "." + yVar + "." + xVar2 + "." + yVar2
	def description = xVar + " vs. " + yVar  //xVar + " vs. " + yVar + "and" + xVar2 + "vs" + yVar2
	def indexScheme = scheme
	def calculateIndex(fieldValues: Map[String, Double]): (Double, Double, Double, Double) =
		(fieldValues(xVar), fieldValues(yVar), fieldValues(xVar2), fieldValues(yVar2))
	def isDensityStrip = yVar == CSVDatasetBase.ZERO_STR
}

class IPv4IndexExtractor (ipField: String) extends CSVIndexExtractor[Array[Byte]] {
	private val scheme = new IPv4ZCurveIndexScheme

	def fields = Array(ipField)
	def name = ipField
	def description = ipField
	def indexScheme = scheme
	def calculateIndex (fieldValues: Map[String, Double]): Array[Byte] = {
		val address = fieldValues(ipField).round.toLong
		Array(((address & 0xff000000L) >> 24).toByte,
		      ((address & 0xff0000L) >> 16).toByte,
		      ((address & 0xff00L) >> 8).toByte,
		      ((address & 0xff)).toByte)
	}
	val isDensityStrip = false
}

class TimeRangeCartesianIndexExtractor
	(timeVar: String, xVar: String, yVar: String,
	 startDate: Double, secsPerPeriod: Double)
		extends TimeRangeCSVIndexExtractor[(Double, Double, Double)]
{
	private val scheme = new TimeRangeCartesianIndexScheme

	def msPerTimeRange = secsPerPeriod * 1000
	
	/**
	 * Floors the date value to the last time period
	 */
	def floorDate(d: Double) = Math.floor((d - startDate) / msPerTimeRange) * msPerTimeRange + startDate
	
	def fields = Array(timeVar, xVar, yVar)
	def name = xVar + "." + yVar
	def description = xVar + " vs. " + yVar
	def indexScheme = scheme
	def timeIndexScheme = scheme
	def calculateIndex (fieldValues: Map[String, Double]): (Double, Double, Double) =
		(floorDate(fieldValues(timeVar)), fieldValues(xVar), fieldValues(yVar))
	def isDensityStrip = yVar == CSVDatasetBase.ZERO_STR
}
