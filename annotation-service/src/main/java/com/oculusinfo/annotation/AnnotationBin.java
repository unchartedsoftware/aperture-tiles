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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

import com.oculusinfo.binning.*;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class represents an aggregation of annotations in a single bin. All annotations
 * are stored by priority.
 * 
 * Annotation Bin JSON format:
 * {
 * 		"priorityName0" : [ AnnotationReference, AnnotationReference, ... ]
 * 		"priorityName1" : [ AnnotationReference, AnnotationReference, ... ]
 * }
 */

public class AnnotationBin implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private final BinIndex _index;
    private Map<String, List<AnnotationReference>> _references = new LinkedHashMap<>();
      
    public AnnotationBin( BinIndex index ) {
    	_index = index;
    }
    public AnnotationBin( BinIndex index, Map<String, List<AnnotationReference>> references ) {
    	_index = index;
    	_references = references;
    }
    public AnnotationBin( BinIndex index, AnnotationData<?> data ) {
    	_index = index;
    	add( data );
    }
    
    
    public BinIndex getIndex() {
    	return _index;
    }
    
    
    public synchronized int size() {
    	return _references.size();
    }
      
    
    public synchronized void add( AnnotationData<?> data ) {
    	
    	String priority = data.getPriority();
    	AnnotationReference reference =  data.getReference();   	
    	List<AnnotationReference> entries;
    	
    	if ( _references.containsKey( priority ) ) {    		
    		entries = _references.get( priority );   		
    		// only add if reference does not already exist
    		if ( !entries.contains( reference ) ) {
    			entries.add( reference );
    		}   		    		
    	} else {
    		entries = new LinkedList<>();
    		entries.add( reference );
    		_references.put( priority, entries );
    	}
    	
    	// sort references after insertion... maybe instead use a SortedSet?
    	Collections.sort( entries );
    }
      
    
    public synchronized boolean remove( AnnotationData<?> data ) { 
    	
    	String priority = data.getPriority();
    	AnnotationReference reference = data.getReference();
    	boolean removedAny = false;
    	
    	if ( _references.containsKey( priority ) ) {
    		
    		List<AnnotationReference> entries = _references.get( priority );
    		
    		if ( entries.contains( reference ) ) {
    			entries.remove( reference );
    			removedAny = true;
    		} 
    		   		
    		if ( entries.size() == 0 ) {
	    		// remove references for priority
    			_references.remove( priority );
	    	}
    	} 

    	return removedAny;
    }

    
    public synchronized List<AnnotationReference> getReferences( String priority ) {
    	
    	if ( _references.containsKey( priority ) ) {
    		return _references.get( priority );
    	} else {
    		return new LinkedList<>();
    	}
    	
    }
    
    
    public synchronized List<AnnotationReference> getAllReferences() {
    	
    	List<AnnotationReference> allReferences = new LinkedList<>();  
    	// for each priority group in a bin
		for ( List<AnnotationReference> references : _references.values() ) {
			allReferences.addAll( references );
		}	
    	return allReferences;
    }    
    
    
    public static AnnotationBin fromJSON( JSONObject json ) throws IllegalArgumentException {
    	
    	try {
			
    		BinIndex index = new BinIndex( json.getInt("x"), json.getInt("y") );
    		
    		Map<String, List<AnnotationReference>> references =  new LinkedHashMap<>();
        	
        	Iterator<?> priorities = json.keys();
            while( priorities.hasNext() ){
            	
                String priority = (String)priorities.next();
                
                if( json.get(priority) instanceof JSONArray ) {
                
	            	JSONArray jsonReferences = json.getJSONArray(priority);
	            	
	            	List<AnnotationReference> referenceList = new LinkedList<>();	            	
	            	for (int i=0; i<jsonReferences.length(); i++) {
	            		
	            		JSONObject jsonRef = jsonReferences.getJSONObject( i );
	            		referenceList.add( AnnotationReference.fromJSON( jsonRef ) );            		
	            	}           	
	            	references.put( priority, referenceList );	
                }
                
            }	        
    	    return new AnnotationBin( index, references );
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e );
		}

    }
    
    
    public JSONObject toJSON() {
    	
    	JSONObject binJSON = new JSONObject();
    	try {
			   	
    		binJSON.put("x", _index.getX() );
    		binJSON.put("y", _index.getY() );
    		
	    	// for each priority group in a bin
		    for (Map.Entry<String, List<AnnotationReference>> referenceEntry : _references.entrySet() ) {		    	
		    	
		    	String priority = referenceEntry.getKey();
		    	List<AnnotationReference> references = referenceEntry.getValue();
		    	
		    	JSONArray referenceJSON = new JSONArray();
		    	for ( AnnotationReference reference : references ) {
		    		referenceJSON.put( reference.toJSON() );
		    	}
		    	
		    	// add priority to bin json object
		    	binJSON.put( priority, referenceJSON );		    	
		    }
		    
    	} catch ( Exception e ) {
    		e.printStackTrace();
    	}
    	
    	return binJSON;
    }
    
     
    @Override
    public int hashCode () {
    	return _index.hashCode();
    }
    
    
    @Override
    public boolean equals (Object that) {   	    	
    	if (that != null)
    	{
    		if (that instanceof AnnotationBin) {
    			return _index.equals( ((AnnotationBin)that)._index );
    		} else if (that instanceof BinIndex) {
    			return _index.equals( (BinIndex)that );
    		}    		
    	}	
    	return false;
    }
    
}
