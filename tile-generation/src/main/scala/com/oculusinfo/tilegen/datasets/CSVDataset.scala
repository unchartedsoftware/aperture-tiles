/**
 * Copyright (c) 2013 Oculus Info Inc.
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

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD




import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid

import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.tilegen.tiling.DataSource
import com.oculusinfo.tilegen.tiling.FieldExtractor
import com.oculusinfo.tilegen.tiling.RecordParser
import com.oculusinfo.tilegen.tiling.StandardDoubleBinDescriptor
import com.oculusinfo.tilegen.tiling.ValueOrException
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
 *  oculus.binning.projection
 *      The type of projection to use when binning data.  Possible values are:
 *          EPSG:4326 - bin linearly over the whole range of values found (default)
 *          EPSG:900913 - web-mercator projection (used for geographic values only)
 * 
 *  oculus.binning.xField
 *      The field to use as the X axis value
 * 
 *  oculus.binning.yField
 *      The field to use as the Y axis value.  Defaults to zero (i.e., a
 *      density strip of x data)
 * 
 *  oculus.binning.valueField
 *      The field to use as the bin value
 *      Default is to count entries only
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
private [datasets]
class CSVRecordPropertiesWrapper (properties: Properties) extends PropertiesWrapper(properties) {
  val fields =
    properties.stringPropertyNames.asScala.toSeq
      .filter(_.startsWith("oculus.binning.parsing."))
      .filter(_.endsWith(".index")).map(property => 
      property.substring("oculus.binning.parsing.".length, property.length-".index".length)
    )
  val fieldIndices =
      Range(0, fields.size).map(n => (fields(n) -> n)).toMap
}

/**
 * A simple data source for binning of generic CSV data based on a
 * property-style configuration file
 */
class CSVDataSource (properties: PropertiesWrapper) extends DataSource {
  def getDataName: String = properties.getProperty("oculus.binning.source.name", "unknown")
  def getDataFiles: Seq[String] = properties.getSeqProperty("oculus.binning.source.location")
  override def getIdealPartitions: Option[Int] = properties.getIntOptionProperty("oculus.binning.source.partitions")
}



/**
 * A simple parser that splits a record according to a given separator
 */
class CSVRecordParser (properties: CSVRecordPropertiesWrapper) extends RecordParser[List[Double]] {
    
  def parseRecords (raw: Iterator[String], variables: String*): Iterator[ValueOrException[List[Double]]] = {
    // Get some simple parsing info we'll need
    val separator = properties.getProperty("oculus.binning.parsing.separator", "\t")


    val dateFormats = properties.fields.filter(field =>
      "date" == properties.getProperty("oculus.binning.parsing."+field+".fieldType", "")
    ).map(field => 
        (field ->
         new SimpleDateFormat(properties.getProperty("oculus.binning.parsing."+field+".dateFormat",
                                                     "yyMMddHHmm")))
    ).toMap




    // A quick couple of inline functions to make our lives easier
    // Split a string (but do so efficiently if the separator is a single character)
    def splitString (input: String, separator: String): Array[String] =
      if (1 == separator.length) input.split(separator.charAt(0))
      else input.split(separator)

    def getFieldType (field: String, suffix: String = "fieldType"): String = {
      properties.getProperty("oculus.binning.parsing."+field+"."+suffix,
                             if ("constant" == field || "zero" == field) "constant"
                             else "")
    }

    // Convert a string to a double value according to field semantics
    def parseValue (value: String, field: String, parseType: String): Double = {
      if ("int" == parseType) {
        value.toInt.toDouble
      } else if ("long" == parseType) {
        value.toLong.toDouble
      } else if ("date" == parseType) {
        dateFormats(field).parse(value).getTime()
      } else if ("propertyMap" == parseType) {
        val property = properties.getOptionProperty("oculus.binning.parsing."+field+".property").get
        val propType = getFieldType(field, "propertyType")
        val propSep = properties.getOptionProperty("oculus.binning.parsing."+field+".propertySeparator").get
        val valueSep = properties.getOptionProperty("oculus.binning.parsing."+field+".propertyValueSeparator").get

        val kvPairs = splitString(value, propSep)
        val propPairs = kvPairs.map(splitString(_, valueSep))

        val propValue = propPairs.filter(kv => property.trim == kv(0).trim).map(kv =>
          if (kv.size>1) kv(1) else ""
        ).takeRight(1)(0)
        parseValue(propValue, field, propType)
      } else {
        value.toDouble
      }
    }




    raw.map(s => {
      val columns = splitString(s, separator)
      try {
        new ValueOrException(Some(properties.fields.toList.map(field => {


          val fieldIndex = properties.getIntOptionProperty("oculus.binning.parsing."+field+".index").get
          val fieldType = getFieldType(field)
          var value = if ("constant" == fieldType || "zero" == fieldType) 0.0
                      else parseValue(columns(fieldIndex.toInt), field, fieldType)
          val fieldScaling = properties.getProperty("oculus.binning.parsing."+field+".fieldScaling", "")
          if ("log" == fieldScaling) {
              val base = properties.getDoubleProperty("oculus.binning.parsing."+field+".fieldBase", math.exp(1.0))
              value = math.log(value)/math.log(base)
            }
          value
        })), None)
      } catch {
        case e: Exception => new ValueOrException(None, Some(e))
      }
    })
  }
}



