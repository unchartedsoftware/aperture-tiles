package software.uncharted.spark.execution.graph

import org.scalatest.FunSuite

/**
 * Created by nkronenfeld on 10/13/2015.
 */
class PipelineMetadataTestSuite extends FunSuite {
  test("Test basic typed get, put, and apply") {
    val data = new PipelineMetadata
    data.put("string", "abc")
    data.put("int", 123)
    assert(123 === data[Int]("int"))
    assert("abc" === data.get[String]("string").get)
  }

  test("Test separation by type") {
    val data = new PipelineMetadata
    data.put("duplicate", "abc")
    data.put("duplicate", 123)
    assert(123 === data[Int]("duplicate"))
    assert("abc" === data[String]("duplicate"))
  }

  test("Test remove") {
    val data = new PipelineMetadata
    data.put("key", "abc")
    data.put("key", 123)

    assert(123 === data[Int]("key"))
    assert("abc" === data[String]("key"))

    data.remove[Int]("key")

    assert(None === data.get[Int]("key"))
    assert("abc" === data[String]("key"))
  }
}
