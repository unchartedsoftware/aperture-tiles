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



import java.io.{OutputStream, OutputStreamWriter, BufferedWriter, FileNotFoundException}
import java.net.URI
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Calendar, GregorianCalendar, Date, TimeZone}

import com.oculusinfo.binning.{BinIndex, TileIndex}
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.tilegen.datasets.ErrorAccumulator.{ErrorCollector, ErrorCollectorAccumulable}
import com.oculusinfo.tilegen.datasets._
import com.oculusinfo.tilegen.datasets.SchemaTypeUtilities._
import com.oculusinfo.tilegen.tiling._
import com.oculusinfo.tilegen.util.{ExtendedNumeric, KeyValueArgumentSource, HdfsFileManager, HdfsSession, HdfsFile}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.compress.CompressionCodecFactory
import org.apache.hadoop.io.compress.GzipCodec
import org.apache.spark.TaskContext
import org.apache.spark.sql.{Column, DataFrame, SQLContext, SaveMode}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StructType, DataType, IntegerType, TimestampType}
import org.apache.spark.rdd.RDD

import scala.collection.mutable.{Map => MutableMap, ListBuffer}
import org.joda.time.base.BaseSingleFieldPeriod
import org.joda.time.{Interval, PeriodType, DateTime, DurationFieldType}

import org.json.{JSONObject, JSONArray}
import grizzled.slf4j.Logger

import scala.reflect.ClassTag

/**
 * Provides operations that can be bound into a TilePipeline
 * tree.
 */
object PipelineOperations {

	import OperationType._

	private val logger = Logger("PipelineOperations")

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

	def coalesce (sqlc: SQLContext, dataFrame: DataFrame, partitions: Option[Int]): DataFrame = {
		partitions.map{n =>
			val baseRDD = dataFrame.rdd
			val curPartitions = baseRDD.partitions.size
			// if we're increasing the number of partitions, just repartition as per normal
			// If we're reducing them, copy data and coalesce, so as to avoid a shuffle.
			if (n > curPartitions) dataFrame.repartition(n)
			else sqlc.createDataFrame(baseRDD.map(_.copy()).coalesce(n), dataFrame.schema)
		}.getOrElse(dataFrame)
	}

	/**
	 * Load data from a JSON file.	The schema is derived from the first json record.
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
	 * Load data from rows of JSON.
	 *
	 * @param records An RDD containing lines of text (JSON records).
	 * @param partitions Number of partitions to load data into.
	 * @param schema The schema to use for reading in the data.
	 * @param schemaInferSamplingRatio Sampling ratio used to infer the data schema. Used if schema is not present.
	 * @param data Used for the SQL context only. There should be no data at this stage.
	 * @return PipelineData with a schema RDD populated from the CSV file.
	 */
	def loadJsonDataOp(records: RDD[String],
		                 partitions: Option[Int],
		                 schema: Option[StructType],
		                 schemaInferSamplingRatio: Option[Double])(data: PipelineData): PipelineData = {
		if (logger.isDebugEnabled)
			logger.debug("loadJsonDataOp: Enter with " + records.count + " strings in RDD")
		val context = data.sqlContext
		val jsonRdd = if (schema != None)
				context.jsonRDD(records, schema.get)
			else if (schemaInferSamplingRatio != None)
				context.jsonRDD(records, schemaInferSamplingRatio.get)
			else
				context.jsonRDD(records)
		val dataFrame = coalesce(context, jsonRdd, partitions)
		PipelineData(context, dataFrame)
	}

	/**
	 * Load data from Parquet file(s)
	 *
	 * @param path Valid HDFS path to the data.
	 * @param partitions Number of partitions to load data into.
	 * @param data Not used.
	 * @return PipelineData with a schema RDD populated from the Parquet file.
	 */
	def loadParquetDataOp(path: String, partitions: Option[Int] = None)(data: PipelineData): PipelineData = {
		val context = data.sqlContext
		val srdd = coalesce(context, context.parquetFile(path), partitions)
		PipelineData(data.sqlContext, srdd)
	}

	/**
	 * Load data from Parquet file(s) that are in directories corresponding to the specified
	 * date range.
	 *
	 * @param hdfsUri HDFS URI
	 * @param path Valid HDFS top-level directory to the data.
	 * @param startDate The earliest date to consider, inclusive
	 * @param endDate The latest date to consider, inclusive
	 * @param partitions Number of partitions to load data into.
	 * @param data Not used.
	 * @return PipelineData with a schema RDD populated from the Parquet file.
	 */
	def loadParquetDataByDateOp(hdfsUri: String, path: String, startDate: Date, endDate: Date, partitions: Option[Int] = None)
		(data: PipelineData): PipelineData = {

		val fname = "loadParquetDataByDateOp: "
		var dirList = Array[String]()

		// Create HDFS file system
		val uri = new URI(hdfsUri)
		val hdfs = FileSystem.get(uri, new Configuration())

		// Convert start and end dates to a more appropriate format
		val loopDate = new GregorianCalendar()
		loopDate.setTimeZone(TimeZone.getTimeZone("GMT"))
		loopDate.setTime(startDate)
		val endCalendar = new GregorianCalendar()
		loopDate.setTimeZone(TimeZone.getTimeZone("GMT"))
		endCalendar.setTime(endDate)

		// Loop over all dates within range
		while (loopDate.compareTo(endCalendar) <= 0) {

			// Construct the directory name based on the date
			val dateStr = loopDate.get(Calendar.YEAR) +
				"/" + Integer.toString(loopDate.get(Calendar.MONTH) + 1) +
				"/" + loopDate.get(Calendar.DAY_OF_MONTH)
			val directoryName = path + "/" + dateStr

			// Append directory name to the directory list if the directory exists
			val pathObj = new Path(directoryName)
			if (hdfs.isDirectory(pathObj))
				dirList = dirList :+ directoryName
			else
				logger.warn(fname + "Directory for date " + loopDate + " in specified date range does not exist: " + directoryName)

			// Move to the next day
			loopDate.add(Calendar.DATE, 1)

		} // while

		// Read the data from the list of comma separated files/directories
		logger.debug(fname + "Attempting to read data from " + dirList.mkString(","))
		val resultDataFrame = data.sqlContext.parquetFile(dirList: _*)
		if (logger.isDebugEnabled)
			logger.debug(fname + "Read in " + resultDataFrame.count + " records")

		// Coalesce and return the result
		val context = data.sqlContext
		val srdd = coalesce(data.sqlContext, resultDataFrame, partitions)
		PipelineData(data.sqlContext, srdd)
	}

