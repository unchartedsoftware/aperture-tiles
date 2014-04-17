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
import java.util.UUID;
import org.json.JSONObject;

/*
 * Annotation Reference
 * {
 * 		uuid:  		UUID, 
 * 		timestamp: 	Long
 * }
 */

public class AnnotationReference implements Comparable<AnnotationReference>, Serializable {
	
    private static final long serialVersionUID = 1L;

    private final UUID _uuid;
    private Long _timestamp;
    
    public AnnotationReference( UUID uuid, Long timestamp ) {
    	_uuid = uuid;
    	_timestamp = timestamp;
    }

    
    public UUID getUUID() {
    	return _uuid;
    }
    
    public Long getTimeStamp() {
    	return _timestamp;
    }
    
    
    public JSONObject toJSON() {
    	
    	JSONObject json = new JSONObject();
    	try {
			   	
    		json.put( "uuid", _uuid.toString() );
    		json.put( "timestamp", _timestamp.toString() );
		    
    	} catch ( Exception e ) {
    		e.printStackTrace();
    	}
    	
    	return json;
    }
    
    
    static public AnnotationReference fromJSON( JSONObject json ) throws IllegalArgumentException {
    	
    	try {
			
    		UUID uuid = UUID.fromString( json.getString("uuid") );
    		Long timestamp = Long.parseLong( json.getString("timestamp") );
    		
    		return new AnnotationReference( uuid, timestamp );
		    
    	} catch ( Exception e ) {
    		throw new IllegalArgumentException( e );
    	}

    }
    
    @Override
    public int compareTo( AnnotationReference o ) {
    	// java sorts in ascending order, we want descending ( new references first )
    	// so we negate the compareTo
    	return -_timestamp.compareTo( o._timestamp );
    }
    
    
    @Override
    public int hashCode () {
    	return _uuid.hashCode() * _timestamp.hashCode();
    }
    
    
    @Override
    public boolean equals (Object that) {   	    	
    	if (that != null)
    	{
    		if (that instanceof AnnotationReference) {
    			return _uuid.equals( ((AnnotationReference)that)._uuid ) &&
    					_timestamp.equals( ((AnnotationReference)that)._timestamp );
    		}
    	}	
    	return false;
    }
    
}
