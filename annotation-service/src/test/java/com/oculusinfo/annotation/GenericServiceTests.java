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

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import com.oculusinfo.annotation.query.*;
import com.oculusinfo.annotation.rest.*;
import com.oculusinfo.annotation.rest.impl.*;


public class GenericServiceTests<T> extends AnnotationTestsBase {
	
	protected static final int BLOCK_SIZE = 250;

	protected AnnotationService<T> _service;	
	protected List<T> _annotations;
	protected T _start;
	protected T _stop;

    //@Test
    public void testIndividualService() {

    	// write all annotations
    	int count = 0;
    	long start, end;
    	double time;
    	double timeSum = 0;
    	
    	System.out.println("Writing " + NUM_ENTRIES + " annotations");
    	start = System.currentTimeMillis();
    	for (T annotation : _annotations ) {
    		_service.writeAnnotation( annotation );
    		count++;
    		if (count % 100 == 0) {
    			end = System.currentTimeMillis();
    			time = ((end-start)/1000.0);
    			timeSum += time;
    			System.out.println( count + " entries written in " + time + " seconds, avg per entry is " + time / 100);
    			start = System.currentTimeMillis();
    		}
    	}
    	System.out.println( "Average write time is " + timeSum / NUM_ENTRIES + " seconds");

    	// scan all
    	System.out.println("Scanning " + NUM_ENTRIES + " annotations");
    	start = System.currentTimeMillis();
    	List<AnnotationBin<T>> scan = _service.readAnnotations( _start, _stop, 0 );
    	end = System.currentTimeMillis();
		time = ((end-start)/1000.0);
		System.out.println( scan.size() + " entries scanned in " + time + " seconds");
		
		/*
		for (T data : _annotations) {
			boolean found = false;
			for (AnnotationBin<T> bin : scan) {
				
				for (int i=0; i < bin.getData().size(); i++) {
					if ( bin.getData().get(i).toString().equals( data.toString() )) {
						found = true;
						break;
					}
				}
				
			}
			if (!found) {
				System.out.println( data.toString() );
			}
		}
		*/
		
		
		
    	// remove annotations
    	System.out.println("Removing " + NUM_ENTRIES + " annotations");
    	count = 0;
    	timeSum = 0;
    	start = System.currentTimeMillis();
    	for (T annotation : _annotations ) {
    		_service.removeAnnotation( annotation );
    		count++;
    		if (count % 100 == 0) {
    			end = System.currentTimeMillis();
    			time = ((end-start)/1000.0);
    			timeSum += time;
    			System.out.println( count + " entries removed in " + time + " seconds, avg per entry is " + time / 100 );
    	    	start = System.currentTimeMillis();
    		}
    	}
    	System.out.println( "Average remove time is " + timeSum / NUM_ENTRIES + " seconds");

    	// ensure everything was removed
    	scan = _service.readAnnotations( _start, _stop, 0 );
    	print( scan );
    	Assert.assertTrue( scan.size() == 0 );
    }
    
    
    //@Test
    public void testBatchService() {

    	long start, end;
    	double time;

    	// write all annotations
    	System.out.println("Writing " + NUM_ENTRIES + " annotations");
    	start = System.currentTimeMillis();
    	_service.writeAnnotations( _annotations );
    	end = System.currentTimeMillis();
		time = ((end-start)/1000.0);
		System.out.println( NUM_ENTRIES + " entries written in " + time + " seconds, avg per entry is " + time / NUM_ENTRIES);
		
    	// scan all
    	System.out.println("Scanning " + NUM_ENTRIES + " annotations");
    	start = System.currentTimeMillis();
    	List<AnnotationBin<T>> scan = _service.readAnnotations( _start, _stop, 0 );
    	end = System.currentTimeMillis();
		time = ((end-start)/1000.0);
		System.out.println( scan.size() + " entries scanned in " + time + " seconds");
		
		//print( scan );
		
		// remove annotations
		System.out.println("Removing " + NUM_ENTRIES + " annotations");
    	start = System.currentTimeMillis();
    	_service.removeAnnotations( _annotations );
    	end = System.currentTimeMillis();
		time = ((end-start)/1000.0);
		System.out.println( NUM_ENTRIES + " entries written in " + time + " seconds, avg per entry is " + time / NUM_ENTRIES);

    	// ensure everything was removed
    	scan = _service.readAnnotations( _start, _stop, 0 );
    	print( scan );
    	Assert.assertTrue( scan.size() == 0 );
    	
    }

    @Test
    public void testMixedService() {

    	long start, end;
    	double time;
    	double timeSum = 0;
    	
    	// write all annotations
    	System.out.println("Writing " + NUM_ENTRIES + " annotations");
    	start = System.currentTimeMillis();
    	for(int i=0; i<NUM_ENTRIES; i+=BLOCK_SIZE) {
    		List<T> block = _annotations.subList(i, i+BLOCK_SIZE);
    		_service.writeAnnotations( block );
			end = System.currentTimeMillis();
			time = ((end-start)/1000.0);
			timeSum += time;
			System.out.println( "Entries from " + i + " to " + (i+BLOCK_SIZE-1) + " written in " + time + " seconds, avg per entry is " + time / BLOCK_SIZE);
			start = System.currentTimeMillis();

    	}
    	System.out.println( "Average write time is " + timeSum / NUM_ENTRIES + " seconds");

    	// scan all
    	System.out.println("Scanning " + NUM_ENTRIES + " annotations");
    	start = System.currentTimeMillis();
    	List<AnnotationBin<T>> scan = _service.readAnnotations( _start, _stop, 0 );
    	end = System.currentTimeMillis();
		time = ((end-start)/1000.0);
		System.out.println( scan.size() + " entries scanned in " + time + " seconds");
		
    	// remove annotations
    	System.out.println("Removing " + NUM_ENTRIES + " annotations");
    	timeSum = 0;
    	start = System.currentTimeMillis();
    	for(int i=0; i<NUM_ENTRIES; i+=BLOCK_SIZE) {
    		List<T> block = _annotations.subList(i, i+BLOCK_SIZE);
    		_service.removeAnnotations( block );
			end = System.currentTimeMillis();
			time = ((end-start)/1000.0);
			timeSum += time;
			System.out.println( "Entries from " + i + " to " + (i+BLOCK_SIZE-1) + " removed in " + time + " seconds, avg per entry is " + time / BLOCK_SIZE );
	    	start = System.currentTimeMillis();
    	}
    	
    	System.out.println( "Average remove time is " + timeSum / NUM_ENTRIES + " seconds");

    	// ensure everything was removed
    	scan = _service.readAnnotations( _start, _stop, 0 );
    	print( scan );
    	Assert.assertTrue( scan.size() == 0 );
    }

	
}
