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



package com.oculusinfo.stats.qualitative

import org.apache.spark._
import SparkContext._
import org.apache.spark.rdd.RDD

/**
 * @author $mkielo
 */


object Frequency {

  def FrequencyTable(column: Int, textFile: RDD[Array[String]]) = {
    textFile.map(line => (line(column), 1)).reduceByKey(_ + _, 1)
  }

  def getFrequency(column: Int, textFile: RDD[Array[String]], key: String) = {
    textFile.map(line => line(column)).filter(_.equals(key)).map(record => 1).reduce(_ + _)
  }


   def MostFrequent(returnNum: Int, column: Int, textFile: RDD[Array[String]], sorted: Boolean): Array[(Int, String)] = { //make sorted default to false if not specified. make the function work if not specified

    val freqTable = FrequencyTable(column, textFile).map(_.swap).sortByKey(false) 
    val tieCheck = freqTable.take(100 + returnNum)
    val rowCount = tieCheck.length
    var tieNum = 0 

    println(rowCount)
    for (i <- 0 to (rowCount - returnNum - 1)) {
        if (tieCheck(returnNum - 1)._1 == tieCheck(returnNum + i)._1) {
          tieNum += 1
        }
      }

    if(tieNum == 100){
      val tiedFreq = tieCheck(returnNum - 1)._1 
      var cleanedNum = returnNum
      for (i <- 2 to returnNum){
        if(tieCheck(returnNum - 1)  == tieCheck(returnNum - i)){
          cleanedNum -= 1
        }
      }
      freqTable.take(cleanedNum):+ ((tiedFreq,"FREQUENCY INFO: There are atleast 100 other values with the frequency: "))     
    }

    else {
      freqTable.take(returnNum + tieNum)}
   }
   

}