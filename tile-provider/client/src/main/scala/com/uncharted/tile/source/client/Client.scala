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

import scala.collection.mutable.{HashMap, SynchronizedMap}
import scala.concurrent.{ExecutionContext, Future}

import grizzled.slf4j.Logging

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.QueueingConsumer

import com.uncharted.tile.source.RabbitMQConnectable
import com.uncharted.tile.source.util.ByteArrayCommunicator


/**
 * A basic, generic client that communicates with our basic, generic server.
 *
 * @tparam RT The type of request to be made
 */
abstract class Client[RT] (host: String, user: String, pswd: String,
                           requestExchange: String, responseExchange: String)
  extends RabbitMQConnectable(host, user, pswd) with Logging
{
  import com.uncharted.tile
  import ExecutionContext.Implicits.global

  // Whether or not this client has had its response thread started.
  private var _started = false
  // Whether or not this client has been shut down
  private var _shutdown = false
  // Our client ID, by which the server knows how to route request responses to us.
  val _requesterId = UUID.randomUUID().toString
  // All pending, unfulfilled requests.
  private val _pending = new HashMap[String, RT] with SynchronizedMap[String, RT]

  /**
   * Start up the response thread of this client, so that it listens for responses to its requests.
   *
   * This is really just a convenient wrapper for {@link #listenForResponses}
   *
   * @return A future that can be watched to see when the client has been shut down.
   */
  def startResponseThread: Future[Unit] = {
    val responseFuture = concurrent.future(listenForResponses)
    while (!_started) {
      Thread.sleep(10)
    }
    responseFuture
  }

  /**
   * Schedule the response thread to shut down at its earliest convenience.
   */
  def shutdown: Unit = {
    _shutdown = true
  }

  /**
   * Listen for tile requests messages, and attempt to fill them, indefinitely.
   */
  def listenForResponses: Unit = {
    info("RabbitMQ client listening for responses")
    // Create a request channel, on which we will listen for requests.
    _channel.exchangeDeclare(requestExchange, "fanout", false, true, false, null)
    // Create a response channel, to which we publish responses to requests.
    _channel.exchangeDeclare(responseExchange, "direct", false, true, false, null)

    // Set up a private queue on which to listen for requests
    val queueName = _channel.queueDeclare().getQueue
    _channel.queueBind(queueName, responseExchange, _requesterId)
    _channel.basicQos(1)
    // Create a consumer to handle tile requests
    val consumer = new QueueingConsumer(_channel)
    _channel.basicConsume(queueName, false, consumer)

    // Loop continually, accepting tile requests, until told to shut down
    while (!_shutdown) {
      _started = true
      val delivery = consumer.nextDelivery(100)
      if (null != delivery) {
        val messageId = delivery.getProperties.getMessageId
        _pending.remove(messageId).map{request =>
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
    }
  }


  /**
   * Make a single request of the server
   * @param request
   */
  def makeRequest (request: RT): Unit = {
    // Create a unique message ID to differentiate the response to this message from that to other messages
    val messageId = UUID.randomUUID.toString
    // Add our response listener to the list of pending reqeusts.
    _pending(messageId) = request
    // Create our tile request data
    val encodedRequest = encodeRequest(request)

    // Send our message
    _channel.basicPublish(requestExchange, "",
      new BasicProperties.Builder().replyTo(_requesterId).messageId(messageId).build(),
      encodeRequest(request))
  }

  private def onError (request: RT, severity: String, rawError: Array[Byte]): Unit =
    processError(request, severity, ByteArrayCommunicator.defaultCommunicator.read[Throwable](rawError))



  /**
   * Encode a request so that it can be sent through RabbitMQ.
   */
  def encodeRequest (request: RT): Array[Byte]

  /**
   * Process an error s returned from the server.
   */
  def processError (request: RT, severity: String, serverError: Throwable): Unit = {
    severity match {
      case tile.source.LOG_ERROR => error("Major server error", serverError)
      case tile.source.LOG_WARNING => warn("Server error", serverError)
      case tile.source.LOG_INFO => info("Minor server error", serverError)
      case tile.source.LOG_DEBUG => debug("Server debug: ", serverError)
    }
  }

  /**
   * Process the data returned from the server when a request is filled.
   * @param request The request that was made
   * @param contentType The content type of the response to said request
   * @param contents The data containing the response to said request
   */
  def processResults (request: RT, contentType: String, contents: Array[Byte])
}