	/**
	 * Writes data by date-specific directories
	 * @param dayOfMonthColName The name of the day-of-month column in the input DataFrame
	 * @param monthColName The name of the month column in the input DataFrame
	 * @param yearColName The name of the year column in the input DataFrame
	 * @param writeFields The columns to write. Empty set, if all columns should be written.
	 *                    If non-empty, the excludeFields set must be empty.
	 * @param excludeFields The columns to exclude. Empty set, if no columns should be excluded.
	 *                      If non-empty, the writeFields set must be empty.
	 * @param applyFiltersToPipelineDataSet Flag indicating whether to apply the write/exclude
	 *                                      filters to the resultant pipeline dataset
	 * @param writeOp User-defined write function to write data. Input to function is a
	 *                DataFrame that contains one day of data with the specified
	 *                writeFields/excludeFields filters applied,
	 *                followed by day-of-month, month (1-indexed, not 0-indexed), and year integers.
	 * @param input The pipeline data
	 * @return
	 */
	def writeDataByDateOp(description: String,
		                    dayOfMonthColName: String,
		                    monthColName: String,
		                    yearColName: String,
		                    writeOp: (DataFrame, Int /*day*/, Int /*month*/, Int /*year*/) => Unit,
		                    writeFields: Set[String] = Set(),
												excludeFields: Set[String] = Set(),
		                    applyFiltersToPipelineDataSet: Boolean = false)
		                   (input: PipelineData) : PipelineData = {

		val fname = "writeDataByDateOp(" + description + "): "

		// Validate input
		if (writeFields == null)
			throw new IllegalArgumentException("writeFields argument must not be null")
		if (excludeFields == null)
			throw new IllegalArgumentException("excludeFields argument must not be null")
		if (!writeFields.isEmpty && !excludeFields.isEmpty)
			throw new IllegalArgumentException("Only one of writeFields or excludeFields must be non-empty")

		// Only write if there are records
		if (input.srdd.take(1).length > 0) {

			// Create a filter for all desired columns
			var includeFilter: Seq[Column] = Seq()
			if (!writeFields.isEmpty) {
				for (field <- input.srdd.schema.fields)
					if (writeFields.contains(field.name))
						includeFilter = includeFilter :+ new Column(field.name)
			}
			else if (!excludeFields.isEmpty) {
				for (field <- input.srdd.schema.fields)
					if (!excludeFields.contains(field.name))
						includeFilter = includeFilter :+ new Column(field.name)
			}
			logger.debug(fname + "Write fields filter: " + includeFilter.mkString(", "))

			// Get the distinct days in this data set
			val tableName = getOrGenTableName(input, "writeDataByDateOp")
			val selectDistinctDaysSql = "SELECT DISTINCT " +
				dayOfMonthColName + "," + monthColName + "," + yearColName +
				" FROM " + tableName + " WHERE " +
				dayOfMonthColName + " IS NOT NULL AND " +
				monthColName + " IS NOT NULL AND " +
				yearColName + " IS NOT NULL"
			val distinctDays = input.sqlContext.sql(selectDistinctDaysSql).collect()
			logger.debug(fname + "Found " + distinctDays.length + " distinct days")

			// Find all of the records for each unique day and write them to a date-specific folder
			for (loopDate <- distinctDays) {
				// Only look at records that have a proper date
				if (loopDate != null && loopDate.length == 3 &&
					!(loopDate.isNullAt(0) || loopDate.isNullAt(1) || loopDate.isNullAt(2))) {

					val dateStr = loopDate(2) + 								// year
						"/" + (loopDate.getInt(1)+1).toString + 	// month (+1 because Java indexes from 0)
						"/" + loopDate(0)													// day
					logger.debug(fname + "Looping over date " + dateStr)

					// Create the date specific filter
					val selectRowsByDaySql = "SELECT *" +
						" FROM " + tableName + " WHERE " +
						dayOfMonthColName + "=" + loopDate(0) + " AND " +
						monthColName + "=" + loopDate(1) + " AND " +
						yearColName + "=" + loopDate(2)
					logger.debug(fname + "Applying date filter: " + selectRowsByDaySql)
					val dayDataFrame = input.srdd.sqlContext.sql(selectRowsByDaySql)
					if (logger.isTraceEnabled)
						logger.trace(fname + "Unfiltered write data contains " + dayDataFrame.collect().length + " rows")
					logger.debug(fname + "Unfiltered write data contains " + dayDataFrame.schema.fields.length + " columns")

					// Filter for only the desired columns, if a filter was specified
					val filteredDayDataFrame =
						if (includeFilter.isEmpty)
							dayDataFrame
						else
							dayDataFrame.select(includeFilter: _*)
						if (logger.isTraceEnabled)
							logger.trace(fname + "Filtered write data contains " + filteredDayDataFrame.collect().length + " rows")
						logger.debug(fname + "Filtered write data contains " + filteredDayDataFrame.schema.fields.length + " columns")

					// Execute the write function
					logger.debug(fname + "Writing data for date " + dateStr)
					writeOp(filteredDayDataFrame, loopDate.getInt(0), loopDate.getInt(1)+1, loopDate.getInt(2))
					logger.debug(fname + "Finished writing data for date " + dateStr)
				}
			} // foreach

			// Filter the result dataset, if desired
			if (applyFiltersToPipelineDataSet && !includeFilter.isEmpty) {
				val filteredDataSet = input.srdd.select(includeFilter: _*)
				if (logger.isTraceEnabled)
					logger.trace(fname + "Filtered pipeline data contains " + filteredDataSet.collect().length + " rows")
				logger.debug(fname + "Filtered pipeline data contains " + filteredDataSet.schema.fields.length + " columns")
				PipelineData(input.sqlContext, filteredDataSet)
			}
			else {
				if (logger.isTraceEnabled)
					logger.trace(fname + "Unfiltered pipeline data contains " + input.srdd.collect().length + " rows")
				logger.debug(fname + "Unfiltered pipeline data contains " + input.srdd.schema.fields.length + " columns")
				input
			}
		} // if records
		else {
			// No records. Return the original input
			logger.debug(fname + "No records. Just returning original pipeline data")
			input
		}
	}

