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

class GraphCSVParser {
	
	//	// loads CSV graph dataset and stores rows in an RDD with "edge" or "node" string as the key, and rest of row data as the value
	//	def loadCSVgraphData(sc: SparkContext, sourceDir: String, partitions: Int, delimiter: String): RDD[(String, String)] = {
	//
	//		val rawData = if (partitions <= 0) {
	//			sc.textFile(sourceDir)
	//		} else {
	//			sc.textFile(sourceDir, partitions)
	//		}
	//
	//		rawData.flatMap(row => {
	//		      //val tokens = row.split(delimiter).map(_.trim())
	//		      val firstDelim = row.indexOf(delimiter)
	//		      var objType = ""
	//		      if (firstDelim > 0) {
	//		     	 var objType = row.substring(0, firstDelim-1)
	//		      }
	//		      if ((objType=="edge") || (objType=="node")) {
	//		     	  val rowData = row.substring(firstDelim+1)
	//
	//
	//		     	  Some((objType, rowData))
	//		      }
	//		      else {
	//		     	  None
	//		      }
	//		})
	//	}
	
	//----------------------
	// Parse edge data for a given hierarchical level
	//(assumes graph data has been louvain clustered using the spark-based graph clustering utility)
	def parseEdgeData(sc: SparkContext,
	                  rawData: RDD[String],
	                  partitions: Int,
	                  delimiter: String,
	                  edgeSrcIDindex: Int=1,
	                  edgeDstIDindex: Int=2,
	                  edgeWeightIndex: Int=3): RDD[Edge[Long]] = {

		rawData.flatMap(row =>
			{
				val tokens = row.split(delimiter).map(_.trim())
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
	
	//----------------------
	// Parse node/community data for a given hierarchical level
	//(assumes graph data has been louvain clustered using the spark-based graph clustering utility)
	//nodeIDindex = column number of nodeID
	//parentIDindex = column number of parentID
	//internalNodesX = column number of num internal node in current community
	//degreeX = column number of community degree
	//bKeepExtraAttributes = bool to look for, and store extra node attributes (at the end of each record line)
	def parseNodeData(sc: SparkContext,
	                  rawData: RDD[String],
	                  partitions: Int,
	                  delimiter: String,
	                  nodeIDindex: Int=1,
	                  parentIDindex: Int=2,
	                  internalNodesX: Int=3,
	                  degreeX: Int=4,
	                  bKeepExtraAttributes: Boolean=true): RDD[(Long, (Long, Long, Int, String))] = {
		
		val nAttrX = Math.max(Math.max(Math.max(nodeIDindex, parentIDindex), internalNodesX), degreeX)+1
		
		rawData.flatMap(row =>
			{
				val tokens = row.split(delimiter).map(_.trim())
				if (tokens(0) == "node") {
					val id = tokens(nodeIDindex).toLong
					val parentID = tokens(parentIDindex).toLong
					val internalNodes = tokens(internalNodesX).toLong
					val degree = tokens(degreeX).toInt
					val metaData = if (bKeepExtraAttributes) {
						// extract node metaData (assumed to be any extra data appended to the end of each node record)
						var str = ""
						val len = tokens.size
						for (n <- nAttrX until len) {
							// get current metadata field, and remove any existing commas
							val metadataField = tokens(n).replace(","," ")
							str += metadataField
							if (n < len-1)
								str += ","	// use commas to de-limit multiple metadata fields, if present
						}
						str
					} else {
						""
					}
					Some((id, (parentID, internalNodes, degree, metaData)))
				}
				else {
					None
				}
			}
		)
	}
	
}
