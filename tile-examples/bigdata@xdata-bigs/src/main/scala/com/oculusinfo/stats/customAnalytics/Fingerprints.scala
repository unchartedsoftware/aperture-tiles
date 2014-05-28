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


package com.oculusinfo.stats.customAnalytics

import java.io._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

import org.apache.spark.AccumulableParam
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
//Fingerprints.fingerPrintsParser("SCAN(V=5.51%D=1/2%OT=21%CT=22%CU=38310%PV=N%DS=4%DC=I%G=Y%TMFingerprints.=1F389%P=mip)")

object Fingerprints extends App {

//:load C:/Users/mkielo/workspace/tools/NSFPScorer.scala




//validFP is used to indicate whether a candidate fingerprint (fp) has a valid format.
//A valid format is defined as meeting all of the following criteria: 
//	- # of open brackets == # of closed brackets == (# of tests + 1) == (# of commas + 1)
//	- # of percentage signs == (# equal signs - # of tests - 1)
//	- There are not sets of brackets nested inside of another set of brackets. 
//	- There are no commas inside pairs of brackets
//	- Each test name is immediately preceeded by an open bracket. Ex: 'SEQ('

//	validFP returns a list of tuples. Each tuple contains a string and an integer. The string is the name of the fp test, 
//and the integer indicates the number of times that test is conducted in the candidate string. The returned listed begins
//with the string "validFP", which has a corresponding value of 1 if the candidate fp has a valid format, and 0 otherwise. 
def validFP(fp: String): List[(String, Integer)] = {
  var openBrackets = 0
  var closedBrackets = 0
  var commas = 0
  
  // helper variable to ensure no bad characters are ever inside brackets
  var inOrOut= "out"
  
  // ensures pairs of brackets are never nested inside brackets
  var bracketsCheck = true
  // ensures there are never commas inside brackets
  var commaCheck = true
  
  var percentageSigns = 0
  var equalSigns = 0
  
  var fpCopy = ""
  var fpcLen = 0
  
  var seqCount = 0
  var opsCount = 0
  var winCount = 0
  var ecnCount = 0
  var t1Count = 0
  var t2Count = 0
  var t3Count = 0
  var t4Count = 0
  var t5Count = 0
  var t6Count = 0
  var t7Count = 0
  var u1Count = 0
  var ieCount = 0
  
  var tempstr = ""
  
  fp.foreach(char => {
    
    tempstr = char.toString
    
    fpCopy += char
    fpcLen += 1
    
	  //checks for (
	if((tempstr == "(") && (inOrOut.equals("in"))){
	    bracketsCheck = false
	 //   println(tempstr)
	  } else if((tempstr == "(") && (inOrOut.equals("out"))){
	    openBrackets += 1
	//    println(tempstr)
	    inOrOut = "in"    
	  }
	
	  //checks for ) 
	  if((tempstr == ")") && (inOrOut.equals("out"))){
	    bracketsCheck = false
	   // println(tempstr)
	  } else if((tempstr == ")") && (inOrOut.equals("in"))){
	    closedBrackets += 1
	    inOrOut = "out"
	   // println(tempstr)
	  }
	  
	  //checks for ,
	  if((tempstr == ",") && (inOrOut.equals("out"))){
	    commas += 1
	  } else if((tempstr == ",") && (inOrOut.equals("in"))){
	    commaCheck = false
	  }
	  
	  //checks for %
	  if(tempstr == "%"){
	    percentageSigns += 1
	  }
	  
	  //checks for =
	  if(tempstr == "="){
	     equalSigns += 1
	  }
	  
	  if((fpcLen > 4) && (fpCopy.substring(fpcLen - 4, fpcLen) == "SEQ(")){
		seqCount += 1

	} else if((fpcLen > 4) && (fpCopy.substring(fpcLen - 4, fpcLen) == "OPS(")){
		opsCount += 1

	} else if((fpcLen > 4) && (fpCopy.substring(fpcLen - 4, fpcLen) == "WIN(")){
		winCount += 1

	} else if((fpcLen > 4) && (fpCopy.substring(fpcLen - 4, fpcLen) == "ECN(")){
		ecnCount += 1

	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "T1(")){
		t1Count += 1

	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "T2(")){
		t2Count += 1

	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "T3(")){
		t3Count += 1
	
	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "T4(")){
		t4Count += 1
	
	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "T5(")){
		t5Count += 1
		
	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "T6(")){
		t6Count += 1

	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "T7(")){
		t7Count += 1

	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "U1(")){
		u1Count += 1
	
	} else if((fpcLen > 3) && (fpCopy.substring(fpcLen - 3, fpcLen) == "IE(")){
		ieCount += 1

	}

  })
  

  val totalTests = (seqCount + opsCount + winCount
					 + ecnCount + t1Count + t2Count
					 + t3Count + t4Count + t5Count
					 + t6Count + t7Count + u1Count 
					 + ieCount)

