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
package com.oculusinfo.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.oculusinfo.annotation.io.*;

import org.junit.Assert;
import org.junit.Test;


public class AnnotationTests {
	
	static final int      NUM_ENTRIES = 1000000;
	static final int      NUM_TESTS = 25;
	static final double[] BOUNDS = {-180.0, -90.0, 180.0, 90.0};
	static final boolean  VERBOSE = false;
	
	/*
	 * Annotation list printing utility function
	 */
	private void print( List<AnnotationData> annotations ) {
		
		int i = 0;
		for (AnnotationData annotation : annotations ) {
			
			System.out.println("\tEntry "+i+": " + annotation.getIndex().getX() 
											     + ", " 
											     + annotation.getIndex().getY() 
											     + ", "
											     + annotation.getIndex().getIndex() 
											     + ", "
											     + annotation.getComment() );
			i++;
		}
	}

	/*
	 * Annotation index generation function
	 */
	private AnnotationIndex generateIndex() {

		final Random rand = new Random();
		
		double x = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
			
		return new AnnotationIndex( x, y ,BOUNDS );
	}
	
	/*
	 * Annotation index generation function
	 */	
	private AnnotationIndex generateIndex(double lon, double lat) {
	    		
		return new AnnotationIndex( lon, lat, BOUNDS );
	}
	
	
	private List<AnnotationData> generateAnnotations(int numEntries) {

		List<AnnotationData> annotations = new ArrayList<>();		
		for (int i=0; i<numEntries; i++) {
			
			AnnotationIndex index = generateIndex();	
			annotations.add(new AnnotationData( index, "p"+Integer.toString((int)Math.random()*5), "comment " + i));	
		}
		return annotations;
	}
	
	
	@Test
	public void divideBBTest() {
	
		/*
		 * Annotation a bounding boxes represent the double x and y coordinates of the arbitrary input
		 * ranges and coordinates as long values, each axis 0 - 2^31
		 * 
		 * Bounding boxes should be able to break into 4 quadrants seamlessly, with no floating point
		 * conversion errors.
		 * 
		 * This test function randomly traverses down to the base resolution
		 */
		
		List<AnnotationData> annotations = generateAnnotations(NUM_ENTRIES);
		AnnotationBB bb = new AnnotationBB( BOUNDS );	
		
		for (int i=0; i<NUM_TESTS; i++) {
			System.out.println("divideBBTest() iteration " + i);
			divideBBTestRecursive(bb, 0);
		}

	}

	private void divideBBTestRecursive( AnnotationBB bb, int depth) {
	
		if (depth == 31) {
			return;
		}
		
		// divide bounding box into quadrants
		AnnotationBB ne = bb.getNE();
		AnnotationBB se = bb.getSE();
		AnnotationBB sw = bb.getSW();
		AnnotationBB nw = bb.getNW();

		if (VERBOSE) {
			System.out.println("Check quadtree range generation, depth: " + depth);
			System.out.println( "\tbb: \t" + bb.getRange().get(0).getIndex() + " - " + bb.getRange().get(1).getIndex() );
			System.out.println( "\t\tsw: " + sw.getRange().get(0).getIndex() + " - " + sw.getRange().get(1).getIndex() );			
			System.out.println( "\t\tse: " + se.getRange().get(0).getIndex() + " - " + se.getRange().get(1).getIndex() );			
			System.out.println( "\t\tnw: " + nw.getRange().get(0).getIndex() + " - " + nw.getRange().get(1).getIndex() );
			System.out.println( "\t\tne: " + ne.getRange().get(0).getIndex() + " - " + ne.getRange().get(1).getIndex() + "\n" );
		}
		
		// ensure index ranges are dividing seamlessly
		Assert.assertTrue(sw.getRange().get(1).getIndex()+1 == se.getRange().get(0).getIndex() &&
					  	  se.getRange().get(1).getIndex()+1 == nw.getRange().get(0).getIndex() &&
						  nw.getRange().get(1).getIndex()+1 == ne.getRange().get(0).getIndex() );
		
		int rand = (int)(Math.random() * 4.0);
		switch (rand) {
			case 0: divideBBTestRecursive( sw, depth+1 ); return;
			case 1: divideBBTestRecursive( se, depth+1 ); return;
			case 2: divideBBTestRecursive( nw, depth+1 ); return;
			default: divideBBTestRecursive( ne, depth+1 ); return;
		}
		
	}
	
