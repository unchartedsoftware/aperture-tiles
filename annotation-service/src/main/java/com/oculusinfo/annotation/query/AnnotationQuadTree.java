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
    private static final int DEFAULT_QUERY_DEPTH = 8;
    
    private AnnotationBB _bb;
    private AnnotationQuadTree _ne;
    private AnnotationQuadTree _se;
    private AnnotationQuadTree _sw;
    private AnnotationQuadTree _nw;
    
  
    public AnnotationQuadTree ( AnnotationBB bb ) {
    	_bb = bb;
    }
    
    
    public List<AnnotationIndex> getIndexRanges( AnnotationBB bb ) {
    	return getIndexRanges( bb, DEFAULT_QUERY_DEPTH );
    }

    
    public List<AnnotationIndex> getIndexRanges( AnnotationBB queryBB, int queryDepth ) {
    	
    	List<AnnotationIndex> results = getIndexRangesRecursive( queryBB, queryDepth );
    	int i = 1;    	
    	while(i < results.size()-1 ) {
    		// check for contiguous ranges, concatenate them
    		if ( results.get(i).getIndex()+1 == results.get(i+1).getIndex() ) {
    			// contiguous range found, remove both interior range caps
    			results.remove(i);
    			results.remove(i);
    		} else {
    			// gap, go on to next range
    			i+=2;
    		}
    	}
    	
    	return results;
    }
    
    public List<AnnotationIndex> getIndexRangesRecursive( AnnotationBB queryBB, int queryDepth ) {
    	
    	// if at max depth, return range
    	if ( queryDepth == 0 )
    		return _bb.getRange();

    	// if it doesn't intersect this node, return empty list
    	if ( !_bb.intersects( queryBB ) )
    		return new ArrayList<AnnotationIndex>();
    	
    	// if node is completely contained don't recurse further into children
    	if ( queryBB.contains( _bb ) )
    		return _bb.getRange();
    	    	
    	// create children nodes if they don't exist
    	if ( _ne == null ) _ne = new AnnotationQuadTree( _bb.getNE() );
    	if ( _se == null ) _se = new AnnotationQuadTree( _bb.getSE() );
    	if ( _sw == null ) _sw = new AnnotationQuadTree( _bb.getSW() );
    	if ( _nw == null ) _nw = new AnnotationQuadTree( _bb.getNW() );
    	
    	// get range list
    	List<AnnotationIndex> list = new ArrayList<AnnotationIndex>();
    	
    	// check each child node
    	list.addAll( _sw.getIndexRangesRecursive( queryBB, queryDepth-1 ) );
    	list.addAll( _se.getIndexRangesRecursive( queryBB, queryDepth-1 ) );
    	list.addAll( _nw.getIndexRangesRecursive( queryBB, queryDepth-1 ) );
    	list.addAll( _ne.getIndexRangesRecursive( queryBB, queryDepth-1 ) );

    	return list;
    }
    
}

