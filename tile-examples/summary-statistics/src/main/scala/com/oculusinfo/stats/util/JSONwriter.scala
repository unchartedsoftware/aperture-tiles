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

import org.json.JSONWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.io.File
import java.io.FileWriter
import java.io.FileOutputStream

object JSONwriter {

  def JSONqualitative(qualitative: Array[(String, String, String, Map[String, Any])], totalRecords: Long): JSONObject = {

    val qualSummary = new JSONObject
    val qualList = new JSONArray
   
    qualitative.foreach(r => {

      val field = new JSONObject
      val alias = r._1
      val name = r._2
      val totalCleanRecords = if(r._4.contains("corruptRecords")){
    	  totalRecords - r._4("corruptRecords").asInstanceOf[String].toDouble
    	 } else {
    	   totalRecords
    	 }
      if (r._4.contains("mostFrequent")) {
        val freqs = r._4("mostFrequent").asInstanceOf[Array[(String, Int)]]
        val frequencies = new JSONArray

        if (freqs.size > 0) {
          val mf1 = new JSONObject
          mf1.put(freqs(0)._1, freqs(0)._2)
          frequencies.put(mf1)
        }
        if (freqs.size > 1) {
          val mf2 = new JSONObject
          mf2.put(freqs(1)._1, freqs(1)._2)
          frequencies.put(mf2)
        }
        if (freqs.size > 2) {
          val mf3 = new JSONObject
          mf3.put(freqs(2)._1, freqs(2)._2)
          frequencies.put(mf3)
        }
        if (freqs.size > 3) {
          val mf4 = new JSONObject
          mf4.put(freqs(3)._1, freqs(3)._2)
          frequencies.put(mf4)
        }
        if (freqs.size > 4) {
          val mf5 = new JSONObject
          mf5.put(freqs(4)._1, freqs(4)._2)
          frequencies.put(mf5)
        }

        field.put("Most Frequent", frequencies)
      }

      if (r._4.contains("countNA")) {
        val na = new JSONObject
        val naCount = new JSONObject
        naCount.put("Type", "integer")
        naCount.put("Value", r._4("countNA"))
        naCount.put("Unit", "")

        val naPercent = new JSONObject
        naPercent.put("Type", "float")

        val naPercentVal = (100 * r._4("countNA").asInstanceOf[String].toDouble) / totalCleanRecords.toDouble
        naPercent.put("Value", naPercentVal)
        naPercent.put("Unit", "%")

        na.put("Count", naCount)
        na.put("Percent", naPercent)
        field.put("# N.A.", na)
      }

      if (r._4.contains("countUnique")) {
        val unique = new JSONObject
        val uniqueCount = new JSONObject
        uniqueCount.put("Type", "integer")
        uniqueCount.put("Value", r._4("countUnique"))
        uniqueCount.put("Unit", "")

        val uniquePercent = new JSONObject
        uniquePercent.put("Type", "float")

        val uniquePercentVal = (100 * r._4("countUnique").asInstanceOf[String].toDouble) / (totalCleanRecords.toDouble - r._4("countNA").asInstanceOf[String].toDouble)
        uniquePercent.put("Value", uniquePercentVal)
        uniquePercent.put("Unit", "%")

        unique.put("Count", uniqueCount)
        unique.put("Percent", uniquePercent)

        field.put("# Unique", unique)
      }

      field.put("Field Alias", alias)
      field.put("Name", name)

      qualList.put(field)
    })

    qualSummary.put("Fields", qualList)
    qualSummary.put("Type", "Qualitative")

    qualSummary
  }

  def JSONnumeric(numerical: Array[(String, String, String, Map[String, Any])], totalRecords: Long): JSONObject = {

    val numericSummary = new JSONObject
    numericSummary.put("Type", "Numerical")

    val numericList = new JSONArray

    numerical.foreach(r => {

      val field = new JSONObject

      val alias = r._1
      val name = r._2

      val totalCleanRecords = if(r._4.contains("corruptRecords")){
    	  totalRecords - r._4("corruptRecords").asInstanceOf[String].toDouble
    	 } else {
    	   totalRecords
    	 }
      
      if (r._4.contains("quartiles")) {

        val quartiles = r._4("quartiles").asInstanceOf[(Double, Double, Double)]

        val q1 = quartiles._1
        val median = quartiles._2
        val q3 = quartiles._3

        field.put("1st Quartile", q1)
        field.put("Median", median)
        field.put("3rd Quartile", q3)
      }

      if (r._4.contains("mean")) {
        val mean = r._4("mean")
        field.put("Mean", mean)
      }

      if (r._4.contains("min")) {
        val min = r._4("min")
        field.put("Minimum", min)
      }

      if (r._4.contains("max")) {
        val max = r._4("max")
        field.put("Maximum", max)
      }

      if (r._4.contains("countNA")) {
        val na = new JSONObject
        val naCount = new JSONObject
        naCount.put("Type", "integer")
        naCount.put("Value", r._4("countNA"))
        naCount.put("Unit", "")

        val naPercent = new JSONObject
        naPercent.put("Type", "float")

        val naPercentVal = (100 * r._4("countNA").asInstanceOf[String].toDouble) / totalCleanRecords.toDouble
        naPercent.put("Value", naPercentVal)
        naPercent.put("Unit", "%")

        na.put("Count", naCount)
        na.put("Percent", naPercent)

        field.put("# N.A.", na)
      }

      if (r._4.contains("countUnique")) {
        val unique = new JSONObject
        val uniqueCount = new JSONObject
        uniqueCount.put("Type", "integer")
        uniqueCount.put("Value", r._4("countUnique"))
        uniqueCount.put("Unit", "")

        val uniquePercent = new JSONObject
        uniquePercent.put("Type", "float")

        val uniquePercentVal = (100 * r._4("countUnique").asInstanceOf[String].toDouble) / (totalCleanRecords.toDouble - r._4("countNA").asInstanceOf[String].toDouble)
        uniquePercent.put("Value", uniquePercentVal)
        uniquePercent.put("Unit", "%")

        unique.put("Count", uniqueCount)
        unique.put("Percent", uniquePercent)

        field.put("# Unique", unique)
      }

      field.put("Field Alias", alias)
      field.put("Name", name)

      numericList.put(field)
    })

    numericSummary.put("Fields", numericList)
    numericSummary
  }

