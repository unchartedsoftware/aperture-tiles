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

import com.rabbitmq.client.AMQP.BasicProperties

import scala.concurrent.{ExecutionContext, Future}

import grizzled.slf4j.Logging
import com.rabbitmq.client.QueueingConsumer
import com.rabbitmq.client.QueueingConsumer.Delivery

import com.uncharted.tile
import com.uncharted.tile.source
import com.uncharted.tile.source.RabbitMQConnectable
import com.uncharted.tile.source.util.ByteArrayCommunicator



/**
 * Generic server object for unspecified asynchronous remote calls
 *
 * @param host The host name of the machine on which resides the RabbitMQ server
 */
abstract class Server (host: String, user: String, pswd: String,
                       requestExchange: String, responseExchange: String, logExchange: String)
  extends RabbitMQConnectable(host, user, pswd) with Logging
{
  import ExecutionContext.Implicits.global

  // Whether or not this server has had its request thread started.
  private var _started = false
  // Whether or not this server has been shut down
  private var _shutdown = false
  // The RabbitMQ message consumer that listens for tile requests
  private val _consumer = {
    // Create a request channel, on which we will listen for requests.
    _channel.exchangeDeclare(requestExchange, "fanout", false, true, false, null)
    // Create a response channel, to which we publish responses to requests.
    _channel.exchangeDeclare(responseExchange, "direct", false, true, false, null)
    // Create a channel to which to send log messages
    _channel.exchangeDeclare(logExchange, "direct", false, true, false, null)
    // Create a consumer to handle tile requests
    new QueueingConsumer(_channel)
  }

  /**
   * Starts up the request thread of this server, so it listens for requests.
   *
   * This is really just a convenient wrapper for {@link #listenForRequests}
   *
   * @return A future that can be watched to see when the server has been shut down.
   */
  def startRequestThread: Future[Unit] = {
    val requestFuture = concurrent.future(listenForRequests)
    while (!_started) {
      Thread.sleep(10)
    }
    requestFuture
  }

  /**
   * Schedule the request thread to shut down at its earliest convenience.
   */
  def shutdown: Unit = {
    _shutdown = true
  }

  /**
   * Listen for tile requests messages, and attempt to fill them, indefinitely.
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
        val requesterId = delivery.getProperties.getReplyTo
        val messageId = delivery.getProperties.getMessageId
        try {
          val response = processRequest(delivery)
          if (response.isDefined) {
            _channel.basicPublish(responseExchange, requesterId,
              new BasicProperties.Builder().contentType(response.get._1).messageId(messageId).build,
              response.get._2)
          } else {
            _channel.basicPublish(responseExchange, requesterId,
              new BasicProperties.Builder().contentType(source.UNIT_TYPE).messageId(messageId).build,
              source.UNIT_RESPONSE)
          }
        } catch {
          case t0: Throwable => {
            info("RabbitMQ server got an error processing message for requester "+requesterId+" for message "+messageId, t0)
            val encodedError = processError(t0)
            _channel.basicPublish(logExchange, tile.source.LOG_WARNING, null, encodedError)
            try {
              _channel.basicPublish(responseExchange, requesterId,
                new BasicProperties.Builder().contentType(tile.source.LOG_WARNING).messageId(messageId).build,
                encodedError)
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

  /**
   * Process a server request
   * @param delivery The message containing the server request
   * @return Optionally, a pair containing the content type and the content of the return message.  None if there is
   *         no return message required.
   */
  def processRequest(delivery: Delivery): Option[(String, Array[Byte])]

  /**
   * Process an error thrown by request processing.
   */
  def processError(throwable: Throwable): Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(throwable)
}

