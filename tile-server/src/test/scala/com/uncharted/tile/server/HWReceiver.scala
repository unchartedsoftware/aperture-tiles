package com.uncharted.tile.server

import com.rabbitmq.client.QueueingConsumer


object HelloWorld {
  val QUEUE_NAME = "hello-world"
}

object HWReceiver extends RabbitMQConnectable {
  import HelloWorld._

  def main (args: Array[String]): Unit = {
    _channel.queueDeclare(QUEUE_NAME, false, false, false, null)
    println("[x] Waiting for messages.")

    val consumer = new QueueingConsumer(_channel)
    _channel.basicConsume(QUEUE_NAME, true, consumer)

    while (true) {
      val delivery = consumer.nextDelivery()
      val message = new String(delivery.getBody)
      println("[x] Recieved: '" + message + "'")
    }
  }
}
