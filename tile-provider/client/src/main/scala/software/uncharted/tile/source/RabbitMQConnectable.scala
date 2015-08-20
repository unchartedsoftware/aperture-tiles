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
package software.uncharted.tile.source

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.ConnectionFactory



/**
 * A simple class to encapsulate the connection to RabbitMQ
 *
 * @param host The hostname of the RabbitMQ
 */
class RabbitMQConnectable (host: String, user: String, pswd: String) {
  // To create a user:
  // rabbitmqctl add_user <user-name> <password>
  // rabbitmqctl set_permissions -p / <user-name> ".*" ".*" ".*"
  protected val _factory = new ConnectionFactory()
  _factory.setHost(host)
  _factory.setUsername(user)
  _factory.setPassword(pswd)

  protected val _connection = _factory.newConnection()

  protected val _channel = _connection.createChannel()

  /** Sends a quick message on a specified auto-delete channel */
  def oneOffDirectMessage (queue: String, contentType: String, message: Array[Byte]): Unit = {
    _channel.queueDeclare(queue, false, false, true, null)
    _channel.basicPublish("", queue, new BasicProperties.Builder().contentType(contentType).build, message)
  }
}