	@Test
	public void containsTest() {
		
		List<AnnotationData> annotations = generateAnnotations(NUM_ENTRIES);
			
		double fullScanRatio = 0.0;
		double quadtreeRatio = 0.0;
		
		for (int i=0; i<NUM_TESTS; i++) {
		
			System.out.println("containsTest() iteration " + i);
			
			// pick two random points
			AnnotationIndex p0 = generateIndex();
			AnnotationIndex p1 = generateIndex();				
			double xMax = Math.max( p0.getX(), p1.getX() );
			double xMin = Math.min( p0.getX(), p1.getX() );
			double yMax = Math.max( p0.getY(), p1.getY() );		
			double yMin = Math.min( p0.getY(), p1.getY() );	
			double [] bounds = { xMin, yMin, xMax, yMax };	
			AnnotationIndex startScan = new AnnotationIndex(xMin, yMin, BOUNDS);
			AnnotationIndex stopScan = new AnnotationIndex(xMax, yMax, BOUNDS);
			
			// create bounding box from points
			AnnotationBB testBB = new AnnotationBB( bounds, BOUNDS );
			
			if (VERBOSE) {
				System.out.println( "Bounding box: bottom left: " + xMin + ", " + yMin +
											  ", top right: " + xMax + ", " + yMax );
			}
		
			List<AnnotationData> fullScanEntries = new ArrayList<AnnotationData>();			
			List<AnnotationData> manualEntries = new ArrayList<AnnotationData>();
			
			// manually check if annotation is in bounding box
			for ( AnnotationData annotation : annotations ) {
				
				boolean bbTest = false;
				boolean manTest = false;
				// use bounding box contains method
				if ( testBB.contains( annotation.getIndex() ) ) {
					bbTest = true;	
				}
				
				// manually check bounding box
				double x = annotation.getIndex().getX();
				double y = annotation.getIndex().getY();				
				if ( x > xMin && x < xMax &&
					 y > yMin && y < yMax ) {
					manTest = true;
					manualEntries.add( annotation );
				}
				
				if ( startScan.getIndex() <= annotation.getIndex().getIndex() &&
					 stopScan.getIndex() >= annotation.getIndex().getIndex() )
				{
					fullScanEntries.add( annotation );
				}
				// ensure contains works correctly
				Assert.assertEquals(bbTest, manTest); 
			}

			
			System.out.println( "\tManual bounding box check returns " + manualEntries.size() + " results");			
			if (VERBOSE) {
				print( manualEntries );
			}
			
			double quadtreePerc = (fullScanEntries.size() == 0)? 0 : 100 * ( (fullScanEntries.size() - manualEntries.size() ) / (double)fullScanEntries.size());			
			fullScanRatio += quadtreePerc;
					
			System.out.println("\tUnprocessed scan range pulled " + fullScanEntries.size() 
							 + " entries, resulting in " 
					         + (fullScanEntries.size() - manualEntries.size() ) 
						     + " redundant matches, (" + quadtreePerc + "%)" );			
			
			// query quad tree for index ranges
			AnnotationBB fullBB = new AnnotationBB( BOUNDS );
			AnnotationQuadTree tree =  new AnnotationQuadTree( fullBB );
			
			List<AnnotationIndex> results = tree.getIndexRanges( testBB, 9 );

			System.out.println("\tQuadtree returned " + results.size()/2 + " ranges");
			if (VERBOSE) {
				for (int j=0; j<results.size(); j+=2) {
					System.out.println("\t\tRange " + j/2 + " from: " + results.get(j).getIndex() + ", to: " + results.get(j+1).getIndex() );
				}
			}
			
			// ensure all manual ranges are found in queried ranges
			for ( AnnotationData annotation : manualEntries ) {
				boolean inside = false;
				long index = annotation.getIndex().getIndex();
				for (int j=0; j<results.size(); j+=2) {			
					long start = results.get(j).getIndex();
					long stop = results.get(j+1).getIndex();					
					if (index >= start && index <= stop) {
						inside = true;
						break;
					}
				}		
				Assert.assertTrue(inside);
			}
		
			// see how many extra entries these ranges pull
			int count = 0;
			for ( AnnotationData annotation : annotations ) {

				long index = annotation.getIndex().getIndex();
				for (int j=0; j<results.size(); j+=2) {			
					long start = results.get(j).getIndex();
					long stop = results.get(j+1).getIndex();					
					if (index >= start && index <= stop) {
						count++;
						break;
					}
				}		
			}
			double fullscanPerc = (count == 0)? 0 : 100 * ( (count - manualEntries.size() ) / (double)count);
			quadtreeRatio += fullscanPerc;
				
			System.out.println("\tQuadtree ranges pulled " + count
					 + " entries, resulting in " 
			         + (count - manualEntries.size())
				     + " redundant matches, (" + fullscanPerc + "%)" );	
		}
		
		System.out.println("\tAverage rate of false positives for unprocessed scan: " + fullScanRatio / NUM_TESTS);	
		System.out.println("\tAverage rate of false positives for quadtree scan: " + quadtreeRatio / NUM_TESTS);
	}

	
}
