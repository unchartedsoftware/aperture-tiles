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



import java.util.UUID
import java.util.concurrent.TimeUnit

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{QueueingConsumer, Channel}
import com.rabbitmq.client.QueueingConsumer.Delivery
import com.uncharted.tile.source.RabbitMQConnectable
import org.scalatest.exceptions.TestCanceledException

import org.scalatest.{Canceled, Outcome, FunSuite}

import scala.collection.mutable.{HashMap, SynchronizedMap}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import com.uncharted.tile.source.util.ByteArrayCommunicator



object ServerTestSuite {
  val TEST_BROKER="hadoop-s1"
  val TEST_USER="test"
  val TEST_PSWD="test"
  val maxResponseTime = 5000
}
class ServerTestSuite extends FunSuite {
  import ServerTestSuite._

  var server: TestServer = null
  var client: TestClient = null

  override def withFixture(test: NoArgTest): Outcome = {
    // We do a couple things in here:
    // First, we consolidate server and client construction so it doesn't have to be done individually in each test.
    // Second, we wrap test calls so that they don't get called at all if the server can't be reached.
    try {
      server = new TestServer
      val runServer = server.startRequestThread
      client = new TestClient
      val runClient = client.startResponseThread
      val outcome =
        try {
          super.withFixture(test)
        } finally {
          server.shutdown
          client.shutdown
        }
      concurrent.Await.result(runServer, Duration(maxResponseTime, TimeUnit.MILLISECONDS))
      concurrent.Await.result(runClient, Duration(maxResponseTime, TimeUnit.MILLISECONDS))
      outcome
    } catch {
      case t: Throwable => new Canceled(new TestCanceledException(Some("Error constructing server"), Some(t), 1))
    }
  }

  test("Test int response") {
      val (first, second, third) = client.sendIntMessage

      assert(1 === first)
      assert(2 === second)
      assert(3 === third)
  }

  test("Test string response") {
    val (first, second, third) = client.sendStringMessage

    assert("abc" === first)
    assert("def" === second)
    assert("ghi" === third)
  }

  test("Test error response") {
    val (severity, error) = client.sendErrorMessage

    assert(com.uncharted.tile.source.LOG_WARNING === severity)
    assert("Test exception" === error.getMessage)
  }
}

import ServerTestSuite._
class TestServer extends Server(TEST_BROKER, TEST_USER, TEST_PSWD, "test-msg", "test-rsp", "test-logs") {
  override def processRequest(delivery: Delivery): Option[(String, Array[Byte])] = {
    println("Processing request (Server test suite)")
    val msg = new String(delivery.getBody)
    if ("numbers" == msg)
      Some(("int", ByteArrayCommunicator.defaultCommunicator.write(1, 2, 3)))
    else if ("strings" == msg)
      Some(("string", ByteArrayCommunicator.defaultCommunicator.write("abc", "def", "ghi")))
    else if ("error" == msg)
      throw new Exception("Test exception")
    else
      None
  }
}

class TestClient extends RabbitMQConnectable(TEST_BROKER, TEST_USER, TEST_PSWD) {

  import ExecutionContext.Implicits.global

  var _started = false
  var _shutdown = false
  val _requesterId = "test-requester"
  val _response = new HashMap[String, (String, Array[Byte])] with SynchronizedMap[String, (String, Array[Byte])]

  def startResponseThread = {
    val responseFuture = concurrent.future(listenForResponses)
    while (!_started) {
      Thread.sleep(10)
    }
    responseFuture
  }

  def listenForResponses: Unit = {
    // Create a response channel, to which we publish responses to requests.
    _channel.exchangeDeclare("test-rsp", "direct", false, true, false, null)

    // Set up a private queue on which to listen for requests
    val queueName = _channel.queueDeclare().getQueue
    _channel.queueBind(queueName, "test-rsp", _requesterId)
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
        _response(messageId) = (delivery.getProperties.getContentType, delivery.getBody)
      }
    }
  }
  def shutdown: Unit = {
    _shutdown = true
  }

  private def sendMessage (messageId: String, message: Array[Byte]): (String, Array[Byte]) = {
    _response.remove(messageId)
    _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(_requesterId).messageId(messageId).build(), message)

    val startWait = System.currentTimeMillis()
    var curTime = startWait
    do {
      Thread.sleep(100)
    } while ((curTime-startWait) < ServerTestSuite.maxResponseTime && _response.get(messageId).isEmpty)
    _response(messageId)
  }

  def sendIntMessage: (Int, Int, Int) = {
    val response = sendMessage(UUID.randomUUID.toString, "numbers".getBytes())
    ByteArrayCommunicator.defaultCommunicator.read[Int, Int, Int](response._2)
  }

  def sendStringMessage: (String, String, String) = {
    val response = sendMessage(UUID.randomUUID.toString, "strings".getBytes())
    ByteArrayCommunicator.defaultCommunicator.read[String, String, String](response._2)
  }
  def sendErrorMessage: (String, Throwable) = {
    val response = sendMessage(UUID.randomUUID.toString, "error".getBytes())
    (response._1, ByteArrayCommunicator.defaultCommunicator.read[Throwable](response._2))
  }
}
