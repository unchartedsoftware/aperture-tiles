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
import java.util.concurrent.atomic.AtomicInteger

import grizzled.slf4j.Logger

import org.apache.avro.file.CodecFactory

import org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute

import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer
import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.tilegen.datasets._
import com.oculusinfo.tilegen.tiling.analytics.AnalysisDescriptionTileWrapper
import com.oculusinfo.tilegen.tiling.analytics.ComposedTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMaxTileAnalytic
import com.oculusinfo.tilegen.tiling.analytics.NumericMinTileAnalytic
import com.oculusinfo.tilegen.util.KeyValueArgumentSource

/**
 * Provides operations and companion argument parsers that can be bound into a TilePipeline
 * tree.
 *
 */
// TODO: Add error checking to all operations and parsers
object TileOperations {
	import com.oculusinfo.tilegen.datasets.SchemaTypeUtilities._

	import scala.collection.JavaConversions._

	protected val logger = Logger[this.type]
	protected var tableIdCount = new AtomicInteger(0)

	/**
	 * KeyValueArgumentSource implementation that passes the supplied map through.
	 *
	 * @param args Wrapped argument map.
	 */
	case class KeyValuePassthrough(args: Map[String, String]) extends KeyValueArgumentSource {
		def properties = args.map(entry => entry._1.toLowerCase -> entry._2)
	}

