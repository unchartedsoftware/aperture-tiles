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

import java.util.concurrent.TimeUnit

import com.rabbitmq.client.QueueingConsumer.Delivery
import org.jamon.escaping.NoneEscaping
import org.scalatest.exceptions.TestCanceledException
import org.scalatest.{Canceled, Outcome, FunSuite}
import software.uncharted.tile.source.client.Client
import software.uncharted.tile.source.util.ByteArrayCommunicator

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration


object QueueingServerTestSuite {
  val HOST = "localhost"
  val USER = "test"
  val PWD = "test"
  val REQUESTS = "requests"
  val RESPONSES = "responses"
  val LOGS = "logs"
  // Maximum allowed time for the server to respond to requests, in milliseconds
  val maxResponseTime = 5000
  // Maximum amount of slop in timing calculations, in milliseconds
  val SLOP = 10
}
import QueueingServerTestSuite._

class QueueingServerTestSuite extends FunSuite {
  import ExecutionContext.Implicits.global



  var server: QueueingTestServer = null
  var client: QueueingTestClient = null

  override def withFixture(test: NoArgTest): Outcome = {
    // We do a couple things in here:
    // First, we consolidate server and client construction so it doesn't have to be done individually in each test.
    // Second, we wrap test calls so that they don't get called at all if the server can't be reached.
    try {
      server = new QueueingTestServer
      val runServer = server.startRequestThread
      client = new QueueingTestClient
      val runClient = client.startResponseThread
      val outcome =
        try {
          super.withFixture(test)
        } finally {
          server.shutdown
          client.shutdown
        }
      concurrent.Await.result(runServer, Duration(maxResponseTime, TimeUnit.MILLISECONDS))
      concurrent.Await.result(runClient, Duration(maxResponseTime, TimeUnit.MILLISECONDS))
      outcome
    } catch {
      case t: Throwable => new Canceled(new TestCanceledException(Some("Error constructing server"), Some(t), 1))
    }
  }

  def awaitRequests (requests: QueueingTestRequest*): Unit = {
    val response = concurrent.future {
      while (requests.map(r => !r.answered).reduce(_ || _)) Thread.sleep(100)
    }
    concurrent.Await.result(response, Duration(maxResponseTime, TimeUnit.MILLISECONDS))
  }

  test("Test request creation timing") {
    // Make sure sleeping adds the requisite time between requests
    val r1 = new QueueingTestRequest("abc", "r1")
    Thread.sleep(10)
    val r2 = new QueueingTestRequest("abc", "r2")
    Thread.sleep(20)
    val r3 = new QueueingTestRequest("abc", "r3")
    Thread.sleep(30)
    val r4 = new QueueingTestRequest("abc", "r4")
    Thread.sleep(40)
    val r5 = new QueueingTestRequest("abc", "r5")

    val dr1 = r2.requestTime - r1.requestTime
    val dr2 = r3.requestTime - r2.requestTime
    val dr3 = r4.requestTime - r3.requestTime
    val dr4 = r5.requestTime - r4.requestTime

    // Allow 5 ms slop
    assert(dr1 >= 10 && dr1 < 10 + SLOP)
    assert(dr2 >= 20 && dr2 < 20 + SLOP)
    assert(dr3 >= 30 && dr2 < 30 + SLOP)
    assert(dr4 >= 40 && dr2 < 40 + SLOP)
  }

  test("Test single request collection") {
    val request = new QueueingTestRequest("abc", "r")
    client.makeRequest(request)
    awaitRequests(request)

    // Make sure it was answered
    assert(request.answered)
    // Make sure enough time was taken between receipt and response for other requests to come in.
    assert((request.responseTime.get - request.requestTime) >= 500)
  }
  test("Test multiple simultaenous request collection") {
    val requests = (1 to 10).map(n => new QueueingTestRequest("abc", "r"+n))
    requests.foreach(r => client.makeRequest(r))
    awaitRequests(requests:_*)


    requests.foreach(r => assert(r.answered))
    val (minTime, maxTime) = requests.map(_.responseTime.get)
      .map(r => (r, r))
      .reduce((a, b) => (a._1 min b._1, a._2 max b._2))
    assert(maxTime - minTime < SLOP, (maxTime - minTime) + " was not less than "+SLOP)
  }

  test("Test request collection catch-up") {
    val requests = (1 to 10).map(n => new QueueingTestRequest("abc", "r"+n))
    requests.foreach(r => client.makeRequest(r))
    awaitRequests(requests:_*)


    requests.foreach(r => assert(r.answered))
    val (minTime, maxTime) = requests.map(_.responseTime.get)
      .map(r => (r, r))
      .reduce((a, b) => (a._1 min b._1, a._2 max b._2))
    assert(maxTime - minTime < SLOP)
  }

