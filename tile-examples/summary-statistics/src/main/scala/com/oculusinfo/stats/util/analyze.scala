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

package com.oculusinfo.stats.util

import java.io.FileInputStream
import java.util.Properties
import java.io._
import scala.collection.JavaConversions.asScalaSet
import org.apache.spark.SparkContext
import com.oculusinfo.stats.numeric.StatTracker
import com.oculusinfo.stats.numeric.Quartiles
import com.oculusinfo.stats.qualitative.CountQualities
import com.oculusinfo.stats.qualitative.Frequency
import com.oculusinfo.stats.customAnalytics._ // get rid of this...
import org.apache.spark.AccumulableParam
import org.apache.spark.rdd.RDD
import scala.collection.mutable.ListBuffer
import scala.collection.mutable

object analyze {

  implicit object StatTrackerAccumulable extends AccumulableParam[StatTracker, Double] with Serializable {

    def zero(initialValue: StatTracker): StatTracker = {
      new StatTracker(0, 0, 0, None, None)
    }

    def addAccumulator(currentValue: StatTracker, addition: Double): StatTracker = {
      currentValue.addStat(addition)
    }

    def addInPlace(Acc1: StatTracker, Acc2: StatTracker): StatTracker = {
      Acc1.addStats(Acc2)
    }
  }
  def tableResults(fulltable: RDD[Array[String]], tableTests: String, writer: PrintWriter): Map[String, String] = {
    val table = fulltable.map(r => r.mkString(""))
	val tests: Array[String] = tableTests.split(",")
    val sc = table.context

    val tableTestResults = collection.mutable.Map.empty[String, String]

    if (tests.contains("totalrecords")) {
      val totalCount = table.count()
      writer.write("Total record count: " + totalCount + "\n")
      tableTestResults("totalRecords") = totalCount.toString
    }

    if (tests.contains("uniquerecords")) {
      val totalUnique = table.distinct().count()
      writer.write("Total record count: " + totalUnique + "\n")
      tableTestResults("uniqueRecords") = totalUnique.toString
    }

    if (!tests.contains("nocorruptcheck")){
	val unparseable = table.filter(r => (r.contains("%% corrupt data check failure invalid length %%"))).map(r => 1).reduce(_ + _)
	writer.write("Total unparsable: " + unparseable + "\n")
	tableTestResults("corruptRecords") = unparseable.toString
	}
	if (tests.contains("bytes")) {

    }
	

    writer.write("\n")

    tableTestResults.toMap

  }

  //

  def qualitativeResults(table: RDD[String], field: String, fieldAlias: String, fieldType: String, testList: String, writer: PrintWriter, dateFormat: String): (String, String, String, Map[String, Any]) = {

     val tests: Array[String] = testList.split(",")
     val sc = table.context

     var statTracker = sc.accumulable(StatTrackerAccumulable.zero(new StatTracker(0, 0, 0, None, None)))

     writer.write("Field: " + field + "\nFieldtype: Qualitative\nRunning tests: " + tests(0))
     for (i <- 1 to (tests.length - 1)) {
       writer.write(", " + tests(i))
     }

     if (tests.contains("min")) { writer.write("\nWARNING Test: min could not be run on quantitative field type") }
     if (tests.contains("max")) { writer.write("\nWARNING Test: max could not be run on quantitative field type") }
     if (tests.contains("sum")) { writer.write("\nWARNING Test: sum could not be run on quantitative field type") }
     if (tests.contains("sumxx")) { writer.write("\nWARNING Test: sumXX could not be run on quantitative field type") }
     if (tests.contains("mean")) { writer.write("\nWARNING Test: mean could not be run on quantitative field type") }
     if (tests.contains("popvar")) { writer.write("\nWARNING Test: popVar could not be run on quantitative field type") }
     if (tests.contains("popstd")) { writer.write("\nWARNING Test: popSTD could not be run on quantitative field type") }
     if (tests.contains("sampvar")) { writer.write("\nWARNING Test: sampVar could not be run on quantitative field type") }
     if (tests.contains("sampstd")) { writer.write("\nWARNING Test: sampSTD could not be run on quantitative field type") }

     val resultMap = collection.mutable.Map.empty[String, Any]

     if (!tests.contains("nocorruptcheck")){
       val corrupt = table.filter(r => (r.contains("%% corrupt data check failure bad field type %%"))).map(r => 1).reduce(_ + _)
       resultMap("corruptRecords") = corrupt.toString
     }
     
     if (tests.contains("countna")) {
       val countNA = CountQualities.CountNASave(table, sc)
       writer.write("\nTest: CountNA, Result: " + countNA)
       resultMap("countNA") = countNA.toString
     }
     if (tests.contains("countunique")) {
       val countUnique = CountQualities.CountUnique(table)
       writer.write("\nTest: CountUnique, Result: " + countUnique)
       resultMap("countUnique") = countUnique.toString
     }
     if (tests.contains("mostfrequent")) {
       val mostFrequent = Frequency.MostFrequent(5, table, false).map(_.swap)
       writer.write("\nTest: MostFrequent, Result as (value,frequency) pairs:")
       for (i <- 0 to mostFrequent.length - 1) {
         writer.write("\n" + mostFrequent(i))
       }
       resultMap("mostFrequent") = mostFrequent
     }

     writer.write("\n\n")

     (field, fieldAlias, fieldType, resultMap.toMap)

   }

