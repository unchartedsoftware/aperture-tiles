/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oculusinfo.tilegen.graph.analytics;

import java.io.Serializable;
import com.oculusinfo.factory.util.Pair;

/**
 * Class for a storing a graph edge object.  Used by GraphCommunity for graph 
 * analytics tile generation.  Contains info about an edge's endpoints and weight.
 * 
 * @param dstID		destination node ID for this edge
 * 
 * @param dstX		x coord of destination node
 * 
 * @param dstY		y coord of destination node
 * 
 * @param weight	edge weight
 * 
 */
public class GraphEdge implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7200813921683778245L;

	// Note: currently this object does NOT need to store its source node ID, 
	// since it is only being used by the source node/community (ie in GraphCommunity object)
	
	//private long _srcID;						// source node ID of edge
	private long _dstID;						// destination node ID of edge	
	private Pair<Double, Double> _dstcoords;	// x,y coords of destination node
	private long _weight;						// edge weight
	
	GraphEdge(long dstID,
				double dstX,
				double dstY,
				long weight) {
		//_srcID = srcID;
		_dstID = dstID;
		_dstcoords = new Pair<Double, Double>(dstX, dstY);
		_weight = weight;
	}
	
//	public long getSrcID() {
//		return _srcID;
//	}
	
	public long getDstID() {
		return _dstID;
	}	
		
	public Pair<Double, Double> getDstCoords() {
		return _dstcoords;
	}		
	
	public long getWeight() {
		return _weight;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (null == obj)
			return false;
		if (!(obj instanceof GraphEdge))
			return false;

		GraphEdge that = (GraphEdge) obj;
		
		if (this.getDstID() != that.getDstID()) {
			return false;
		}
		else if (!(this.getDstCoords().equals(that.getDstCoords()))) {
			return false;
		}
		else if ((this.getWeight() != that.getWeight())) {
			return false;
		}
		else {
			return true;
		}
	}	
	
}
