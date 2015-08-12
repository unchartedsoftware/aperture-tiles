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
package com.uncharted.tile

/**
 * This package, and sub-packages, deals with sourcing tiles from an external server.
 *
 * The external server is in the server sub-package, the client that uses it, in the client sub-package.
 *
 * The client/server communications for this external server is handles using RabbitMQ, but these communications
 * should all be encapsulated within the client, so users need not know that (except for configuration details,
 * of course).
 */
package object source {
  val TILE_REQUEST_EXCHANGE = "tile-requests"
  val LOG_EXCHANGE = "tile-request-log"
  val LOG_ERROR = "error"
  val LOG_WARNING = "warning"
  val LOG_INFO = "info"
  val LOG_DEBUG = "debug"

  val UNIT_TYPE = "no-response"
  val UNIT_RESPONSE = Array[Byte]()
}
