package com.oculusinfo.stats.util

import java.io.FileInputStream
import java.util.Properties
import java.io._
import scala.collection.JavaConversions.asScalaSet
import org.apache.spark.SparkContext
import com.oculusinfo.stats.numeric.StatTracker
import com.oculusinfo.stats.qualitative.CountQualities
import com.oculusinfo.stats.qualitative.Frequency
import com.oculusinfo.stats.customAnalytics._ // get rid of this...
import org.apache.spark.AccumulableParam
import org.apache.spark.rdd.RDD
import scala.collection.mutable.ListBuffer
import scala.collection.mutable


object analyze{
  
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
  
    
def tableResults (table: RDD[Array[String]], tableTests: String, writer: PrintWriter){
	val tests: Array[String] = tableTests.split(",")
    val sc = table.context
  
    if(tests.contains("totalRecords")){
       println("test1")
        val totalCount = table.count()
    	writer.write("Total record count: " + totalCount + "\n")
      }
	
//	if(tests.contains("uniqueRecords")){
//	  println("test2")
//        val totalUnique = table.distinct().count()
//        println("test2")
//    	writer.write("Total record count: " + totalUnique + "\n")
//      }
	
	writer.write("\n") 
}


def fieldResults (table: RDD[Array[String]], field: String, index: Int, fieldType: String, testList: String, writer: PrintWriter){
    
    val tests: Array[String] = testList.split(",")
    val sc = table.context
    
      if(fieldType.equals("chararray")){

	      writer.write("Field: " + field + "\nFieldtype: Qualitative\nRunning tests: " + tests(0))
	      for (i <- 1 to (tests.length - 1)) {
	        writer.write(", " + tests(i))
	      }
	      
      if(tests.contains("countNA")){
        val countNA = CountQualities.CountNASave(index, table, sc)
        writer.write("\nTest: CountNA, Result: " + countNA)
      }
      if(tests.contains("countUnique")){
        val countUnique = CountQualities.CountUnique(index, table)
        writer.write("\nTest: CountUnique, Result: " + countUnique)
      }
      // requires param   if(tests.contains("getFrequency")){}
      if(tests.contains("mostFrequent")){
        val mostFrequent = Frequency.MostFrequent(5, index, table, false).map(_.swap)
        writer.write("\nTest: MostFrequent, Result as (value,frequency) pairs:")
        for (i <- 0 to mostFrequent.length - 1) {
          writer.write("\n" + mostFrequent(i))
        }
      }
      if(tests.contains("leastFrequent")){
   //     val leastFrequent = Frequency.LeastFrequent(5, index, table, false).map(_.swap)
        writer.write("\nTest: leastFrequent, Result as (value,frequency) pairs:")
    //    for (i <- 0 to leastFrequent.length - 1) {
     //     writer.write("\n" + leastFrequent(i))
     //   }
      }
      //freqRange not implemented yet if(tests.contains("freqRange")){}
      //not yet implemented for qualitative types if(tests.contains("count")){}

      if(tests.contains("min")){writer.write("\nWARNING Test: min could not be run on quantitative field type")}
      if(tests.contains("max")){writer.write("\nWARNING Test: max could not be run on quantitative field type")}
      if(tests.contains("sum")){writer.write("\nWARNING Test: sum could not be run on quantitative field type")}
      if(tests.contains("sumXX")){writer.write("\nWARNING Test: sumXX could not be run on quantitative field type")}
      if(tests.contains("mean")){writer.write("\nWARNING Test: mean could not be run on quantitative field type")}
      if(tests.contains("popVar")){writer.write("\nWARNING Test: popVar could not be run on quantitative field type")}
      if(tests.contains("popSTD")){writer.write("\nWARNING Test: popSTD could not be run on quantitative field type")}
      if(tests.contains("sampVar")){writer.write("\nWARNING Test: sampVar could not be run on quantitative field type")}
      if(tests.contains("sampSTD")){writer.write("\nWARNING Test: sampSTD could not be run on quantitative field type")}
      
    }
    
    if(fieldType.contains("double") || fieldType.contains("int") || fieldType.contains("long")){
      
      writer.write("Field: " + field + "\nFieldtype: Numeric\nRunning tests: " + tests(0))
      for (i <- 1 to (tests.length - 1)) {
        writer.write(", " + tests(i))
      }

      if(tests.contains("countNA")){
        val countNA = CountQualities.CountNASave(index, table, sc)
        writer.write("\nTest: CountNA, Result: " + countNA)
      }
      if(tests.contains("countUnique")){
        val countUnique = CountQualities.CountUnique(index, table)
        writer.write("\nTest: CountUnique, Result: " + countUnique)
      }
      // requires param   if(tests.contains("getFrequency")){}
      if(tests.contains("mostFrequent")){
        val mostFrequent = Frequency.MostFrequent(5, index, table, false).map(_.swap)
        writer.write("\nTest: MostFrequent, Result as (value,frequency) pairs:")
        for (i <- 0 to mostFrequent.length - 1) {
          writer.write("\n" + mostFrequent(i))
        }
      }
      if(tests.contains("leastFrequent")){
     //   val leastFrequent = Frequency.LeastFrequent(5, index, table, false).map(_.swap)
        writer.write("\nTest: leastFrequent, Result as (value,frequency) pairs:")
     //   for (i <- 0 to leastFrequent.length - 1) {
     //    writer.write("\n" + leastFrequent(i))
     //   }
      }
      
      var statTracker = sc.accumulable(StatTrackerAccumulable.zero(new StatTracker(0, 0, 0, None, None)))
      
      if((tests.contains("min"))
		  	|| (tests.contains("max"))
		  	|| (tests.contains("sum")) 
		  	|| (tests.contains("sumXX"))
		  	|| (tests.contains("count"))
		  	|| (tests.contains("mean"))
		  	|| (tests.contains("popVar"))
		  	|| (tests.contains("popSTD"))
		  	|| (tests.contains("sampVar"))
		  	|| (tests.contains("sampSTD")))
      {
    	  table.map(line => line(index)).foreach(r => { statTracker += r.toDouble })  	
      }
      
      if(tests.contains("min")){
        val min = statTracker.value.getMin()
         writer.write("\nTest: min, Result: " + min)}
      if(tests.contains("max")){
        val max = statTracker.value.getMax()
         writer.write("\nTest: max, Result: " + max)}
      if(tests.contains("sum")){
        val sum = statTracker.value.getSum()
         writer.write("\nTest: sum, Result: " + sum)}
      if(tests.contains("sumXX")){
        val sumXX = statTracker.value.getSumXX()
         writer.write("\nTest: sumXX, Result: " + sumXX)}
      if(tests.contains("count"))
      {val count = statTracker.value.getCount()
         writer.write("\nTest: count, Result: " + count)}
      if(tests.contains("mean")){
        val mean = statTracker.value.getMean()
         writer.write("\nTest: mean, Result: " + mean)}
      if(tests.contains("popVar")){
        val popVar = statTracker.value.getPopulationVariance()
         writer.write("\nTest: popVar, Result: " + popVar)}
      if(tests.contains("popSTD")){
        val popSTD = statTracker.value.getPopulationStdDeviation()
         writer.write("\nTest: popSTD, Result: " + popSTD)}
      if(tests.contains("sampVar")){
        val sampVar = statTracker.value.getSampleVariance()
         writer.write("\nTest: sampVar, Result: " + sampVar)}
      if(tests.contains("sampSTD")){
        val sampSTD = statTracker.value.getSampleStdDeviation()
         writer.write("\nTest: sampSTD, Result: " + sampSTD)}
    }
    
    if(!(fieldType.contains("double") || fieldType.contains("int") || fieldType.contains("long") || fieldType.contains("chararray"))){
      writer.write("Field: " + field + "\nWARNING: Invalid fieldType specified\n")
    }
    
    writer.write("\n\n")
    
  }

	def customAnalytic(table: RDD[Array[String]], field: String, index: Int, customAnalytic: String, customVars: String, customOutput: PrintWriter){
  //make it so I dont need to import the analytic
		Fingerprints.main(table, field, index, customVars, customOutput)
	}
}



















