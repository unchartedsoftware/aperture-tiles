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
//import scala.Int;



import com.oculusinfo.factory.util.Pair;

/**
 * Class for a record for graph analytics. Used for custom tile generation for
 * graph analytics. For example, each graph analytics tile contains info about
 * one GraphAnalyticsRecord. In this case, a GraphAnalticsRecord aggregates
 * information for up to MAX_COMMUNITIES of the largest graph communities in a
 * given tile, whereas _numCommunities is the total number of communities in a
 * given tile.
 * 
 * This class is used by GraphAnalyticsRecordParser to parse raw graph community
 * data. It keeps track of detailed information for the most important graph
 * communities in a tile.
 * 
 * Description of member variables:
 * 	MAX_COMMUNITIES
 *            The max number of communities to keep analytics info of per record
 * 
 * 	_numCommunities
 *            Total number of communities in a given tile
 * 
 * 	_communities
 *            List of GraphCommunity objects for storing analytics info for a
 *            given graph community
 */
public class GraphAnalyticsRecord implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7929675167195806162L;

	private static int MAX_COMMUNITIES = 25;	// Max number of communities to keep analytics of 
												//    per record.
	private int _numCommunities;				// Total number of communities in a given tile
	private List<GraphCommunity> _communities;
	
	//---- Constructor
	public GraphAnalyticsRecord(int numCommunities, List<GraphCommunity> communities) {	
		
		_numCommunities = numCommunities;
		
		if (communities == null) {
			_communities = new ArrayList<GraphCommunity>();
		}
		else {
			if (communities.size() > MAX_COMMUNITIES) {
				throw new IllegalArgumentException("Number of communities in list must be <= " + MAX_COMMUNITIES);
			}
			else if (numCommunities < 0) {
				throw new IllegalArgumentException("numCommunities must be >= 0");
			}
			_communities = communities;
		}
	}
	
	public static void setMaxCommunities(int max) {
		MAX_COMMUNITIES = max;
	}	
	
	public int getNumCommunities() {
		return _numCommunities;
	}	
	
	public List<GraphCommunity> getCommunities() {
		return _communities;
	}
	
	private int getHash(Object obj) {	//TODO do we need these?
		if (null == obj)
			return 0;
		return obj.hashCode();
	}

	@Override
	public int hashCode() {
		return (getHash(_communities));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (null == obj)
			return false;
		if (!(obj instanceof GraphAnalyticsRecord))
			return false;

		GraphAnalyticsRecord that = (GraphAnalyticsRecord) obj;
		
		if (this.getNumCommunities() != that.getNumCommunities()) {
			return false;
		}
		else if ((this.getCommunities()!= null) && (!listsEqual(this.getCommunities(), that.getCommunities()))) {
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

	private static String escapeString(String string) {
		if (null == string)
			return "null";
		else
			return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"")
					+ "\"";
	}

	private static String unescapeString(String string) {
		if (null == string)
			return null;
		if ("null".equals(string))
			return null;

		// Remove start and end quote
		string = string.substring(1, string.length() - 1);
		// Replace escaped characters
		return string.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	private static String eat(String from, String prefix) {
		if (from.startsWith(prefix)) {
			return from.substring(prefix.length());
		}
		throw new IllegalArgumentException("String " + from
				+ " didn't begin with expected prefix " + prefix);
	}

	private static int getQuotedStringEnd(String from) {
		if (from.startsWith("null"))
			return 4;
		if (!from.startsWith("\""))
			throw new IllegalArgumentException(
					"Quoted string didn't start with quote");
		int lastQuote = 0;
		while (true) {
			lastQuote = from.indexOf("\"", lastQuote + 1);
			if (lastQuote < 0)
				throw new IllegalArgumentException(
						"Couldn't find the end of quoted string");
			int slashes = 0;
			for (int i = lastQuote - 1; i >= 0; --i) {
				if ('\\' == from.charAt(i)) {
					++slashes;
				} else {
					break;
				}
			}
			if (0 == (slashes % 2)) {
				// final quote - we're done
				return lastQuote + 1;
			}
		}
	}

	@Override
	public String toString() {
		String result = ("{\"numCommunities\": " + _numCommunities + ", "
				+ "\"communities\": [");
		for (int i = 0; i < _communities.size(); ++i) {
			GraphCommunity node = _communities.get(i);
			// List<GraphEdge> interEdges = node.getInterEdges();
			// List<GraphEdge> intraEdges = node.getIntraEdges();

			if (i > 0)
				result += ", ";
			result += "{\"hierLevel\": " + node.getHierLevel() + ", "
						 + "\"id\": " + node.getID() + ", "
						 + "\"coords\": [" + node.getCoords().getFirst() + ", " + node.getCoords().getSecond() + "], "
						 + "\"radius\": " + node.getRadius() + ", "
						 + "\"degree\": " + node.getDegree() + ", "
						 + "\"numNodes\": " + node.getNumNodes() + ", "
						 + "\"metadata\": " + escapeString(node.getMetadata()) + ", "
						 + "\"isPrimaryNode\": " + node.isPrimaryNode() + ", "
						 + "\"parentID\": " + node.getParentID() + ", "
						 + "\"parentCoords\": [" + node.getParentCoords().getFirst() + ", " + node.getParentCoords().getSecond() + "], "
						 + "\"parentRadius\": " + node.getParentRadius() + ", ";
			
			result += "\"statsList\": [";
			if (node.getStatsList() != null) {
				for (int n = 0; n < node.getStatsList().size(); n++) {
					if (n > 0)
						result += ", ";
					result += node.getStatsList().get(n);
				}				
			}
			
			result += "], \"interEdges\": [";
			if (node.getInterEdges() != null) {
				for (int n = 0; n < node.getInterEdges().size(); n++) {
					GraphEdge edge = node.getInterEdges().get(n);
					if (n > 0)
						result += ", ";
					result += "{\"dstID\": " + edge.getDstID() + ", "
							 + "\"dstCoords\": [" + edge.getDstCoords().getFirst() + ", " + edge.getDstCoords().getSecond() + "], "
							 + "\"weight\": " + edge.getWeight() + "}";
				}
			}
			result += "], \"intraEdges\": [";
			
			if (node.getIntraEdges() != null) {
				
				for (int n = 0; n < node.getIntraEdges().size(); n++) {
					GraphEdge edge = node.getIntraEdges().get(n);
					if (n > 0)
						result += ", ";
					result += "{\"dstID\": " + edge.getDstID() + ", "
							 + "\"dstCoords\": [" + edge.getDstCoords().getFirst() + ", " + edge.getDstCoords().getSecond() + "], "
							 + "\"weight\": " + edge.getWeight() + "}";
				}
			}
			result += "]}";
		}
		result += "]}";
		return result;
	}

	public static GraphAnalyticsRecord fromString(String value) {
		value = eat(value, "{\"numCommunities\": ");
		int end = value.indexOf(",");
		int numCommunities = Integer.parseInt(value.substring(0, end));

		value = eat(value.substring(end), ", \"communities\": [");
		List<GraphCommunity> communities = new ArrayList<>();
		
		while (value.startsWith("{")) {
			value = eat(value, "{\"hierLevel\": ");
			end = value.indexOf(", ");
			int hierLevel = Integer.parseInt(value.substring(0, end));
			
			value = eat(value.substring(end), ", \"id\": ");
			end = value.indexOf(", ");
			long id = Long.parseLong(value.substring(0, end));
			
			value = eat(value.substring(end), ", \"coords\": [");
			end = value.indexOf(",");
			double x = Double.parseDouble(value.substring(0, end));
			value = eat(value.substring(end), ", ");
			end = value.indexOf("], ");
			double y = Double.parseDouble(value.substring(0, end));
			
			value = eat(value.substring(end), "], \"radius\": ");
			end = value.indexOf(", ");
			double radius = Double.parseDouble(value.substring(0, end));			
			
			value = eat(value.substring(end), ", \"degree\": ");
			end = value.indexOf(", ");
			int degree = Integer.parseInt(value.substring(0, end));
			
			value = eat(value.substring(end), ", \"numNodes\": ");
			end = value.indexOf(", ");
			long numNodes = Long.parseLong(value.substring(0, end));				

			value = eat(value.substring(end), ", \"metadata\": ");
			end = getQuotedStringEnd(value);
			String metadata = unescapeString(value.substring(0, end));
			
			value = eat(value.substring(end), ", \"isPrimaryNode\": ");
			end = value.indexOf(", ");
			boolean bIsPrimaryNode = Boolean.parseBoolean(value.substring(0, end));
			
			value = eat(value.substring(end), ", \"parentID\": ");
			end = value.indexOf(", ");
			long parentID = Long.parseLong(value.substring(0, end));
			
			value = eat(value.substring(end), ", \"parentCoords\": [");
			end = value.indexOf(",");
			double parentX = Double.parseDouble(value.substring(0, end));
			value = eat(value.substring(end), ", ");
			end = value.indexOf("], ");
			double parentY = Double.parseDouble(value.substring(0, end));			
			
			value = eat(value.substring(end), "], \"parentRadius\": ");
			end = value.indexOf(", ");
			double parentRadius = Double.parseDouble(value.substring(0, end));
			
			value = eat(value.substring(end), ", \"statsList\": [");
			List<Double> statsList = new ArrayList<>();
			
			while (!value.startsWith("]")) {
				end = value.indexOf(", ");
				double currentValue = Double.parseDouble(value.substring(0, end));	
				
				statsList.add(currentValue);	// add currentValue to the list
				
				value = value.substring(end + 1);
				if (value.startsWith(", "))
					value = eat(value, ", ");			
			}			
			
			value = eat(value.substring(end), "], \"interEdges\": [");
			List<GraphEdge> interEdges = new ArrayList<>();
			
			while (value.startsWith("{")) {
				value = eat(value, "{\"dstID\": ");
				end = value.indexOf(", ");
				long dstID = Long.parseLong(value.substring(0, end));
				
				value = eat(value.substring(end), ", \"dstCoords\": [");
				end = value.indexOf(",");
				double dstX = Double.parseDouble(value.substring(0, end));
				value = eat(value.substring(end), ", ");
				end = value.indexOf("], ");
				double dstY = Double.parseDouble(value.substring(0, end));
							
				value = eat(value.substring(end), "], \"weight\": ");
				end = value.indexOf("}");
				long weight = Long.parseLong(value.substring(0, end));			
				
				GraphEdge currentEdge = new GraphEdge(dstID, dstX, dstY, weight);
				interEdges.add(currentEdge);	// add currentEdge to the list
				
				value = value.substring(end + 1);
				if (value.startsWith(", "))
					value = eat(value, ", ");
			}
			
			value = eat(value.substring(end-1), "], \"intraEdges\": [");
			List<GraphEdge> intraEdges = new ArrayList<>();
			
			while (value.startsWith("{")) {
				value = eat(value, "{\"dstID\": ");
				end = value.indexOf(", ");
				long dstID = Long.parseLong(value.substring(0, end));
				
				value = eat(value.substring(end), ", \"dstCoords\": [");
				end = value.indexOf(",");
				double dstX = Double.parseDouble(value.substring(0, end));
				value = eat(value.substring(end), ", ");
				end = value.indexOf("], ");
				double dstY = Double.parseDouble(value.substring(0, end));
							
				value = eat(value.substring(end), "], \"weight\": ");
				end = value.indexOf("}");
				long weight = Long.parseLong(value.substring(0, end));			
				
				GraphEdge currentEdge = new GraphEdge(dstID, dstX, dstY, weight);
				intraEdges.add(currentEdge);	// add currentEdge to the list
				
				value = value.substring(end + 1);
				if (value.startsWith(", "))
					value = eat(value, ", ");
			}			

			GraphCommunity currentCommunity = new GraphCommunity(
						hierLevel,
						id,
						new Pair<Double, Double>(x, y),
						radius,
						degree,
						numNodes,
						metadata,
						bIsPrimaryNode,
						parentID,
						new Pair<Double, Double>(parentX, parentY),
						parentRadius,
						statsList,
						interEdges,
						intraEdges
					);
			
			communities.add(currentCommunity);	// add currentCommunity to the list

			value = value.substring(end + 1);
			if (value.startsWith(", "))
				value = eat(value, ", ");
		}

		return new GraphAnalyticsRecord(numCommunities, communities);
	}	
	
	private static void addCommunityInPlace(
			LinkedList<GraphCommunity> accumulatedCommunities,
			GraphCommunity newCommunity) {
		ListIterator<GraphCommunity> i = accumulatedCommunities.listIterator();
		int size = 0;
		while (true) {
			if (i.hasNext()) {
				GraphCommunity next = i.next();
				size++;
				int hierLevel = next.getHierLevel();
				if (hierLevel != newCommunity.getHierLevel()) {
					throw new IllegalArgumentException("Cannot aggegrate communities from different hierarchy levels.");
				}

				// Rank communities based on degree for lowest hierarchy level, OR
				// rank by number of internal nodes for hierarchy levels > 0
				if ((hierLevel==0 && next.getDegree() < newCommunity.getDegree()) || 		//TODO -- ideally, could use 'weighted degree' here 
				   (hierLevel>0 && next.getNumNodes() < newCommunity.getNumNodes())) {
					
					// Insert the new community if it is larger (ie contains more raw nodes)
					i.previous();
					i.add(newCommunity);
					size++;
					i.next();
					
					if (!i.hasNext() && size == MAX_COMMUNITIES+1) {	// already at end of list but is 1 element too big
						i.remove();
						--size;
					}
					else {
						// ... and trim the list to MAX_COMMUNITIES elements
						while (i.hasNext() && size < MAX_COMMUNITIES) {
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
				if (size < MAX_COMMUNITIES) {
					i.add(newCommunity);
				}
				return;
			}
		}
	}

	private static void addCommunitiesInPlace(
			LinkedList<GraphCommunity> accumulatedCommunities,
			List<GraphCommunity> newCommunities) {
		for (GraphCommunity newCommunity : newCommunities) {
			addCommunityInPlace(accumulatedCommunities, newCommunity);
		}
	}

	/**
	 * Combine two records.
	 */
	public static GraphAnalyticsRecord addRecords(
			GraphAnalyticsRecord... records) {
		if (null == records || 0 == records.length)
			return null;

		int numCommunities = records[0].getNumCommunities();
		LinkedList<GraphCommunity> communities = new LinkedList<>(records[0].getCommunities());

		for (int i = 1; i < records.length; ++i) {
			numCommunities += records[i].getNumCommunities();
			addCommunitiesInPlace(communities, records[i].getCommunities());
		}
		return new GraphAnalyticsRecord(numCommunities, communities);
	}

	/**
	 * Add a graph community to an existing record, summing counts as needed, and keeping
	 * the MAX_COMMUNITIES largest communities.
	 */
	public static GraphAnalyticsRecord addCommunityToRecord(
			GraphAnalyticsRecord record, GraphCommunity newCommunity) {
		if (null == record)
			return null;
		
		int numCommunities = record.getNumCommunities() + 1;
		LinkedList<GraphCommunity> accumulatedCommunities = new LinkedList<>(record.getCommunities());
		addCommunityInPlace(accumulatedCommunities, newCommunity);

		return new GraphAnalyticsRecord(numCommunities, accumulatedCommunities);		
	}

	private static void minInPlace(GraphCommunity accumulatedMin,
			List<GraphCommunity> newMin) {
		for (int i = 0; i < newMin.size(); ++i) {
			accumulatedMin.minInPlace(newMin.get(i));
		}
	}

	/**
	 * Get minimums of all counts across some number of records.
	 * 
	 * @param records A variable number of records
	 * @return The minimums of all counts
	 */
	public static GraphAnalyticsRecord minOfRecords(
			GraphAnalyticsRecord... records) {
		if (null == records || 0 == records.length)
			return null;

		int minNumCommunities = Integer.MAX_VALUE;
		List<GraphEdge> minEdgeList = Arrays.asList(new GraphEdge(Long.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Long.MAX_VALUE));
		GraphCommunity minCommunity = new GraphCommunity(Integer.MAX_VALUE,
														Long.MAX_VALUE,
														new Pair<Double, Double>(Double.MAX_VALUE, Double.MAX_VALUE),
														Double.MAX_VALUE,
														Integer.MAX_VALUE,
														Long.MAX_VALUE,
														"",
														false,
														Long.MAX_VALUE,
														new Pair<Double, Double>(Double.MAX_VALUE, Double.MAX_VALUE),
														Double.MAX_VALUE,
														Arrays.asList(Double.MAX_VALUE),
														minEdgeList,
														minEdgeList);

		for (GraphAnalyticsRecord record : records) {
			if (null != record) {
				minNumCommunities = Math.min(minNumCommunities, record.getNumCommunities());
				minInPlace(minCommunity, record.getCommunities());
			}
		}
		return new GraphAnalyticsRecord(minNumCommunities, Arrays.asList(minCommunity));
	}
		
	private static void maxInPlace(GraphCommunity accumulatedMin,
			List<GraphCommunity> newMin) {
		for (int i = 0; i < newMin.size(); ++i) {
			accumulatedMin.maxInPlace(newMin.get(i));
		}
	}	

	/**
	 * Get maximums of all counts across some number of records.
	 * 
	 * @param records A variable number of records
	 * @return The maximums of all counts
	 */
	public static GraphAnalyticsRecord maxOfRecords(
			GraphAnalyticsRecord... records) {
		if (null == records || 0 == records.length)
			return null;

		int maxNumCommunities = 0;
		List<GraphEdge> maxEdgeList = Arrays.asList(new GraphEdge(Long.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Long.MIN_VALUE));
		GraphCommunity maxCommunity = new GraphCommunity(Integer.MIN_VALUE,
														Long.MIN_VALUE,
														new Pair<Double, Double>(Double.MIN_VALUE, Double.MIN_VALUE),
														Double.MIN_VALUE,
														Integer.MIN_VALUE,
														Long.MIN_VALUE,
														"",
														false,
														Long.MIN_VALUE,
														new Pair<Double, Double>(Double.MIN_VALUE, Double.MIN_VALUE),
														Double.MIN_VALUE,
														Arrays.asList(Double.MIN_VALUE),
														maxEdgeList,
														maxEdgeList);

		for (GraphAnalyticsRecord record : records) {
			if (null != record) {
				maxNumCommunities = Math.max(maxNumCommunities, record.getNumCommunities());
				maxInPlace(maxCommunity, record.getCommunities());
			}
		}
		return new GraphAnalyticsRecord(maxNumCommunities, Arrays.asList(maxCommunity));
	}
}
