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

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.factory.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;


/*
 * JSONAnnotation
 * {
 * 		x:
 * 		y:
 * 		level:
 * 	    range: {
 * 	        min:
 * 	        max:
 * 	    }
 * 	    group:
 * 		certificate: {
 * 	        timestamp:
 * 	    	uuid:
 * 	    }
 * 		data: {}
 * }
 */

public class JSONAnnotation extends AnnotationData<JSONObject> {

	private static final long serialVersionUID = 1L;
	
	Double _x0 = null;
    Double _y0 = null;
    Double _x1 = null;
    Double _y1 = null;
	Integer _level = null;			
	UUID _uuid = null;
	Long _timestamp = null;
	String _group = null;
	JSONObject _data = null;
	Pair<Integer, Integer> _range = null;

    public JSONAnnotation( Double x0, Double x1, Double y0, Double y1, Integer level, Pair<Integer,Integer> range, String group, UUID uuid, Long timestamp, JSONObject data ) {
        _x0 = x0;
        _y0 = y0;
        _x1 = x1;
        _y1 = y1;
        _level = level;
        _range = range;
        _group = group;
        _uuid = uuid;
        _timestamp = timestamp;
        _data = data;
    }


    public JSONAnnotation( Double x0, Double x1, Double y0, Double y1, Integer level, Pair<Integer,Integer> range, String group, JSONObject data ) {
        _x0 = x0;
        _y0 = y0;
        _x1 = x1;
        _y1 = y1;
        _level = level;
        _range = range;
        _group = group;
        _data = data;
        // generate certificate
        _uuid = UUID.randomUUID();
        _timestamp = new Timestamp( new Date().getTime() ).getTime();
    }

	public <T> void add( String key, T data ) {		
		try {
			_data.put( key, data );
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Double getX() {
		return _x0;
	}

	public Double getY() {
		return _y0;
	}

    public Double getX0() {
        return _x0;
    }

    public Double getY0() {
        return _y0;
    }

    public Double getX1() {
        return _x1;
    }

    public Double getY1() {
        return _y1;
    }

    public boolean isRangeBased() {
        return ( getX1() != null ||
                 getY1() != null );
    }

	public Integer getLevel() {
		return _level;
	}

	public UUID getUUID() {
		return _uuid;		
	}

	public Long getTimestamp() {
		return _timestamp;		
	}

	public String getGroup() {
		return _group;
	}
	
	public JSONObject getData() {
		return _data;
	}

    public Pair<Integer, Integer> getRange() {
        return _range;
    }

    public void updateCertificate() {
        _timestamp = new Timestamp( new Date().getTime() ).getTime();
    }

	static public JSONAnnotation fromJSON( JSONObject json ) throws IllegalArgumentException {

		try {
            Integer level = json.getInt("level");

            Double x0, x1;
            if ( json.optJSONArray("x") != null ) {
                JSONArray xs = json.getJSONArray("x");
                x0 = xs.getDouble(0);
                x1 = xs.getDouble(1);
                // if range but values are the same, collapse to a point
                if (x0.equals(x1)) {
                	x1 = null;
                }
            } else {
                x0 = Double.isNaN( json.optDouble("x") ) ? null : json.getDouble("x");
                x1 = null;
            }

            Double y0, y1;
            if ( json.optJSONArray("y") != null ) {
                JSONArray ys = json.getJSONArray("y");
                y0 = ys.getDouble(0);
                y1 = ys.getDouble(1);
                // if range but values are the same, collapse to a point
                if (y0.equals(y1)) {
                	y1 = null;
                }
            } else {
                y0 = Double.isNaN( json.optDouble("y") ) ? null : json.getDouble("y");
                y1 = null;
            }

            JSONObject rangeJson = json.getJSONObject("range");
            Integer min = rangeJson.getInt("min");
            Integer max = rangeJson.getInt("max");
            Pair<Integer, Integer> range = new Pair<>( min, max );

			String group = json.getString("group");
            JSONObject data = json.getJSONObject("data");

            UUID uuid;
            Long timestamp;

            try {
                // certificate may not exist on write
                JSONObject certificate = json.getJSONObject("certificate");
                uuid = UUID.fromString(certificate.getString("uuid"));
                timestamp = Long.parseLong(certificate.getString("timestamp"));
            } catch ( Exception e ) {
                // no certificate is provided, generate them
                return new JSONAnnotation( x0, x1, y0, y1, level, range, group, data );
            }
            // certificate is provided
			return new JSONAnnotation( x0, x1, y0, y1, level, range, group, uuid, timestamp, data );
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e );
		}

	}

}
