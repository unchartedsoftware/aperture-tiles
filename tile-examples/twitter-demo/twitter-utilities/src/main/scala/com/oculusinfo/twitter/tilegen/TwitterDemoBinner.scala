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
 
package com.oculusinfo.twitter.tilegen



import java.text.SimpleDateFormat
import java.util.Date

import com.oculusinfo.binning.impl.WebMercatorTilePyramid

import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.tiling.RDDBinner
import com.oculusinfo.tilegen.tiling.TileIO

import com.oculusinfo.twitter.binning.TwitterDemoRecord



object TwitterDemoBinner {
  def main (args: Array[String]) {
    val argParser = new ArgumentParser(args)
    argParser.debug

    val sc = argParser.getSparkConnector.getSparkContext("Twitter demo data tiling")
    val source = argParser.getStringArgument("source", "The source location at which to find twitter data")
    val dateParser = new SimpleDateFormat("yyyy/MM/dd.HH:mm:ss.zzzz")
    val startTime = dateParser.parse(argParser.getStringArgument("start", "The start time for binning.  Format is yyyy/MM/dd.HH:mm:ss.zzzz"))
    val endTime = dateParser.parse(argParser.getStringArgument("end", "The end time for binning.  Format is yyyy/MM/dd.HH:mm:ss.zzzz"))
    val bins = argParser.getIntArgument("bins", "The number of time bins into which to divide the time range?", Some(1))
    val stopWordList = new TwitterDemoRecordParser(0L, 1L, 1).getStopWordList

    val levelSets = argParser.getStringArgument("levels", "The level sets (;-separated) of ,-separated levels to bin.").split(";").map(_.split(",").map(_.toInt))

    val pyramidId = argParser.getStringArgument("id", "An ID by which to identify the finished pyramid.")
    val pyramidName = argParser.getStringArgument("name", "A name with which to label the finished pyramid").replace("_", " ")
    val pyramidDescription = argParser.getStringArgument("description", "A description with which to present the finished pyramid").replace("_", " ")
    val partitions = argParser.getIntArgument("partitions", "The number of partitions into which to read the raw data", Some(0))
    val useWords = argParser.getBooleanArgument("words", "If true, pull out all non-trival words from tweet text; if false, just pull out tags.", Some(false))

    val binner = new RDDBinner
    binner.debug = true
    val binDesc = new TwitterDemoBinDescriptor
    val tilePyramid = new WebMercatorTilePyramid
    val tileIO = TileIO.fromArguments(argParser)

    val rawData = if (0 == partitions) {
      sc.textFile(source)
    } else {
      sc.textFile(source, partitions)
    }
    val data = rawData.mapPartitions(i => {
      val recordParser = new TwitterDemoRecordParser(startTime.getTime(), endTime.getTime, bins)
      i.flatMap(line => {
	try {
          if (useWords) {
	    recordParser.getRecordsByWord(line, stopWordList)
          } else {
            recordParser.getRecordsByTag(line)
          }
	} catch {
	    // Just ignore bad records, there aren't many
	    case _ => Seq[(Double, Double, Map[String, TwitterDemoRecord])]()
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
