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

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.serialization.TileSerializer

import com.oculusinfo.tilegen.spark.DoubleMaxAccumulatorParam
import com.oculusinfo.tilegen.spark.DoubleMinAccumulatorParam
import com.oculusinfo.tilegen.tiling.AnalysisDescription
import com.oculusinfo.tilegen.tiling.BinningAnalytic
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.PropertiesWrapper



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






class CSVFieldExtractor (properties: CSVRecordPropertiesWrapper) {
	def getValidFieldList: List[String] = List()
	def isValidField (field: String): Boolean = true
	def isConstantField (field: String): Boolean = {
		val getFieldType =
			(field: String) => {
				properties.getString("oculus.binning.parsing."+field+".fieldType",
				                     "The type of the "+field+" field",
				                     Some(if ("constant" == field || "zero" == field) "constant"
				                          else ""))
			}

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
				                                       "The minimum x value to use for "+
					                                       "the tile pyramid").get
				val maxXp = properties.getDoubleOption("oculus.binning.projection.maxx",
				                                       "The maximum x value to use for "+
					                                       "the tile pyramid").get
				val minYp = properties.getDoubleOption("oculus.binning.projection.miny",
				                                       "The minimum y value to use for "+
					                                       "the tile pyramid").get
				val maxYp = properties.getDoubleOption("oculus.binning.projection.maxy",
				                                       "The maximum y value to use for "+
					                                       "the tile pyramid").get
				new AOITilePyramid(minXp, minYp, maxXp, maxYp)
			}
		}
	}
}

object CSVDatasetBase {
	val ZERO_STR = "zero"
}

abstract class CSVDatasetBase[IT: ClassTag,
                              PT: ClassTag,
                              DT: ClassTag,
                              AT: ClassTag,
                              BT]
	(indexer: CSVIndexExtractor[IT],
	 valuer: CSVValueExtractor[PT, BT],
	 dataAnalytics: Option[AnalysisDescription[(IT, PT), DT]],
	 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
	 tileWidth: Int,
	 tileHeight: Int,
	 levels: Seq[Seq[Int]],
	 properties: CSVRecordPropertiesWrapper)
		extends Dataset[IT, PT, DT, AT, BT] {

	private val description = properties.getStringOption("oculus.binning.description",
	                                                     "The description to put in the tile metadata")
	

	private val consolidationPartitions =
		properties.getIntOption("oculus.binning.consolidationPartitions",
		                        "The number of partitions into which to consolidate data when done")


	def getIndexer = indexer



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

		pyramidName+"."+indexer.name+"."+valuer.name
	}

	def getDescription =
		description.getOrElse(
			"Binned "+getName+" data showing "+indexer.description)

	def getLevels = levels

	private def getAxisBounds (): (Double, Double, Double, Double) = {
		val localIndexer = indexer
		val cartesianConversion = localIndexer.indexScheme.toCartesian(_)
		val toCartesian: RDD[(IT, PT, Option[DT])] => RDD[(Double, Double)] =
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
	def getValueScheme: ValueDescription[BT] = valuer
	
	def getBinningAnalytic: BinningAnalytic[PT, BT] = valuer.getBinningAnalytic

	override def isDensityStrip = indexer.isDensityStrip

	def getDataAnalytics: Option[AnalysisDescription[(IT, PT), DT]] = dataAnalytics
	def getTileAnalytics: Option[AnalysisDescription[TileData[BT], AT]] = tileAnalytics
}



/**
 * A standard processing strategy for CSV binning of various sorts
 */
