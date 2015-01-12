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

import java.lang.{Double => JavaDouble}
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.tilegen.datasets.CountValueExtractor
import com.oculusinfo.tilegen.tiling.analytics._
import com.oculusinfo.tilegen.util.KeyValueArgumentSource

import scala.util.Try

/**
 * Provides operations and companion argument parsers that can be bound into a TilePipeline
 * tree.
 *
 * Note: These are mostly placeholders right now.
 */
object TileOperations {

  /**
   * KeyValueArgumentSource implementation that simply passes the supplied map through.
   */
  case class KeyValuePassthrough(args: Map[String, String]) extends KeyValueArgumentSource {
    def properties = args
  }

  /**
   * Registers this standard set of ops
   */
  def registerOperations(tilePipeline: TilePipelines) = {
    // Register basic pipeline operations
    tilePipeline.registerPipelineOp("json_load", parseLoadJsonDataOp)
    tilePipeline.registerPipelineOp("date_filter", parseDateFilterOp)
    tilePipeline.registerPipelineOp("cache", parseCacheDataOp)
  }

  /**
   * Parse data path from args
   */
  def parseLoadJsonDataOp(args: Map[String, String]) = {
    val argParser = new KeyValuePassthrough(args)
    val path = argParser.getString("path", "HDFS path to data")
    loadJsonDataOp(path)_
  }

  /**
   * Load data from a JSON file.  The schema is derived from the first json record.
   */
  def loadJsonDataOp(path: String)(data: PipelineData): PipelineData = {
    new PipelineData(data.sqlContext, data.sqlContext.jsonFile(path))
  }

  /**
   * Parse start / end times from args.  Arguments are:
   *
   * timeField: the name of the time column in the input schema RDD
   * start: the filter start date as a Long unix timestamp
   * end: the filter end date as a Long unix timestamp
   */
  def parseDateFilterOp(args: Map[String, String]) = {
    val argParser = new KeyValuePassthrough(args)
    val timeCol = argParser.getString("timeField", "Name of time field in input data")
    val start = argParser.getLong("start", "Start date")
    val end = argParser.getLong("end", "End date")
    dateFilterOp(start, end, timeCol)_
  }

  /**
   * Pipeline op to filter records to a date range
   */
  def dateFilterOp(minTime: Long,
                   maxTime: Long,
                   timeCol: String)
                  (input: PipelineData) = {
    val result = input.srdd.filter(row => row(0).asInstanceOf[Long] >= minTime && row(0).asInstanceOf[Long] <= maxTime)
    new PipelineData(input.sqlContext, result)
  }

  /**
   * Parses args for cache data.  No arguments required.
   */
  def parseCacheDataOp(args: Map[String, String]) = {
    cacheDataOp()_
  }

  /**
   * Pipeline op to cache data
   */
  def cacheDataOp()(input: PipelineData) = {
    new PipelineData(input.sqlContext, input.srdd.cache())
  }

  /**
   * Pipeline op to generate heatmap
   */
  def generateHeatmapOp(xCol: String,
                        yCol: String,
                        levelSets: Array[Array[Int]],
                        id: String,
                        name: String,
                        description: String,
                        tileIO: TileIO)
                       (input: PipelineData) = {
    val pyramid = new WebMercatorTilePyramid

    val indexScheme = new CartesianIndexScheme

    val heatmapAnalytic = new NumericSumBinningAnalytic[Double, JavaDouble]()

    // A tile analytic to compute max and min counts.  This gets applied globally, as well as per-level.
    val tileAnalytics = Some {
      val convert: JavaDouble => Double = a => a.doubleValue
      new CompositeAnalysisDescription(
        new AnalysisDescriptionTileWrapper(convert, new NumericMinTileAnalytic[Double]),
        new AnalysisDescriptionTileWrapper(convert, new NumericMaxTileAnalytic[Double]))
    }
    tileAnalytics.map(_.addGlobalAccumulator(input.sqlContext.sparkContext)) // apply globally

    // No data analytics to apply
    val dataAnalytics: Option[AnalysisDescription[((Double, Double), Double), Int]] = None

    input.tableName match {
      case None => input.srdd.registerTempTable("heatmapTable")
      case _ =>
    }
    val locations = input.sqlContext.sql("SELECT " + xCol + ", " + yCol + " FROM heatmapTable")

    val intNone: Option[Int] = None
    val tilableData = locations.flatMap(record => Try(((record(0).asInstanceOf[Double], record(1).asInstanceOf[Double]), 1.0, intNone)).toOption)

    val binner = new RDDBinner
    binner.debug = true
    levelSets.foreach { levels =>
      // Add whole-level analytic accumulators for these levels
      tileAnalytics.map(analytic => levels.map(level => analytic.addLevelAccumulator(input.sqlContext.sparkContext, level)))

      val tiles = binner.processDataByLevel(
        tilableData,
        indexScheme,
        heatmapAnalytic,
        tileAnalytics,
        dataAnalytics,
        pyramid,
        levels.toSeq,
        256, 256)

      tileIO.writeTileSet(pyramid,
        id,
        tiles,
        new CountValueExtractor,
        tileAnalytics,
        dataAnalytics,
        name,
        description)
    }
  }
}
