import scala.math.random
import org.apache.spark._
import SparkContext._
import org.apache.spark.rdd.RDD
import simple_statistics.qualitative.Frequency

/**
 * @author ${user.name}
 */

/*WILL EVENTUALLY NEED TO UNCOMMENT HADOOP IN POM & SORT OUT VERSION ISSUE*/

object fileIOtest {
  def main(args: Array[String]) {
    println("stop1")
    //Set path to file
    val logFile = "src/test.txt"
    println("stop2")
    val sc = new SparkContext("local", "Simple Job")
    println("stop3")
    val logData = sc.textFile(logFile, 2).cache()
    println("stop4")
    val numAs = logData.filter(line => line.contains("a")).count()
    println("stop5")
    val numBs = logData.filter(line => line.contains("b")).count()
    println("Lines with a: %s, Lines with b: %s".format(numAs, numBs))
  }
}