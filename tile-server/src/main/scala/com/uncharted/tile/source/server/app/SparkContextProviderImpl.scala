/*
 * Copyright (c) 2015 Uncharted Software Inc.
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
package com.uncharted.tile.source.server.app

import java.io.{FilenameFilter, File}

import grizzled.slf4j.Logging
import org.apache.log4j.{Level, Logger}
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SQLContext
import org.json.{JSONException, JSONObject}

import scala.util.Try

/**
 * A simple, stand-alone application implementation of SparkContextProvider
 *
 * Note: This is NOT thread-safe; spark querries should always all happen on the same thread.
 */
class SparkContextProviderImpl (master: String, jobName: String, sparkHome: String, extraJars: String) extends SparkContextProvider  with Logging {
  val jarList: Array[String] = Array(
    getJarPathForClass(classOf[com.oculusinfo.binning.TilePyramid]),
    getJarPathForClass(classOf[com.oculusinfo.tilegen.tiling.TileIO]),
    getJarPathForClass(classOf[org.apache.hadoop.hbase.HBaseConfiguration])
  ) ++ {
    if (null == extraJars || extraJars.isEmpty) Array[String]()
    else extraJars.split(":").map(_.trim).filter(!_.isEmpty)
  }

  private def getJarPathForClass(representativeClass: Class[_]): String = {
    var location: String = representativeClass.getProtectionDomain.getCodeSource.getLocation.getPath
    if (location.endsWith("classes/")) {
      val target: File = new File(location).getParentFile
      val children = target.listFiles(new FilenameFilter() {
        def accept(dir: File, name: String): Boolean = {
          name.endsWith(".jar") && !name.endsWith("sources.jar") && !name.endsWith("javadoc.jar") && !name.endsWith("tests.jar")
        }
      })
      if (null != children && 1 == children.length) location = children(0).toURI.getPath
    }
    location
  }



  var _context: SparkContext = null
  var _SQLContext: SQLContext = null

  def createContexts (configuration: JSONObject): Unit = {
    if (null == _context) {
      // Reduce spark logs to only what is necessary
      Logger.getLogger("org.eclipse.jetty").setLevel(Level.WARN)
      Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
      Logger.getLogger("org.apache.hadoop").setLevel(Level.WARN)
      Logger.getLogger("akka").setLevel(Level.WARN)

      val config = new SparkConf().setMaster(master).setAppName(jobName).setSparkHome(sparkHome).setJars(jarList).set("spark.logConf", "true")

      if (null != configuration) {
        JSONObject.getNames(configuration).foreach { key =>
          if (key.toLowerCase().startsWith("akka.") || key.toLowerCase().startsWith("spark.")) {
            try {
              val value = configuration.getString(key)
              config.set(key, value);
            } catch {
              case e: JSONException => warn("Error getting value for key {}", key, e)
            }
          }
        }
      }
      _context = new SparkContext(config)
      _SQLContext = new SQLContext(_context)
    }
  }



  /** @inheritdoc*/
  def getSparkContext(configuration: JSONObject): SparkContext = {
    createContexts(configuration)
    _context
  }

  /** @inheritdoc*/
  def getSQLContext(configuration: JSONObject): SQLContext = {
    createContexts(configuration)
    _SQLContext
  }

  /** @inheritdoc*/
  def shutdownSparkContext = {
    if (null != _context) {
      _context.stop()
      _context = null
      _SQLContext = null
    }
  }
}
