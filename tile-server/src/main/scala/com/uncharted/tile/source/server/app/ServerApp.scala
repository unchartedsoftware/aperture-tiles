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



import scala.collection.JavaConverters._

import grizzled.slf4j.Logging

import com.oculusinfo.binning.io.DefaultPyramidIOFactoryProvider
import com.oculusinfo.binning.io.serialization.DefaultTileSerializerFactoryProvider
import com.oculusinfo.tilegen.util.{MissingArgumentException, ArgumentParser}
import com.uncharted.tile.source.server.io.{OnDemandTilePyramidIOFactoryProvider, StandardPyramidIOFactoryProvider}
import com.uncharted.tile.source.server.TileServer


/**
 * A stand-alone application that starts a spark context, and sets up a tile server to run on it, so as to server
 * tiles on-demand
 */
object ServerApp extends Logging {
  def main (args: Array[String]): Unit = {
    val argParser = new ArgumentParser(args)
    try {
      val jobName = argParser.getString("serverName",
        "The name by which the tile server is known on the spark task web interface",
        Some("On-demand tile server"))
      val context = argParser.getSparkConnector().createContext()
      val contextProvider = new SparkContextProviderImpl(context)

      val rabbitMQHost = argParser.getString("rabbitMQHost",
        "The host name of the machine on which the RabbitMQ server is running",
        Some("localhost"))
      val pyramidIOFactoryProvider = new StandardPyramidIOFactoryProvider((DefaultPyramidIOFactoryProvider.values() :+ new OnDemandTilePyramidIOFactoryProvider(contextProvider)).toSet.asJava)

      val server = new TileServer(rabbitMQHost, pyramidIOFactoryProvider)
    } catch {
      case mae: MissingArgumentException => {
        error("Argument exception: " + mae.getMessage)
        argParser.usage
      }
    }
  }
}
