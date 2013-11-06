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
 
package com.oculusinfo.tilegen.examples.apps



import java.lang.{Double => JavaDouble}
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Properties

import scala.collection.JavaConverters._

import spark._
import spark.SparkContext._

import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid

import com.oculusinfo.tilegen.spark.GeneralSparkConnector
import com.oculusinfo.tilegen.spark.MavenReference

import com.oculusinfo.tilegen.tiling.ObjectifiedBinnerBase
import com.oculusinfo.tilegen.tiling.DataSource
import com.oculusinfo.tilegen.tiling.RecordParser
import com.oculusinfo.tilegen.tiling.FieldExtractor
import com.oculusinfo.tilegen.tiling.HBaseTileIO
import com.oculusinfo.tilegen.tiling.LocalTileIO
import com.oculusinfo.tilegen.tiling.ValueOrException



/*
 * Simple CSV binning works on any tabular, character-separated file, and is
 * configured via a standard property file.
 *
 * The following properties control how binning is done:
 *
 *  hbase.zookeeper.quorum
 *      If tiles are written to hbase, the zookeeper quorum location needed to
 *      connect to hbase.
 * 
 *  hbase.zookeeper.port
 *      If tiles are written to hbase, the port through which to connect to
 *      zookeeper.  Defaults to 2181
 * 
 *  hbase.master
 *      If tiles are written to hbase, the location of the hbase master to
 *      which to write them
 *
 * 
 *  spark.connection.url
 *      The location of the spark master.
 *      Defaults to "localhost"
 *
 *  spark.connection.home
 *      The file system location of Spark in the remote location (and,
 *      necessarily, on the local machine too)
 *      Defaults to "/srv/software/spark-0.7.2"
 * 
 *  spark.connection.user
 *      A user name to stick in the job title so people know who is running the
 *      job
 *
 * 
 *  oculus.binning.name
 *      The name of the output data pyramid
 * 
 *  oculus.binning.source.location
 *      The path to the data file or files to be binned
 * 
 *  oculus.tileio.type
 *      The way in which tiles are written - either hbase (to write to hbase,
 *      see hbase. properties above to specify where) or file  to write to the
 *      local file system
 *      Default is hbase
 *
 *  oculus.binning.prefix
 *      A prefix to be prepended to the name of every pyramid location, used to
 *      separate this run of binning from previous runs.
 *      If not present, no prefix is used.
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
 */




object PropertiesWrapper {
  def debugProperties (properties: Properties): Unit = {
    properties.stringPropertyNames.asScala.foreach(property =>
      println("Property \""+property+"\" : \""+properties.getProperty(property.toString)+"\"")
    )
  }
}
class PropertiesWrapper (properties: Properties) extends Serializable {
  val fields =
    properties.stringPropertyNames.asScala.toList
      .filter(_.startsWith("oculus.binning.parsing."))
      .filter(_.endsWith(".index")).map(property => 
      property.substring("oculus.binning.parsing.".length, property.length-".index".length)
    )
  val fieldIndices =
    Range(0, fields.size).map(n => (fields(n) -> n)).toMap

  def getProperty (property: String, default: String): String = {
    val value = properties.getProperty(property)
    if (null == value) default else value
  }

  def getOptionProperty (property: String): Option[String] = {
    val value = properties.getProperty(property)
    if (null == value) None else Some(value)
  }

  def getListProperty (property: String): List[String] = {
    val entries = properties.stringPropertyNames.asScala.filter(_.startsWith(property))
    if (1 == entries.size) {
      List(getOptionProperty(entries.head).get)
    } else {
      val maxEntry = entries.map(_.substring(property.length+1).toInt).reduce(_ max _)
      Range(0, maxEntry+1).map(index => getOptionProperty(property+"."+index).get).toList
    }
  }

  def getIntProperty (property: String, default: Int): Int =
    getProperty(property, default.toString).toInt

  def getIntOptionProperty (property: String): Option[Int] = {
    val value = properties.getProperty(property)
    try {
      if (null == value) None else Some(value.toInt)
    } catch {
      case e: Exception => None
    }
  }

  def getDoubleProperty (property: String, default: Double): Double =
    getProperty(property, default.toString).toDouble

  def getDoubleOptionProperty (property: String): Option[Double] = {
    val value = properties.getProperty(property)
    try {
      if (null == value) None else Some(value.toDouble)
    } catch {
      case e: Exception => None
    }
  }

  def debug: Unit = PropertiesWrapper.debugProperties(properties)
}



class PropertyBasedSparkConnector (properties: PropertiesWrapper)
extends GeneralSparkConnector(
  properties.getProperty("spark.connection.url", "local"),
  properties.getProperty("spark.connection.home", "/srv/software/spark-0.7.2"),
  properties.getOptionProperty("spark.connection.user"))
{
}

