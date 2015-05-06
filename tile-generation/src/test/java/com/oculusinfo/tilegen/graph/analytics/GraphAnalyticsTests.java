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
package com.oculusinfo.tilegen.graph.analytics;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.tilegen.graph.analytics.GraphAnalyticsRecord;
import com.oculusinfo.tilegen.graph.analytics.GraphCommunity;

/**
 * Unit tests for Graph Analytics (i.e. GraphAnalyticsRecord and GraphCommunity objects)
 */
public class GraphAnalyticsTests {

	// sample graph community record
	int _hierLevel = 1;
	long _id = 123L;
	Pair<Double, Double>_coords = new Pair<Double, Double>(1.2, 3.4);
	double _radius = 5.6;
	int _degree = 3;
	long _numNodes = 42L;
	String _metadata = "blah1\tblah2\tblah3";
	boolean _bIsPrimaryNode = false;
	long _parentID = 456L;
	Pair<Double, Double>_parentCoords = new Pair<Double, Double>(3.3, 4.4);
	double _parentRadius = 10.2;
	
	List<Double> _statsList = Arrays.asList(1.0, 2.0, 33.0, -23.0);
	List<GraphEdge> _interEdges = Arrays.asList(new GraphEdge(0L, 4.3, 2.1, 5L),
	                                new GraphEdge(43L, 5.6, 7.8, 3L));
	List<GraphEdge> _intraEdges = Arrays.asList(new GraphEdge(2L, 4.2, 2.0, 6L),
            new GraphEdge(44L, 5.5, 7.7, 4L));
	
	private GraphCommunity _sampleCommunity = new GraphCommunity(_hierLevel, 
																_id, 
																_coords,
																_radius,
																_degree,
																_numNodes,
																_metadata,
																_bIsPrimaryNode,
																_parentID,
																_parentCoords,
																_parentRadius,
																_statsList,
																_interEdges,
																_intraEdges);
	
	private GraphAnalyticsRecord _sampleRecord = new GraphAnalyticsRecord(1, Arrays.asList(_sampleCommunity));
	
	//---- Test that two records with the same data are equal
	@Test
	public void testRecordsEqual() {
		GraphAnalyticsRecord a = new GraphAnalyticsRecord(1, Arrays.asList(_sampleCommunity));
			
		Assert.assertEquals(_sampleRecord, a);
	}
	
	//---- Adding a community to an existing record
	@Test
    public void testCommunityToRecord () {
		GraphAnalyticsRecord a = new GraphAnalyticsRecord(1, Arrays.asList(_sampleCommunity));
		
		GraphCommunity community_b = new GraphCommunity(_hierLevel, 
														456L, 
														new Pair<Double, Double>(3.3, 4.4),
														3.4,
														4,
														54,
														"blah4\tblah5",
														true,
														_parentID,
														_parentCoords,
														_parentRadius,
														_statsList,
														_interEdges,
														_intraEdges);

		GraphAnalyticsRecord c = new GraphAnalyticsRecord(2, Arrays.asList(community_b, _sampleCommunity));		

		Assert.assertEquals(c, GraphAnalyticsRecord.addCommunityToRecord(a, community_b));
    }
	
	
	//---- Adding an inter edge to an existing community
	@Test
    public void testInterEdgeToCommunity () {
		GraphCommunity a = _sampleCommunity;
		GraphEdge e1 = new GraphEdge(987L, 0.1, 0.2, 999L);
		GraphCommunity b = a;
		b.addInterEdgeToCommunity(e1);
		
		List<GraphEdge> edges = Arrays.asList(e1, 
											new GraphEdge(0L, 4.3, 2.1, 5L),
                							new GraphEdge(43L, 5.6, 7.8, 3L));
		
		GraphCommunity c = new GraphCommunity(_hierLevel, 
											_id, 
											_coords,
											_radius,
											_degree,
											_numNodes,
											_metadata,
											_bIsPrimaryNode,
											_parentID,
											_parentCoords,
											_parentRadius,
											_statsList,
											edges,
											_intraEdges);

		Assert.assertEquals(c, b);
    }
	
	//---- Adding an intra edge to an existing community
	@Test
    public void testIntraEdgeToCommunity () {
		GraphCommunity a = _sampleCommunity;
		GraphEdge e2 = new GraphEdge(988L, 0.11, 0.22, 1L);
		GraphCommunity b = a;
		b.addIntraEdgeToCommunity(e2);
		
		List<GraphEdge> edges = Arrays.asList(new GraphEdge(2L, 4.2, 2.0, 6L),
	            							new GraphEdge(44L, 5.5, 7.7, 4L),
                							e2);
		
		GraphCommunity c = new GraphCommunity(_hierLevel, 
											_id, 
											_coords,
											_radius,
											_degree,
											_numNodes,
											_metadata,
											_bIsPrimaryNode,
											_parentID,
											_parentCoords,
											_parentRadius,
											_statsList,
											_interEdges,
											edges);

		Assert.assertEquals(c, b);
    }
	
