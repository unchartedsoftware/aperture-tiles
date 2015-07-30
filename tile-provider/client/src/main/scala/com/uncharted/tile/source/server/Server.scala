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



import scala.concurrent.{ExecutionContext, Future}

import grizzled.slf4j.Logging
import com.rabbitmq.client.QueueingConsumer
import com.rabbitmq.client.QueueingConsumer.Delivery

import com.uncharted.tile
import com.uncharted.tile.source.RabbitMQConnectable
import com.uncharted.tile.source.util.ByteArrayCommunicator



/**
 * Generic server object for unspecified asynchronous remote calls
 *
 * @param host The host name of the machine on which resides the RabbitMQ server
 */
abstract class Server (host: String, requestExchange: String, logExchange: String)
  extends RabbitMQConnectable(host) with Logging
{
  import ExecutionContext.Implicits.global

  // The RabbitMQ message consumer that listens for tile requests
  private val _consumer = {
    // Create a tile request channel, on which we will listen for tile requests.
    _channel.exchangeDeclare(requestExchange, "fanout", false, true, false, null)
    // Create a channel to which to send log messages
    _channel.exchangeDeclare(logExchange, "direct", false, true, false, null)
    // Create a consumer to handle tile requests
    new QueueingConsumer(_channel)
  }

  /**
   * Process a server request
   * @param delivery The message containing the server request
   * @return Optionally, a pair containing the content type and the content of the return message.  None if there is
   *         no return message required.
   */
  def processRequest(delivery: Delivery): Option[(String, Array[Byte])]

  def processError(throwable: Throwable): Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(throwable)

  private var _shutdown = false
  private var _started = false
  def shutdown: Unit = {
    _shutdown = true
  }

  def startRequestThread: Future[Unit] = {
    val requestFuture = concurrent.future(listenForRequests)
    while (!_started) {
      Thread.sleep(10)
    }
    requestFuture
  }

  /**
   * Listen for tile requests messages, and attempt to fil them, indefinitely.
   */
  def listenForRequests: Unit = {
    info("RabbitMQ request server listening")
    // Set up a private queue on which to listen for requests
    val queueName = _channel.queueDeclare.getQueue
    // No routing key - we accept any tile request.
    _channel.queueBind(queueName, requestExchange, "")
    _channel.basicQos(1)
    _channel.basicConsume(queueName, false, _consumer)

    // Loop continually, accepting tile requests, until told to shut down
    while (!_shutdown) {
      _started = true
      val delivery = _consumer.nextDelivery(100)
      if (null != delivery) {
        val responseQueue = delivery.getProperties.getReplyTo
        info("RabbitMQ server got a delivery. Replying on channel "+responseQueue)
        try {
          processRequest(delivery).foreach { response =>
            info("RabbitMQ server responding on channel "+responseQueue)
            oneOffDirectMessage(responseQueue, response._1, response._2)
          }
          info("RabbitMQ server finished processing message for channel "+responseQueue)
        } catch {
          case t0: Throwable => {
            info("RabbitMQ server got an error processing message for channel "+responseQueue, t0)
            val encodedError = processError(t0)
            _channel.basicPublish(logExchange, tile.source.LOG_WARNING, null, encodedError)
            try {
              oneOffDirectMessage(responseQueue, tile.source.LOG_WARNING, encodedError)
            } catch {
              case t1: Throwable => {
                error("Error writing error message", t1)
                _channel.basicPublish(logExchange, tile.source.LOG_ERROR, null, processError(t1))
              }
            }
          }
        } finally {
          _channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
        }
      }
    }
  }
}