	/*
	 * Writes the date-specific DataFrame as parquet
	 *
	 * @param basePath Destination base path
	 * @param partitions Number of partitions to use to coalesce the data
	 *
	 * @param dayDataFrame Date-specific data
	 * @param dayOfMonth The day-of-month
	 * @param month The month
	 * @param year The year
	 */
	def writeDateSpecificParquet(basePath: String,
		                           partitions: Option[Int])
		                          (dayDataFrame: DataFrame,
		                           dayOfMonth: Int,
		                           month: Int,
		                           year: Int) : Unit = {

		val fname = "writeDateSpecificParquet: "

		// Coalesce the data to be written to avoid writing lots of small files
		val coalescedDayDataFrame = coalesce(dayDataFrame.sqlContext, dayDataFrame, partitions)

		// Write the records for this date to a date-specific folder (year-month-day)
		// DataFrame.saveAsParquetFile doesn't allow specifying SaveMode.
		// save() is an experimental API in Spark 1.3.0.
		// save() is deprecated in Spark 1.4.0 and replaced by another experimental API: write().parquet()[.mode()]
		val path = basePath + "/" + year + "/" + month.toString + "/" + dayOfMonth.toString
		logger.debug(fname + "Coalesced data to " + partitions + " partitions. Writing data to: " + path)
		coalescedDayDataFrame.save(path, SaveMode.Append)
	}

	/**
	 * Get the field index for the specified field name.
	 * Only applies to the first level of columns, not nested columns.
	 *
	 * @param input The data
	 * @param name The name of the field
	 * @return The index of the field.
	 * @throws Exception If field doesn't exist
	 */
	def getFieldIndexByName(input: PipelineData, name: String) : Integer = {
		var fieldIndex = -1
		var loopIndex = 0
		while ((fieldIndex == -1) && (loopIndex < input.srdd.schema.fieldNames.length)) {
			if (input.srdd.schema.fieldNames(loopIndex) == name)
				fieldIndex = loopIndex
			loopIndex += 1
		}
		if (fieldIndex == -1)
			throw new Exception("Could not find field index. Field " + name + " does not exist in schema: " +
				input.srdd.schema.fieldNames.mkString(","))
		fieldIndex
	}

	/**
	 * Writes data by date-specific directories
	 * @param dayOfMonthColName The name of the day-of-month column in the input DataFrame
	 * @param monthColName The name of the month column in the input DataFrame
	 * @param yearColName The name of the year column in the input DataFrame
	 * @param writeFieldName The column to write.
	 * @param hdfsUri The HDFS URI
	 * @param destination The base desination directory
	 * @param input The pipeline data
	 * @return
	 */
	def writeDataByDateToHdfsOp(dayOfMonthColName: String,
		monthColName: String,
		yearColName: String,
		writeFieldName: String,
		hdfsUri: String,
		destination: String,
		partitions: Option[Int] = None)
		(input: PipelineData) : PipelineData = {

		// Because we'll be operating on rows, need to find what these columns are by index rather than by name
		val sc = input.sqlContext.sparkContext
		val dayOfMonthColIndex = sc.broadcast(getFieldIndexByName(input, dayOfMonthColName))
		val monthColIndex = sc.broadcast(getFieldIndexByName(input, monthColName))
		val yearColIndex = sc.broadcast(getFieldIndexByName(input, yearColName))
		val writeFieldColIndex = sc.broadcast(getFieldIndexByName(input, writeFieldName))

		// Coalesce the data to avoid writing a lot of small files
		val coalescedDataFrame = coalesce(input.sqlContext, input.srdd, partitions)

		coalescedDataFrame.foreachPartition(partition => {
			// Create a session for each partition, so that we can keep files open for the duration of the session
			var session : HdfsSession = null
			try {
				session = HdfsFileManager.getSession(None, hdfsUri)

				// Write each row to the appropriate file
				partition.foreach(row => {
					val dateDir = destination + "/" + row(yearColIndex.value) +
						"/" + (row.getInt(monthColIndex.value) + 1).toString +
						"/" + row(dayOfMonthColIndex.value)
					val fileName = dateDir + "/part-" + TaskContext.get().partitionId() + session.codec.getDefaultExtension()
					val file = session.getFile(fileName)
					file.writer.write(row(writeFieldColIndex.value).asInstanceOf[String])
					file.writer.newLine()
				})
			} finally {
				HdfsFileManager.endSession(session)
			} // try
		}) //foreachPartition

		// Return the coalesced input
		new PipelineData(input.sqlContext, coalescedDataFrame)
	}

