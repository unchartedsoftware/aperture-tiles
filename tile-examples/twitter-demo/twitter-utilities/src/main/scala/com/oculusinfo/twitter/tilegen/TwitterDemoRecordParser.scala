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



import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.{List => JavaList}

import scala.collection.JavaConverters._
import scala.util.parsing.json.JSON

import com.oculusinfo.binning.util.Pair

import com.oculusinfo.twitter.binning.TwitterDemoRecord



object Sentiment extends Enumeration("negative", "neutral", "positive") {
  type SentimentType = Value
  val negative, neutral, positive = Value
}

private[tilegen]
class TwitterDemoRecordLine (val createdAt: Date,
                             val userId: String,
                             val userName: String,
                             val tweetId: String,
                             val text: String,
                             val tags: Array[String],
                             val latitude: Double,
                             val longitude: Double,
                             val country: String,
                             val placeName: String,
                             val placeType: String,
                             val sentiment: Sentiment.SentimentType,
                             val confidence: Double) {}

object TwitterDemoRecordParser {
}

class TwitterDemoRecordParser (startTime: Long, endTime: Long, timeBins: Int) {
  private val emptyBins = Range(0, timeBins).map(n => new JavaInt(0)).toList.asJava

  def getStopWordList: Set[String] = {
    val stops = List("stop-word-en", "stop-word-fr", "stop-word-sp", "stop-word-tech")
    stops.map(stop => {
      val resource = getClass().getResource("/com/oculusinfo/twitter/tilegen/"+stop+".json")
      val source = scala.io.Source.fromURL(resource)
      val text = source.mkString
      val json = JSON.parseFull(text)
      json match {
        case Some(a: List[_]) => a.map(_.toString).toSet
        case _ => Set[String]()
      }
    }).reduce(_ union _)
  }

  private def getBin (time: Long): Int = {
    math.max(0, math.min(timeBins+1, ((timeBins * (time-startTime)) / (endTime-startTime+1L)).toInt))
  }

  // Thanh's files have:
  // Column 1: created_at
  // Column 2: uwer:id or user:id_str
  // Column 3: user:screen_name
  // Column 4: id or id_str
  // Column 5: text (bounded at start and end by '|')
  // Column 6: #-separated tag list from text (not in original at all)
  // Column 7: geo:coordinates[0] or coordinates:coordinates[1]
  // Column 8: geo:coordinates[1] or coordinates:coordinates[0]
  // Column 9: place:country
  // Column 10: place:full_name
  // Column 11: place: place_type
  //
  // Scott adds:
  // Column 12: sentiment tag
  // Column 13: sentiment confidence

  private val dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZ yyyy")

  private def splitTags (tagsList: String): Array[String] =
    tagsList.split("#").filter(!_.isEmpty)

  private[tilegen]
  def parseLine (line: String): TwitterDemoRecordLine = {
    val fields = line.split("\t")
    new TwitterDemoRecordLine(dateParser.parse(fields(0)),
                              fields(1),
                              fields(2),
                              fields(3),
                              fields(4),
                              splitTags(fields(5)),
                              fields(6).trim.toDouble,
                              fields(7).trim.toDouble,
                              fields(8),
                              fields(9),
                              fields(10),
                              Sentiment.withName(fields(11).trim.toLowerCase),
                              fields(12).trim.toDouble)
  }


  def getRecordsByTag (line: String):
  Seq[(Double, Double, Map[String, TwitterDemoRecord])] =
    getRecords(recordLine => recordLine.tags)(line)

  def getRecordsByWord (line: String, stopWordList: Set[String]):
  Seq[(Double, Double, Map[String, TwitterDemoRecord])] = {
    val doubleChars = Set('.', 'e', 'E', ',', '-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    getRecords(recordLine => {
      // For each line:
      //   Split to words
      //   for each word:
      //     Convert to lower case
      //     Make sure it's not empty
      //     Make sure it's not simply a number
      //     Make sure it's not on our stop-words list
      recordLine.text.split("[^a-zA-Z_0-9']").map(_.toLowerCase).toSet
	.filter(!_.isEmpty)
        .filter(_.map(!doubleChars.contains(_)).reduce(_ || _))
	.filter(!stopWordList.contains(_))
    })(line)
  }

  private def getRecords (wordFcn: TwitterDemoRecordLine => Iterable[String])
			 (line: String): Seq[(Double, Double, Map[String, TwitterDemoRecord])] = {
    val recordLine = parseLine(line)
    val time = recordLine.createdAt.getTime()
    val bin = getBin(time)
    val empty = emptyBins
    val full = new ArrayList[JavaInt](emptyBins)
    full.set(bin, 1)

    val (posCount, posBins,
	 neutCount, neutBins,
	 negCount, negBins) = recordLine.sentiment match {
      case Sentiment.positive => (1, full,  0, empty, 0, empty)
      case Sentiment.neutral =>  (0, empty, 1, full,  0, empty)
      case Sentiment.negative => (0, empty, 0, empty, 1, full)
    }

    val textTime = new Pair[String, JavaLong](recordLine.text, time)
    val textTimeList = List[Pair[String, JavaLong]](textTime).asJava
    Seq((recordLine.longitude, recordLine.latitude,
	 wordFcn(recordLine).map(tag => {
	   (tag, new TwitterDemoRecord(tag, 1, full,
				       posCount, posBins, neutCount,
				       neutBins, negCount, negBins,
				       textTimeList))
	 }).toMap))
  }
}
