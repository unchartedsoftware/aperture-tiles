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

import org.apache.spark.SparkContext

class GeneralSparkConnector (master: String,
                             sparkHome: String,
                             user: Option[String],
                             jars: Seq[Object] = SparkConnector.getDefaultLibrariesFromMaven,
                             kryoRegistrator: Option[String])
extends SparkConnector(jars)
{
  override def getSparkContext (jobName: String): SparkContext = {
    debugConnection("property-based", jobName)

    val appName = jobName + (if (user.isDefined) "("+user.get+")" else "")

    println("Creating spark context")
    println("\tMaster: "+master)
    println("\tJob name: "+appName)
    println("\tHome: "+sparkHome)

    if (kryoRegistrator != None) {
      // inform spark that we are using kryo serialization instead of java serialization
      System.setProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      // set the kryo registrator to point to the base tile registrator 
      System.setProperty("spark.kryo.registrator", kryoRegistrator.get)
      println("\tKryo Serialization Enabled, registrator: "+kryoRegistrator.get)
    } else {
      println("\tJava Serialization Enabled")
    }   

    new SparkContext(master, appName, sparkHome, jarList, null, null)
  }
}
