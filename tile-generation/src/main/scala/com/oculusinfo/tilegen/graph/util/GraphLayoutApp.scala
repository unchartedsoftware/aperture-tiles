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
import com.oculusinfo.tilegen.graph.util.ForceDirectedLayout
import com.oculusinfo.tilegen.spark.MavenReference
import com.oculusinfo.tilegen.spark.SparkConnector

object GraphLayoutApp {

	def main(args: Array[String]) {

		val argParser = new ArgumentParser(args)
		argParser.debug

		val jars =
			Seq(new MavenReference("com.oculusinfo", "tile-generation", "0.3-SNAPSHOT") //"twitter-utilities", "0.3-SNAPSHOT")
			) union SparkConnector.getDefaultLibrariesFromMaven
		val sc = argParser.getSparkConnector(jars).getSparkContext("Graph Clustering") //"Twitter demo data tiling")
		val source = argParser.getString("source", "The source location at which to find the data")
		val output = argParser.getString("output", "The output location where to save data")
		val partitions = argParser.getInt("partitions", "The number of partitions into which to read the raw data", Some(0))
		val parallelism = argParser.getInt("p", "Sets spark.default.parallelism and minSplits on the edge file. default=based on input partitions", Some(-1))
//		val minProgress = argParser.getDouble("xf", "Percent of vertices that must change communites for the algorithm to consider progress relative to total vertices in a level. default=0.15", Some(0.15))
//		val progressCounter = argParser.getInt("y", "Number of times the algorithm can fail to make progress before exiting. default=1", Some(1))
		val edgedelimiter = argParser.getString("d", "Specify input file edge delimiter. default is tab-delimited", Some("\t"))
		val maxIterations = argParser.getInt("i", "Max number of iterations for force-directed algorithm", Some(10000))
		
		//opt[Boolean]('z',"ipaddress") action {(x,c)=> c.copy(ipaddress=x)} text("Set to true to convert ipaddresses to Long ids. Defaults to false")	    

		var edgeFile = source
		var outputdir = output
		var ipaddress = false
		//sc.setCheckpointDir("hdfs://hadoop-s1/user/dgiesbrecht/checkpoint_dir")	//dgdg
		val fileStartTime = System.currentTimeMillis()

		// read the input data 
		val rawData = if (0 == partitions) {
			sc.textFile(edgeFile)
		} else {
			sc.textFile(edgeFile, partitions)
		}

		// store data a distributed edge list  
		//val inputHashFunc = if (ipaddress) (id: String) => IpAddress.toLong(id) else (id: String) => id.toLong
		val inputHashFunc = (id: String) => id.toLong
		var edgeRDD = rawData.map(row => { //sc.textFile(edgeFile).map(row=> {
			val tokens = row.split(edgedelimiter).map(_.trim())
			tokens.length match {
				case 2 => { new Edge(inputHashFunc(tokens(0)), inputHashFunc(tokens(1)), 1L) }
				case 3 => { new Edge(inputHashFunc(tokens(0)), inputHashFunc(tokens(1)), tokens(2).toLong) }
				case _ => { throw new IllegalArgumentException("invalid input line: " + row) }
			}
		})

		// if the parallelism option was set map the input to the correct number of partitions,
		// otherwise parallelism will be based off number of HDFS blocks
		if (parallelism != -1) edgeRDD = edgeRDD.coalesce(parallelism, shuffle = true)

		// create the graph
		val graph = Graph.fromEdges(edgeRDD, None)
		
		val layouter = new ForceDirectedLayout()
		val nodePositions = layouter.determineLayout(sc, graph, maxIterations)
				
		// save results
		nodePositions.saveAsTextFile(output)
		
		val fileEndTime = System.currentTimeMillis()
		println("Finished graph layout job in "+((fileEndTime-fileStartTime)/60000.0)+" minutes")
		
		println("DONE!!")
	}
	
}