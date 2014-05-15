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
  
  
  //json output for qual
  def JSONqualitative(qualitative: Array[(String, String, String, Map[String, Any])], totalRecords: Int): JSONObject = {
    
      val qualSummary = new JSONObject
      
      val qualList = new JSONArray
      
    qualitative.foreach(r => {

      val field = new JSONObject

      val alias = r._1
      val name = r._2
      //not safe
      val freqs = r._4("mostFrequent").asInstanceOf[Array[(String, Int)]]
      val frequencies = new JSONArray

      val mf1 = new JSONObject
      mf1.put(freqs(0)._1, freqs(0)._2)
      val mf2 = new JSONObject
      mf2.put(freqs(1)._1, freqs(1)._2)
      val mf3 = new JSONObject
      mf3.put(freqs(2)._1, freqs(2)._2)
      val mf4 = new JSONObject
      mf4.put(freqs(3)._1, freqs(3)._2)
      val mf5 = new JSONObject
      mf5.put(freqs(4)._1, freqs(4)._2)

      frequencies.put(mf2)
      frequencies.put(mf2)
      frequencies.put(mf3)
      frequencies.put(mf4)
      frequencies.put(mf5)
                      
      
      val na = new JSONObject
      val naCount = new JSONObject
      naCount.put("Type", "integer")
      naCount.put("Value", r._4("countNA"))
      naCount.put("Unit", "")
      
      val naPercent = new JSONObject
      naPercent.put("Type","float")
      //not safe
      val naPercentVal = r._4("countNA").asInstanceOf[String].toInt / totalRecords
      naPercent.put("Value", naPercentVal)
      naPercent.put("Unit","%")
      
      na.put("Count",naCount)
      na.put("Percent", naPercent)
        
      
      val unique = new JSONObject
      val uniqueCount = new JSONObject
      uniqueCount.put("Type", "integer")
      uniqueCount.put("Value", r._4("countUnique"))
      uniqueCount.put("Unit", "")
      
      val uniquePercent = new JSONObject
      uniquePercent.put("Type","float")
      //not safe
      val uniquePercentVal = r._4("countUnique").asInstanceOf[String].toInt / (totalRecords - r._4("countNA").asInstanceOf[String].toInt)
      uniquePercent.put("Value", uniquePercentVal)
      uniquePercent.put("Unit","%")
       
      unique.put("Count",uniqueCount)
      unique.put("Percent", uniquePercent)

      
      field.put("Field Alias", alias)
      field.put("Name", name)
      field.put("Most Frequent", frequencies)
      field.put("# N.A.", na)
      field.put("# Unique", unique)
      
      qualList.put(field)
    })
    
    qualSummary.put("Fields", qualList)
    qualSummary.put("Type","Qualitative")
    
    qualSummary
  }
  
  
  def JSONnumeric(numerical: Array[(String, String, String, Map[String, Any])], totalRecords: Int): JSONObject = {
  //json output for numeric
  val numericSummary = new JSONObject
  numericSummary.put("Type","Numerical")
  

  val numericList = new JSONArray

    numerical.foreach(r => {

      val field = new JSONObject

      val alias = r._1
      val name = r._2

      val quartiles = r._4("quartiles").asInstanceOf[(Double, Double, Double)]
      
      val mean = r._4("mean")
      val min = r._4("min")
      val q1 = quartiles._1
      
      val median = quartiles._2
      val q3 = quartiles._3
      val max = r._4("max")
      
      val na = new JSONObject
      val naCount = new JSONObject
      naCount.put("Type", "integer")
      naCount.put("Value", r._4("countNA"))
      naCount.put("Unit", "")
      
      val naPercent = new JSONObject
      naPercent.put("Type","float")
      //not safe
      val naPercentVal = r._4("countNA").asInstanceOf[String].toInt / totalRecords
      naPercent.put("Value", naPercentVal)
      naPercent.put("Unit","%")
      
      na.put("Count",naCount)
      na.put("Percent", naPercent)
        
      
      val unique = new JSONObject
      val uniqueCount = new JSONObject
      uniqueCount.put("Type", "integer")
      uniqueCount.put("Value", r._4("countUnique"))
      uniqueCount.put("Unit", "")
      
      val uniquePercent = new JSONObject
      uniquePercent.put("Type","float")
      //not safe
      val uniquePercentVal = r._4("countUnique").asInstanceOf[String].toInt / (totalRecords - r._4("countNA").asInstanceOf[String].toInt)
      uniquePercent.put("Value", uniquePercentVal)
      uniquePercent.put("Unit","%")
       
      unique.put("Count",uniqueCount)
      unique.put("Percent", uniquePercent)

      
      field.put("Field Alias", alias)
      field.put("Name", name)
//      field.put("Most Frequent", frequencies)
      field.put("# N.A.", na)
      field.put("# Unique", unique)
      
      numericList.put(field)
    })
    
    numericSummary.put("Fields", numericList)
    numericSummary
  }
  

  //json output for date
  
  
  
 def JSONdate(date: Array[(String, String, String, Map[String, Any])], totalRecords: Int): JSONObject = { 
  val dateSummary = new JSONObject  
  dateSummary.put("Type","Date")

  val dateList = new JSONArray

    date.foreach(r => {

      val field = new JSONObject

      val alias = r._1
      val name = r._2

      val quartiles = r._4("quartiles").asInstanceOf[(Double, Double, Double)]
      
      val mean = r._4("mean")
      val min = r._4("min")
      val q1 = quartiles._1
      
      val median = quartiles._2
      val q3 = quartiles._3
      val max = r._4("max")
      
      val na = new JSONObject
      val naCount = new JSONObject
      naCount.put("Type", "integer")
      naCount.put("Value", r._4("countNA"))
      naCount.put("Unit", "")
      
      val naPercent = new JSONObject
      naPercent.put("Type","float")
      //not safe
      val naPercentVal = r._4("countNA").asInstanceOf[String].toInt / totalRecords
      naPercent.put("Value", naPercentVal)
      naPercent.put("Unit","%")
      
      na.put("Count",naCount)
      na.put("Percent", naPercent)
        
      
      val unique = new JSONObject
      val uniqueCount = new JSONObject
      uniqueCount.put("Type", "integer")
      uniqueCount.put("Value", r._4("countUnique"))
      uniqueCount.put("Unit", "")
      
      val uniquePercent = new JSONObject
      uniquePercent.put("Type","float")
      //not safe
      val uniquePercentVal = r._4("countUnique").asInstanceOf[String].toInt / (totalRecords - r._4("countNA").asInstanceOf[String].toInt)
      uniquePercent.put("Value", uniquePercentVal)
      uniquePercent.put("Unit","%")
       
      unique.put("Count",uniqueCount)
      unique.put("Percent", uniquePercent)

      
      field.put("Field Alias", alias)
      field.put("Name", name)
//      field.put("Most Frequent", frequencies)
      field.put("# N.A.", na)
      field.put("# Unique", unique)
      
      dateList.put(field)
    })
    
    dateSummary.put("Fields", dateList)
  
  dateSummary
 }
