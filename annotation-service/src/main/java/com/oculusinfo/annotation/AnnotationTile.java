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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.oculusinfo.annotation.index.*;
import com.oculusinfo.binning.*;

import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Annotation Tile
 * {
 * 		binKey0: AnnotationBin0, 
 * 		binKey1: AnnotationBin1,
 * 		binKey4: AnnotationBin4
 * }
 */

public class AnnotationTile implements Serializable {
	
    public static final int NUM_BINS = 8;
	
	private static final long serialVersionUID = 1L;

    private TileIndex  	_index;
    private Map<BinIndex, AnnotationBin> _bins = new LinkedHashMap<>();
     
    public AnnotationTile( TileIndex index ) {   
    	_index = index;
    }
    
    public AnnotationTile( TileIndex index, AnnotationBin bin ) {   
    	_index = index;
    	_bins.put( bin.getIndex(), bin );
    }
    
    public AnnotationTile( TileIndex index, Map<BinIndex, AnnotationBin> bins ) {   
    	_index = index;
    	_bins = bins;
    }

    public void add( BinIndex bin, AnnotationData data ) {
    	
    	if ( _bins.containsKey( bin ) ) {
    		_bins.get( bin ).add( data );
    	} else {
    		_bins.put( bin, new AnnotationBin( bin, data ) );
    	}
    	
    }
    
    public boolean remove( AnnotationData data ) { 
    	
    	boolean removedAny = false;
    	Iterator<Map.Entry<BinIndex, AnnotationBin>> iter = _bins.entrySet().iterator();	
    	while ( iter.hasNext() ) {
    		
    	    Map.Entry<BinIndex, AnnotationBin> entry = iter.next();   	    
    	    AnnotationBin bin = entry.getValue();
    	    
    	    String priority = data.getPriority();
    	    Long index = data.getIndex();
    	    Map<String, List<Long>> binReferences = bin.getReferences();
    	    
    	    // check if bin contains this priority group
    	    if ( binReferences.containsKey( priority ) ) {
    	    	
    	    	List<Long> references = binReferences.get( priority );
    	    	
    	    	// data found in bin references
    	    	if ( references.contains( index ) ) {
    	    		// remove index from references
    	    		references.remove( index );
    	    		removedAny = true;
    	    	}
    	    	
    	    	if ( references.size() == 0 ) {
    	    		// remove references for priority
    	    		//System.out.println( "No more references for priority, remove priority ");
    	    		binReferences.remove( priority );
    	    	}
    	    	
    	    	if ( binReferences.size() == 0 ) {
    	    		// remove bin
    	    		iter.remove();
    	    	}
    	    }
    	}    	

    	return removedAny;
    }

        
    public TileIndex getIndex() {
    	return _index;
    }
    
    
    public Map<BinIndex, AnnotationBin> getBins() {
    	return _bins;
    } 

    
    public List<Long> getAllReferences() {
    	
    	List<Long> references = new LinkedList<>();    	
    	// for each bin
    	for ( AnnotationBin bin : _bins.values() ) {  		
    		// for each priority group in a bin
		    for (Map.Entry<String, List<Long>> referenceEntry : bin.getReferences().entrySet() ) {
		    	
		    	references.addAll( referenceEntry.getValue() );
		    }
    	}   	
    	return references;
    }
    
    
    public List<Long> getFilteredReferences( Map<String, Integer> filter ) {
    	
    	List<Long> filtered = new LinkedList<>();
    	// for each bin
    	for ( AnnotationBin bin : _bins.values() ) { 
    		
			// go through filter list get references by priority and by count
			for (Map.Entry<String, Integer> f : filter.entrySet() ) {
				
				String priority = f.getKey();
				Integer count = f.getValue();
				
				if ( bin.getReferences().containsKey( priority ) ) {
					filtered.addAll( bin.getReferences().get( priority ).subList( 0, count ) );
				}
			}
    	}
		return filtered;
    }
    
    
    @Override
    public int hashCode () {
    	return _index.hashCode();
    }

    @Override
    public boolean equals (Object that) {   	    	
    	if (that != null)
    	{
    		if (that instanceof AnnotationTile) {
    			return _index.equals( ((AnnotationTile)that)._index );
    		} 		
    	}	
    	return false;
    }
    
}
