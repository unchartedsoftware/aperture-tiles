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
  
    
def tableResults (table: RDD[Array[String]], tableTests: String, writer: PrintWriter): Map[String, String] = {
	val tests: Array[String] = tableTests.split(",")
    val sc = table.context
  
    val tableTestResults = collection.mutable.Map.empty[String, String]

	    if(tests.contains("totalrecords")){
	        val totalCount = table.count()
	    	writer.write("Total record count: " + totalCount + "\n")
	    	tableTestResults("totalRecords") = totalCount.toString
	      }
		
		if(tests.contains("uniquerecords")){
	        val totalUnique = table.distinct().count()
	    	writer.write("Total record count: " + totalUnique + "\n")
	    	tableTestResults("uniqueRecords") = totalUnique.toString
		}
	
	writer.write("\n") 
	
	tableTestResults.toMap
}

//



def fieldResults (table: RDD[Array[String]], field: String, fieldAlias: String, index: Int, fieldType: String, testList: String, writer: PrintWriter): (String, String, String, Map[String, Any])= {

	val tests: Array[String] = testList.split(",")
	val sc = table.context

	var statTracker = sc.accumulable(StatTrackerAccumulable.zero(new StatTracker(0, 0, 0, None, None)))

	if(fieldType.toLowerCase.equals("qualitative") || fieldType.toLowerCase.equals("text")){  
		writer.write("Field: " + field + "\nFieldtype: Qualitative\nRunning tests: " + tests(0))
		for (i <- 1 to (tests.length - 1)) {
			writer.write(", " + tests(i))
		}

		if(tests.contains("min")){writer.write("\nWARNING Test: min could not be run on quantitative field type")}
		if(tests.contains("max")){writer.write("\nWARNING Test: max could not be run on quantitative field type")}
		if(tests.contains("sum")){writer.write("\nWARNING Test: sum could not be run on quantitative field type")}
		if(tests.contains("sumxx")){writer.write("\nWARNING Test: sumXX could not be run on quantitative field type")}
		if(tests.contains("mean")){writer.write("\nWARNING Test: mean could not be run on quantitative field type")}
		if(tests.contains("popvar")){writer.write("\nWARNING Test: popVar could not be run on quantitative field type")}
		if(tests.contains("popstd")){writer.write("\nWARNING Test: popSTD could not be run on quantitative field type")}
		if(tests.contains("sampvar")){writer.write("\nWARNING Test: sampVar could not be run on quantitative field type")}
		if(tests.contains("sampstd")){writer.write("\nWARNING Test: sampSTD could not be run on quantitative field type")}

	}


	if(fieldType.contains("numerical") || fieldType.contains("date")){
		writer.write("Field: " + field + "\nFieldtype: Numerical\nRunning tests: " + tests(0))
		for (i <- 1 to (tests.length - 1)) {
			writer.write(", " + tests(i))
		}

		if((tests.contains("min"))
			|| (tests.contains("max"))
			|| (tests.contains("sum")) 
			|| (tests.contains("sumxx"))
			|| (tests.contains("count"))
			|| (tests.contains("mean"))
			|| (tests.contains("popvar"))
			|| (tests.contains("popstd"))
			|| (tests.contains("sampvar"))
			|| (tests.contains("sampstd"))) {
			table.map(line => line(index)).foreach(r => { statTracker += r.toDouble })  	
		}

	}


val resultMap = collection.mutable.Map.empty[String, Any]

	if(fieldType.toLowerCase.equals("qualitative") || fieldType.toLowerCase.equals("text")){
			
				if(tests.contains("countna")){
					val countNA = CountQualities.CountNASave(index, table, sc)
					writer.write("\nTest: CountNA, Result: " + countNA)
					resultMap("countNA") = countNA.toString
					}
					if(tests.contains("countunique")){
						val countUnique = CountQualities.CountUnique(index, table)
						writer.write("\nTest: CountUnique, Result: " + countUnique)
						resultMap("countUnique") = countUnique.toString
						}
						if(tests.contains("mostfrequent")){
							val mostFrequent = Frequency.MostFrequent(5, index, table, false).map(_.swap)
							writer.write("\nTest: MostFrequent, Result as (value,frequency) pairs:")
							for (i <- 0 to mostFrequent.length - 1) {
								writer.write("\n" + mostFrequent(i))
							}
							resultMap("mostFrequent") = mostFrequent
						}
	
	} else if ((fieldType.contains("numerical") || fieldType.contains("date"))){
				if(tests.contains("countna")){
					val countNA = CountQualities.CountNASave(index, table, sc)
					writer.write("\nTest: CountNA, Result: " + countNA)
					resultMap("countNA") = countNA.toString
					}
					if(tests.contains("countunique")){
						val countUnique = CountQualities.CountUnique(index, table)
						writer.write("\nTest: CountUnique, Result: " + countUnique)
						resultMap("countUnique") = countUnique.toString
						}
						if(tests.contains("mostfrequent")){
							val mostFrequent = Frequency.MostFrequent(5, index, table, false).map(_.swap)
							writer.write("\nTest: MostFrequent, Result as (value,frequency) pairs:")
							for (i <- 0 to mostFrequent.length - 1) {
								writer.write("\n" + mostFrequent(i))
							}
							resultMap("mostFrequent") = mostFrequent
							}
							if(tests.contains("min")){
								val min = statTracker.value.getMin()
								writer.write("\nTest: min, Result: " + min.get)
								resultMap("min") = min.get.toString
								}
								if(tests.contains("max")){
									val max = statTracker.value.getMax()
									writer.write("\nTest: max, Result: " + max.get)
									resultMap("max") = max.get.toString
									}
									if(tests.contains("sum")){
										val sum = statTracker.value.getSum()
										writer.write("\nTest: sum, Result: " + sum)
										resultMap("sum") = sum.toString
										}
										if(tests.contains("sumxx")){
											val sumXX = statTracker.value.getSumXX()
											writer.write("\nTest: sumXX, Result: " + sumXX)
											resultMap("sumXX") = sumXX.toString
											}
											if(tests.contains("count")){
												val count = statTracker.value.getCount()
												writer.write("\nTest: count, Result: " + count)
												resultMap("count") = count.toString
												}
												if(tests.contains("mean")){
													val mean = statTracker.value.getMean()
													writer.write("\nTest: mean, Result: " + mean)
													resultMap("mean") = mean.toString
													}
													if(tests.contains("popvar")){
														val popVar = statTracker.value.getPopulationVariance()
														writer.write("\nTest: popVar, Result: " + popVar)
														resultMap("popVar") = popVar.toString
														}
														if(tests.contains("popstd")){
															val popSTD = statTracker.value.getPopulationStdDeviation()
															writer.write("\nTest: popSTD, Result: " + popSTD)
															resultMap("popSTD") = popSTD.toString
															}
															if(tests.contains("sampvar")){
																val sampVar = statTracker.value.getSampleVariance()
																writer.write("\nTest: sampVar, Result: " + sampVar)
																resultMap("sampVar") = sampVar.toString
																}
																if(tests.contains("sampstd")){
																	val sampSTD = statTracker.value.getSampleStdDeviation()
																	writer.write("\nTest: sampSTD, Result: " + sampSTD)
																	resultMap("sampSTD") = sampSTD.toString
																	}
		}
		
writer.write("\n\n")

(field, fieldAlias, fieldType, resultMap.toMap)

}

	def customAnalytic(table: RDD[Array[String]], field: String, index: Int, customAnalytic: String, customVars: String, customOutput: PrintWriter){
  //make it so I dont need to directly hard code import the analytic
		Fingerprints.main(table, field, index, customVars, customOutput)
	}
}



















