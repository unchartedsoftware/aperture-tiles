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
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.oculusinfo.binning.*;

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

    private final TileIndex _index;
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
    
    
    public TileIndex getIndex() {
    	return _index;
    }
     
    
    public synchronized int size() {
    	return _bins.size();
    }
    
    
    public synchronized void add( BinIndex binIndex, AnnotationData data ) {
    	
		if ( _bins.containsKey( binIndex ) ) {
    		_bins.get( binIndex ).add( data );
    	} else {
    		_bins.put( binIndex, new AnnotationBin( binIndex, data ) );
    	}	   	
    }
    
    
    public synchronized boolean remove( BinIndex binIndex, AnnotationData data ) { 
    	
		AnnotationBin bin = _bins.get( binIndex );		
		if ( bin.remove( data ) ) {
			// remove bin if empty
			if ( bin.size() == 0 ) {    				   				
				_bins.remove( binIndex );
			}
			return true;
		}
		return false;
    }

    
    public synchronized List<Long> getAllReferences() {
    	
    	List<Long> allReferences = new LinkedList<>();  
    	// for each bin
		for ( AnnotationBin bin : _bins.values() ) {
			// get all references
			allReferences.addAll( bin.getAllReferences() );
		} 	
    	return allReferences;
    }
       
    
    public synchronized List<Long> getFilteredReferences( Map<String, Integer> filter ) {
    	
    	List<Long> filtered = new LinkedList<>();
    	// for each bin
    	for ( AnnotationBin bin : _bins.values() ) {
    		
			// go through filter list get references by priority and by count
			for (Map.Entry<String, Integer> f : filter.entrySet() ) {
				
				String priority = f.getKey();
				Integer count = f.getValue();
				
				List<Long> references = bin.getReferences( priority );
				filtered.addAll( references.subList( 0, count < references.size() ? count : references.size() ) );
			}
    	}
		return filtered;
    }    
    
    
    public JSONObject toJSON() {
    	
    	JSONObject tileJSON = new JSONObject();
		
		try {
			
			tileJSON.put("level", _index.getLevel() );
			tileJSON.put("x", _index.getX() );
			tileJSON.put("y", _index.getY() );
			
			// for each bin
			for (Map.Entry<BinIndex, AnnotationBin> binEntry : _bins.entrySet() ) {
								
				BinIndex key = binEntry.getKey();
				AnnotationBin bin = binEntry.getValue();

			    // add bin object to tile
			    tileJSON.put( key.toString(), bin.toJSON() );
			}

		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		return tileJSON;
    	
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
