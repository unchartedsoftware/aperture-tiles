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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.annotation.rest.impl.*;
import com.oculusinfo.binning.TileIndex;


public class ConcurrentServiceTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;
	static final int NUM_THREADS = 8;
	protected AnnotationService    _service;	
	protected List<AnnotationData> _annotations;
	
    @Before
    public void setup () {
    	
    	_service = new CachedAnnotationService();

    }

    @After
    public void teardown () {
    	_service = null;
    }


	private class TestRunnable implements Runnable {
		
		String _name;
		
		TestRunnable( String name ) {
			_name = name;
		}
		
		public void run() {
			
			List<AnnotationData> annotations = generateJSONAnnotations( NUM_ENTRIES );	
			List<AnnotationData> firstHalf = annotations.subList(0, NUM_ENTRIES/2);
			List<AnnotationData> secondHalf = annotations.subList(NUM_ENTRIES/2, NUM_ENTRIES);
	    				
			// write
			System.out.println( "Thread " + _name + " begins writing first half" );    	
	    	for (AnnotationData annotation : firstHalf ) {    	    		
	    		_service.writeAnnotation( annotation );
	    	}
	    	System.out.println( "Thread " + _name + " finished writing first half" );
	    	
	    	// read
	    	readAll();	
	    	
	    	// write
	    	System.out.println( "Thread " + _name + " begins writing second half" );  	
	    	for (AnnotationData annotation : secondHalf ) {    	    		
	    		_service.writeAnnotation( annotation );
	    	}
	    	System.out.println( "Thread " + _name + " finished writing second half" );
	    	
	    	// remove
	    	System.out.println( "Thread " + _name + " begins removing all" );
			for (AnnotationData annotation : annotations ) {
	    		_service.removeAnnotation( annotation );
	    	}
			System.out.println( "Thread " + _name + " finished removing all" );
	    	
		}
		
	}
	
	@Test
	public void concurrentTest() {
			
		List<Thread> threads = new LinkedList<>();
		for (int i=0; i<NUM_THREADS; i++) {
						
			Thread t = new Thread( new TestRunnable( ""+i ) );
			threads.add( t );			
			t.start();
		}
		
		for( Thread t : threads ) {
			try {
				t.join();
			} catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		
		// ensure everything was removed
    	List<AnnotationData> scan = readAll();
    	printData( scan );
    	Assert.assertTrue( scan.size() == 0 );
	}
	
	private List<AnnotationData> readAll() {
		
		// scan all
		TileIndex tile = new TileIndex( 0, 0, 0 );
    	List<AnnotationData> scan = _service.readAnnotation( tile );
    	return scan;

	}

}
