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

import java.lang.{Iterable => JavaIterable}

import org.json.JSONObject

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.uncharted.tile.source.{RequestTypes, TileRequest, TileInitializationRequest, TileMetaDataRequest, TileDataRequest, TileStreamRequest}
import com.uncharted.tile.source.util.ByteArrayCommunicator
import software.uncharted.tile.source.{TileRequest, TileDataRequest, TileInitializationRequest}

import scala.Enumeration.Value


object ServerTileRequest {
  def fromByteArray (encoded: Array[Byte]): TileRequest = {
    val requestType = ByteArrayCommunicator.defaultCommunicator.read[Value](encoded)
    requestType match {
      case RequestTypes.Initialization => {
        val (requestType, table, width, height, rawConfiguration) =
          ByteArrayCommunicator.defaultCommunicator.read[RequestTypes.Value, String, Int, Int, String](encoded)
        new ServerTileInitializationRequest(table, width, height, new JSONObject(rawConfiguration))
      }
      case RequestTypes.Metadata => {
        val (requestType, table) =
          ByteArrayCommunicator.defaultCommunicator.read[RequestTypes.Value, String](encoded)
        new ServerTileMetaDataRequest(table)
      }
      case RequestTypes.Tiles => {
        val (requestType, table, serializer, indexInfos) =
          ByteArrayCommunicator.defaultCommunicator.read[RequestTypes.Value, String, TileSerializer[_], Array[Array[Int]]](encoded)

        def withTypedSerializer[T] (typedSerializer: TileSerializer[T]): ServerTileDataRequest[T] = {
          new ServerTileDataRequest[T](table, typedSerializer, TileDataRequest.indexInfoArrayToIndices(indexInfos))
        }
        withTypedSerializer(serializer)
      }
      case RequestTypes.TileStream => {
        val (requestType, table, serializer, index) =
          ByteArrayCommunicator.defaultCommunicator.read[RequestTypes.Value, String, TileSerializer[_], TileIndex](encoded)

        def withTypedSerializer[T] (typedSerializer: TileSerializer[T]): ServerTileStreamRequest[T] = {
          new ServerTileStreamRequest[T](table, typedSerializer, index)
        }
        withTypedSerializer(serializer)
      }
    }
  }
}

case class ServerTileInitializationRequest (table: String, width: Int, height: Int, configuration: JSONObject) extends TileInitializationRequest

case class ServerTileMetaDataRequest (table: String) extends TileMetaDataRequest

case class ServerTileDataRequest[T] (table: String, serializer: TileSerializer[T], indices: JavaIterable[TileIndex]) extends TileDataRequest[T]

case class ServerTileStreamRequest[T] (table: String, serializer: TileSerializer[T], index: TileIndex) extends TileStreamRequest[T]
