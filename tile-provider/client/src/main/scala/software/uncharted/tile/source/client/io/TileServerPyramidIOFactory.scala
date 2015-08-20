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
package software.uncharted.tile.source.client.io

import java.util.{List => JavaList}

import com.oculusinfo.binning.io.{PyramidIOFactory, PyramidIO}
import com.oculusinfo.factory.properties.{IntegerProperty, StringProperty}
import com.oculusinfo.factory.{SharedInstanceFactory, ConfigurableFactory}
import grizzled.slf4j.Logging
import org.json.JSONObject

import scala.util.Try


object TileServerPyramidIOFactory {
  val BROKER_HOST = new StringProperty("brokerHost", "The host name of the RabbitMQ message broker", "localhost")
  val BROKER_USER = new StringProperty("brokerUser", "The user name with which to log in to the RabbitMQ message broker", "")
  val BROKER_PASSWORD  = new StringProperty("brokerPassword", "The password with which to log in to the RabbitMQ message broker", "")
  val MAXIMUM_WAIT_TIME = new IntegerProperty("maxWaitTime", "The maximum amount of time to wait for a tile, in milliseconds", 2000)
}
/**
 * Factory for building a pyramid io that retrieves tiles from a tile-provider server
 */
class TileServerPyramidIOFactory  (parent: ConfigurableFactory[_],
                                   path: JavaList[String])
  extends SharedInstanceFactory[PyramidIO]("tile-server", classOf[PyramidIO], parent, path) with Logging
{
  import TileServerPyramidIOFactory._

  addProperty(PyramidIOFactory.INITIALIZATION_DATA)
  addProperty(BROKER_HOST)
  addProperty(BROKER_USER)
  addProperty(BROKER_PASSWORD)
  addProperty(MAXIMUM_WAIT_TIME)

  override protected def createInstance: PyramidIO = {
    Try {
      val config: JSONObject = getPropertyValue(PyramidIOFactory.INITIALIZATION_DATA)
      val brokerHost = getPropertyValue(BROKER_HOST)
      val brokerUser = getPropertyValue(BROKER_USER)
      val brokerPassword = getPropertyValue(BROKER_PASSWORD)
      val maxWaitTime = getPropertyValue(MAXIMUM_WAIT_TIME).toLong
      new TileServerPyramidIO(brokerHost, brokerUser, brokerPassword, maxWaitTime)
    }.recover {
      case t: Throwable => {
        error("Error trying to create FileBasedPyramidIO", t)
        null
      }
    }.get
  }
}
