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



import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConverters._
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.spark.GeneralSparkConnector
import com.oculusinfo.tilegen.datasets.Dataset
import com.oculusinfo.tilegen.datasets.DatasetFactory
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.HBaseTileIO
import com.oculusinfo.tilegen.tiling.LocalTileIO
import com.oculusinfo.tilegen.tiling.ValueOrException
import com.oculusinfo.tilegen.util.PropertiesWrapper
import org.apache.log4j.Logger
import org.apache.log4j.Level
import com.oculusinfo.tilegen.datasets.StreamingProcessingStrategy
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.DStream
import com.oculusinfo.tilegen.datasets.ProcessingStrategy
import org.apache.spark.streaming.Seconds
import com.oculusinfo.tilegen.tiling.DataSource
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import com.oculusinfo.tilegen.tiling.RecordParser
import com.oculusinfo.tilegen.tiling.FieldExtractor
import java.text.SimpleDateFormat
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.tilegen.datasets.StreamingProcessingStrategy
import com.oculusinfo.tilegen.tiling.StandardDoubleBinDescriptor
import java.util.Date
import org.apache.spark.streaming.Time
import com.oculusinfo.tilegen.datasets.BasicDataset
import com.oculusinfo.tilegen.datasets.BasicStreamingDataset
import com.oculusinfo.tilegen.datasets.StreamingProcessor
import java.util.Calendar



/*
 * The following properties control how the application runs:
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
 *  oculus.tileio.type
 *      The way in which tiles are written - either hbase (to write to hbase,
 *      see hbase. properties above to specify where) or file  to write to the
 *      local file system
 *      Default is hbase
 * 
 *  oculus.binning.source.pollTime
 *      The amount of time (in seconds) before it looks for new input in the
 *      source foder and starts to process it.
 * 
 *  oculus.binning.source.batches.{name}
 *      A tree of batch jobs that will be processed separately. The name given
 *      will be used as the job's name during output. 
 *      
 *  oculus.binning.source.batches.{name}.time
 *      The time (in seconds) of the job's window. All results will be grouped
 *      up over this window, and a new tile pyramid will be created for each
 *      time interval. This time must be a multiple of the polling time.
 *      
 *  oculus.binning.source.batches.{name}.name
 *      Gives the ability to override the name used for output.
 * 
 */

class StreamingCSVRecordPropertiesWrapper (properties: Properties) extends PropertiesWrapper(properties) {
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
class StreamingCSVDataSource (properties: PropertiesWrapper, ssc: StreamingContext) extends DataSource {

  def getDataName: String = properties.getProperty("oculus.binning.source.name", "unknown")
  def getDataFiles: Seq[String] = properties.getSeqProperty("oculus.binning.source.location")
  override def getIdealPartitions: Option[Int] = properties.getIntOptionProperty("oculus.binning.source.partitions")

  //a file filter to get rid of any files that end in "_COPYING_"
  val fileFilter: Path => Boolean = path => {
    !path.getName().endsWith("_COPYING_")
  }
  
  def getDataStream: DStream[String] = { ssc.fileStream[LongWritable, Text, TextInputFormat](getDataFiles.head, fileFilter, true).map(_._2.toString) }
  
  def start() = { ssc.start }
  def stop() = { ssc.stop }
  
}

/**
 * A simple parser that splits a record according to a given separator
 */
class StreamingCSVRecordParser (properties: StreamingCSVRecordPropertiesWrapper) extends RecordParser[List[Double]] {
    
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
      if (1 == separator.length) input.split(separator.charAt(0)).map(_.trim)
      else input.split(separator).map(_.trim)

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



class StreamingCSVFieldExtractor (properties: StreamingCSVRecordPropertiesWrapper) extends FieldExtractor[List[Double]] {
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




class WindowedProcessingStrategy(stream: DStream[(Double, Double, Double)], windowDurSec: Int, slideDurSec: Int)
extends StreamingProcessingStrategy[Double] {
  protected def getData: DStream[(Double, Double, Double)] = stream.window(Seconds(windowDurSec), Seconds(slideDurSec))
}

class StreamingSourceProcessor(properties: PropertiesWrapper) {

  private val xVar = properties.getOptionProperty("oculus.binning.xField").get
  private val yVar = properties.getProperty("oculus.binning.yField", "zero")
  private val zVar = properties.getProperty("oculus.binning.valueField", "count")

  def getStream(source: StreamingCSVDataSource, parser: StreamingCSVRecordParser, extractor: StreamingCSVFieldExtractor): DStream[(Double, Double, Double)] = {
    val localXVar = xVar
    val localYVar = yVar
    val localZVar = zVar

    val strm = source.getDataStream
    val data = strm.mapPartitions(iter =>
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

    data.cache
  }

}

object StreamingCSVBinner {
  
