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
package software.uncharted.tile.source.server



import java.util.{Arrays => JavaArrays}

import scala.collection.mutable.{Map => MutableMap}

import com.rabbitmq.client.QueueingConsumer.Delivery
import grizzled.slf4j.Logging

import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.factory.providers.FactoryProvider

import software.uncharted.tile.source._
import software.uncharted.tile



/**
 * This class sets up a tile server, linking it to the appropriate channels, so it can listen to and fulfil tile
 * requests.
 *
 * This is a trivial version, meant mostly just for testing, that only produces a single tile at a time.
 *
 * @param host The host name of the machine on which resides the RabbitMQ server
 * @param pyramidIOFactoryProvider An object that constructs PyramidIO factories to use to fulfil tile requests.
 */
class MultiTileServer(host: String, user: String, password: String,
                      pyramidIOFactoryProvider: FactoryProvider[PyramidIO])
  extends Server(host, user, password,
    tile.source.TILE_REQUEST_EXCHANGE,
    tile.source.TILE_RESPONSE_EXCHANGE,
    tile.source.LOG_EXCHANGE,
    tile.source.DEFAULT_GRANULARITY)
  with Logging
  with QueueingServer {
  val pyramidIOs = MutableMap[String, PyramidIO]()

  /**
   * The time to wait after receiving a request before we start processing it, so as to make sure all requests in
   * that queue have been received.
   */
  override def getWaitTime(queueId: Any): Long = 100

  /**
   * Determine the queue in which a given delivery should be processed
   * @param delivery The delivery in question
   * @return The queue in which the delivery should be processed
   */
  override def getQueue(delivery: Delivery): Any = {
    ServerTileRequest.fromByteArray(delivery.getBody) match {
      case tir: TileInitializationRequest => None
      case tmr: TileMetaDataRequest => None
      case tdrRaw: TileDataRequest[_] => Some(tdrRaw.table)
      case tsrRaw: TileStreamRequest[_] => Some(tsrRaw.table)
    }
  }

  /**
   * Process a bunch of requests
   * @param deliveries The messages containing the requests
   * @return For each request, and in the same order, either an optional response containing the content type and
   *         the content of the response to each message (with None indicating a Unit response), or an error thrown
   *         in the process of trying to fulfil the request.
   */
  override def processRequests(deliveries: Array[Delivery]): Array[Either[Option[(String, Array[Byte])], Throwable]] = {
    deliveries.map { delivery =>
      try {
        ServerTileRequest.fromByteArray(delivery.getBody) match {
          case tir: TileInitializationRequest => {
            info("Initialization request for tile set " + tir.table)
            debug("Height: " + tir.height)
            debug("Width: " + tir.width)
            debug("Configuration: " + tir.configuration)

            // Construct the pyramidIO we need to fulfil the request
            val pioFactory = pyramidIOFactoryProvider.createFactory(null, null, JavaArrays.asList[String]())
            pioFactory.readConfiguration(tir.configuration)
            val pyramidIO = pioFactory.produce(classOf[PyramidIO])
            pyramidIO.initializeForRead(tir.table, tir.width, tir.height, JsonUtilities.jsonObjToProperties(tir.configuration))
            pyramidIOs(tir.table) = pyramidIO
            Left(None)
          }
          case tmr: TileMetaDataRequest => {
            info("Metadata request for tile set " + tmr.table)

            if (pyramidIOs.get(tmr.table).isEmpty) throw new IllegalArgumentException("Attempt to get metadata for uninitialized pyramid " + tmr.table)

            val pio = pyramidIOs(tmr.table)
            Left(Some((RequestTypes.Metadata.toString, pio.readMetaData(tmr.table).getBytes)))
          }
          case tdrRaw: TileDataRequest[_] => Some(tdrRaw.table)
          case tsrRaw: TileStreamRequest[_] => Some(tsrRaw.table)
        }
      } catch {
        case t: Throwable => Right(t)
      }
    }
  }
}
