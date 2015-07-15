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

/**
 * This package contains a tile server, which can communicate to clients using RabbitMQ, so as to hand them tiles.
 *
 * Tile requests are done using a request/callback pattern - all requests must be asynchronous.
 *
 * I think the way this will work is that the client sends a request that includes a private queue name, and listens
 * on that private queue.  The server fulfills the request, and pushes it to that private queue (or sends an error,
 * if appropriate)
 */
package object server {
  val TILE_REQUEST_EXCHANGE = "tile-requests"
  val TILE = "tile"
  val LOG_EXCHANGE = "tile-request-log"
  val LOG_ERROR = "error"
  val LOG_WARNING = "warning"
  val LOG_INFO = "info"
  val LOG_DEBUG = "debug"
}
