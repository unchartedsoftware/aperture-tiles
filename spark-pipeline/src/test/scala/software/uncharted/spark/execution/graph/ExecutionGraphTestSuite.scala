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



class ExecutionGraphTestSuite extends FunSuite {
  /** Helper function to simplify and clarify checking execution results */
  def testResult[D](expected: D)(stage: ExecutionGraphNode[D :: EGDNil]): Unit = {
    val result :: endNil = stage.execute
    assert(result === expected)
  }

  /** Simple helper function to simplify creation of no-input graph nodes */
  def toPD[T] (t: T): T :: EGDNil = t :: EGDNil

  /** Simple helper function to simplify creation of single-input graph nodes */
  def fcn1[I, O] (baseFcn: I => O): I :: EGDNil => O :: EGDNil =
    inputWithNil => {
      val input :: endNil = inputWithNil
      baseFcn(input) :: EGDNil
    }

  test("Linear pipeline example") {
    //  simple example:
    //     a --> b --> c --> d
    val stageA = ExecutionGraphNode(toPD(1))
    val stageB = ExecutionGraphNode(fcn1((n: Int) => n + 4), stageA)
    val stageC = ExecutionGraphNode(fcn1((n: Int) => n*1.5), stageB)
    val stageD = ExecutionGraphNode(fcn1((d: Double) => (d*2).toInt), stageC)

    testResult(1)(stageA)
    testResult(5)(stageB)
    testResult(7.5)(stageC)
    testResult(15)(stageD)
  }

  test("Tree example") {
    //  tree example:
    //
    //        1a
    //       /  \
    //      /    \
    //    2a      2b
    //     |     /  \
    //     |    /    \
    //    3a  3b      3c

    val stage1A = ExecutionGraphNode(toPD("abcd"))

    val stage2A = ExecutionGraphNode(fcn1((s: String) => s + ": efgh"), stage1A)
    val stage2B = ExecutionGraphNode(fcn1((s: String) => s.length), stage1A)

    val stage3A = ExecutionGraphNode(fcn1((s: String) => s.split(":").map(_.trim).toList), stage2A)
    val stage3B = ExecutionGraphNode(fcn1((n: Int) => "length was "+n), stage2B)
    val stage3C = ExecutionGraphNode(fcn1((n: Int) => n*1.5), stage2B)

    testResult("abcd")(stage1A)
    testResult("abcd: efgh")(stage2A)
    testResult(4)(stage2B)
    testResult(List("abcd", "efgh"))(stage3A)
    testResult("length was 4")(stage3B)
    testResult(6.0)(stage3C)
  }

  test("Graph example") {
    //  complex example:
    //     1a      1b  1c      1d
    //       \    /      \    /
    //        \  /        \  /
    //         2a          2b
    //        /  \__    __/  \____
    //       /      \  /      \   \
    //     3a        3b        3c  3d
    //
    //    val node2a = new Node(2a) from new Node(1a) andFrom new Node(1b) to new Node(3a)
    //    val node2b = new Node(2b) from new Node(1c) andFrom new Node(1d) to new Node(3c) andTo new Node(3d)
    //    val node3b = new Node(3b) from node2a andFrom node2b
    val n1a = ExecutionGraphNode(1 :: EGDNil)
    val n1b = ExecutionGraphNode("1" :: EGDNil)
    val n1c = ExecutionGraphNode(1.2 :: EGDNil)
    val n1d: ExecutionGraphNode[EGDNil] = ExecutionGraphNode(EGDNil)

    val n2a = ExecutionGraphNode((input: Int :: String :: EGDNil) => {
      val aI :: bS :: endNil = input
      ((aI * 3) + "=" + bS) :: EGDNil
    }, n1a :: n1b :: EGNINil)
    testResult("3=1")(n2a)
    val n2b = ExecutionGraphNode((input:Double :: EGDNil) => {
      val aD :: endNil = input
      val newValue = (aD*45).round/10.0
      newValue :: EGDNil
    }, n1c :: n1d :: EGNINil)
    testResult(5.4)(n2b)

    val n3a = ExecutionGraphNode((input: String :: EGDNil) => {
      val value :: endnil = input
      (value + value) :: EGDNil
    }, n2a)
    testResult("3=13=1")(n3a)
    val n3b = ExecutionGraphNode((input: String :: Double :: EGDNil) => {
      val aS :: dD :: endNil = input
      """{"string-value": "%s", "double-value": %.1f}""".format(aS, dD) :: EGDNil
    }, n2a :: n2b :: EGNINil)
    testResult("""{"string-value": "3=1", "double-value": 5.4}""")(n3b)
    val n3c = ExecutionGraphNode((input: Double :: EGDNil) => {
      val value :: endnil = input
      (value + ":" + value) :: EGDNil
    }, n2b)
    testResult("5.4:5.4")(n3c)
    val n3d = ExecutionGraphNode((input: Double :: EGDNil) => {
      val value :: endnil = input
      (value + value) :: EGDNil
    }, n2b)
    testResult(10.8)(n3d)
  }
}
