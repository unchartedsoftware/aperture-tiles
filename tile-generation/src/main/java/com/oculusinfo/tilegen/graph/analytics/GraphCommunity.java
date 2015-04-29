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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.oculusinfo.factory.util.Pair;


/**
 * Class for a storing a graph community object.  Used by GraphAnalyticsRecord for graph 
 * analytics tile generation.  Contains info about a graph community's size, location, ID,
 * generic metadata, its parent graph community (if applicable), as well as the MAX_EDGES highest
 * weighted inter-community and intra-community edges connected to the graph community.
 * 
 * Edge info is stored in lists of GraphEdge objects, ranked by weight (highest to lowest).
 * 
 * @param hierLevel
 *         	Hierarchy level of community
 * 
 * @param id
 *      	ID of community
 *            
 * @param coords
 *  		x,y coords of centre of community
 *  
 * @param radius
 *  		radius of community
 *  
 * @param degree
 *  		degree of community
 *  
 * @param numNodes
 * 			number of internal raw nodes in this community
 * 
 * @param metadata
 * 			community metdata
 * 
 * @param bIsPrimaryNode
 * 			bool whether or not this community is the 'primary node' within its parent community
 * 			(if so, the parent community is labelled with the ID and metadata of this primary community)
 * 
 * @param parentID
 * 			ID of parent community
 * 
 * @param parentCoords
 * 			x,y coords of centre of parent community
 * 
 * @param parentRadius
 * 			radius of parent community
 * 
 * @param communityStats
 * 			generic list of up to MAX_STATS entries to store/aggregate numeric stats about a graph community
 * 
 * @param interEdges
 * 			list of up to MAX_EDGES highest weighted inter-community edges connected to this node (highest to lowest)
 * 
 * @param intraEdges
 * 			list of up to MAX_EDGES highest weighted intra-community edges connected to this node (highest to lowest)
 */
public class GraphCommunity implements Serializable {	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2127685671353087035L;
	
	private static int MAX_STATS = 32;		// max allowable size of _communityStats list

	private static int MAX_EDGES = 10;		// max number of both inter and intra community 
											//	edges to keep per record.	

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

	private List<Double> _communityStats;	// generic list of numbers to store/aggregate numeric stats about a graph community
	
