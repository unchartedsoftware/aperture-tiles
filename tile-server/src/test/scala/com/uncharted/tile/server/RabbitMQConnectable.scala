package com.uncharted.tile.server

import com.rabbitmq.client.ConnectionFactory


object RabbitMQConnectable {
  val HOST = "localhost"
}
class RabbitMQConnectable {
  protected val _factory = new ConnectionFactory()
  _factory.setHost(RabbitMQConnectable.HOST)
  protected val _connection = _factory.newConnection()
  protected val _channel = _connection.createChannel()
}
