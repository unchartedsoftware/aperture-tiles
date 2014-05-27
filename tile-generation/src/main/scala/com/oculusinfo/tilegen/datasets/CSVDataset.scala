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

package com.oculusinfo.tilegen.datasets



import java.lang.{Double => JavaDouble}
import java.text.SimpleDateFormat
import java.util.Properties
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.Time
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.tilegen.spark.DoubleMaxAccumulatorParam
import com.oculusinfo.tilegen.spark.DoubleMinAccumulatorParam
import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.tilegen.tiling.IndexScheme
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme
import com.oculusinfo.tilegen.tiling.StandardDoubleBinDescriptor
import com.oculusinfo.tilegen.tiling.MaximumDoubleBinDescriptor
import com.oculusinfo.tilegen.tiling.MinimumDoubleBinDescriptor
import com.oculusinfo.tilegen.tiling.LogDoubleBinDescriptor
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.PropertiesWrapper
import com.oculusinfo.tilegen.tiling.TimeRangeCartesianIndexScheme
import com.oculusinfo.tilegen.tiling.TimeIndexScheme
import com.oculusinfo.tilegen.tiling.LineSegmentIndexScheme
import java.util.TimeZone



/**
 * This class handles reading in a dataset from a CSV file, based on properties
 * it is given that should describe the dataset.  It should work on any tabular,
 * character-separated file.
 *
 * The following properties control how the data is read:
 * 
 *  oculus.binning.name
 *      The name of the output data pyramid
 * 
 *  oculus.binning.prefix
 *      A prefix to be prepended to the name of every pyramid location, used to
 *      separate this run of binning from previous runs.
 *      If not present, no prefix is used.
 * 
 *  oculus.binning.source.location
 *      The path to the data file or files to be binned
 * 
 *  oculus.binning.source.partitions
 *      The number of partitions into which to break up each source file
 * 
 *  oculus.binning.index.type
 *      The type of index to use in the data.  Currently suppoted options are 
 *      cartesian (the default) and ipv4.
 * 
 *  oculus.binning.xField
 *      The field to use as the X axis value (if a cartesian index is used)
 * 
 *  oculus.binning.yField
 *      The field to use as the Y axis value (if a cartesian index is used).  
 *      Defaults to zero (i.e., a density strip of x data)
 * 
 * oculus.binning.ipv4Field
 *      The field from which to get the ipv4 address (if an ipv4 index is 
 *      used).  The field type for this field must be "ipv4".
 * 
 *  oculus.binning.valueField
 *      The field to use as the bin value
 *      Default is to count entries only
 *
 *  oculus.binning.projection
 *      The type of projection to use when binning data.  Possible values are:
 *          EPSG:4326 - bin linearly over the whole range of values found (default)
 *          EPSG:900913 - web-mercator projection (used for geographic values only)
 *
 *  oculus.binning.projection.autobounds
 *      Only used if oculus.binning.projection is EPSG:4326, this indicates, if
 *      true, that the bounds of the axes should be determined automatically.  If
 *      false (the default), the minx, maxx, miny, and maxy properties of
 *      oculus.binning.projection are instead used as axis bounds.
 *
 *  oculus.binning.projection.minx
 *  oculus.binning.projection.miny
 *  oculus.binning.projection.maxx
 *  oculus.binning.projection.maxy
 *      Only used if oculus.binning.projection is EPSG:4326 and
 *      oculus.binning.projection.autobounds is false.  In that case, these four
 *      Properties are used as the axis bounds in the tile projection.
 * 
 *  oculus.binning.levels.<order>
 *      This is an array property - i.e., if one wants to bin levels in three groups,
 *      then one should have oculus.binning.levels.0, oculus.binning.levels.1, and
 *      oculus.binning.levels.2.  Each is a description of the leels to bin in that
 *      group - a comma-separate list of individual integers, or ranges of integers
 *      (described as start-end).  So "0-4,6,8" would mean levels 0, 1, 2, 3, 4, 6,
 *      and 8.
 *      If only one levels set is needed, the .0 is not required.
 *      If there are multiple level sets, the parsing of the raw data is only done
 *      once, and is cached for use with each level set.
 *      This property is mandatory, and has no default.
 * 
 *  oculus.binning.consolidationPartitions
 *      The number of partitions into which to consolidate data when binning it.
 *      If left out, spark will automatically choose something (hopefully a
 *      reasonable something); this parameter is only needed for fine-tuning failing
 *      processes
 * 
 * 
 *  oculus.binning.parsing.separator
 *      The character or string to use as a separator between columns.
 *      Default is a tab
 *
 *  oculus.binning.parsing.<field>.index
 *      The column number of the described field
 *      This field is mandatory for every field type to be used
 * 
 *  oculus.binning.parsing.<field>.fieldType
 *      The type of value expected in the column specified by
 *      oculus.binning.parsing.<field>.index.  Default is to treat the column as
 *      containing real, double-precision values.  Other possible types are:
 *          constant or zero - treat the column as containing 0.0 (the
 *              column doesn't actually have to exist)
 *          int - treat the column as containing integers
 *          long - treat the column as containing double-precision integers
 *          ipv4 - treat the column as an IP address.  It will be treated as a 
 *              4-digit base 256 number, and just turned into a double
 *          date - treat the column as containing a date.  The date will be
 *              parsed and transformed into milliseconds since the
 *              standard java start date (using SimpleDateFormatter).
 *              Default format is yyMMddHHmm, but this can be overridden
 *              using the oculus.binning.parsing.<field>.dateFormat
 *          propertyMap - treat the column as a property map.  Further
 *              information is then needed to get the specific property.  All
 *              four of the following properties must be present to read the
 *              property.
 *              oculus.binning.parsing.<field>.property - the name of the
 *                  property to read
 *              oculus.binning.parsing.<field>.propertyType - equivalent to
 *                  fieldType
 *              oculus.binning.parsing.<field>.propertySeparator - the
 *                  character or string to use to separate one property from
 *                  the next
 *              oculus.binning.parsing.<field>.propertyValueSeparator - the
 *                  character or string used to separate a property key from
 *                  its value
 *
 *  oculus.binning.parsing.<field>.fieldScaling
 *      How the field values should be scaled.  Default is to leave values as
 *      they are.  Other possibilities are:
 *          log - take the log of the value
 *                (oculus.binning.parsing.<field>.fieldBase is used, just as
 *                with fieldAggregation)
 * 
 *  oculus.binning.parsing.<field>.fieldAggregation
 *      The method of aggregation to be used on values of the X field.
 *      Default is addition.  Other possible aggregation types are:
 *          min - find the minimum value
 *          max - find the maximum value
 *          log - treat the number as a  logarithmic value; aggregation of
 *               a and b is log_base(base^a+base^b).  Base is taken from
 *               property oculus.binning.parsing.<field>.fieldBase, and defaults
 *               to e
 * 
 */

