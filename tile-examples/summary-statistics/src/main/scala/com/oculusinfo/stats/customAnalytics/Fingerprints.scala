package com.oculusinfo.stats.customAnalytics

import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.io.Source

import java.io._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

object Fingerprints extends App {

// 
//:load C:/Users/mkielo/workspace/tools/NSFPScorer.scala
//

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

	val textFile = Source.fromFile("c:/Users/mkielo/workspace/tools/nmap-os-db.txt")("UTF-8").getLines();
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

	val textFile = Source.fromFile("c:/Users/mkielo/workspace/tools/nmap-weight-db.txt")("UTF-8").getLines();
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
		println(min, " ", max, " ", candInt)

		(candInt <= max && candInt >= min)
		}
		
		else if (dashCount == 2 &&
				reference.substring(0,1) == "-"){
		  	val range = reference.substring(1,reference.length).split("-")
			//convert from hex
			val min = Integer.parseInt(reference.substring(0,reference.indexOf("-",1)),16)
			val max = Integer.parseInt(reference.substring(reference.indexOf("-",1)+1,reference.length),16)
			val candInt = Integer.parseInt(candidate,16)
			println(min, " ", max, " ", candInt)
		
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
		println(candInt, greaterVal)
		
		(candInt > greaterVal)
	} else {
	  false
	}
}

def scorer(fingerprint: String, refDB: List[(String, Map[String,String])], weightDB: Map[String,Int])= {

	val candidateTRList: Map[String,String] = fingerPrintsParser(fingerprint).toMap
 	var i = 0
 	refDB.map(reference => {
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
 }
 	

//	val test = "SCAN(V=6.01%E=4%D=9/1%OT=80%CT=21%CU=41879%PV=N%DS=17%DC=I%G=N%TM=5041B791%P=mipsel-openwrt-linux-gnu),SEQ(SP=103%GCD=1%ISR=107%TI=I%CI=I%II=I%SS=S%TS=8),OPS(O1=M5B4NW0NNT11%O2=M5B4NW0NNT11%O3=M5B4NW0NNT11%O4=M5B4NW0NNT11%O5=M5B4NW0NNT11%O6=M5B4NNT11),WIN(W1=4000%W2=4000%W3=4000%W4=4000%W5=4000%W6=4000),ECN(R=Y%DF=Y%T=47%W=4000%O=M5B4NW0%CC=N%Q=),T1(R=Y%DF=Y%T=47%S=O%A=S+%F=AS%RD=0%Q=),T2(R=N),T3(R=Y%DF=Y%T=47%W=4000%S=O%A=S+%F=AS%O=M5B4NW0NNT11%RD=0%Q=),T4(R=Y%DF=N%T=47%W=0%S=A%A=Z%F=R%O=%RD=0%Q=),T5(R=Y%DF=N%T=47%W=0%S=Z%A=S+%F=AR%O=%RD=0%Q=),T6(R=Y%DF=N%T=47%W=0%S=A%A=Z%F=R%O=%RD=0%Q=),T7(R=Y%DF=N%T=47%W=0%S=Z%A=S%F=AR%O=%RD=0%Q=),U1(R=Y%DF=N%T=47%IPL=70%UN=0%RIPL=G%RID=G%RIPCK=G%RUCK=0%RUD=G),IE(R=Y%DFI=S%T=47%CD=S)"
// 	val refDB = buildRefDB
// 	println(refDB.size)
// 	val weightDB = buildWeightDB
//
//	println(scoreRange("103","0-106"))
//	println(scoreGreater("5",">2"))
//	println("Done")

 def main(table: RDD[Array[String]], field: String, index: Int, customVars: String){
	
   	val sc = table.context
   	
   //might need to broadcast.?
	val writer = new PrintWriter(new File(customVars))
   //val writerBroadcast = sc.broadcast(writer) ... how would I close this?
   	
 	val refDB = buildRefDB
 	val refBroadcast = sc.broadcast(refDB)
 	
 	val weightDB = buildWeightDB
 	val weightBroadcast = sc.broadcast(weightDB)
 	
 	table.map(line => line(index)).foreach(r => {
 	  val a = scorer(r, refBroadcast.value, weightBroadcast.value)
 	  
 	})  	

 }
}