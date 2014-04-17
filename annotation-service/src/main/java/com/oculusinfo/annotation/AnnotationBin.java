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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.oculusinfo.binning.*;

import org.json.JSONArray;
import org.json.JSONObject;

/*
 * Annotation Bin:
 * {
 * 		priorityName0 : [dataIndex0, dataIndex1, ... ]
 * 		priorityName1 : [dataIndex2, dataIndex3, ... ]
 * }
 */

public class AnnotationBin implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private final BinIndex _index;
    private Map<String, List<UUID>> _references = new LinkedHashMap<>();
      
    public AnnotationBin( BinIndex index ) {
    	_index = index;
    }
    public AnnotationBin( BinIndex index, Map<String, List<UUID>> references ) {
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
    	
    	String priority = data.getPriority().toLowerCase();
    	UUID uuid =  data.getUUID();   	
    	
    	if ( _references.containsKey( priority ) ) {    		
    		List<UUID> entries = _references.get( priority );   		
    		// only add if reference does not already exist
    		if ( !entries.contains( uuid ) ) {
    			entries.add( uuid );
    		}   		    		
    	} else {
    		List<UUID> entries = new LinkedList<>();
    		entries.add( uuid );
    		_references.put( priority, entries );
    	}    	
    }
      
    
    public synchronized boolean remove( AnnotationData<?> data ) { 
    	
    	String priority = data.getPriority().toLowerCase();
    	UUID uuid = data.getUUID();
    	boolean removedAny = false;
    	
    	if ( _references.containsKey( priority ) ) {
    		
    		List<UUID> entries = _references.get( priority );
    		
    		if ( entries.contains( uuid ) ) {
    			entries.remove( uuid );
    			removedAny = true;
    		} 
    		   		
    		if ( entries.size() == 0 ) {
	    		// remove references for priority
    			_references.remove( priority );
	    	}
    	} 

    	return removedAny;
    }

    
    public synchronized List<UUID> getReferences( String priority ) {
    	
    	String lcPriority = priority.toLowerCase();
    	if ( _references.containsKey( lcPriority ) ) {
    		return _references.get( lcPriority );
    	} else {
    		return new LinkedList<>();
    	}
    	
    }
    
    
    public synchronized List<UUID> getAllReferences() {
    	
    	List<UUID> allReferences = new LinkedList<>();  
    	// for each priority group in a bin
		for ( List<UUID> references : _references.values() ) {
			allReferences.addAll( references );
		}	
    	return allReferences;
    }    
    
    
    public JSONObject toJSON() {
    	
    	JSONObject binJSON = new JSONObject();
    	try {
			   	
	    	// for each priority group in a bin
		    for (Map.Entry<String, List<UUID>> referenceEntry : _references.entrySet() ) {
		    	
		    	String priority = referenceEntry.getKey().toLowerCase();
		    	List<UUID> references = referenceEntry.getValue();
		    	
		    	JSONArray referenceJSON = new JSONArray();
		    	for ( UUID reference : references ) {
		    		referenceJSON.put( reference );
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
