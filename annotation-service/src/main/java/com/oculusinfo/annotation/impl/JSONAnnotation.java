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

import java.util.UUID;
import org.json.JSONObject;

import com.oculusinfo.annotation.*;


/*
 * JSONAnnotation
 * {
 * 		x:
 * 		y:
 * 		level:
 * 		uuid:
 * 		priority:
 * 		data: {}
 * }
 */

public class JSONAnnotation extends AnnotationData<JSONObject> {

	private static final long serialVersionUID = 1L;
	
	Double _x = null;
	Double _y = null;
	Integer _level = null;			
	UUID _uuid = null;
	Long _timestamp = null;
	String _priority = null;
	JSONObject _data = null;
	
	
	public JSONAnnotation( Double x, Double y, Integer level, String priority, UUID uuid, Long timestamp, JSONObject data ) {
		_x = x;
		_y = y;
		_level = level;
		_uuid = uuid;
		_timestamp = timestamp;
		_priority = priority;		
		_data = data;
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
	
	
	public Integer getLevel() {
		return _level;
	}
	
	
	public UUID getUUID() {
		return _uuid;		
	}
	
	public Long getTimeStamp() {
		return _timestamp;		
	}
		
	public String getPriority() {
		return _priority;
	}
	
	public JSONObject getData() {
		return _data;
	}
	
	static public JSONAnnotation fromJSON( JSONObject json ) throws IllegalArgumentException {		

		try {
			
			Double x = json.getDouble("x");
			Double y = json.getDouble("y");
			Integer level = json.getInt("level");
			String priority = json.getString("priority");
			UUID uuid = UUID.fromString( json.getString("uuid") );
			Long timestamp = Long.parseLong( json.getString("timestamp") );
			JSONObject data = json.getJSONObject("data");						
			return new JSONAnnotation( x, y, level, priority, uuid, timestamp, data );
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e );
		}

	}

}
