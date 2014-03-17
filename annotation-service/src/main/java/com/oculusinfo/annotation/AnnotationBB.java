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

/*
 * A bounding box class in annotation index space
 * 
 */
public class AnnotationBB implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private long _x;	// lower left corner
    private long _y;
    private long _width;
    private long _height;
    private long _start = -1;
    private long _stop = -1;
    private double[] _bounds;
    
    
    public AnnotationBB ( double[] bounds ) {
    	
    	_x = AnnotationIndex.transformToUnit( bounds[0], bounds[0], bounds[2] );
    	_y = AnnotationIndex.transformToUnit( bounds[1], bounds[1], bounds[3] ); 	
    	_width = AnnotationIndex.MAX_UNIT+1;
    	_height = AnnotationIndex.MAX_UNIT+1;	
    	_start = AnnotationIndex.getIndex( _x, _y );
    	_stop = AnnotationIndex.getIndex( _x+_width-1, _y+_height-1 );
    	_bounds = bounds;
    }
    
    
    public AnnotationBB ( double[] extents, double[] bounds ) {
    	
    	_x = AnnotationIndex.transformToUnit( extents[0], bounds[0], bounds[2] );
    	_y = AnnotationIndex.transformToUnit( extents[1], bounds[1], bounds[3] );    	
    	long mx = AnnotationIndex.transformToUnit( extents[2], bounds[0], bounds[2] );
    	long my = AnnotationIndex.transformToUnit( extents[3], bounds[1], bounds[3] );   	   	
    	_width = mx - _x;
    	_height = my - _y;	
    	_start = AnnotationIndex.getIndex( _x, _y );
    	_stop = AnnotationIndex.getIndex( _x+_width-1, _y+_height-1 );
    	_bounds = bounds;
    }
    
    
    private AnnotationBB ( long x, long y, long width, long height, long start, long stop, double [] bounds ) {
    	_x = x;
    	_y = y;
    	_width = width;
    	_height = height;    	
    	_start = start;   	
    	_stop = stop;
    	_bounds = bounds;
    }

    /*
    public boolean contains( long x, long y ) {   	
    	return x > _x && x < _x + _width &&
    		   y > _y && y < _y + _height;
    }
    */
    
    public boolean contains( AnnotationIndex index ) {   
    	long x = AnnotationIndex.transformToUnit( index.getX(), 
    											  index.getBounds()[0], 
    											  index.getBounds()[2] );
    	long y = AnnotationIndex.transformToUnit( index.getY(), 
    											  index.getBounds()[1], 
    											  index.getBounds()[3] );
    	return x > _x && x < _x + _width &&
     		   y > _y && y < _y + _height;
    }
    
    public boolean contains( AnnotationBB box ) {
    	
    	return box._x + box._width < _x + _width &&
    		   box._x > _x &&
    		   box._y + box._height < _y + _height &&
    		   box._y > _y;
    }
    
    public boolean intersects( AnnotationBB box ) {
    	return !( _x+_width < box._x ||
    			  _y+_height < box._y ||
    			  _x > box._x+box._width ||
    			  _y > box._y+box._height );
    }
    
    public List<AnnotationIndex> getRange() {
    	
    	List<AnnotationIndex> range = new ArrayList<AnnotationIndex>();
    	
    	// start range
    	range.add( new AnnotationIndex( _start,
    								    _bounds ) );
    	// stop range
    	range.add( new AnnotationIndex( _stop,
									    _bounds ) );  	
    	return range;
    }

    private long floorInc() {    	
    	return (_width * _height)/4 - 1;
    }
    
    private long ceilInc() {
    	return (_width * _height)/4;
    }
    
    public AnnotationBB getNE() {
    	return new AnnotationBB( _x+(_width/2), 
						 	     _y+(_height/2), 
						  	     _width/2, _height/2,
						  	     _start + ceilInc()*3,
						  	     _start + ceilInc()*3+floorInc(),
						  	     _bounds);
    }
    
    public AnnotationBB getSE() {
    	return new AnnotationBB( _x+(_width/2), 
						 	     _y, 
						  	     _width/2, _height/2,
						  	     _start + ceilInc(), 
						  	     _start + ceilInc() + floorInc(),
						  	     _bounds);
    }
    
    public AnnotationBB getSW() {
    	return new AnnotationBB( _x, 
						 	     _y, 
						  	     _width/2, _height/2,
						  	     _start, 
						  	     _start + floorInc(),
						  	     _bounds);
    }
    
    public AnnotationBB getNW() {
    	return new AnnotationBB( _x, 
    							 _y+(_height/2), 
						  	     _width/2, _height/2,
						  	     _start + ceilInc()*2, 
						  	     _start + ceilInc()*2+floorInc(),
						  	     _bounds);
    }
    
}