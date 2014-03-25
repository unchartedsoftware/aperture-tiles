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

import com.oculusinfo.annotation.index.*;
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

    private BinIndex 			    _index;
    private Map<String, List<Long>> _references = new LinkedHashMap<>();
    
    
    public AnnotationBin( BinIndex index, Map<String, List<Long>> references ) {   
    	_index = index;
    	_references = references;
    }
    
    
    public AnnotationBin( BinIndex index, AnnotationData data ) {   
    	_index = index;
    	add( data );
    }
    

    public void add( AnnotationData data ) {
    	if ( _references.containsKey( data.getPriority() ) ) {
    		List<Long> entries = _references.get( data.getPriority() );
    		entries.add( data.getIndex() );
    	} else {
    		List<Long> entries = new LinkedList<>();
    		entries.add( data.getIndex() );
    		_references.put( data.getPriority(), entries );
    	}    	
    }
    
    
    public boolean remove( AnnotationData data ) { 
    	
    	if ( _references.containsKey( data.getPriority() ) ) {
    		List<Long> entries = _references.get( data.getPriority() );
    		if ( entries.contains( data.getIndex() ) ) {
    			entries.remove( data.getIndex() );
    			return true;
    		}   		
    	} 
    	return false;
    }
    
    
    public BinIndex getIndex() {
    	return _index;
    }
    
    
    public Map<String, List<Long>> getReferences() {
    	return _references;
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
