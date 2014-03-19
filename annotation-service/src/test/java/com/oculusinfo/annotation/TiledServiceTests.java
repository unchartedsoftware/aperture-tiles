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
import com.oculusinfo.annotation.rest.impl.*;


public class TiledServiceTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;

	private TiledAnnotationService _service;
	
	private List<JSONObject> _annotations;
	private double[] _start = new double[2];
	private double[] _stop = new double[2];
	
    @Before
    public void setup () {
    	
    	_service = new TiledAnnotationService();

    	_annotations = generateJSONs( NUM_ENTRIES );
    	
		final Random rand = new Random();
		
		double x0 = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y0 = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
		
		double x1 = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y1 = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));

		_start[0] = Math.min( x0, x1 );
		_start[1] = Math.min( y0, y1 );	
		
		_stop[0] = Math.max( x0, x1 );
		_stop[1] = Math.max( y0, y1 );		
    }

    @After
    public void teardown () {
    	_service = null;
    }
	
	
    @Test
    public void testService() {
    	
    	// write all annotations
    	System.out.println("Writing " + NUM_ENTRIES + " annotations");
    	for (JSONObject annotation : _annotations ) {
    		_service.writeAnnotation( annotation );
    	}
    	
    	// scan all
    	System.out.println("Scanning " + NUM_ENTRIES + " annotations");
    	List<AnnotationBin<JSONObject>> scan = _service.readAnnotations( BOUNDS[0], BOUNDS[1],
				 														 BOUNDS[2],  BOUNDS[3], 0 );


    	// remove annotations
    	System.out.println("Removing " + NUM_ENTRIES + " annotations");
    	for (JSONObject annotation : _annotations ) {
    		_service.removeAnnotation( annotation );
    	}

    	// ensure everything was removed
    	scan = _service.readAnnotations( BOUNDS[0], BOUNDS[1],
				 						 BOUNDS[2],  BOUNDS[3], 0 );
    	Assert.assertTrue( scan.size() == 0 );
    	
    }


	
}
