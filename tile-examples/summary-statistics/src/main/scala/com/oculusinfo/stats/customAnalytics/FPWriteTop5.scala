package com.oculusinfo.stats.customAnalytics

import java.io.FileInputStream
import java.util.Properties
import java.io._
import scala.collection.JavaConversions.asScalaSet


import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

class FPWriteTop5 {

  
def buildDescDB(): Array[String] = {

	val textFile = Source.fromFile("C:/Users/mkielo/workspace/experiments/os-db.txt")("UTF-8").getLines();
	
	val refDB = mutable.ListBuffer.empty[String]
	while(!textFile.isEmpty){
		//reads in all data from 1 OS
		val block = textFile.takeWhile(!_.isEmpty).map(_+",")
		val blockString = block.addString(new StringBuilder).toString

		//splits the data into 2 parts: OS description and OS test results
		val parts = blockString.split("SEQ")
		refDB += parts(0)
	}
	refDB.toArray
}


  def write5Keys(resultArray: Array[Int], writer: PrintWriter){
  
    //(score, position) tuples
    var first = (0,0)
    var second = (0,0)
    var third = (0,0)
    var fourth = (0,0)
    var fifth = (0,0)

    val refArray = buildDescDB

    var i = 0
    resultArray.foreach(r => {
      if(r.toInt >= first._1){
        fifth = fourth
        fourth = third
        third = second
        second = first
        first = (r.toInt, i)
      } else if(r.toInt >= second._1){
        fifth = fourth
        fourth = third
        third = second
        second = (r.toInt, i)
      } else if(r.toInt >= third._1){
        fifth = fourth
        fourth = third
        third = (r.toInt, i)
      } else if(r.toInt >= fourth._1){
        fifth = fourth
        fourth = (r.toInt, i)
      } else if(r.toInt >= fifth._1){
        fifth = (r.toInt, i)
      }
    i += 1
    })
    
    
    if(resultArray.length > 2){
 //if(first._1 > 0 && second._1 > 0 && third._1 > 0 && fourth._1 > 0 && fifth._1 > 0){   
    val firstOS = refArray(first._2 - 1)
    val secondOS = refArray(second._2 - 1)
    val thirdOS = refArray(third._2 - 1)
    val fourthOS = refArray(fourth._2 - 1)
    val fifthOS = refArray(fifth._2 - 1)
    
    //FPresultsFIRSTOS.txt
    //writer.write(firstOS + "\n")

    //FPresultsTOP5OS.txt
    //writer.write(firstOS + "\n" + secondOS +  "\n" + thirdOS +  "\n" + fourthOS +  "\n" + fifthOS +  "\n")


    //FPresultsSCORES.txt
    //writer.write(first._1 + "," + second._1 + "," + third._1 + "," + fourth._1 + "," + fifth._1 + "\n")

    //FPresultsAssociation.txt
     writer.write(firstOS + "%KIELO%" + secondOS + "%KIELO%" + thirdOS + "%KIELO%" + fourthOS + "%KIELO%" + fifthOS + "\n")
   
    } //else {
//      writer.write("SCORING ERROR\n")
  //  }
  }


  // def Get5Keys(resultArray: Array[Int], writer: PrintWriter) {
  
  //   //(score, position) tuples
  //   var first = (0,0)
  //   var second = (0,0)
  //   var third = (0,0)
  //   var fourth = (0,0)
  //   var fifth = (0,0)

  //   val refArray = buildDescDB

  //   var i = 0
  //   resultArray.foreach(r => {
  //     if(r.toInt >= first._1){
  //       fifth = fourth
  //       fourth = third
  //       third = second
  //       second = first
  //       first = (r.toInt, i)
  //     } else if(r.toInt >= second._1){
  //       fifth = fourth
  //       fourth = third
  //       third = second
  //       second = (r.toInt, i)
  //     } else if(r.toInt >= third._1){
  //       fifth = fourth
  //       fourth = third
  //       third = (r.toInt, i)
  //     } else if(r.toInt >= fourth._1){
  //       fifth = fourth
  //       fourth = (r.toInt, i)
  //     } else if(r.toInt >= fifth._1){
  //       fifth = (r.toInt, i)
  //     }
  //   i += 1
  //   })
    
    
  //   if(resultArray.length > 2){
    
  //   val firstOS = refArray(first._2 - 1)
  //   val secondOS = refArray(second._2 - 1)
  //   val thirdOS = refArray(third._2 - 1)
  //   val fourthOS = refArray(fourth._2 - 1)
  //   val fifthOS = refArray(fifth._2 - 1)
    

  //   Array((first._1, firstOS),(second._1, secondOS),(third._1, thirdOS),(fourth._1, fourthOS),(fifth._1, fifthOS))
    
  //   } else {
  //   Array((-1,"ERROR"),(-1,"ERROR"),(-1,"ERROR"),(-1,"ERROR"),(-1,"ERROR"))
  //   }
//}
    
    
    def writeResults() = {
      val rawData = scala.io.Source.fromFile("C:/Users/mkielo/workspace/experiments/FPresults.csv").getLines
      val writer = new PrintWriter("C:/Users/mkielo/workspace/experiments/FPresultsAssociation.txt")
      rawData.foreach(r => {
        val testResults = r.split(",").map(a => a.toInt)
        write5Keys(testResults, writer)
      })
      writer.close
      }


  
  
}