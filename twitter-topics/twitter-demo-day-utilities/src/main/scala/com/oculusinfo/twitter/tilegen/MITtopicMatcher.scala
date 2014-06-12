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
import java.lang.{Integer => JavaInt}
import java.util.{List => JavaList}
import java.util.ArrayList
import scala.collection.JavaConverters._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD


class MITtopicMatcher {	
  
  // create a map of twitter keywords (key) and their English translations (value)
  def getKeywordList(keywordFile: String): List[(String, String)] = {

      //val resource = getClass().getResource("/com/oculusinfo/twitter/tilegen/extractedKeywords.txt")
      val resource = getClass().getResource(keywordFile)
      val source = scala.io.Source.fromURL(resource, "UTF-8")	// need to make sure reading in topics as UTF-8
      //var topicsMap:List[(String,String)] = List() // = Map[String, String]
      var fileLines = source.getLines.toList
      var topicsMap = fileLines.map { line => 	// iterate through all lines in txt file
      	val lineArray = line.split("\t")	// split line by tabs (0th element is original topic, 1st is English topic)
      	(lineArray(0), lineArray(1))		// store as a List of (String, String) pairs
      }	//.toArray
      source.close()
      return topicsMap
  }
  
  // create a map of tweet IDs to Topics
  def getTweetIDtoTopicsMap(sc: SparkContext, tweetIdFile: String, partitions: Int): RDD[(String, Int)] = {

    val tweetIDdata = if (0 == partitions) {
      sc.textFile(tweetIdFile)
    } else {
      sc.textFile(tweetIdFile, partitions)
    }
    //val tweetIDdata2 = sc.parallelize(tweetIDdata.take(50000))	//temp
    //val tabbedData = tweetIDdata2.map(line => line.split("\t"))	//temp
    val tabbedData = tweetIDdata.map(line => line.split("\t"))	// split line by tabs
    
    // create a pair with tweetID, and topic index number
    val pairedData = tabbedData.map(fields => {
      try {
        (fields(0), fields(3).toInt)
      } catch {
        case _: Throwable => ("", -1)
      }
    })
    
    pairedData.filter(pairs => pairs._2 != -1)	// discard entries with topic index = -1 (meaning no topic)
  }
  
  
  def appendTopicsToData(sc: SparkContext, raw: RDD[String], topicsMap:  List[(String, String)], tweetIdMap: RDD[(String, Int)], endTimeSecs: Long): RDD[String] = {
	
		val bTopics = sc.broadcast(topicsMap)	// broadcast topics list to all workers
	    
	    val rawDataPairs = raw.map(line => {     
	      val tabbedData = line.split("\t")	// split raw data lines by tabs
	      try {
	        (tabbedData(3), line)			// create (String, String) pair of (tweetID, raw data line)
	      } catch {
	        case _: Throwable => ("", "")
	      }    
	    })
	    
	    val joinedData = tweetIdMap.join(rawDataPairs)	// join raw data with topicsMap.  Output is (tweetID, (topicIndex, rawDataLine))
	    
	    joinedData.map(data => {
	    	val topicIndex = data._2._1
	    	val rawDataLine = data._2._2
	    	if (topicIndex >= 4) {	// topic indices 0 to 3 represent invalid topics (i.e., just language markers, etc.)
	    		val currentTopics = (bTopics.value)(topicIndex)	// string pair of topic and Eng topic
	    		//append to raw data line with tabs and commas (since that's what twitterTopic parser class expects)
	    		rawDataLine.concat("\t" + currentTopics._1 + ",\t" + currentTopics._2 + ",")
	    	} else {
	    		rawDataLine.substring(0,0) // replace raw data line with an empty string if no topic matches have been found
	    	}    
	    }).filter(line => {	// discard empty lines
		  !(line.isEmpty)
		})
  }
 
}