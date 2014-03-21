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


public class TiledServiceTests extends GenericServiceTests<JSONObject> {
	
	static final boolean VERBOSE = true;
	
	public void setMaxStartStop( int level ) {
		_start = new JSONObject();
		_stop = new JSONObject();
		
		int numTiles = (int)Math.pow(2, level);		
		int maxTiles = ( numTiles > 5 )? 5 : numTiles;

		// range covered per tile
		double xRange = ( BOUNDS[2] - BOUNDS[0] )/numTiles;
		double yRange = ( BOUNDS[3] - BOUNDS[1] )/numTiles;
		
		try {
			_start.put("x", BOUNDS[0] );
			_start.put("y", BOUNDS[1] );
			
			_stop.put("x",  BOUNDS[0] + xRange * maxTiles );
			_stop.put("y",  BOUNDS[1] + yRange * maxTiles );
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	
	public void setRandomStartStop() {
		
		final Random rand = new Random();
		
		double x0 = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y0 = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
		double x1 = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y1 = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
		
		double xMax = Math.max( x0, x1);
		double xMin = Math.min( x0, x1 );
		double yMax = Math.max( y0, y1 );		
		double yMin = Math.min( y0, y1 );	
		
		_start = new JSONObject();
		_stop = new JSONObject();
		
		try {
			_start.put("x", xMin );
			_start.put("y", yMin );
			
			_stop.put("x", xMax );
			_stop.put("y", yMax );
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
    @Before
    public void setup () {
    	
    	_service = new TiledAnnotationService();
    	_annotations = generateJSONs( NUM_ENTRIES );

    }

    @After
    public void teardown () {
    	_service = null;
    }
	
}
