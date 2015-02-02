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

import java.lang.{Double => JavaDouble, Long => JavaLong}
import java.text.SimpleDateFormat

import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.tilegen.datasets._
import com.oculusinfo.tilegen.tiling.analytics.{AnalysisDescriptionTileWrapper, ComposedTileAnalytic, NumericMaxTileAnalytic, NumericMinTileAnalytic}
import com.oculusinfo.tilegen.util.KeyValueArgumentSource
import org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute

/**
 * Provides operations and companion argument parsers that can be bound into a TilePipeline
 * tree.
 *
 */
// TODO: Provide example of usage in object docs above
// TODO: Document args for each operation
// TODO: Add error checking to all operations and parsers
object TileOperations {
  import com.oculusinfo.tilegen.datasets.SchemaTypeUtilities._

import scala.collection.JavaConversions._

  /**
   * KeyValueArgumentSource implementation that simply passes the supplied map through.
   */
  case class KeyValuePassthrough(args: Map[String, String]) extends KeyValueArgumentSource {
    def properties = args.map(entry => entry._1.toLowerCase -> entry._2)
  }

  /**
   * Registers this standard set of ops
   */
  def registerOperations(tilePipeline: TilePipelines) = {
    // Register basic pipeline operations
    tilePipeline.registerPipelineOp("csv_load", parseLoadCsvDataOp)
    tilePipeline.registerPipelineOp("json_load", parseLoadJsonDataOp)
    tilePipeline.registerPipelineOp("cache", parseCacheDataOp)
    tilePipeline.registerPipelineOp("date_filter", parseDateFilterOp)
    tilePipeline.registerPipelineOp("integral_range_filter", parseIntegralRangeFilterOp)
    tilePipeline.registerPipelineOp("fractional_range_filter", parseFractionalRangeFilterOp)
    tilePipeline.registerPipelineOp("regex_filter", parseRegexFilterOp)
    tilePipeline.registerPipelineOp("file_heatmap_tiling", parseFileHeatmapOp)
    tilePipeline.registerPipelineOp("hbase_heatmap_tiling", parseHbaseHeatmapOp)
  }

