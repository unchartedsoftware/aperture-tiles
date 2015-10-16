package software.uncharted.spark.pipeline



import org.scalatest.FunSuite

import software.uncharted.spark.pipeline.ExecutionGraphData._



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
