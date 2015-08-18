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
package com.uncharted.tile.source.client.io



import java.io.{ByteArrayInputStream, IOException, InputStream}
import java.lang.{Iterable => JavaIterable}
import java.util.concurrent.TimeUnit
import java.util.{List => JavaList}
import java.util.Properties

import org.json.JSONObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import com.oculusinfo.binning.{TileData, TileIndex}
import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.factory.providers.FactoryProvider
import com.uncharted.tile.source.client._





/**
 * A tile pyramid IO that acts as a facade for a TileClient
 */
class TileServerPyramidIO (brokerHostName: String, brokerUserName: String, brokerPassword: String,
                           maximumWaitTime: Long = 2000) extends PyramidIO {
  private val client = new TileClient(brokerHostName, brokerUserName, brokerHostName)
  private val clientTimeout = Duration(maximumWaitTime, TimeUnit.MILLISECONDS)

  override def initializeForWrite(pyramidId: String): Unit = throw new UnsupportedOperationException("TileServerPyramidIO is read-only")
  override def writeTiles[T](pyramidId: String, serializer: TileSerializer[T], data: JavaIterable[TileData[T]]): Unit = throw new UnsupportedOperationException("TileServerPyramidIO is read-only")
  override def removeTiles(id: String, tiles: JavaIterable[TileIndex]): Unit = throw new UnsupportedOperationException("TileServerPyramidIO is read-only")
  override def writeMetaData(pyramidId: String, metaData: String): Unit = throw new UnsupportedOperationException("TileServerPyramidIO is read-only")

  override def initializeForRead(pyramidId: String, width: Int, height: Int, dataDescription: Properties): Unit = {
    val configuration = JsonUtilities.propertiesObjToJSON(dataDescription)
    val request = ClientTileInitializationRequest(pyramidId, width, height, configuration)

    client.makeRequest(request)
  }

  private def submitAndWaitForResponse (request: ClientTileRequest): Unit = {
    val response = concurrent.future{
      client.makeRequest(request)
      while (!request.isAnswered) Thread.sleep(100)
      request
    }
    concurrent.Await.result(response, clientTimeout)
  }

  override def readMetaData(pyramidId: String): String = {
    val request = new ClientTileMetaDataRequest(pyramidId)
    submitAndWaitForResponse(request)

    if (request._error.isDefined) throw new IOException("Server error", request._error.get)
    request._metaData.get
  }

  override def readTiles[T](pyramidId: String, serializer: TileSerializer[T], tiles: JavaIterable[TileIndex]): JavaList[TileData[T]] = {
    val request = new ClientTileDataRequest[T](pyramidId, serializer, tiles)
    submitAndWaitForResponse(request)

    if (request._error.isDefined) throw new IOException("Server error", request._error.get)
    request._tiles.get
  }

  override def readTiles[T](pyramidId: String, serializer: TileSerializer[T], tiles: JavaIterable[TileIndex], properties: JSONObject): JavaList[TileData[T]] = {
    // TODO: Add properties to the tile request
    val request = new ClientTileDataRequest[T](pyramidId, serializer, tiles)
    submitAndWaitForResponse(request)

    if (request._error.isDefined) {
      throw new IOException("Server error", request._error.get)
    } else {
      request._tiles.get
    }
  }

  override def getTileStream[T](pyramidId: String, serializer: TileSerializer[T], tile: TileIndex): InputStream = {
    val request = new ClientTileStreamRequest[T](pyramidId, serializer, tile)
    submitAndWaitForResponse(request)

    if (request._error.isDefined) throw new IOException("Server error", request._error.get)
    new ByteArrayInputStream(request._data.get)
  }
}
