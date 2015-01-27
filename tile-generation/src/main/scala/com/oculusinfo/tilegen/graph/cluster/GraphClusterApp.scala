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

package com.oculusinfo.tilegen.graph.cluster

import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.util.ArgumentParser
import org.apache.spark.graphx._


/**
 * Sample application for hierarchical clustering of graph data using Louvain-based community detection.
 * 
 * Adapted from Sotera's graphX implementation of the distributed Louvain modularity algorithm
 * https://github.com/Sotera/spark-distributed-louvain-modularity
 * 
 * This application handles reading in a graph dataset from a CSV file, and performing hierarchical clustering.
 * 
 * NOTE:  It is expected that the 1st column of the CSV graph data will contain either the keyword "node"
 * for data lines representing a graph's node/vertex, or the keyword "edge" for data lines representing
 * a graph's edge.  However, if "-onlyEdges" is set to true, then the source data can simply be a delimited file
 * of only edges.  Node ID's must be type Long.
 * 
 * The following command line arguments control how the application runs:
 * 
 * -source  -- The source location at which to find the data [required].
 * -onlyEdges -- [Boolean, optional] If set to true then source data can simply be a delimited file of edges only. Default = false.
 * -output -- The output location where to save results [required].
 * 				Note: the format for community results is CommunityID \t Parent community ID \t Number of internal nodes \t Community Degree
 * -parts -- The number of partitions into which to break up each source file.  Default = chosen automatically by Spark.
 * -p -- Sets spark.default.parallelism and minSplits on the edge file. Default = based on input partitions.
 * -progMin -- Percent of nodes that must change communites for the algorithm to consider progress relative to total vertices in a level. Default = 0.15
 * -progCount -- Number of times the algorithm can fail to make progress before exiting. Default = 1.
 * -d -- Specify the input dataset delimiter. Default is tab-delimited.
 * -nID -- The column number in the raw data of node ID's.  ID's must be type Long [required if onlyEdges=false].
 * -nAttr -- Column numbers of additional node metadata to parse and save with cluster results (attribute ID tags separated by commas) [optional].
 * -eSrcID -- The column number of an edge's source ID.  ID's must be type Long [required].
 * -eDstID -- The column number of an edge's destination ID.  ID's must be type Long [required].
 * -eWeight -- The column number of an edge's weight.  Default = -1, meaning no edge weighting is used.
 * 
 * Results for each hierarchical level are stored in a "level_#" sub-directory, with the following data format,
 * For nodes:
 * 		"node\t" + id + "\t" + parentId + "\t" + internalNodes + "\t" + nodeDegree + "\t" + extraAttributes (tab-delimited)
 * And for edges:
 * 		"edge\t" + srcID + "\t" + dstID + "\t" + edge weight
 **/

object GraphClusterApp {
	
