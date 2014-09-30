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
//import java.util.ArrayList
import java.util.{List => JavaList}
import scala.collection.JavaConverters._
import scala.util.parsing.json.JSON
import com.oculusinfo.binning.util.Pair
import com.oculusinfo.tilegen.datasets.CSVRecordPropertiesWrapper
//import com.oculusinfo.tilegen.graph.analytics.GraphCommunity
//import com.oculusinfo.tilegen.graph.analytics.GraphAnalyticsRecord
//import java.util.Arrays

object GraphAnalyticsRecordParser {
}

class GraphAnalyticsRecordParser (hierarchyLevel: Int, properties: CSVRecordPropertiesWrapper) {

	val hierlevel = hierarchyLevel
	val delimiter = properties.getString("oculus.binning.parsing.separator", "", Some("\t"))
	val xField = properties.getString("oculus.binning.parsing.xField", "", Some("x"))
	val yField = properties.getString("oculus.binning.parsing.yField", "", Some("y"))
	val i_x = properties.getInt("oculus.binning.parsing." + xField + ".index", "")
	val i_y = properties.getInt("oculus.binning.parsing." + yField + ".index", "")
	val i_r = properties.getInt("oculus.binning.graph.r.index", "")	
	val i_ID = properties.getInt("oculus.binning.graph.id.index", "")
	val i_degree = properties.getInt("oculus.binning.graph.degree.index", "")
	val i_numNodes = properties.getInt("oculus.binning.graph.numnodes.index", "")
	val i_metadata = properties.getInt("oculus.binning.graph.metadata.index", "")
	val i_pID = properties.getInt("oculus.binning.graph.parentID.index", "")	
	val i_px = properties.getInt("oculus.binning.graph.parentX.index", "")
	val i_py = properties.getInt("oculus.binning.graph.parentY.index", "")
	val i_pr = properties.getInt("oculus.binning.graph.parentR.index", "")	
	
  def getGraphRecords (line: String): Option[((Double, Double), GraphAnalyticsRecord)] = {
    
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
												pr);
		
			val record = new GraphAnalyticsRecord(1,List(community).asJava);
	
			Some(((x, y), record))
		}
		catch {
			case _: Throwable => None
		}
	}	
}
   	  	
}