  def getBatchJob(batchName: String, props: Map[String, String]) = {
    val nameProp = props.get("name")
    val timeProp = props.get("time")
    
    val name = if (nameProp.isDefined) nameProp.get else batchName
    val time = if (timeProp.isDefined) timeProp.get.toInt else -1
    
    if (time > 0)
    	Some((name, time))
    else
    	None
  }
  
  def dtFormatter = new SimpleDateFormat("yyyDDDHHmm")

  def getTimeString(time: Long, intervalTimeSec: Int): String = {
    //round the time to the last interval time
    val previousIntervalTime = (time / (intervalTimeSec * 1000)) * (intervalTimeSec * 1000)
    dtFormatter.format(new Date(previousIntervalTime))
  }
  
  def main (args: Array[String]): Unit = {
	  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
	  Logger.getLogger("org.apache.spark.storage.BlockManager").setLevel(Level.ERROR)
	  Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.WARN)
    if (args.size<1) {
      println("Usage:")
      println("\tStreamingCSVBinner [-d default_properties_file] job_properties_file_1 job_properties_file_2 ...")
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
    
    val batchDuration = defaultProperties.getIntProperty("oculus.binning.source.pollTime", 60)
    val ssc = new StreamingContext(sc, Seconds(batchDuration))

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

    val batchJobs = {
      val basePropName = "oculus.binning.source.batches"
	  val batchJobNames = defaultProperties.getSeqPropertyNames(basePropName)
	  
	  var jobs = batchJobNames.flatMap(batchName => {
	    val pathName = basePropName + "." + batchName
	    getBatchJob(batchName, defaultProperties.getSeqPropertyMap(pathName))
	  })
    
      //make sure there's at least one basic job
	  if (jobs.isEmpty) {
	    jobs :+ ("", 1)
	  }
	  else
        jobs
    }

    println("Jobs:")
    batchJobs.foreach{job =>
      println("  " + job._1 + ": every " + job._2 + " seconds")
    }
    
    // Run for each real properties file
    while (argIdx < args.size) {
      val props = new Properties(defProps)
      val propStream = new FileInputStream(args(argIdx))
      props.load(propStream)
      propStream.close()
      
      val properties = new StreamingCSVRecordPropertiesWrapper(props)
      val source = new StreamingCSVDataSource(properties, ssc)
      val parser = new StreamingCSVRecordParser(properties)
      val extractor = new StreamingCSVFieldExtractor(properties)

      val streamingStrategy = new StreamingSourceProcessor(properties)
        
      
      batchJobs.foreach{job =>
        def processDatasetInternal[BT: ClassManifest, PT] (dataset: Dataset[BT, PT] with StreamingProcessor[BT]): Unit = {
		  val binner = new RDDBinner
		  binner.debug = true
		
          //go through each of the level sets and process them 
  		  dataset.getLevels.map(levels => {
		    val procFcn:  Time => RDD[(Double, Double, BT)] => Unit =
  		      (time: Time) => (rdd: RDD[(Double, Double, BT)]) => {
  		        if (rdd.count > 0) {
  		          val stime = System.currentTimeMillis()
		          println("processing level: " + levels)
		          val jobName = job._1 + "." + getTimeString(time.milliseconds, job._2) + "." + dataset.getName
		          val tiles = binner.processDataByLevel(rdd,
							        dataset.getBinDescriptor,
							        dataset.getTilePyramid,
							        levels,
							        dataset.getBins,
							        dataset.getConsolidationPartitions)
	              tileIO.writeTileSet(dataset.getTilePyramid,
					  jobName,
					  tiles,
					  dataset.getBinDescriptor,
					  jobName,
					  dataset.getDescription)
					  
			      //grab the final processing time and print out some time stats
			      val ftime = System.currentTimeMillis()
  		          val timeInfo = new StringBuilder().append("Levels ").append(levels).append(" for job ").append(jobName).append(" finished\n")
  		              .append("  Preprocessing: ").append((stime - time.milliseconds)).append("ms\n")
  		              .append("  Processing: ").append((ftime - stime)).append("ms\n")
  		              .append("  Total: ").append((ftime - time.milliseconds)).append("ms\n")
  		          println(timeInfo)
  		        }
  		        else {
  		          println("No data to process")
  		        }
		      }
		    dataset.processWithTime(procFcn, None)
          })
        }
        def processDataset[BT, PT] (dataset: Dataset[BT, PT] with StreamingProcessor[BT]): Unit =
          processDatasetInternal(dataset)(dataset.binTypeManifest)
        
        val stream = streamingStrategy.getStream(source, parser, extractor)
        val windowDurTimeSec = job._2
        val slideDurTimeSec = job._2
        
        val strategy = new WindowedProcessingStrategy(stream, windowDurTimeSec, slideDurTimeSec)
        processDataset(DatasetFactory.createStreamingDataset(sc, strategy, new StandardDoubleBinDescriptor, props, 256))
      }

      argIdx = argIdx + 1
    }
    
    ssc.start
    
  }
}
