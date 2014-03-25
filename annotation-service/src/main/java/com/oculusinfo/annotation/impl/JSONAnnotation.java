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
package com.oculusinfo.annotation.impl;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oculusinfo.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.json.JSONObject;

/*
 * JSONAnnotation
 * {
 * 		x:
 * 		y:
 * 		priority:
 * 		data: {}
 * }
 */

public class JSONAnnotation extends AnnotationData {

	private static final long serialVersionUID = 1L;
	
	Double _x = null;
	Double _y = null;
	String _priority;
	JSONObject _data;
	
	public JSONAnnotation( JSONObject json ) {
		
		boolean xExists = !json.isNull("x");
    	boolean yExists = !json.isNull("y");    	
		try {			
			_x = ( xExists ) ? json.getDouble("x") : null;
			_y = ( yExists ) ? json.getDouble("y") : null;
			_priority = json.getString("priority");
			_data = json.getJSONObject("data");
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
	}
	
	public <T> void add( String key, T data ) {		
		try {
			_data.put( key, data );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Double getX() {
		return _x;
	}
	
	public Double getY() {
		return _y;
	}
	
	public JSONObject getData() {
		return _data;
	}
	
	public String getPriority() {
		return _priority;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();
			if (_x != null) json.put( "x", _x );
			if (_y != null) json.put( "y", _y );
			json.put("priority", _priority);
			json.put("data", _data );
			return json;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;	
	}
	
	private Long hash( JSONObject data ) {
    	
    	String dataStr = data.toString();
    	long mix = 0 ^ 104395301;
    	long  mulp = 2654435789L;
    	
    	for (int i = 0; i < dataStr.length(); i++){
    	    char c = dataStr.charAt(i);  
    	    mix += (c * mulp) ^ (mix >> 23);
    	}
    	
    	return ( mix ^ (mix << 37) );
    }
	
	public Long getIndex() {
		return hash( toJSON() );		
	}
	
	@Override
    public boolean equals (Object that) {   	    	
    	if (that != null)
    	{
    		if (that instanceof JSONAnnotation) {
    			return getIndex().equals( ((JSONAnnotation)that).getIndex() );
    		}    		
    	}	
    	return false;
    }
}
