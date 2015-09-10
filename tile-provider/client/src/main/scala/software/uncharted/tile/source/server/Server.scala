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

import software.uncharted.tile
import software.uncharted.tile.source
import software.uncharted.tile.source.RabbitMQConnectable
import software.uncharted.tile.source.util.ByteArrayCommunicator


/**
 * Generic server object for unspecified asynchronous remote calls
 *
 * This server is designed to be used with one of a few simple mix-ins that control how messages are answered,
 * notably (@link SimpleServer} and {@link CollectingServer}
 *
 * @param host The host name of the machine on which resides the RabbitMQ server
 * @param user The user name with which to log on to the RabbitMQ server
 * @param password The password with which to log on to the specified user on the RabbitMQ server
 * @param requestExchange The exchange on which to listen to requests
 * @param responseExchange The exchange on which to publish responses
 * @param logExchange The exchange on which to publish log messages
 * @param granularity The minimum time, in milliseconds, between processing requests
 */
abstract class Server(host: String, user: String, password: String,
                      requestExchange: String, responseExchange: String, logExchange: String,
                      granularity: Long)
  extends RabbitMQConnectable(host, user, password) with Logging with ServerPublications {

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
      onDelivery(_consumer.nextDelivery(granularity))
    }
  }

  /**
   * Process a single message to this server
   * @param delivery The message that has been received - or null if no message has been received in the most recent
   *                 message poll.
   */
  def onDelivery(delivery: Delivery): Unit


  // ///////////////////////////////////////////////////////////////////////////////////////////////
  // Publication functions, for use by our mixins
  //
  /**
   * {@inheritdoc}
   */
  def acknowledge(delivery: Delivery): Unit = {
    _channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
  }

  /**
   * {@inheritdoc}
   */
  def publishResponse(delivery: Delivery, response: Option[(String, Array[Byte])]): Unit = {
    try {
      val requesterId = delivery.getProperties.getReplyTo
      val messageId = delivery.getProperties.getMessageId
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
      case t: Throwable =>
        error("Error publishing valid response " + response + " to message " + delivery)
    }
  }

  /**
   * {@inheritdoc}
   */
  def publishError(delivery: Delivery, primaryError: Throwable): Unit = {
    try {
      val requesterId = delivery.getProperties.getReplyTo
      val messageId = delivery.getProperties.getMessageId
      info("RabbitMQ server got an error processing message for requester " + requesterId + " for message " + messageId, primaryError)
      val encodedError = encodeError(primaryError)
      _channel.basicPublish(logExchange, tile.source.LOG_WARNING, null, encodedError)
      _channel.basicPublish(responseExchange, requesterId,
        new BasicProperties.Builder().contentType(tile.source.LOG_WARNING).messageId(messageId).build,
        encodedError)
    } catch {
      case secondaryError: Throwable => {
        error("Error writing error message", primaryError)
      }
    }
  }

  /**
   * Process an error thrown by request processing.
   */
  def encodeError(throwable: Throwable): Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(throwable)
}


/*
 * A small mixin to guarantee that server mixins can call a few basic server publication methods.
 */
trait ServerPublications {
  /**
   * Acknowledge a delivery, so that the RabbitMQ server knows it has been answered, and doesn't ask again.
   * @param delivery The answered message.
   */
  def acknowledge(delivery: Delivery): Unit

  /**
   * Publish a response to a given request back to the client
   * @param delivery The message containing the request to which we are responding
   * @param response The response to said request
   */
  def publishResponse(delivery: Delivery, response: Option[(String, Array[Byte])]): Unit

  /**
   * Publish an error back to the client, associated with a given request.
   * @param delivery The message containing the request to which we are responding
   * @param primaryError The error that arose in attempting to answer said request
   */
  def publishError(delivery: Delivery, primaryError: Throwable): Unit
}
