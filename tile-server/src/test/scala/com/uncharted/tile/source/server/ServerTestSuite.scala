package com.uncharted.tile.source.server

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{QueueingConsumer, Channel}
import com.rabbitmq.client.QueueingConsumer.Delivery
import com.uncharted.tile.source.util.ByteArrayCommunicator
import org.scalatest.FunSuite

import scala.collection.mutable.Stack
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
 * Created by nkronenfeld on 7/14/2015.
 */
class ServerTestSuite extends FunSuite {
  import ExecutionContext.Implicits.global

  class TestServer extends Server("localhost", "test-msg", "test-logs") {
    override def processRequest(delivery: Delivery): Option[Array[Byte]] = {
      val msg = new String(delivery.getBody)
      if ("numbers" == msg)
        Some(ByteArrayCommunicator.defaultCommunicator.write(1, 2, 3))
      else if ("strings" == msg)
        Some(ByteArrayCommunicator.defaultCommunicator.write("abc", "def", "ghi"))
      else if ("error" == msg)
        throw new Exception("Test exception")
      else
        None
    }
  }

  class TestResponseListener (queue: String, channel: Channel) {
    val responses = new Stack[Array[Byte]]()
    val consumer = new QueueingConsumer(channel)
    channel.queueDeclare(queue, false, false, false, null)
    channel.basicQos(1)
    channel.basicConsume(queue, true, consumer)
    val delivery = consumer.nextDelivery()
    responses.push(delivery.getBody)
  }

  class TestClient extends RabbitMQConnectable("localhost") {
    def sendIntMessage: (Int, Int, Int) = {
      var replyQueue = UUID.randomUUID.toString
      val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
      _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "numbers".getBytes())
      val rawResult = concurrent.Await.result(response, Duration(1000, TimeUnit.MILLISECONDS)).responses.pop
      ByteArrayCommunicator.defaultCommunicator.read[Int, Int, Int](rawResult)
    }
    def sendStringMessage: (String, String, String) = {
      var replyQueue = UUID.randomUUID.toString
      val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
      _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "strings".getBytes())
      val rawResult = concurrent.Await.result(response, Duration(1000, TimeUnit.MILLISECONDS)).responses.pop
      ByteArrayCommunicator.defaultCommunicator.read[String, String, String](rawResult)
    }
    def sendErrorMessage: (String, Throwable) = {
      var replyQueue = UUID.randomUUID.toString
      val response = concurrent.future(new TestResponseListener(replyQueue, _channel))
      _channel.basicPublish("test-msg", "", new BasicProperties.Builder().replyTo(replyQueue).build(), "error".getBytes())
      val rawResult = concurrent.Await.result(response, Duration(1000, TimeUnit.MILLISECONDS)).responses.pop
      ByteArrayCommunicator.defaultCommunicator.read[String, Throwable](rawResult)
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

      assert(LOG_WARNING === severity)
      assert("Test exception" === error.getMessage)
    } finally {
      server.shutdown
    }
  }
}