	/**
	 * Load data from a CSV file using CSVReader.
	 * The arguments map will be passed through to that object, so all arguments required
	 * for its configuration should be set in the map.
	 *
	 * To get a report on rejected lines supply this argument in your script "-errorLog <output-stream>"
	 * Permissible output streams: "stdout"
	 *														 "stderr"
	 *														 "<file-path>"
	 *
	 * @param path HDSF path to the data object.
	 * @param argumentSource Arguments to forward to the CSVReader.
	 * @param partitions Number of partitions to load data into.
	 * @param errorLog The filestream to output the results of the error accumulator to.
	 * @return PipelineData with a schema RDD populated from the CSV file.
	 */
	def loadCsvDataOp(path: String, argumentSource: KeyValueArgumentSource, partitions: Option[Int] = None, errorLog: Option[String] = None)
									 (data: PipelineData): PipelineData = {
		val context = data.sqlContext
		val reader = new CSVReader(context, path, argumentSource)
		val dataFrame = coalesce(context, reader.asDataFrame, partitions)

		val outStream: Option[OutputStream] = errorLog match {
			case Some("stdout") => Some(System.out)
			case Some("stderr") => Some(System.err)
			case Some(name) => Some(new java.io.FileOutputStream(new java.io.File(name)))
			case _ => None
		}

		if (outStream.isDefined) {
      val accumulator = new ErrorCollectorAccumulable(ListBuffer(new ErrorCollector))

      // add lines into error accumulator
      reader.readErrors.foreach(r => accumulator.add(r))

      accumulator.value.foreach{ e =>
				for ((k,v) <- e.getError) printf("%s -> %s\n", k, v)
				outStream.get.flush()
			}
		}

		PipelineData(reader.sqlc, dataFrame)
	}

	/**
	 * Load data from lines of text using CSVReader.
	 * The arguments map will be passed through to that object, so all arguments required
	 * for its configuration should be set in the map.
	 *
	 * @param records An RDD containing lines of text (CSV records).
	 * @param argumentSource Arguments to forward to the CSVReader.
	 * @param partitions Number of partitions to load data into.
	 * @param data Not used.
	 * @return PipelineData with a schema RDD populated from the CSV file.
	 */
	def loadCsvDataOp(records: RDD[String], argumentSource: KeyValueArgumentSource, partitions: Option[Int])(data: PipelineData): PipelineData = {
		val context = data.sqlContext
		val reader = new CSVReader(context, records, argumentSource)
		val dataFrame = coalesce(context, reader.asDataFrame, partitions)
		PipelineData(reader.sqlc, dataFrame)
	}

	/**
	 * Check whether this JSON array is empty / all elements are null.
	 * If any of the elements in the array are JSON objects or are comprised of JSON Objects:
	 * - This will strip those JSON objects of any null fields, modifying the content of the array.
	 * - If all of these JSON objects have all null fields, the array is considered to be null as well
	 *
	 * @param jsonArray The JSON array
	 * @return True if the array is empty or all elements are null, false otherwise.
	 */
	private def purgeNullFieldsFromJsonArray(jsonArray: JSONArray): Boolean = {
		// Return true if it's an empty array
		if (jsonArray.length == 0)
			true
		else {
			// Determine if all of the fields of this array are null
			var isAllNull = true
			var index = 0
			while (isAllNull && index < jsonArray.length) {
				if (!jsonArray.isNull(index)) {
					var element = jsonArray.get(index)

					// If the element is a JSON object, check whether this
					// JSON object is null
					if (element.isInstanceOf[JSONObject]) {
						val isNull = purgeNullFieldsFromJsonObject(element.asInstanceOf[JSONObject])
						if (!isNull)
							isAllNull = false
					}
					// If the element is a nested JSON array, check whether
					// this JSON array is null / empty
					else if (element.isInstanceOf[JSONArray]) {
						val isNull = purgeNullFieldsFromJsonArray(element.asInstanceOf[JSONArray])
						if (!isNull)
							isAllNull = false
					}
					else {
						// Else it is a primitive type
						isAllNull = false
					}
				}
				index += 1
			} // while

			// Return whether all of the fields of this array are null
			isAllNull
		}
	}

	/**
	 * Purge null fields from the JSON Object.
	 * This method will modify the input object.
	 *
	 * @param jsonObject The JSON object
	 * @return True if all of the fields in the object are null, false otherwise.
	 */
	private def purgeNullFieldsFromJsonObject(jsonObject: JSONObject): Boolean = {
		// Iterate all of the keys of this JSONObject and
		// remove keys will null values
		var keyIterator = jsonObject.keys()
		while (keyIterator.hasNext()) {

			var shouldRemoveKey = false
			val key: String = keyIterator.next().asInstanceOf[String]
			if (jsonObject.isNull(key)) {
				// If key has a null value, just remove it.
				shouldRemoveKey = true
			}
			else {
				// The value for this key is not null.
				// If the value is a JSONObject, recursively check its fields for
				// null and strip them.
				val child: Object = jsonObject.get(key)
				if (child.isInstanceOf[JSONObject])
					shouldRemoveKey = purgeNullFieldsFromJsonObject(child.asInstanceOf[JSONObject])
				// If the value is a JSONArray, remove it if it's empty or all elements
				// are null. If any elements are JSONObjects or are comprised of
				// JSONObjects, this will strip those of null fields as well
				else if (child.isInstanceOf[JSONArray])
					shouldRemoveKey = purgeNullFieldsFromJsonArray(child.asInstanceOf[JSONArray])
				// Else it is a primitive type. Leave it.
			}

			// Remove key-value if null
			if (shouldRemoveKey) {
				keyIterator.remove()
				jsonObject.remove(key)
			}
		}

		// We have stripped all of the nulls from this JSONObject.
		// If there are no keys left, return true - the object is null.
		// Otherwise, return false.
		!jsonObject.keys().hasNext()
	}