class CSVStaticProcessingStrategy[IT: ClassTag, PT: ClassTag, BT, DT: ClassTag]
	(sc: SparkContext,
	 indexer: CSVIndexExtractor[IT],
	 valuer: CSVValueExtractor[PT, BT],
	 dataAnalytics: Option[AnalysisDescription[(IT, PT), DT]],
	 properties: CSVRecordPropertiesWrapper,
	 cacheRaw: Boolean,
	 cacheFilterable: Boolean,
	 cacheProcessed: Boolean)
		extends StaticProcessingStrategy[IT, PT, DT](sc) {
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
				val parser = CSVRecordParser.fromProperties(localProperties)
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
	def getDataAnalytics: Option[AnalysisDescription[_, DT]] = dataAnalytics


	def getData: RDD[(IT, PT, Option[DT])] = {
		val localProperties = properties
		val localIndexer = indexer
		val localValuer = valuer
		val localDataAnalytics = dataAnalytics
		// Determine which fields we need
		val fields = localIndexer.fields ++ localValuer.fields

		rawData2 = {
			val source = new CSVDataSource(properties)
			val data = source.getData(sc)
			if (cacheRaw)
				data.persist(StorageLevel.MEMORY_AND_DISK)
			data
		}

		val data = rawData2.mapPartitions(iter =>
			{
				val parser = CSVRecordParser.fromProperties(localProperties)

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
				// Determine which fields we need
				val indexFields = localIndexer.fields
				val valueFields = localValuer.fields

				iter.map(t =>
					{
						Try{
							val fieldValues = fields.map(field =>
								(field -> extractor.getFieldValue(field)(t).get)
							).toMap

							(localIndexer.calculateIndex(fieldValues),
							 localValuer.calculateValue(fieldValues))
						}
					}
				)
			}
		).filter(_.isSuccess).map(_.get).map{case (index, value) =>
				// Run any data analytics
				(index, value,  localDataAnalytics.map(_.convert((index, value))))
		}

		if (cacheProcessed)
			data.persist(StorageLevel.MEMORY_AND_DISK)

		data
	}
}



/**
 * Handles basic RDD's using a ProcessingStrategy. 
 */
class CSVDataset[IT: ClassTag, PT: ClassTag, DT: ClassTag, AT: ClassTag, BT]
	(indexer: CSVIndexExtractor[IT],
	 valuer: CSVValueExtractor[PT, BT],
	 dataAnalytics: Option[AnalysisDescription[(IT, PT), DT]],
	 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
	 tileWidth: Int,
	 tileHeight: Int,
	 levels: Seq[Seq[Int]],
	 properties: CSVRecordPropertiesWrapper)
		extends CSVDatasetBase[IT, PT, DT, AT, BT](indexer, valuer,
		                                           dataAnalytics, tileAnalytics,
		                                           tileWidth, tileHeight, levels, properties)
{
	// Just some Filter type aliases from Queries.scala
	import com.oculusinfo.tilegen.datasets.FilterAware._


	type STRATEGY_TYPE = CSVStaticProcessingStrategy[IT, PT, BT, DT]
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
		initialize(new CSVStaticProcessingStrategy(sc,
		                                           indexer,
		                                           valuer,
		                                           dataAnalytics,
		                                           properties,
		                                           cacheRaw,
		                                           cacheFilterable,
		                                           cacheProcessed))
	
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
class StreamingCSVDataset[IT: ClassTag, PT: ClassTag, BT, DT: ClassTag, AT: ClassTag]
	(indexer: CSVIndexExtractor[IT],
	 valuer: CSVValueExtractor[PT, BT],
	 dataAnalytics: Option[AnalysisDescription[(IT, PT), DT]],
	 tileAnalytics: Option[AnalysisDescription[TileData[BT], AT]],
	 tileWidth: Int,
	 tileHeight: Int,
	 levels: Seq[Seq[Int]],
	 properties: CSVRecordPropertiesWrapper)
		extends CSVDatasetBase[IT, PT, DT, AT, BT](indexer, valuer,
		                                           dataAnalytics, tileAnalytics,
		                                           tileWidth, tileHeight, levels, properties)
		with StreamingProcessor[IT, PT, DT]  {
	
	type STRATEGY_TYPE = StreamingProcessingStrategy[IT, PT, DT]
	protected var strategy: STRATEGY_TYPE = null
	
	override protected def allowAutoBounds = false

	def processWithTime[OUTPUT] (fcn: Time => RDD[(IT, PT, Option[DT])] => OUTPUT,
	                             completionCallback: Option[Time => OUTPUT => Unit]): Unit = {
		if (null == strategy) {
			throw new Exception("Attempt to process uninitialized dataset "+getName)
		} else {
			strategy.processWithTime(fcn, completionCallback)
		}
	}
}

