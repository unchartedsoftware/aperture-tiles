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

package com.oculusinfo.tilegen.graph.util
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._

import com.oculusinfo.tilegen.util.ArgumentParser
//import com.oculusinfo.tilegen.graph.util.ForceDirectedLayout
import com.oculusinfo.tilegen.spark.MavenReference
import com.oculusinfo.tilegen.spark.SparkConnector

object ClusteredGraphLayoutApp {

	def main(args: Array[String]) {

		val argParser = new ArgumentParser(args)
		argParser.debug

		val jars =
			Seq(new MavenReference("com.oculusinfo", "tile-generation", "0.3-SNAPSHOT")
			) union SparkConnector.getDefaultLibrariesFromMaven
		val sc = argParser.getSparkConnector(jars).getSparkContext("Clustered Graph Layout") 
		val sourceDir = argParser.getString("source", "The source directory where to find clustered graph data")
		val outputDir = argParser.getString("output", "The output location where to save data")
		val partitions = argParser.getInt("partitions", "The number of partitions into which to read the raw data", Some(0))
		val consolidationPartitions = argParser.getInt("p", "The number of partitions for data processing. Default=based on input partitions", Some(0))
		val dataDelimiter = argParser.getString("d", "Delimiter for the source graph data. Default is comma-delimited", Some(","))
		val maxIterations = argParser.getInt("i", "Max number of iterations for force-directed algorithm", Some(500))
		val maxHierarchyLevel = argParser.getInt("maxLevel","Max cluster hierarchy level to use for determining graph layout", Some(0))
		val borderOffset = argParser.getInt("border","Percent of boundingBox width and height to leave as whitespace when laying out leaf nodes. Default is 5 percent", Some(5))
		val layoutLength = argParser.getDouble("layoutLength", "Desired width/height length of the total node layout region. Default = 256.0", Some(256.0))	
		
		val fileStartTime = System.currentTimeMillis()
		
		val layouter = new HierarchicGraphLayout()
		val nodePositions = layouter.determineLayout(sc, 
													maxIterations, 
													maxHierarchyLevel, 
													partitions, 
													consolidationPartitions, 
													sourceDir, 
													dataDelimiter,
													(layoutLength,layoutLength),
													borderOffset)
		
		nodePositions.saveAsTextFile(outputDir)	// save results -- format is (nodeID, x coord, y coord) 
		
		val fileEndTime = System.currentTimeMillis()
		println("Finished hierarchic graph layout job in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")
		
		println("DONE!!")
	}
		
	
}