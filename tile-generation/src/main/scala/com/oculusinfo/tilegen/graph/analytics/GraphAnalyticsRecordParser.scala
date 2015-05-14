/*
 * Copyright (c) 2013 Oculus Info Inc.
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

package com.oculusinfo.tilegen.graph.analytics

import java.lang.{Integer => JavaInt}
import java.lang.{Long => JavaLong}
import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}
import com.oculusinfo.tilegen.util.KeyValueArgumentSource
import scala.collection.JavaConverters._
import scala.util.parsing.json.JSON
import com.oculusinfo.factory.util.Pair
import org.apache.spark.graphx._

/**
 * Class for parsing raw graph community and edge data for Graph Analytics tile generation.
 */
object GraphAnalyticsRecordParser {
}

class GraphAnalyticsRecordParser (hierarchyLevel: Int, properties: KeyValueArgumentSource) extends Serializable {

	val hierlevel = hierarchyLevel
	val delimiter = properties.getString("oculus.binning.parsing.separator", "", Some("\t"))
	val i_x = properties.getInt("oculus.binning.graph.x.index", "")
	val i_y = properties.getInt("oculus.binning.graph.y.index", "")
	val i_r = properties.getInt("oculus.binning.graph.r.index", "")
	val i_ID = properties.getInt("oculus.binning.graph.id.index", "")
	val i_degree = properties.getInt("oculus.binning.graph.degree.index", "")
	val i_numNodes = properties.getInt("oculus.binning.graph.numnodes.index", "")
	val i_metadata = properties.getInt("oculus.binning.graph.metadata.index", "")
	val i_pID = properties.getInt("oculus.binning.graph.parentID.index", "")
	val i_px = properties.getInt("oculus.binning.graph.parentX.index", "")
	val i_py = properties.getInt("oculus.binning.graph.parentY.index", "")
	val i_pr = properties.getInt("oculus.binning.graph.parentR.index", "")
	val maxCommunities = properties.getInt("oculus.binning.graph.maxcommunities", "", Some(25))	
	val i_stats = properties.getInt("oculus.binning.graph.stats.index", "", Some(-1))
	val maxStats = properties.getInt("oculus.binning.graph.maxstats", "", Some(32))
	
	val i_edgeSrcID = properties.getInt("oculus.binning.graph.edges.srcID.index", "", Some(-1))
	val i_edgeDstID = properties.getInt("oculus.binning.graph.edges.dstID.index", "", Some(-1))
	val i_edgeWeight = properties.getInt("oculus.binning.graph.edges.weight.index", "", Some(-1))
	val i_edgeType = properties.getInt("oculus.binning.graph.edges.type.index", "", Some(-1))
	val maxEdges = properties.getInt("oculus.binning.graph.maxedges", "", Some(10))
	
	GraphAnalyticsRecord.setMaxCommunities(maxCommunities)	//set max number of graph communities to store per tile
	GraphCommunity.setMaxStats(maxStats)	//set max size of generic stats list for a given graph community 	
	GraphCommunity.setMaxEdges(maxEdges)	//set max number of edges to store per graph community
	
	def getNodes (line: String): Option[(Long, GraphCommunity)] = {
		
		val fields = line.split(delimiter)
		if (fields(0) != "node") {	// check if 1st column in data line is tagged with "node" keyword
			None
		}
		else {
			try {
				
				var id = 0L
				var x = 0.0
				var y = 0.0
				
				id = fields(i_ID).toLong	// if any of these three attributes fails to parse then discard this data line
				x = fields(i_x).toDouble
				y = fields(i_y).toDouble
				
				val r = try fields(i_r).toDouble catch { case _: Throwable => 0.0 }
				val degree = try fields(i_degree).toInt catch { case _: Throwable => 0 }
				val numNodes = try fields(i_numNodes).toInt catch { case _: Throwable => 0L }
				val metadata = try fields(i_metadata) catch { case _: Throwable => "" }
				val pID = try fields(i_pID).toLong catch { case _: Throwable => -1L }
				val px = try fields(i_px).toDouble catch { case _: Throwable => -1.0 }
				val py = try fields(i_py).toDouble catch { case _: Throwable => -1.0 }
				val pr = try fields(i_pr).toDouble catch { case _: Throwable => 0.0 }
				
				// parse stats list
				val statsList = try {
					var values = new java.util.ArrayList[JavaDouble]()
					if (i_stats < 0) { 
					  values 
					}
					else {
						val csvStatsList = fields(i_stats)
						val statsFields = csvStatsList.split(",")	// assumes that raw stats list are comma-delimited values				
						val end = Math.min(statsFields.size, maxStats)
						var i = 0
						for (i <- 0 until end) {
							values.add(statsFields(i).trim.toDouble)
						}					
						values
					}
				}
				catch {
				   case _: Throwable => new java.util.ArrayList[JavaDouble]()
				}
				
				val community = new GraphCommunity(hierlevel,
				                                   id,
				                                   new Pair[JavaDouble, JavaDouble](x, y),
				                                   r,
				                                   degree,
				                                   numNodes,
				                                   metadata,
				                                   (id==pID),
				                                   pID,
				                                   new Pair[JavaDouble, JavaDouble](px, py),
				                                   pr,
				                                   statsList,
				                                   null,
				                                   null);
				
				Some((id, community))
			}
			catch {
				case _: Throwable => None
			}
		}
	}
	
	
	def getEdges (line: String): Option[(Edge[(Long, Boolean)])] = {
		
		val fields = line.split(delimiter)
		
		// check if 1st column in data line is tagged with "edge" keyword
		if ((fields(0) != "edge") || (i_edgeSrcID == -1) || (i_edgeDstID == -1))  {
			None
		}
		else {
			try {
				// if any of these attributes fails to parse then discard this data line
				val srcID = fields(i_edgeSrcID).toLong
				val dstID = fields(i_edgeDstID).toLong
				val weight = if (i_edgeWeight == -1) 1L else fields(i_edgeWeight).toLong
				val isInterEdge = if (i_edgeType == -1) true else fields(i_edgeType).toInt==1
				
				
				Some(new Edge(srcID, dstID, (weight, isInterEdge)))	//create a new edge object
			}
			catch {
				case _: Throwable => None
			}
		}
	}
}