	/**
	 * Purge null fields from the JSON string
	 * Note that we lose the distinction between "null" and "unspecified" by applying this method.
	 * However, this method is required to work around an issue where Spark interprets null fields
	 * as String data type:
	 * https://mail-archives.apache.org/mod_mbox/spark-user/201507.mbox/%3CCABtLTB=P-U8Rrjg8r0YHrSFO+9bgQ6OsSwrkT8v=DBWUep6h4A@mail.gmail.com%3E
	 *
	 * @param jsonString The JSON string
	 * @return The JSON string, stripped of any null fields. Empty string if all fields are null.
	 */
	def purgeNullFieldsFromJsonString(jsonString: String): String = {

		// If the JSON string is null or empty, return empty string
		if (jsonString == null || jsonString.isEmpty)
			return ""

		// Otherwise, do a more thorough inspection on the string
		try {
			val jsonObject = new JSONObject(jsonString)
			val isNull = purgeNullFieldsFromJsonObject(jsonObject)
			if (isNull)
				""
			else
				jsonObject.toString
		} catch {
			case ex : Exception => {
				logger.warn("Exception caught when parsing JSON: " + ex.toString)
				logger.warn("Problematic JSON string: " + jsonString)
				""
			}
		}
	}

	/**
	 * Purge null fields from the JSON strings.
	 * Note that we lose the distinction between "null" and "unspecified" by applying this method.
	 * However, this method is required to work around an issue where Spark interprets null fields
	 * as String data type:
	 * https://mail-archives.apache.org/mod_mbox/spark-user/201507.mbox/%3CCABtLTB=P-U8Rrjg8r0YHrSFO+9bgQ6OsSwrkT8v=DBWUep6h4A@mail.gmail.com%3E
	 *
	 * @param jsonStrings RDD of JSON strings, where each string is a record / JSON Object
	 * @return RDD of JSON strings that have non-null values in all of the specified fields.
	 */
	def purgeNullFieldsFromJson(jsonStrings: RDD[String]): RDD[String] = {
		if (logger.isDebugEnabled)
			logger.debug("purgeNullFieldsFromJson: Enter with " + jsonStrings.count + " strings in RDD")
		jsonStrings.map(x => purgeNullFieldsFromJsonString(x))
	}

	/**
	 * Pipeline op to add a new column to the PipelineData DataFrame
	 * Note that the PipelineData DataFrame and the RDD must have the
	 * same number of partitions.
	 *
	 * @param columnName The name of the column to add
	 * @param columnValues The values of the column to add
	 * @param input Input pipeline data to transform
	 * @return Transformed pipeline data with the new columns and values.
	 */
	def addColumnOp[T: ClassTag] (columnName: String, columnValues: RDD[T])
		              (input: PipelineData): PipelineData = {
		PipelineData(input.sqlContext, SchemaTypeUtilities.zip[T](input.srdd, columnValues, columnName))
	}

	/**
	 * Pipeline op to remove columns from the PipelineData DataFrame
	 *
	 * @param columnNames The names of the columns to remove
	 * @param input Input pipeline data to transform
	 * @return Transformed pipeline data with the specified columns removed.
	 */
	def removeColumnOp (columnNames: Set[String])
		                 (input: PipelineData): PipelineData = {
		PipelineData(input.sqlContext, SchemaTypeUtilities.removeColumns(input.srdd, columnNames))
	}

	/**
	 * Pipeline op to filter records to a specific date range.
	 *
	 * @param minDate Start date for the range.
	 * @param maxDate End date for the range.
	 * @param format Date parsing string, expressed according to java.text.SimpleDateFormat.
	 * @param timeCol Column spec denoting name of time column in input schema RDD.	Column is expected
	 *								to be a string.
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
	 * Pipeline op to filter records to a specific date range.
	 *
	 * @param minDate Start date for the range, expressed in a format parsable by java.text.SimpleDateFormat.
	 * @param maxDate End date for the range, expressed in a format parsable by java.text.SimpleDateFormat.
	 * @param timeCol Column spec denoting name of time column in input DataFrame.	In this case time column
	 *								is expected to store a Date.
	 * @param input Input pipeline data to filter.
	 * @return Transformed pipeline data, where records outside the specified time range have been removed.
	 */
	def dateFilterOp(minDate: Date, maxDate: Date, timeCol: String)(input: PipelineData): PipelineData = {
		val minTime = minDate.getTime
		val maxTime = maxDate.getTime
		val filterFcn = udf((time: Timestamp) => {
													minTime <= time.getTime && time.getTime <= maxTime
												})
		PipelineData(input.sqlContext, input.srdd.filter(filterFcn(new Column(timeCol))))
	}

