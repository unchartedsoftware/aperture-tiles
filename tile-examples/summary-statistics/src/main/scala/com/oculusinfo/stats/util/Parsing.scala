package com.oculusinfo.stats.util

import java.io.FileInputStream
import java.util.Properties
import java.io._
import scala.collection.JavaConversions.asScalaSet
import org.apache.spark.SparkContext
import org.apache.spark.AccumulableParam
import org.apache.spark.rdd.RDD
import scala.collection.mutable.ListBuffer
import scala.collection.mutable




object Parsing {
  
  def validFieldType(fieldVal: String, fieldType: String): Boolean = {
    try {
      if (fieldType.equals("any")) {
        val test = fieldVal
      } else if (fieldType.equals("int")) {
        val test =  fieldVal.toInt
      } else if (fieldType.equals("double")) {
        val test = fieldVal.toDouble
      } else if (fieldType.equals("long")) {
        val test = fieldVal.toLong
      } else if (fieldType.equals("string")) {
        val test = fieldVal.toString
      } //else if fieldType.equals("date"){
      //	parsedLine(i).toInt
      //}
      true
    } catch {
      case e: Exception => false
    }
  }

  def rddCleaner(myRDD: RDD[String], delimiter: String, columns: Int, fields: Map[Int, String], log: Boolean): RDD[Array[String]] = {

    var deletedLines: Int = 0

    val markedRDD = myRDD.map(line => {
      val parsedLine = line.split(delimiter)
      val parsedLength = parsedLine.length

      //check if parsed line is the correct length, if not flag the line
      if (columns != parsedLength) {
        
	println("FIFA WORLD CUP")
	Array("%% corrupt data check failure invalid length %%")
        
      }

      //check if each element of the parsed line is the correct type
      else {parsedLine.zipWithIndex.map(r => {
        if(fields.contains(r._2)){
        	if(validFieldType(r._1, fields(r._2))){
        		r._1
        	} else {"%% corrupt data check failure bad field type %%"}
        } 
	else {r._1}  
      })
     }
    })

//    if (log) {
//      val writer = new PrintWriter(new File("output/corrupt-data.log"))
//      writer.write("Invalid Length Errors: " + "\n")
//      val toLogLength = markedRDD.filter(r => (r.contains("[%% corrupt data check failure invalid length %%]:"))).take(100)
//      toLogLength.foreach(r => { writer.write(r + "\n") })
//
//      writer.write("\n" + "Invalid Field Type Errors: " + "\n")
//      val toLogType = markedRDD.filter(r => r.contains("[%%CORRUPT DATA CHECK FAILURE BAD FIELD TYPE%%]"))
//      toLogType.foreach(r => { writer.write(r + "\n") })
//      
//      writer.close
//    }

    markedRDD
  }
}