object CSVIndexExtractor {
	def fromProperties (properties: PropertiesWrapper): CSVIndexExtractor[_] = {
		val indexType = properties.getString("oculus.binning.index.type",
		                                     "The type of index to use in the data.  Currently "+
			                                     "suppoted options are cartesian (the default) "+
			                                     "and ipv4.",
		                                     Some("cartesian"))

		indexType match {
			case "cartesian" => {
				val xVar = properties.getString("oculus.binning.xField",
				                                "The field to use for the X axis of tiles produced",
				                                Some(CSVDatasetBase.ZERO_STR))
				val yVar = properties.getString("oculus.binning.yField",
				                                "The field to use for the Y axis of tiles produced",
				                                Some(CSVDatasetBase.ZERO_STR))
				new CartesianIndexExtractor(xVar, yVar)
			}
			case "ipv4" => {
				val ipVar = properties.getString("oculus.binning.ipv4Field",
				                                 "The field from which to get the ipv4 address.  "+
					                                 "Field type must be \"ipv4\".",
				                                 None)
				new IPv4IndexExtractor(ipVar)
			}
		}
	}
}
abstract class CSVIndexExtractor[T: ClassTag] extends Serializable {
	val indexTypeManifest = implicitly[ClassTag[T]]

	// The fields this extractor needs
	def fields: Array[String]

	// The name of the indexing scheme - usually refering to the fields it 
	// uses - for use in table naming.
	def name: String

	// A description of the data set axes
	def description: String

	// The index scheme the binner needs to know what to do with the index
	// values we generate
	def indexScheme: IndexScheme[T]
	
