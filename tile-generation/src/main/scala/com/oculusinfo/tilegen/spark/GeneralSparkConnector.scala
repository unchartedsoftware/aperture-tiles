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
 
package com.oculusinfo.tilegen.spark



import spark.SparkContext



class GeneralSparkConnector (master: String,
                             sparkHome: String,
                             user: Option[String])
extends SparkConnector(
  List(new MavenReference("com.oculusinfo", "math-utilities", "0.1-SNAPSHOT"),
       new MavenReference("com.oculusinfo", "geometric-utilities", "0.1-SNAPSHOT"),
       new MavenReference("com.oculusinfo", "binning-utilities", "0.1-SNAPSHOT"),
       new MavenReference("com.oculusinfo", "tile-generation", "0.1-SNAPSHOT"),
       // These two are needed for avro serialization
       new MavenReference("org.apache.avro", "avro", "1.7.4"),
       new MavenReference("org.apache.commons", "commons-compress", "1.4.1")
     ))
{
  override def getSparkContext (jobName: String): SparkContext = {
    debugConnection("property-based", jobName)

    val appName = jobName + (if (user.isDefined) "("+user.get+")" else "")

    println("Creating spark context")
    println("\tmaster: "+master)
    println("\thome: "+sparkHome)
    println("\tuser: "+ (if (user.isDefined) user.get else "unknown"))

    new SparkContext(master, appName, sparkHome, jarList)
  }
}
