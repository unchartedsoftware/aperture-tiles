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

object NBAplaybyplay extends App {

  def run(sc: SparkContext){
    
   var i = 0

   val refData = scala.io.Source.fromFile("playbyplay.txt").getLines().next.toString
   val JSONList = refData.replace("\"","").split(",")
  
  var files = ""

   for (files <- JSONList){

    val table = sc.textFile("hdfs://xd-namenode.xdata.data-tactics-corp.com:/SummerCamp2014/nba/playbyplay/" + files + ".json").collect()
    val data = new JSONObject(table(0))
    val res1 = new JSONArray(data.get("resultSets").toString)
    val res2 = new JSONObject(res1.get(0).toString)
    val res3 = new JSONArray(res2.get("rowSet").toString)
  
    val rowSetLength = res3.length()

    var runningTable = if(res3.length > 0){res3.get(0).toString.replace("[","").replace("]","").replace("\"","")} else {""}
var row = ""

   for(row <- 1 to (rowSetLength - 1)){ 
    runningTable = runningTable + "\n" + res3.get(row).toString.replace("[","").replace("]","").replace("\"","")
    }

    sc.parallelize(runningTable.split("\n")).coalesce(1).saveAsTextFile("hdfs://xd-namenode.xdata.data-tactics-corp.com:/user/mkielo/nba/playbyplay/"+i)

    i = i + 1
}

//  val res5 = res4.get(0)
//.get(0).get("rowSet").get(0).get(0).toString

 }
//  def run(table: RDD[Array[String]], field: String, index: Int, customVars: String, writer: PrintWriter, partitionNum: String){
	   
  // val startTime = System.currentTimeMillis
//	
   //	val logwriter = new PrintWriter(new File("output/logs.txt"))
   //	val sc = table.context	
  // 	val t1 = System.currentTimeMillis - startTime
 //	val refDB = buildRefDB
 //	val refBroadcast = sc.broadcast(refDB)
 //	val t2 = System.currentTimeMillis - startTime
 //	val weightDB = buildWeightDB
 //	val t3 = System.currentTimeMillis - startTime
//	val weightBroadcast = sc.broadcast(weightDB)
//	val t4 = System.currentTimeMillis - startTime
//	val t5 = System.currentTimeMillis - startTime
 //
//	val newTable = table.map(r => {
 //	  	val a = scorer(r(index), refBroadcast.value, weightBroadcast.value)
 //		var outString = a._1.toString + ","  
//		a._2.foreach(test => {
 //	   	 outString += test._2.toString + ","
 //	    	})
//	      r(0) + "\t" + r(1) + "\t" + r(2) + "\t" + outString
 //	  })
 //	
   //     println(newTable.toDebugString)  
 //	newTable.saveAsTextFile(customVars + partitionNum)
//
//	val t6 = System.currentTimeMillis - startTime  	
//
//	val t7 = System.currentTimeMillis - startTime
 //	writer.close
//	logwriter.write("T1: " + t1 + "\nT2: " + t2 + "\nT3: " + t3 + "\nT4: " + t4 + "\nT5: " + t5 + "\nT6: " + t6 + "\nT7: " + t7) 
//	logwriter.close
//}
 }
