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

import com.rabbitmq.client.QueueingConsumer.Delivery

/**
 * A mix-in to cause a server to process each message one at a time, as it comes in.
 */
trait SimpleServer extends ServerPublications {
  /**
   * Implements the basic onDelivery method of Server to process each message as it comes in
   * @param delivery The message that has been delivered (existing or no-existent)
   */
  def onDelivery (delivery: Delivery): Unit = {
    if (null != delivery) {
      val requesterId = delivery.getProperties.getReplyTo
      val messageId = delivery.getProperties.getMessageId
      try {
        publishResponse(delivery, processRequest(delivery))
      } catch {
        case responseError: Throwable => {
          publishError(delivery, responseError)
        }
      } finally {
        acknowledge(delivery)
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
}