	/**
	 * Registers a standard set of operations against a pipeline.
	 *
	 * @param tilePipeline Unique ID of the pipeline registered against.
	 * @return Updated tile pipeline object.
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

		logger.debug(s"Registered tile operations: ${tilePipeline.pipelineOps.keys}")

		tilePipeline
	}

	/**
	 * Parses arguments for the JSON load operation, and creates an instance
	 * of a pipeline operation with those arguments applied to it.
	 *
	 * Arguments:
	 *    ops.path - Valid HDFS path to data.
	 */
	def parseLoadJsonDataOp(args: Map[String, String]) = {
		logger.debug(s"Parsing loadJsonDataOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val path = argParser.getString("ops.path", "HDFS path to data")
		loadJsonDataOp(path)(_)
	}

	/**
	 * Load data from a JSON file.  The schema is derived from the first json record.
	 *
	 * @param path Valid HDFS path to the data.
	 * @param data Not used.
	 * @return PipelineData with a schema RDD populated from the JSON file.
	 */
	def loadJsonDataOp(path: String)(data: PipelineData): PipelineData = {
		PipelineData(data.sqlContext, data.sqlContext.jsonFile(path))
	}

	/**
	 * Parses arguments for the CSV load operation, and creates an instance
	 * of a pipeline operation with those arguments applied to it.
	 *
	 * Arguments:
	 *    ops.path - Valid HDFS path to data.
	 *
	 * @see com.oculusinfo.tilegen.datasets.CSVReader for arguments passed into the CSV reader.
	 *
	 */
	def parseLoadCsvDataOp(args: Map[String, String]) = {
		logger.debug(s"Parsing loadCSVDataOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val path = argParser.getString("ops.path", "HDFS path to data")
		loadCsvDataOp(path, argParser)(_)
	}

	/**
	 * Load data from a CSV file using CSVReader.
	 * The arguments map will be passed through to that object, so all arguments required
	 * for its configuration should be set in the map.
	 *
	 * @param path HDSF path to the data object.
	 * @param argumentSource Arguments to forward to the CSVReader.
	 * @param data Not used.
	 * @return PipelineData with a schema RDD populated from the CSV file.
	 */
	def loadCsvDataOp(path: String, argumentSource: KeyValueArgumentSource)(data: PipelineData): PipelineData = {
		val reader = new CSVReader(data.sqlContext, path, argumentSource)
		PipelineData(reader.sqlc, reader.asSchemaRDD)
	}

	/**
	 * Parse start / end times from args for the date filter op, and creates an instance
	 * of a pipeline operation with those arguments applied to it.
	 *
	 * Arguments:
	 *    ops.column - Column spec denoting the time field in input data
	 *    ops.start - Start date string
	 *    ops.end - End date string
	 *    ops.format - Date format string (based on [[java.text.SimpleDateFormat]])
	 */
	def parseDateFilterOp(args: Map[String, String]) = {
		logger.debug(s"Parsing dateFilterOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val timeCol = argParser.getString("ops.column", "Col spec denoting the time field in input data")
		val start = argParser.getString("ops.start", "Start date")
		val end = argParser.getString("ops.end", "End date")
		val formatter = argParser.getString("ops.format", "Date format string")
		dateFilterOp(start, end, formatter, timeCol)(_)
	}

	/**
	 * Pipeline op to filter records to a specific date range.
	 *
	 * @param minDate Start date for the range, expressed in a format parsable by [[java.text.SimpleDateFormat]].
	 * @param maxDate End date for the range, expressed in a format parsable by [[java.text.SimpleDateFormat]].
	 * @param format Date parsing string, expressed according to [[java.text.SimpleDateFormat]].
	 * @param timeCol Column spec denoting name of time column in input schema RDD.
	 * @param input Input pipeline data to filter.
	 * @return Transformed pipeline data, where records outside the specified time range have been removed.
	 */
	def dateFilterOp(minDate: String, maxDate: String, format: String, timeCol: String)(input: PipelineData) = {
		val formatter = new SimpleDateFormat(format)
		val minTime = formatter.parse(minDate).getTime
		val maxTime = formatter.parse(maxDate).getTime
		val timeExtractor = calculateExtractor(timeCol, input.srdd.schema)
		val filtered = input.sqlContext.createDataFrame(input.srdd.rdd.filter { row =>
			val time = formatter.parse(timeExtractor(row).toString).getTime
			minTime <= time && time <= maxTime
		}, input.srdd.schema)
		PipelineData(input.sqlContext, filtered)
	}

	/**
	 * Parses args for cache data operation.  No arguments required.
	 */
	def parseCacheDataOp(args: Map[String, String]) = {
		logger.debug(s"Parsing cacheDataOp with args $args")
		cacheDataOp()(_)
	}

	/**
	 * Pipeline op to cache data - this allows for subsequent stages in the pipeline to run against computed
	 * results, rather than the input data set.
	 */
	def cacheDataOp()(input: PipelineData) = {
		val tableName = getOrGenTableName(input, "cached_table_")
		input.sqlContext.cacheTable(tableName)
		PipelineData(input.sqlContext, input.srdd, Some(tableName))
	}

	/**
	 * Parses args for an integral n-dimensional range filter and returns an instance of the filter
	 * with those arguments applied to it.  The dimensionality is consistent across the min, max and
	 * columns arguments.
	 *
	 * Arguments:
	 *  ops.min - sequence of min values, 1 for each dimension of the data
	 *  ops.max - sequence of max values, 1 for each dimension of the data
	 *  ops.exclude - boolean indicating whether values in the range are excluded or included.
	 *  ops.columns - sequence of column specs, 1 for each dimension of the data
	 */
	def parseIntegralRangeFilterOp(args: Map[String, String]) = {
		logger.debug(s"Parsing integralRangeFilterOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val min = argParser.getLongSeq("ops.min", "min value")
		val max = argParser.getLongSeq("ops.max", "max value")
		val exclude = argParser.getBoolean("ops.exclude", "exclude values in the range", Some(false))
		val colSpecs = argParser.getStringSeq("ops.columns", "column ids")
		integralRangeFilterOp(min, max, colSpecs, exclude)(_)
	}

	/**
	 * A generalized n-dimensional range filter operation for integral types.
	 *
	 * @param min Sequence of min values, 1 for each dimension of the data
	 * @param max Sequence of max values, 1 for each dimension of the data
	 * @param colSpecs Sequence of column specs, 1 for each dimension of the data
	 * @param exclude Boolean indicating whether values in the range are excluded or included.
	 * @param input Pipeline data to apply filter to
	 * @return Transformed pipeline data, where records inside/outside the specified time range have been removed.
	 */
	def integralRangeFilterOp(min: Seq[Long], max: Seq[Long], colSpecs: Seq[String], exclude: Boolean = false)(input: PipelineData) = {
		val extractors = colSpecs.map(cs => calculateExtractor(cs, input.srdd.schema))
		val result = input.sqlContext.createDataFrame(input.srdd.rdd.filter { row =>
			val data = extractors.map(_(row).asInstanceOf[Number].longValue)
			val inRange = data.zip(min).forall(x => x._1 >= x._2) && data.zip(max).forall(x => x._1 <= x._2)
			if (exclude) !inRange else inRange
		}, input.srdd.schema)
		PipelineData(input.sqlContext, result)
	}

	/**
	 * Parses args for a fractional n-dimensional range filter and returns an instance of the filter
	 * with those arguments applied to it.  The dimensionality is consistent across the min, max and
	 * columns arguments.
	 *
	 * Arguments:
	 *  ops.min - sequence of min values, 1 for each dimension of the data
	 *  ops.max - sequence of max values, 1 for each dimension of the data
	 *  ops.exclude - boolean indicating whether values in the range are excluded or included.
	 *  ops.columns - sequence of column specs, 1 for each dimension of the data
	 */
	def parseFractionalRangeFilterOp(args: Map[String, String]) = {
		logger.debug(s"Parsing fractionalRangeFilterOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val min = argParser.getDoubleSeq("ops.min", "min value")
		val max = argParser.getDoubleSeq("ops.max", "max value")
		val exclude = argParser.getBoolean("ops.exclude", "exclude values in the range", Some(false))
		val colSpecs = argParser.getStringSeq("ops.columns", "numeric column id")
		fractionalRangeFilterOp(min, max, colSpecs, exclude)(_)
	}

	/**
	 * A generalized n-dimensional range filter operation for fractional types.
	 *
	 * @param min Sequence of min values, 1 for each dimension of the data
	 * @param max Sequence of max values, 1 for each dimension of the data
	 * @param colSpecs Sequence of column specs, 1 for each dimension of the data
	 * @param exclude Boolean indicating whether values in the range are excluded or included.
	 * @param input Pipeline data to apply filter to
	 * @return Transformed pipeline data, where records inside/outside the specified time range have been removed.
	 */
	def fractionalRangeFilterOp(min: Seq[Double], max: Seq[Double], colSpecs: Seq[String], exclude: Boolean = false)(input: PipelineData) = {
		val extractors = colSpecs.map(cs => calculateExtractor(cs, input.srdd.schema))
		val result = input.sqlContext.createDataFrame(input.srdd.rdd.filter { row =>
			val data = extractors.map(_(row).asInstanceOf[Number].doubleValue)
			val inRange = data.zip(min).forall(x => x._1 >= x._2) && data.zip(max).forall(x => x._1 <= x._2)
			if (exclude) !inRange else inRange
		}, input.srdd.schema)
		PipelineData(input.sqlContext, result)
	}

	/**
	 * Parse args for regex filter operation and returns an instance of the operation with those
	 * arguments applied.
	 *
	 * Arguments:
	 *  ops.regex - Regex string to use as filter
	 *  ops.column - Column spec denoting the column to test the regex against.
	 *  ops.exclude - Boolean indicating whether to exclude or include values that match
	 */
	def parseRegexFilterOp(args: Map[String, String]) = {
		logger.debug(s"Parsing regexFilterOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val regex = argParser.getString("ops.regex", "regex to use as filter")
		val colSpec = argParser.getString("ops.column", "numeric column id")
		val exclude = argParser.getBoolean("ops.exclude", "exclude values that match", Some(false))
		regexFilterOp(regex, colSpec, exclude)(_)
	}

	/**
	 * A regex filter operation.
	 *
	 * @param regexStr Regex string to use as filter
	 * @param colSpec Column spec denoting the column to test the regex against.
	 * @param exclude Boolean indicating whether to exclude or include values that match.
	 * @param input Pipeline data to apply the filter to.
	 * @return Transformed pipeline data, where records matching the regex are included or excluded.
	 */
	def regexFilterOp(regexStr: String, colSpec: String, exclude: Boolean = false)(input: PipelineData) = {
		val regex = regexStr.r
		val extractor = calculateExtractor(colSpec, input.srdd.schema)
		val result = input.sqlContext.createDataFrame(input.srdd.rdd.filter { row =>
			extractor(row).toString match {
				case regex(_*) => if (exclude) false else true
				case _ => if (exclude) true else false
			}
		}, input.srdd.schema)
		PipelineData(input.sqlContext, result)
	}

	/**
	 * Parse operation args for operation that creates a new schema RDD from selected columns, and return
	 * an instance of that operation with those arguments applied.
	 *
	 * Arguments:
	 *  ops.columns - Sequence of column specs denoting the columns to select.
	 */
	def parseColumnSelectOp(args: Map[String, String]) = {
		logger.debug(s"Parsing columnSelectOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val colSpecs = argParser.getStringSeq("ops.columns", "Col specs denoting columns to select")
		columnSelectOp(colSpecs)(_)
	}

	/**
	 * Operation to create a new schema RDD from selected columns.
	 *
	 * @param colSpecs Sequence of column specs denoting the columns to select.
	 * @param input Pipeline data to select columns from.
	 * @return Pipeline data containing a schema RDD with only the selected columns.
	 */
	def columnSelectOp(colSpecs: Seq[String])(input: PipelineData) = {
//		val colExprs = colSpecs.map(UnresolvedAttribute(_))
//		val result = input.srdd.select(colExprs:_*)
		val result = input.srdd.selectExpr(colSpecs:_*)
		PipelineData(input.sqlContext, result)
	}

	/**
	 * Parse args for basic heatmap tile generator operation that writes to HDFS, and return an instance of that
	 * operation with those arguments applied.
	 *
	 * Arguments:
	 *    hbase.zookeeper.quorum - Zookeeper quorum addresses specified as a comma separated list.
	 *    hbase.zookeeper.port - Zookeeper port.
	 *    hbase.master - HBase master address.
	 *    ops.xColumn - Colspec denoting data x column
	 *    ops.yColumn - Colspec denoting data y column
	 *
	 *    Tiling task consumes a TilingTaskParameters object that
	 *    is populated via an argument map.  The argument map passed to this function will be used for that
	 *    purpose, so all arguments required by that object should be set.
	 */
	def parseHbaseHeatmapOp(args: Map[String, String]) = {
		logger.debug(s"Parsing hbaseHeatmapOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val zookeeperQuorum = argParser.getString("hbase.zookeeper.quorum", "Zookeeper quorum addresses")
		val zookeeperPort = argParser.getString("hbase.zookeeper.port", "Zookeeper port")
		val hbaseMaster = argParser.getString("hbase.master", "HBase master address")
		val heatmapParams = parseHeatMapOpImpl(args, argParser)

		hbaseHeatMapOp(heatmapParams._1, heatmapParams._2, heatmapParams._3, zookeeperQuorum, zookeeperPort, hbaseMaster)(_)
	}

	/**
	 * Parse args for basic heatmap tile generator operation that writes to the local file system, and return an
	 * instance of that operation with those arguments applied.
	 *
	 * Arguments:
	 *    ops.xColumn - Colspec denoting data x column
	 *    ops.yColumn - Colspec denoting data y column
	 *
	 *    Tiling task consumes a TilingTaskParameters object that
	 *    is populated via an argument map.  The argument map passed to this function will be used for that
	 *    purpose, so all arguments required by that object should be set.
	 */
	def parseFileHeatmapOp(args: Map[String, String]) = {
		logger.debug(s"Parsing fileHeatmapOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val heatmapParams = parseHeatMapOpImpl(args, argParser)
		fileHeatMapOp(heatmapParams._1, heatmapParams._2, heatmapParams._3)(_)
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
	 * A basic heatmap tile generator that writes output to HDFS. Uses
	 * TilingTaskParameters to manage tiling task arguments; the arguments map
	 * will be passed through to that object, so all arguments required for its configuration should be set in the map.
	 *
	 * @param xColSpec Colspec denoting data x column
	 * @param yColSpec Colspec denoting data y column
	 * @param tilingParams Parameters to forward to the tiling task.
	 * @param zookeeperQuorum Zookeeper quorum addresses specified as a comma separated list.
	 * @param zookeeperPort Zookeeper port.
	 * @param hbaseMaster HBase master address.
	 * @param input Pipeline data to tile.
	 * @return Unmodified input data.
	 */
	def hbaseHeatMapOp(xColSpec: String,
	                   yColSpec: String,
	                   tilingParams: TilingTaskParameters,
	                   zookeeperQuorum: String,
	                   zookeeperPort: String,
	                   hbaseMaster: String)
	                  (input: PipelineData) = {

		val tileIO = new HBaseTileIO(zookeeperQuorum, zookeeperPort, hbaseMaster)
		heatMapOpImpl(xColSpec, yColSpec, tilingParams, tileIO)(input)
	}

	/**
	 * A basic heatmap tile generator that writes output to the local file system. Uses
	 * TilingTaskParameters to manage tiling task arguments; the arguments map
	 * will be passed through to that object, so all arguments required for its configuration should be set in the map.
	 *
	 * @param xColSpec Colspec denoting data x column
	 * @param yColSpec Colspec denoting data y column
	 * @param tilingParams Parameters to forward to the tiling task.
	 * @param input Pipeline data to tile.
	 * @return Unmodified input data.
	 */
	def fileHeatMapOp(xColSpec: String,
	                  yColSpec: String,
	                  tilingParams: TilingTaskParameters)
	                 (input: PipelineData) = {
		val tileIO = new LocalTileIO("avro")
		heatMapOpImpl(xColSpec, yColSpec, tilingParams, tileIO)(input)
	}

	private def heatMapOpImpl(xColSpec: String,
	                          yColSpec: String,
	                          tilingParms: TilingTaskParameters,
	                          tileIO: TileIO)
	                         (input: PipelineData) = {
		// TODO: Switch to the factory based invocation
		val valuer = new CountValueExtractor[Long, JavaLong](new PrimitiveAvroSerializer[JavaLong](classOf[JavaLong], CodecFactory.bzip2Codec))
		val indexer = new CartesianIndexExtractor(xColSpec, yColSpec)
		val deferredPyramid = new DeferredTilePyramid(new AOITilePyramid(0.0, 0.0, 1.0, 1.0), true)

		// TODO: Replace with analytics from valuer after move to factory invocation
		val tileAnalytics = new ExtremaTileAnalytic

		val tilingTask = new StaticTilingTask(
			input.sqlContext,
			getOrGenTableName(input, "heatmap_op"),
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

	/**
	 * Gets a table name out of the input if one exists, otherwise creates a new name
	 * using a base and an internally incremented counter.
	 *
	 * @param input PipelineData object with an optional table data name
	 * @param baseName Name to append counter to if no table name is found
	 */
	def getOrGenTableName(input: PipelineData, baseName: String) = {
		input.tableName.getOrElse {
			val name = baseName + tableIdCount.getAndIncrement
			input.srdd.registerTempTable(name)
			name
		}
	}
}
