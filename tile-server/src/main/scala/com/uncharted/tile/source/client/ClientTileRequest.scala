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



import java.io.ByteArrayInputStream
import java.lang.{Iterable => JavaIterable}
import java.util
import java.util.{List => JavaList}

import org.json.JSONObject

import scala.collection.JavaConverters._

import grizzled.slf4j.Logging

import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.{TileData, TileIndex}
import com.uncharted.tile.source.util.ByteArrayCommunicator
import com.uncharted.tile.source.{RequestTypes, TileRequest, TileInitializationRequest, TileMetaDataRequest, TileDataRequest, TileStreamRequest}



trait ClientTileRequest extends TileRequest {
  def isAnswered: Boolean
  def onError (t: Throwable): Unit
  def onFinished (data: Array[Byte]): Unit
}

case class ClientTileInitializationRequest (table: String, width: Int, height: Int, configuration: JSONObject)
  extends TileInitializationRequest with ClientTileRequest with Logging
{
  def isAnswered: Boolean = true
  def onError (t: Throwable): Unit = {
    warn("Error initializing layer "+table, t)
  }
  def onFinished (data: Array[Byte]): Unit = {}
}

case class ClientTileMetaDataRequest (table: String) extends TileMetaDataRequest with ClientTileRequest {
  var _error: Option[Throwable] = None
  var _metaData: Option[String] = None

  override def isAnswered: Boolean = _error.isDefined || _metaData.isDefined

  override def onFinished(data: Array[Byte]): Unit =
    _metaData = Some(new String(data))

  override def onError(t: Throwable): Unit =
    _error = Some(t)
}

case class ClientTileDataRequest[T] (table: String, serializer: TileSerializer[T], indices: JavaIterable[TileIndex])
  extends TileDataRequest[T] with ClientTileRequest {
  var _error: Option[Throwable] = None
  var _tiles: Option[JavaList[TileData[T]]] = None

  override def isAnswered: Boolean = _error.isDefined || _tiles.isDefined

  override def onFinished(data: Array[Byte]): Unit = {
    // Extract our results
    val encodedTiles = ByteArrayCommunicator.defaultCommunicator.read[JavaList[Array[Byte]]](data)
    val tiles = new util.ArrayList[TileData[T]]()
    var n = 0
    indices.asScala.foreach { index =>
      val bais = new ByteArrayInputStream(encodedTiles.get(n))
      tiles.add(serializer.deserialize(index, bais))
      n = n + 1
    }
    _tiles = Some(tiles)
  }

  override def onError(t: Throwable): Unit = _error = Some(t)
}

case class ClientTileStreamRequest[T] (table: String, serializer: TileSerializer[T], index: TileIndex)
  extends TileStreamRequest[T] with ClientTileRequest {
  var _error: Option[Throwable] = None
  var _data: Option[Array[Byte]] = None

  override def isAnswered: Boolean = _error.isDefined || _data.isDefined

  override def onFinished(data: Array[Byte]): Unit = _data = Some(data)

  override def onError(t: Throwable): Unit = _error = Some(t)
}
