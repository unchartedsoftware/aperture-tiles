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

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class AnnotationBB implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private long _centreX;
    private long _centreY;
    private long _width;
    private long _height;
    private long _start = -1;
    private long _stop = -1;
    private double[] _bounds;
    
    public AnnotationBB ( double[] bounds ) {
    	_centreX = AnnotationIndex.transformToUnit( (bounds[0]+bounds[2])/2, bounds[0], bounds[2] );
    	_centreY = AnnotationIndex.transformToUnit( (bounds[1]+bounds[3])/2, bounds[1], bounds[3] ); 	
    	_width = AnnotationIndex.MAX_UNIT;
    	_height = AnnotationIndex.MAX_UNIT;	
    	_start = AnnotationIndex.getIndex( _centreX-_width/2,
    								       _centreY-_height/2 );    	
    	_stop = AnnotationIndex.getIndex( _centreX+_width/2,
			       						   _centreY+_height/2 );
    	_bounds = bounds;
    }
    
    private AnnotationBB ( long x, long y, long width, long height, long start, long stop, double [] bounds ) {
    	_centreX = x;
    	_centreY = y;
    	_width = width;
    	_height = height;    	
    	_start = start;   	
    	_stop = stop;
    	_bounds = bounds;
    }

    public boolean contains( double x, double y ) {   	
    	return x >= _centreX - (_width/2) && x <= _centreX + (_width/2) &&
    		   y >= _centreY - (_height/2) && y <= _centreY + (_height/2);
    }
    
    public boolean contains( AnnotationBB box ) {
    	
    	return box._centreX + box._width/2 < _centreX + _width/2 &&
    		   box._centreX - box._width/2 > _centreX - _width/2 &&
    		   box._centreY + box._height/2 < _centreY + _height/2 &&
    		   box._centreY - box._height/2 > _centreY - _height/2;
    }
    
    public boolean intersects( AnnotationBB box ) {
    	return (Math.abs(_centreX - box._centreX) * 2 < (_width + box._width)) &&
    		   (Math.abs(_centreY - box._centreY) * 2 < (_height + box._height));
    }
    
    public List<AnnotationIndex> getRange() {
    	
    	List<AnnotationIndex> range = new ArrayList<AnnotationIndex>();
    	
    	// start range
    	range.add( new AnnotationIndex( _start,
    								    _bounds ) );
    	// stop range
    	range.add( new AnnotationIndex( _stop,
									    _bounds ) );  	
    	
    	System.out.println( "ll: " + range.get(0).getX() + ", " + range.get(0).getY() + 
    						" ur: " + range.get(1).getX() + ", " + range.get(1).getY() );
    	
    	return range;
    }

    private long floorInc() {
    	return (long)Math.floor( (_stop-_start)/4 );
    }
    
    private long ceilInc() {
    	return (long)Math.ceil( (_stop-_start)/4 );
    }
    
    public AnnotationBB getNE() {
    	return new AnnotationBB( _centreX+_width/4, 
						 	     _centreY+_height/4, 
						  	     _width/2, _height/2,
						  	     ceilInc()*3, ceilInc()*3+floorInc(),
						  	     _bounds);
    }
    
    public AnnotationBB getSE() {
    	return new AnnotationBB( _centreX+_width/4, 
						 	     _centreY-_height/4, 
						  	     _width/2, _height/2,
						  	     ceilInc(), ceilInc() + floorInc(),
						  	     _bounds);
    }
    
    public AnnotationBB getSW() {
    	return new AnnotationBB( _centreX-_width/4, 
						 	     _centreY-_height/4, 
						  	     _width/2, _height/2,
						  	     _start, floorInc(),
						  	     _bounds);
    }
    
    public AnnotationBB getNW() {
    	return new AnnotationBB( _centreX-_width/4, 
						 	     _centreY+_height/4, 
						  	     _width/2, _height/2,
						  	     ceilInc()*2, ceilInc()*2+floorInc(),
						  	     _bounds);
    }
    
}