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

package com.oculusinfo.stats.numeric

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

object Quartiles {

  def getQuartile(sortedRDD: RDD[Double], size: Long, countPartitions: (Int, Iterator[Double]) => Iterator[(Int, Int)], quartile: Int): Double = {

    val quartileSpot = if (quartile == 1) {
      (1 * (size + 1)).toDouble / 4.toDouble
    } else if (quartile == 2) {
      (size + 1).toDouble / 2.toDouble
    } else {
      (3 * (size + 1)).toDouble / 4.toDouble
    }

    val partitionEntryCountsRDD = sortedRDD.mapPartitionsWithIndex(countPartitions).collect

    var i = -1
    var p = 0

    while (p < quartileSpot) {
      i += 1
      p += partitionEntryCountsRDD(i)._2
    }

    val b = p - partitionEntryCountsRDD(i)._2
    val aoi = quartileSpot - b

    //we want the Cth spot of partition i in testPA

    sortedRDD.mapPartitionsWithIndex((index, iter) =>
      {
        println(index)
        if (index == i && aoi.isWhole) {
          //			iter.foreach(println)
          println(88)
          iter.drop(aoi.toInt - 1).take(1)

        } else if (index == i && !aoi.isWhole) {
          val lower = iter.drop((aoi - 1.5).toInt).take(1).next
          val higher = iter.take(1).next
          println(99)
          println(lower)
          println(higher)
          println(((lower + higher).toDouble / 2.toDouble))
          List(((lower + higher).toDouble / 2.toDouble)).iterator
        } else {
          println(1)
          List().iterator
        }
      }).first
  }

  def getAllQuartiles(data: RDD[String], sorted: Boolean) = {

    val numericData = data.map(r => r.toDouble)

    val sortedRDD = if (!sorted) { numericData.map(r => (r, 1)).sortByKey().map(r => r._1) } else numericData
    val size = sortedRDD.count

    val countPartitions = (partitions: Int, iter: Iterator[Any]) => List((partitions, iter.count(Any => true))).iterator

    //median is at odd position

    val first = getQuartile(sortedRDD, size, countPartitions, 1)
    val second = getQuartile(sortedRDD, size, countPartitions, 2)
    val third = getQuartile(sortedRDD, size, countPartitions, 3)

    (first, second, third)

  }
}