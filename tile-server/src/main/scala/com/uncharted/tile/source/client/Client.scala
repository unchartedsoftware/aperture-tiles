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



import java.util.UUID

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.QueueingConsumer
import grizzled.slf4j.Logging

import com.uncharted.tile.source.server.RabbitMQConnectable
import com.uncharted.tile.source.util.ByteArrayCommunicator



/**
 * A basic, generic client that communicates with our basic, generic server.
 *
 * @tparam RT The type of request to be made
 */
abstract class Client[RT] (host: String, requestExchange: String) extends RabbitMQConnectable(host) with Logging {
  import com.uncharted.tile
  import scala.concurrent.ExecutionContext.Implicits.global

  def encodeRequest (request: RT): Array[Byte]
  def processError (request: RT, severity: String, serverError: Throwable): Unit = {
    severity match {
      case tile.source.LOG_ERROR => error("Major server error", serverError)
      case tile.source.LOG_WARNING => warn("Server error", serverError)
      case tile.source.LOG_INFO => info("Minor server error", serverError)
      case tile.source.LOG_DEBUG => debug("Server debug: ", serverError)
    }
  }
  def processResults (request: RT, contentType: String, contents: Array[Byte])

  def makeRequest (request: RT): Unit = {
    // Create a unique channel on which to listen for responses
    var replyQueue = UUID.randomUUID.toString
    // Listen on our response channel for our tiles
    concurrent.future(new RequestResponseListener(replyQueue, request))
    // Create our tile request data
    val encodedRequest = encodeRequest(request)

    // Send our message
    _channel.basicPublish(requestExchange, "", new BasicProperties.Builder().replyTo(replyQueue).build(), encodeRequest(request))
  }

  private def onError (request: RT, severity: String, rawError: Array[Byte]): Unit =
    processError(request, severity, ByteArrayCommunicator.defaultCommunicator.read[Throwable](rawError))

  class RequestResponseListener (queue: String, request: RT) {
    val consumer = new QueueingConsumer(_channel)
    _channel.queueDeclare(queue, false, false, false, null)
    _channel.basicQos(1)
    _channel.basicConsume(queue, true, consumer)
    val delivery = consumer.nextDelivery()
    val contentType = delivery.getProperties.getContentType
    contentType match {
      case tile.source.LOG_ERROR => onError(request, tile.source.LOG_ERROR, delivery.getBody)
      case tile.source.LOG_WARNING => onError(request, tile.source.LOG_WARNING, delivery.getBody)
      case tile.source.LOG_INFO => onError(request, tile.source.LOG_INFO, delivery.getBody)
      case tile.source.LOG_DEBUG => onError(request, tile.source.LOG_DEBUG, delivery.getBody)
      case _ => processResults(request, contentType, delivery.getBody)
    }
  }
}
