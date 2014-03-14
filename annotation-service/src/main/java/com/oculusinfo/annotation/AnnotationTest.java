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
	
	public static void main(String [] args)
	{
		long startTime;
		long endTime;
		
		String TABLE_NAME = "AnnotationTable";
		int NUM_ENTRIES = 100;
		
		HBaseAnnotationIO hbaseIO;
		try {
			/*
			hbaseIO = new HBaseAnnotationIO("hadoop-s1.oculus.local",
											"2181",
										    "hadoop-s1.oculus.local:60000");

			System.out.println("Creating table");	
			startTime = System.currentTimeMillis();
			hbaseIO.createTable( TABLE_NAME );
			endTime = System.currentTimeMillis();
	        System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
			 */
	        
			System.out.println("Writing "+NUM_ENTRIES+" to table");	
			startTime = System.currentTimeMillis();
			List<AnnotationData> annotations = generateAnnotations(NUM_ENTRIES);
			annotations.add( new AnnotationData( generateIndex(-180, -90), "p0", "bl") );
			annotations.add( new AnnotationData( generateIndex(-0.01, -0.01), "p0", "blt") );
						
			annotations.add( new AnnotationData( generateIndex(0.01, -90), "p0", "br") );
			annotations.add( new AnnotationData( generateIndex(180.0, -0.01), "p0", "brt") );
			
			annotations.add( new AnnotationData( generateIndex(-180, 0.01), "p0", "tl") );
			annotations.add( new AnnotationData( generateIndex(-0.01, 90.00), "p0", "tlt") );
			
			annotations.add( new AnnotationData( generateIndex(0.01, 0.01), "p0", "tr") );
			annotations.add( new AnnotationData( generateIndex(180.00, 90.00), "p0", "trt") );
			
			/*
			hbaseIO.writeAnnotations(TABLE_NAME, annotations);	
			endTime = System.currentTimeMillis();
			System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
			*/
			print( annotations );
			
			// build bounding box from 2 random points
			// bounding box corners must be created from lowest val corner to highest val corner
			AnnotationIndex p0 = generateIndex();
			AnnotationIndex p1 = generateIndex();				
			double xMax = Math.max( p0.getX(), p1.getX() );
			double xMin = Math.min( p0.getX(), p1.getX() );
			double yMax = Math.max( p0.getY(), p1.getY() );		
			double yMin = Math.min( p0.getY(), p1.getY() );			
			AnnotationIndex start = new AnnotationIndex(xMin, yMin, BOUNDS);
			AnnotationIndex stop = new AnnotationIndex(xMax, yMax, BOUNDS);			
			System.out.println( "Bounding box: bottom left: " + start.getX() + ", " + start.getY() );
			System.out.println( "		         top right: " + stop.getX() + ", " + stop.getY() );
			
			/*
			// spatial indexing bounding box test
			System.out.println( "Spatial indexing test\nScanning table from " + start.getIndex() + " to " + stop.getIndex() );	
			startTime = System.currentTimeMillis();
			List<AnnotationData> scannedEntries = hbaseIO.scanAnnotations( TABLE_NAME, start, stop );			
			endTime = System.currentTimeMillis();
			System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
			print( scannedEntries );
			
			// manual bounding box test, make sure indexing pulls in at least all of these... it will pull in more
			System.out.println( "Manual indexing test\nScanning full table" );	
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
			print( manualEntries );
			
			
			System.out.println( "Comparing scan to manual, scan returned " + scannedEntries.size() + " indices, manual " + manualEntries.size() );			
			for ( AnnotationData annotation : manualEntries ) {
				if ( !scannedEntries.contains( annotation ) ) {
					System.out.println("Test failed: index " + annotation.getIndex().getIndex() + " is missing" );
				}				
			}
			*/
			System.out.println("Check quadtree range generation");
			AnnotationBB bb = new AnnotationBB( BOUNDS );	
			AnnotationBB ne = bb.getNE();
			AnnotationBB se = bb.getSE();
			AnnotationBB sw = bb.getSW();
			AnnotationBB nw = bb.getNW();
			AnnotationQuadTree tree =  new AnnotationQuadTree( bb );
			System.out.println( "bb ll: " + bb.getRange().get(0).getIndex() + ", ur: " + bb.getRange().get(1).getIndex() );
			System.out.println( "ne ll: " + ne.getRange().get(0).getIndex() + ", ur: " + ne.getRange().get(1).getIndex() );
			System.out.println( "se ll: " + se.getRange().get(0).getIndex() + ", ur: " + se.getRange().get(1).getIndex() );
			System.out.println( "sw ll: " + sw.getRange().get(0).getIndex() + ", ur: " + sw.getRange().get(1).getIndex() );
			System.out.println( "nw ll: " + nw.getRange().get(0).getIndex() + ", ur: " + nw.getRange().get(1).getIndex() );
			
			/*
			System.out.println("Disabling and dropping table");
			startTime = System.currentTimeMillis();
			hbaseIO.dropTable(TABLE_NAME);
			endTime = System.currentTimeMillis();
			System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
			*/
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			
			
		}

		
		
	}

	
}
