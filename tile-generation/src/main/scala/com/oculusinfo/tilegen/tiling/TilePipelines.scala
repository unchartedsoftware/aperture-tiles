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

import com.oculusinfo.tilegen.tiling.TilePipelines.PipelineArgParser
import org.apache.spark.sql.{SQLContext, SchemaRDD, StructType}

/**
 * Defines the data that is passed from stage to stage of the tile pipeline.
 */
case class PipelineData(sqlContext: SQLContext, srdd: SchemaRDD, tableName: Option[String] = None)

/**
 * Defines a pipeline stage that consists of an operation, a name and a list of child stages.
 */
case class PipelineStage(name: String, op: PipelineData => PipelineData, var children: List[PipelineStage] = List()) {
  def addChild(child: PipelineStage): PipelineStage = {
    children = children :+ child
    child
  }
}

object TilePipelines {

  /**
   * Function type that takes a map of arguments and binds them to a pipeline
   * operation.
   */
  type PipelineArgParser = Map[String, String] => (PipelineData => PipelineData)

  case class Empty(i: Int)

  /**
   *  Executes the pipeline via depth first tree traversal.
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
 * Abstracted tile pipeline interface - allows pipelines to be constructed using strings, making it suitable for calls
 * via a rest interface or a config file.
 */
class TilePipelines(val pipelineRoots: Map[String, PipelineStage] = Map(), val pipelineOps: Map[String, PipelineArgParser] = Map()) {

  def registerPipelineOp(pipelineOpId: String, pipelineOp: PipelineArgParser) = {
      new TilePipelines(pipelineRoots, pipelineOps + (pipelineOpId -> pipelineOp))
  }

  def createPipeline(pipelineId: String) = {
    new TilePipelines(pipelineRoots + (pipelineId -> new PipelineStage("root", input => input)), pipelineOps)
  }

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

  def getPipelineStage(pipelineId: String, stageId: String) = {
    findNode(stageId, List(pipelineRoots(pipelineId)))
  }

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
