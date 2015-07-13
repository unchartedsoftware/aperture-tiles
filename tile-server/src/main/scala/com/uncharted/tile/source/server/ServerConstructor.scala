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



import java.util.{Arrays => JavaArrays}
import java.util.{List => JavaList}

import com.oculusinfo.binning.io.serialization.TileSerializer
import org.json.JSONObject

import com.rabbitmq.client.QueueingConsumer

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.io.{PyramidIO, DefaultPyramidIOFactoryProvider}
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
class ServerConstructor(host: String,
                        pyramidIOFactoryProvider: FactoryProvider[PyramidIO],
                        serializerFactoryProvider: FactoryProvider[TileSerializer[_]]) extends RabbitMQConnectable(host) {
  // The RabbitMQ message consumer that listens for tile requests
  private val _consumer = {
    // Create a tile request channel, on which we will listen for tile requests.
    _channel.exchangeDeclare(TILE_REQUEST_EXCHANGE, "fanout")
    // Create a channel to which to send log messages
    _channel.exchangeDeclare(LOG_EXCHANGE, "direct")
    // Create a consumer to handle tile requests
    new QueueingConsumer(_channel)
  }

  /**
   * Listen for tile requests messages, and attempt to fil them, indefinitely.
   */
  def listenForRequests: Unit = {
    // Set up a private queue on which to listen for requests
    val queueName = _channel.queueDeclare.getQueue
    // No routing key - we accept any tile request.
    _channel.queueBind(queueName, TILE_REQUEST_EXCHANGE, "")
    _channel.basicQos(1)
    _channel.basicConsume(queueName, false, _consumer)

    // Loop continually, accepting tile requests, until told to shut down
    var _interrupt = false
    while (!_interrupt) {
      val delivery = _consumer.nextDelivery()
      try {
        // Get the information we need about this request
        // TODO: Encapsulate this in a request object.
        val (responseQueue, configuration, table, indices) =
          ByteArrayCommunicator.defaultCommunicator.read[String, JSONObject, String, JavaList[TileIndex]](delivery.getBody)

        // Construct the pyramidIO and serializer we need to fulfil the request
        val pioFactory = pyramidIOFactoryProvider.createFactory("", null, JavaArrays.asList[String]())
        pioFactory.readConfiguration(configuration)
        val pyramidIO = pioFactory.produce(classOf[PyramidIO])

        val tsFactory = serializerFactoryProvider.createFactory("", null, JavaArrays.asList[String]())
        tsFactory.readConfiguration(configuration)
        val serializer = tsFactory.produce(classOf[TileSerializer[_]])

        // Get our tiles
        val tiles = pyramidIO.readTiles(table, serializer, tiles)

        // Return our tiles.
        oneOffDirectMessage(responseQueue, ByteArrayCommunicator.defaultCommunicator.write("TILES", tiles))
      } catch {
        case t0: Throwable => {
          val error = ByteArrayCommunicator.defaultCommunicator.write("ERROR", t0)
          _channel.basicPublish(LOG_EXCHANGE, LOG_WARNING, null, error)
          try {
            val responseQueue = ByteArrayCommunicator.defaultCommunicator.read[String](delivery.getBody)
            oneOffDirectMessage(responseQueue, error)
          } catch {
            case t1: Throwable => {
              _channel.basicPublish(LOG_EXCHANGE, LOG_ERROR, null, ByteArrayCommunicator.defaultCommunicator.write("ERROR", t1))
            }
          }
        }
      } finally {
        _channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
      }
    }
  }
}
