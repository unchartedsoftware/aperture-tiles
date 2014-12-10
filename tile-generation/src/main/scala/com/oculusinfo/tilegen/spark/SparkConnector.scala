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

package com.oculusinfo.tilegen.spark

import org.apache.spark._
import org.slf4j.LoggerFactory

object SparkConnector {
	val LOGGER = LoggerFactory.getLogger(getClass)
}

/**
 * Creates a SparkConnector that will accumulate configuration options and create a
 * SparkContext from them.
 * @param args A map of spark config arguments as key/value pairs.
 */
class SparkConnector(args: Map[String, String] = Map()) {

	// Pass spark command line arguments through to conf object
	private val conf = new SparkConf()
	args.foreach(kv => conf.set(kv._1, kv._2))

	// If we have a kryo registrator, automatically use kryo serialization
	if (args.contains("spark.kryo.registrator"))
		conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

	private val debug = true

	/**
	 * Passes property through to the SparkConf instance.  These values will overwrite
	 * any that have been defined as args to spark-submit or through property files.
	 */
	def setConfProperty(name: String, value: String) : Unit = conf.set(name, value)

	/**
	 * Fetches a property as an option from the SparkConf instance.
	 */
	def getConfProperty(name: String) = conf.getOption(name)

	/**
	 * Creates a new SparkContext from the SparkConf instance.
	 */
	def createContext(jobName: Option[String] = None) = {
		jobName.foreach(conf.set("spark.app.name", _))
		if (debug) {
			println()
			println("Configuration:")
			println(conf.toDebugString)
			println()
		}
		new SparkContext(conf)
	}
}