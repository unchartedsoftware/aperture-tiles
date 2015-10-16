/*
 * Copyright © 2013-2015 Uncharted Software Inc.
 *
 * Property of Uncharted™, formerly Oculus Info Inc.
 * http://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package software.uncharted.spark.execution.graph

import org.scalatest.FunSuite

import ExecutionGraphData._



/**
 * Created by nkronenfeld on 10/15/2015.
 */
class ExecutionGraphDataTestSuite extends FunSuite {
  test("Test basic typed data construction") {
    val eg1: Int :: Int :: EGDNil = 1 :: 2 :: EGDNil
    val a :: b :: c = eg1
    assert(1 === a)
    assert(2 === b)
  }
  test("Test concattenation of data types") {
    val eg1 = 1 :: 2 :: EGDNil
    val eg2 = 3 :: 4 :: EGDNil
    val eg3 = eg1 ::: eg2
    val a :: b :: c :: d :: e = eg3
    assert(1 === a)
    assert(2 === b)
    assert(3 === c)
    assert(4 === d)
  }
}