/**
 * A simple data source for binning of generic CSV data based on a
 * property-style configuration file
 */
class CSVDataSource (properties: PropertiesWrapper) extends DataSource {
  def getDataName: String = properties.getProperty("oculus.binning.source.name", "unknown")
  def getDataFiles: Seq[String] = properties.getListProperty("oculus.binning.source.location")
  override def getIdealPartitions: Option[Int] = properties.getIntOptionProperty("oculus.binning.source.partitions")
}



/**
 * A simple parser that splits a record according to a given separator
 */
class CSVRecordParser (properties: PropertiesWrapper) extends RecordParser[List[Double]] {
    
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
        new ValueOrException(Some(properties.fields.map(field => {


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



class CSVFieldExtractor (properties: PropertiesWrapper) extends FieldExtractor[List[Double]] {
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
      new AOITilePyramid(minX, minY, maxX, maxY)
    }
  }
}




object CSVBinner {
  def main (args: Array[String]): Unit = {
    if (args.size<1) {
      println("Usage:")
      println("\tCSVBinner [-d default_properties_file] job_properties_file_1 job_properties_file_2 ...")
      System.exit(1)
    }

    // Read default properties
    var argIdx = 0
    var defProps = new Properties()

    while ("-d" == args(argIdx)) {
      argIdx = argIdx + 1
      val stream = new FileInputStream(args(argIdx))
      defProps.load(stream)
      stream.close()
      argIdx = argIdx + 1
    }
    val defaultProperties = new PropertiesWrapper(defProps)

    val connector = new PropertyBasedSparkConnector(defaultProperties)
    val sc = connector.getSparkContext("Pyramid Binning")
    val tileIO = defaultProperties.getProperty("oculus.tileio.type", "hbase") match {
      case "hbase" => {
        val quorum = defaultProperties.getOptionProperty("hbase.zookeeper.quorum").get
        val port = defaultProperties.getProperty("hbase.zookeeper.port", "2181")
        val master = defaultProperties.getOptionProperty("hbase.master").get
        new HBaseTileIO(quorum, port, master)
      }
      case _ => {
        val extension =
            defaultProperties.getProperty("oculus.tileio.file.extension",
                                          "avro")
        new LocalTileIO(extension)
      }
    }


    // Run for each real properties file
    while (argIdx < args.size) {
      val props = new Properties(defProps)
      val propStream = new FileInputStream(args(argIdx))
      props.load(propStream)
      propStream.close()

      val properties = new PropertiesWrapper(props)


      // Create binning helper classes
      val prefix = properties.getOptionProperty("oculus.binning.prefix")
      val name = properties.getProperty("oculus.binning.name", "unknown")
      val xVar = properties.getOptionProperty("oculus.binning.xField").get
      val yVar = properties.getProperty("oculus.binning.yField", "zero")
      val zVar = properties.getProperty("oculus.binning.valueField", "count")
      val source = new CSVDataSource(properties)
      val parser = new CSVRecordParser(properties)
      val extractor = new CSVFieldExtractor(properties)

      val levels = properties.getListProperty("oculus.binning.levels").map(lvlString => {
        lvlString.split(',').map(levelRange => {
          val extrema = levelRange.split('-')

          if (0 == extrema.size) List[Int]()
          if (1 == extrema.size) List[Int](extrema(0).toInt)
          else Range(extrema(0).toInt, extrema(1).toInt+1).toList
        }).reduce(_ ++ _)
      })
      val consolidationPartitions = properties.getIntOptionProperty("oculus.binning.consolidationPartitions")

      val binner = new ObjectifiedBinnerBase[List[Double]](source, parser, extractor)
      binner.debug = true
      binner.execute = true

      val pyramidName = if (prefix.isDefined) prefix.get+"."+name
                        else name
      def extractResult (record: List[Double]): ValueOrException[Double] =
        extractor.getFieldValue(zVar)(record)

      binner.doBinning(sc, tileIO,
                       pyramidName, xVar, yVar, zVar, extractResult,
                       levels, consolidationPartitions)

      argIdx = argIdx + 1
    }
  }
}


object TestPropertyParsing {
  def main (args: Array[String]): Unit = {
    args.foreach(file => {
      val stream = new FileInputStream(file)
      var props = new Properties()
      props.load(stream)
      stream.close()
      
      println("\n\nRead in "+file)
      PropertiesWrapper.debugProperties(props)
      println("\nRaw properties:")
      props.keySet.asScala.foreach(prop => {
        println("\t"+prop+": "+props.getProperty(prop.toString()))
      })
    })
  }
}
