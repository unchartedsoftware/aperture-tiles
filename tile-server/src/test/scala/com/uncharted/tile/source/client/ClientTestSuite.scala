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

package com.uncharted.tile.source.client

import java.util.concurrent.TimeUnit

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.QueueingConsumer.Delivery
import com.uncharted.tile.source.server.Server
import com.uncharted.tile.source.util.ByteArrayCommunicator
import org.scalatest.FunSuite

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration


class ClientTestSuite extends FunSuite {
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

  class TestClientMessage(val requestType: String) {
    var answered: Boolean = false
    var contentType: String = null
    var contents: Array[Byte] = null
    var severity: String = null
    var error: Throwable = null
  }
  class TestClient extends Client[TestClientMessage]("localhost", "test-msg") {
    override def encodeRequest(request: TestClientMessage): Array[Byte] = request.requestType.getBytes

    override def processResults(request: TestClientMessage, contentType: String, contents: Array[Byte]): Unit = {
      if (request.answered) throw new Exception("Two answers to one request")

      request.contentType = contentType
      request.contents = contents
      request.answered = true
    }

    override def processError(request: TestClientMessage, severity: String, serverError: Throwable): Unit = {
      if (request.answered) throw new Exception("Two answers to one request")

      request.severity = severity
      request.error = serverError
      request.answered = true
    }
  }



  test("Test normal client functioning") {
    val server = new TestServer
    val client = new TestClient
    try {
      val runServer = concurrent.future(server.listenForRequests)

      def makeRequest (request: TestClientMessage): TestClientMessage = {
        client.makeRequest(request)
        val response = concurrent.future {
          while (!request.answered) Thread.sleep(100)
        }
        concurrent.Await.result(response, Duration(2000, TimeUnit.MILLISECONDS))
        request
      }



      val stringResult = makeRequest(new TestClientMessage("strings"))
      assert("string" === stringResult.contentType)
      assert(("abc", "def", "ghi") ===
        ByteArrayCommunicator.defaultCommunicator.read[String, String, String](stringResult.contents))

      val intResult = makeRequest(new TestClientMessage("numbers"))
      assert("int" === intResult.contentType)
      assert((1, 2, 3) === ByteArrayCommunicator.defaultCommunicator.read[Int, Int, Int](intResult.contents))

      val errorResult = makeRequest(new TestClientMessage("error"))
      assert(com.uncharted.tile.source.server.LOG_WARNING === errorResult.severity)
      assert("Test exception" === errorResult.error.getMessage)
    } finally {
      server.shutdown
    }
  }
}
