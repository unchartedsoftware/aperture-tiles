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

import grizzled.slf4j.Logging
import org.scalatest.exceptions.TestCanceledException
import org.scalatest.{Outcome, FunSuite, Canceled}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import com.rabbitmq.client.QueueingConsumer.Delivery

import com.uncharted.tile.source
import com.uncharted.tile.source.server.Server
import com.uncharted.tile.source.util.ByteArrayCommunicator

import scala.util.Try


object ClientTestSuite {
  val TEST_BROKER="hadoop-s1"
  val TEST_USER="test"
  val TEST_PSWD="test"
  val maxResponseTime = 5000
}
class ClientTestSuite extends FunSuite with Logging {
  import ClientTestSuite._
  import ExecutionContext.Implicits.global

  var server: TestServer = null
  var client: TestClient = null

  override def withFixture(test: NoArgTest): Outcome = {
    // We do a couple things in here:
    // First, we consolidate server and client construction so it doesn't have to be done individually in each test.
    // Second, we wrap test calls so that they don't get called at all if the server can't be reached.♦
    try {
      server = new TestServer
      val runServer = server.startRequestThread
      var outcome =
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

  def makeRequest(request: TestClientMessage): TestClientMessage = {
    client.makeRequest(request)
    val response = concurrent.future {
      while (!request.answered) Thread.sleep(100)
    }
    concurrent.Await.result(response, Duration(maxResponseTime, TimeUnit.MILLISECONDS))
    request
  }

  test("Test normal client functioning") {
    val stringResult = makeRequest(new TestClientMessage("strings"))
    assert("string" === stringResult.contentType)
    assert(("abc", "def", "ghi") ===
      ByteArrayCommunicator.defaultCommunicator.read[String, String, String](stringResult.contents))

    val intResult = makeRequest(new TestClientMessage("numbers"))
    assert("int" === intResult.contentType)
    assert((1, 2, 3) === ByteArrayCommunicator.defaultCommunicator.read[Int, Int, Int](intResult.contents))

    val errorResult = makeRequest(new TestClientMessage("error"))
    assert(com.uncharted.tile.source.LOG_WARNING === errorResult.severity)
    assert("Test exception" === errorResult.error.getMessage)
  }

  test("Test no-response messages") {
    (1 to 100).foreach { n =>
      assert(source.UNIT_TYPE === makeRequest(new TestClientMessage("no-response-" + n)).contentType)
    }
    val answered = makeRequest(new TestClientMessage("numbers"))
    assert("int" === answered.contentType)
    assert((1, 2, 3) === ByteArrayCommunicator.defaultCommunicator.read[Int, Int, Int](answered.contents))
    assert(101 === server.requests)
  }
}


class TestClientMessage(val requestType: String) {
  var answered: Boolean = false
  var contentType: String = null
  var contents: Array[Byte] = null
  var severity: String = null
  var error: Throwable = null
}

class TestServer extends Server(ClientTestSuite.TEST_BROKER, ClientTestSuite.TEST_USER, ClientTestSuite.TEST_PSWD,
  "test-msg", "test-logs") {
  var requests = 0
  override def processRequest(delivery: Delivery): Option[(String, Array[Byte])] = {
    requests = requests + 1
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

class TestClient extends Client[TestClientMessage](
  ClientTestSuite.TEST_BROKER, ClientTestSuite.TEST_USER, ClientTestSuite.TEST_PSWD,
  "test-msg")
{
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
