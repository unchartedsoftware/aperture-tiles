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
import java.util.Date
import java.util.{List => JavaList}

import scala.reflect.ClassTag

import org.apache.spark.rdd.RDD

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.impl.WebMercatorTilePyramid

import com.oculusinfo.tilegen.spark.MavenReference
import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.tiling.AnalysisDescription
import com.oculusinfo.tilegen.tiling.CompositeAnalysisDescription
import com.oculusinfo.tilegen.tiling.CartesianIndexScheme
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.TileIO
import com.oculusinfo.tilegen.util.ArgumentParser

import com.oculusinfo.twitter.binning.TwitterDemoRecord



object TwitterDemoBinner {
	def main (args: Array[String]) {
		val argParser = new ArgumentParser(args)
		argParser.debug

		val jars =
			Seq(new MavenReference("com.oculusinfo", "twitter-utilities", "0.3-SNAPSHOT")
			) union SparkConnector.getDefaultLibrariesFromMaven
		val sc = argParser.getSparkConnector(jars).getSparkContext("Twitter demo data tiling")

		val source = argParser.getString("source",
		                                 "The source location at which to find twitter data")
		val dateParser = new SimpleDateFormat("yyyy/MM/dd.HH:mm:ss.zzzz")
		val startTime = dateParser.parse(argParser.getString("start",
		                                                     "The start time for binning.  "
			                                                     +"Format is "
			                                                     +"yyyy/MM/dd.HH:mm:ss.zzzz"))
		val endTime = dateParser.parse(argParser.getString("end",
		                                                   "The end time for binning.  "
			                                                   +"Format is "
			                                                   +"yyyy/MM/dd.HH:mm:ss.zzzz"))
		val bins = argParser.getInt("bins",
		                            "The number of time bins into which to divide the time range?",
		                            Some(1))
		val levelSets = argParser.getString("levels",
		                                    "The level sets (;-separated) of ,-separated "
			                                    +"levels to bin.")
			.split(";").map(_.split(",").map(_.toInt))
		val levelBounds = levelSets.map(_.map(a => (a, a))
			                                .reduce((a, b) => (a._1 min b._1, a._2 max b._2)))
			.reduce((a, b) => (a._1 min b._1, a._2 max b._2))

		val pyramidId = argParser.getString("id",
		                                    "An ID by which to identify the finished pyramid.")
		val pyramidName = argParser.getString("name",
		                                      "A name with which to label the finished "
			                                      +"pyramid").replace("_", " ")
		val pyramidDescription = argParser.getString("description",
		                                             "A description with which to present "
			                                             +"the finished pyramid").replace("_", " ")
		val partitions = argParser.getInt("partitions",
		                                  "The number of partitions into which to "
			                                  +"read the raw data",
		                                  Some(0))
		val useWords = argParser.getBoolean("words",
		                                    "If true, pull out all non-trival words from "
			                                    +"tweet text; if false, just pull out tags.",
		                                    Some(false))

		val tileIO = TileIO.fromArguments(argParser)

		val rawData = if (0 == partitions) {
			sc.textFile(source)
		} else {
			sc.textFile(source, partitions)
		}

		val minAnalysis:
				AnalysisDescription[TileData[JavaList[TwitterDemoRecord]],
				                    List[TwitterDemoRecord]] =
			new TwitterDemoListAnalysis(
				sc, new TwitterMinRecordAnalytic,
				Range(levelBounds._1, levelBounds._2+1).map(level =>
					(level+".min" -> ((index: TileIndex) => (level == index.getLevel())))
				).toMap + ("global.min" -> ((index: TileIndex) => true))
			)

		val maxAnalysis:
				AnalysisDescription[TileData[JavaList[TwitterDemoRecord]],
				                    List[TwitterDemoRecord]] =
			new TwitterDemoListAnalysis(
				sc, new TwitterMaxRecordAnalytic,
				Range(levelBounds._1, levelBounds._2+1).map(level =>
					(level+".max" -> ((index: TileIndex) => (level == index.getLevel())))
				).toMap + ("global.max" -> ((index: TileIndex) => true))
			)

		val tileAnalytics: Option[AnalysisDescription[TileData[JavaList[TwitterDemoRecord]],
		                                              (List[TwitterDemoRecord],
		                                               List[TwitterDemoRecord])]] =
			Some(new CompositeAnalysisDescription(minAnalysis, maxAnalysis))
		val dataAnalytics: Option[AnalysisDescription[((Double, Double),
		                                               Map[String, TwitterDemoRecord]),
		                                              Int]] =
			None

		genericProcessData(rawData, levelSets, tileIO, tileAnalytics, dataAnalytics,
		            startTime, endTime, bins, useWords,
		            pyramidId, pyramidName,
		            pyramidDescription)
	}