	// Get the index value from the field values
	def calculateIndex (fieldValues: Map[String, Double]): T

	// Indicate if the index implies a density strip
	def isDensityStrip: Boolean
}

abstract class TimeRangeCSVIndexExtractor[T: ClassManifest] extends CSVIndexExtractor[T] {
	// The time based index scheme the binner needs to know what to do with the index
	// values we generate
	def timeIndexScheme: TimeIndexScheme[T]
	
	def msPerTimeRange: Double
}

class CartesianIndexExtractor(xVar: String, yVar: String) extends CSVIndexExtractor[(Double, Double)] {
	private val scheme = new CartesianIndexScheme

	def fields = Array(xVar, yVar)
	def name = xVar + "." + yVar
	def description = xVar + " vs. " + yVar
	def indexScheme = scheme
	def calculateIndex (fieldValues: Map[String, Double]): (Double, Double) =
		(fieldValues(xVar), fieldValues(yVar))
	def isDensityStrip = yVar == CSVDatasetBase.ZERO_STR
}

class LineSegmentIndexExtractor(xVar: String, yVar: String, xVar2: String, yVar2: String) extends CSVIndexExtractor[(Double, Double, Double, Double)] {
	private val scheme = new LineSegmentIndexScheme

	def fields = Array(xVar, yVar, xVar2, yVar2)
	def name = xVar + "." + yVar + "." + xVar2 + "." + yVar2
	def description = xVar + " vs. " + yVar + "and" + xVar2 + "vs" + yVar2
	def indexScheme = scheme
	def calculateIndex(fieldValues: Map[String, Double]): (Double, Double, Double, Double) =
		(fieldValues(xVar), fieldValues(yVar), fieldValues(xVar2), fieldValues(yVar2))
	def isDensityStrip = yVar == CSVDatasetBase.ZERO_STR
}

class IPv4IndexExtractor (ipField: String) extends CSVIndexExtractor[Array[Byte]] {
	private val scheme = new IPv4ZCurveIndexScheme

	def fields = Array(ipField)
	def name = ipField
	def description = ipField
	def indexScheme = scheme
	def calculateIndex (fieldValues: Map[String, Double]): Array[Byte] = {
		val address = fieldValues(ipField).round.toLong
		Array(((address & 0xff000000L) >> 24).toByte,
		      ((address & 0xff0000L) >> 16).toByte,
		      ((address & 0xff00L) >> 8).toByte,
		      ((address & 0xff)).toByte)
	}
	val isDensityStrip = false
}

class TimeRangeCartesianIndexExtractor(timeVar: String, xVar: String, yVar: String, startDate: Double, secsPerPeriod: Double) extends TimeRangeCSVIndexExtractor[(Double, Double, Double)] {
	private val scheme = new TimeRangeCartesianIndexScheme

	def msPerTimeRange = secsPerPeriod * 1000
	
	/**
	 * Floors the date value to the last time period
	 */
	def floorDate(d: Double) = Math.floor((d - startDate) / msPerTimeRange) * msPerTimeRange + startDate
	
	def fields = Array(timeVar, xVar, yVar)
	def name = xVar + "." + yVar
	def description = xVar + " vs. " + yVar
	def indexScheme = scheme
	def timeIndexScheme = scheme
	def calculateIndex (fieldValues: Map[String, Double]): (Double, Double, Double) = 
		(floorDate(fieldValues(timeVar)), fieldValues(xVar), fieldValues(yVar))
	def isDensityStrip = yVar == CSVDatasetBase.ZERO_STR
}


/*
 * Simple class to add standard field interpretation to a properties wrapper
 */
class CSVRecordPropertiesWrapper (properties: Properties) extends PropertiesWrapper(properties) {
	val fields = {
		val indexProps = properties.stringPropertyNames.asScala.toSeq
			.filter(_.startsWith("oculus.binning.parsing."))
			.filter(_.endsWith(".index"))


		val orderedProps = indexProps.map(prop => (prop, properties.getProperty(prop).toInt))
			.sortBy(_._2).map(_._1)

		orderedProps.map(property =>
			property.substring("oculus.binning.parsing.".length, property.length-".index".length)
		)
	}
	val fieldIndices =
		Range(0, fields.size).map(n => (fields(n) -> n)).toMap
}

