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



import scala.collection.mutable.{Map => MutableMap}



/**
 * Created by nkronenfeld on 11/4/2015.
 */
object PipelineDebugOperations {
  /**
   * Time all operations from the last cache to the current point, allowing further operations to start from this point
   * without having to run previous ops
   *
   * @param name The name by which to record the time in the timestamp map
   * @param timestamps A map of all timestamps recorded by versions of this operation
   */
  def timestampOp(name: String, timestamps: MutableMap[String, Long])(data: PipelineData): PipelineData = {
    data.srdd.cache

    val startTime = System.currentTimeMillis()
    data.srdd.count
    val endTime = System.currentTimeMillis()
    val time = endTime - startTime

    timestamps(name) = time
    data
  }

  /**
   * Time all operations from the last cache to the current point.
   *
   * This can run the previous op several times first, to solidify any caching that is in place
   *
   * @param name The name by which to record the time in the timestamp map
   * @param timestamps A map of all timestamps recorded by versions of this operation
   */
  def timestampNoCacheOp(name: String, timestamps: MutableMap[String, Long], dryRunIterations: Int = 0)(data: PipelineData): PipelineData = {
    // Run the previous op a couple times to solidify any caching it has already done
    (1 to dryRunIterations).map(n => data.srdd.count)

    // Time an iteration through the previous op
    val startTime = System.currentTimeMillis()
    data.srdd.count
    val endTime = System.currentTimeMillis()
    val time = endTime - startTime

    timestamps(name) = time
    data
  }

  /**
   * Print out the RDD DAG of the input pipeline data
   * @param prefix A prefix to print first to make it clear in the logs where we are
   */
  def printInputDAGOp (prefix: String)(data: PipelineData): PipelineData = {
    println(prefix)
    println(data.srdd.rdd.toDebugString)
    data
  }

  // Sample usage code
//  def pipeline () = {
//    val stage1 = PipelineStage
//    val stage2 = PipelineStage
//    val stage3 = PipelineStage
//    val stage4 = PipelineStage
//    val stage5 = PipelineStage
//    val stage6 = PipelineStage
//
//    // version 1
//    val times = MutableMap[String, Long]()
//    stage1
//      .addChild(new PipelineStage("time stage 2", timestampOp("stage 2", times)(_)))
//      .addChild(stage2)
//      .addChild(new PipelineStage("time stage 3", timestampOp("stage 3", times)(_)))
//      .addChild(stage3)
//
//    stage1
//      .addChild(new PipelineStage("time stage 1", timestampOp("stage 1", times, 3)(_)))
//      .addChild(new PipelineStage("cache stage 1", timestampNoCacheOp("cache stage 1", cacheOp(_))))
//      .addChild(stage2)
//      .addChild(new PipelineStage("time stage 2", timestampOp("stage 2", times, 3)(_)))
//      .addChild(new PipelineStage("cache stage 2", timestampNoCacheOp("cache stage 2", cacheOp(_))))
//      .addChild(stage3)
//      .addChild(new PipelineStage("time stage 3", timestampOp("stage 3", times, 3)(_)))
//      .addChild(new PipelineStage("cache stage 3", timestampNoCacheOp("cache stage 3", cacheOp(_))))
//  }
}
