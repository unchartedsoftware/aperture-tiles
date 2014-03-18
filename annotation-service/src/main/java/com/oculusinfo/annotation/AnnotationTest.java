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

import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.Number;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.oculusinfo.annotation.io.*;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.impl.*;

public class AnnotationTest{
	
	static final double[] BOUNDS = {-180.0, -90.0, 180.0, 90.0};	
	static final String   TABLE_NAME = "AnnotationTable";
	static final int      NUM_ENTRIES = 1000;
	
	public static void print( List<AnnotationData> annotations ) {
		
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
	
	
	public static AnnotationIndex generateIndex() {

		final Random rand = new Random();
		
		double x = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
			
		return new AnnotationIndex( x, y ,BOUNDS );
	}
	
	public static AnnotationIndex generateIndex(double lon, double lat) {
	    		
		return new AnnotationIndex( lon, lat, BOUNDS );
	}
	
	
	public static List<AnnotationData> generateAnnotations(int numEntries) {

		List<AnnotationData> annotations = new ArrayList<>();		
		for (int i=0; i<numEntries; i++) {
			
			AnnotationIndex index = generateIndex();	
			annotations.add(new AnnotationData( index, "p"+Integer.toString((int)Math.random()*5), "comment " + i));	
		}
		return annotations;
	}
	
	
	public static boolean divideBBTest( AnnotationBB bb, int depth, boolean verbose ) {
	
		/*
		 * Annotation a bounding boxes represent the double x and y coordinates of the arbitrary input
		 * ranges and coordinates as long values, each axis 0 - 2^31
		 * 
		 * Bounding boxes should be able to break into 4 quadrants seamlessly, with no floating point
		 * conversion errors.
		 * 
		 * This test function randomly traverses down to the base resolution
		 */
		
		if (depth == 31) {
			System.out.println( "Success: all indexes are continuous along span of 0 to " + AnnotationIndex.MAX_UNIT * AnnotationIndex.MAX_UNIT);			
			return true;
		}
		
		AnnotationBB ne = bb.getNE();
		AnnotationBB se = bb.getSE();
		AnnotationBB sw = bb.getSW();
		AnnotationBB nw = bb.getNW();

		if (verbose) {
			System.out.println("Check quadtree range generation, depth: " + depth);
			System.out.println( "\tbb: \t" + bb.getRange().get(0).getIndex() + " - " + bb.getRange().get(1).getIndex() );
			System.out.println( "\t\tsw: " + sw.getRange().get(0).getIndex() + " - " + sw.getRange().get(1).getIndex() );			
			System.out.println( "\t\tse: " + se.getRange().get(0).getIndex() + " - " + se.getRange().get(1).getIndex() );			
			System.out.println( "\t\tnw: " + nw.getRange().get(0).getIndex() + " - " + nw.getRange().get(1).getIndex() );
			System.out.println( "\t\tne: " + ne.getRange().get(0).getIndex() + " - " + ne.getRange().get(1).getIndex() + "\n" );
		}
		
		
		if ( sw.getRange().get(1).getIndex()+1 != se.getRange().get(0).getIndex() &&
			 se.getRange().get(1).getIndex()+1 != nw.getRange().get(0).getIndex() &&
			 nw.getRange().get(1).getIndex()+1 != ne.getRange().get(0).getIndex() ) {			
			System.out.println( "Error: Indexes are not continuous along span of 0 to " + AnnotationIndex.MAX_UNIT);
			return false;
		}
		
		int rand = (int)(Math.random() * 4.0);
		switch (rand) {
			case 0: return divideBBTest( sw, depth+1, verbose );
			case 1: return divideBBTest( se, depth+1, verbose );
			case 2: return divideBBTest( nw, depth+1, verbose );
			default: return divideBBTest( ne, depth+1, verbose );
		}
		
	}
	
	
	public static boolean quadTreeTest( List<AnnotationData> annotations, boolean verbose ) {
		
		AnnotationIndex p0 = generateIndex();
		AnnotationIndex p1 = generateIndex();				
		double xMax = Math.max( p0.getX(), p1.getX() );
		double xMin = Math.min( p0.getX(), p1.getX() );
		double yMax = Math.max( p0.getY(), p1.getY() );		
		double yMin = Math.min( p0.getY(), p1.getY() );	
		double [] bounds = { xMin, yMin, xMax, yMax };	
		AnnotationBB testBB = new AnnotationBB( bounds, BOUNDS );
		
		if (verbose) {
			System.out.println( "\tBounding box: bottom left: " + xMin + ", " + yMin +
										  ", top right: " + xMax + ", " + yMax );
		}
		
		List<AnnotationData> manualEntries = new ArrayList<AnnotationData>();
		for ( AnnotationData annotation : annotations ) {
			
			boolean bbTest = false;
			boolean manTest = false;
			if ( testBB.contains( annotation.getIndex() ) ) {
				bbTest = true;	
			}
			
			double x = annotation.getIndex().getX();
			double y = annotation.getIndex().getY();
			
			if ( x > xMin && x < xMax &&
				 y > yMin && y < yMax ) {
				manTest = true;
				manualEntries.add( annotation );
			}
			
			if ( bbTest != manTest ) {
				System.out.println("Error: bounding box containment function failed");
				System.out.println("x: " + x + ", y: " + y);
				System.out.println( "\tBounding box: bottom left: " + xMin + ", " + yMin +
						  ", top right: " + xMax + ", " + yMax );
				return false;
			}
		}
		
		if (verbose) {
			System.out.println( "\tManual box check returns " + manualEntries.size() + " results");
			print( manualEntries );
		}
		
		AnnotationBB fullBB = new AnnotationBB( BOUNDS );
		AnnotationQuadTree tree =  new AnnotationQuadTree( fullBB );
		
		List<AnnotationIndex> results = tree.getIndexRanges(testBB);

		if (verbose) {

			System.out.println("\tQuadtree returned " + results.size()/2 + " ranges");
			for (int i=0; i<results.size(); i+=2) {
				System.out.println("\t\tRange " + i/2 + " from: " + results.get(i).getIndex() + ", to: " + results.get(i+1).getIndex() );
			}
		}
		
		// make sure all manual entries fit within given ranges
		for ( AnnotationData annotation : manualEntries ) {
			//
			boolean inside = false;
			long index = annotation.getIndex().getIndex();
			for (int i=0; i<results.size(); i+=2) {
				
				long start = results.get(i).getIndex();
				long stop = results.get(i+1).getIndex();
				if (index >= start && index <= stop) {
					inside = true;
					break;
				}
			}
			if (!inside) {
				System.out.println("Error: annotation " + index + " inside bounding box was not found in quad tree index range!");
				return false;
			}					
		}
		
		System.out.println("Success: all annotations found inside ranges return from quadtree");		
		return true;
	}
	
	
	public static void main(String [] args)
	{
		long startTime;
		long endTime;

		
		HBaseAnnotationIO hbaseIO;
		try {
			
			// create hbase annotation io
			hbaseIO = new HBaseAnnotationIO("hadoop-s1.oculus.local",
											"2181",
										    "hadoop-s1.oculus.local:60000");

			/*
			 *  Create Table
			 */
			System.out.println("Creating table");	
			startTime = System.currentTimeMillis();
			hbaseIO.createTable( TABLE_NAME );
			endTime = System.currentTimeMillis();
	        System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
			 
	        /*
			 *  Write annotations
			 */
			System.out.println("Writing "+NUM_ENTRIES+" to table");	
			startTime = System.currentTimeMillis();
			List<AnnotationData> annotations = generateAnnotations(NUM_ENTRIES);
			hbaseIO.writeAnnotations(TABLE_NAME, annotations);	
			endTime = System.currentTimeMillis();
			System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");			
			//print( annotations );

			/*
			 *  Create bounding box query
			 */
			// build bounding box from 2 random points
			AnnotationIndex p0 = generateIndex();
			AnnotationIndex p1 = generateIndex();		
			// bounding box corners must be created from lowest val corner to highest val corner
			double xMax = Math.max( p0.getX(), p1.getX() );
			double xMin = Math.min( p0.getX(), p1.getX() );
			double yMax = Math.max( p0.getY(), p1.getY() );		
			double yMin = Math.min( p0.getY(), p1.getY() );	
			double [] bounds = { xMin, yMin, xMax, yMax };	
			AnnotationIndex start = new AnnotationIndex(xMin, yMin, BOUNDS);
			AnnotationIndex stop = new AnnotationIndex(xMax, yMax, BOUNDS);			
			
			/*
			 *  Raw range indexing
			 */
			// spatial indexing bounding box test
			System.out.println( "Raw indexing test\nScanning table from " + start.getIndex() + " to " + stop.getIndex() );	
			startTime = System.currentTimeMillis();
			List<AnnotationData> scannedEntries = hbaseIO.scanAnnotations( TABLE_NAME, start, stop );			
			endTime = System.currentTimeMillis();
			System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
			//print( scannedEntries );
			
			/*
			 *  Manual full scan
			 */
			// manual bounding box test, make sure indexing pulls in at least all of these... it will pull in more
			System.out.println( "Manual indexing test\nScanning full table and parsing indices manually" );	
			startTime = System.currentTimeMillis();
			List<AnnotationData> allEntries = hbaseIO.scanAnnotations( TABLE_NAME );
			List<AnnotationData> manualEntries = new ArrayList<AnnotationData>();
			for ( AnnotationData annotation : allEntries ) {
				if ( annotation.getIndex().getX() >= xMin &&
					 annotation.getIndex().getY() >= yMin &&
					 annotation.getIndex().getX() <= xMax &&
					 annotation.getIndex().getY() <= yMax ) {
					manualEntries.add( annotation );
				}
			}
			endTime = System.currentTimeMillis();
			System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
			//print( manualEntries );
			
			/*
			 *  Quadtree range query
			 */
			System.out.println( "Quad tree based query");
			AnnotationQuadTree tree =  new AnnotationQuadTree( new AnnotationBB( BOUNDS ) );	
			startTime = System.currentTimeMillis();
			AnnotationBB query = new AnnotationBB( bounds, BOUNDS );			
			List<AnnotationIndex> ranges = tree.getIndexRanges(query);
			List<AnnotationData> quadtreeEntries = new ArrayList<AnnotationData>();
			for (int i=0; i<ranges.size(); i+=2) {
				quadtreeEntries.addAll( hbaseIO.scanAnnotations( TABLE_NAME, ranges.get(i), ranges.get(i+1) ) );				
			}
			endTime = System.currentTimeMillis();
			System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
			//print( quadtreeEntries );
			
			
			
			
			
			
			/*
			 * Drop table
			 */
			System.out.println("Disabling and dropping table");
			startTime = System.currentTimeMillis();
			hbaseIO.dropTable(TABLE_NAME);
			endTime = System.currentTimeMillis();
			System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");

			
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			
			
		}

		
		
	}

	
}
