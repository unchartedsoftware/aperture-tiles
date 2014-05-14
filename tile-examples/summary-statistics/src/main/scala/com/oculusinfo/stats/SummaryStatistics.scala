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

package com.oculusinfo.stats


import java.io.FileInputStream
import java.util.Properties
import java.io._
import scala.collection.JavaConversions.asScalaSet

import com.oculusinfo.stats.numeric.StatTracker
import com.oculusinfo.stats.qualitative.CountQualities
import com.oculusinfo.stats.qualitative.Frequency
import com.oculusinfo.stats.util.analyze
import com.oculusinfo.stats.util.JSONwriter
import com.oculusinfo.stats.customAnalytics.Fingerprints

import org.apache.spark.AccumulableParam
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.json.JSONWriter


/**
 * @author mkielo
 */

object SummaryStatistics {
  
 def main(args: Array[String]): Unit = {
  // Load, parse, and cache data
  val propertiesFile = "config.properties"
  val prop = new Properties()

  prop.load(new FileInputStream(propertiesFile))
  
  // Extract all fields identified in the properties file
  val fields = prop.stringPropertyNames
              .filter(_.startsWith("oculus.binning.parsing."))
              .filter(_.endsWith(".index"))
              .map(property => property.substring("oculus.binning.parsing.".length, property.length - ".index".length))

  //make it read all files in /* directory **
  val inputLocation = prop.getProperty("oculus.binning.source.location")
  val title = prop.getProperty("oculus.binning.name")
  val delimiter = prop.getProperty("oculus.binning.parsing.separator")
  val outputLocation = prop.getProperty("oculus.binning.output.location")
  val writer = new PrintWriter(new File(outputLocation))

  val sparkMaster = prop.getProperty("spark.connection.url","local")
  val sparkHome = prop.getProperty("spark.connection.home","/opt/spark")
  
  val sc = new SparkContext(sparkMaster, "Summary Stats", sparkHome, Seq("target/summary-statistics-0.3-SNAPSHOT.jar"))
  val textFile = sc.textFile(inputLocation)
  
  val table = textFile.map(record => (record.split(delimiter))).cache()

  val tableTests = prop.getProperty("oculus.binning.table.tests")
  
  //analyze dataset at a high level. count total records etc.
  val tableTestResults = analyze.tableResults(table, tableTests.toLowerCase, writer)
  
  // Run custom analysis analysis on each field. Custom analysis is not included in the output JSON file
  fields.foreach(field => {

    val index = prop.getProperty("oculus.binning.parsing." + field + ".index").toInt
    val fieldType = prop.getProperty("oculus.binning.parsing." + field + ".fieldType")
    val customAnalytics = prop.getProperty("oculus.binning.parsing." + field + ".custom.analytics","")
    
     if(customAnalytics != ""){
      //allows user to specify variables for the custom analytic
      val customVariables = prop.getProperty("oculus.binning.parsing." + field + ".custom.variables","")
      val customOutput =  prop.getProperty("oculus.binning.parsing." + field + ".custom.output","")
      if (customOutput == ""){
        util.analyze.customAnalytic(table, field, index, customAnalytics, customVariables, writer)
      } else {
        val customWriter = new PrintWriter(new File(customOutput))
        util.analyze.customAnalytic(table, field, index, customAnalytics, customVariables, customWriter)
      }
     }
  })
    
 // Run analysis on each field. The type of analysis run is determined by whether the field is specified as numeric or qualitative.   

  val fieldTestResults = fields.map(field => {
    // Load field information
    val index = prop.getProperty("oculus.binning.parsing." + field + ".index").toInt
    val fieldType = prop.getProperty("oculus.binning.parsing." + field + ".fieldType")
    val fieldAlias = prop.getProperty("oculus.binning.parsing." + field +  ".fieldAlias", field)
   //Set default tests if none specified based on whether data is quantitative or numeric
    val testList = if((fieldType.contains("numerical") || fieldType.contains("date"))){
				      prop.getProperty("oculus.binning.parsing." + field + ".tests","min,max,mean,count,stdev,countna,countunique").toLowerCase
				    } else {
				      prop.getProperty("oculus.binning.parsing." + field + ".tests","countna,countunique,mostfrequent")
				    }
   
    util.analyze.fieldResults(table, field, fieldAlias, index, fieldType, testList, writer)
  })
  
  //tableTestResults
  //fieldTestResults
  //Set((String, String, String, List[Any]))
  
  //sort results by data type: qualitative, numeric, date, text
  
  val qualitative = fieldTestResults.map(r => {if(r._3 == "qualitative"){r} else {("delete4269","d","d", Map(("d" -> "d")))}}).filter(!_.toString.equals("(delete4269,d,d,Map(d -> d))")).toArray
  val numerical = fieldTestResults.map(r => {if(r._3 == "numerical"){r} else {("delete4269","d","d", Map(("d" -> "d")))}}).filter(!_.toString.equals("(delete4269,d,d,Map(d -> d))")).toArray
  val date = fieldTestResults.map(r => {if(r._3 == "date"){r} else {("delete4269","d","d", Map(("d" -> "d")))}}).filter(!_.toString.equals("(delete4269,d,d,Map(d -> d))")).toArray
  val text = fieldTestResults.map(r => {if(r._3 == "text"){r} else {("delete4269","d","d", Map(("d" -> "d")))}}).filter(!_.toString.equals("(delete4269,d,d,Map(d -> d))")).toArray

  println(fieldTestResults.size)
 println(qualitative.size)
  println(numerical.size)
  
  println("GAGA")
  val totalRecords = tableTestResults("totalRecords").toInt
  
  val qualSummary = JSONwriter.JSONqualitative(qualitative, totalRecords)
  val numericSummary = JSONwriter.JSONnumeric(numerical, totalRecords)
  val dateSummary = JSONwriter.JSONdate(date, totalRecords)
  val textSummary = JSONwriter.JSONtext(text, totalRecords)
  
  val totalBytes = 0
  val sampleRecords = totalRecords
  
  JSONwriter.JSONoutput(title, totalRecords, totalBytes, sampleRecords, qualSummary, numericSummary, dateSummary, textSummary)
  
    writer.close()
 }
}
                                               