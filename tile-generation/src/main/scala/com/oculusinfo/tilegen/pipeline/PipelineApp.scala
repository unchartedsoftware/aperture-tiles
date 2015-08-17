/**
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

import java.io.FileInputStream
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Properties

import com.oculusinfo.tilegen.tiling.{HBaseTileIO, LocalTileIO, TileIO}
import com.oculusinfo.tilegen.util.{ArgumentParser, MissingArgumentException}
import grizzled.slf4j.Logger

/**
 * Base class for pipeline operations.  Performs parsing of a core set of arguments and instantiates
 * relevant objects for use by subclasses.
 *
 * @param jobType  Name of the job - will be passed along to the spark context and used as the logging
 *                 tag.
 */
abstract class PipelineApp(val jobType: String, val args: Array[String]) {

	val logger = Logger(jobType)
	val argParser = new ArgumentParser(args)

	// Parse input arguments - no really clean way to handle exceptions in val init, below is what is suggested
	// by the scala community.
	val (
		source: String,
		name: String,
		description: String,
		partitions: Int,
		levelSets: Seq[Seq[Int]] @unchecked,
		columnMap: Map[String, String] @unchecked,
		hbaseParameters: Option[HBaseParameters] @unchecked,
		startTime: Date,
		endTime: Date,
		) = try {
		(
			argParser.getString("source", "The source location at which to find twitter data"),
			argParser.getString("name", "A name with which to label the finished pyramid").replace("\\W", "_"),
			argParser.getString("description", "A description with which to present the finished pyramid").replace("_", " "),
			argParser.getInt("partitions", "The number of partitions into which to read the raw data", Some(200)),
			parseLevels(argParser.getString("levels", "The level sets (;-separated) of ,-separated levels to bin.")),
			parseColumnMap(argParser.getString("columnMap", "Properties defining column mapping",
			                                   Some("/uncharted-twitter-columns.properties"))),
			parseHBaseParams(argParser.getStringOption("hbasemaster", "Address of hbase master", None),
			                 argParser.getStringOption("zookeeperquorum", "Address of zookeeper quorum", None),
			                 argParser.getStringOption("zookeeperport", "Address of zookeeper quorum", None),
			                 argParser.getBooleanOption("slicing", "Whether sliced tiles are stored as one column or as separate columns", Some(false))),
			parseDate(argParser.getStringOption("start",
			                                    "The start time for binning.  Format is yyyy/MM/dd.HH:mm:ss.+zzzz", None)),
			parseDate(argParser.getStringOption("end",
			                                    "The end time for binning.  Format is yyyy/MM/dd.HH:mm:ss.+zzzz", None)),
      )

  } catch {
		case e: MissingArgumentException =>
			logger.error("Argument exception: " + e.getMessage, e)
			argParser.usage
			System.exit(-1)
		case t: Throwable =>
			logger.error("Pipeline app exception: " + t.getMessage, t)
			System.exit(-1)
	}

	if (logger.isInfoEnabled) {
		logger.info("Arguments: " + argParser.properties.map(p => s"${p._1}: ${p._2}").mkString("\n\t", "\n\t", ""))
	} else {
		println("Arguments: " + argParser.properties.map(p => s"${p._1}: ${p._2}").mkString("\n\t", "\n\t", ""))
	}

	// Create our context
	val sc = argParser.getSparkConnector().createContext(Some(s"$jobType: $name"))

	// Instantiate tile
	val tileIO: TileIO = parseHBaseArgs(hbaseParameters)

	// Parse start/end times
	private def parseDate(date: Option[String]) = {
		new Date(date.map(d => new SimpleDateFormat("yyyy/MM/dd.HH:mm:ss.zzzz").parse(d).getTime).getOrElse(Long.MinValue))
	}

	// Setup hbase or file IO
	private def parseHBaseArgs(hbaseParam: Option[HBaseParameters]) = {
		hbaseParam.map(p => new HBaseTileIO(p.zookeeperQuorum, p.zookeeperPort, p.hbaseMaster, p.slicing))
			.getOrElse(new LocalTileIO)
	}

	// Setup hbase params for ops that take the args rather than the constructed tileIO
	private def parseHBaseParams(master: Option[String], quorum: Option[String], port: Option[String], slicing: Option[Boolean]) = {
		if (List(quorum, port, master).flatten.size == 3)
			new Some(HBaseParameters(quorum.get, port.get, master.get, slicing.get))
		else None
	}

	// Parse column string into a sequence of level sequences
	private def parseLevels(levels: String) = levels.split(";").toSeq.map(_.split(",").toSeq.map(_.toInt))

	// Loads a column file
	private def parseColumnMap(columnPath: String) = {
		import scala.collection.JavaConverters._
		val inputStream = new FileInputStream(columnPath)
		val props = new Properties()
		props.load(inputStream)
		Map[String, String]() ++ props.asScala
	}
}