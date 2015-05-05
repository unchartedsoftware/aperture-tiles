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
import com.oculusinfo.tilegen.util.ArgumentParser
//import com.oculusinfo.tilegen.graph.util.ForceDirectedLayout
import com.oculusinfo.tilegen.spark.SparkConnector

object ClusteredGraphLayoutApp {

	def main(args: Array[String]) {

		val argParser = new ArgumentParser(args)
		argParser.debug

		val sc = argParser.getSparkConnector.createContext(Some("Clustered Graph Layout"))
		val sourceDir = argParser.getString("source", "The source directory where to find clustered graph data")
		val outputDir = argParser.getString("output", "The output location where to save data")
		val partitions = argParser.getInt("parts", "The number of partitions into which to read the raw data", Some(0))
		val consolidationPartitions = argParser.getInt("p", "The number of partitions for data processing. Default=based on input partitions", Some(0))
		val dataDelimiter = argParser.getString("d", "Delimiter for the source graph data. Default is tab-delimited", Some("\t"))
		val maxIterations = argParser.getInt("i", "Max number of iterations for force-directed algorithm", Some(500))
		val maxHierarchyLevel = argParser.getInt("maxLevel","Max cluster hierarchy level to use for determining graph layout", Some(0))
		val borderPercent = argParser.getDouble("border","Percent of parent bounding box to leave as whitespace between neighbouring communities during initial layout. Default is 2 percent", Some(2.0))
		val layoutLength = argParser.getDouble("layoutLength", "Desired width/height length of the total node layout region. Default = 256.0", Some(256.0))
		val nodeAreaPercent = argParser.getInt("nArea", "Used for Hierarchical Force-directed layout ONLY. Sets the area of all node 'circles' within the boundingBox vs whitespace.  Default is 30 percent", Some(30))
		val bUseEdgeWeights = argParser.getBoolean("eWeight", "Use edge weights, if present, to scale force-directed attraction forces.  Default is false", Some(false))
		val gravity = argParser.getDouble("g", "Amount of gravitational force to use for Force-Directed layout to prevent outer nodes from spreading out too far. Default = 0 (no gravity)", Some(0.0))
		val isolatedDegreeThres = argParser.getInt("degreeThres", "Degree threshold used to define 'leaf communities'. Such leaf communities are automatically laid out in an outer radial/spiral pattern. Default = 0", Some(0))
		val communitySizeThres = argParser.getInt("commSizeThres", "Community size threshold used to exclude communities with < communitySizeThres nodes from layout. Default = 0", Some(0))
 		
		val fileStartTime = System.currentTimeMillis()
		
		// Hierarchical Force-Directed layout scheme
		val layouter = new HierarchicFDLayout()
		
		layouter.determineLayout(sc,
		                         maxIterations,
		                         maxHierarchyLevel,
		                         partitions,
		                         consolidationPartitions,
		                         sourceDir,
		                         dataDelimiter,
		                         (layoutLength,layoutLength),
		                         borderPercent,
		                         nodeAreaPercent,
		                         bUseEdgeWeights,
		                         gravity,
		                         isolatedDegreeThres,
		                         communitySizeThres,
		                         outputDir)
		
		val fileEndTime = System.currentTimeMillis()
		println("Finished hierarchic graph layout job in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")
		
		println("DONE!!")
	}
	
	
}
