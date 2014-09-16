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

//import java.io.Serializable;
import com.oculusinfo.binning.util.Pair;

// A class for stored a graph community object.  Used for Graph Analytics.

public class GraphCommunity { //implements Serializable {
	//private static final long serialVersionUID = 1L;	//NOTE:  using default serialVersion ID

	private int _hierLevel;					// hierarchy level of community	
	private long _id;						// ID of community
	private Pair<Double, Double> _coords;	// x,y coords of centre of community
	private double _radius;					// radius of community

	private int _degree;						// degree of community
	private long _numNodes;					// number of raw nodes in this community
	private String _metadata;				// community metdata
	private boolean _bIsPrimaryNode;		// bool whether or not this community is a 'primary node' within its parent community			

	private long _parentID;					// ID of parent community
	private Pair<Double, Double> _parentCoords;	// x,y coords of centre of parent community
	private double _parentRadius;			// radius of parent community
	
	//---- Constructor
	GraphCommunity(int hierLevel,
					long id,
					Pair<Double, Double> coords,
					double radius,
					int degree,
					long numNodes,
					String metadata,
					boolean bIsPrimaryNode,
					long parentID,
					Pair<Double, Double> parentCoords,
					double parentRadius) {
		
		_hierLevel = hierLevel;
		_id = id;
		_coords = coords;
		_radius = radius;
		_degree = degree;
		_numNodes = numNodes;
		_metadata = metadata;
		_bIsPrimaryNode = bIsPrimaryNode;
		_parentID = parentID;
		_parentCoords = parentCoords;
		_parentRadius = parentRadius;
	}
	
	public int getHierLevel() {
		return _hierLevel;
	}
	
	public long getID() {
		return _id;
	}
		
	public Pair<Double, Double> getCoords() {
		return _coords;
	}		
	
	public double getRadius() {
		return _radius;
	}
	
	public int getDegree() {
		return _degree;
	}
	
	public long getNumNodes() {
		return _numNodes;
	}
	
	public String getMetadata() {
		return _metadata;
	}
	
	public boolean isPrimaryNode() {
		return _bIsPrimaryNode;
	}
		
	public long getParentID() {
		return _parentID;
	}
		
	public Pair<Double, Double> getParentCoords() {
		return _parentCoords;
	}		
	
	public double getParentRadius() {
		return _parentRadius;
	}
	
	//---- min attribute values between two communities
	public void minInPlace(GraphCommunity b) {
		
		double x = Math.min(this.getCoords().getFirst(), b.getCoords().getFirst());
		double y = Math.min(this.getCoords().getSecond(), b.getCoords().getSecond());
		double px = Math.min(this.getParentCoords().getFirst(), b.getParentCoords().getFirst());
		double py = Math.min(this.getParentCoords().getSecond(), b.getParentCoords().getSecond());
		
		_hierLevel = Math.min(this.getHierLevel(), b.getHierLevel());
		_id = Math.min(this.getID(), b.getID());
		_coords = new Pair<Double, Double>(x,y);
		_radius = Math.min(this.getRadius(), b.getRadius());
		_degree = Math.min(this.getDegree(), b.getDegree());
		_numNodes = Math.min(this.getNumNodes(), b.getNumNodes());
		_metadata = "";
		_bIsPrimaryNode	= false;	
		_parentID = Math.min(this.getParentID(), b.getParentID());
		_parentCoords = new Pair<Double, Double>(px,py);
		_parentRadius = Math.min(this.getParentRadius(), b.getParentRadius());
	}

	//---- max attribute values between two communities
	public void maxInPlace(GraphCommunity b) {
		
		double x = Math.max(this.getCoords().getFirst(), b.getCoords().getFirst());
		double y = Math.max(this.getCoords().getSecond(), b.getCoords().getSecond());
		double px = Math.max(this.getParentCoords().getFirst(), b.getParentCoords().getFirst());
		double py = Math.max(this.getParentCoords().getSecond(), b.getParentCoords().getSecond());
		
		_hierLevel = Math.max(this.getHierLevel(), b.getHierLevel());
		_id = Math.max(this.getID(), b.getID());
		_coords = new Pair<Double, Double>(x,y);
		_radius = Math.max(this.getRadius(), b.getRadius());
		_degree = Math.max(this.getDegree(), b.getDegree());
		_numNodes = Math.max(this.getNumNodes(), b.getNumNodes());
		_metadata = "";
		_bIsPrimaryNode	= false;	
		_parentID = Math.max(this.getParentID(), b.getParentID());
		_parentCoords = new Pair<Double, Double>(px,py);
		_parentRadius = Math.max(this.getParentRadius(), b.getParentRadius());
	}	
		
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (null == obj)
			return false;
		if (!(obj instanceof GraphCommunity))
			return false;

		GraphCommunity that = (GraphCommunity) obj;
		
		if (this.getHierLevel() != that.getHierLevel()) {
			return false;
		}
		else if ((this.getID() != that.getID())) {
			return false;
		}
		else if (!(this.getCoords().equals(that.getCoords()))) {
			return false;
		}
		else if ((this.getRadius() != that.getRadius())) {
			return false;
		}
		else if ((this.getDegree() != that.getDegree())) {
			return false;
		}
		else if ((this.getNumNodes() != that.getNumNodes())) {
			return false;
		}
		else if (!(this.getMetadata().equals(that.getMetadata()))) {
			return false;
		}
		else if ((this.isPrimaryNode() != that.isPrimaryNode())) {
			return false;
		}
		else if ((this.getParentID() != that.getParentID())) {
			return false;
		}	
		else if (!(this.getParentCoords().equals(that.getParentCoords()))) {
			return false;
		}
		else if ((this.getParentRadius() != that.getParentRadius())) {
			return false;
		}		
		else {
			return true;
		}
	}
}
