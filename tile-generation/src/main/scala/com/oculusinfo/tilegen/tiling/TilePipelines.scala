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

import com.oculusinfo.tilegen.tiling.TilePipelines.PipelineOpBinding
import org.apache.spark.sql.{SQLContext, SchemaRDD, StructType}

/**
 * Data that is passed from stage to stage of the tile pipeline.
 *
 * @param sqlContext The spark SQL context
 * @param srdd Valid SchemaRDD
 * @param tableName Optional associated temporary table name
 */
case class PipelineData(sqlContext: SQLContext, srdd: SchemaRDD, tableName: Option[String] = None)

/**
 * Tile pipeline tree node that transforms PipelineData
 *
 * @param name The name of the pipeline stage
 * @param op The transformation operation applied by this stage
 * @param children The children of this stage
 */
case class PipelineStage(name: String, op: PipelineData => PipelineData, var children: List[PipelineStage] = List()) {
  def addChild(child: PipelineStage): PipelineStage = {
    children = children :+ child
    child
  }
}

/**
 * Functions for executing tile pipelines.  A tile pipeline is created by building a tree of PipelineStage
 * objects and passing them to the execute function. Example:
 *
 * {{{
 * def foo(opName: String)(input: PipelineData) = {
 *   PipelineData(input.sqlContext, input.srdd.map(_ * 2))
 * }
 *
 * val parent = PipelineStage("parent", foo("p")(_))
 * val child0 = PipelineStage("child0", foo("c0")(_))
 * val child1 = PipelineStage("child1", foo("c1")(_))
 * val grandchild0 = PipelineStage("grandchild0", foo("g0")(_))
 *
 * parent.addChild(child0)
 * parent.addChild(child1)
 * child0.addChild(grandchild0)
 *
 * TilePipelines.execute(parent, sqlc)
 * }}}
 *
 */
object TilePipelines {

  /**
   * Function type that takes a map of arguments and binds them to a pipeline
   * operation.
   */
  type PipelineOpBinding = Map[String, String] => (PipelineData => PipelineData)

  /**
   * Executes the pipeline via depth first tree traversal.
   *
   * @param start PipelineStage to start the traversal from
   * @param sqlContext Spark SQL context to run the jobs under
   * @param input Optional start data.  Data based on an empty SchemaRDD will be used if not set.
   */
  def execute(start: PipelineStage, sqlContext: SQLContext, input: Option[PipelineData] = None) = {
    // TODO: Should run a check for cycles here (tsort?)
    def ex(stage: PipelineStage, result: PipelineData): Unit = {
      val stageResult = stage.op(result)
      stage.children.foreach(ex(_, stageResult))
    }

    input match {
      case Some(i) => ex(start, i)
      case None =>
        val emptySchema = sqlContext.jsonRDD(sqlContext.sparkContext.emptyRDD[String], new StructType(Seq()))
        ex(start, new PipelineData(sqlContext, emptySchema))
    }

  }
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
 * val pipelines = new TilePipelines()
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
class TilePipelines(val pipelineRoots: Map[String, PipelineStage] = Map.empty,
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
      new TilePipelines(pipelineRoots, pipelineOps + (pipelineOpId -> pipelineOpBinding))
  }

  /**
   * Creates a new pipeline with an assigned ID.
   *
   * @param pipelineId Unique identifer for the pipeline.
   * @return Updated instance of this object.
   */
  def createPipeline(pipelineId: String) = {
    new TilePipelines(pipelineRoots + (pipelineId -> new PipelineStage("root", input => input)), pipelineOps)
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
      .map(_.addChild(new PipelineStage(stageId, pipelineFunc)))
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
    TilePipelines.execute(pipelineRoots(pipelineId), sqlContext)
  }

  private def findNode(nodeId: String, toVisit: List[PipelineStage]): Option[PipelineStage] = {
    toVisit match {
      case x :: xs if x.name != nodeId => findNode(nodeId, xs ++ x.children)
      case x :: xs if x.name == nodeId => Some(x)
      case Nil => None
    }
  }
}
