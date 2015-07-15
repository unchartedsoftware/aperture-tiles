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
package com.uncharted.tile.source.util


import java.io.NotSerializableException

import com.oculusinfo.binning.util.JSONUtilitiesTests
import org.json.JSONObject
import org.scalatest.FunSuite



class ByteArrayCommunicatorTestSuite extends FunSuite {
  test("Test writing and reading booleans") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(true, false, false, true)
    val results = com.read[Boolean, Boolean, Boolean, Boolean](storage)

    assert(true === results._1)
    assert(false === results._2)
    assert(false === results._3)
    assert(true === results._4)
  }

  test("Test writing and reading bytes") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(1.toByte, 2.toByte, 3.toByte, 4.toByte)
    val results = com.read[Byte, Byte, Byte, Byte](storage)

    assert(1.toByte === results._1)
    assert(2.toByte === results._2)
    assert(3.toByte === results._3)
    assert(4.toByte === results._4)
  }

  test("Test writing and reading chars") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(1.toChar, 2.toChar, 3.toChar, 4.toChar)
    val results = com.read[Char, Char, Char, Char](storage)

    assert(1.toChar === results._1)
    assert(2.toChar === results._2)
    assert(3.toChar === results._3)
    assert(4.toChar === results._4)
  }

  test("Test writing and reading shorts") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(1.toShort, 2.toShort, 3.toShort, 4.toShort)
    val results = com.read[Short, Short, Short, Short](storage)

    assert(1.toShort === results._1)
    assert(2.toShort === results._2)
    assert(3.toShort === results._3)
    assert(4.toShort === results._4)
  }

  test("Test writing and reading integers") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(1, 2, 3, 4)
    val results = com.read[Int, Int, Int, Int](storage)

    assert(1 === results._1)
    assert(2 === results._2)
    assert(3 === results._3)
    assert(4 === results._4)
  }

  test("Test writing and reading longs") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(1L, 2L, 3L, 4L)
    val results = com.read[Long, Long, Long, Long](storage)

    assert(1L === results._1)
    assert(2L === results._2)
    assert(3L === results._3)
    assert(4L === results._4)
  }

  test("Test writing and reading floats") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(1.0f, 2.0f, 3.0f, 4.0f)
    val results = com.read[Float, Float, Float, Float](storage)

    assert(1.0f === results._1)
    assert(2.0f === results._2)
    assert(3.0f === results._3)
    assert(4.0f === results._4)
  }

  test("Test writing and reading doubles") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(1.0, 2.0, 3.0, 4.0)
    val results = com.read[Double, Double, Double, Double](storage)

    assert(1.0 === results._1)
    assert(2.0 === results._2)
    assert(3.0 === results._3)
    assert(4.0 === results._4)
  }

  test("Test writing and reading objects") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write("abc", new UnsupportedOperationException("foo"))
    val results = com.read[String, Throwable](storage)

    assert("abc" === results._1)
    assert(results._2.isInstanceOf[UnsupportedOperationException] && "foo" === results._2.getMessage)
  }

  test("Test mixed writing") {
    val com = new JavaSerializationByteArrayCommunicator
    val storage = com.write(true, "abc", 4, 3.0, 6L, new UnsupportedOperationException("foo"))
    val results = com.read[Boolean, String, Int, Double, Long, Throwable](storage)

    assert(true === results._1)
    assert("abc" === results._2)
    assert(4 === results._3)
    assert(3.0 === results._4)
    assert(6L === results._5)
    assert(results._6.isInstanceOf[UnsupportedOperationException] && "foo" === results._6.getMessage)
  }

  test("Make sure unserializable things aren't accepted") {
    val com = new JavaSerializationByteArrayCommunicator
    intercept[NotSerializableException] {
      com.write(new JSONObject("""{"x": 4}"""))
    }
  }

  test("Test all argument lengths") {
    val com = new JavaSerializationByteArrayCommunicator

    assert(3 === com.read[Int](com.write(3)))
    assert((3, 4) === com.read[Int, Int](com.write(3, 4)))
    assert((3, 4, 5) === com.read[Int, Int, Int](com.write(3, 4, 5)))
    assert((3, 4, 5, 6) === com.read[Int, Int, Int, Int](com.write(3, 4, 5, 6)))
    assert((3, 4, 5, 6, 7) === com.read[Int, Int, Int, Int, Int](com.write(3, 4, 5, 6, 7)))
    assert((3, 4, 5, 6, 7, 8) === com.read[Int, Int, Int, Int, Int, Int](com.write(3, 4, 5, 6, 7, 8)))
    assert((3, 4, 5, 6, 7, 8, 9) === com.read[Int, Int, Int, Int, Int, Int, Int](com.write(3, 4, 5, 6, 7, 8, 9)))
  }
}
