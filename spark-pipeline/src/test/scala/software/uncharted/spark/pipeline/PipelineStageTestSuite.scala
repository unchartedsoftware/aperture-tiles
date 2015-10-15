package software.uncharted.spark.pipeline

import org.scalatest.FunSuite
import software.uncharted.spark.pipeline.PipelineData._

/**
 * Created by nkronenfeld on 10/15/2015.
 */
class PipelineStageTestSuite extends FunSuite {
  def testResult[D](expected: D)(stage: PipelineStage[D :: PDNil]): Unit = {
    val result :: endNil = stage.execute
    assert(result === expected)
  }
  test("Complex example") {
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
    val n1a = PipelineStage(1 :: PDNil)
    val n1b = PipelineStage("1" :: PDNil)
    val n1c = PipelineStage(1.2 :: PDNil)
    val n1d: PipelineStage[PDNil] = PipelineStage(PDNil)

    val n2a = PipelineStage((input: (Int :: PDNil) :: (String :: PDNil) :: PDNil) => {
      val (aI :: aN) :: (bS :: bN) :: cN = input
      ((aI * 3) + "=" + bS) :: PDNil
    }, n1a :: n1b :: PSINil)
    testResult("3=1")(n2a)
    val n2b = PipelineStage((input: (Double :: PDNil) :: PDNil :: PDNil) => {
      val (aD :: aN) :: bN :: cN = input
      val newValue = (aD*45).round/10.0
      newValue :: PDNil
    }, n1c :: n1d :: PSINil)
    testResult(5.4)(n2b)

    val n3a = PipelineStage((input: String :: PDNil) => {
      val value :: endnil = input
      (value + value) :: PDNil
    }, n2a)
    testResult("3=13=1")(n3a)
    val n3b = PipelineStage((input: (String :: PDNil) :: (Double :: PDNil) :: PDNil) => {
      val (aS :: aN) :: (dD :: dN) :: n = input
      """{"string-value": "%s", "double-value": %.1f}""".format(aS, dD) :: PDNil
    }, n2a :: n2b :: PSINil)
    testResult("""{"string-value": "3=1", "double-value": 5.4}""")(n3b)
    val n3c = PipelineStage((input: Double :: PDNil) => {
      val value :: endnil = input
      (value + ":" + value) :: PDNil
    }, n2b)
    testResult("5.4:5.4")(n3c)
    val n3d = PipelineStage((input: Double :: PDNil) => {
      val value :: endnil = input
      (value + value) :: PDNil
    }, n2b)
    testResult(10.8)(n3d)
  }
}
