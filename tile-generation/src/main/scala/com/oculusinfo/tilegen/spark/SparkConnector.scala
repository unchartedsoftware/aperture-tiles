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

import java.io.File

import org.apache.spark._
import org.apache.spark.SparkContext._



class MavenReference (groupId: String, 
		      artifactId: String, 
		      version: String = "0.0.1-SNAPSHOT") {
  override def toString: String = {
    var libLocation = (System.getProperty("user.home") + "/.m2/repository/"
      + groupId.split("\\.").mkString("/") + "/" + artifactId + "/"
      + version + "/" + artifactId + "-" + version + ".jar")
    // we have to do some stupid name-mangling on windows
    val os = System.getProperty("os.name").toLowerCase()
    if (os.contains("windows"))
      libLocation = libLocation.replace('\\', '/')
    libLocation
  }
}


object SparkConnector {
  def getDefaultSparkConnector: SparkConnector = {
    val allSparkLibs = System.getenv("SPARK_CLASSPATH")
    val sparkLibs = allSparkLibs.split(":").filter(!_.isEmpty)
    new SparkConnector(sparkLibs.toList)
  }
}
class SparkConnector (jars: List[Object]) {
  protected lazy val jarList : List[String] = {
    jars.map(_.toString).map(jar => {
      println("Checking "+jar)
      println("\t"+new File(jar).exists())
      jar
    })
  }


  private def getHost: String =
    java.net.InetAddress.getLocalHost().getHostName()

  private def isActive (hostname: String): Boolean = 
    java.net.InetAddress.getByName(hostname).isReachable(5000)


  def getSparkContext (jobName: String): SparkContext = {
      getLocalSparkContext(jobName)
  }


  def debugConnection (connectionType: String,
                       jobName: String): Unit = {
    println("Connection to " + connectionType + " spark context")
    println("\tjob: "+jobName)
    println("\tjars:")
    if (jarList.isEmpty) println("\t\tNone")
    else jarList.foreach(j => println("\t\t"+j))
  }

  def getLocalSparkContext (jobName: String): SparkContext = {
    debugConnection("local", jobName)
    new SparkContext("local", jobName, "/opt/spark-0.7.2", jarList)
  }
}


object TestSparkConnector {
  def main (args: Array[String]): Unit = {
    testDefaultSparkConnector()
  }

  def testDefaultSparkConnector (): Unit = {
    val connector = SparkConnector.getDefaultSparkConnector
    connector.debugConnection("test", "test")
  }
}