/**
 * A simple data source for binning of generic CSV data based on a
 * property-style configuration file
 */
class CSVDataSource (properties: CSVRecordPropertiesWrapper) {
	def getDataFiles: Seq[String] = properties.getStringPropSeq(
		"oculus.binning.source.location",
		"The hdfs file name from which to get the CSV data.  Either a directory, all "+
			"of whose contents should be part of this dataset, or a single file.")

	def getIdealPartitions: Option[Int] = properties.getIntOption(
		"oculus.binning.source.partitions",
		"The number of partitions to use when reducing data, if needed")

	/**
	 * Actually retrieve the data.
	 * This can be overridden if the data is not a simple file or set of files,
	 * but normally shouldn't be touched.
	 */
	def getData (sc: SparkContext): RDD[String] =
		if (getIdealPartitions.isDefined) {
			getDataFiles.map(sc.textFile(_, getIdealPartitions.get)).reduce(_ union _)
		} else {
			getDataFiles.map(sc.textFile(_)).reduce(_ union _)
		}
}



/**
 * A simple parser that splits a record according to a given separator
 */
class CSVRecordParser (properties: CSVRecordPropertiesWrapper) {

	// A quick couple of inline functions to make our lives easier
	// Split a string (but do so efficiently if the separator is a single character)
	def splitString(input: String, separator: String): Array[String] =
		if (1 == separator.length) input.split(separator.charAt(0))
		else input.split(separator)

	def getFieldType(field: String, suffix: String = "fieldType"): String = {
		properties.getString("oculus.binning.parsing." + field + "." + suffix,
			"",
			Some(if ("constant" == field || "zero" == field) "constant"
			else ""))
	}

	// Convert a string to a double value according to field semantics
	def parseValue(value: String, field: String, parseType: String, dateFormats: Map[String, SimpleDateFormat]): Double = {
		if ("int" == parseType) {
			value.toInt.toDouble
		} else if ("long" == parseType) {
			value.toLong.toDouble
		} else if ("date" == parseType) {
			dateFormats(field).parse(value).getTime()
		} else if ("ipv4" == parseType) {
			value.split("\\.").map(_.toLong).foldLeft(0L)((a, b) => (256L * a + b)).toDouble
		} else if ("propertyMap" == parseType) {
			val property = properties.getStringOption("oculus.binning.parsing." + field + ".property", "").get
			val propType = getFieldType(field, "propertyType")
			val propSep = properties.getStringOption("oculus.binning.parsing." + field +
				".propertySeparator", "").get
			val valueSep = properties.getStringOption("oculus.binning.parsing." + field +
				".propertyValueSeparator", "").get

			val kvPairs = splitString(value, propSep)
			val propPairs = kvPairs.map(splitString(_, valueSep))

			val propValue = propPairs.filter(kv => property.trim == kv(0).trim).map(kv =>
				if (kv.size > 1) kv(1) else "").takeRight(1)(0)
			parseValue(propValue, field, propType, dateFormats)
		} else {
			value.toDouble
		}
	}

	def parseRecords(raw: Iterator[String], variables: String*): Iterator[(String, Try[List[Double]])] = {
		// This method generally is only called on workers, therefore properties can't really be documented here.

		// Get some simple parsing info we'll need
		val separator = properties.getString(
			"oculus.binning.parsing.separator", "", Some("\t"))

		val dateFormats = properties.fields.filter(field =>
			"date" == properties.getString("oculus.binning.parsing." + field + ".fieldType", "", Some(""))).map(field => {
			val format = new SimpleDateFormat(properties.getString(
				"oculus.binning.parsing." + field + ".dateFormat",
				"", Some("yyMMddHHmm")))
			format.setTimeZone(TimeZone.getTimeZone("GMT"))
			(field -> format)
		}).toMap

		raw.map(s =>
			{
				val columns = splitString(s, separator)

				(s,
					Try(
						properties.fields.toList.map(field =>
							{
								val fieldType = getFieldType(field)
								var value = if ("constant" == fieldType ||
									"zero" == fieldType) 0.0
								else {
									val fieldIndex =
										properties.getIntOption("oculus.binning.parsing." +
											field + ".index",
											"").get
									parseValue(columns(fieldIndex.toInt), field, fieldType, dateFormats)
								}
								val fieldScaling =
									properties.getString("oculus.binning.parsing." + field +
										".fieldScaling", "", Some(""))
								if ("log" == fieldScaling) {
									val base =
										properties.getDouble("oculus.binning.parsing." +
											field + ".fieldBase",
											"", Some(math.exp(1.0)))
									value = math.log(value) / math.log(base)
								}
								value
							})))
			})
	}
}