  /**
   * Parse data path from args
   */
  def parseLoadJsonDataOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val path = argParser.getString("ops.path", "HDFS path to data")
    loadJsonDataOp(path)(_)
  }

  /**
   * Load data from a JSON file.  The schema is derived from the first json record.
   */
  def loadJsonDataOp(path: String)(data: PipelineData): PipelineData = {
    PipelineData(data.sqlContext, data.sqlContext.jsonFile(path))
  }

  /**
   * Parse data path from args
   */
  def parseLoadCsvDataOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val path = argParser.getString("ops.path", "HDFS path to data")
    loadCsvDataOp(path, argParser)(_)
  }

  /**
   * Load data from a CSV file.  The schema is derived from the first json record.
   */
  def loadCsvDataOp(path: String, argumentSource: KeyValueArgumentSource)(data: PipelineData): PipelineData = {
    val reader = new CSVReader(data.sqlContext, path, argumentSource)
    PipelineData(reader.sqlc, reader.asSchemaRDD)
  }

  /**
   * Parse start / end times from args.
   */
  def parseDateFilterOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val timeCol = argParser.getString("ops.column", "Col spec denoting the time field in input data")
    val start = argParser.getString("ops.start", "Start date")
    val end = argParser.getString("ops.end", "End date")
    val formatter = argParser.getString("ops.format", "Date format string")
    dateFilterOp(start, end, formatter, timeCol)(_)
  }

  /**
   * Pipeline op to filter records to a specific date range
   */
  def dateFilterOp(minDate: String, maxDate: String, format: String, timeCol: String)(input: PipelineData) = {
    val formatter = new SimpleDateFormat(format)
    val minTime = formatter.parse(minDate).getTime
    val maxTime = formatter.parse(maxDate).getTime
    val timeExtractor = calculateExtractor(timeCol, input.srdd.schema)
    val filtered = input.srdd.filter { row =>
      val time = formatter.parse(timeExtractor(row).toString).getTime
      minTime <= time && time <= maxTime
    }
    PipelineData(input.sqlContext, filtered)
  }

  /**
   * Parses args for cache data.  No arguments required.
   */
  def parseCacheDataOp(args: Map[String, String]) = {
    cacheDataOp()(_)
  }

  /**
   * Pipeline op to cache data
   */
  def cacheDataOp()(input: PipelineData) = {
    input.srdd.registerTempTable("cached_table")
    input.sqlContext.cacheTable("cached_table")
    PipelineData(input.sqlContext, input.srdd, Some("cached_table"))
  }

  /**
   * Parse args for integral n-dimensional range filter
   */
  def parseIntegralRangeFilterOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val min = argParser.getLongSeq("ops.min", "min value")
    val max = argParser.getLongSeq("ops.max", "max value")
    val exclude = argParser.getBoolean("ops.exclude", "exclude values in the range", Some(false))
    val colSpecs = argParser.getStringSeq("ops.columns", "numeric column id")
    integralRangeFilterOp(min, max, colSpecs, exclude)(_)
  }

  /**
  * A generalized n-dimensional range filter operation for integral types.
  */
  def integralRangeFilterOp(min: Seq[Long], max: Seq[Long], colSpecs: Seq[String], exclude: Boolean = false)(input: PipelineData) = {
    val extractors = colSpecs.map(cs => calculateExtractor(cs, input.srdd.schema))
    val result = input.srdd.filter { row =>
      val data = extractors.map(_(row).asInstanceOf[Number].longValue)
      val inRange = data.zip(min).forall(x => x._1 >= x._2) && data.zip(max).forall(x => x._1 <= x._2)
      if (exclude) !inRange else inRange
    }
    PipelineData(input.sqlContext, result)
  }

  /**
   * Parse args for n-dimensional fractional range filter
   */
  def parseFractionalRangeFilterOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val min = argParser.getDoubleSeq("ops.min", "min value")
    val max = argParser.getDoubleSeq("ops.max", "max value")
    val exclude = argParser.getBoolean("ops.exclude", "exclude values in the range", Some(false))
    val colSpecs = argParser.getStringSeq("ops.columns", "numeric column id")
    fractionalRangeFilterOp(min, max, colSpecs, exclude)(_)
  }

  /**
   * A generalized n-dimensional range filter operation for fractional types
   */
  def fractionalRangeFilterOp(min: Seq[Double], max: Seq[Double], colSpecs: Seq[String], exclude: Boolean = false)(input: PipelineData) = {
    val extractors = colSpecs.map(cs => calculateExtractor(cs, input.srdd.schema))
    val result = input.srdd.filter { row =>
      val data = extractors.map(_(row).asInstanceOf[Number].doubleValue)
      val inRange = data.zip(min).forall(x => x._1 >= x._2) && data.zip(max).forall(x => x._1 <= x._2)
      if (exclude) !inRange else inRange
    }
    PipelineData(input.sqlContext, result)
  }

  /**
   * Parse args for regex filter operation
   */
  def parseRegexFilterOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val regex = argParser.getString("ops.regex", "regex to use as filter")
    val colSpec = argParser.getString("ops.column", "numeric column id")
    val exclude = argParser.getBoolean("ops.exclude", "exclude values that match", Some(false))
    regexFilterOp(regex, colSpec, exclude)(_)
  }

  /**
   * A regex filter operation
   */
  def regexFilterOp(regexStr: String, colSpec: String, exclude: Boolean = false)(input: PipelineData) = {
    val regex = regexStr.r
    val extractor = calculateExtractor(colSpec, input.srdd.schema)
    val result = input.srdd.filter { row =>
      extractor(row).toString match {
        case regex(_*) => if (exclude) false else true
        case _ => if (exclude) true else false
      }
    }
    PipelineData(input.sqlContext, result)
  }

  /**
   * Parse operation to create a new SchemaRDD from selected columns
   */
  def parseColumnSelectOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val colSpecs = argParser.getStringSeq("ops.columns", "Col specs denoting columns to select")
    columnSelectOp(colSpecs)(_)
  }

  /**
   * Operation to create a new SchemaRDD from selected columns
   */
  def columnSelectOp(colSpecs: Seq[String])(input: PipelineData) = {
    val colExprs = colSpecs.map(UnresolvedAttribute(_))
    val result = input.srdd.select(colExprs:_*)
    PipelineData(input.sqlContext, result)
  }

  /**
   * Parse args for basic heatmap tile generator.
   */
  def parseHbaseHeatmapOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val zookeeperQuorum = argParser.getString("hbase.zookeeper.quorum", "Zookeeper quorum addresses")
    val zookeeperPort = argParser.getString("hbase.zookeeper.port", "Zookeeper quorum addresses")
    val hbaseMaster = argParser.getString("hbase.master", "Zookeeper quorum addresses")
    val heatmapParams = parseHeatMapOpImpl(args, argParser)

    hbaseHeatMapOp(heatmapParams._1, heatmapParams._2, heatmapParams._3, zookeeperQuorum, zookeeperPort, hbaseMaster)(_)
  }

  private def parseHeatMapOpImpl(args: Map[String, String], argParser: KeyValueArgumentSource) = {
    val xColSpec = argParser.getString("ops.xColumn", "Colspec denoting data x column")
    val yColSpec = argParser.getString("ops.yColumn", "Colspec denoting data y column")

    val taskParametersFactory = new TilingTaskParametersFactory(null, List("ops"))
    taskParametersFactory.readConfiguration(JsonUtilities.mapToJSON(args))
    val taskParameters = taskParametersFactory.produce(classOf[TilingTaskParameters])

    (xColSpec, yColSpec, taskParameters)
  }

  /**
   * A basic heatmap tile generator that writes output to HDFS.
   */
  def hbaseHeatMapOp(xColSpec: String,
                yColSpec: String,
                tilingParms: TilingTaskParameters,
                zookeeperQuorum: String,
                zookeeperPort: String,
                hbaseMaster: String)
               (input: PipelineData) = {

    val tileIO = new HBaseTileIO(zookeeperQuorum, zookeeperPort, hbaseMaster)
    heatMapOpImpl(xColSpec, yColSpec, tilingParms, tileIO)(input)
  }

  /**
   * Parse args for basic heatmap tile generator.
   */
  def parseFileHeatmapOp(args: Map[String, String]) = {
    val argParser = KeyValuePassthrough(args)
    val heatmapParams = parseHeatMapOpImpl(args, argParser)
    fileHeatMapOp(heatmapParams._1, heatmapParams._2, heatmapParams._3)(_)
  }

  /**
   * A basic heatmap tile generator that writes output to the local file system.
   */
  def fileHeatMapOp(xColSpec: String,
                yColSpec: String,
                tilingParms: TilingTaskParameters)
               (input: PipelineData) = {
    val tileIO = new LocalTileIO(".avro")
    heatMapOpImpl(xColSpec, yColSpec, tilingParms, tileIO)(input)
  }

  private def heatMapOpImpl(xColSpec: String,
                    yColSpec: String,
                    tilingParms: TilingTaskParameters,
                    tileIO: TileIO)
                   (input: PipelineData) = {
    // TODO: Switch to the factory based invocation
    val valuer = new CountValueExtractor[Long, java.lang.Long]()
    val indexer = new CartesianIndexExtractor(xColSpec, yColSpec)
    val deferredPyramid = new DeferredTilePyramid(new AOITilePyramid(0.0, 0.0, 1.0, 1.0), true)

    // TODO: Replace with analytics from valuer after move to factory invocation
    val tileAnalytics = new ExtremaTileAnalytic

    input.srdd.registerTempTable("heatmap_op")

    val tilingTask = new StaticTilingTask(
      input.sqlContext,
      input.tableName.getOrElse("heatmap_op"),
      tilingParms,
      indexer,
      valuer,
      deferredPyramid,
      Nil,
      None,
      Some(tileAnalytics))

    tilingTask.initialize()
    tilingTask.doTiling(tileIO)

    PipelineData(input.sqlContext, input.srdd, Some("heatmap_op"))
  }

  /**
   * Composite analytic to compute local min/max value of Double data for each tile the pyramid.
   */
  class ExtremaTileAnalytic extends AnalysisDescriptionTileWrapper[JavaLong, (Long, Long)](
    d => (d.longValue(), d.longValue()),
    new ComposedTileAnalytic[Long, Long](
      new NumericMinTileAnalytic[Long](),
      new NumericMaxTileAnalytic[Long]()))

  /**
   * Analytic to compute local max value of Double data for each tile in the pyramid.
   */
  class MaxTileAnalytic extends AnalysisDescriptionTileWrapper[JavaDouble, Long](
    _.longValue,
    new NumericMaxTileAnalytic[Long]()) {}

  /**
   * Analytic to compute local min value of Double data for each tile in the pyramid.
   */
  class MinTileAnalytic extends AnalysisDescriptionTileWrapper[JavaDouble, Long](
    _.longValue,
    new NumericMaxTileAnalytic[Long]()) {}

}