  test("Test request collection spacing") {
    val requests = (1 to 9).map(n => (n, new QueueingTestRequest("abc", "r"+n)))
    // Pause between requests so they are spaced out as follows:
    //   Block 1:
    //      request 1 (0), request 2 (105), request 3 (210), request 4 (315), request 5 (420)
    //   Block 2:
    //      request 6 (525), request 7 (630), request 8 (735), request 9 (840)
    requests.foreach { case (index, request) =>
      client.makeRequest(request)
      Thread.sleep(105)
    }
    awaitRequests((requests.filter(_._1 < 6).map(_._2)): _*)
    awaitRequests((requests.filter(_._1 > 5).map(_._2)): _*)

    requests.foreach(r => assert(r._2.answered))
    val (min1, max1) = requests.filter(_._1 < 6).map(_._2.responseTime.get)
      .map(r => (r, r))
      .reduce((a, b) => (a._1 min b._1, a._2 max b._2))
    val (min2, max2) = requests.filter(_._1 > 5).map(_._2.responseTime.get)
      .map(r => (r, r))
      .reduce((a, b) => (a._1 min b._1, a._2 max b._2))

    assert(max1 - min1 < SLOP)
    assert(max2 - min2 < SLOP)
    assert(min2 - min1 > 500)
  }

  test("Test immediate vs. queued reqeusts") {
    val requests = (0 to 8).map(n => new QueueingTestRequest("abc", ""+n, 1 == (n%2))).toArray
    // Pause between requests so they are spaced out as follows:
    //   request 1 (105, immediate)
    //   request 3 (315, immediate)
    //   Block 1:
    //      request 0 (0), request 2 (210), request 4 (420)
    //   request 5 (525, immediate)
    //   request 7 (735, immediate)
    //   Block 2:
    //      request 6 (630), request 8 (840)
    requests.foreach{request =>
      client.makeRequest(request)
      Thread.sleep(105)
    }
    awaitRequests(requests(1))
    awaitRequests(requests(3))
    awaitRequests(requests(0), requests(2), requests(4))
    awaitRequests(requests(5))
    awaitRequests(requests(7))
    awaitRequests(requests(6), requests(8))
    assert(math.abs(requests(0).responseTime.get - requests(2).responseTime.get) < SLOP, "Requests 0 and 2 weren't made together")
    assert(math.abs(requests(0).responseTime.get - requests(4).responseTime.get) < SLOP, "Requests 0 and 4 weren't made together")
    assert(math.abs(requests(6).responseTime.get - requests(8).responseTime.get) < SLOP, "Requests 6 and 8 weren't made together")
    assert(math.abs(requests(1).responseTime.get - requests(0).responseTime.get) > SLOP, "Requests 0 and 1 were made at the same time")
    assert(math.abs(requests(3).responseTime.get - requests(0).responseTime.get) > SLOP, "Requests 0 and 3 were made at the same time")
    assert(math.abs(requests(5).responseTime.get - requests(6).responseTime.get) > SLOP, "Requests 6 and 5 were made at the same time")
    assert(math.abs(requests(7).responseTime.get - requests(6).responseTime.get) > SLOP, "Requests 6 and 7 were made at the same time")

  }
}

class QueueingTestRequest (val queue: String, val name: String, val immediate: Boolean = false) extends Serializable {
  val requestTime = System.currentTimeMillis()
  var responseTime: Option[Long] = None
  var answered: Boolean = false
  override def toString: String = name
}

class QueueingTestClient
  extends Client[QueueingTestRequest](HOST, USER, PWD, REQUESTS, RESPONSES) {
  /**
   * Encode a request so that it can be sent through RabbitMQ.
   */
  override def encodeRequest(request: QueueingTestRequest): Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(request)

  /**
   * Process the data returned from the server when a request is filled.
   * @param request The request that was made
   * @param contentType The content type of the response to said request
   * @param contents The data containing the response to said request
   */
  override def processResults(request: QueueingTestRequest, contentType: String, contents: Array[Byte]): Unit = {
    val response = ByteArrayCommunicator.defaultCommunicator.read[QueueingTestRequest](contents)
    request.responseTime = response.responseTime
    request.answered = true
  }
}

class QueueingTestServer
  extends Server(HOST, USER, PWD, REQUESTS, RESPONSES, LOGS, 10)
  with QueueingServer {
  override def getWaitTime(queueId: Any): Long = 500

  /**
   * Process a bunch of requests
   * @param deliveries The messages containing the requests
   * @return For each request, and in the same order, either an optional response containing the content type and
   *         the content of the response to each message (with None indicating a Unit response), or an error thrown
   *         in the process of trying to fulfil the request.
   */
  override def processRequests(deliveries: Array[Delivery]): Array[Either[Option[(String, Array[Byte])], Throwable]] = {
    deliveries.map { d =>
      try {
        val request = ByteArrayCommunicator.defaultCommunicator.read[QueueingTestRequest](d.getBody)
        request.responseTime = Some(System.currentTimeMillis())
        request.answered = true
        Left(Some(("", ByteArrayCommunicator.defaultCommunicator.write(request))))
      } catch {
        case t: Throwable => Right(t)
      }
    }
  }


  override def getQueue(delivery: Delivery): Option[Any] = {
    val request = ByteArrayCommunicator.defaultCommunicator.read[QueueingTestRequest](delivery.getBody)

    if (request.immediate) None
    else Some(request.queue)
  }
}