	def main(args: Array[String]) {

		val argParser = new ArgumentParser(args)
		argParser.debug

		val sc = argParser.getSparkConnector.createContext(Some("Graph Clustering"))
		
		val sourceFile = argParser.getString("source", "The source location at which to find the data")
		val bOnlyEdges = argParser.getBoolean("onlyEdges", "If set to true then source data can simply be a delimited file of edges only", Some(false))
		val outputDir = argParser.getString("output", "The output location where to save data")
		val partitions = argParser.getInt("parts", "The number of partitions into which to break up each source file", Some(0))
		val parallelism = argParser.getInt("p", "Sets spark.default.parallelism and minSplits on the edge file. Default=based on input partitions", Some(-1))
		val minProgress = argParser.getDouble("progMin", "Percent of nodes that must change communites for the algorithm to consider progress relative to total vertices in a level. default=0.15", Some(0.15))
		val progressCounter = argParser.getInt("progCount", "Number of times the algorithm can fail to make progress before exiting. default=1", Some(1))
		val edgedelimiter = argParser.getString("d", "Specify input file edge delimiter. Default is tab-delimited", Some("\t"))
		val nodeIDindex = argParser.getInt("nID", "The column number in the raw data of node ID's", Some(-1))
		val edgeSrcIDindex = argParser.getInt("eSrcID", "The column number of an edge's source ID", Some(0))
		val edgeDstIDindex = argParser.getInt("eDstID", "The column number of an edge's source ID", Some(1))
		val edgeWeightIndex = argParser.getInt("eWeight", "The column number of an edge's weight", Some(-1))
		val nodeAttrIndices = argParser.getString("nAttr",
		                                          "Column numbers of additional node metadata to parse and save with cluster results (attribute ID tags separated by commas)",
		                                          Some("-1")).split(",").map(_.trim().toInt)
		
		// read the input data
		val rawData = if (0 == partitions) {
			sc.textFile(sourceFile)
		} else {
			sc.textFile(sourceFile, partitions)
		}
		
		//TODO -- edge weights are currently assumed to be Long.  Should this be changed to Double?
		// (would need to modify internal Louvain clustering code)
		
		// Parse the raw data into an edgeRDD
		var edgeRDD = if (bOnlyEdges) {
			//input data is a delimited file of edges-only
			rawData.map(row =>
				{
					val tokens = row.split(edgedelimiter).map(_.trim())
					val srcID = tokens(edgeSrcIDindex).toLong
					val dstID = tokens(edgeDstIDindex).toLong
					val weight = if (edgeWeightIndex == -1) 1L else tokens(edgeWeightIndex).toLong
					new Edge(srcID, dstID, weight)
				}
			)
		}
		else {
			//check the first column to see which rows correspond to edges or nodes
			rawData.flatMap(row =>
				{
					val tokens = row.split(edgedelimiter).map(_.trim())
					if (tokens(0) == "edge") {
						val srcID = tokens(edgeSrcIDindex).toLong
						val dstID = tokens(edgeDstIDindex).toLong
						val weight = if (edgeWeightIndex == -1) 1L else tokens(edgeWeightIndex).toLong
						Some(new Edge(srcID, dstID, weight))
					}
					else {
						None
					}
				}
			)
		}
		
		// if the parallelism option was set map the input to the correct number of partitions,
		// otherwise parallelism will be based off number of HDFS blocks
		if ((parallelism != -1 ) && (edgeRDD.partitions.size != parallelism)) {
			edgeRDD = edgeRDD.coalesce(parallelism,shuffle=true)
		}
		
		if ((bOnlyEdges) || (nodeAttrIndices(0) == -1)) {
			// create the graph
			val graph = Graph.fromEdges(edgeRDD, None)
			
			// use a helper class to execute the louvain algorithm and save the output.
			val runner = new HDFSLouvainRunner(minProgress,progressCounter,outputDir)
			runner.run(sc, graph)
		}
		else {
			
			if (nodeIDindex <= 0)
				throw new IllegalArgumentException("nID arguement must be > 0")

			//check the first column to see which rows correspond to nodes
			// TODO -- this flatMap operation should be combined with one above so we aren't iterating through the dataset twice
			var nodeRDD = rawData.flatMap(row =>
				{
					val tokens = row.split(edgedelimiter).map(_.trim())
					if (tokens(0) == "node") {
						val nodeID = tokens(nodeIDindex).toLong
						var nodeAttributes = ""
						val len = nodeAttrIndices.size
						val lenTokens = tokens.size
						for (i <- 0 until len) {
							if (nodeAttrIndices(i) < lenTokens) {
								nodeAttributes += tokens(nodeAttrIndices(i))
							}
							if (i < len-1)
								nodeAttributes += "\t"
						}
						Some((nodeID, nodeAttributes))
					}
					else {
						None
					}
				}
			)
			
			// if the parallelism option was set map the input to the correct number of partitions,
			// otherwise parallelism will be based off number of HDFS blocks
			if ((parallelism != -1 ) && (nodeRDD.partitions.size != parallelism)) {
				nodeRDD = nodeRDD.coalesce(parallelism,shuffle=true)
			}
			
			// create the graph (with default vertex attribute = an empty string)
			val graph = Graph(nodeRDD, edgeRDD, "")
			
			// use a helper class to execute the louvain algorithm and save the output.
			val runner = new HDFSLouvainRunner(minProgress,progressCounter,outputDir)
			runner.run(sc, graph)
		}
		
		println("DONE!!")
	}
	
}