//println("totalTests: " + totalTests)
//println("openBrackets: " + openBrackets)
//println("closedBrackets: " + closedBrackets)
//println("commas: " + commas)
//println("percentageSigns: " + percentageSigns)
//println("equalSigns: " + equalSigns)
//println("bracketsCheck: " + bracketsCheck)
//println("commaCheck: " + commaCheck)

//println(seqCount)
//println(opsCount)
//println(winCount)
//println(ecnCount)
//println(t1Count)
//println(t2Count)
//println(t3Count)
//println(t4Count)
//println(t5Count)
//println(t6Count)
//println(t7Count)
//println(u1Count)
//println(ieCount)
//


	var validFP = 0

	  if((openBrackets == closedBrackets && openBrackets == (totalTests + 1) && openBrackets == (commas + 1))
	//ensure # of '%' equals # of "=" - 1
		&& (percentageSigns == (equalSigns - totalTests - 1))
	//ensure no nested brackets, no commas inside brackets
		&& (bracketsCheck && commaCheck)){
	    validFP = 1
	  }
	
  List(("validFP", validFP), ("SEQ(", seqCount), ("OPS(", opsCount), ("WIN(", winCount), ("ECN(", ecnCount), ("T1(", t1Count), ("T2(", t2Count), ("T3(", t3Count), ("T4(", t4Count), ("T5(", t5Count), ("T6(", t6Count), ("T7(", t7Count), ("U1(", u1Count), ("IE(", ieCount))

}  
    


//	fingerPrintsParser takes in a fingerprint (fp) text string, and returns a map of test/result pairs
//for each test found in the fp text string.

//fingerPrintsParser makes the following assumptions:
//	- Main tests are delimited with ','
//	- Sub tests are delimited with '%'
//	- The fp being passed satisfies validFP

def fingerPrintsParser(fp: String): scala.collection.immutable.Map[String,String] = {
	val candidateTL = mutable.Map.empty[String,String]
	val comma = fp.split(",")

	comma.foreach(r => {

		val mainTest = r.substring(0,r.indexOf("("));
		val testLR = r.substring(r.indexOf("(")+1,r.indexOf(")"));
		val testLRsplit = testLR.split("%");
		testLRsplit.foreach(i => {

			val testname = (mainTest + i).split("=");
			if(testname.size == 2){
				candidateTL(testname(0)) = testname(1)
			};
			//If the test has no result specified, defaul the result to "". Ex: for "TESTX=" we add (TESTX -> "") to the map
			if(testname.size == 1){
				candidateTL(testname(0)) = ""}
			})
		})
	candidateTL.toMap
}


//buildRefDB consumes the raw nmap-os-db textfile and cleans it to simplify comparisons between candidate and reference fingerprints

//	buildRefDB outputs a list of (String,Map) tuples. The string is the reference OS name. The map is (string -> string) of (tests -> results) 
//and is the output of running fingerPrintsParser on each reference's test string. Pulled from: https://svn.nmap.org/nmap/nmap-os-db
def buildRefDB(): List[(String,Map[String,String])] = {

	val textFile = Source.fromFile("src/main/scala/com/oculusinfo/stats/customAnalytics/data/nmap-os-db.txt")("UTF-8").getLines();
	
	val refDB = mutable.ListBuffer.empty[(String,Map[String,String])]
	while(!textFile.isEmpty){
		//reads in all data from 1 OS
		val block = textFile.takeWhile(!_.isEmpty).map(_+",")
		val blockString = block.addString(new StringBuilder).toString

		//splits the data into 2 parts: OS description and OS test results
		val parts = blockString.split("SEQ")
		val testsRaw = "SEQ" + parts(1)

		//score the test part of OS data
		val testsClean = fingerPrintsParser(testsRaw)

		val description = parts(0)
		val next = (description,testsClean)
		refDB += next
	}
	refDB.toList
}

