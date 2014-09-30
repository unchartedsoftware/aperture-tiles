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

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

class GeneralSparkConnector (master: Option[String],
                             sparkHome: String,
                             user: Option[String],
                             jars: Seq[Object] = SparkConnector.getLibrariesFromClasspath,
                             sparkArgs: Map[String, String])
		extends SparkConnector(jars)
{
	var debug = true

	override def getSparkContext (jobName: String): SparkContext = {
		debugConnection("property-based", jobName)

		val appName = jobName + (if (user.isDefined) "("+user.get+")" else "")

		println()
		println()
		println("Setting master to " + master.getOrElse("default"))
		println("Setting app name to "+appName)
		println("Setting spark home to "+sparkHome)
		println("Setting spark jars to ")
		jarList.foreach(jar => println("\t"+jar))

		val conf = new SparkConf(true)
				.setAppName(appName)
				.setSparkHome(sparkHome)
				.setJars(jarList)				
		// Only set a master when it has been passed in.  This lets the default
		// value specified in spark-conf get picked up when no master is specified.
		master.foreach(conf.setMaster(_))
		
		sparkArgs.foreach(kv => conf.set(kv._1, kv._2))
		// If we have a kryo registrator, automatically use kryo serialization
		if (sparkArgs.contains("spark.kryo.registrator"))
			conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

		if (debug) {
			println()
			println("Configuration:")
			println(conf.toDebugString)
			println()
		}

		new SparkContext(conf);
	}
}
