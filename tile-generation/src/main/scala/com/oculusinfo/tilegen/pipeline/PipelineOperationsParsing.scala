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

import com.oculusinfo.binning.util.JsonUtilities
import com.oculusinfo.factory.properties.IntegerProperty
import com.oculusinfo.tilegen.datasets.{LineDrawingType, TilingTaskParameters, TilingTaskParametersFactory}
import com.oculusinfo.tilegen.pipeline.PipelineOperations._
import com.oculusinfo.tilegen.util.KeyValueArgumentSource
import grizzled.slf4j.Logging

/**
 * Provides argument parsers that can be bound into a TilePipeline
 * tree.
 *
 */
// TODO: Add error checking to all operations and parsers
object PipelineOperationsParsing extends Logging {
	import scala.collection.JavaConversions._

	/**
	 * Registers a standard set of operations against a pipeline.
	 *
	 * @param tilePipeline Unique ID of the pipeline registered against.
	 * @return Updated tile pipeline object.
	 */
	def registerOperations(tilePipeline: Pipelines) = {
		// Register basic pipeline operations
		tilePipeline.registerPipelineOp("csv_load", parseLoadCsvDataOp)
		tilePipeline.registerPipelineOp("json_load", parseLoadJsonDataOp)
		tilePipeline.registerPipelineOp("cache", parseCacheDataOp)
		tilePipeline.registerPipelineOp("date_filter", parseDateFilterOp)
		tilePipeline.registerPipelineOp("mercator_filter", parseMercatorFilterOp)
		tilePipeline.registerPipelineOp("integral_range_filter", parseIntegralRangeFilterOp)
		tilePipeline.registerPipelineOp("fractional_range_filter", parseFractionalRangeFilterOp)
		tilePipeline.registerPipelineOp("regex_filter", parseRegexFilterOp)
		tilePipeline.registerPipelineOp("geo_heatmap_tiling", parseGeoHeatMapOp)
		tilePipeline.registerPipelineOp("crossplot_heatmap_tiling", parseCrossplotHeatmapOp)

		logger.debug(s"Registered tile operations: ${tilePipeline.pipelineOps.keys}")

		tilePipeline
	}