	//---- Adding a 'very small weight' edge to a community already containing 10 edges	
	//TODO -- need to change this test if MAX_EDGES in GraphCommunity != 10
/*	@Test
    public void testEdgeAggregationSmall() {
		GraphEdge e1 = new GraphEdge(0L, 0.1, 0.1, 100L);
		List<GraphEdge> edges = Arrays.asList(e1,e1,e1,e1,e1,e1,e1,e1,e1,e1);
			
		GraphCommunity a = new GraphCommunity(_hierLevel, 
												_id, 
												_coords,
												_radius,
												_degree,
												_numNodes,
												_metadata,
												_bIsPrimaryNode,
												_parentID,
												_parentCoords,
												_parentRadius,
												_interEdges,
												edges);
		
		GraphEdge e2 = new GraphEdge(0L, 0.1, 0.1, 1L);
		GraphCommunity b = a;
		b.addIntraEdgeToCommunity(e2);
	
		Assert.assertEquals(a, b);
    }	
	
	//---- Adding a 'very high weight' edge to a community already containing 10 edges	
	//TODO -- need to change this test if MAX_EDGES in GraphCommunity != 10
	@Test
    public void testEdgeAggregationLarge() {
		GraphEdge e1 = new GraphEdge(0L, 0.1, 0.1, 1L);
		List<GraphEdge> edges = Arrays.asList(e1,e1,e1,e1,e1,e1,e1,e1,e1,e1);
			
		GraphCommunity a = new GraphCommunity(_hierLevel, 
												_id, 
												_coords,
												_radius,
												_degree,
												_numNodes,
												_metadata,
												_bIsPrimaryNode,
												_parentID,
												_parentCoords,
												_parentRadius,
												edges,
												_intraEdges);
		
		GraphEdge e2 = new GraphEdge(0L, 0.1, 0.1, 10L);
		GraphCommunity b = a;
		b.addInterEdgeToCommunity(e2);
		
		List<GraphEdge> edges2 = Arrays.asList(e2,e1,e1,e1,e1,e1,e1,e1,e1,e1);
		GraphCommunity c = new GraphCommunity(_hierLevel, 
											_id, 
											_coords,
											_radius,
											_degree,
											_numNodes,
											_metadata,
											_bIsPrimaryNode,
											_parentID,
											_parentCoords,
											_parentRadius,
											edges2,
											_intraEdges);
	
		Assert.assertEquals(c, b);
    }	
*/	
	//---- Adding a record to an empty record
	@Test
    public void testEmptyRecordAggregation () {
		GraphAnalyticsRecord a = new GraphAnalyticsRecord(0, null);
		
		GraphCommunity community_b = new GraphCommunity(_hierLevel, 
														456L, 
														new Pair<Double, Double>(3.3, 4.4),
														3.4,
														4,
														54,
														"blah4\tblah5",
														true,
														_parentID,
														_parentCoords,
														_parentRadius,
														_statsList,
														_interEdges,
														_intraEdges);

		GraphAnalyticsRecord b = new GraphAnalyticsRecord(1, Arrays.asList(community_b));

		Assert.assertEquals(b, GraphAnalyticsRecord.addRecords(a, b));
    }
	
	
	//---- Adding two records
	@Test
    public void testRecordAggregation () {
		GraphAnalyticsRecord a = new GraphAnalyticsRecord(1, Arrays.asList(_sampleCommunity));
		
		GraphCommunity community_b = new GraphCommunity(_hierLevel, 
														456L, 
														new Pair<Double, Double>(3.3, 4.4),
														3.4,
														4,
														54,
														"blah4\tblah5",
														true,
														_parentID,
														_parentCoords,
														_parentRadius,
														_statsList,
														_interEdges,
														_intraEdges);

		GraphAnalyticsRecord b = new GraphAnalyticsRecord(1, Arrays.asList(community_b));

		GraphAnalyticsRecord c = new GraphAnalyticsRecord(2, Arrays.asList(community_b, _sampleCommunity));		

		Assert.assertEquals(c, GraphAnalyticsRecord.addRecords(a, b));
    }
	
