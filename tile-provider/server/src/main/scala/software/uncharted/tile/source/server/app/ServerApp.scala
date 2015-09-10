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
package software.uncharted.tile.source.server.app

import org.apache.log4j.{Level, Logger}

import scala.collection.JavaConverters._

import grizzled.slf4j.Logging

import com.oculusinfo.binning.io.DefaultPyramidIOFactoryProvider
import com.oculusinfo.binning.io.serialization.DefaultTileSerializerFactoryProvider
import com.oculusinfo.tilegen.util.{MissingArgumentException, ArgumentParser}
import software.uncharted.tile.source.server.io.{OnDemandTilePyramidIOFactoryProvider, StandardPyramidIOFactoryProvider}
import software.uncharted.tile.source.server.SimpleTileServer
import scala.reflect.runtime.universe

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

      // Get rid of extraneous spark logging
      Logger.getLogger("akka").setLevel(Level.WARN)
      Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
      Logger.getLogger("org.spark-project").setLevel(Level.WARN)
      Logger.getLogger("org.apache.hadoop").setLevel(Level.WARN)

      val context = argParser.getSparkConnector().createContext(Some(jobName))
      val contextProvider = new SparkContextProviderImpl(context)

      val rabbitMQHost = argParser.getString("rabbitMQHost",
        "The host name of the machine on which the RabbitMQ broker is running",
        Some("localhost"))
      val rabbitMQUser = argParser.getString("rabbitMQUser",
        "The user name with which to log in to the RabbitMQ broker",
        None)
      val rabbitMQPassword = argParser.getString("rabbitMQPassword",
        "The password with which to log in to the RabbitMQ broker",
        None)
      val pyramidIOFactoryProvider = new StandardPyramidIOFactoryProvider((DefaultPyramidIOFactoryProvider.values() :+ new OnDemandTilePyramidIOFactoryProvider(contextProvider)).toSet.asJava)

      val server = new SimpleTileServer(rabbitMQHost, rabbitMQUser, rabbitMQPassword, pyramidIOFactoryProvider)
      server.listenForRequests
    } catch {
      case mae: MissingArgumentException => {
        error("Argument exception: " + mae.getMessage)
        argParser.usage
      }
    }
  }
}