  def JSONdate(date: Array[(String, String, String, Map[String, Any])], totalRecords: Long): JSONObject = {
    val dateSummary = new JSONObject
    dateSummary.put("Type", "Date")

    val dateList = new JSONArray

    date.foreach(r => {

      val field = new JSONObject

      val alias = r._1
      val name = r._2

      val totalCleanRecords = if(r._4.contains("corruptRecords")){
    	  totalRecords - r._4("corruptRecords").asInstanceOf[String].toDouble
    	 } else {
    	   totalRecords
    	 }
      
      if (r._4.contains("quartiles")) {

        val quartiles = r._4("quartiles").asInstanceOf[(java.util.Date, java.util.Date, java.util.Date)]

        val q1 = quartiles._1
        val median = quartiles._2
        val q3 = quartiles._3

        field.put("1st Quartile", q1)
        field.put("Median", median)
        field.put("3rd Quartile", q3)
      }

      if (r._4.contains("mean")) {
        val mean = r._4("mean")
        field.put("Mean", mean)
      }

      if (r._4.contains("min")) {
        val min = r._4("min")
        field.put("Minimum", min)
      }

      if (r._4.contains("max")) {
        val max = r._4("max")
        field.put("Maximum", max)
      }

      if (r._4.contains("countNA")) {
        val na = new JSONObject
        val naCount = new JSONObject
        naCount.put("Type", "integer")
        naCount.put("Value", r._4("countNA"))
        naCount.put("Unit", "")

        val naPercent = new JSONObject
        naPercent.put("Type", "float")
        //not safe
        val naPercentVal = (100 * r._4("countNA").asInstanceOf[String].toDouble) / totalCleanRecords.toDouble
        naPercent.put("Value", naPercentVal)
        naPercent.put("Unit", "%")

        na.put("Count", naCount)
        na.put("Percent", naPercent)

        field.put("# N.A.", na)
      }

      if (r._4.contains("countUnique")) {
        val unique = new JSONObject
        val uniqueCount = new JSONObject
        uniqueCount.put("Type", "integer")
        uniqueCount.put("Value", r._4("countUnique"))
        uniqueCount.put("Unit", "")

        val uniquePercent = new JSONObject
        uniquePercent.put("Type", "float")
        //not safe
        val uniquePercentVal = (100 * r._4("countUnique").asInstanceOf[String].toDouble) / (totalCleanRecords.toDouble - r._4("countNA").asInstanceOf[String].toDouble)
        uniquePercent.put("Value", uniquePercentVal)
        uniquePercent.put("Unit", "%")

        unique.put("Count", uniqueCount)
        unique.put("Percent", uniquePercent)

        field.put("# Unique", unique)
      }

      field.put("Field Alias", alias)
      field.put("Name", name)

      dateList.put(field)
    })

    dateSummary.put("Fields", dateList)
    dateSummary
  }

  def JSONtext(text: Array[(String, String, String, Map[String, Any])], totalRecords: Long): JSONObject = {
    val textSummary = new JSONObject
    textSummary.put("Type", "Text")
    val textList = new JSONArray

    textSummary.put("Fields", textList)
    textSummary
  }

  def JSONoutput(title: String, totalRecords: Long, totalBytes: Long, samp: Long,
    qualSummary: JSONObject, numericSummary: JSONObject, dateSummary: JSONObject, textSummary: JSONObject) {

    val myjson = new JSONObject
    myjson.put("Title", title)
    myjson.put("Total Records", totalRecords)

    myjson.put("Sample Size", samp)

    val sumList = new JSONArray

    sumList.put(qualSummary)
    sumList.put(numericSummary)
    sumList.put(dateSummary)
    sumList.put(textSummary)

    myjson.put("Summaries", sumList)

    val file = new PrintWriter(new File("output/" + title.replace(" ", "-") + "-tables.json"))

    file.write(myjson.toString(4))
    file.close()

  }

}
