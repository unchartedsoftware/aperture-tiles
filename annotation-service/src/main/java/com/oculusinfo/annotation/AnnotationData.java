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

import com.oculusinfo.binning.util.Pair;

import org.json.JSONObject;


/**
 * This class represents a single annotation
 */
public abstract class AnnotationData<T> implements Serializable {
    
	private static final long serialVersionUID = 1L;
	
	public abstract Double getX();
	public abstract Double getY();
	public abstract Integer getLevel();
	public abstract UUID getUUID();
	public abstract Long getTimeStamp();
	public abstract String getPriority();
	public abstract T getData();
	
	public Pair<String, Long> getReference() {
		return new Pair<String, Long>( getUUID().toString(), getTimeStamp() );
	}
	
	static public AnnotationData<?> fromJSON( JSONObject json ) throws IllegalArgumentException {		
		return null;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			json.put( "x", getX() );
			json.put( "y", getY() );
			json.put("level", getLevel() );
			json.put("uuid", getUUID().toString() );
			json.put("timestamp", getTimeStamp().toString() );
			json.put("priority", getPriority() );
			json.put("data", getData() );
			return json;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
    public int hashCode () {
    	return getUUID().hashCode();
    }

    @Override
    public boolean equals (Object that) {   	    	
    	if (that != null)
    	{
    		if (that instanceof AnnotationData<?>) {
    			AnnotationData<?> o = (AnnotationData<?>)that;
    			return getUUID().equals( o.getUUID() ) &&
    				   getX() == o.getX() &&
    				   getY() == o.getY() &&
    				   getLevel() == o.getLevel() &&
    				   getData().equals( o.getData() );
    		}    		
    	}	
    	return false;
    }
}
