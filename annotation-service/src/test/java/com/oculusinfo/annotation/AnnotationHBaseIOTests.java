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

import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;


public class AnnotationHBaseIOTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;

	private HBaseAnnotationIO _hbaseIO;
    private List<AnnotationData> _annotations;
    private AnnotationIndex _start;
    private AnnotationIndex _stop;
    
    @Before
    public void setup () {
    	try {
    		_hbaseIO = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										     "2181",
									         "hadoop-s1.oculus.local:60000");
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {
		}	
    	_annotations = generateAnnotations(NUM_ENTRIES);
    	
    	// build bounding box from 2 random points
		AnnotationIndex p0 = generateIndex();
		AnnotationIndex p1 = generateIndex();		
		// bounding box corners must be created from lowest val corner to highest val corner
		double xMax = Math.max( p0.getX(), p1.getX() );
		double xMin = Math.min( p0.getX(), p1.getX() );
		double yMax = Math.max( p0.getY(), p1.getY() );		
		double yMin = Math.min( p0.getY(), p1.getY() );		
		_start = new AnnotationIndex(xMin, yMin, BOUNDS);
		_stop = new AnnotationIndex(xMax, yMax, BOUNDS);	
    }

    @After
    public void teardown () {
    	_hbaseIO = null;
    	_annotations = null;
    	_start = null;
    	_stop = null;
    }
	
	public AnnotationIndex generateIndex() {

		final Random rand = new Random();
		
		double x = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
			
		return new AnnotationIndex( x, y ,BOUNDS );
	}
	
	public AnnotationIndex generateIndex(double lon, double lat) {
	    		
		return new AnnotationIndex( lon, lat, BOUNDS );
	}
	
	public List<AnnotationData> generateAnnotations(int numEntries) {

		List<AnnotationData> annotations = new ArrayList<>();		
		for (int i=0; i<numEntries; i++) {
			
			AnnotationIndex index = generateIndex();	
			annotations.add(new AnnotationData( index, "p"+Integer.toString((int)Math.random()*5), "comment " + i));	
		}
		return annotations;
	}
    
    @Test
    public void testHBaseIO() {
    	
    	boolean USE_KRYO = true;
    	boolean USE_COMPRESSION = true;
    	
    	_hbaseIO.setKryo( USE_KRYO );
    	_hbaseIO.setCompression( USE_COMPRESSION );
    	
    	try {
    		
	        /*
	    	 *  Create Table
	    	 */
			System.out.println("Creating table");
	    	_hbaseIO.createTable( TABLE_NAME );
	    	
	        /*
	    	 *  Write annotations
	    	 */ 	
	    	System.out.println("Writing "+NUM_ENTRIES+" to table");	
	    	_hbaseIO.writeAnnotations(TABLE_NAME, _annotations);
	    	
	        /*
	    	 *  Scan range annotations
	    	 */
	    	System.out.println( "Scanning table from " + _start.getIndex() + " to " + _stop.getIndex() );	
	    	List<AnnotationData> scannedEntries = _hbaseIO.scanAnnotations( TABLE_NAME, _start, _stop );			
	        if (VERBOSE) print( scannedEntries );
	        
	    	/*
	    	 *  Scan and check all annotations
	    	 */
	    	System.out.println( "Scanning all annotations" );
	    	List<AnnotationData> allEntries = _hbaseIO.scanAnnotations( TABLE_NAME );
	    	if (VERBOSE) print( allEntries );
	    	for ( AnnotationData annotation : _annotations ) {
	    		Assert.assertTrue( allEntries.contains( annotation ) );
	    		allEntries.remove( annotation );
	    	}
	
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {
			/*
	    	 * Drop table
	    	 */
	    	System.out.println("Disabling and dropping table");
	    	_hbaseIO.dropTable(TABLE_NAME);
		}
    }


	
}