	private List<GraphEdge> _interEdges;	// inter-community edges connected to this node 
	private List<GraphEdge> _intraEdges;	// intra-community edges connected to this node
	
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
					double parentRadius,
					List<Double> communityStats,
					List<GraphEdge> interEdges,
					List<GraphEdge> intraEdges) {
		
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
		
		
		if (communityStats == null) {
			_communityStats = new ArrayList<Double>();
		}
		else {
			if (communityStats.size() > MAX_STATS) {
				_communityStats = new ArrayList<Double>();
				for (int i=0; i<MAX_STATS; i++) {
					_communityStats.add(communityStats.get(i));	// save MAX_STATS entries of communityStats
				}
				//throw new IllegalArgumentException("Number of communityStats in list must be <= " + MAX_STATS);
			} else {
				_communityStats = communityStats;
			}
		}		
				
		if (interEdges == null) {
			_interEdges = new ArrayList<GraphEdge>();
		}
		else {
			if (interEdges.size() > MAX_EDGES) {
				throw new IllegalArgumentException("Number of inter-community edges in list must be <= " + MAX_EDGES);
			}
			_interEdges = interEdges;
		}
		
		if (intraEdges == null) {
			_intraEdges = new ArrayList<GraphEdge>();
		}
		else {
			if (intraEdges.size() > MAX_EDGES) {
				throw new IllegalArgumentException("Number of intra-community edges in list must be <= " + MAX_EDGES);
			}
			_intraEdges = intraEdges;
		}
	}
	
	public static void setMaxStats(int max) {		//TODO -- This is not the best way to set this static objects for a serializable java class.
		MAX_STATS = max;							// Perhaps set them from within GraphAnalyticsRecordParser object and access them from within the this class constructor instead
	}		
	
	public static void setMaxEdges(int max) {
		MAX_EDGES = max;
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
	
	
	public List<Double> getStatsList() {
		return _communityStats;
	}	
	
	public List<GraphEdge> getInterEdges() {
		return _interEdges;
	}
	
	public List<GraphEdge> getIntraEdges() {
		return _intraEdges;
	}
	
	
	private static void addEdgeInPlace(LinkedList<GraphEdge> accumulatedEdges, GraphEdge newEdge) {
		ListIterator<GraphEdge> i = accumulatedEdges.listIterator();
		int size = 0;
		while (true) {
			if (i.hasNext()) {
				GraphEdge next = i.next();
				size++;

				// Rank edges based on weight
				if (next.getWeight() < newEdge.getWeight()) {
					
					// Insert the new edge if has higher weight
					i.previous();
					i.add(newEdge);
					size++;
					i.next();
					
					if (!i.hasNext() && size == MAX_EDGES+1) {	// already at end of list but is 1 element too big
						i.remove();
						--size;
					}
					else {
						// ... and trim the list to MAX_EDGES elements
						while (i.hasNext() && size < MAX_EDGES) {
							i.next();
							++size;
						}
						while (i.hasNext()) {
							i.next();
							i.remove();
						}
					}

					return;
				}
			} else {
				if (size < MAX_EDGES) {
					i.add(newEdge);
				}
				return;
			}
		}
	}

//	private static void addEdgesInPlace(LinkedList<GraphEdge> accumulatedEdges, List<GraphEdge> newEdges) {
//		for (GraphEdge newEdge : newEdges) {
//			addEdgeInPlace(accumulatedEdges, newEdge);
//		}
//	}

	/**
	 * Add an inter-community edge to an existing community, summing counts as needed, and keeping
	 * the MAX_EDGES largest edges.
	 */
	public void addInterEdgeToCommunity(GraphEdge newEdge) {
		
		LinkedList<GraphEdge> accumulatedEdges = new LinkedList<>(_interEdges);
		addEdgeInPlace(accumulatedEdges, newEdge);
		_interEdges = accumulatedEdges;	
	}
	
	/**
	 * Add an intra-community edge to an existing community, summing counts as needed, and keeping
	 * the MAX_EDGES largest edges.
	 */
	public void addIntraEdgeToCommunity(GraphEdge newEdge) {
		
		LinkedList<GraphEdge> accumulatedEdges = new LinkedList<>(_intraEdges);
		addEdgeInPlace(accumulatedEdges, newEdge);
		_intraEdges = accumulatedEdges;	
	}

	private List<GraphEdge> minOfEdgeLists(GraphEdge accumulatedMin, List<GraphEdge> newMin) {
		long dstID = accumulatedMin.getDstID();
		double x = accumulatedMin.getDstCoords().getFirst();
		double y = accumulatedMin.getDstCoords().getSecond();
		long weight = accumulatedMin.getWeight();
		for (int i = 0; i < newMin.size(); ++i) {
			dstID = Math.min(dstID, newMin.get(i).getDstID());
			x = Math.min(x, newMin.get(i).getDstCoords().getFirst());
			y = Math.min(y, newMin.get(i).getDstCoords().getSecond());
			weight = Math.min(weight, newMin.get(i).getWeight());
		}
		return Arrays.asList(new GraphEdge(dstID, x, y, weight));
	}
	
	private List<GraphEdge> maxOfEdgeLists(GraphEdge accumulatedMax, List<GraphEdge> newMax) {
		long dstID = accumulatedMax.getDstID();
		double x = accumulatedMax.getDstCoords().getFirst();
		double y = accumulatedMax.getDstCoords().getSecond();
		long weight = accumulatedMax.getWeight();
		for (int i = 0; i < newMax.size(); ++i) {
			dstID = Math.max(dstID, newMax.get(i).getDstID());
			x = Math.max(x, newMax.get(i).getDstCoords().getFirst());
			y = Math.max(y, newMax.get(i).getDstCoords().getSecond());
			weight = Math.max(weight, newMax.get(i).getWeight());
		}
		return Arrays.asList(new GraphEdge(dstID, x, y, weight));
	}
	
	private List<Double> minOfStatsList(double accumulatedMin, List<Double> statsList) {
		double minVal = accumulatedMin;
		
		for (int i = 0; i < statsList.size(); i++) {
			minVal = Math.min(minVal, statsList.get(i));
		}
		return Arrays.asList(minVal);
	}
	
	private List<Double> maxOfStatsList(double accumulatedMax, List<Double> statsList) {
		double maxVal = accumulatedMax;
		
		for (int i = 0; i < statsList.size(); i++) {
			maxVal = Math.max(maxVal, statsList.get(i));
		}
		return Arrays.asList(maxVal);
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
		_communityStats = minOfStatsList(this.getStatsList().get(0), b.getStatsList());		
		_interEdges = minOfEdgeLists(this.getInterEdges().get(0), b.getInterEdges());
		_intraEdges = minOfEdgeLists(this.getIntraEdges().get(0), b.getIntraEdges());
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
		_communityStats = maxOfStatsList(this.getStatsList().get(0), b.getStatsList());
		_interEdges = maxOfEdgeLists(this.getInterEdges().get(0), b.getInterEdges());
		_intraEdges = maxOfEdgeLists(this.getIntraEdges().get(0), b.getIntraEdges());		
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
		else if (!listsEqual(this.getStatsList(), that.getStatsList())) {
			return false;
		}		
		else if (!listsEqual(this.getInterEdges(), that.getInterEdges())) {
			return false;
		}
		else if (!listsEqual(this.getIntraEdges(), that.getIntraEdges())) {
			return false;
		}
		else {
			return true;
		}
	}
	
	private static boolean objectsEqual(Object a, Object b) {
		if (null == a)
			return null == b;
		return a.equals(b);
	}
	
	private static <T> boolean listsEqual(List<T> a, List<T> b) {
		if (null == a)
			return null == b;
		if (null == b)
			return false;
		if (a.size() != b.size())
			return false;
		for (int i = 0; i < a.size(); ++i) {
			if (!objectsEqual(a.get(i), b.get(i)))
				return false;
		}
		return true;
	}
}
