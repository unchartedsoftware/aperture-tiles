/**
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
import com.oculusinfo.tilegen.datasets.StreamingProcessor
import java.util.Calendar
import com.oculusinfo.tilegen.datasets.StreamingCSVDataset
import com.oculusinfo.tilegen.datasets.CSVRecordPropertiesWrapper
import com.oculusinfo.tilegen.datasets.CSVRecordParser
import com.oculusinfo.tilegen.datasets.CSVFieldExtractor
import com.oculusinfo.tilegen.tiling.TileIO



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
 * A streaming strategy that takes a given preparsed dstream and then windows it
 * up over the given window and slide durations.
 */
class WindowedProcessingStrategy(stream: DStream[(Double, Double, Double)], windowDurSec: Int, slideDurSec: Int)
extends StreamingProcessingStrategy[Double] {
  protected def getData: DStream[(Double, Double, Double)] = stream.window(Seconds(windowDurSec), Seconds(slideDurSec))
}


object StreamingCSVBinner {

  /**
   * Preparsing function that changes a stream of csv files into the required
   * DStream[(Double, Double, Double)]. All data is cached at the end so that
   * any windowed operations after will start from here.
   */
  def getParsedStream(properties: PropertiesWrapper, source: StreamingCSVDataSource, parser: CSVRecordParser, extractor: CSVFieldExtractor): DStream[(Double, Double, Double)] = {
    val localXVar = properties.getOptionProperty("oculus.binning.xField").get
    val localYVar = properties.getProperty("oculus.binning.yField", "zero")
    val localZVar = properties.getProperty("oculus.binning.valueField", "count")

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
  
  def dtFormatter = new SimpleDateFormat("yyyyDDDHHmm")

  def getTimeString(time: Long, intervalTimeSec: Int): String = {
    //round the time to the last interval time
    val previousIntervalTime = (time / (intervalTimeSec * 1000)) * (intervalTimeSec * 1000)
    dtFormatter.format(new Date(previousIntervalTime))
  }
  
  /**
   * Selects between an HBaseTileIO and LocalTileIO depending on 'oculus.tileio.type'
   */
  def getTileIO(properties: PropertiesWrapper): TileIO = {
    properties.getProperty("oculus.tileio.type", "hbase") match {
      case "hbase" => {
        val quorum = properties.getOptionProperty("hbase.zookeeper.quorum").get
        val port = properties.getProperty("hbase.zookeeper.port", "2181")
        val master = properties.getOptionProperty("hbase.master").get
        new HBaseTileIO(quorum, port, master)
      }
      case _ => {
        val extension =
            properties.getProperty("oculus.tileio.file.extension",
                                          "avro")
        new LocalTileIO(extension)
      }
    }
  }

  /**
   * Returns a sequence of (name: String, time: Int) jobs. 
   */
  def getBatchJobs(properties: PropertiesWrapper): Seq[(String, Int)] = {
    val batchJobs = {
      val basePropName = "oculus.binning.source.batches"
      val batchJobNames = properties.getSeqPropertyNames(basePropName)
	  
	  var jobs = batchJobNames.flatMap(batchName => {
	    val pathName = basePropName + "." + batchName
	    getBatchJob(batchName, properties.getSeqPropertyMap(pathName))
	  })
    
      //make sure there's at least one basic job
	  if (jobs.isEmpty) {
	    jobs :+ ("", 1)
	  }
	  else {
        jobs
	  }
    }

    //print out the list of batch jobs 
    println("Jobs:")
    batchJobs.foreach{job =>
      println("  " + job._1 + ": every " + job._2 + " seconds")
    }
    
    batchJobs
  }
  
  /**
   * The actual processing function for the streaming dataset. This bins up the data and then writes it out.
   */
  def processDataset[BT: ClassManifest, PT] (dataset: Dataset[BT, PT] with StreamingProcessor[BT], job: (String, Int), tileIO: TileIO): Unit = {
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
  
  def processDatasetGeneric[BT, PT] (dataset: Dataset[BT, PT] with StreamingProcessor[BT], tileIO: TileIO, job: (String, Int)): Unit =
    processDataset(dataset, job, tileIO)(dataset.binTypeManifest)
  
  
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

    val tileIO = getTileIO(defaultProperties)

    val batchJobs = getBatchJobs(defaultProperties)
    
    // Run for each real properties file
    while (argIdx < args.size) {
      val props = new Properties(defProps)
      val propStream = new FileInputStream(args(argIdx))
      props.load(propStream)
      propStream.close()
      
      val properties = new CSVRecordPropertiesWrapper(props)
      val source = new StreamingCSVDataSource(properties, ssc)
      val parser = new CSVRecordParser(properties)
      val extractor = new CSVFieldExtractor(properties)

      //preparse the stream before we start to process the actual data
      val parsedStream = getParsedStream(properties, source, parser, extractor)
        
      //loop through each batch job, setup each streaming window, and process it
      batchJobs.foreach{job =>
        //grab the actual preparsed dstream  
        val windowDurTimeSec = job._2
        val slideDurTimeSec = job._2
        
        //create a the windowed strategy for the job
        val strategy = new WindowedProcessingStrategy(parsedStream, windowDurTimeSec, slideDurTimeSec)
        
        val dataset = new StreamingCSVDataset(props, 256)
        dataset.initialize(strategy)

        processDatasetGeneric(dataset, tileIO, job)
      }

      argIdx = argIdx + 1
    }
    
    ssc.start
    
  }
}
