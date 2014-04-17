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

import org.apache.spark.AccumulableParam
import org.apache.spark.AccumulatorParam
import org.apache.spark.SparkContext._

/**
 * @author $mkielo
 */


//used to be: object StatTrackerAccumulable extends AccumulalableParam[StatTracker]{
//
//object StatTrackerAccumulable extends AccumulableParam[StatTracker, Double]{
// 
//  def zero (initialValue: StatTracker): StatTracker = {
//	  new StatTracker(0, 0, 0, None, None)
//	}
//  
//  def addAccumulator(currentValue: StatTracker, addition: Double): StatTracker = {
//    currentValue.addStat(addition)
//  }
//  
//  def addInPlace(Acc1: StatTracker, Acc2: StatTracker): StatTracker = {
//    Acc1.addStats(Acc2)
//  }
//  
//}
//    Array(n, sumX, sumXSquared, min, max)
//  }
//
//  def addInPlace(Acc1: Array[Double], Acc2: Array[Double]) = {
//    val n = Acc1(0) + Acc2(0)
//    val sumX = Acc1(1) + Acc2(1)
//    val sumXSquared = sumX * sumX
//    var min = Acc1(3)
//    var max = Acc1(4)
//
//    if (min == null || Acc2(3) < min) {
//      min = Acc2(3)
//    }
//    if (max == null || max < Acc2(4)) {
//      max = Acc2(4)
//    }
//
//    Array(n, sumX, sumXSquared, min, max)
//  }
//
//  def zero(initialValue: Double) = {
//    val n = 0
//    val sumX = 0
//    val sumXSquared = 0
//    val min = null
//    val max = null
//    Array(n, sumX, sumXSquared, min, max)
//  }*/