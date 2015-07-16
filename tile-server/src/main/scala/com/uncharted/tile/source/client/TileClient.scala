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
package com.uncharted.tile.source.client


import java.io.ByteArrayInputStream
import java.util
import java.util.{List => JavaList, Arrays => JavaArrays, UUID}
import com.uncharted.tile.source.server

import scala.collection.JavaConverters._

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.factory.providers.FactoryProvider

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.QueueingConsumer
import com.uncharted.tile.source.server.{ServerTileRequest, RabbitMQConnectable}
import com.uncharted.tile.source.util.ByteArrayCommunicator



/**
 * This class handles communications with a tile server, wrapping tile requests appropriately, and letting users know
 * when tiles are completed (or have errored).
 */
class TileClient(host: String,
                 serializerFactoryProvider: FactoryProvider[TileSerializer[_]])
extends Client[ClientTileRequest[_]](host, server.TILE_REQUEST_EXCHANGE) {
  def encodeRequest(request: ClientTileRequest[_]): Array[Byte] =
    ServerTileRequest.toByteArray(request)

  def processResults(request: ClientTileRequest[_], contentType: String, contents: Array[Byte]) =
    processResultsInternal(request, contentType, contents)

  def processResultsInternal[T] (request: ClientTileRequest[T], contentType: String, contents: Array[Byte]): Unit = {
    // Get a serializer to deserialize our results
    val tsFactory = serializerFactoryProvider.createFactory("", null, JavaArrays.asList[String]())
    tsFactory.readConfiguration(request.configuration)
    val serializer = tsFactory.produce(classOf[TileSerializer[T]])

    // Extract our results
    val encodedTiles = ByteArrayCommunicator.defaultCommunicator.read[JavaList[Array[Byte]]](contents)
    val tiles = new util.ArrayList[TileData[T]]()
    for (n <- 0 until request.indices.size()) {
      val bais = new ByteArrayInputStream(encodedTiles.get(n))
      tiles.add(serializer.deserialize(request.indices.get(n), bais))
    }
    request.onTileRetrieved(tiles)
  }
}
