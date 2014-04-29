package com.oculusinfo.stats.customAnalytics

import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.io.Source

import java.io._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.AccumulableParam
//Fingerprints.fingerPrintsParser("SCAN(V=5.51%D=1/2%OT=21%CT=22%CU=38310%PV=N%DS=4%DC=I%G=Y%TMFingerprints.=1F389%P=mip)")

object Fingerprints extends App {

// 
//:load C:/Users/mkielo/workspace/tools/NSFPScorer.scala
//

implicit object OutputAccumulable extends AccumulableParam[String,String] with Serializable {

    def zero(initialValue: String): String = {
      ""
    }

    def addAccumulator(currentValue: String, addition: String): String = {
      currentValue + addition
    }

    def addInPlace(Acc1: String, Acc2: String): String = {
      Acc1 + Acc2
    }
  }  

// def findMatchingBracket  ... implement this if necessary
// breaks if tests are missing  
//def customSplit(str: String): Array[String] = {
//  val tests = Array("SEQ(","OPS(","WIN(","ECN(","T1(","T2(","T3(","T4(","T5(","T6(","T7(","U1(","IE(")
//  
//  tests.map(r => {
//    println(r)
//	  str.substring(str.indexOf(r),str.indexOf(")",str.indexOf(r)) + 1)
//  })
//}

def fingerPrintsParser(fp: String): scala.collection.mutable.Map[String,String] = {
	val candidateTL = mutable.Map.empty[String,String]
	val comma = fp.split(",")

	comma.foreach(r => {

		val mainTest = r.substring(0,r.indexOf("("));
		val testLR = r.substring(r.indexOf("(")+1,r.indexOf(")"));
		val testLRsplit = testLR.split("%");
		testLRsplit.foreach(i => {
			val testname = (mainTest + i).split("=");
			if(testname.length == 2){
				candidateTL(testname(0)) = testname(1)
			};
			if(testname.length == 1){
				candidateTL(testname(0)) = ""}
			})
		})
	candidateTL
}


def buildRefDB(): List[(String,Map[String,String])] = {

	val textFile = Source.fromFile("src/main/scala/com/oculusinfo/stats/customAnalytics/data/nmap-os-db.txt")("UTF-8").getLines();
	//object of string OS descipritions and string OS raw test results
	val refDB = mutable.ListBuffer.empty[(String,Map[String,String])]
	while(!textFile.isEmpty){
		val block = textFile.takeWhile(!_.isEmpty).map(_+",")
		val blockString = block.addString(new StringBuilder).toString
		val parts = blockString.split("SEQ")

		val testsRaw = "SEQ" + parts(1)
		val testsClean = fingerPrintsParser(testsRaw).toMap

		val description = parts(0)
		val next = (description,testsClean)
		refDB += next
	}
	refDB.toList
}



def buildWeightDB() = {

	val textFile = Source.fromFile("src/main/scala/com/oculusinfo/stats/customAnalytics/data/nmap-weight-db.txt")("UTF-8").getLines();
	var test = ""

	textFile.foreach(r => {
		test += r + ","
		})

	val weightDBraw = fingerPrintsParser(test)
	val weightDB = mutable.Map.empty[String,Int]

	weightDBraw.foreach(r => {
		weightDB(r._1) = r._2.toInt
		})

	weightDB.toMap
}


def isHex(str: String): Boolean = {
	try {
		Integer.parseInt(str,16)
		true
	} catch {
		case e: Exception => false
	}
}


// Scorer helper functions
def scoreRange(candidate: String, reference: String): Boolean = {  
	if(isHex(candidate)){
		val dashCount = reference.count(_ == '-')
		
		if (dashCount == 1 &&
		    reference.substring(0,1) != ">" &&
		    reference.substring(0,1) != "-"){
		val range = reference.split("-")
		//convert from hex
		val min = Integer.parseInt(range(0),16)
		val max = Integer.parseInt(range(1),16)
		val candInt = Integer.parseInt(candidate,16)

		(candInt <= max && candInt >= min)
		}
		
		else if (dashCount == 2 &&
				reference.substring(0,1) == "-"){
		  	val range = reference.substring(1,reference.length).split("-")
			//convert from hex
			val min = Integer.parseInt(reference.substring(0,reference.indexOf("-",1)),16)
			val max = Integer.parseInt(reference.substring(reference.indexOf("-",1)+1,reference.length),16)
			val candInt = Integer.parseInt(candidate,16)
		
			(candInt <= max && candInt >= min)
		}
		
		else if (dashCount == 2 &&
		    reference.substring(0,1) != "-"){
		  	val range = reference.split("-",2)
			//convert from hex
			val min = Integer.parseInt(range(0),16)
			val max = Integer.parseInt(range(1),16)
			val candInt = Integer.parseInt(candidate,16)

			(candInt <= max && candInt >= min)
		}
		
		else if (dashCount == 3){
			//convert from hex
			val min = Integer.parseInt(reference.substring(0,reference.indexOf("-",1)),16)
			val max = Integer.parseInt(reference.substring(reference.indexOf("-",1)+1,reference.length),16)
			val candInt = Integer.parseInt(candidate,16)
			(candInt <= max && candInt >= min)
		}
		
		else {
		  false
		}
		} else {
		false
	}
}

def scoreGreater(candidate: String, reference: String): Boolean = {
	//convert from hex
	if(isHex(candidate)){
		val greaterVal = Integer.parseInt(reference.substring(1,reference.length),16)
		val candInt = Integer.parseInt(candidate,16)

		(candInt > greaterVal)
	} else {
	  false
	}
}

def scorer(fingerprint: String, refDB: List[(String, Map[String,String])], weightDB: Map[String,Int])= {
	val startTime = System.currentTimeMillis
  try{
	val candidateTRList: Map[String,String] = fingerPrintsParser(fingerprint).toMap
 	var i = 0
 	val amount = refDB.map(reference => {
// 	refDB.map(reference => {
 	  //println(1)
 		i += 1
 		val refTRPair = reference._2
 		val score = candidateTRList.map(candidateTest => {
 			//println(2)
 		 	if (refTRPair.contains(candidateTest._1)){
 		 		//println(3)

 		 		//println(candidateTest._1)
 		 		//println(candidateTest._2)
 		 		//println("#Candidate/Reference pair: ", candidateTest, " ", refTRPair(candidateTest._1))
 		 		val refValidTestResults = refTRPair(candidateTest._1).split("\\|")
 		 		//println(4)
	 			// Are there any of the allowed cases that match?
	 			if (refValidTestResults.exists(validResult => 
	 				// See if this option matches our observed value
	 				{
	 					//println("val: " + validResult)
	 					((validResult.contains("-") && scoreRange(candidateTest._2, validResult)) ||
	 				 (validResult.contains(">") && scoreGreater(candidateTest._2, validResult)) ||
	 				 validResult == candidateTest._2
	 				)})) {
	 				// yep, it matches
	 				//println(6)
	 				weightDB(candidateTest._1)
	 			} else {
	 			  0
	 			}
 		 	}
 		 	else {
 		 		0
 		 	}
 		 }).reduce(_ + _)
 		(reference._1, score)
 		})
 		val time = (startTime - System.currentTimeMillis) * 1000
 		(time,amount)
	} catch {
	  case e: Exception => {
	    val time = (startTime - System.currentTimeMillis) * 1000
	    (time,List((fingerprint,-1)))}
	  }
 }
 	
// test functions
//	val test = "SCAN(V=6.01%E=4%D=9/1%OT=80%CT=21%CU=41879%PV=N%DS=17%DC=I%G=N%TM=5041B791%P=mipsel-openwrt-linux-gnu),SEQ(SP=103%GCD=1%ISR=107%TI=I%CI=I%II=I%SS=S%TS=8),OPS(O1=M5B4NW0NNT11%O2=M5B4NW0NNT11%O3=M5B4NW0NNT11%O4=M5B4NW0NNT11%O5=M5B4NW0NNT11%O6=M5B4NNT11),WIN(W1=4000%W2=4000%W3=4000%W4=4000%W5=4000%W6=4000),ECN(R=Y%DF=Y%T=47%W=4000%O=M5B4NW0%CC=N%Q=),T1(R=Y%DF=Y%T=47%S=O%A=S+%F=AS%RD=0%Q=),T2(R=N),T3(R=Y%DF=Y%T=47%W=4000%S=O%A=S+%F=AS%O=M5B4NW0NNT11%RD=0%Q=),T4(R=Y%DF=N%T=47%W=0%S=A%A=Z%F=R%O=%RD=0%Q=),T5(R=Y%DF=N%T=47%W=0%S=Z%A=S+%F=AR%O=%RD=0%Q=),T6(R=Y%DF=N%T=47%W=0%S=A%A=Z%F=R%O=%RD=0%Q=),T7(R=Y%DF=N%T=47%W=0%S=Z%A=S%F=AR%O=%RD=0%Q=),U1(R=Y%DF=N%T=47%IPL=70%UN=0%RIPL=G%RID=G%RIPCK=G%RUCK=0%RUD=G),IE(R=Y%DFI=S%T=47%CD=S)"
// 	val refDB = buildRefDB
// 	println(refDB.size)
// 	val weightDB = buildWeightDB
//
//	println(scoreRange("103","0-106"))
//	println(scoreGreater("5",">2"))
//	println("Done")

// non-distributed version:
//def main(){
//  val refDB = buildRefDB
//  val weightDB = buildWeightDB
//  val table = scala.io.Source.fromFile("C:\\Users\\mkielo\\workspace\\vm\\net-scan\\fingerprints\\fingerPrintsCandidateSample1000.csv").getLines()
//
//  val writer = new PrintWriter("output/customAnalytic/FP.txt")
//  
//   	table.map(line => line.split("\t")(2)).foreach(r => {
// 	  val a = scorer(r, refDB, weightDB)
// 	  
// 	  a.foreach(r => {
// 	    val next = r._2.toString + ","
// 	    writer.write(next)
// 	  })
// 	  writer.write("\n")
// 	  })
//  
//}


// scala version

// distributed version
 def main(table: RDD[Array[String]], field: String, index: Int, customVars: String, writer: PrintWriter){
	
	val startTime = System.nanoTime
   
   	val sc = table.context	
   
 	val refDB = buildRefDB
 	val refBroadcast = sc.broadcast(refDB)
 	
 	val weightDB = buildWeightDB
 	val weightBroadcast = sc.broadcast(weightDB)

 	var outputAcc = sc.accumulable(OutputAccumulable.zero(""))

 	table.map(line => line(index)).foreach(r => {
 	  val a = scorer(r, refBroadcast.value, weightBroadcast.value)
 	  outputAcc += a._1.toString + ","
 	  a._2.foreach(r => {
 	    val next = r._2.toString + ","
 	    outputAcc += next
 	  })
 	  outputAcc += "\n"
 	  }
 	
 	)  	
 	writer.write(outputAcc.toString)
 	writer.close
 }
 
}