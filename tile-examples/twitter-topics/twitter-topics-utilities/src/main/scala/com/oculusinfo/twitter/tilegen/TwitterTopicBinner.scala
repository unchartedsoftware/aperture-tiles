/*
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

package com.oculusinfo.twitter.tilegen



import java.text.SimpleDateFormat
import java.util.{Date, List => JavaList}

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.tilegen.tiling.analytics.{AnalysisDescription, CompositeAnalysisDescription}
import com.oculusinfo.tilegen.tiling.{CartesianIndexScheme, UniversalBinner, TileIO}
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.twitter.binning.{TwitterTopicAvroSerializer, TwitterDemoTopicRecord}
import org.apache.avro.file.CodecFactory
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import scala.util.Try



object TwitterTopicBinner {
	def main (args: Array[String]) {
		val argParser = new ArgumentParser(args)
		argParser.debug

		val sc = argParser.getSparkConnector.createContext(Some("Twitter demo data tiling"))
		val source = argParser.getString("source", "The source location at which to find twitter data")
		val dateParser = new SimpleDateFormat("yyyy/MM/dd.HH:mm:ss.zzzz")
		// Note:  don't need a start time for this binning project.  Start time is assumed to be 31 days prior to end time.
		val endTime = dateParser.parse(argParser.getString("end", "The end time for binning.  Format is yyyy/MM/dd.HH:mm:ss.+zzzz"))

		val levelSets = argParser.getString("levels",
		                                    "The level sets (;-separated) of ,-separated levels to bin.")
			.split(";").map(_.split(",").map(_.toInt))

		val pyramidId = argParser.getString("id", "An ID by which to identify the finished pyramid.")
		val pyramidName = argParser.getString("name", "A name with which to label the finished pyramid").replace("_", " ")
		val pyramidDescription = argParser.getString("description", "A description with which to present the finished pyramid").replace("_", " ")
		val partitions = argParser.getInt("partitions", "The number of partitions into which to read the raw data", Some(0))
		val topicList = argParser.getString("topicList", "Path and filename of list of extracted topics and English translations")

		val tileIO = TileIO.fromArguments(argParser)

		val files = source.split(",")
		// For each file, attempt create an RDD, then immediately force an
		// exception in the case it does not exist. Union all RDDs together.
		val rawData = files.map { file =>
			Try({
				    val tmp = if (0 == partitions) {
					    sc.textFile( file )
				    } else {
					    sc.textFile( file , partitions)
				    }
				    tmp.partitions // force exception if file does not exist
				    tmp
			    }).getOrElse( sc.emptyRDD )
		}.reduce(_ union _)

		val minAnalysis:
				AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]],
				                    List[TwitterDemoTopicRecord]] =
			new TwitterTopicListAnalysis(new TwitterMinRecordAnalytic)

		val maxAnalysis:
				AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]],
				                    List[TwitterDemoTopicRecord]] =
			new TwitterTopicListAnalysis(new TwitterMaxRecordAnalytic)

		val tileAnalytics: Option[AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]],
		                                              (List[TwitterDemoTopicRecord],
		                                               List[TwitterDemoTopicRecord])]] =
			Some(new CompositeAnalysisDescription(minAnalysis, maxAnalysis))
		val dataAnalytics: Option[AnalysisDescription[((Double, Double),
		                                               Map[String, TwitterDemoTopicRecord]),
		                                              Int]] =
			None

		genericProcessData(rawData, levelSets, tileIO, tileAnalytics, dataAnalytics,
		                   endTime, pyramidId, pyramidName, pyramidDescription, topicList)
	}


	private def genericProcessData[AT, DT]
		(rawData: RDD[String],
		 levelSets: Array[Array[Int]],
		 tileIO: TileIO,
		 tileAnalytics: Option[AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]], AT]],
		 dataAnalytics: Option[AnalysisDescription[((Double, Double),
		                                            Map[String, TwitterDemoTopicRecord]), DT]],
		 endTime: Date,
		 pyramidId: String,
		 pyramidName: String,
		 pyramidDescription: String,
		 topicList: String) =
	{
		val tileAnalyticsTag: ClassTag[AT] = tileAnalytics.map(_.analysisTypeTag).getOrElse(ClassTag.apply(classOf[Int]))
		val dataAnalyticsTag: ClassTag[DT] = dataAnalytics.map(_.analysisTypeTag).getOrElse(ClassTag.apply(classOf[Int]))

		processData(rawData, levelSets, tileIO, tileAnalytics, dataAnalytics,
		            endTime, pyramidId, pyramidName, pyramidDescription, topicList)(tileAnalyticsTag, dataAnalyticsTag)
	}

	private def processData[AT: ClassTag, DT: ClassTag]
		(rawData: RDD[String],
		 levelSets: Array[Array[Int]],
		 tileIO: TileIO,
		 tileAnalytics: Option[AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]], AT]],
		 dataAnalytics: Option[AnalysisDescription[((Double, Double),
		                                            Map[String, TwitterDemoTopicRecord]), DT]],
		 endTime: Date,
		 pyramidId: String,
		 pyramidName: String,
		 pyramidDescription: String,
		 topicList: String) =
	{
		val endTimeSecs = endTime.getTime()/1000;	// convert time from msec to sec
		val topicMatcher = new TopicMatcher
		val topicsMap = topicMatcher.getKeywordList(topicList)	// get pre-extracted topics
		
		// append topics to end of data entries
		val rawDataWithTopics = topicMatcher.appendTopicsToData(rawData.sparkContext, rawData, topicsMap, endTimeSecs)
		
		val data = rawDataWithTopics.mapPartitions(i =>
			{
				val recordParser = new TwitterTopicRecordParser(endTimeSecs)
				i.flatMap(line =>
					{
						try {
							recordParser.getRecordsByTopic(line)
						} catch {
							// Just ignore bad records, there aren't many
							case _: Throwable => Seq[((Double, Double), Map[String, TwitterDemoTopicRecord])]()
						}
					}
				)
			}
		).map(record => (record._1, record._2, dataAnalytics.map(_.convert(record))))
		data.cache

		val binner = new UniversalBinner
		val tilePyramid = new WebMercatorTilePyramid

		// Add global analytic accumulators
		val sc = rawData.context
		tileAnalytics.map(_.addGlobalAccumulator(sc))
		dataAnalytics.map(_.addGlobalAccumulator(sc))
		levelSets.foreach(levelSet =>
			{
				println()
				println()
				println()
				println("Starting binning levels "+levelSet.mkString("[", ",", "]")+" at "+new Date())

				// Add whole-level analytic accumulators for these levels
				tileAnalytics.map(analytic =>
					levelSet.map(level => analytic.addLevelAccumulator(sc, level))
				)
				dataAnalytics.map(analytic =>
					levelSet.map(level => analytic.addLevelAccumulator(sc, level))
				)

				// Do actual binning
				val taskStart = System.currentTimeMillis
				val tiles = binner.processDataByLevel(data,
				                                      new CartesianIndexScheme,
				                                      new TwitterTopicBinningAnalytic,
				                                      tileAnalytics,
				                                      dataAnalytics,
				                                      tilePyramid,
				                                      levelSet,
				                                      xBins=1,
				                                      yBins=1)
				tileIO.writeTileSet(tilePyramid,
				                    pyramidId,
				                    tiles,
				                    new TwitterTopicAvroSerializer(CodecFactory.bzip2Codec()),
				                    tileAnalytics,
				                    dataAnalytics,
				                    pyramidName,
				                    pyramidDescription)
				val taskEnd = System.currentTimeMillis()
				val elapsedMinutes = (taskEnd - taskStart)/60000.0
				println("Finished binning levels "+levelSet.mkString("[", ",", "]")+" at "+new Date())
				println("\telapsed time: "+elapsedMinutes+" minutes")
				println()
			}
		)
	}
}
