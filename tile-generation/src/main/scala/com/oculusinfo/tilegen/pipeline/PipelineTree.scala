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

import grizzled.slf4j.Logging
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SQLContext}

/**
 * Data that is passed from stage to stage of the tile pipeline.
 *
 * @param sqlContext The spark SQL context
 * @param srdd Valid DataFrame
 * @param tableName Optional associated temporary table name
 */
case class PipelineData(sqlContext: SQLContext, srdd: DataFrame, tableName: Option[String] = None)

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
 * Functions for executing pipelines.  A pipeline is created by building a tree of PipelineStage
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
 * Pipelines.execute(parent, sqlc)
 * }}}
 *
 */
object PipelineTree extends Logging {
	/**
	 * Executes the pipeline via depth first tree traversal.
	 *
	 * @param start PipelineStage to start the traversal from
	 * @param sqlContext Spark SQL context to run the jobs under
	 * @param input Optional start data.  Data based on an empty DataFrame will be used if not set.
	 */
	def execute(start: PipelineStage, sqlContext: SQLContext, input: Option[PipelineData] = None) = {
		// TODO: Should run a check for cycles here (tsort?)
		def ex(stage: PipelineStage, result: PipelineData): Unit = {
			logger.info(s"Executing pipeline stage [${stage.name}]")
			val stageResult = stage.op(result)
			stage.children.foreach(ex(_, stageResult))
		}

		input match {
			case Some(i) => ex(start, i)
			case None =>
				val emptySchema = sqlContext.jsonRDD(sqlContext.sparkContext.emptyRDD[String], new StructType(Array()))
				ex(start, PipelineData(sqlContext, emptySchema))
		}
	}
}
