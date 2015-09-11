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


import java.io.ByteArrayOutputStream
import java.util.{Arrays => JavaArrays, ArrayList}

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.io.serialization.TileSerializer
import software.uncharted.tile.source.util.ByteArrayCommunicator

import scala.collection.JavaConverters._
import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag
import scala.util.Try

import com.rabbitmq.client.QueueingConsumer.Delivery
import grizzled.slf4j.Logging

import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.factory.providers.FactoryProvider

import software.uncharted.tile.source._
import software.uncharted.tile


/**
 * This class sets up a tile server, linking it to the appropriate channels, so it can listen to and fulfil tile
 * requests.
 *
 * This is a trivial version, meant mostly just for testing, that only produces a single tile at a time.
 *
 * @param host The host name of the machine on which resides the RabbitMQ server
 * @param pyramidIOFactoryProvider An object that constructs PyramidIO factories to use to fulfil tile requests.
 */
class MultiTileServer(host: String, user: String, password: String,
                      pyramidIOFactoryProvider: FactoryProvider[PyramidIO])
  extends Server(host, user, password,
    tile.source.TILE_REQUEST_EXCHANGE,
    tile.source.TILE_RESPONSE_EXCHANGE,
    tile.source.LOG_EXCHANGE,
    tile.source.DEFAULT_GRANULARITY)
  with Logging
  with QueueingServer {
  val pyramidIOs = MutableMap[String, PyramidIO]()

  /**
   * The time to wait after receiving a request before we start processing it, so as to make sure all requests in
   * that queue have been received.
   */
  override def getWaitTime(queueId: Any): Long = 100

  /**
   * Determine the queue in which a given delivery should be processed
   * @param delivery The delivery in question
   * @return The queue in which the delivery should be processed
   */
  override def getQueue(delivery: Delivery): Option[Any] = {
    ServerTileRequest.fromByteArray(delivery.getBody) match {
      case tir: TileInitializationRequest => None
      case tmr: TileMetaDataRequest => None
      case tdrRaw: TileDataRequest[_] => Some(tdrRaw.table)
      case tsrRaw: TileStreamRequest[_] => Some(tsrRaw.table)
    }
  }

  private def getRequestsOfType[T <: TileRequest : ClassTag](requests: Array[(TileRequest, Int)]): Array[(T, Int)] = {
    val tag = implicitly[ClassTag[T]]
    requests.filter(r => tag.runtimeClass.isInstance(r._1)).map(r => (r._1.asInstanceOf[T], r._2))
  }

  private def processInitializationRequests(requests: Array[(TileInitializationRequest, Int)]):
  Array[(Either[Option[(String, Array[Byte])], Throwable], Int)] = {
    requests.map { case (request, index) =>
      try {
        info("Initialization request for tile set " + request.table)
        debug("Height: " + request.height)
        debug("Width: " + request.width)
        debug("Configuration: " + request.configuration)

        // Construct the pyramidIO we need to fulfil the request
        val pioFactory = pyramidIOFactoryProvider.createFactory(null, null, JavaArrays.asList[String]())
        pioFactory.readConfiguration(request.configuration)
        val pyramidIO = pioFactory.produce(classOf[PyramidIO])
        pyramidIO.initializeForRead(request.table, request.width, request.height, JsonUtilities.jsonObjToProperties(request.configuration))
        pyramidIOs(request.table) = pyramidIO
        (Left(None), index)
      } catch {
        case t: Throwable => (Right(t), index)
      }
    }
  }

  private def processMetaDataRequests(requests: Array[(TileMetaDataRequest, Int)]):
  Array[(Either[Option[(String, Array[Byte])], Throwable], Int)] = {
    requests.map { case (request, index) =>
      try {
        info("Metadata request for tile set " + request.table)

        if (pyramidIOs.get(request.table).isEmpty) throw new IllegalArgumentException("Attempt to get metadata for uninitialized pyramid " + request.table)

        val pio = pyramidIOs(request.table)
        (Left(Some((RequestTypes.Metadata.toString, pio.readMetaData(request.table).getBytes))), index)
      } catch {
        case t: Throwable => (Right(t), index)
      }
    }
  }

  private def processTileRequests(requests: Array[(Either[TileDataRequest[_], TileStreamRequest[_]], Int)]):
  Array[(Either[Option[(String, Array[Byte])], Throwable], Int)] = {
    def choose[T](dataFcn: TileDataRequest[_] => T, streamFcn: TileStreamRequest[_] => T)
                 (source: Either[TileDataRequest[_], TileStreamRequest[_]]): T =
      source match {
        case Left(request) => dataFcn(request)
        case Right(request) => streamFcn(request)
      }

    if (0 == requests.size) {
      // Make sure further cases always have a request to deal with.
      Array()
    } else if (requests.size > 1 && requests.map(c => choose(_.table, _.table)(c._1)).sliding(2).map(tables => tables(0) != tables(1)).reduce(_ || _)) {
      // Multiple tables given in a single call.  This shouldn't happen - each table should be in its own queue - so
      // seeing this error should be very weird and very bad.
      val tables = requests.map(c => choose(_.table, _.table)(c._1)).toSet.toList.sorted
      error(tables.mkString("Multiple simultaneous tables: ", ", ", ""))
      requests.map(r => (Right(new MismatchedRequestsException(tables.mkString("Multiple simultaneous tables: ", ", ", ""))), r._2))
    } else if (requests.size > 1 && requests.map(c => choose(_.serializer, _.serializer)(c._1)).sliding(2).map(serializers => serializers(0) != serializers(1)).reduce(_ || _)) {
      // Multiple serializers given in a single call.  This shouldn't happen - each table should be in its own queue -
      // so seeing this error should be very weird and bad.
      val serializers = requests.map(c => choose(_.serializer.getClass.getName, _.serializer.getClass.getName)(c._1)).toSet.toList.sorted
      error(serializers.mkString("Multiple simultaneous serializers: ", ", ", ""))
      requests.map(r => (Right(new MismatchedRequestsException("Different requests have different serializers")), r._2))
    } else {
      val sampleRequest = requests(0)._1
      val table = choose(_.table, _.table)(sampleRequest)
      val rawSerializer = choose(_.serializer, _.serializer)(sampleRequest)
      val indices: Iterable[TileIndex] = requests.flatMap(c => choose(dr => dr.indices.asScala, sr => Iterable(sr.index))(c._1)).toSet
      info("Tile request for tile set " + table)
      debug("Serializer: " + rawSerializer.getClass)
      debug("Tiles: " + indices.toList)

      if (pyramidIOs.get(table).isEmpty) {
        requests.map(r => (Right(new IllegalArgumentException("Attempt to get tile data for uninitialized pyramid " + table)), r._2))
      } else {
        def doWork[T](serializer: TileSerializer[T]) = {
          val pio = pyramidIOs(table)
          pio.readTiles(table, serializer, indices.asJava).asScala.map { tile =>
            // Serialize each tile
            val baos = new ByteArrayOutputStream()
            serializer.serialize(tile, baos)
            baos.flush()
            baos.close()
            (tile.getDefinition, baos.toByteArray)
          }.toMap
        }
        val tileData = doWork(rawSerializer)

        requests.map { case (request, requestIndex) =>
          try {
            choose(
              request => {
                val tileIndices = request.indices.asScala.toArray
                val data = new ArrayList(tileIndices.map(index => tileData.get(index).getOrElse(null)).toList.asJava)

                (Left(Some((RequestTypes.Tiles.toString, ByteArrayCommunicator.defaultCommunicator.write(data)))), requestIndex)
              },
              request => {
                val tile = tileData.get(request.index).getOrElse(null)

                (Left(Some((RequestTypes.TileStream.toString, tile))), requestIndex)
              }
            )(request)
          } catch {
            case t: Throwable => (Right(t), requestIndex)
          }
        }
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
  override def processRequests(deliveries: Array[Delivery]): Array[Either[Option[(String, Array[Byte])], Throwable]] = {
    val indexedRequests = deliveries.map(delivery =>
      Try(ServerTileRequest.fromByteArray(delivery.getBody))
    ).zipWithIndex
    val serverErrors: Array[(Either[Option[(String, Array[Byte])], Throwable], Int)] =
      indexedRequests.filter(_._1.isFailure).map { case (failure, index) =>
        (Right(failure.failed.get), index)
      }
    val validRequests = indexedRequests.filter(_._1.isSuccess).map(r => (r._1.get, r._2))
    val initializationResponses = processInitializationRequests(getRequestsOfType[TileInitializationRequest](validRequests))
    val metaDataResponses = processMetaDataRequests(getRequestsOfType[TileMetaDataRequest](validRequests))
    val tileResponses = processTileRequests(
      getRequestsOfType[TileDataRequest[_]](validRequests).map { case (request, index) => (Left(request), index) } ++
        getRequestsOfType[TileStreamRequest[_]](validRequests).map { case (request, index) => (Right(request), index) }
    )

    val result = (serverErrors
      union initializationResponses
      union metaDataResponses
      union tileResponses
      ).sortBy(_._2).map(_._1)

    result
  }
}


class MismatchedRequestsException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this(null, cause)

  def this() = this(null, null)
}
