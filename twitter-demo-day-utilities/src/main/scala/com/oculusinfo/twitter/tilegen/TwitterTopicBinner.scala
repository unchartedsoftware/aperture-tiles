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

import com.oculusinfo.binning.impl.WebMercatorTilePyramid

import com.oculusinfo.tilegen.spark.MavenReference
import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.TileIO
import com.oculusinfo.tilegen.util.ArgumentParser

import com.oculusinfo.twitter.binning.TwitterDemoTopicRecord



object TwitterTopicBinner {
  def main (args: Array[String]) {
    val argParser = new ArgumentParser(args)
    argParser.debug

    val jars = 
      Seq(new MavenReference("com.oculusinfo", "twitter-utilities", "0.3-SNAPSHOT")
      ) union SparkConnector.getDefaultLibrariesFromMaven
    val sc = argParser.getSparkConnector(jars).getSparkContext("Twitter demo data tiling")
    val source = argParser.getString("source", "The source location at which to find twitter data")
    val dateParser = new SimpleDateFormat("yyyy/MM/dd.HH:mm:ss.zzzz")
    //Note:  don't need a start time for this binning project.  Start time is assumed to be 31 days prior to end time.
    //val startTime = dateParser.parse(argParser.getString("start", "The start time for binning.  Format is yyyy/MM/dd.HH:mm:ss.zzzz"))
    val endTime = dateParser.parse(argParser.getString("end", "The end time for binning.  Format is yyyy/MM/dd.HH:mm:ss.zzzz"))
    val bins = argParser.getInt("bins", "The number of time bins into which to divide the time range?", Some(1))
    //val stopWordList = new TwitterTopicRecordParser(0L, 1L, 1).getStopWordList	//don't need to use stop-words for this demo

    val levelSets = argParser.getString("levels", "The level sets (;-separated) of ,-separated levels to bin.").split(";").map(_.split(",").map(_.toInt))

    val pyramidId = argParser.getString("id", "An ID by which to identify the finished pyramid.")
    val pyramidName = argParser.getString("name", "A name with which to label the finished pyramid").replace("_", " ")
    val pyramidDescription = argParser.getString("description", "A description with which to present the finished pyramid").replace("_", " ")
    val partitions = argParser.getInt("partitions", "The number of partitions into which to read the raw data", Some(0))
    //val useWords = argParser.getBoolean("words", "If true, pull out all non-trival words from tweet text; if false, just pull out tags.", Some(false))
    val topicList = argParser.getString("topicList", "Path and filename of list of extracted topics and English translations")
    
    val binner = new RDDBinner
    binner.debug = true
    val binDesc = new TwitterTopicBinDescriptor
    val tilePyramid = new WebMercatorTilePyramid
    val tileIO = TileIO.fromArguments(argParser)

    val rawData = if (0 == partitions) {
      sc.textFile(source)
    } else {
      sc.textFile(source, partitions)
    }
    
    val endTimeSecs = endTime.getTime()/1000;	// convert time from msec to sec 
    val topicMatcher = new TopicMatcher
    val topicsMap = topicMatcher.getKeywordList(topicList)	// get pre-extracted topics 
  
    // append topics to end of data entries
    val rawDataWithTopics = topicMatcher.appendTopicsToData(sc, rawData, topicsMap)    
   
    val data = rawDataWithTopics.mapPartitions(i => {     
      val recordParser = new TwitterTopicRecordParser(endTimeSecs, bins)
      i.flatMap(line => {
    	  try {
    	      //val N = recordParser.getNumTopics(line)	//TODO ... to handle multiple topics per line??
    	      //for (n 1 to N) {
    	      //	  recordParser.getRecordsByTopic(line, n)
    	      //}
    		  recordParser.getRecordsByTopic(line)
    		  
    	  } catch {
    		  // Just ignore bad records, there aren't many
    	  	case _: Throwable => Seq[(Double, Double, Map[String, TwitterDemoTopicRecord])]()
    	  }
      })
    })
    data.cache

    levelSets.foreach(levelSet => {
      println()
      println()
      println()
      println("Starting binning levels "+levelSet.mkString("[", ",", "]")+" at "+new Date())
      val startTime = System.currentTimeMillis
      val tiles = binner.processDataByLevel(data, binDesc, tilePyramid, levelSet, bins=1)
      tileIO.writeTileSet(tilePyramid, pyramidId, tiles, binDesc, 
			  pyramidName, pyramidDescription)
      val endTime = System.currentTimeMillis()
      println("Finished binning levels "+levelSet.mkString("[", ",", "]")+" at "+new Date())
      println("\telapsed time: "+((endTime-startTime)/60000.0)+" minutes")
      println()
    })
  }
}