class CSVFieldExtractor (properties: CSVRecordPropertiesWrapper) extends FieldExtractor[List[Double]] {
  def getValidFieldList: List[String] = List()
  def isValidField (field: String): Boolean = true
  def isConstantField (field: String): Boolean = {
    val getFieldType = (field: String) => properties.getProperty("oculus.binning.parsing."+field+".fieldType",
                                                                 if ("constant" == field || "zero" == field) "constant"
                                                                 else "")

    val fieldType = getFieldType(field)
    ("constant" == fieldType || "zero" == fieldType)
  }

  def getFieldValue (field: String)(record: List[Double]) : ValueOrException[Double] =
    if ("count" == field) new ValueOrException(Some(1.0), None)
    else if ("zero" == field) new ValueOrException(Some(0.0), None)
    else new ValueOrException(Some(record(properties.fieldIndices(field))), None)

  override def aggregateValues (valueField: String)(a: Double, b: Double): Double = {
    val fieldAggregation = properties.getProperty("oculus.binning.parsing."+valueField+".fieldAggregation", "add")
    if ("log" == fieldAggregation) {
      val base = properties.getDoubleProperty("oculus.binning.parsing."+valueField+".fieldBase", math.exp(1.0))
      math.log(math.pow(base, a) + math.pow(base, b))/math.log(base)
    } else if ("min" == fieldAggregation) {
      a min b
    } else if ("max" == fieldAggregation) {
      a max b
    } else {
      a + b
    }
  }

  override def getTilePyramid (xField: String, minX: Double, maxX: Double,
			       yField: String, minY: Double, maxY: Double): TilePyramid = {
    val projection = properties.getProperty("oculus.binning.projection", "EPSG:4326")
    if ("EPSG:900913" == projection) {
      new WebMercatorTilePyramid()
    } else {
      val minX = properties.getDoubleOptionProperty("oculus.binning.projection.minx").get
      val maxX = properties.getDoubleOptionProperty("oculus.binning.projection.maxx").get
      val minY = properties.getDoubleOptionProperty("oculus.binning.projection.miny").get
      val maxY = properties.getDoubleOptionProperty("oculus.binning.projection.maxy").get
      new AOITilePyramid(minX, minY, maxX, maxY)
    }
  }
}


class CSVDataset (rawProperties: Properties,
		  tileSize: Int) extends Dataset[Double, JavaDouble] {
  def manifest = implicitly[ClassManifest[Double]]

  private val properties = new CSVRecordPropertiesWrapper(rawProperties)

  private val description = properties.getOptionProperty("oculus.binning.description")
  private val xVar = properties.getOptionProperty("oculus.binning.xField").get
  private val yVar = properties.getProperty("oculus.binning.yField", "zero")
  private val zVar = properties.getProperty("oculus.binning.valueField", "count")
  private val levels = properties.getSeqProperty("oculus.binning.levels").map(lvlString => {
    lvlString.split(',').map(levelRange => {
      val extrema = levelRange.split('-')

      if (0 == extrema.size) Seq[Int]()
      if (1 == extrema.size) Seq[Int](extrema(0).toInt)
      else Range(extrema(0).toInt, extrema(1).toInt+1).toSeq
    }).reduce(_ ++ _)
  })
  private val consolidationPartitions =
    properties.getIntOptionProperty("oculus.binning.consolidationPartitions")


  //////////////////////////////////////////////////////////////////////////////
  // Section: Dataset implementation
  //
  def getName = {
    val name = properties.getProperty("oculus.binning.name", "unknown")
    val prefix = properties.getOptionProperty("oculus.binning.prefix")
    val pyramidName = if (prefix.isDefined) prefix.get+"."+name
		      else name

    pyramidName+"."+xVar+"."+yVar+(if ("count".equals(zVar)) "" else "."+zVar)
  }

  def getDescription =
    description.getOrElse(
      "Binned "+getName+" data showing "+xVar+" vs. "+yVar)

  def getLevels = levels

  def getTilePyramid = {
    val extractor = new CSVFieldExtractor(properties)
    extractor.getTilePyramid("", 0, 0, "", 0, 0)
  }

  override def getBins = tileSize

  def getBinDescriptor: BinDescriptor[Double, JavaDouble] = new StandardDoubleBinDescriptor


  class CSVStaticProcessingStrategy (sc: SparkContext, cache: Boolean)
  extends StaticProcessingStrategy[Double](sc, cache) {
    protected def getData: RDD[(Double, Double, Double)] = {
      val source = new CSVDataSource(properties)
      val parser = new CSVRecordParser(properties)
      val extractor = new CSVFieldExtractor(properties)

      val localXVar = xVar
      val localYVar = yVar
      val localZVar = zVar

      val rawData = source.getData(sc)
      val data = rawData.mapPartitions(iter =>
	// Parse the records from the raw data
	parser.parseRecords(iter, localXVar, localYVar)
      ).filter(r =>
	// Filter out unsuccessful parsings
	r.hasValue
      ).map(_.get).mapPartitions(iter =>
	iter.map(t => (extractor.getFieldValue(localXVar)(t),
                       extractor.getFieldValue(localYVar)(t),
		       extractor.getFieldValue(localZVar)(t)))
      ).filter(record =>
	record._1.hasValue && record._2.hasValue && record._3.hasValue
      ).map(record =>
	(record._1.get, record._2.get, record._3.get)
      )

      if (cache)
	data.cache

      data
    }
  }

  def initialize (sc: SparkContext, cache: Boolean): Unit =
    initialize(new CSVStaticProcessingStrategy(sc, cache))
}
