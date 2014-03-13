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


import scala.collection.JavaConverters._
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}

import org.scalatest.FunSuite



class PoissonTestSuite extends FunSuite {
  def toJava (l: List[Double]) = l.map(new JavaDouble(_)).asJava

  def assertSeqsEqual[T] (a: Seq[T], b: Seq[T]): Unit = {
    assert(a.size == b.size)
    for (n <- 0 until a.size) assert(a(n) == b(n))
  }

  def assertListsEqual[T] (a: JavaList[T], b: JavaList[T]): Unit = {
    assert(a.size == b.size)
    for (n <- 0 until a.size) assert(a.get(n) == b.get(n))
  }

  test("Standard Double Bin Descriptor") {
    val bd = new StandardDoubleBinDescriptor
    assert(0.0 == bd.defaultBinValue)
    assert(1.0 == bd.min(new JavaDouble(1.0),
                         new JavaDouble(2.0)).doubleValue)
    assert(2.0 == bd.max(new JavaDouble(1.0),
                         new JavaDouble(2.0)).doubleValue)
    assert(3.0 == bd.aggregateBins(1.0, 2.0))
    assert(4.0 == bd.convert(4.0).doubleValue)
  }


  test("Minimum Double Bin Descriptor") {
    val bd = new MinimumDoubleBinDescriptor
    assert(0.0 == bd.defaultBinValue)
    assert(1.0 == bd.min(new JavaDouble(1.0),
                         new JavaDouble(2.0)).doubleValue)
    assert(2.0 == bd.max(new JavaDouble(1.0),
                         new JavaDouble(2.0)).doubleValue)
    assert(1.0 == bd.aggregateBins(1.0, 2.0))
    assert(4.0 == bd.convert(4.0).doubleValue)
  }


  test("Maximum Double Bin Descriptor") {
    val bd = new MaximumDoubleBinDescriptor
    assert(0.0 == bd.defaultBinValue)
    assert(1.0 == bd.min(new JavaDouble(1.0),
                         new JavaDouble(2.0)).doubleValue)
    assert(2.0 == bd.max(new JavaDouble(1.0),
                         new JavaDouble(2.0)).doubleValue)
    assert(2.0 == bd.aggregateBins(1.0, 2.0))
    assert(4.0 == bd.convert(4.0).doubleValue)
  }

  test("Standard Double Array Bin Descriptor") {
    val aBase = List(1.0, 2.0, 3.0, 4.0)
    val a = toJava(aBase)
    val bBase = List(5.0, 4.0, 3.0, 2.0, 1.0)
    val b = toJava(bBase)

    val bd = new StandardDoubleArrayBinDescriptor
    assertListsEqual(bd.min(bd.defaultMin, bd.min(a, b)),
                     toJava(List(1.0, 2.0, 3.0, 2.0, 1.0)))

    assertListsEqual(bd.max(bd.defaultMax, bd.max(a, b)),
                     toJava(List(5.0, 4.0, 3.0, 4.0, 1.0)))

    assertSeqsEqual(bd.aggregateBins(aBase, bBase),
                     List(6.0, 6.0, 6.0, 6.0, 1.0))
  }
}


