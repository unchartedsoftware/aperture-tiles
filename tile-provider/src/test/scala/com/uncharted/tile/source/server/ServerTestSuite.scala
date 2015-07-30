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

import org.scalatest.FunSuite

import scala.collection.mutable.Stack
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import com.uncharted.tile.source.util.ByteArrayCommunicator



class ServerTestSuite extends FunSuite {
  import ExecutionContext.Implicits.global

  class TestServer extends Server("localhost", "test-msg", "test-logs") {
    override def processRequest(delivery: Delivery): Option[(String, Array[Byte])] = {
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
    channel.queueDeclare(queue, false, false, false, null)
    channel.basicQos(1)
    channel.basicConsume(queue, true, consumer)
    val delivery = consumer.nextDelivery()
    responses.push(delivery.getBody)
    responseTypes.push(delivery.getProperties.getContentType)
  }

  class TestClient extends RabbitMQConnectable("localhost") {
    def sendIntMessage: (Int, Int, Int) = {
      var replyQueue = UUID.randomUUID.toString
      val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
      _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "numbers".getBytes())
      val rawResult = concurrent.Await.result(response, Duration(2000, TimeUnit.MILLISECONDS)).responses.pop
      ByteArrayCommunicator.defaultCommunicator.read[Int, Int, Int](rawResult)
    }
    def sendStringMessage: (String, String, String) = {
      var replyQueue = UUID.randomUUID.toString
      val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
      _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "strings".getBytes())
      val rawResult = concurrent.Await.result(response, Duration(2000, TimeUnit.MILLISECONDS)).responses.pop
      ByteArrayCommunicator.defaultCommunicator.read[String, String, String](rawResult)
    }
    def sendErrorMessage: (String, Throwable) = {
      var replyQueue = UUID.randomUUID.toString
      val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
      _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "error".getBytes())
      val rawResult = concurrent.Await.result(response, Duration(2000, TimeUnit.MILLISECONDS))
      (rawResult.responseTypes.pop, ByteArrayCommunicator.defaultCommunicator.read[Throwable](rawResult.responses.pop()))
    }
  }

  test("Test int response") {
    val server = new TestServer
    try {
      val runServer = concurrent.future(server.listenForRequests)
      val (first, second, third) = (new TestClient).sendIntMessage

      assert(1 === first)
      assert(2 === second)
      assert(3 === third)
    } finally {
      server.shutdown
    }
  }

  test("Test string response") {
    val server = new TestServer
    try {
      val runServer = concurrent.future(server.listenForRequests)
      val (first, second, third) = (new TestClient).sendStringMessage

      assert("abc" === first)
      assert("def" === second)
      assert("ghi" === third)
    } finally {
      server.shutdown
    }
  }

  test("Test error response") {
    val server = new TestServer
    try {
      val runServer = concurrent.future(server.listenForRequests)
      val (severity, error) = (new TestClient).sendErrorMessage

      assert(com.uncharted.tile.source.LOG_WARNING === severity)
      assert("Test exception" === error.getMessage)
    } finally {
      server.shutdown
    }
  }
}
