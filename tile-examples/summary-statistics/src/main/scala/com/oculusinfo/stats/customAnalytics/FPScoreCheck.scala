package com.oculusinfo.stats.customAnalytics

import java.io._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

import org.apache.spark.AccumulableParam
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

object FPScoreCheck {
	
  def scoreLogger(fingerprint: String, refDB: List[(String, Map[String,String])], weightDB: Map[String,Int], writer: PrintWriter) = {
  val startTime = System.currentTimeMillis
  val FPtests = Fingerprints.validFP(fingerprint)
  if(FPtests(0)._2 == 1){ 
	  
    val cleanFP = Fingerprints.removeDuplicates(fingerprint, FPtests)
    
    writer.write(cleanFP + "\n")
    
	  try{
		val candidateTRList: Map[String,String] = Fingerprints.fingerPrintsParser(cleanFP)
	 	var i = 0
	 	val amount = refDB.map(reference => {
	 		i += 1
	 		writer.write("QQQQQ: " + i + " " + reference + "\n")
	 		val refTRPair = reference._2
	 		val score = candidateTRList.map(candidateTest => {
	 		var a = "ff"
	 		 	if (refTRPair.contains(candidateTest._1)){

	 		 		val refValidTestResults = refTRPair(candidateTest._1).split("\\|")
	 		 		
		 			// Are there any of the allowed cases that match?
		 			if (refValidTestResults.exists(validResult => 
		 				// See if this option matches our observed value
		 				{
		 				  a = validResult
	 					((validResult.contains("-") && Fingerprints.scoreRange(candidateTest._2.toString, validResult.toString)) ||
		 				 (validResult.contains(">") && Fingerprints.scoreGreater(candidateTest._2.toString, validResult.toString)) ||
		 				 validResult == candidateTest._2
		 				)})) {
		 				// yep, it matches
		 				writer.write("PASS: " + candidateTest._2 + " == " + a + "\n")
		 			} else {
		 			  writer.write("FAIL: " + candidateTest._2 + " != " + a + "\n")
		 			}
	 		 	} else {
	 		 		writer.write("MISSING: " + candidateTest._1 + "\n")
	 		 	}
	 		 })
	 		(reference._1, score)
	 		writer.write("NEWLINE\n")
	 		})
		} catch {
		  case e: Exception => {
		    writer.write("[ERROR]-EXCEPTION: " + fingerprint + "\nNEWLINE\n")
		    
		  }
		  }
  } else {
    writer.write("[ERROR]-FORMAT: " + fingerprint + "\n")
    }
 }
  

  
    def mainRun() = {
	
	val fp = "SCAN(V=6.01%E=4%D=9/4%OT=21%CT=23%CU=33146%PV=N%DS=7%DC=I%G=N%TM=50469599%P=mipsel-openwrt-linux-gnu),SEQ(SP=106%GCD=1%ISR=109%TI=Z%CI=Z%II=I%TS=8),OPS(O1=M5B4ST11NW7%O2=M5B4ST11NW7%O3=M5B4NNT11NW7%O4=M5B4ST11NW7%O5=M5B4ST11NW7%O6=M5B4ST11),WIN(W1=3890%W2=3890%W3=3890%W4=3890%W5=3890%W6=3890),ECN(R=Y%DF=Y%T=39%W=3908%O=M5B4NNSNW7%CC=Y%Q=),T1(R=Y%DF=Y%T=39%S=O%A=S+%F=AS%RD=0%Q=),T2(R=N),T3(R=Y%DF=Y%T=39%W=3890%S=O%A=S+%F=AS%O=M5B4ST11NW7%RD=0%Q=),T4(R=Y%DF=Y%T=39%W=0%S=A%A=Z%F=R%O=%RD=0%Q=),T5(R=N),T6(R=Y%DF=Y%T=39%W=0%S=A%A=Z%F=R%O=%RD=0%Q=),T7(R=Y%DF=Y%T=39%W=0%S=Z%A=S+%F=AR%O=%RD=0%Q=),U1(R=Y%DF=N%T=39%IPL=164%UN=0%RIPL=G%RID=G%RIPCK=G%RUCK=G%RUD=G),IE(R=Y%DFI=N%T=39%CD=S)"
  
   	val logwriter = new PrintWriter(new File("output/scoreVerificationLogs.txt"))
 	val refDB = Fingerprints.buildRefDB
 	val weightDB = Fingerprints.buildWeightDB
 	
 	scoreLogger(fp, refDB, weightDB, logwriter)
 	logwriter.close

    }


}