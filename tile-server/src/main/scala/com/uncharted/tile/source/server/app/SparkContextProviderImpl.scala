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

import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.tilegen.util.PropertiesWrapper
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
class SparkContextProviderImpl (_context: SparkContext) extends SparkContextProvider  with Logging {
  val _SQLContext: SQLContext = new SQLContext(_context)



  /** @inheritdoc*/
  def getSparkContext(configuration: JSONObject): SparkContext = {
    _context
  }

  /** @inheritdoc*/
  def getSQLContext(configuration: JSONObject): SQLContext = {
    _SQLContext
  }

  /** @inheritdoc*/
  def shutdownSparkContext = {
    if (null != _context) {
      _context.stop()
    }
  }
}
