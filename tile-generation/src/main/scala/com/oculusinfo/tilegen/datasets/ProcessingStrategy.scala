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



import scala.reflect.ClassTag

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.Time

import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescription





trait StreamingProcessor[IT, PT, DT] {
	def processWithTime[OUTPUT] (fcn: Time => RDD[(IT, PT, Option[DT])] => OUTPUT,
	                             completionCallback: Option[Time => OUTPUT => Unit]): Unit
}

abstract class ProcessingStrategy[IT: ClassTag, PT: ClassTag, DT: ClassTag] {
	def process[OUTPUT] (fcn: RDD[(IT, PT, Option[DT])] => OUTPUT,
	                     completionCallback: Option[OUTPUT => Unit]): Unit

	def getDataAnalytics: Option[AnalysisDescription[_, DT]]

	def transformRDD[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE]

	def transformDStream[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE]
}

abstract class StaticProcessingStrategy[IT: ClassTag, PT: ClassTag, DT: ClassTag] (sc: SparkContext)
		extends ProcessingStrategy[IT, PT, DT] {
	private val rdd = getData

	protected def getData: RDD[(IT, PT, Option[DT])]

	final def process[OUTPUT] (fcn: RDD[(IT, PT, Option[DT])] => OUTPUT,
	                           completionCallback: Option[OUTPUT => Unit] = None): Unit = {
		val result = fcn(rdd)
		completionCallback.map(_(result))
	}

	final def transformRDD[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		fcn(rdd)

	final def transformDStream[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		throw new Exception("Attempt to call DStream transform on RDD processor")
}

abstract class StreamingProcessingStrategy[IT: ClassTag, PT: ClassTag, DT: ClassTag]
		extends ProcessingStrategy[IT, PT, DT] {
	private val dstream = getData

	protected def getData: DStream[(IT, PT, Option[DT])]

	private final def internalProcess[OUTPUT] (rdd: RDD[(IT, PT, Option[DT])],
	                                           fcn: RDD[(IT, PT, Option[DT])] => OUTPUT,
	                                           completionCallback: Option[OUTPUT => Unit] = None): Unit = {
		val result = fcn(rdd)
		completionCallback.map(_(result))
	}

	def process[OUTPUT] (fcn: RDD[(IT, PT, Option[DT])] => OUTPUT,
	                     completionCallback: Option[(OUTPUT => Unit)] = None): Unit = {
		dstream.foreachRDD(internalProcess(_, fcn, completionCallback))
	}

	def processWithTime[OUTPUT] (fcn: Time => RDD[(IT, PT, Option[DT])] => OUTPUT,
	                             completionCallback: Option[Time => OUTPUT => Unit]): Unit = {
		dstream.foreachRDD{(rdd, time) =>
			internalProcess(rdd, fcn(time), completionCallback.map(_(time)))
		}
	}
	
	final def transformRDD[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): RDD[OUTPUT_TYPE] =
		throw new Exception("Attempt to call RDD transform on DStream processor")

	final def transformDStream[OUTPUT_TYPE: ClassTag]
		(fcn: RDD[(IT, PT, Option[DT])] => RDD[OUTPUT_TYPE]): DStream[OUTPUT_TYPE] =
		dstream.transform(fcn)
}