/**
 * Custom record parser for use with graph data				//TODO -- should this be moved to CSVGraphBinner.scala?
 */
class GraphRecordParser(properties: CSVRecordPropertiesWrapper) extends CSVRecordParser(properties) {

	def parseGraphRecords(raw: Iterator[String], variables: String*): Iterator[(String, Try[List[Double]])] = {
		// This method generally is only called on workers, therefore properties can't really be documented here.

		val graphDataType = properties.getString("oculus.binning.graph.data","", Some("nodes"))	// to choose if parsing nodes or edges
		val graphFieldID = if (graphDataType=="nodes") "node" else "edge"
		
		// Get some simple parsing info we'll need
		val separator = properties.getString(
			"oculus.binning.parsing.separator", "", Some("\t"))

		val dateFormats = properties.fields.filter(field =>
			"date" == properties.getString("oculus.binning.parsing." + field + ".fieldType", "", Some(""))).map(field => {
			val format = new SimpleDateFormat(properties.getString(
				"oculus.binning.parsing." + field + ".dateFormat",
				"", Some("yyMMddHHmm")))
			format.setTimeZone(TimeZone.getTimeZone("GMT"))
			(field -> format)
		}).toMap

		raw.map(s =>
			{
				val columns = splitString(s, separator)

				(s,
					Try(
						// Only parse lines that have 1st column = graphFieldID, else disregard
						if (columns(0) == graphFieldID) {	
							properties.fields.toList.map(field =>
								{
									val fieldType = getFieldType(field)
									var value = if ("constant" == fieldType ||
										"zero" == fieldType) 0.0
									else {
										val fieldIndex =
											properties.getIntOption("oculus.binning.parsing." +
												field + ".index",
												"").get
										parseValue(columns(fieldIndex.toInt), field, fieldType, dateFormats)
									}
									val fieldScaling =
										properties.getString("oculus.binning.parsing." + field +
											".fieldScaling", "", Some(""))
									if ("log" == fieldScaling) {
										val base =
											properties.getDouble("oculus.binning.parsing." +
												field + ".fieldBase",
												"", Some(math.exp(1.0)))
										value = math.log(value) / math.log(base)
									}
									value
								})
						}
						else {
							List[Double]()
						}
					)
				)
			})
	}
}



class CSVFieldExtractor (properties: CSVRecordPropertiesWrapper) {
	def getValidFieldList: List[String] = List()
	def isValidField (field: String): Boolean = true
	def isConstantField (field: String): Boolean = {
		val getFieldType =
			(field: String) => properties.getString("oculus.binning.parsing."+field+".fieldType",
			                                        "The type of the "+field+" field",
			                                        Some(if ("constant" == field || "zero" == field) "constant"
			                                             else ""))

		val fieldType = getFieldType(field)
		("constant" == fieldType || "zero" == fieldType)
	}

	def getFieldValue (field: String)(record: List[Double]) : Try[Double] =
		if ("count" == field) Try(1.0)
		else if ("zero" == field) Try(0.0)
		else Try(record(properties.fieldIndices(field)))

	def getTilePyramid (autoBounds: Boolean,
	                    xField: String, minX: Double, maxX: Double,
	                    yField: String, minY: Double, maxY: Double): TilePyramid = {
		val projection = properties.getString("oculus.binning.projection",
		                                      "The type of tile pyramid to use",
		                                      Some("EPSG:4326"))
		if ("EPSG:900913" == projection) {
			new WebMercatorTilePyramid()
		} else {
			if (autoBounds) {
				new AOITilePyramid(minX, minY, maxX, maxY)
			} else {
				val minXp = properties.getDoubleOption("oculus.binning.projection.minx",
				                                       "The minimum x value to use for the tile pyramid").get
				val maxXp = properties.getDoubleOption("oculus.binning.projection.maxx",
				                                       "The maximum x value to use for the tile pyramid").get
				val minYp = properties.getDoubleOption("oculus.binning.projection.miny",
				                                       "The minimum y value to use for the tile pyramid").get
				val maxYp = properties.getDoubleOption("oculus.binning.projection.maxy",
				                                       "The maximum y value to use for the tile pyramid").get
				new AOITilePyramid(minXp, minYp, maxXp, maxYp)
			}
		}
	}
}

