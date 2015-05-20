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
package com.oculusinfo.tilegen.pipeline

import com.oculusinfo.tilegen.pipeline.Pipelines.PipelineOpBinding
import grizzled.slf4j.Logger
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType

object Pipelines {

	protected val logger = Logger[this.type]

	/**
	 * Function type that takes a map of arguments and binds them to a pipeline
	 * operation.
	 */
	type PipelineOpBinding = Map[String, String] => (PipelineData => PipelineData)

	def apply(pipelineRoots: Map[String, PipelineStage] = Map.empty,
	          pipelineOps: Map[String, PipelineOpBinding] = Map.empty) = new Pipelines(pipelineRoots, pipelineOps)
}

/**
 * Symbolic tile pipeline interface - allows pipelines to be constructed and manipulated using strings, making it
 * suitable for calls via a rest interface or a config file.  Direct programmatic creation and execution should be
 * done by instancing PipelineStage objects and using the execute function directly.
 *
 * Example usage:
 *
 * {{{
 * def testOp(name: String)(input: PipelineData) = input
 * def parseTestOp(args: Map[String, String]) = testOp(args("name"))(_)
 * val pipelines = Pipelines()
 *    .registerPipelineOp("some-operation", parseTestOp)
 *    .createPipeline("some-pipeline")
 *    .addPipelineStage("stage-0", "some-operation", Map("name" -> "foo-0"), "some-pipeline", "root", sqlc)
 *    .addPipelineStage("stage-1", "some-operation", Map("name" -> "foo-1"), "some-pipeline", "stage-0", sqlc)
 *    .runPipeline("some-pipeline", sqlc)
 * }}}
 *
 * @param pipelineRoots Map of [pipeline ID, PipelineStage] tuples that define the roots for a set of pipelines.
 *                      Defaults to empty.
 * @param pipelineOps Map of [pipeline stage type ID, PipelineArgParser] tuples that define available pipeline
 *                    operation types.  Defaults to empty.
 */
class Pipelines(val pipelineRoots: Map[String, PipelineStage] = Map.empty,
                    val pipelineOps: Map[String, PipelineOpBinding] = Map.empty) {

	/**
	 * Adds a new pipeline operation with an assigned ID.
	 *
	 * @param pipelineOpId Unique identifier for the operation binding.
	 * @param pipelineOpBinding Function to bind [string,string] arguments to a pipeline data tranform
	 *                          function.
	 * @return Updated instance of this object.
	 */
	def registerPipelineOp(pipelineOpId: String, pipelineOpBinding: PipelineOpBinding) = {
		Pipelines(pipelineRoots, pipelineOps + (pipelineOpId -> pipelineOpBinding))
	}

	/**
	 * Creates a new pipeline with an assigned ID.
	 *
	 * @param pipelineId Unique identifer for the pipeline.
	 * @return Updated instance of this object.
	 */
	def createPipeline(pipelineId: String) = {
		Pipelines(pipelineRoots + (pipelineId -> PipelineStage("root", input => input)), pipelineOps)
	}

	/**
	 * Adds a new stage to an existing pipeline.
	 *
	 * @param stageId Unique ID for this stage.
	 * @param stageType Type of stage, selected from those added by calls to registerPipelineOp.
	 * @param stageArgs Arguments to bind to the stage
	 * @param pipelineId ID of the pipeline this stage will be part of
	 * @param parentStageId ID of the this stage's parent
	 * @param sqlContext Spark SQL context to operate under
	 * @return Updated instance of this object.
	 */
	def addPipelineStage(stageId: String,
	                     stageType: String,
	                     stageArgs: Map[String, String],
	                     pipelineId: String,
	                     parentStageId: String,
	                     sqlContext: SQLContext) = {
		val pipelineFunc = pipelineOps(stageType)(stageArgs)
		findNode(parentStageId, List(pipelineRoots(pipelineId)))
			.map(_.addChild(PipelineStage(stageId, pipelineFunc)))
		this
	}

	/**
	 * Gets a stage from a pipeline.
	 *
	 * @param pipelineId Unique ID of the pipeline containing the stage.
	 * @param stageId Unique ID of the stage to fetch.
	 */
	def getPipelineStage(pipelineId: String, stageId: String) = {
		findNode(stageId, List(pipelineRoots(pipelineId)))
	}

	/**
	 * Executes a pipeline.
	 *
	 * @param pipelineId Unique ID of the pipeline.
	 * @param sqlContext Spark SQL context to run the job under.
	 */
	def runPipeline(pipelineId: String, sqlContext: SQLContext) = {
		PipelineTree.execute(pipelineRoots(pipelineId), sqlContext)
	}

	private def findNode(nodeId: String, toVisit: List[PipelineStage]): Option[PipelineStage] = {
		toVisit match {
			case x :: xs if x.name != nodeId => findNode(nodeId, xs ++ x.children)
			case x :: xs if x.name == nodeId => Some(x)
			case Nil => None
		}
	}
}
