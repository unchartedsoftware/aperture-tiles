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
import com.oculusinfo.stats.util.Parsing
import com.oculusinfo.stats.customAnalytics.Fingerprints
import org.apache.spark.AccumulableParam
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.json.JSONWriter
import java.text.SimpleDateFormat

/**
 * @author mkielo
 */

object SummaryStatistics {

	def getFieldType(mytype: String): String = {
		if(mytype.equals("int") || mytype.equals("long") || mytype.equals("double") || mytype.equals("numerical")){
			"numerical"
		}
		else if (mytype.equals("date")){
			"date"
		} else {
			"qualitative"
		}
	}


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

    val inputLocation = prop.getProperty("oculus.binning.source.location", "No input location specified")
    val title = prop.getProperty("oculus.binning.name", "No Title Specified")
    val delimiter = prop.getProperty("oculus.binning.parsing.separator", "\t")
    val outputLocation = prop.getProperty("oculus.binning.output.location", "/output/summarystatsOutput")

    val sparkMaster = prop.getProperty("spark.connection.url", "local")
    val sparkHome = prop.getProperty("spark.connection.home", "/opt/spark")
    val sparkClassPath = prop.getProperty("spark.connection.classpath", "").split(",")


val subsets = prop.getProperty("oculus.binning.subsets","none")
val partitions = prop.getProperty("oculus.binning.partitions","none")

for (it <- subsets.split(",")){
	
	val i = if(it == "none"){""} else {it}
	 
   	val sc = new SparkContext(sparkMaster, "Summary Stats", sparkHome, sparkClassPath)
    
   	val writer = new PrintWriter(new File(outputLocation)) 
      val textFile = if(partitions.equals("none")){sc.textFile(inputLocation + "/" + i)} else {sc.textFile(inputLocation + "/" + i).coalesce(partitions.toInt)}

      //analyze dataset at a high level. count total records etc.
     

      
	val toLog = prop.getProperty("oculus.table.cleaning.log", "false").toBoolean
	val columns = prop.getProperty("oculus.table.cleaning.columns").toInt

	val fieldMaptemp = collection.mutable.Map.empty[Int, String]
	fields.foreach(r => {
		val colIndex = prop.getProperty("oculus.binning.parsing." + r + ".index").toInt
		val colType = prop.getProperty("oculus.binning.parsing." + r + ".fieldType")
		fieldMaptemp(colIndex) = colType
	})

	val fieldMap = fieldMaptemp.toMap

      
      val dirtytable = Parsing.rddCleaner(textFile, delimiter, columns, fieldMap, toLog)
      
      val tableTests = prop.getProperty("oculus.binning.table.tests", "none")
      val tableTestResults = analyze.tableResults(dirtytable, tableTests.toLowerCase, writer)

      val table = dirtytable.filter(r => (!r.contains("%% corrupt data check failure invalid length %%")))
//      val corruptLines = dirtytable.filter(r => (r.contains("%% corrupt data check failure invalid length %%"))).map(r => 1).reduce(_ + _)

      //val table = textFile.map(record => (record.split(delimiter)))

      // Run custom analysis analysis on each field. Custom analysis is not included in the output JSON file
      fields.foreach(field => {
        
        val index = prop.getProperty("oculus.binning.parsing." + field + ".index").toInt
        val fieldType = getFieldType(prop.getProperty("oculus.binning.parsing." + field + ".fieldType").toLowerCase)
        val customAnalytics = prop.getProperty("oculus.binning.parsing." + field + ".custom.analytics", "")

        if (!customAnalytics.isEmpty) {
          //allows user to specify variables for the custom analytic
          val customVariables = prop.getProperty("oculus.binning.parsing." + field + ".custom.variables", "")
          val customOutput = prop.getProperty("oculus.binning.parsing." + field + ".custom.output", "")
          if (customOutput == "") {
            util.analyze.customAnalytic(table, field, index, customAnalytics, customVariables, writer, sc, i)
          } else {
            val customWriter = new PrintWriter(new File(customOutput))
            util.analyze.customAnalytic(table, field, index, customAnalytics, customVariables, customWriter, sc, i)
          }
        }
      })

      // Run analysis on each field. The type of analysis run is determined by whether the field is specified as numeric or qualitative.   
      val fieldTestResults = fields.map(field => {
        // Load field information
        val index = prop.getProperty("oculus.binning.parsing." + field + ".index").toInt
        val fieldType = getFieldType(prop.getProperty("oculus.binning.parsing." + field + ".fieldType").toLowerCase)
        val fieldAlias = prop.getProperty("oculus.binning.parsing." + field + ".fieldAlias", field)
        //Set default tests if none specified based on whether data is quantitative or numeric
        val testList = if ((fieldType.contains("numerical") || fieldType.contains("date"))) {
          prop.getProperty("oculus.binning.parsing." + field + ".tests", "min,max,mean,count,stdev,countna,countunique").toLowerCase
        } else {
          prop.getProperty("oculus.binning.parsing." + field + ".tests", "countna,countunique,mostfrequent")
        }
        


	val column = table.map(line => line(index)).filter(r => (!r.contains("%% corrupt data check failure bad field type %%")))
	//val column = table.map(line => {println("LENGTH: " + line.size + "STRING: " + line.mkString(" %% ")); line(index)})
        if (fieldType == "date") {
          val dateFormat = prop.getProperty("oculus.binning.parsing." + field + ".dateFormat", "yyyy-MM-dd HH:mm:ss.S")
          val dateColumn = column.map(line => {
            val strDate = line.toString
            //convert data to dates
              try {
                val date= new SimpleDateFormat(dateFormat).parse(strDate)
                date.getTime.toString
                //remove invalid dates
              } catch {
                case e: Exception => "corrupt"
              }
          }
            ).filter(r => (!r.contains("corrupt")))
          util.analyze.quantitativeResults(dateColumn, field, fieldAlias, fieldType, testList, writer, dateFormat)
        } else if (fieldType == "numerical") {
          util.analyze.quantitativeResults(column, field, fieldAlias, fieldType, testList, writer, "")
        } else {
          util.analyze.qualitativeResults(column, field, fieldAlias, fieldType, testList, writer, "")
  
        }
      })

      //sort results by data type: qualitative, numeric, date, text 
      val qualitative = fieldTestResults.map(r => { if (r._3 == "qualitative") { r } else { ("delete4269", "d", "d", Map(("d" -> "d"))) } }).filter(!_.toString.equals("(delete4269,d,d,Map(d -> d))")).toArray
      val numerical = fieldTestResults.map(r => { if (r._3 == "numerical") { r } else { ("delete4269", "d", "d", Map(("d" -> "d"))) } }).filter(!_.toString.equals("(delete4269,d,d,Map(d -> d))")).toArray
      val date = fieldTestResults.map(r => { if (r._3 == "date") { r } else { ("delete4269", "d", "d", Map(("d" -> "d"))) } }).filter(!_.toString.equals("(delete4269,d,d,Map(d -> d))")).toArray
      val text = fieldTestResults.map(r => { if (r._3 == "text") { r } else { ("delete4269", "d", "d", Map(("d" -> "d"))) } }).filter(!_.toString.equals("(delete4269,d,d,Map(d -> d))")).toArray

      val totalRecords = if (tableTestResults.contains("totalRecords")) {
        tableTestResults("totalRecords").toLong
      } else {
        1L
      }

	  val corruptRecords = if (tableTestResults.contains("corruptRecords")) {
        tableTestResults("corruptRecords").toLong
      } else {
        1L
      }
	
      val qualSummary = JSONwriter.JSONqualitative(qualitative, totalRecords)
      val numericSummary = JSONwriter.JSONnumeric(numerical, totalRecords)
      val dateSummary = JSONwriter.JSONdate(date, totalRecords)
      val textSummary = JSONwriter.JSONtext(text, totalRecords)

      val totalBytes = 0L
      val sampleRecords = totalRecords

      JSONwriter.JSONoutput(title, totalRecords, totalBytes, sampleRecords, qualSummary, numericSummary, dateSummary, textSummary)

      writer.close()

    }
  }
}                                 

