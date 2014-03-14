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
import java.lang.Number;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oculusinfo.binning.*;
import com.oculusinfo.binning.impl.*;

public class AnnotationIndex implements Serializable {
	
    private static final long serialVersionUID = 2L;
    
    // treat bounds of annotations as 0.0 to 1.0 at level0, annotations don't have units,
    // they are to be data independent
    //private static final TilePyramid       _tilePyramid = new AOITilePyramid(0.0, 0.0, 1.0, 1.0);
    //private static final PyramidComparator _comparator  = new PyramidComparator(_tilePyramid, 8);
    
    private static final long BITS[] = { 0x5555555555555555L, 
    									 0x3333333333333333L, 
    									 0x0F0F0F0F0F0F0F0FL,
    									 0x00FF00FF00FF00FFL, 
    									 0x0000FFFF0000FFFFL,
    									 0x00000000FFFFFFFFL};

    private static final long SHIFTS[] = {1, 2, 4, 8, 16};
       
    public static final long MAX_UNIT = Integer.MAX_VALUE;
    
    private double _xCoord;
    private double _yCoord;
    private double _bounds[];
    private long   _index;

    public AnnotationIndex (double x, double y, double bounds[]) {
    	_xCoord = x;
    	_yCoord = y;
    	_bounds = bounds;
    	long lx = transformToUnit( _xCoord, _bounds[0], _bounds[2] );
    	long ly = transformToUnit( _yCoord, _bounds[1], _bounds[3] );
    	_index = getIndex( lx, ly );
    }
    
    public AnnotationIndex( long index, double bounds[] ) {
    	_index = index;
    	_bounds = bounds;
    	
    	long [] coords = getCoord( _index, _bounds );
    	
    	_xCoord = transformFromUnit( coords[0], _bounds[0], _bounds[2] );
    	_yCoord = transformFromUnit( coords[1], _bounds[1], _bounds[3] );   	
    }

    public double getX() {
    	return _xCoord;
    }
    
    public double getY() {
    	return _yCoord;
    }
    
    public long getIndex() {
    	return _index;
    }
    
    public double squaredDistanceFrom( AnnotationIndex other ) {
    	double dx = _xCoord - other.getX();
    	double dy = _yCoord - other.getY();
    	return dx + dy;
    }
    
    public final byte[] getBytes() {

        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(_index);
        byte [] bytes = buf.array();
        return bytes;
    }
    
    public double[] getBounds() {
    	return _bounds;
    }  
    
    public static double transformFromUnit( long v, double min, double max ) {   	
    	// normalize to the range [0 .. 1]
    	double nv = v/(double)MAX_UNIT;   	
    	return nv * ( max - min ) + min;
    }
    
    public static long transformToUnit( double v, double min, double max ) {
    	// normalize to the range [0 .. 1]
    	double nv = ( v - min ) / ( max - min ); 
    	// each axis will have 2^31 increments, allotting 2^62 total unique spatial indexes
    	// for a ESPG:900913 projection, this gives a resolution of 0.0093 meters
    	return (long)(MAX_UNIT*nv);
    }
    
    public static long[] getCoord( long index, double[] bounds ) {
    	
    	long x = index;
    	long y = index >> 1;
    	
    	long[] coords = new long[2];
    	
    	x = x & BITS[0];
        x = (x | (x >> SHIFTS[0])) & BITS[1];
        x = (x | (x >> SHIFTS[1])) & BITS[2];
        x = (x | (x >> SHIFTS[2])) & BITS[3];
        x = (x | (x >> SHIFTS[3])) & BITS[4];
        x = (x | (x >> SHIFTS[4])) & BITS[5];
        coords[0] = x;
        
        y = y & BITS[0];
        y = (y | (y >> SHIFTS[0])) & BITS[1];
        y = (y | (y >> SHIFTS[1])) & BITS[2];
        y = (y | (y >> SHIFTS[2])) & BITS[3];
        y = (y | (y >> SHIFTS[3])) & BITS[4];
        y = (y | (y >> SHIFTS[4])) & BITS[5];
        
        coords[1] = y;
        
        return coords;
    }
    
    public static long getIndex( long x, long y ) {
    	
    	x = (x | (x << SHIFTS[4])) & BITS[4];
        x = (x | (x << SHIFTS[3])) & BITS[3];
        x = (x | (x << SHIFTS[2])) & BITS[2];
        x = (x | (x << SHIFTS[1])) & BITS[1];
        x = (x | (x << SHIFTS[0])) & BITS[0];

        y = (y | (y << SHIFTS[4])) & BITS[4];
        y = (y | (y << SHIFTS[3])) & BITS[3];
        y = (y | (y << SHIFTS[2])) & BITS[2];
        y = (y | (y << SHIFTS[1])) & BITS[1];
        y = (y | (y << SHIFTS[0])) & BITS[0];

        return x | (y << 1);


    	/*
    	final int MAX_DEPTH = 8;
    	
    	double xMin = _bounds[0];
    	double yMin = _bounds[1];
    	double xMax = _bounds[2];
    	double yMax = _bounds[3];
    	double xRange = xMax - xMin;
    	double yRange = yMax - yMin;
    	
    	_index = 0;
    	int currentDepth = 0;
    	while (currentDepth < MAX_DEPTH) {
    		
    		final double xMid = xMin+ (xRange / 2.0);
        	final double yMid = yMin+ (yRange / 2.0);

        	_index = _index << 2;
        	
    		if (_xCoord > xMid ) {
    			xMin = xMid;
    			_index = _index | 2;
    		}
    		
    		if (_yCoord > yMid ) {
    			yMin = yMid;
    			_index = _index | 1;
    		}
    		currentDepth++;
    		xRange /= 2.0;
    		yRange /= 2.0;
    	}
    	*/

    }
    
    @Override
    public int hashCode () {
    	return (int)_index;
    }

    @Override
    public boolean equals (Object that) {  	    	
    	if (that != null && that instanceof AnnotationIndex)
        {
            return _index == ((AnnotationIndex)that)._index;
        }    	
    	return false;
    }
    
}
