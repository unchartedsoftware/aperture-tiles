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

package com.oculusinfo.tilegen.graph.cluster

import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * 
 * Code adapted from Sotera's graphX implementation of the distributed Louvain modularity algorithm
 * https://github.com/Sotera/spark-distributed-louvain-modularity
 */
object IpAddress {
  
  def toString(address: Long) = {
    val byteBuffer = ByteBuffer.allocate(8)
    val addressBytes = byteBuffer.putLong(address)
    // The below is needed because we don't have an unsigned Long, and passing a byte array
    // with more than 4 bytes causes InetAddress to interpret it as a (bad) IPv6 address
    val tmp = new Array[Byte](4)
    Array.copy(addressBytes.array, 4, tmp, 0, 4)
    InetAddress.getByAddress(tmp).getHostAddress()
  }
  
  
  def toLong(_address: String): Long = {
    val address = try {
      InetAddress.getByName(_address)
    } catch {
      case e: Throwable => throw new IllegalArgumentException("Could not parse address: " + e.getMessage)
    }
    val addressBytes = address.getAddress
    val bb = ByteBuffer.allocate(8)
    addressBytes.length match {
      case 4 =>
        bb.put(Array[Byte](0,0,0,0)) // Need a filler
        bb.put(addressBytes)
      case n =>
        throw new IndexOutOfBoundsException("Expected 4 byte address, got " + n)
    }
    bb.getLong(0)
  }

}