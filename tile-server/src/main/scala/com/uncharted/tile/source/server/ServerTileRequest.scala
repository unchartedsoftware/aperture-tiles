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
package com.uncharted.tile.source.server



import java.util.{List => JavaList}
import com.oculusinfo.binning.TileIndex
import com.uncharted.tile.source.util.ByteArrayCommunicator
import org.json.JSONObject


trait TileRequest {
  val table: String
  val indices: JavaList[TileIndex]
  val configuration: JSONObject
}
object ServerTileRequest {
  def fromByteArray (encoded: Array[Byte]): TileRequest = {
    val (table, indices, rawConfiguration) =
      ByteArrayCommunicator.defaultCommunicator.read[String, JavaList[TileIndex], String](encoded)
    new ServerTileRequest(table, indices, new JSONObject(rawConfiguration))
  }
  def toByteArray (request: TileRequest): Array[Byte] = {
    ByteArrayCommunicator.defaultCommunicator.write(request.table, request.indices, request.configuration.toString())
  }

}
/**
 * Encapsulate all parts of a tile request into one easy-to-use package
 */
case class ServerTileRequest (table: String, indices: JavaList[TileIndex], configuration: JSONObject) extends TileRequest {
  def toByteArray: Array[Byte] =
    ByteArrayCommunicator.defaultCommunicator.write(table, indices, configuration.toString)
}