	/**
	 * Parses arguments for the JSON load operation, and creates an instance
	 * of a pipeline operation with those arguments applied to it.
	 *
	 * Arguments:
	 *		ops.path - Valid HDFS path to data.
	 *		ops.partitions - Number of partitions
	 */
	def parseLoadJsonDataOp(args: Map[String, String]) = {
		logger.debug(s"Parsing loadJsonDataOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val path = argParser.getString("ops.path", "HDFS path to data")
		val partitions = argParser.getIntOption("ops.partitions", "Number of data partitions")
		loadJsonDataOp(path, partitions)(_)
	}

	/**
	 * Parses arguments for the CSV load operation, and creates an instance
	 * of a pipeline operation with those arguments applied to it.
	 *
	 * Arguments:
	 *		ops.path - Valid HDFS path to data.
	 *		ops.partitions - Number of partitions
	 *
	 * @see com.oculusinfo.tilegen.datasets.CSVReader for arguments passed into the CSV reader.
	 *
	 */
	def parseLoadCsvDataOp(args: Map[String, String]) = {
		logger.debug(s"Parsing loadCSVDataOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val path = argParser.getString("ops.path", "HDFS path to data")
		val partitions = argParser.getIntOption("ops.partitions", "Number of data partitions")
		val errorLog = argParser.getStringOption("ops.errorLog", "File stream to output load parse errors", None)
		loadCsvDataOp(path, argParser, partitions, errorLog)(_)
	}

	/**
	 * Parse start / end times from args for the date filter op, and creates an instance
	 * of a pipeline operation with those arguments applied to it.
	 *
	 * Arguments:
	 *		ops.column - Column spec denoting the time field in input data
	 *		ops.start - Start date string
	 *		ops.end - End date string
	 *		ops.format - Date format string (based on java.text.SimpleDateFormat)
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
	 * Parses args for cache data operation.	No arguments required.
	 */
	def parseCacheDataOp(args: Map[String, String]) = {
		logger.debug(s"Parsing cacheDataOp with args $args")
		cacheDataOp()(_)
	}

	/**
	 * Parses args for a filter to the valid range of a mercator geographic projection.
	 *
	 * Arguments:
	 *	ops.latitude - the column containing the latitude value
	 */
	def parseMercatorFilterOp (args: Map[String, String]) = {
		logger.debug(s"Parsing mercatorFilterOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val latCol = argParser.getString("ops.latitude", "The column containing the latitude value")
		mercatorFilterOp(latCol)(_)
	}

	/**
	 * Parses args for an integral n-dimensional range filter and returns an instance of the filter
	 * with those arguments applied to it.	The dimensionality is consistent across the min, max and
	 * columns arguments.
	 *
	 * Arguments:
	 *	ops.min - sequence of min values, 1 for each dimension of the data
	 *	ops.max - sequence of max values, 1 for each dimension of the data
	 *	ops.exclude - boolean indicating whether values in the range are excluded or included.
	 *	ops.columns - sequence of column specs, 1 for each dimension of the data
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
	 * Parses args for a fractional n-dimensional range filter and returns an instance of the filter
	 * with those arguments applied to it.	The dimensionality is consistent across the min, max and
	 * columns arguments.
	 *
	 * Arguments:
	 *	ops.min - sequence of min values, 1 for each dimension of the data
	 *	ops.max - sequence of max values, 1 for each dimension of the data
	 *	ops.exclude - boolean indicating whether values in the range are excluded or included.
	 *	ops.columns - sequence of column specs, 1 for each dimension of the data
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
	 * Parse args for regex filter operation and returns an instance of the operation with those
	 * arguments applied.
	 *
	 * Arguments:
	 *	ops.regex - Regex string to use as filter
	 *	ops.column - Column spec denoting the column to test the regex against.
	 *	ops.exclude - Boolean indicating whether to exclude or include values that match
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
	 * Parse operation args for operation that creates a new schema RDD from selected columns, and return
	 * an instance of that operation with those arguments applied.
	 *
	 * Arguments:
	 *	ops.columns - Sequence of column specs denoting the columns to select.
	 *	distinct - Boolean indicating whether or not the selected columns should contain only distinct values
	 */
	def parseColumnSelectOp(args: Map[String, String], distinct:Boolean=false) = {
		logger.debug(s"Parsing columnSelectOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val colSpecs = argParser.getStringSeq("ops.columns", "Col specs denoting columns to select")
		if (distinct)	{
			columnSelectDistinctOp(colSpecs)(_)
		} else {
			columnSelectOp(colSpecs)(_)
		}
	}

	/**
	 * Parse args for basic heatmap tile generator operation that writes to HDFS, and return an instance of that
	 * operation with those arguments applied.	If HBase properties are not set, file based IO is used.
	 *
	 * Arguments:
	 *		hbase.zookeeper.quorum - Zookeeper quorum addresses specified as a comma separated list. (optional)
	 *		hbase.zookeeper.port - Zookeeper port. (optional)
	 *		hbase.master - HBase master address. (optional)
	 *		ops.xColumn - Colspec denoting data x column
	 *		ops.yColumn - Colspec denoting data y column
	 *		ops.aggregationType - Aggregation operation applied during tiling - allowed values are "count",
	 *													"sum", "mean", "max"
	 *		ops.valueColumn - Colspec denoting data column to use as aggregation source.	Not required when type is
	 *
	 *		Tiling task consumes a TilingTaskParameters object that
	 *		is populated via an argument map.	The argument map passed to this function will be used for that
	 *		purpose, so all arguments required by that object should be set.
	 */
	def parseGeoHeatMapOp(args: Map[String, String]) = {
		logger.debug(s"Parsing hbaseHeatmapOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val heatmapParams = parseHeatMapOpImpl(args, argParser)
		geoHeatMapOp(heatmapParams._1, heatmapParams._2, heatmapParams._3, heatmapParams._4, heatmapParams._5,
								 heatmapParams._6, heatmapParams._7)(_)
	}

	/**
	 * Parse args for basic heatmap tile generator operation that writes to the local file system, and return an
	 * instance of that operation with those arguments applied.	If hbase parameters are unset, file IO will be used.
	 * Likewise, if bounds are not set, auto bounds will be used.
	 *
	 * Arguments:
	 *		ops.xColumn - Colspec denoting data x column
	 *		ops.yColumn - Colspec denoting data y column
	 *
	 *		ops.bounds.minX - Bounds minX value (optional)
	 *		ops.bounds.minY - Bounds minX value (optional)
	 *		ops.bounds.maxX - Bounds minX value (optional)
	 *		ops.bounds.maxT - Bounds minX value (optional)
	 *
	 *		hbase.zookeeper.quorum - Zookeeper quorum addresses specified as a comma separated list. (optional)
	 *		hbase.zookeeper.port - Zookeeper port. (optional)
	 *		hbase.master - HBase master address. (optional)
	 *
	 *		ops.aggregationType - Aggregation operation applied during tiling - allowed values are "count",
	 *													 "sum", "mean", "max"
	 *
	 *		ops.valueColumn - Colspec denoting data column to use as aggregation source.	Not required when type is
	 *
	 *		Tiling task consumes a TilingTaskParameters object that
	 *		is populated via an argument map.	The argument map passed to this function will be used for that
	 *		purpose, so all arguments required by that object should be set.
	 */
	def parseCrossplotHeatmapOp(args: Map[String, String]) = {
		logger.debug(s"Parsing fileHeatmapOp with args $args")
		val argParser = KeyValuePassthrough(args)

		val heatmapParams = parseHeatMapOpImpl(args, argParser)

		val parsedBounds = List(argParser.getDoubleOption("ops.bounds.minX", "Bounds min X", None),
														argParser.getDoubleOption("ops.bounds.minY", "Bounds min Y", None),
														argParser.getDoubleOption("ops.bounds.maxX", "Bounds max X", None),
														argParser.getDoubleOption("ops.bounds.maxY", "Bounds max Y", None))
		val aoiBounds = if (parsedBounds.flatten.length == parsedBounds.length) {
			Some(Bounds(parsedBounds.head.get, parsedBounds(1).get, parsedBounds(1).get, parsedBounds(3).get))
		} else {
			None
		}
		crossplotHeatMapOp(heatmapParams._1, heatmapParams._2, heatmapParams._3, heatmapParams._4, heatmapParams._5,
											 heatmapParams._6, heatmapParams._7, aoiBounds)(_)
	}

	private def parseHeatMapOpImpl(args: Map[String, String], argParser: KeyValueArgumentSource) = {
		val xColSpec = argParser.getString("ops.xColumn", "Colspec denoting data x column")
		val yColSpec = argParser.getString("ops.yColumn", "Colspec denoting data y column")
		val operationType = argParser.getString("ops.aggregationType", "")
		val valueColSpec = argParser.getStringOption("ops.valueColumn", "", None)
		val valueColType = argParser.getStringOption("ops.valueType", "", None)

		val parsedArgs = List(argParser.getStringOption("hbase.zookeeper.quorum", "Zookeeper quorum addresses", None),
													argParser.getStringOption("hbase.zookeeper.port", "Zookeeper port", None),
													argParser.getStringOption("hbase.master", "HBase master address", None))
		val hbaseArgs = if (parsedArgs.flatten.length == parsedArgs.length) {
			Some(HBaseParameters(parsedArgs.head.get, parsedArgs(1).get, parsedArgs(2).get))
		} else {
			None
		}

		val operationEnum = OperationType.withName(operationType.toUpperCase)

		val taskParametersFactory = new TilingTaskParametersFactory(null, List("ops"))
		taskParametersFactory.readConfiguration(JsonUtilities.mapToJSON(args))
		val taskParameters = taskParametersFactory.produce(classOf[TilingTaskParameters])

		(xColSpec, yColSpec, taskParameters, hbaseArgs, operationEnum, valueColSpec, valueColType)
	}

	/**
	 * Parse args for basic line tile generation operation that writes to HDFS, and return an instance of that
	 * operation with those arguments applied.	If HBase properties are not set, file based IO is used.
	 *
	 * Arguments:
	 *		hbase.zookeeper.quorum - Zookeeper quorum addresses specified as a comma separated list. (optional)
	 *		hbase.zookeeper.port - Zookeeper port. (optional)
	 *		hbase.master - HBase master address. (optional)
	 *		ops.xColumn - Colspec denoting data x column
	 *		ops.yColumn - Colspec denoting data y column
	 *		ops.aggregationType - Aggregation operation applied during tiling - allowed values are "count",
	 *													"sum", "mean", "max"
	 *		ops.valueColumn - Colspec denoting data column to use as aggregation source.	Not required when type is
	 *
	 *		Tiling task consumes a TilingTaskParameters object that
	 *		is populated via an argument map.	The argument map passed to this function will be used for that
	 *		purpose, so all arguments required by that object should be set.
	 */
	def parseGeoSegmentTilingOp(args: Map[String, String]) = {
		logger.debug(s"Parsing hbaseSegmentTilingOp with args $args")
		val argParser = KeyValuePassthrough(args)
		val params = parseSegmentTilingOpImpl(args, argParser)
		geoSegmentTilingOp(params._1, params._2, params._3, params._4, params._5,
											 params._6, params._7, params._8, params._9, params._10,
											 params._11, params._12, params._13)(_)
		}

		private def parseSegmentTilingOpImpl(args: Map[String, String], argParser: KeyValueArgumentSource) = {
			val x1ColSpec = argParser.getString("ops.x1Column", "Colspec denoting data x column for segment origins")
			val y1ColSpec = argParser.getString("ops.y1Column", "Colspec denoting data y column for segment origins")
			val x2ColSpec = argParser.getString("ops.x2Column", "Colspec denoting data x column for segment destinations")
			val y2ColSpec = argParser.getString("ops.y2Column", "Colspec denoting data y column for segmeng destinations")
			val operationType = argParser.getString("ops.aggregationType", "")
			val valueColSpec = argParser.getStringOption("ops.valueColumn", "", None)
			val valueColType = argParser.getStringOption("ops.valueType", "", None)
			val lineType = argParser.getStringOption("ops.lineType", "The line type to use", None)
      val lineTypeEnum = LineDrawingType.valueOf(lineType.getOrElse(LineDrawingType.Lines.toString))
			val minimumSegmentLength = argParser.getIntOption("", "The minimum length of a segment (in bins) before it is drawn", Some(4))
			val maximumSegmentLength = argParser.getIntOption("", "The maximum length of a segment (in bins) before it is no longer drawn", Some(1024))
			val maximumLeaderLength = argParser.getIntOption("", "The maximum number of bins to draw at each end of a segment.	Bins farther than this distance from both endpoints will be ignored.", Some(1024))

			val parsedArgs = List(argParser.getStringOption("hbase.zookeeper.quorum", "Zookeeper quorum addresses", None),
														argParser.getStringOption("hbase.zookeeper.port", "Zookeeper port", None),
														argParser.getStringOption("hbase.master", "HBase master address", None))
			val hbaseArgs = if (parsedArgs.flatten.length == parsedArgs.length) {
				Some(HBaseParameters(parsedArgs.head.get, parsedArgs(1).get, parsedArgs(2).get))
			} else {
				None
			}

			val operationEnum = OperationType.withName(operationType.toUpperCase)
			val taskParametersFactory = new TilingTaskParametersFactory(null, List("ops"))
			taskParametersFactory.readConfiguration(JsonUtilities.mapToJSON(args))
			val taskParameters = taskParametersFactory.produce(classOf[TilingTaskParameters])

			(x1ColSpec, y1ColSpec, x2ColSpec, y2ColSpec, taskParameters, hbaseArgs, operationEnum, valueColSpec, valueColType, Some(lineTypeEnum), minimumSegmentLength, maximumSegmentLength, maximumLeaderLength)
		}

	}