object CSVDatasetBase {
	val ZERO_STR = "zero"
}

abstract class CSVDatasetBase[IT: ClassTag]
	(indexer: CSVIndexExtractor[IT],
	 properties: CSVRecordPropertiesWrapper,
	 tileWidth: Int,
	 tileHeight: Int)
		extends Dataset[IT, Double, JavaDouble] {
	def manifest = implicitly[ClassTag[Double]]

	private val description = properties.getStringOption("oculus.binning.description",
	                                                     "The description to put in the tile metadata")
	private val zVar = properties.getString("oculus.binning.valueField",
	                                        "The field to use for the value to tile",
	                                        Some("count"))
	private val levels = properties.getStringPropSeq("oculus.binning.levels",
	                                                 "The levels to bin").map(lvlString =>
		{
			lvlString.split(',').map(levelRange =>
				{
					val extrema = levelRange.split('-')

					if (0 == extrema.size) Seq[Int]()
					if (1 == extrema.size) Seq[Int](extrema(0).toInt)
					else Range(extrema(0).toInt, extrema(1).toInt+1).toSeq
				}
			).fold(Seq[Int]())(_ ++ _)
		}
	)

	private val consolidationPartitions =
		properties.getIntOption("oculus.binning.consolidationPartitions",
		                        "The number of partitions into which to consolidate data when done")


	//////////////////////////////////////////////////////////////////////////////
	// Section: Dataset implementation
	//
	def getName = {
		val name = properties.getString("oculus.binning.name",
		                                "The name of the tileset",
		                                Some("unknown"))
		val prefix = properties.getStringOption("oculus.binning.prefix",
		                                        "A prefix to add to the tile pyramid ID")
		val pyramidName = if (prefix.isDefined) prefix.get+"."+name
		else name

		pyramidName+"."+indexer.name+(if ("count".equals(zVar)) "" else "."+zVar)
	}

	def getDescription =
		description.getOrElse(
			"Binned "+getName+" data showing "+indexer.description)

	def getLevels = levels

	private def getAxisBounds (): (Double, Double, Double, Double) = {
		val localIndexer = indexer
		val cartesianConversion = localIndexer.indexScheme.toCartesian(_)
		val toCartesian: RDD[(IT, Double)] => RDD[(Double, Double)] =
			rdd => rdd.map(_._1).map(cartesianConversion)
		val coordinates = transformRDD(toCartesian)

		// Figure out our axis bounds
		val minXAccum = coordinates.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam)
		val maxXAccum = coordinates.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam)
		val minYAccum = coordinates.context.accumulator(Double.MaxValue)(new DoubleMinAccumulatorParam)
		val maxYAccum = coordinates.context.accumulator(Double.MinValue)(new DoubleMaxAccumulatorParam)

		properties.setDistributedComputation(true)
		coordinates.foreach(p =>
			{
				val (x, y) = p
				minXAccum += x
				maxXAccum += x
				minYAccum += y
				maxYAccum += y
			}
		)
		properties.setDistributedComputation(false)

		val minX = minXAccum.value
		val maxX = maxXAccum.value
		val minY = minYAccum.value
		val maxY = maxYAccum.value

		// Include a fraction of a bin extra in the bounds, so the max goes on the
		// right side of the last tile, rather than forming an extra tile.
		val maxLevel = {
			if (levels.isEmpty) 18
			else levels.map(_.reduce(_ max _)).reduce(_ max _)
		}
		val epsilon = (1.0/(1 << maxLevel))
		val adjustedMaxX = maxX+(maxX-minX)*epsilon/(tileWidth*tileWidth)
		val adjustedMaxY = maxY+(maxY-minY)*epsilon/(tileHeight*tileHeight)
		if (_debug) {
			println(("\n\n\nGot bounds: %.4f to %.4f (%.4f) x, "+
				         "%.4f to %.4f (%.4f) y").format(minX, maxX, adjustedMaxX, minY, maxY, adjustedMaxY))
		}

		(minX, adjustedMaxX, minY, adjustedMaxY)
	}

	protected def allowAutoBounds = true
	private lazy val axisBounds = getAxisBounds()

	def getTilePyramid = {
		val extractor = new CSVFieldExtractor(properties)
		val autoBounds = (
			allowAutoBounds &&
				properties.getBoolean("oculus.binning.projection.autobounds",
				                      "If true, calculate tile pyramid bounds automatically; "+
					                      "if false, use values given by properties",
				                      Some(true)).get
		)
		val (minX, maxX, minY, maxY) =
			if (autoBounds) {
				axisBounds
			} else {
				(0.0, 0.0, 0.0, 0.0)
			}

		extractor.getTilePyramid(autoBounds, "", minX, maxX, "", minY, maxY)
	}

	override def getNumXBins = tileWidth
	override def getNumYBins = tileHeight
	override def getConsolidationPartitions: Option[Int] = consolidationPartitions

	def getIndexScheme = indexer.indexScheme
 
	def getBinDescriptor: BinDescriptor[Double, JavaDouble] = {
		val fieldAggregation = properties.getString("oculus.binning.parsing." + zVar + ".fieldAggregation",
		                                            "The way to aggregate the value field when binning",
		                                            Some("add"))
		if ("log" == fieldAggregation) {
			val base = properties.getDouble("oculus.binning.parsing." + zVar + ".fieldBase",
			                                "The base to use when taking value the logarithm of "+
				                                "values.  Default is e.",
			                                Some(math.exp(1.0)))
			new LogDoubleBinDescriptor(base)
		}
		else if ("min" == fieldAggregation)
			new MinimumDoubleBinDescriptor
		else if ("max" == fieldAggregation)
			new MaximumDoubleBinDescriptor
		else
			new StandardDoubleBinDescriptor
	}

	override def isDensityStrip = indexer.isDensityStrip


	class CSVStaticProcessingStrategy (sc: SparkContext,
	                                   cacheRaw: Boolean,
	                                   cacheFilterable: Boolean,
	                                   cacheProcessed: Boolean)
			extends StaticProcessingStrategy[IT, Double](sc) {
		// This is a weird initialization problem that requires some
		// documentation to explain.
		// What we really want here is for rawData to be initialized in the
		// getData method, below.  However, this method is called from
		// StaticProcessingStrategy.rdd, which is called during the our
		// parent's <init> call - which happens before we event get to this
		// line.  So when we get here, if we assigned rawData directly in
		// getData, this line below, having to assign rawData some value,
		// would overwrite it.
		// We could just say rawData = rawData (that does work, I checked),
		// but that seemed semantically too confusing to abide. So instead,
		// getData sets rawData2, which can the be assigned to rawData before
		// it gets written in its own initialization (since initialization
		// lines are run in order).
		private var rawData: RDD[String] = rawData2
		private var rawData2: RDD[String] = null

		private lazy val filterableData: RDD[(String, List[Double])] = {
			val localProperties = properties
			val data = rawData.mapPartitions(iter =>
				{
					val parser = new CSVRecordParser(localProperties)
					// Parse the records from the raw data, parsing all fields
					// The funny end syntax tells scala to treat fields as a varargs
					parser.parseRecords(iter, localProperties.fields:_*)
						.filter(_._2.isSuccess).map{case (record, fields) => (record, fields.get)}
				}
			)
			if (cacheFilterable)
				data.persist(StorageLevel.MEMORY_AND_DISK)
			data
		}

		def getRawData = rawData
		def getFilterableData = filterableData

		def getData: RDD[(IT, Double)] = {
			val localProperties = properties
			val localIndexer = indexer
			val localZVar = zVar

			rawData2 = {
				val source = new CSVDataSource(properties)
				val data = source.getData(sc);
				if (cacheRaw)
					data.persist(StorageLevel.MEMORY_AND_DISK)
				data
			}

			val data = rawData2.mapPartitions(iter =>
				{
					val parser = new CSVRecordParser(localProperties)
					// Determine which fields we need
					val fields = if ("count" == localZVar) {
						localIndexer.fields
					} else {
						localIndexer.fields :+ localZVar
					}

					// Parse the records from the raw data
					parser.parseRecords(iter, fields:_*)
						.map(_._2) // We don't need the original record (in _1)
				}
			).filter(r =>
				// Filter out unsuccessful parsings
				r.isSuccess
			).map(_.get).mapPartitions(iter =>
				{
					val extractor = new CSVFieldExtractor(localProperties)

					iter.map(t =>
						{
							// Determine our index value
							val indexValue = Try(
								{
									val indexFields = localIndexer.fields
									val fieldValues = indexFields.map(field =>
										(field -> extractor.getFieldValue(field)(t))
									).map{case (k, v) => (k, v.get)}.toMap
									localIndexer.calculateIndex(fieldValues)
								}
							)

							// Determine and add in our binnable value
							(indexValue,
							 extractor.getFieldValue(localZVar)(t))
						}
					)
				}
			).filter(record =>
				record._1.isSuccess && record._2.isSuccess
			).map(record =>
				(record._1.get, record._2.get)
			)

			if (cacheProcessed)
				data.persist(StorageLevel.MEMORY_AND_DISK)

			data
		}
	}
}

