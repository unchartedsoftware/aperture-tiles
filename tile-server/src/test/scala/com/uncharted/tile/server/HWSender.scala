package com.uncharted.tile.server

/**
 * Created by nkronenfeld on 7/8/2015.
 */
object HWSender extends RabbitMQConnectable {
  import HelloWorld._

  def main (args: Array[String]): Unit = {
    try {
      _channel.queueDeclare(QUEUE_NAME, false, false, false, null)
      val message = "Hello, World!"
      _channel.basicPublish("", QUEUE_NAME, null, message.getBytes())
      println("[x] Sent: '" + message + "'")
    } finally {
      _channel.close()
      _connection.close()
    }
  }
}
