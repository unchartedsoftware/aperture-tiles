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

      val min = r._4("max")
      val q1 = r._4("max")
      val mean = r._4("max")
      val median = r._4("max")
      val q3 = r._4("max")
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
  val testStr = myjson.toString.getBytes
  file.write(myjson.toString)
  file.close()

 }
 
}