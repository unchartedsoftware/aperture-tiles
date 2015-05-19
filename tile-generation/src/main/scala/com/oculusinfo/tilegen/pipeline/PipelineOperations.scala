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



import java.text.SimpleDateFormat
import java.sql.Date
import java.util.concurrent.atomic.AtomicInteger

import scala.util.matching.Regex

import grizzled.slf4j.Logger

import org.apache.avro.file.CodecFactory

import org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute
import org.apache.spark.sql.{SQLContext, Column, DataFrame}
import org.apache.spark.sql.functions._

import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.WebMercatorTilePyramid

import com.oculusinfo.tilegen.datasets.{TilingTask, TilingTaskParameters, CSVReader}
import com.oculusinfo.tilegen.tiling.{TileIO, LocalTileIO, HBaseTileIO}
import com.oculusinfo.tilegen.util.KeyValueArgumentSource



/**
 * Provides operations that can be bound into a TilePipeline
 * tree.
 */
object PipelineOperations {
	import OperationType._
	import com.oculusinfo.tilegen.datasets.SchemaTypeUtilities._

	protected var tableIdCount = new AtomicInteger(0)

	/**
	 * KeyValueArgumentSource implementation that passes the supplied map through.
	 *
	 * @param args Wrapped argument map.
	 */
	case class KeyValuePassthrough(args: Map[String, String]) extends KeyValueArgumentSource {
		def properties = args.map(entry => entry._1 -> entry._2)
	}

	/**
	 * Load data into the pipeline directly from a schema rdd
	 *
	 * @param rdd The DataFrame containing the data
	 * @param tableName The name of the table the rdd has been assigned in the SQLContext, if any.
	 */
	def loadRDDOp (rdd: DataFrame, tableName: Option[String] = None)(data: PipelineData): PipelineData = {
		PipelineData(rdd.sqlContext, rdd, tableName)
	}

	private def coalesce (sqlc: SQLContext, dataFrame: DataFrame, partitions: Option[Int]): DataFrame = {
		partitions.map{n =>
			val baseRDD = dataFrame.queryExecution.toRdd
			val curPartitions = baseRDD.partitions.size
			// if we're increasing the number of partitions, just repartition as per normal
			// If we're reducing them, copy data and coalesce, so as to avoid a shuffle.
			if (n > curPartitions) dataFrame.repartition(n)
			else sqlc.createDataFrame(baseRDD.map(_.copy()).coalesce(n), dataFrame.schema)
		}.getOrElse(dataFrame)
	}

	/**
	 * Load data from a JSON file.  The schema is derived from the first json record.
	 *
	 * @param path Valid HDFS path to the data.
	 * @param partitions Number of partitions to load data into.
	 * @param data Not used.
	 * @return PipelineData with a schema RDD populated from the JSON file.
	 */
	def loadJsonDataOp(path: String, partitions: Option[Int] = None)(data: PipelineData): PipelineData = {
		val context = data.sqlContext
		val srdd = coalesce(context, context.jsonFile(path), partitions)
		PipelineData(data.sqlContext, srdd)
	}

	/**
	 * Load data from a CSV file using CSVReader.
	 * The arguments map will be passed through to that object, so all arguments required
	 * for its configuration should be set in the map.
	 *
	 * @param path HDSF path to the data object.
	 * @param argumentSource Arguments to forward to the CSVReader.
	 * @param partitions Number of partitions to load data into.
	 * @param data Not used.
	 * @return PipelineData with a schema RDD populated from the CSV file.
	 */
	def loadCsvDataOp(path: String, argumentSource: KeyValueArgumentSource, partitions: Option[Int] = None)
	                 (data: PipelineData): PipelineData = {
		val context = data.sqlContext
		val reader = new CSVReader(context, path, argumentSource)
		val dataFrame = coalesce(context, reader.asDataFrame, partitions)
		PipelineData(reader.sqlc, dataFrame)
	}

	/**
	 * Pipeline op to filter records to a specific date range.
	 *
	 * @param minDate Start date for the range.
	 * @param maxDate End date for the range.
	 * @param format Date parsing string, expressed according to java.text.SimpleDateFormat.
	 * @param timeCol Column spec denoting name of time column in input schema RDD.
	 * @param input Input pipeline data to filter.
	 * @return Transformed pipeline data, where records outside the specified time range have been removed.
	 */
	def dateFilterOp(minDate: Date, maxDate: Date, format: String, timeCol: String)(input: PipelineData): PipelineData = {
		val formatter = new SimpleDateFormat(format)
		val minTime = minDate.getTime
		val maxTime = maxDate.getTime

		val filterFcn = udf((value: String) => {
			val time = formatter.parse(value).getTime
			minTime <= time && time <= maxTime
		})
		PipelineData(input.sqlContext, input.srdd.filter(filterFcn(new Column(timeCol))))
	}