	private def genericProcessData[AT, DT]
		(rawData: RDD[String],
		 levelSets: Array[Array[Int]],
		 tileIO: TileIO,
		 tileAnalytics: Option[AnalysisDescription[TileData[JavaList[TwitterDemoRecord]], AT]],
		 dataAnalytics: Option[AnalysisDescription[((Double, Double),
		                                            Map[String, TwitterDemoRecord]), DT]],
		 startTime: Date,
		 endTime: Date,
		 bins: Int,
		 useWords: Boolean,
		 pyramidId: String,
		 pyramidName: String,
		 pyramidDescription: String) =
	{
		val tileAnalyticsTag: ClassTag[AT] = tileAnalytics.map(_.analysisTypeTag).getOrElse(ClassTag.apply(classOf[Int]))
		val dataAnalyticsTag: ClassTag[DT] = dataAnalytics.map(_.analysisTypeTag).getOrElse(ClassTag.apply(classOf[Int]))
		processData(rawData, levelSets, tileIO, tileAnalytics, dataAnalytics, startTime, endTime, bins, useWords, pyramidId, pyramidName, pyramidDescription)(tileAnalyticsTag, dataAnalyticsTag)
	}

	private def processData[AT: ClassTag, DT: ClassTag]
		(rawData: RDD[String],
		 levelSets: Array[Array[Int]],
		 tileIO: TileIO,
		 tileAnalytics: Option[AnalysisDescription[TileData[JavaList[TwitterDemoRecord]], AT]],
		 dataAnalytics: Option[AnalysisDescription[((Double, Double),
		                                            Map[String, TwitterDemoRecord]), DT]],
		 startTime: Date,
		 endTime: Date,
		 bins: Int,
		 useWords: Boolean,
		 pyramidId: String,
		 pyramidName: String,
		 pyramidDescription: String) =
	{
		val stopWordList = new TwitterDemoRecordParser(0L, 1L, 1).getStopWordList
		val binner = new RDDBinner
		binner.debug = true
		val tilePyramid = new WebMercatorTilePyramid
		val data = rawData.mapPartitions(i =>
			{
				val recordParser = new TwitterDemoRecordParser(startTime.getTime(),
				                                               endTime.getTime, bins)
				i.flatMap(line =>
					{
						try {
							if (useWords) {
								recordParser.getRecordsByWord(line, stopWordList)
							} else {
								recordParser.getRecordsByTag(line)
							}
						} catch {
							// Just ignore bad records, there aren't many
							case _: Throwable =>
								Seq[((Double, Double), Map[String, TwitterDemoRecord])]()
						}
					}
				).map(record =>
					(record._1, record._2, dataAnalytics.map(_.convert(record)))
				)
			}
		)
		data.cache


		levelSets.foreach(levelSet =>
			{
				println()
				println()
				println()
				println("Starting binning levels "+levelSet.mkString("[", ",", "]")+" at "+new Date())
				val startTime = System.currentTimeMillis
				val tiles = binner.processDataByLevel(data,
				                                      new CartesianIndexScheme,
				                                      new TwitterDemoBinningAnalytic,
				                                      tileAnalytics,
				                                      dataAnalytics,
				                                      tilePyramid,
				                                      levelSet,
				                                      bins=1)
				tileIO.writeTileSet(tilePyramid,
				                    pyramidId,
				                    tiles,
				                    new TwitterDemoValueDescription,
				                    tileAnalytics,
				                    dataAnalytics,
				                    pyramidName,
				                    pyramidDescription)
				val endTime = System.currentTimeMillis()
				println("Finished binning levels "+levelSet.mkString("[", ",", "]")+" at "+new Date())
				println("\telapsed time: "+((endTime-startTime)/60000.0)+" minutes")
				println()
			}
		)
	}
}