	/**
	 * Pipeline op to parse a string date into a timestamp
	 * @param stringDateCol The column from which to get the date (as a string)
	 * @param dateCol The column into which to put the date (as a timestamp)
	 * @param format The expected format of the date
	 * @param input Input pipeline data to transform
	 * @return Transformed pipeline data with the new time field column.
	 */
	def parseDateOp (stringDateCol: String, dateCol: String, format: String)(input: PipelineData): PipelineData = {
		val formatter = new SimpleDateFormat(format);
		val fieldExtractor: Array[Any] => Any = row => {
			try {
				val date = formatter.parse(row(0).toString)
				new Timestamp(date.getTime)
			}
			catch {
				case ex : Exception => {
					logger.warn("Exception caught when parsing date: " + ex.toString)
					null
				}
			}
		}
		val output = SchemaTypeUtilities.addColumn(input.srdd, dateCol, TimestampType, fieldExtractor, stringDateCol)
		val filteredOutput = output.filter(new Column(dateCol).isNotNull)
		PipelineData(input.sqlContext, filteredOutput)
	}

	/**
	 * Filter out rows that are null in any of the specified columns
	 * @param colList List containing the column names on which to filter
	 */
	def nullValueFilterOp(colList: List[String])(input: PipelineData): PipelineData = {
		var filterExpression = ""
		colList.foreach(colName => {
			if (!filterExpression.isEmpty)
				filterExpression = filterExpression + " AND "
			filterExpression = filterExpression + "(" + colName + " is not null)"
		})
		new PipelineData(input.sqlContext, input.srdd.filter(filterExpression))
	}

	/**
	 * Pipeline op to get a single field out of a date, and create a new column with that field
	 *
	 * For instance, this can take a date, and transform it to a week of the year, or a day of the month.
	 *
	 * @param timeCol Column spec denoting the name of a time column in the input DataFrame.	In this case,
	 *								the column is expected to store a Date.
	 * @param fieldCol The name of the column to create with the time field value
	 * @param timeField The field of the date to retrieve
	 * @param input Input pipeline data to transform
	 * @return Transformed pipeline data with the new time field column.
	 */
	def dateFieldOp (timeCol: String, fieldCol: String, timeField: Int)(input: PipelineData): PipelineData = {
		val fieldExtractor: Array[Any] => Any = row => {
			val date = row(0).asInstanceOf[Date]
			val calendar = new GregorianCalendar()
			calendar.setTime(date)
			calendar.get(timeField)
		}
		val output = SchemaTypeUtilities.addColumn(input.srdd, fieldCol, IntegerType, fieldExtractor, timeCol)
		PipelineData(input.sqlContext, output)
	}

	def getJodaTypes (timeField: Int) =
	timeField match {
		case Calendar.MILLISECOND					=> (PeriodType.millis(), DurationFieldType.millis())
		case Calendar.SECOND							 => (PeriodType.seconds(), DurationFieldType.seconds())
		case Calendar.MINUTE							 => (PeriodType.minutes(), DurationFieldType.minutes())
		case Calendar.HOUR								 => (PeriodType.hours(), DurationFieldType.hours())
		case Calendar.HOUR_OF_DAY					=> (PeriodType.hours(), DurationFieldType.hours())
		case Calendar.DAY_OF_WEEK					=> (PeriodType.days(), DurationFieldType.days())
		case Calendar.DAY_OF_WEEK_IN_MONTH => (PeriodType.days(), DurationFieldType.days())
		case Calendar.DAY_OF_MONTH				 => (PeriodType.days(), DurationFieldType.days())
		case Calendar.DAY_OF_YEAR					=> (PeriodType.days(), DurationFieldType.days())
		case Calendar.WEEK_OF_MONTH				=> (PeriodType.weeks(), DurationFieldType.weeks())
		case Calendar.WEEK_OF_YEAR				 => (PeriodType.weeks(), DurationFieldType.weeks())
		case Calendar.MONTH								=> (PeriodType.months(), DurationFieldType.months())
		case Calendar.YEAR								 => (PeriodType.years(), DurationFieldType.years())
	}
	def getIntervalFromJoda (startMoment: Long, currentMoment: Long, intervalType: (PeriodType, DurationFieldType)): Int = {
		if (currentMoment < startMoment) {
			-(new Interval(currentMoment, startMoment).toPeriod(intervalType._1).get(intervalType._2))
		} else {
			(new Interval(startMoment, currentMoment).toPeriod(intervalType._1).get(intervalType._2))
		}
	}

