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
package com.uncharted.tile.source.server


import java.io.ByteArrayOutputStream
import java.util
import java.util.{Arrays => JavaArrays}

import scala.collection.JavaConverters._

import com.oculusinfo.binning.io.serialization.TileSerializer
import com.rabbitmq.client.QueueingConsumer.Delivery

import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.factory.providers.FactoryProvider

import com.uncharted.tile.source.util.ByteArrayCommunicator



/**
 * This class sets up a tile server, linking it to the appropriate channels, so it can listen to and fulfil tile
 * requests.
 *
 * @param host The host name of the machine on which resides the RabbitMQ server
 * @param pyramidIOFactoryProvider An object that constructs PyramidIO factories to use to fulfil tile requests.
 * @param serializerFactoryProvider An object tjat cpmstricts TileSerializer factories to use to fulfill tile requests
 */
class TileServer(host: String,
                 pyramidIOFactoryProvider: FactoryProvider[PyramidIO],
                 serializerFactoryProvider: FactoryProvider[TileSerializer[_]])
  extends Server(host, TILE_REQUEST_EXCHANGE, LOG_EXCHANGE) {
  override def processRequest(delivery: Delivery): Option[(String, Array[Byte])] = {
    // Get the information we need about this request
    val request = ServerTileRequest.fromByteArray(delivery.getBody)

    // Construct the pyramidIO and serializer we need to fulfil the request
    val pioFactory = pyramidIOFactoryProvider.createFactory("", null, JavaArrays.asList[String]())
    pioFactory.readConfiguration(request.configuration)
    val pyramidIO = pioFactory.produce(classOf[PyramidIO])

    val tsFactory = serializerFactoryProvider.createFactory("", null, JavaArrays.asList[String]())
    tsFactory.readConfiguration(request.configuration)
    val serializer = tsFactory.produce(classOf[TileSerializer[_]])

    // Get our tiles
    val tiles = pyramidIO.readTiles(request.table, serializer, request.indices)

    // Serialize them all
    val tileData = new util.ArrayList[Array[Byte]]()
    tiles.asScala.foreach { tile =>
      val baos = new ByteArrayOutputStream()
      serializer.serialize(tile, baos)
      baos.flush()
      baos.close()
      tileData.add(baos.toByteArray)
    }

    Some((TILE, ByteArrayCommunicator.defaultCommunicator.write(tileData)))
  }
}
