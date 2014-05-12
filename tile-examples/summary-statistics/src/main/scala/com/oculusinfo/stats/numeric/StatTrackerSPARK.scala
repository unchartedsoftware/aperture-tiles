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

/**
 * @author $mkielo
 */


import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._


class StatTracker(val count: Long, val sumX: Double, val sumXX: Double, val min: Option[Double], val max: Option[Double]) extends Serializable{

    /**
    * Add a new datum to the series tracked. Statistics on this datum are stored,
    * but the datum itself is forgotten.
    **/
  def addStat(value: Double): StatTracker = {
    val newCount = count + 1
    val newSumX = sumX + value
    val newSumXX = sumXX + (value * value)

    val newMin = (max, value) match {
      case (None, value) => Some(value)
      case (Some(a), value) => Some(a min value)
    }

    val newMax = (max, value) match {
      case (None, value) => Some(value)
      case (Some(a), value) => Some(a max value)
    }

    new StatTracker(newCount, newSumX, newSumXX, newMin, newMax)
  }


    /**
    * Add a two tracked series. Statistics on this datum are stored,
    * but the datum itself is forgotten.
    **/
  def addStats(values: StatTracker): StatTracker = {
    val newCount = count + values.getCount()
    val newSumX = sumX + values.getSum()
    val newSumXX = sumXX + values.getSumXX()// * values.getSum()

    val newMin = (min, values.min) match {
      case (None, None) => None
      case (Some(a), None) => Some(a)
      case (None, Some(a)) => Some(a)
      case (Some(a), Some(b)) => Some(a min b)
    }

    val newMax = (max, values.max) match {
      case (None, None) => None
      case (Some(a), None) => Some(a)
      case (None, Some(a)) => Some(a)
      case (Some(a), Some(b)) => Some(a max b)
    }

    new StatTracker(newCount, newSumX, newSumXX, newMin, newMax)
  }

  
  def getSum(): Double = {
    sumX
  }
  
  def getSumXX(): Double = {
    sumXX
  }

  def getCount(): Long = {
    count
  }

  def getMean(): Double = {
    sumX / count
  }

  def getMin(): Option[Double] = {
    min
  }

  def getMax(): Option[Double] = {
    max
  }

  def getPopulationVariance(): Double = {
    val mean = getMean()
    (sumXX / count) - mean * mean
  }

  def getPopulationStdDeviation(): Double = {
    val popVariance = getPopulationVariance()
    math.sqrt(popVariance)
  }

  def getSampleVariance(): Double = {
    val mean = getMean()
    (sumXX / (count - 1)) - mean * mean * count / (count - 1)
  }

  def getSampleStdDeviation(): Double = {
    val popVariance = getSampleVariance()
    math.sqrt(popVariance)

  }

}