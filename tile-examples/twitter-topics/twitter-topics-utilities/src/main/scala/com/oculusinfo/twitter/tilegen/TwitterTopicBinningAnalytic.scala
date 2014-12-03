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

import com.oculusinfo.twitter.binning.TwitterDemoTopicRecord
import com.oculusinfo.tilegen.tiling.analytics.BinningAnalytic



class TwitterTopicBinningAnalytic
		extends BinningAnalytic[Map[String, TwitterDemoTopicRecord], JavaList[TwitterDemoTopicRecord]]
{
	def aggregate (a: Map[String, TwitterDemoTopicRecord],
	               b: Map[String, TwitterDemoTopicRecord]): Map[String, TwitterDemoTopicRecord] = {
		a ++ b.map{case (k, v) =>
			k -> a.get(k).map(TwitterDemoTopicRecord.addRecords(_, v)).getOrElse(v)
		}
	}

	/**
	 * The default processing value to use for an analytic group known
	 * to have no value.
	 */
	def defaultProcessedValue: Map[String, TwitterDemoTopicRecord] =
		Map[String, TwitterDemoTopicRecord]()

	/**
	 * The default processing value to use for an analytic group whose
	 * value is unknown, so as to initialize it for aggregation with
	 * any known values.
	 */
	def defaultUnprocessedValue: Map[String, TwitterDemoTopicRecord] =
		Map[String, TwitterDemoTopicRecord]()

	def finish (value: Map[String, TwitterDemoTopicRecord]): JavaList[TwitterDemoTopicRecord] =
		value.values.toList.sortBy(-_.getCountMonthly()).slice(0, 10).asJava
}