/**
 * Handles basic RDD's using a ProcessingStrategy. 
 */
class CSVDataset[IT: ClassTag] (indexer: CSVIndexExtractor[IT],
                                properties: CSVRecordPropertiesWrapper,
                                tileWidth: Int,
                                tileHeight: Int)
		extends CSVDatasetBase[IT](indexer, properties, tileWidth, tileHeight) {
	// Just some Filter type aliases from Queries.scala
	import com.oculusinfo.tilegen.datasets.FilterAware._


	type STRATEGY_TYPE = CSVStaticProcessingStrategy
	protected var strategy: STRATEGY_TYPE = null

	def getRawData: RDD[String] = strategy.getRawData

	def getRawFilteredData (filterFcn: Filter):	RDD[String] = {
		strategy.getFilterableData
			.filter{ case (record, fields) => filterFcn(fields)}
			.map(_._1)
	}
	def getRawFilteredJavaData (filterFcn: Filter): JavaRDD[String] =
		JavaRDD.fromRDD(getRawFilteredData(filterFcn))

	def getFieldFilterFunction (field: String, min: Double, max: Double): Filter = {
		val localProperties = properties
		new FilterFunction with Serializable {
			def apply (valueList: List[Double]): Boolean = {
				val index = localProperties.fieldIndices(field)
				val value = valueList(index)
				min <= value && value <= max
			}
			override def toString: String = "%s Range[%.4f, %.4f]".format(field, min, max)
		}
	}

	def initialize (sc: SparkContext,
	                cacheRaw: Boolean,
	                cacheFilterable: Boolean,
	                cacheProcessed: Boolean): Unit =
		initialize(new CSVStaticProcessingStrategy(sc, cacheRaw, cacheFilterable, cacheProcessed))
	
}


/**
 * The streaming version of the CSVDataset. This will use a StreamingProcessingStrategy
 * that works on a DStream
 * 
 * NOTE: StreamingCSVDataset requires the user to call initialize with a
 * strategy. This is because a default strategy cannot be easily constructed
 * for the case where the stream is windowed. In this case the stream must be
 * preparsed and then a new strategy created for each window.  
 */
class StreamingCSVDataset[IT: ClassTag] (indexer: CSVIndexExtractor[IT],
                                         properties: CSVRecordPropertiesWrapper,
                                         tileWidth: Int,
                                         tileHeight: Int)
		extends CSVDatasetBase[IT](indexer, properties,
		                       tileWidth, tileHeight) with StreamingProcessor[IT, Double]  {
	
	type STRATEGY_TYPE = StreamingProcessingStrategy[IT, Double]
	protected var strategy: STRATEGY_TYPE = null
	
	override protected def allowAutoBounds = false

	def processWithTime[OUTPUT] (fcn: Time => RDD[(IT, Double)] => OUTPUT,
	                             completionCallback: Option[Time => OUTPUT => Unit]): Unit = {
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized dataset "+getName)
		} else {
			strategy.processWithTime(fcn, completionCallback)
		}
	}
}