	/**
	 * Pipeline op to filter records to a specific date range.
	 *
	 * @param minDate Start date for the range, expressed in a format parsable by java.text.SimpleDateFormat.
	 * @param maxDate End date for the range, expressed in a format parsable by java.text.SimpleDateFormat.
	 * @param format Date parsing string, expressed according to java.text.SimpleDateFormat.
	 * @param timeCol Column spec denoting name of time column in input schema RDD.
	 * @param input Input pipeline data to filter.
	 * @return Transformed pipeline data, where records outside the specified time range have been removed.
	 */
	def dateFilterOp(minDate: String, maxDate: String, format: String, timeCol: String)(input: PipelineData): PipelineData = {
		val formatter = new SimpleDateFormat(format)
		val minTime = new Date(formatter.parse(minDate).getTime)
		val maxTime = new Date(formatter.parse(maxDate).getTime)
		dateFilterOp(minTime, maxTime, format, timeCol)(input)
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
	 * A very specific filter to filter geographic data to only that data that projects into the
	 * standard tile set under a mercator projection
	 *
	 * @param latCol The column of data containing the longitude value
	 */
	def mercatorFilterOp (latCol: String)(input: PipelineData): PipelineData = {
		val inputTable = getOrGenTableName(input, "mercator_filter_op")
		val pyramid = new WebMercatorTilePyramid
		val area = pyramid.getTileBounds(new TileIndex(0, 0, 0))
		val selectStatement = "SELECT * FROM "+inputTable+" WHERE "+latCol+" >= "+area.getMinY+" AND "+latCol+" < "+area.getMaxY
		val outputTable = input.sqlContext.sql(selectStatement)
		PipelineData(input.sqlContext, outputTable)
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
		val test: Column = colSpecs.zip(min.zip(max)).map{case (name, (mn, mx)) =>
			val col = new Column(name)
			val result: Column = (col >= mn && col <= mx)
			if (exclude) result.unary_! else result
		}.reduce(_ && _)
		PipelineData(input.sqlContext, input.srdd.filter(test))
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
		val test: Column = colSpecs.zip(min.zip(max)).map{case (name, (mn, mx)) =>
			val col = new Column(name)
			val result: Column = (col >= mn && col <= mx)
			if (exclude) result.unary_! else result
		}.reduce(_ && _)
		PipelineData(input.sqlContext, input.srdd.filter(test))
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
		val regexFcn = udf((value: String) =>
			value match {
				case regex(_*) => if (exclude) false else true
				case _ => if (exclude) true else false
			})
		PipelineData(input.sqlContext, input.srdd.filter(regexFcn(new Column(colSpec))))
	}

	/**
	 * Operation to create a new schema RDD from selected columns.
	 *
	 * @param colSpecs Sequence of column specs denoting the columns to select.
	 * @param input Pipeline data to select columns from.
	 * @return Pipeline data containing a schema RDD with only the selected columns.
	 */
	def columnSelectOp(colSpecs: Seq[String])(input: PipelineData) = {
		PipelineData(input.sqlContext, input.srdd.selectExpr(colSpecs:_*))
	}

	/**
	 * A basic heatmap tile generator that writes output to HDFS. Uses
	 * TilingTaskParameters to manage tiling task arguments; the arguments map
	 * will be passed through to that object, so all arguments required for its configuration should be set in the map.
	 *
	 * @param xColSpec Colspec denoting data x column
	 * @param yColSpec Colspec denoting data y column
	 * @param tilingParams Parameters to forward to the tiling task.
	 * @param operation Aggregating operation.  Defaults to type Count if unspecified.
	 * @param valueColSpec Colspec denoting the value column to use for the aggregating operation.  None
	 *                     if the default type of Count is used.
	 * @param valueColType Type to interpret colspec value as - float, double, int, long.  None if the default
	 *                     type of count is used for the operation.
	 * @param hbaseParameters HBase connection configuration.
	 * @param input Pipeline data to tile.
	 * @return Unmodified input data.
	 */
	def geoHeatMapOp(xColSpec: String,
	                 yColSpec: String,
	                 tilingParams: TilingTaskParameters,
	                 hbaseParameters: Option[HBaseParameters],
	                 operation: OperationType = COUNT,
	                 valueColSpec: Option[String] = None,
	                 valueColType: Option[String] = None)
	                (input: PipelineData) = {
		val tileIO = hbaseParameters match {
			case Some(p) => new HBaseTileIO(p.zookeeperQuorum, p.zookeeperPort, p.hbaseMaster)
			case None => new LocalTileIO("avro")
		}
		val properties = Map("oculus.binning.projection.type" -> "webmercator")

		heatMapOpImpl(xColSpec, yColSpec, operation, valueColSpec, valueColType, tilingParams, tileIO, properties)(input)
	}

	/**
	 * A basic heatmap tile generator that writes output to the local file system. Uses
	 * TilingTaskParameters to manage tiling task arguments; the arguments map
	 * will be passed through to that object, so all arguments required for its configuration should be set in the map.
	 *
	 * @param xColSpec Colspec denoting data x column
	 * @param yColSpec Colspec denoting data y column
	 * @param tilingParams Parameters to forward to the tiling task.
	 * @param operation Aggregating operation.  Defaults to type Count if unspecified.
	 * @param valueColSpec Colspec denoting the value column to use for the aggregating operation.  None
	 *                     if the default type of Count is used.
	 * @param valueColType Type to interpret colspec value as - float, double, int, long.  None if the default type
	 count is used for the operation.
	 * @param bounds The bounds for the crossplot.  None indicates that bounds will be auto-generated based on input data.
	 * @param input Pipeline data to tile.
	 * @return Unmodified input data.
	 */
	def crossplotHeatMapOp(xColSpec: String,
	                       yColSpec: String,
	                       tilingParams: TilingTaskParameters,
	                       hbaseParameters: Option[HBaseParameters],
	                       operation: OperationType = COUNT,
	                       valueColSpec: Option[String] = None,
	                       valueColType: Option[String] = None,
	                       bounds: Option[Bounds] = None)
	                      (input: PipelineData) = {
		val tileIO = hbaseParameters match {
			case Some(p) => new HBaseTileIO(p.zookeeperQuorum, p.zookeeperPort, p.hbaseMaster)
			case None => new LocalTileIO("avro")
		}


		val properties = Map("oculus.binning.projection.type" -> "areaofinterest")
		val boundsProps = bounds match {
			case Some(b) => Map("oculus.binning.projection.autobounds" -> "false",
			                    "oculus.binning.projection.minX" -> b.minX.toString,
			                    "oculus.binning.projection.minY" -> b.minY.toString,
			                    "oculus.binning.projection.maxX" -> b.maxX.toString,
			                    "oculus.binning.projection.maxY" -> b.maxY.toString)
			case None => Map("oculus.binning.projection.autobounds" -> "true")
		}

		heatMapOpImpl(xColSpec, yColSpec, operation, valueColSpec, valueColType, tilingParams, tileIO,
		              properties ++ boundsProps)(input)
	}

	private def heatMapOpImpl(xColSpec: String,
	                          yColSpec: String,
	                          operation: OperationType,
	                          valueColSpec: Option[String],
	                          valueColType: Option[String],
	                          taskParameters: TilingTaskParameters,
	                          tileIO: TileIO,
	                          properties: Map[String, String])
	                         (input: PipelineData) = {
		// Populate baseline args
		val args = Map(
			"oculus.binning.name" -> taskParameters.name,
			"oculus.binning.description" -> taskParameters.description,
			"oculus.binning.tileWidth" -> taskParameters.tileWidth.toString,
			"oculus.binning.tileHeight" -> taskParameters.tileHeight.toString,
			"oculus.binning.index.type" -> "cartesian",
			"oculus.binning.index.field.0" -> xColSpec,
			"oculus.binning.index.field.1" -> yColSpec)

		val valueProps = operation match {
			case SUM | MAX | MIN | MEAN =>
				Map("oculus.binning.value.type" -> "field",
				    "oculus.binning.value.field" -> valueColSpec.get,
				    "oculus.binning.value.valueType" -> valueColType.get,
				    "oculus.binning.value.aggregation" -> operation.toString.toLowerCase,
				    "oculus.binning.value.serializer" -> s"[${valueColType.get}]-a")
			case _ =>
				Map("oculus.binning.value.type" -> "count",
				    "oculus.binning.value.valueType" -> "int",
				    "oculus.binning.value.serializer" -> "[int]-a")
		}

		// Parse bounds and level args
		val levelsProps = createLevelsProps("oculus.binning", taskParameters.levels)

		val tableName = PipelineOperations.getOrGenTableName(input, "heatmap_op")

		val tilingTask = TilingTask(input.sqlContext, tableName, args ++ levelsProps ++ valueProps ++ properties)
		tilingTask.doTiling(tileIO)

		PipelineData(input.sqlContext, input.srdd, Option(tableName))
	}

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

	/**
	 * Creates a map of levels properties that can be concatenated to another properties map
	 */
	def createLevelsProps(path: String, levels: Iterable[Seq[Int]]) = {
		levels.zipWithIndex
			.map(l => (s"$path.levels.${l._2}", l._1.mkString(",")))
			.toMap
	}

	// Implicit for converting a scala map to a java properties object
	implicit def map2Properties(map: Map[String,String]): java.util.Properties = {
		(new java.util.Properties /: map) {case (props, (k,v)) => props.put(k,v); props}
	}
}

/**
 * HBase connection parameters
 *
 * @param zookeeperQuorum Zookeeper quorum addresses specified as a comma separated list.
 * @param zookeeperPort Zookeeper port.
 * @param hbaseMaster HBase master address
 */
case class HBaseParameters(zookeeperQuorum: String, zookeeperPort: String, hbaseMaster: String)

/**
 * Area of interest region
 */
case class Bounds(minX: Double, minY: Double, maxX: Double, maxY: Double)

/**
 * Supported heatmap aggregation types.  Count assigns a value of 1 to for each record and sums,
 * Sum extracts a value from each record and sums, Max/Min extracts a value from each record and takes
 * the max/min, mean extracts a value and computes a mean.
 */
object OperationType extends Enumeration {
	type OperationType = Value
	val COUNT, SUM, MIN, MAX, MEAN = Value
}


