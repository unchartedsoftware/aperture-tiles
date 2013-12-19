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

import com.oculusinfo.tilegen.spark.GeneralSparkConnector

import com.oculusinfo.tilegen.datasets.CSVDatasetDescriptor

import com.oculusinfo.tilegen.tiling.ObjectifiedBinnerBase
import com.oculusinfo.tilegen.tiling.HBaseTileIO
import com.oculusinfo.tilegen.tiling.LocalTileIO
import com.oculusinfo.tilegen.tiling.ValueOrException

import com.oculusinfo.tilegen.util.PropertiesWrapper



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
 *  oculus.binning.name
 *      The name of the output data pyramid
 * 
 */








class PropertyBasedSparkConnector (properties: PropertiesWrapper)
extends GeneralSparkConnector(
  properties.getProperty("spark.connection.url", "local"),
  properties.getProperty("spark.connection.home", "/srv/software/spark-0.7.2"),
  properties.getOptionProperty("spark.connection.user"))
{
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

      val dataset = new CSVDatasetDescriptor(props)


      // Create binning helper classes

      val binner = new ObjectifiedBinnerBase[List[Double]](dataset.source,
							   dataset.parser,
							   dataset.extractor)
      binner.debug = true
      binner.execute = true
      val name = dataset.properties.getProperty("oculus.binning.name", "unknown")

      val pyramidName = if (dataset.prefix.isDefined) dataset.prefix.get+"."+name
                        else name
      def extractResult (record: List[Double]): ValueOrException[Double] =
        dataset.extractor.getFieldValue(dataset.zVar)(record)

      binner.doBinning(sc, tileIO,
                       pyramidName, dataset.xVar, dataset.yVar, dataset.zVar, extractResult,
                       dataset.levels, dataset.consolidationPartitions)

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
