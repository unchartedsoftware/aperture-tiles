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

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Iterator;

import com.oculusinfo.annotation.io.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class AnnotationSpeedTests extends AnnotationTestsBase {
	
	static final boolean  VERBOSE = false;	
	static final int	  STARTING_DEPTH = 6;
	private HBaseAnnotationIO 	 _hbaseIO;
    private List<AnnotationData> _annotations;

    
	//@Before
    public void setup ( int numEntries, boolean useKryo, boolean compress ) {
		
		_annotations = generateAnnotations( numEntries );
		
    	try {
    		_hbaseIO = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										 "2181",
									     "hadoop-s1.oculus.local:60000");
    		
    		_hbaseIO.setKryo( useKryo );
    		_hbaseIO.setCompression( compress );
    		_hbaseIO.dropTable( TABLE_NAME );
    		_hbaseIO.createTable( TABLE_NAME );
	    	_hbaseIO.writeAnnotations( TABLE_NAME, _annotations );
	    	
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		}
    }

    //@After
    public void teardown () {
    	_hbaseIO.dropTable(TABLE_NAME);
    	_hbaseIO = null;
    	_annotations = null;	
    }
    
    /*
    @Test
    public void rawIndexScanTests() {
    	
    	long startTime, endTime;
    	double timeSum = 0;
    	double areaSum = 0;
    	
    	for (int i=0; i<NUM_TESTS; i++) {
    		
    		AnnotationIndex p0 = generateIndex();
			AnnotationIndex p1 = generateIndex();				
			double xMax = Math.max( p0.getX(), p1.getX() );
			double xMin = Math.min( p0.getX(), p1.getX() );
			double yMax = Math.max( p0.getY(), p1.getY() );		
			double yMin = Math.min( p0.getY(), p1.getY() );		
			AnnotationIndex startScan = new AnnotationIndex(xMin, yMin, BOUNDS);
			AnnotationIndex stopScan = new AnnotationIndex(xMax, yMax, BOUNDS);
			List<AnnotationData> results = new LinkedList<AnnotationData>();
			System.out.println("Scanning from index " + startScan.getIndex() + " to " + stopScan.getIndex() + " over area of " + (xMax-xMin)*(yMax-yMin) );
    		startTime = System.currentTimeMillis();
    		try {
    			results = _hbaseIO.scanAnnotations(TABLE_NAME, startScan, stopScan);
    		} catch (Exception e) {}  		
    		
    		// aggregate code
    		endTime = System.currentTimeMillis();   		
    		double time = ((endTime-startTime)/1000.0);
    		double area = (xMax-xMin)*(yMax-yMin);
    		areaSum += area;
    		timeSum += time;
    		System.out.println("Returned " + results.size() + " entries, time: " + time + " seconds");
    	}
		System.out.println("Average time: "+ timeSum / (double)NUM_TESTS +" seconds");
		System.out.println("Average time: "+ timeSum / areaSum +" per area sqrd");	
    }
    
    @Test
    public void quadtreeScanTests() {
    	
    	long startTime, endTime;
    	double timeSum = 0;
    	double areaSum = 0;
    	
    	AnnotationBB fullBB = new AnnotationBB( BOUNDS );
		AnnotationQuadTree tree =  new AnnotationQuadTree( fullBB );

    	for (int i=0; i<NUM_TESTS; i++) {
    		
    		AnnotationIndex p0 = generateIndex();
			AnnotationIndex p1 = generateIndex();				
			double xMax = Math.max( p0.getX(), p1.getX() );
			double xMin = Math.min( p0.getX(), p1.getX() );
			double yMax = Math.max( p0.getY(), p1.getY() );		
			double yMin = Math.min( p0.getY(), p1.getY() );		
			double [] bounds = { xMin, yMin, xMax, yMax };	
			AnnotationBB query = new AnnotationBB( bounds, BOUNDS );

			List<AnnotationIndex> ranges = new LinkedList<AnnotationIndex>();
			List<AnnotationData> quadtreeEntries = new LinkedList<AnnotationData>();
			startTime = System.currentTimeMillis();
			
    		try {   
    								
    			ranges = tree.getIndexRanges( query, 6 );    			
    			quadtreeEntries = new LinkedList<AnnotationData>();
    			
    			Iterator<AnnotationIndex> rangeItr = ranges.iterator();
    			
    		    while ( rangeItr.hasNext() ){
    		      quadtreeEntries.addAll( _hbaseIO.scanAnnotations( TABLE_NAME, rangeItr.next(), rangeItr.next() ) );			
    		    }
  			
    		} catch (Exception e) {}  		
    		
    		// aggregate code
    		endTime = System.currentTimeMillis();
    		double time = ((endTime-startTime)/1000.0);
    		double area = (xMax-xMin)*(yMax-yMin);
    		areaSum += area;
    		timeSum += time;
    		System.out.println("Scanning " + ranges.size() + " ranges over area of " + area );
    		
    		System.out.println("Returned " + quadtreeEntries.size() + " entries, time: " + time + " seconds");        	
    	}
		System.out.println("Average time: "+ timeSum / (double)NUM_TESTS +" seconds");
		System.out.println("Average time: "+ timeSum / areaSum +" per area sqrd");	
    }
    */
    

    private double[] singleBatchTest( int depth, boolean compress ) {
    	
    	long startTime, endTime;
		double timeSum = 0;
		double entrySum = 0;
		AnnotationBB fullBB = new AnnotationBB( BOUNDS );
		AnnotationQuadTree tree =  new AnnotationQuadTree( fullBB );
		
		System.out.println( "\tTesting depth of " + depth );
		
    	for (int i=0; i<NUM_TESTS; i++) {
    		
    		AnnotationIndex p0 = generateIndex();
			AnnotationIndex p1 = generateIndex();				
			double xMax = Math.max( p0.getX(), p1.getX() );
			double xMin = Math.min( p0.getX(), p1.getX() );
			double yMax = Math.max( p0.getY(), p1.getY() );		
			double yMin = Math.min( p0.getY(), p1.getY() );	
			if (i == NUM_TESTS/2) {
				System.out.println( "****FULL DB SCAN****" );
				xMin = BOUNDS[0];
				yMin = BOUNDS[1];
				xMax = BOUNDS[2];
				yMax = BOUNDS[3];
			}
			double [] bounds = { xMin, yMin, xMax, yMax };	
			AnnotationBB query = new AnnotationBB( bounds, BOUNDS );

			List<AnnotationIndex> ranges = new LinkedList<AnnotationIndex>();
			List<AnnotationData> quadtreeEntries = new LinkedList<AnnotationData>();
			
			// start time
			startTime = System.currentTimeMillis();
			
    		try {   
    								
    			ranges = tree.getIndexRanges( query, depth );    			
    			quadtreeEntries = new LinkedList<AnnotationData>();
    			
    			Iterator<AnnotationIndex> rangeItr = ranges.iterator();
    			
    		    while ( rangeItr.hasNext() ){
    		    	AnnotationIndex start = rangeItr.next();
    		    	AnnotationIndex stop = rangeItr.next();
    		        quadtreeEntries.addAll( _hbaseIO.scanAnnotations( TABLE_NAME, start, stop ) );			
    		    }
  			
    		} catch (Exception e) {}  		
    		
    		// stop time
    		endTime = System.currentTimeMillis();
    		double time = ((endTime-startTime)/1000.0);
    		double area = (xMax-xMin)*(yMax-yMin);
    		timeSum += time;
    		entrySum += quadtreeEntries.size();
    		System.out.print("\t\tScanning " + ranges.size()/2 + " ranges over area of " + area + ", " );	    		
    		System.out.println("returned " + quadtreeEntries.size() + " entries in " + time + " seconds");        	
    	}
    	
    	System.out.println("\tAverage time: "+ timeSum / NUM_TESTS 
				  +" seconds, entries per second: " + entrySum / timeSum );
    	
    	double[] results = { timeSum / NUM_TESTS, entrySum / timeSum };
    	return results;
    	
	
    }

    private void multiBatchTest() {
    	
    	double bestAvgTime = 9999999999.0;
    	double bestEntriesByTime = 0.0;
    	int bestAvgTimeDepth = -1;
    	int bestEntriesByTimeDepth = -1;
    	int currentDepth = STARTING_DEPTH;

		while( currentDepth < 9 ) {

			double[] results = singleBatchTest( currentDepth, true );
			
			if ( results[0] < bestAvgTime) {
				System.out.println( "New best depth by avg time: " + results[0] );
				bestAvgTime =  results[0];
				bestAvgTimeDepth = currentDepth;
            }
			
			if ( results[1] > bestEntriesByTime) {
				System.out.println( "New best depth by entries per second: " + results[1] );
				bestEntriesByTime =  results[1];
				bestEntriesByTimeDepth = currentDepth;
            }
			
			currentDepth++;
		}
		
		System.out.println( "Best average time: "+ bestAvgTime +" seconds at depth: " + bestAvgTimeDepth );
		System.out.println( "Best entries per second: "+ bestEntriesByTime +" at depth: " + bestEntriesByTimeDepth );

    }
    
    /*
    @Test
    public void kryoCompressedTest() {	
    	
    	setup( true, true );
    	System.out.println( "Kryo Compressed" ); 	
    	multiBatchTest();
    	teardown();
    }
    
    @Test
    public void kryoUncompressedTest() {
    	
    	setup( true, false );
    	System.out.println( "Kryo Uncompressed" );
    	multiBatchTest();
    	teardown();

    }
    */

    @Test
    public void javaUncompressedTest() {
    	
    	//for (int i=1; i < 8; i+=2) {
	    	setup( 1000, false, false );
	    	System.out.println( "Java Uncompressed, db size of " + 1000 );
	    	multiBatchTest();
	    	teardown();
    	//}

    }
}
