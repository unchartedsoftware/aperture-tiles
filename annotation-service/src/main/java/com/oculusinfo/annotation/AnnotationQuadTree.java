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


public class AnnotationQuadTree implements Serializable {
	
    private static final long serialVersionUID = 1L;
    private static final int MAX_DEPTH = 8;
    
    private AnnotationBB _bb;
    private AnnotationQuadTree _ne;
    private AnnotationQuadTree _se;
    private AnnotationQuadTree _sw;
    private AnnotationQuadTree _nw;
    private int _depth;
    
  
    public AnnotationQuadTree ( AnnotationBB bb ) {
    	_bb = bb;
    	_depth = 0;
    }
    
    
    public AnnotationQuadTree ( AnnotationBB bb, int depth ) {
    	_bb = bb;
    	_depth = depth;
    }

    
    public List<AnnotationIndex> getIndexRanges( AnnotationBB bb ) {
    	
    	// if it doesn't intersect this node, return empty list
    	if (!_bb.intersects( bb ) || _depth == MAX_DEPTH)
    		return new ArrayList<AnnotationIndex>();
    	
    	// if node is completely contained don't recurse further into children
    	if (bb.contains( _bb ))
    		return _bb.getRange();
    	    	
    	// create children nodes
    	_ne = new AnnotationQuadTree( _bb.getNE(), _depth+1 );
    	_se = new AnnotationQuadTree( _bb.getSE(), _depth+1 );
    	_sw = new AnnotationQuadTree( _bb.getSW(), _depth+1 );
    	_nw = new AnnotationQuadTree( _bb.getNW(), _depth+1 );
    	
    	// get range list
    	List<AnnotationIndex> list = new ArrayList<AnnotationIndex>();
    	
    	// check each child node
    	list.addAll( _ne.getIndexRanges( bb ) );
    	list.addAll( _se.getIndexRanges( bb ) );
    	list.addAll( _sw.getIndexRanges( bb ) );
    	list.addAll( _nw.getIndexRanges( bb ) );

    	return list;
    }
    
}


/*
public class AnnotationQuadTree implements Serializable {
	
    private static final long serialVersionUID = 1L;
    
    private long _centreX;
    private long _centreY;
    private long _width;
    private long _height;
    
    public AnnotationQuadTree (long x, long y, long width, long height) {
    	_centreX = x;
    	_centreY = y;
    	_width = width;
    	_height = height;
    }

    public boolean contains( long x, long y ) {
    	
    	return x >= _centreX - (_width/2) && x <= _centreX + (_width/2) &&
    		   y >= _centreY - (_height/2) && y <= _centreY + (_height/2);
    }
    
    public boolean intersects( AnnotationQuadTree tree ) {
    	return (Math.abs(_centreX - tree._centreX) * 2 < (_width + tree._width)) &&
    		   (Math.abs(_centreY - tree._centreY) * 2 < (_height + tree._height));
    }
    
    public List<long[]> queryRanges( long min, long max ) {
    	
    	List<long[]> list = new ArrayList<long[]>();
    	
    	
    }
    
    
    
    
}


public class AnnotationQuadTree implements Serializable {
	
    private static final long serialVersionUID = 1L;
    
    private long _min;
    private long _max;
    private AnnotationQuadTree _ne;
    private AnnotationQuadTree _se;
    private AnnotationQuadTree _sw;
    private AnnotationQuadTree _nw;

    public AnnotationQuadTree (long min, long max) {
    	_min = min;
    	_max = max;
    }

    public long getMin() {
    	return _min;
    }
    
    public long getMax() {
    	return _max;
    }
    
    public boolean contains( long index ) {
    	return index <= _max && index >= _min;
    }
    
    public boolean intersects( AnnotationQuadTree tree ) {
    	return _max >= tree._min && tree._max >= _min;
    }
    
    public List<long[]> queryRanges( long min, long max ) {
    	
    	List<long[]> list = new ArrayList<long[]>();
    	
    	
    }
   
    @Override
    public int hashCode () {
    	return (int)( _min | _max << 1 );
    }

    @Override
    public boolean equals (Object that) {  	    	
    	if (that != null && that instanceof AnnotationQuadTree)
        {
            return _min == ((AnnotationQuadTree)that)._min && 
            	   _max == ((AnnotationQuadTree)that)._max;
        }    	
    	return false;
    }
    
}
*/
