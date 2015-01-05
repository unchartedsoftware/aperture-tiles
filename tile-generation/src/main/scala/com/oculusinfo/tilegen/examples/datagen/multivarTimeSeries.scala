/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tilegen.examples.datagen

import com.oculusinfo.tilegen.util.MissingArgumentException
import com.oculusinfo.tilegen.util.ArgumentParser
import java.util.GregorianCalendar
import java.util.Calendar
import java.util.Date
import scala.collection.immutable.NumericRange
import scala.util.Random
import java.io.File
import java.io.PrintWriter

object MultivarTimeSeries {
	
	/**
	 * Breaks the input time range up into sub-ranges and returns them as a list.  
	 * If the step in the time range is not equally divided by the step, the 
	 *  range will be truncated.
	 */
	private def computePartitionRanges(partitions: Int, timeRange: NumericRange[Long]) = {
		val partitionExtra = (timeRange.end - timeRange.start) % timeRange.step
		val aliasedRange = (timeRange.start to timeRange.end - partitionExtra by timeRange.step)
		val partitionSize = (aliasedRange.end - aliasedRange.start) / aliasedRange.step / partitions * aliasedRange.step

		def computePartitionRange(partitionRange: NumericRange[Long], partitionNumber: Int): Seq[NumericRange[Long]] = {
			val newRangeList = List(partitionRange.start to partitionRange.end by partitionRange.step)
			if (partitionNumber == partitions - 1) {
				newRangeList ++: computePartitionRange(
					partitionRange.end + aliasedRange.step to aliasedRange.end by aliasedRange.step, partitionNumber + 1)
			} else if (partitionNumber < partitions - 1) {
				newRangeList ++: computePartitionRange(
					partitionRange.end + aliasedRange.step to partitionRange.end + partitionSize + aliasedRange.step by aliasedRange.step, partitionNumber + 1)
			} else {
				newRangeList
			}
		}
		computePartitionRange(aliasedRange.start to aliasedRange.start + partitionSize by aliasedRange.step, 1)
	}
	
	/**
	 * Creates a sequence of names based on a sequence source values.  Names are formatted
	 * name-#.
	 */
	private def generateNames(numNames: Int, names: IndexedSeq[_]) = {
		for (i <- 0 until numNames) yield names(i % names.length) + "-" + i / names.length
	}

	/**
	 * Spark job main
	 */
	def main(args: Array[String]): Unit = {
		val argParser = new ArgumentParser(args)
		try {
			argParser.debug
			
			val fileName = argParser.getString("output", "Name for generated output files", Some("multivar_data"))

			val numPoints = argParser.getLong("timestamps", "Number of timestamps to generate", Some(1000))
			val partitions = argParser.getInt("partitions", "Number of partitions to split the data into when generating", Some(4))
			
			val numDays = argParser.getInt("days", "Number of days in the time period", Some(90))
			
			val numVariables = argParser.getInt("variables", "Number of variables for each timestamp", Some(10))
			
			val octaves = argParser.getInt("octaves", "Number of frequences to use in random signal", Some(6))
			val h = argParser.getDouble("h", "Noise increment value", Some(0.1))
			val lacunarity = argParser.getDouble("lacunarity", "Gap between frequences in random signal", Some(2.0))
			
			val startTime = new Date().getTime()
			val cal = new GregorianCalendar()
			cal.add(Calendar.DAY_OF_MONTH, numDays)
			val endTime = cal.getTime().getTime()
			val timeRange = (startTime to endTime by (endTime - startTime) / numPoints)
			
			val jobName = "Multi-variable Timeseries Generator"
			val sc = argParser.getSparkConnector().createContext(Some(jobName))
			
			// Create the RDD for spark to operate on.
			val parallelCollection = sc.parallelize(computePartitionRanges(partitions, timeRange), partitions)
			
			/**
			 * Generates a time series where each timestamp has a corresponding set of variable values generated
			 * using perlin noise.  The min is computed and added to the entire dataset to ensure positive values.
			 */
			val generateVarData = (timeStamp: Long, timeRange: NumericRange[Long], numVars: Int, h: Double, octaves: Int, lacunarity: Double) => {
				val offsets = Array.fill(numVars)(Random.nextDouble);
				val noiseVals = for (i <- 0 until numVars) yield {
					Multifractal.fBm(((timeStamp.toDouble - timeRange.start) / (timeRange.end - timeRange.start)) + 0.1, offsets(i), 0.0, h, octaves, lacunarity)
				}
				val result = noiseVals map (_ + -noiseVals.foldLeft(noiseVals.head)(Math.min(_,_)))
				result.mkString(timeStamp + ",", ",", "")
			}

			// Run spark parallel job against each time range and save the result to a text file
			parallelCollection mapPartitions { timeRangeIter =>
				val timeRange = timeRangeIter.next()
				timeRange.iterator map (time => generateVarData(time, timeRange, numVariables, h, octaves, lacunarity))
			} saveAsTextFile(fileName)
			
			sc.stop()
			
		} catch {
			case e: MissingArgumentException => {
				println("Argument exception: " + e.getMessage())
				argParser.usage
			}
		}
	}
}
