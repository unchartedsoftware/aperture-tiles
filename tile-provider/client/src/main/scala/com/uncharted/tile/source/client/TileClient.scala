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



import com.uncharted.tile



/**
 * This class handles communications with a tile server, wrapping tile requests appropriately, and letting users know
 * when tiles are completed (or have errored).
 */
class TileClient(host: String)
extends Client[ClientTileRequest](host, tile.source.TILE_REQUEST_EXCHANGE) {
  def encodeRequest(request: ClientTileRequest): Array[Byte] =
    request.toByteArray

  def processResults(request: ClientTileRequest, contentType: String, contents: Array[Byte]) = {
    request.onFinished(contents)
  }
  override def processError (request: ClientTileRequest, severity: String, serverError: Throwable): Unit = {
    request.onError(serverError)
  }
}