//buildWeightDB consumes the nmap-os-weight textfile and cleans it to simplify the scoring of matching candidate and reference tests

//	buildRefDB outputs a Map of (String -> Int). Where the string is the name of the test, and the Int is the weight assigned to the
//given test according to the matchpoints entry on: https://svn.nmap.org/nmap/nmap-os-db
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

//returns true if the submitted string is a valid hexadecimal string. False otherwise. Returns false for the empty string.
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
	if(isHex(candidate.toString)){
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
	if(isHex(candidate.toString)){
		val greaterVal = Integer.parseInt(reference.substring(1,reference.length),16)
		val candInt = Integer.parseInt(candidate,16)

		(candInt > greaterVal)
	} else {
	  false
	}
}



def removeDuplicates(fp: String, tests: List[(String, Integer)]): String = {
  var cleanFP = ""
  tests.foreach(r => {
    if(r._2 > 0 && (!r._1.equals("validFP"))){
      val firstPos = fp.indexOf(r._1)
      val endPos = fp.indexOf(")", firstPos + 1)
      cleanFP += fp.substring(firstPos,endPos+1) + ","
    }  
  }) 
  cleanFP
}




def scorer(fingerprint: String, refDB: List[(String, Map[String,String])], weightDB: Map[String,Int])= {
  val startTime = System.currentTimeMillis
  val fingerprint2 = fingerprint.replace(",OS:","")
  val FPtests = validFP(fingerprint2)
  if(FPtests(0)._2 == 1){ 
	  
    val cleanFP = removeDuplicates(fingerprint, FPtests)
    
	  try{
		val candidateTRList: Map[String,String] = fingerPrintsParser(cleanFP)
	 	var i = 0
	 	val amount = refDB.map(reference => {
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
		 					((validResult.contains("-") && scoreRange(candidateTest._2.toString, validResult.toString)) ||
		 				 (validResult.contains(">") && scoreGreater(candidateTest._2.toString, validResult.toString)) ||
		 				 validResult == candidateTest._2
		 				)})) {
		 				// yep, it matches
		 				//println(6)
		 				weightDB(candidateTest._1)
		 			} else {
		 			  0
		 			}
	 		 	} else {
	 		 		0
	 		 	}
	 		 }).reduce(_ + _)
	 		(reference._1, score)
	 		})
	 		val time = (System.currentTimeMillis - startTime)
	 		(time,amount)
		} catch {
		  case e: Exception => {
		    val time = (System.currentTimeMillis - startTime)
		    (time,List((cleanFP,-1)))}
		  }
  } else {
    val time = (System.currentTimeMillis - startTime)
    (time,List((fingerprint,-2)))
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

// distributed version

 def run(table: RDD[Array[String]], field: String, index: Int, customVars: String, writer: PrintWriter){
	   
   val startTime = System.currentTimeMillis
	
   	val logwriter = new PrintWriter(new File("output/logs.txt"))
   	val sc = table.context	
   	val t1 = System.currentTimeMillis - startTime
 	val refDB = buildRefDB
 	val refBroadcast = sc.broadcast(refDB)
 	val t2 = System.currentTimeMillis - startTime
 	val weightDB = buildWeightDB
 	val t3 = System.currentTimeMillis - startTime
	val weightBroadcast = sc.broadcast(weightDB)
	val t4 = System.currentTimeMillis - startTime
	val t5 = System.currentTimeMillis - startTime
 
	val newTable = table.map(r => {
 	  	val a = scorer(r(index), refBroadcast.value, weightBroadcast.value)
 		var outString = a._1.toString + ","  
		a._2.foreach(test => {
 	   	 outString += test._2.toString + ","
 	    	})
		outString
 	  })
 	  
 	newTable.saveAsTextFile(customVars)

	val t6 = System.currentTimeMillis - startTime  	

	val t7 = System.currentTimeMillis - startTime
 	writer.close
	logwriter.write("T1: " + t1 + "\nT2: " + t2 + "\nT3: " + t3 + "\nT4: " + t4 + "\nT5: " + t5 + "\nT6: " + t6 + "\nT7: " + t7) 
	logwriter.close
}
 
}
