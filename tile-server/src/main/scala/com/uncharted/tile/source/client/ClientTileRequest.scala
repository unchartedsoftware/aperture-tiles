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



import java.util.{List => JavaList}
import org.json.JSONObject

import com.oculusinfo.binning.{TileData, TileIndex}
import com.uncharted.tile.source.server.TileRequest



/**
 * This class encapsulates the information and methods needed to process a tile request
 *
 * @tparam T The bin type of the tiles to retrieve
 */
trait ClientTileRequest[T] extends TileRequest {
  /** The name of the table from which to retrieve tiles */
  val table: String
  /** The tile indices to retrieve */
  val indices: JavaList[TileIndex]
  /**
   * The configuration from which the server should construct relevant PyramidIOs and TileSerializers with which to
   * request tiles
   */
  val configuration: JSONObject

  /** Called when a tile is retrieved */
  def onTileRetrieved (tiles: JavaList[TileData[T]])

  /** Called when an error was encountered retrieving tiles */
  def onError (t: Throwable)
}
