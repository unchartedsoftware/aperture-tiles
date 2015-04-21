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
package com.oculusinfo.tilegen.pipeline

import org.apache.spark.SharedSparkContext
import org.scalatest.FunSuite

class PipelinesTests extends FunSuite with SharedSparkContext {

	case class TestData(x: Int, y: String)

	def testOp(name: String)(input: PipelineData) = {
		input
	}

	def parseTestOp(args: Map[String, String]) = {
		testOp(args("name"))_
	}

	test("Test functional pipeline tree traversal") {
		var data = List[String]()

		def testOp(opName: String)(input: PipelineData) = {
			data = data :+ opName
			input
		}

		val parent = PipelineStage("parent", testOp("p")(_))
		val child0 = PipelineStage("child0", testOp("c0")(_))
		val child1 = PipelineStage("child1", testOp("c1")(_))
		val grandchild0 = PipelineStage("grandchild0", testOp("g0")(_))
		val grandchild1 = PipelineStage("grandchild1", testOp("g1")(_))

		parent.addChild(child0)
		parent.addChild(child1)
		child0.addChild(grandchild0)
		child1.addChild(grandchild1)

		PipelineTree.execute(parent, sqlc)

		assertResult(List("p", "c0", "g0", "c1", "g1"))(data)
	}

	test("Test symbolic pipeline creation") {
		val pipelines = Pipelines()
			.createPipeline("pipeline")

		assert(pipelines.pipelineRoots.contains("pipeline"))
	}

	test("Test symbolic pipeline op registration") {
		val pipelines = Pipelines()
			.registerPipelineOp("some_op_one", parseTestOp)
			.registerPipelineOp("some_op_two", parseTestOp)
			.registerPipelineOp("some_op_three", parseTestOp)

		assert(pipelines.pipelineOps.contains("some_op_one"))
		assert(pipelines.pipelineOps.contains("some_op_two"))
		assert(pipelines.pipelineOps.contains("some_op_three"))
	}

	test("Test symbolic pipeline stage add") {
		val pipelines = Pipelines()
			.registerPipelineOp("operation", parseTestOp)
			.createPipeline("pipeline")
			.addPipelineStage("stage-0", "operation", Map("name" -> "foo-0"), "pipeline", "root", sqlc)
			.addPipelineStage("stage-1", "operation", Map("name" -> "foo-1"), "pipeline", "stage-0", sqlc)

		val stage0 = pipelines.getPipelineStage("pipeline", "stage-0")
		val stage1 = pipelines.getPipelineStage("pipeline", "stage-1")
		val stage2 = pipelines.getPipelineStage("pipeline", "stage-2")

		assert(stage0.isDefined)
		assert(stage1.isDefined)
		assert(stage2.isEmpty)
		assertResult("stage-0")(stage0.get.name)
		assertResult("stage-1")(stage1.get.name)
	}

	test("Test symbolic pipeline execute") {
		var data = List[String]()

		def parseTestOp(args: Map[String, String]) = {
			testOp(args("name"))_
		}

		def testOp(opName: String)(input: PipelineData) = {
			data = data :+ opName
			input
		}

		Pipelines()
			.registerPipelineOp("operation", parseTestOp)
			.createPipeline("pipeline")
			.addPipelineStage("stage-0", "operation", Map("name" -> "foo-0"), "pipeline", "root", sqlc)
			.addPipelineStage("stage-1", "operation", Map("name" -> "foo-1"), "pipeline", "stage-0", sqlc)
			.runPipeline("pipeline", sqlc)

		assertResult(List("foo-0", "foo-1"))(data)
	}
}
