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

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.*;
import com.oculusinfo.annotation.rest.*;


public abstract class GenericServiceTests<T> extends AnnotationTestsBase {
	
	protected static final int BLOCK_SIZE = 250;

	protected AnnotationService    _service;	
	protected List<AnnotationData<?>> _annotations;

	
	private Map<BinIndex, List<AnnotationData<?>>> readAll() {
		
		TileIndex tile = new TileIndex( 0, 0, 0 );
		 
		// scan all
    	System.out.println("Reading ALL annotations");
    	long start = System.currentTimeMillis();
    	Map<BinIndex, List<AnnotationData<?>>> scan = _service.readAnnotations( TEST_LAYER_NAME, tile );
    	long end = System.currentTimeMillis();
    	double time = ((end-start)/1000.0);
		System.out.println( "\t" + scan.size() + " entries scanned in " + time + " seconds");
		return scan;

	}

	
    @Test
    public void testService() {
    	
    	// write all annotations
    	int count = 0;
    	long start, end;
    	double time;
    	double timeSum = 0;
    	
    	int INDIVIDUAL_NUM_ENTRIES = NUM_ENTRIES;
    	int BATCH_SIZE = INDIVIDUAL_NUM_ENTRIES / 10;
    	
    	List<AnnotationData<?>> annotations = generateJSONAnnotations( NUM_ENTRIES );
    	annotations = annotations.subList(0, INDIVIDUAL_NUM_ENTRIES);
    	
    	System.out.println("Writing " + INDIVIDUAL_NUM_ENTRIES + " annotations");
    	start = System.currentTimeMillis();
    	for (AnnotationData<?> annotation : annotations ) {
    		_service.writeAnnotation( TEST_LAYER_NAME, annotation );
    		count++;
    		if (count % BATCH_SIZE == 0) {
    			end = System.currentTimeMillis();
    			time = ((end-start)/1000.0);
    			timeSum += time;
    			System.out.println( "\tEntries from " + (count-BATCH_SIZE) + " to " + (count-1) + " written in " + time + " seconds, avg per entry is " + time / BATCH_SIZE);
    			start = System.currentTimeMillis();
    		}
    	}
    	System.out.println( "Average write time is " + timeSum / INDIVIDUAL_NUM_ENTRIES + " seconds");

    	// scan all
    	readAll();		
		
    	// remove annotations
    	System.out.println("Removing " + INDIVIDUAL_NUM_ENTRIES + " annotations");
    	count = 0;
    	timeSum = 0;
    	start = System.currentTimeMillis();
    	for (AnnotationData<?> annotation : annotations ) {
    		_service.removeAnnotation( TEST_LAYER_NAME, annotation );
    		count++;
    		if (count % BATCH_SIZE == 0) {
    			end = System.currentTimeMillis();
    			time = ((end-start)/1000.0);
    			timeSum += time;
    			System.out.println( "\t" + count + " entries removed in " + time + " seconds, avg per entry is " + time / BATCH_SIZE );
    	    	start = System.currentTimeMillis();
    		}
    	}
    	System.out.println( "Average remove time is " + timeSum / INDIVIDUAL_NUM_ENTRIES + " seconds");

    	// ensure everything was removed
    	Map<BinIndex, List<AnnotationData<?>>> scan = readAll();
    	printData( scan );
    	Assert.assertTrue( scan.size() == 0 );
    }
    
   
	
}
