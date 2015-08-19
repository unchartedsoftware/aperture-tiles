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

import scala.collection.mutable.Stack
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
      val outcome =
        try {
          client = new TestClient
          super.withFixture(test)
        } finally {
          server.shutdown
        }
      concurrent.Await.result(runServer, Duration(maxResponseTime, TimeUnit.MILLISECONDS))
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
class TestServer extends Server(TEST_BROKER, TEST_USER, TEST_PSWD, "test-msg", "test-logs") {
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

class TestResponseListener (queue: String, channel: Channel) {
  val responseTypes = new Stack[String]
  val responses = new Stack[Array[Byte]]()
  val consumer = new QueueingConsumer(channel)
  channel.queueDeclare(queue, false, false, true, null)
  channel.basicQos(1)
  channel.basicConsume(queue, true, consumer)
  val delivery = consumer.nextDelivery()
  responses.push(delivery.getBody)
  responseTypes.push(delivery.getProperties.getContentType)
}

class TestClient extends RabbitMQConnectable(TEST_BROKER, TEST_USER, TEST_PSWD) {
  import ExecutionContext.Implicits.global

  def sendIntMessage: (Int, Int, Int) = {
    var replyQueue = UUID.randomUUID.toString
    val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
    _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "numbers".getBytes())
    val rawResult = concurrent.Await.result(response, Duration(maxResponseTime, TimeUnit.MILLISECONDS)).responses.pop
    ByteArrayCommunicator.defaultCommunicator.read[Int, Int, Int](rawResult)
  }
  def sendStringMessage: (String, String, String) = {
    var replyQueue = UUID.randomUUID.toString
    val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
    _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "strings".getBytes())
    val rawResult = concurrent.Await.result(response, Duration(maxResponseTime, TimeUnit.MILLISECONDS)).responses.pop
    ByteArrayCommunicator.defaultCommunicator.read[String, String, String](rawResult)
  }
  def sendErrorMessage: (String, Throwable) = {
    var replyQueue = UUID.randomUUID.toString
    val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
    _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "error".getBytes())
    val rawResult = concurrent.Await.result(response, Duration(maxResponseTime, TimeUnit.MILLISECONDS))
    (rawResult.responseTypes.pop, ByteArrayCommunicator.defaultCommunicator.read[Throwable](rawResult.responses.pop()))
  }
}
