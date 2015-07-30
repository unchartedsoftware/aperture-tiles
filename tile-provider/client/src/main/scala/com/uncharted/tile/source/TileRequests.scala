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
package com.uncharted.tile.source



import java.lang.{Iterable => JavaIterable}

import com.uncharted.tile.source.util.ByteArrayCommunicator
import org.json.JSONObject

import scala.collection.JavaConverters._

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.io.serialization.TileSerializer



object RequestTypes extends Enumeration {
  val Initialization, Metadata, Tiles, TileStream = Value
}
trait TileRequest {
  /** The type of request being made */
  val requestType: RequestTypes.Value

  /** Convert the tile request into a transmitable byte array */
  def toByteArray: Array[Byte]
}

/**
 * A request for read initialiation of a tile pyramid
 */
trait TileInitializationRequest extends TileRequest {
  final val requestType = RequestTypes.Initialization
  /** The name of the table from which to retrieve tiles */
  val table: String
  /** The width of each tile to be read, in pixels */
  val width: Int
  /** The width of each tile to be read, in pixels */
  val height: Int
  /**
   * The configuration from which the server should construct relevant PyramidIOs and TileSerializers with which to
   * request tiles
   */
  val configuration: JSONObject

  def toByteArray: Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(RequestTypes.Initialization, table, width, height, configuration.toString)
}

/**
 * A request to read a tile pyramid's metadata
 */
trait TileMetaDataRequest extends TileRequest {
  final val requestType = RequestTypes.Metadata
  /** The name of the table from which to retrieve tiles */
  val table: String

  def toByteArray: Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(RequestTypes.Metadata, table)
}

/**
 * A request to read tiles from a tile pyramid
 * @tparam T The bin type of the tile to be read
 */
trait TileDataRequest[T] extends TileRequest {
  final val requestType = RequestTypes.Tiles
  /** The name of the table from which to retrieve tiles */
  val table: String
  /** A serializer that knows how to read and write tiles of our request type */
  val serializer: TileSerializer[T]
  /** The tile indices to retrieve */
  val indices: JavaIterable[TileIndex]

  def toByteArray: Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(RequestTypes.Tiles, table, serializer,
      TileDataRequest.indicesToIndexInfoArray(indices))
}
object TileDataRequest {
  def indicesToIndexInfoArray (indices: JavaIterable[TileIndex]): Array[Array[Int]] =
    indices.asScala.toArray.map(index =>
      Array[Int](index.getLevel, index.getX, index.getY, index.getXBins, index.getYBins)
    )
  def indexInfoArrayToIndices (infoArray: Array[Array[Int]]): JavaIterable[TileIndex] =
    infoArray.map(info => new TileIndex(info(0), info(1), info(2), info(3), info(4))).toList.asJava
}

/**
 * A request to get a serialized stream of a tile from a tile pyramid
 * @tparam T The bin type of the tile to be read
 */
trait TileStreamRequest[T] extends TileRequest {
  final val requestType = RequestTypes.TileStream
  /** The name of the table from which to retrieve tiles */
  val table: String
  /** A serializer that knows how to read and write tiles of our request type */
  val serializer: TileSerializer[T]
  /** The tile index to retrieve */
  val index: TileIndex

  def toByteArray: Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(RequestTypes.TileStream, table, serializer, index)
}
