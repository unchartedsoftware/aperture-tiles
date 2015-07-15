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
                 serializerFactoryProvider: FactoryProvider[TileSerializer[_]]) extends RabbitMQConnectable(host) {
  import com.uncharted.tile.source.server
  import scala.concurrent.ExecutionContext.Implicits.global

  def requestTile[T] (request: ClientTileRequest[T]): Unit = {
    // Create a unique channel on which to listen for responses
    var replyQueue = UUID.randomUUID.toString
    // Listen on our response channel for our tiles
    concurrent.future(new TileRequestResponseListener(replyQueue, request))
    // Create our tile request data
    val postableRequest = ServerTileRequest.toByteArray(request)

    // Send our message
    _channel.basicPublish(server.TILE_REQUEST_EXCHANGE, "", new BasicProperties.Builder().replyTo(replyQueue).build(), postableRequest)
  }

  class TileRequestResponseListener[T] (queue: String, request: ClientTileRequest[T]) {
    val consumer = new QueueingConsumer(_channel)
    _channel.queueDeclare(queue, false, false, false, null)
    _channel.basicQos(1)
    _channel.basicConsume(queue, true, consumer)
    val delivery = consumer.nextDelivery()
    val contentType = delivery.getProperties.getContentType
    contentType match {
      case server.TILE => {
        // Get a serializer to deserialize our results
        val tsFactory = serializerFactoryProvider.createFactory("", null, JavaArrays.asList[String]())
        tsFactory.readConfiguration(request.configuration)
        val serializer = tsFactory.produce(classOf[TileSerializer[T]])

        // Extract our results
        val encodedTiles = ByteArrayCommunicator.defaultCommunicator.read[JavaList[Array[Byte]]](delivery.getBody)
        val tiles = new util.ArrayList[TileData[T]]()
        for (n <- 0 until request.indices.size()) {
          val bais = new ByteArrayInputStream(encodedTiles.get(n))
          tiles.add(serializer.deserialize(request.indices.get(n), bais))
        }
        request.onTileRetrieved(tiles)
      }
      case _ => {
        // Must be an error.
        val severity = contentType
        val error = ByteArrayCommunicator.defaultCommunicator.read[Throwable](delivery.getBody)
        request.onError(error)
      }
    }
  }
}