//  
////  "Field Alias": "Created At", 
////                    "Name": "created_at", 
////                    "Minimum": "2012-02-07 10:35:11", 
////                    "Maximum": "2013-05-30 07:40:20", 
////                    "# N.A.": {
////                        "Count": {
////                            "Type": "integer", 
////                            "Value": 0, 
////                            "Unit": ""
////                        }, 
////                        "Percent": {
////                            "Type": "float", 
////                            "Value": 0.0, 
////                            "Unit": "%"
////                        }
////                    }, 
////                    "# Unique": {
////                        "Count": {
////                            "Type": "integer", 
////                            "Value": 19641444, 
////                            "Unit": ""
////                        }, 
////                        "Percent": {
////                            "Type": "float", 
////                            "Value": 6.0, 
////                            "Unit": "%"
////                        }
////                    }, 
////                    "Density Strip": ""
//  
//  
 
 //json output for text
 def JSONtext(text: Array[(String, String, String, Map[String, Any])], totalRecords: Int): JSONObject = { 
  val textSummary = new JSONObject  
  textSummary.put("Type","Text")
  
  textSummary
 } 
  
 def JSONoutput(title: String, totalRecords: Int, totalBytes: Int, samp: Int,
		 		qualSummary: JSONObject, numericSummary: JSONObject, dateSummary: JSONObject, textSummary: JSONObject){
 
  val myjson= new JSONObject
  myjson.put("Title",title)
  myjson.put("Total Records",totalRecords)
  myjson.put("Total Bytes",totalBytes)
  myjson.put("Sample Size",samp)
  
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