	def elapsedDateOp (timeCol: String, fieldCol: String, timeField: Int, startTime: Date)(input: PipelineData): PipelineData = {
		val jodaTypes = getJodaTypes(timeField)
		val startMoment = startTime.getTime

		val fieldExtractor: Array[Any] => Any = row => {
			val moment = row(0).asInstanceOf[Date].getTime
			getIntervalFromJoda(startMoment, moment, jodaTypes)
		}
		val output = SchemaTypeUtilities.addColumn(input.srdd, fieldCol, IntegerType, fieldExtractor, timeCol)
		PipelineData(input.sqlContext, output)
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
		// Don't backtick-quote the column names.
		// When using a schema with nested arrays, using backtick-quotes around the element access will fail.
		// E.g. `array[0]` will fail. It must be array[0] or `array`[0]
		val selectStatement = "SELECT * FROM " + inputTable + " WHERE " + latCol + " >= " + area.getMinY + " AND " + latCol + " < " + area.getMaxY
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
	 * Operation to create a new schema RDD from selected columns with only distinct rows.
	 *
	 * @param colSpecs Sequence of column specs denoting the columns to select.
	 * @param input Pipeline data to select columns from.
	 * @return Pipeline data containing a schema RDD with only the selected columns with only distinct rows.
	 */
	def columnSelectDistinctOp(colSpecs: Seq[String])(input: PipelineData) = {
		PipelineData(input.sqlContext, input.srdd.selectExpr(colSpecs:_*).distinct)
	}

	/**
	 * Convert the data type of a column
	 *
	 * @param sourceColSpec name of the colum to be converted
	 * @param columnFcn function doing the type conversion
	 * @param columnType target data type
	 * @param input PipelineData from previous stage
	 * @return PipelineData containing updated Dataframe
	 */
	def convertColumnTypeOp (sourceColSpec: String, columnFcn: Array[Any] => Any, columnType: DataType)(input: PipelineData) = {

		// Make sure the temp column name is unique
		val tempCol = "temp-column-" + System.currentTimeMillis()

		val columnAdded = addColumn(input.srdd, tempCol, columnType, c => columnFcn(c) , sourceColSpec)

		val result = columnAdded.select(columnAdded.columns
			.filter(_ != sourceColSpec)
			.map(colName => new Column(colName)) : _ *
		).withColumnRenamed(tempCol, sourceColSpec)

		PipelineData(input.sqlContext, result)
	}

	/**
	 * A basic heatmap tile generator that writes output to HDFS. Uses
	 * TilingTaskParameters to manage tiling task arguments; the arguments map
	 * will be passed through to that object, so all arguments required for its configuration should be set in the map.
	 *
	 * @param xColSpec Colspec denoting data x column
	 * @param yColSpec Colspec denoting data y column
	 * @param tilingParams Parameters to forward to the tiling task.
	 * @param operation Aggregating operation.	Defaults to type Count if unspecified.
	 * @param valueColSpec Colspec denoting the value column to use for the aggregating operation.	None
	 *										 if the default type of Count is used.
	 * @param valueColType Type to interpret colspec value as - float, double, int, long.	None if the default
	 *										 type of count is used for the operation.
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
	 * @param operation Aggregating operation.	Defaults to type Count if unspecified.
	 * @param valueColSpec Colspec denoting the value column to use for the aggregating operation.	None
	 *										 if the default type of Count is used.
	 * @param valueColType Type to interpret colspec value as - float, double, int, long.	None if the default type
	 count is used for the operation.
	 * @param bounds The bounds for the crossplot.	None indicates that bounds will be auto-generated based on input data.
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

	def heatMapBlurredImpl(xColSpec: String,
												 yColSpec: String,
												 tilingParams: TilingTaskParameters,
												 hbaseParameters: Option[HBaseParameters],
												 operation: OperationType = COUNT,
												 kernelRadius: Int = 4,
												 kernelSigma: Double = 3,
												 valueColSpec: Option[String] = None,
												 valueColType: Option[String] = None)
												(input: PipelineData) = {
		val tileIO = hbaseParameters match {
			case Some(p) => new HBaseTileIO(p.zookeeperQuorum, p.zookeeperPort, p.hbaseMaster)
			case None => new LocalTileIO("avro")
		}
		val properties = Map("oculus.binning.projection.type" -> "webmercator")

		// Populate baseline args
		val args = Map(
			"oculus.binning.name" -> tilingParams.name,
			"oculus.binning.description" -> tilingParams.description,
			"oculus.binning.tileWidth" -> tilingParams.tileWidth.toString,
			"oculus.binning.tileHeight" -> tilingParams.tileHeight.toString,
			"oculus.binning.index.type" -> "cartesian",
			"oculus.binning.index.field.0" -> xColSpec,
			"oculus.binning.index.field.1" -> yColSpec)

		val (valueProps, numeric) = operation match {
			case SUM | MAX | MIN | MEAN =>
				(Map("oculus.binning.value.type" -> "field",
				     "oculus.binning.value.field" -> valueColSpec.get,
				     "oculus.binning.value.valueType" -> valueColType.get,
				     "oculus.binning.value.aggregation" -> operation.toString.toLowerCase,
				     "oculus.binning.value.serializer" -> s"[${valueColType.get}]-a"),
				 valueColType.get match {
					 case "double-a" => ExtendedNumeric.ExtendedDouble
					 case "float-a" => ExtendedNumeric.ExtendedFloat
					 case "int-a" => ExtendedNumeric.ExtendedInt
					 case "long-a" => ExtendedNumeric.ExtendedLong
					 case "double-k" => ExtendedNumeric.ExtendedDouble
					 case "float-k" => ExtendedNumeric.ExtendedFloat
					 case "int-k" => ExtendedNumeric.ExtendedInt
					 case "long-k" => ExtendedNumeric.ExtendedLong
					 case _ => throw new IllegalArgumentException("Blurred tiling requires a numeric binning type")
				 }
				)
			case _ =>
				(Map("oculus.binning.value.type" -> "count",
						 "oculus.binning.value.valueType" -> "int",
						 "oculus.binning.value.serializer" -> "[int]-a"),
				 ExtendedNumeric.ExtendedDouble)
		}

		// Parse bounds and level args
		val levelsProps = createLevelsProps("oculus.binning", tilingParams.levels)
		val tableName = PipelineOperations.getOrGenTableName(input, "heatmap_op")
		val tilingTask = TilingTask(input.sqlContext, tableName, args ++ levelsProps ++ valueProps ++ properties)

		def withValueType[PT: ClassTag] (task: TilingTask[PT, _, _, _]): PipelineData = {
			val typedNumeric = numeric.asInstanceOf[ExtendedNumeric[PT]]
			val kernel = StandardBinningFunctions.makeGaussianKernel(kernelRadius, kernelSigma)

			task.doParameterizedTiling(tileIO,
				StandardBinningFunctions.locateIndexOverLevelsWithKernel(kernel, task.getIndexScheme, task.getTilePyramid, task.getNumXBins, task.getNumYBins),
				StandardBinningFunctions.populateTileGaussian(kernel)(typedNumeric)
			)
			PipelineData(input.sqlContext, input.srdd, Option(tableName))
		}

		withValueType(tilingTask)
	}

	def geoSegmentTilingOp(x1ColSpec: String,
												 y1ColSpec: String,
												 x2ColSpec: String,
												 y2ColSpec: String,
												 tilingParams: TilingTaskParameters,
												 hbaseParameters: Option[HBaseParameters],
												 operation: OperationType = COUNT,
												 valueColSpec: Option[String] = None,
												 valueColType: Option[String] = None,
												 lineType: Option[LineDrawingType] = Some(LineDrawingType.Lines),
												 minimumSegmentLength: Option[Int] = Some(4),
												 maximumSegmentLength: Option[Int] = Some(1024),
												 maximumLeaderLength: Option[Int] = Some(1024))
												(input: PipelineData) = {
		val tileIO = hbaseParameters match {
			case Some(p) => new HBaseTileIO(p.zookeeperQuorum, p.zookeeperPort, p.hbaseMaster)
			case None => new LocalTileIO("avro")
		}
		val properties = Map("oculus.binning.projection.type" -> "webmercator")

		segmentTilingOpImpl(x1ColSpec, y1ColSpec, x2ColSpec, y2ColSpec, operation, valueColSpec, valueColType,
												lineType, minimumSegmentLength, maximumSegmentLength, maximumLeaderLength,
												tilingParams, tileIO, properties)(input)
	}

	private def segmentTilingOpImpl(x1ColSpec: String,
																	y1ColSpec: String,
																	x2ColSpec: String,
																	y2ColSpec: String,
																	operation: OperationType,
																	valueColSpec: Option[String],
																	valueColType: Option[String],
																	lineType: Option[LineDrawingType] = Some(LineDrawingType.Lines),
																	minimumSegmentLength: Option[Int] = Some(4),
																	maximumSegmentLength: Option[Int] = Some(1024),
																	maximumLeaderLength: Option[Int] = Some(1024),
																	taskParameters: TilingTaskParameters,
																	tileIO: TileIO,
																	properties: Map[String, String])
																 (input: PipelineData) = {
		// Populate baseline args
		val args: Map[String, String] = Map(
			"oculus.binning.name" -> taskParameters.name,
			"oculus.binning.description" -> taskParameters.description,
			"oculus.binning.tileWidth" -> taskParameters.tileWidth.toString,
			"oculus.binning.tileHeight" -> taskParameters.tileHeight.toString,
			"oculus.binning.index.type" -> "cartesian",
			"oculus.binning.index.field.0" -> x1ColSpec,
			"oculus.binning.index.field.1" -> y1ColSpec,
			"oculus.binning.index.field.2" -> x2ColSpec,
			"oculus.binning.index.field.3" -> y2ColSpec)

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

		def withValueType[PT: ClassTag] (task: TilingTask[PT, _, _, _]): PipelineData = {
		  val (locateFcn, populateFcn): (Traversable[Int] => Seq[Any] => Traversable[(TileIndex, Array[BinIndex])],
				(TileIndex, Array[BinIndex], PT) => MutableMap[BinIndex, PT]) =
				lineType match {
				  case LineDrawingType.LeaderLines => {
						(
						  StandardBinningFunctions.locateLineLeaders(task.getIndexScheme, task.getTilePyramid,
								minimumSegmentLength, maximumLeaderLength.getOrElse(1024),
								task.getNumXBins, task.getNumYBins),
						  StandardBinningFunctions.populateTileWithLineLeaders(maximumLeaderLength.getOrElse(1024),
								StandardScalingFunctions.identityScale)
						  )
				  }
				  case LineDrawingType.Arcs => {
						(
						  StandardBinningFunctions.locateArcs(task.getIndexScheme, task.getTilePyramid,
								minimumSegmentLength, maximumLeaderLength,
								task.getNumXBins, task.getNumYBins),
						  StandardBinningFunctions.populateTileWithArcs(maximumLeaderLength,
								StandardScalingFunctions.identityScale)
						  )
				  }
				  case LineDrawingType.Lines | _ => {
						(
						  StandardBinningFunctions.locateLine(task.getIndexScheme, task.getTilePyramid,
								minimumSegmentLength, maximumSegmentLength, task.getNumXBins, task.getNumYBins),
						  StandardBinningFunctions.populateTileWithLineSegments(StandardScalingFunctions.identityScale)
						  )
				  }
				}
		  task.doParameterizedTiling(tileIO, locateFcn, populateFcn)
		  PipelineData(input.sqlContext, input.srdd, Option(tableName))
		}
		withValueType(tilingTask)

		PipelineData(input.sqlContext, input.srdd, Option(tableName))
	}

	/**
	 * Debug op that will run a take operation on the RDD and print the data out to the console.
	 * Output has a prefix attached to it to allow for filtering through a tool like AWK.
	 *
	 * @param count Number of records to take
	 * @param prefix String to prepend
	 * @param input Pipeline data from previous stage
	 * @return Unmodified input data
	 */
	def takeAndPrintOp(count: Int, prefix: String = "")(input: PipelineData) = {
		input.srdd.take(count).foreach(s => println(s"$prefix: $s"))
		input
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
case class HBaseParameters(zookeeperQuorum: String, zookeeperPort: String, hbaseMaster: String, slicing: Boolean = false)

/**
 * Area of interest region
 */
case class Bounds(minX: Double, minY: Double, maxX: Double, maxY: Double)

/**
 * Supported heatmap aggregation types.	Count assigns a value of 1 to for each record and sums,
 * Sum extracts a value from each record and sums, Max/Min extracts a value from each record and takes
 * the max/min, mean extracts a value and computes a mean.
 */
object OperationType extends Enumeration {
	type OperationType = Value
	val COUNT, SUM, MIN, MAX, MEAN = Value
}