	//---- Adding a 'too-small' community to a record already containing 10 communities	
	//TODO -- need to change this test if MAX_COMMUNITIES in GraphAnalyticsRecord class != 10
/*	@Test
    public void testRecordAggregationSmall () {
		GraphAnalyticsRecord a = new GraphAnalyticsRecord(10, Arrays.asList(_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity));
		
		GraphCommunity community_b = new GraphCommunity(_hierLevel, 
														456L, 
														new Pair<Double, Double>(3.3, 4.4),
														3.4,
														4,
														2,
														"blain h4\tblah5",
														true,
														_parentID,
														_parentCoords,
														_parentRadius,
														_interEdges,
														_intraEdges);

		GraphAnalyticsRecord b = new GraphAnalyticsRecord(1, Arrays.asList(community_b));

		GraphAnalyticsRecord c = new GraphAnalyticsRecord(11, Arrays.asList(_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity));		

		Assert.assertEquals(c, GraphAnalyticsRecord.addRecords(a, b));
    }
	
	//---- Adding a 'very large' community to a record already containing 10 communities	
	//TODO -- need to change this test if MAX_COMMUNITIES in GraphAnalyticsRecord class != 10
	@Test
    public void testRecordAggregationLarge () {
		GraphAnalyticsRecord a = new GraphAnalyticsRecord(10, Arrays.asList(_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity));
		
		GraphCommunity community_b = new GraphCommunity(_hierLevel, 
														456L, 
														new Pair<Double, Double>(3.3, 4.4),
														3.4,
														4,
														1000,
														"blain h4\tblah5",
														true,
														_parentID,
														_parentCoords,
														_parentRadius,
														_interEdges,
														_intraEdges);

		GraphAnalyticsRecord b = new GraphAnalyticsRecord(1, Arrays.asList(community_b));

		GraphAnalyticsRecord c = new GraphAnalyticsRecord(11, Arrays.asList(community_b,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity,
																			_sampleCommunity));		

		Assert.assertEquals(c, GraphAnalyticsRecord.addRecords(a, b));
    }
*/

	//---- Min of two records
    @Test
    public void testMin() {
    	GraphAnalyticsRecord a = new GraphAnalyticsRecord(2, Arrays.asList(_sampleCommunity));
		GraphCommunity community_b = new GraphCommunity(1, 
														567L, 
														new Pair<Double, Double>(3.3, 4.4),
														3.4,
														4,
														54,
														"blah4\tblah5",
														true,
														567L,
														new Pair<Double, Double>(7.2, 0.1),
														10.1,
														Arrays.asList(99.0),
														Arrays.asList(new GraphEdge(1L, 4.3, 2.1, 5L)),
														Arrays.asList(new GraphEdge(1L, 4.3, 2.1, 5L)));
		GraphAnalyticsRecord b = new GraphAnalyticsRecord(1, Arrays.asList(community_b));
		
		GraphCommunity community_c = new GraphCommunity(1, 
				123L, 
				new Pair<Double, Double>(1.2, 3.4),
				3.4,
				3,
				42L,
				"",
				false,
				456L,
				new Pair<Double, Double>(3.3, 0.1),
				10.1,
				Arrays.asList(-23.0),
				Arrays.asList(new GraphEdge(0L, 4.3, 2.1, 3L)),
				Arrays.asList(new GraphEdge(1L, 4.2, 2.0, 4L)));		

		GraphAnalyticsRecord c = new GraphAnalyticsRecord(1, Arrays.asList(community_c));		
		
        Assert.assertEquals(c, GraphAnalyticsRecord.minOfRecords(a, b));
    }	
	
	//---- Max of two records
    @Test
    public void testMax() {
    	GraphAnalyticsRecord a = new GraphAnalyticsRecord(2, Arrays.asList(_sampleCommunity));
		GraphCommunity community_b = new GraphCommunity(1, 
														567L, 
														new Pair<Double, Double>(3.3, 4.4),
														3.4,
														4,
														54,
														"blah4\tblah5",
														true,
														567L,
														new Pair<Double, Double>(7.2, 0.1),
														10.1,
														Arrays.asList(-23.0),
														Arrays.asList(new GraphEdge(1L, 4.3, 2.1, 5L)),
														Arrays.asList(new GraphEdge(1L, 4.3, 2.1, 5L)));
		GraphAnalyticsRecord b = new GraphAnalyticsRecord(1, Arrays.asList(community_b));
		
		GraphCommunity community_c = new GraphCommunity(1, 
				567L, 
				new Pair<Double, Double>(3.3, 4.4),
				5.6,
				4,
				54L,
				"",
				false,
				567L,
				new Pair<Double, Double>(7.2, 4.4),
				10.2,
				Arrays.asList(33.0),
				Arrays.asList(new GraphEdge(43L, 5.6, 7.8, 5L)),
				Arrays.asList(new GraphEdge(44L, 5.5, 7.7, 6L)));		

		GraphAnalyticsRecord c = new GraphAnalyticsRecord(2, Arrays.asList(community_c));		
		
        Assert.assertEquals(c, GraphAnalyticsRecord.maxOfRecords(a, b));
    }	
	
}
