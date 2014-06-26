/*
 * Copyright (c) 2014 Oculus Info Inc.
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

package com.oculusinfo.tilegen.tiling



import org.scalatest.FunSuite



class IPv4TileIndexTestSuite extends FunSuite {
	test("Test indexing") {
		val ipv4 = Array[Byte](171.toByte, 172.toByte, 167.toByte, 176.toByte)
		val indexer = new IPv4ZCurveIndexScheme
		val cartesian = indexer.toCartesian(ipv4)

		assert(0x1234L === cartesian._1)
		assert(0xfedcL === cartesian._2)

		val cMin = indexer.toCartesian(Array(0.toByte, 0.toByte, 0.toByte, 0.toByte))
		assert(0L === cMin._1)
		assert(0L === cMin._2)

		val cMax = indexer.toCartesian(Array(255.toByte, 255.toByte, 255.toByte, 255.toByte))
		assert(0xffffL === cMax._1)
		assert(0xffffL === cMax._2)

		val xMax = indexer.toCartesian(Array(85.toByte, 85.toByte, 85.toByte, 85.toByte))
		assert(0xffffL === xMax._1)
		assert(0L      === xMax._2)

		val yMax = indexer.toCartesian(Array(170.toByte, 170.toByte, 170.toByte, 170.toByte))
		assert(0L      === yMax._1)
		assert(0xffffL === yMax._2)
	}
}
