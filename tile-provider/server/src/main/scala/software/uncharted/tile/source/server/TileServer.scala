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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util
import java.util.{Arrays => JavaArrays}

import com.oculusinfo.binning.util.JsonUtilities
import grizzled.slf4j.Logging

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}

import com.rabbitmq.client.QueueingConsumer.Delivery

import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.factory.providers.FactoryProvider

import com.uncharted.tile
import com.uncharted.tile.source.{RequestTypes, TileInitializationRequest, TileMetaDataRequest, TileDataRequest, TileStreamRequest}
import com.uncharted.tile.source.util.ByteArrayCommunicator



/**
 * This class sets up a tile server, linking it to the appropriate channels, so it can listen to and fulfil tile
 * requests.
 *
 * @param host The host name of the machine on which resides the RabbitMQ server
 * @param pyramidIOFactoryProvider An object that constructs PyramidIO factories to use to fulfil tile requests.
 */
class TileServer(host: String, user: String, password: String,
                 pyramidIOFactoryProvider: FactoryProvider[PyramidIO])
  extends Server(host, user, password,
    tile.source.TILE_REQUEST_EXCHANGE, tile.source.TILE_RESPONSE_EXCHANGE, tile.source.LOG_EXCHANGE) with Logging
{
  val pyramidIOs = MutableMap[String, PyramidIO]()
  override def processRequest(delivery: Delivery): Option[(String, Array[Byte])] = {
    // Get the information we need about this request
    val request = ServerTileRequest.fromByteArray(delivery.getBody)

    info("request type: "+request.requestType)
    request match {
      case tir: TileInitializationRequest => {
        info("Initialization request for tile set "+tir.table)
        debug("Height: "+tir.height)
        debug("Width: "+tir.width)
        debug("Configuration: "+tir.configuration)

        // Construct the pyramidIO we need to fulfil the request
        val pioFactory = pyramidIOFactoryProvider.createFactory(null, null, JavaArrays.asList[String]())
        pioFactory.readConfiguration(tir.configuration)
        val pyramidIO = pioFactory.produce(classOf[PyramidIO])
        pyramidIO.initializeForRead(tir.table, tir.width, tir.height, JsonUtilities.jsonObjToProperties(tir.configuration))
        pyramidIOs(tir.table) = pyramidIO
        None
      }
      case tmr: TileMetaDataRequest => {
        info("Metadata request for tile set "+tmr.table)

        if (pyramidIOs.get(tmr.table).isEmpty) throw new IllegalArgumentException("Attempt to get metadata for uninitialized pyramid "+tmr.table)

        val pio = pyramidIOs(tmr.table)
        Some((RequestTypes.Metadata.toString, pio.readMetaData(tmr.table).getBytes))
      }
      case tdrRaw: TileDataRequest[_] => {
        info("Tile request for tile set "+tdrRaw.table)
        debug("Serializer: "+tdrRaw.serializer.getClass)
        debug("Tiles: "+tdrRaw.indices.asScala.toList)

        if (pyramidIOs.get(tdrRaw.table).isEmpty) throw new IllegalArgumentException("Attempt to get tile data for uninitialized pyramid "+tdrRaw.table)

        def doWork[T] (tdr: TileDataRequest[T]) = {
          val pio = pyramidIOs(tdr.table)
          val tiles = pio.readTiles(tdr.table, tdr.serializer, tdr.indices)

          // Serialize them all
          val tileData = new util.ArrayList[Array[Byte]]()
          tiles.asScala.foreach { tile =>
            val baos = new ByteArrayOutputStream()
            tdr.serializer.serialize(tile, baos)
            baos.flush()
            baos.close()
            tileData.add(baos.toByteArray)
          }

          Some((RequestTypes.Tiles.toString, ByteArrayCommunicator.defaultCommunicator.write(tileData)))
        }
        doWork(tdrRaw)
      }
      case tsrRaw: TileStreamRequest[_] => {
        info("Tile stream request for tile set "+tsrRaw.table)
        debug("Serializer: "+(if (tsrRaw.serializer == null) "nul" else tsrRaw.serializer.getClass))
        debug("Index: "+tsrRaw.index)

        if (pyramidIOs.get(tsrRaw.table).isEmpty) throw new IllegalArgumentException("Attempt to get tile stream for uninitialized pyramid "+tsrRaw.table)

        def doWork[T] (tsr: TileStreamRequest[T]) = {
          val pio = pyramidIOs(tsr.table)
          val inputStream = pio.getTileStream(tsr.table, tsr.serializer, tsr.index)

          val outputStream = new ByteArrayOutputStream()
          var b = inputStream.read
          while (b != -1) {
            outputStream.write(b)
            b = inputStream.read
          }
          inputStream.close
          outputStream.flush
          outputStream.close

          Some((RequestTypes.TileStream.toString, outputStream.toByteArray))
        }

        doWork(tsrRaw)
      }
    }
  }
}