   def quantitativeResults(table: RDD[String], field: String, fieldAlias: String, fieldType: String, testList: String, writer: PrintWriter, dateFormat: String): (String, String, String, Map[String, Any]) = {

     val tests: Array[String] = testList.split(",")
     val sc = table.context

     var statTracker = sc.accumulable(StatTrackerAccumulable.zero(new StatTracker(0, 0, 0, None, None)))

     if (fieldType.contains("numerical")) {
       writer.write("Field: " + field + "\nFieldtype: Numerical\nRunning tests: " + tests(0))
     }

 if (fieldType.contains("date")) {
       writer.write("Field: " + field + "\nFieldtype: Date\nDate format: " + dateFormat + "\nRunning tests: " + tests(0))
     }

     for (i <- 1 to (tests.length - 1)) {
       writer.write(", " + tests(i))
     }

     if ((tests.contains("min"))
       || (tests.contains("max"))
       || (tests.contains("sum"))
       || (tests.contains("sumxx"))
       || (tests.contains("count"))
       || (tests.contains("mean"))
       || (tests.contains("popvar"))
       || (tests.contains("popstd"))
       || (tests.contains("sampvar"))
       || (tests.contains("sampstd"))) {
       table.foreach(r => {
         val toadd = try {
           r.toDouble.toString
           } catch {
             case e: NumberFormatException => "corrupt"
           }
         if(!toadd.equals("corrupt")){
           statTracker += toadd.toDouble
          }})
     }

     val resultMap = collection.mutable.Map.empty[String, Any]

     if (!tests.contains("nocorruptcheck")){
       val corrupt = table.filter(r => (r.contains("%% corrupt data check failure bad field type %%"))).map(r => 1).reduce(_ + _)
       resultMap("corruptRecords") = corrupt.toString
     }
     
     if (fieldType.contains("numerical")) {
       if (tests.contains("countna")) {
         val countNA = CountQualities.CountNASave(table, sc)
         writer.write("\nTest: CountNA, Result: " + countNA)
         resultMap("countNA") = countNA.toString
       }
       if (tests.contains("countunique")) {
         val countUnique = CountQualities.CountUnique(table)
         writer.write("\nTest: CountUnique, Result: " + countUnique)
         resultMap("countUnique") = countUnique.toString
       }
       if (tests.contains("mostfrequent")) {
         val mostFrequent = Frequency.MostFrequent(5, table, false).map(_.swap)
         writer.write("\nTest: MostFrequent, Result as (value,frequency) pairs:")
         for (i <- 0 to mostFrequent.length - 1) {
           writer.write("\n" + mostFrequent(i))
         }
         resultMap("mostFrequent") = mostFrequent
       }
       if (tests.contains("quartiles")) {
         val quartiles = Quartiles.getAllQuartiles(table, false)
         writer.write("Test: Quartiles, Result: \nQuartile 1: " + quartiles._1 + "\nQuartile 2: " + quartiles._2 + "\n Quartile 3: " + quartiles._3)
         resultMap("quartiles") = (quartiles._1, quartiles._2, quartiles._3)
       }
       if (tests.contains("min")) {
         val min = statTracker.value.getMin()
         writer.write("\nTest: min, Result: " + min.get)
         resultMap("min") = min.get.toString
       }
       if (tests.contains("max")) {
         val max = statTracker.value.getMax()
         writer.write("\nTest: max, Result: " + max.get)
         resultMap("max") = max.get.toString
       }

       if (tests.contains("sum")) {
         val sum = statTracker.value.getSum()
         writer.write("\nTest: sum, Result: " + sum)
         resultMap("sum") = sum.toString
       }

	   if (tests.contains("sumxx")) {
         val sumXX = statTracker.value.getSumXX()
         writer.write("\nTest: sumXX, Result: " + sumXX)
         resultMap("sumXX") = sumXX.toString
       }
       if (tests.contains("count")) {
         val count = statTracker.value.getCount()
         writer.write("\nTest: count, Result: " + count)
         resultMap("count") = count.toString
       }
       if (tests.contains("mean")) {
         val mean = statTracker.value.getMean()
         writer.write("\nTest: mean, Result: " + mean)
         resultMap("mean") = mean.toString
       }
       if (tests.contains("popvar")) {
         val popVar = statTracker.value.getPopulationVariance()
         writer.write("\nTest: popVar, Result: " + popVar)
         resultMap("popVar") = popVar.toString
       }
       if (tests.contains("popstd")) {
         val popSTD = statTracker.value.getPopulationStdDeviation()
         writer.write("\nTest: popSTD, Result: " + popSTD)
         resultMap("popSTD") = popSTD.toString
       }
       if (tests.contains("sampvar")) {
         val sampVar = statTracker.value.getSampleVariance()
         writer.write("\nTest: sampVar, Result: " + sampVar)
         resultMap("sampVar") = sampVar.toString
       }
       if (tests.contains("sampstd")) {
         val sampSTD = statTracker.value.getSampleStdDeviation()
         writer.write("\nTest: sampSTD, Result: " + sampSTD)
         resultMap("sampSTD") = sampSTD.toString
       }
     } else if (fieldType.contains("date")) {
       if (tests.contains("countna")) {
         val countNA = CountQualities.CountNASave(table, sc)
         writer.write("\nTest: CountNA, Result: " + countNA)
         resultMap("countNA") = countNA.toString
       }
       if (tests.contains("countunique")) {
         val countUnique = CountQualities.CountUnique(table)
         writer.write("\nTest: CountUnique, Result: " + countUnique)
         resultMap("countUnique") = countUnique.toString
       }
       if (tests.contains("mostfrequent")) {
         val mostFrequent = Frequency.MostFrequent(5, table, false).map(_.swap)
         writer.write("\nTest: MostFrequent, Result as (value,frequency) pairs:")
         for (i <- 0 to mostFrequent.length - 1) {
           val date = new java.util.Date(mostFrequent(i)._1.toLong)
           writer.write("\n" + date.toString)
         }
         resultMap("mostFrequent") = mostFrequent
       }
       if (tests.contains("quartiles")) {
         val quartiles = Quartiles.getAllQuartiles(table, false)
         val q1 = new java.util.Date(quartiles._1.toLong)
         val q2 = new java.util.Date(quartiles._2.toLong)
         val q3 = new java.util.Date(quartiles._3.toLong)
         writer.write("Test: Quartiles, Result: \nQuartile 1: " + q1 + "\nQuartile 2: " + q2 + "\n Quartile 3: " + q3)
         resultMap("quartiles") = (q1, q2, q3)
       }
       if (tests.contains("min")) {
         val min = statTracker.value.getMin()
         val date = new java.util.Date(min.get.toLong)
         writer.write("\nTest: min, Result: " + date.toString)
         resultMap("min") = date.toString
       }
       if (tests.contains("max")) {
         val max = statTracker.value.getMax()
         val date = new java.util.Date(max.get.toLong)
         writer.write("\nTest: max, Result: " + date.toString)
         resultMap("max") = date.toString
       }

        if (tests.contains("sum")) {
         val sum = statTracker.value.getSum()
         writer.write("\nTest: sum, Result (as timestamp): " + sum)
         resultMap("sum") = sum.toString
       }
       if (tests.contains("sumxx")) {
         val sumXX = statTracker.value.getSumXX()
         writer.write("\nTest: sumXX, Result (as timestamp): " + sumXX)
         resultMap("sumXX") = sumXX.toString
       }
       if (tests.contains("count")) {
         val count = statTracker.value.getCount()
         writer.write("\nTest: count, Result: " + count)
         resultMap("count") = count.toString
       }
       if (tests.contains("mean")) {
         val mean = statTracker.value.getMean()
         val date = new java.util.Date(mean.toLong)
         writer.write("\nTest: mean, Result: " + date.toString)
         resultMap("mean") = mean.toString
       }
       if (tests.contains("popvar")) {
         val popVar = statTracker.value.getPopulationVariance()
         writer.write("\nTest: popVar, Result (as timestamp): " + popVar)
         resultMap("popVar") = popVar.toString
       }
       if (tests.contains("popstd")) {
         val popSTD = statTracker.value.getPopulationStdDeviation()
         writer.write("\nTest: popSTD, Result: (as timestamp) " + popSTD)
         resultMap("popSTD") = popSTD.toString
       }
       if (tests.contains("sampvar")) {
         val sampVar = statTracker.value.getSampleVariance()
         writer.write("\nTest: sampVar, Result (as timestamp): " + sampVar)
         resultMap("sampVar") = sampVar.toString
       }
       if (tests.contains("sampstd")) {
         val sampSTD = statTracker.value.getSampleStdDeviation()
         writer.write("\nTest: sampSTD, Result (as timestamp): " + sampSTD)
         resultMap("sampSTD") = sampSTD.toString
       }
     }

     writer.write("\n\n")

     (field, fieldAlias, fieldType, resultMap.toMap)

   }

   def customAnalytic(table: RDD[Array[String]], field: String, index: Int, customAnalytic: String, customVars: String, customOutput: PrintWriter, sc: SparkContext, i: String) {
     //make it so I dont need to directly hard code import the analytic
  //   NBAgamesummary.run(sc)
 //Fingerprints.run(table, field, index, customVars, customOutput, i)
   }
 }

 //        try {
 //        val c = Class.forName(customAnalytic);
 //        val cons = c.getConstructor();
 //        val custom = cons.newInstance()
 //        custom.run(table, field, index, customVars, customOutput)}
 //
 //        catch {
 //          case e: Exception => println("")
 //        }
 //
 //      }
 //
 //
