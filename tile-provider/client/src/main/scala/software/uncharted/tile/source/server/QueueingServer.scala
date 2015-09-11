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
import grizzled.slf4j.Logging

import scala.collection.mutable.{Buffer, Map => MutableMap, Queue => MutableQueue}


/**
 * A mix-in to cause a server to process queue up messages into multiple queues, each queue to be processed as a batch
 */
trait QueueingServer extends ServerPublications with Logging {
  /**
   * Specifies the minimum time to wait for more reqeusts in a given queue, after a first request is made in that
   * gueue, before starting processing.  This is to allow more requests to come in.
   * @param queueId The id of the queue whose wait time is needed
   * @return The minimum time to wait for further requests before beginning processing
   */
  def getWaitTime(queueId: Any): Long

  var _queuesByAge = MutableQueue[(Any, Long)]()
  var _queues = MutableMap[Any, Buffer[Delivery]]()

  def onDelivery(delivery: Delivery): Unit = {
    if (null != delivery) {
      // Acknowledge delivery immediately.  If we do not, we won't get more messages until we answer this one.
      acknowledge(delivery)
      getQueue(delivery) match {
        case Some(queueId) => queue(delivery, queueId)
        case None => {
          onQueueRipe(Array(delivery))
        }
      }
    }

    checkQueues
  }

  /**
   * Add the current delivery to the appropriate queue for later processing
   * @param delivery The delivery to queue up
   */
  def queue(delivery: Delivery, queueId: Any): Unit = {
    if (!_queuesByAge.map(_._1).contains(queueId)) {
      _queuesByAge.enqueue((queueId, System.currentTimeMillis()))
    }
    if (!_queues.contains(queueId)) {
      _queues(queueId) = Buffer[Delivery]()
    }
    _queues(queueId) += delivery
  }

  /**
   * Determine the queue in which a given delivery should be processed
   * @param delivery The delivery in question
   * @return Optionally, a queue on which the delivery should be processed.  If no queue is returned (None), then the
   *         message is processed immediately.
   */
  def getQueue(delivery: Delivery): Option[Any]

  /**
   * This method takes the first queue that is ripe for processing - i.e., it's first request was at least getWaitTime
   * milliseconds ago - and processes it.
   */
  def checkQueues: Unit = {
    if (!_queuesByAge.isEmpty) {
      val dequeueTest: ((Any, Long)) => Boolean = q => (System.currentTimeMillis() - q._2) >= getWaitTime(q._1)
      _queuesByAge.dequeueFirst(dequeueTest).foreach { case (queueId, time) =>
        info("Executing queue "+queueId)
        onQueueRipe(_queues.remove(queueId).get.toArray)
      }
    }
  }

  /*
   * A small wrapper around processRequests that handles returning the processed values to the client properly.
   */
  private def onQueueRipe (requests: Array[Delivery]): Unit = {
    requests.zip(processRequests(requests)).foreach { case (request, response) =>
      try {
        if (response.isLeft) {
          publishResponse(request, response.left.get)
        } else {
          publishError(request, response.right.get)
        }
      } catch {
        case t: Throwable =>
          error("Error publishing response to request " + request + ": " + response)
      }
    }
  }

  /**
   * Process a bunch of requests
   * @param deliveries The messages containing the requests
   * @return For each request, and in the same order, either an optional response containing the content type and
   *         the content of the response to each message (with None indicating a Unit response), or an error thrown
   *         in the process of trying to fulfil the request.
   */
  def processRequests(deliveries: Array[Delivery]): Array[Either[Option[(String, Array[Byte])], Throwable]]
}
