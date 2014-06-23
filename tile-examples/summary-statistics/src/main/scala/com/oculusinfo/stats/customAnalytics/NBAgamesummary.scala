  /*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.oculusinfo.stats.customAnalytics

import java.io._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

import org.apache.spark.AccumulableParam
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import org.json.JSONWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.io.File
import java.io.FileWriter
import java.io.FileOutputStream
import scala.collection.mutable.ArrayBuffer

object NBAgamesummary extends App {

  
  def parseJson(myjson: JSONObject, i: String, field: String, file: String, sc: SparkContext){
    //val res2 = new JSONObject(myjson.get(0).toString)
      val res3 = new JSONArray(myjson.get("rowSet").toString)
  
      val rowSetLength = res3.length()
  
      var runningTable = if(res3.length > 0){file + "," + res3.get(0).toString.replace("[","").replace("]","").replace("\"","")} else {""}
        
    var row = ""
  
      for(row <- 1 to (rowSetLength - 1)){
        runningTable = runningTable + "\n" + file + "," + res3.get(row).toString.replace("[","").replace("]","").replace("\"","")
      }
  
      sc.parallelize(runningTable.split("\n")).coalesce(1).saveAsTextFile("hdfs://xd-namenode.xdata.data-tactics-corp.com:/user/mkielo/nba/gamestats/"+ field + "/" + i)

  }

  def run(sc: SparkContext){

   val refData = scala.io.Source.fromFile("gamesummaries.txt").getLines().next.toString
   val JSONList = refData.replace("\"","").split(",")
   var i = 0
    var files = ""

   for (files <- JSONList){

    val table = sc.textFile("hdfs://xd-namenode.xdata.data-tactics-corp.com:/SummerCamp2014/nba/gamestats/" + files + ".json").collect()
    val data = new JSONObject(table(0))
    val res1 = new JSONArray(data.get("resultSets").toString)

    val gamesum = new JSONObject(res1.get(0).toString)
    val linescore = new JSONObject(res1.get(1).toString)
    val seasonseries = new JSONObject(res1.get(2).toString)
    val lastmeeting = new JSONObject(res1.get(3).toString)
    val playerstats = new JSONObject(res1.get(4).toString)
    val teamstats = new JSONObject(res1.get(5).toString)
    val otherstats = new JSONObject(res1.get(6).toString)
    val officials = new JSONObject(res1.get(7).toString)
    val gameinfo = new JSONObject(res1.get(8).toString)
    val inactiveplayers = new JSONObject(res1.get(9).toString)

    parseJson(gamesum, i.toString, "game-summary", files, sc)
    parseJson(linescore, i.toString, "line-score", files, sc)
    parseJson(seasonseries, i.toString, "season-series", files, sc)
    parseJson(lastmeeting, i.toString, "last-meeting", files, sc)
    parseJson(playerstats, i.toString, "player-stats", files, sc)
    parseJson(teamstats, i.toString, "team-stats", files, sc)
    parseJson(otherstats, i.toString, "other-stats", files, sc)
    parseJson(officials, i.toString, "officials", files, sc)
    parseJson(gameinfo, i.toString, "game-info", files, sc)
    parseJson(inactiveplayers, i.toString, "inactive-players", files, sc)

  i = i + 1
  }

  } 
 
}
