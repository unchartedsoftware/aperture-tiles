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


class TopicMatcher {
  
    //  private val dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")    
  
    // create a map of twitter keywords (key) and their English translations (value)
    def getKeywordList(keywordFile: String): Map[String, String] = {

        //val resource = getClass().getResource("/com/oculusinfo/twitter/tilegen/extractedKeywords.txt")
        val resource = getClass.getResource(keywordFile)
        val source = scala.io.Source.fromURL(resource, "UTF-8")   // need to make sure reading in topics as UTF-8
        var topicsMap:Map[String,String] = Map() // = Map[String, String]
        for ( line <- source.getLines() ) {   // iterate through all lines in txt file
            // split line by tabs (0th element is original topic, 1st is English topic, and 2nd is topic count)
            val lineArray = line.split("\t")
            if (lineArray.length > 1) {
                topicsMap += (lineArray(0) -> lineArray(1))   // store original topic as map key, and translated topic as value
            }
        }
        topicsMap
    }
  
  
  def appendTopicsToData(sc: SparkContext, raw: RDD[String], topicsMap:  Map[String, String], endTimeSecs: Long): RDD[String] = {
        
        val bTopics = sc.broadcast(topicsMap)   // broadcast topics list to all workers
    
        raw.map(line => {
         
            val tabbedData = line.split("\t")     // separate data by tabs (output is array of strings)
            var tweet = ""
            try {
                tweet = tabbedData(4) // get 5th element
                // exclude | | on either side of twitter message and convert to lower case
                tweet = tweet.substring(1, tweet.length()-1).toLowerCase
            } catch {
                case _: Throwable => " "
            }

            // remove punctuation
            tweet = tweet.replace(","," ").replace("."," ").replace("!"," ").replace("?"," ").replace(":", " ").replace("("," ").
                          replace(")"," ").replace("["," ").replace("]"," ").replace("\""," ").replace("@", " ").replace("#"," ")

            val words = tweet.split(" ")  //split into words

            val foundTopics = bTopics.value.filterKeys(words.contains(_))     // find matches with keyword list (returns a map with matching keywords)

            if (foundTopics.size == 0) {
                line.substring(0,0) // replace with an empty string if no topic matches have been found
            } else {
                var topics = ""       // build a string of found topics and translated topics (comma separated)
                var topicsEnglish = ""
                foundTopics.keys.foreach { n =>
                    topics += n + ","
                    topicsEnglish += foundTopics(n) + ","
                }

                // remove all whitespace at end of line and append found topics to end...
                //(NO! Don't remove whitespace at end, because many records have tabs with empty fields at
                //end of the line for country, full_name, place_type, etc. ... and we still need to keep these
                //fields even if they are empty otherwise the record will be classified as 'bad' during record parsing!)
                //line.replaceFirst("\\s+$", "").concat("\t" + topics + "\t" + topicsEnglish)

                //append found topics to end
                line.concat("\t" + topics + "\t" + topicsEnglish)
            }
        }).filter(line => {   // discard empty lines
            !line.isEmpty
        })
    }

}