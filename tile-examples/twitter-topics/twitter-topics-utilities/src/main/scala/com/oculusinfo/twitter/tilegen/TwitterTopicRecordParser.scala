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

import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.{List => JavaList}

import scala.collection.JavaConverters._
import scala.util.parsing.json.JSON

import com.oculusinfo.factory.util.Pair

import com.oculusinfo.twitter.binning.{RecentTweet, TwitterDemoTopicRecord}

private[tilegen]
class TwitterTopicRecordLine (val createdAt: Date,
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
                              val topics: Array[String],
                              val topicsEng: Array[String]) {}

object TwitterTopicRecordParser {
}

class TwitterTopicRecordParser (endTimeSecs: Long) {

  // South American Twitter Dataset has:
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
  
  // Appended data...
  // Column 12: topics (original language)
  // Column 13: topics (English)

  //private val dateParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZ yyyy")
  private val dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")	  
  
  private def splitTags (tagsList: String): Array[String] =
    tagsList.split("#").filter(!_.isEmpty)

  private[tilegen]
  def parseLine (line: String): TwitterTopicRecordLine = {
    val fields = line.split("\t")
    new TwitterTopicRecordLine(dateParser.parse(fields(0)),
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
                              fields(11).split(","),
                              fields(12).split(","))
  }

  def getRecordsByTopic (line: String):								
    Seq[((Double, Double), Map[String, TwitterDemoTopicRecord])] = {
    val recordLine = parseLine(line)
    val time = (recordLine.createdAt.getTime()*0.001).toLong	// convert from msec to sec

    if ((endTimeSecs - time > 0L) && (endTimeSecs - time <= 2678400L)) {
    	// tweet time is valid time interva (i.e., within 1 month prior to endTime)    
	    val textTime = new RecentTweet(recordLine.text, time, recordLine.userName, "")

	    val newRecordsMap =
	    	recordLine.topics
	    	          .zip(recordLine.topicsEng)		// Combine raw topic and translation
	                  .map{case (raw: String, english: String) => {
	                    	 							// Change topic/translation pair to a topic record
	    	val textTimeList = List[RecentTweet](textTime).asJava
	    	(raw -> new TwitterDemoTopicRecord(raw, english, textTimeList, endTimeSecs))
	    }}.toMap
	
	    Seq(((recordLine.longitude, recordLine.latitude), newRecordsMap))
    } else {
    	// tweet time is invalid, so disregard
    	Seq[((Double, Double), Map[String, TwitterDemoTopicRecord])]()
    }
  }    	  	
}
