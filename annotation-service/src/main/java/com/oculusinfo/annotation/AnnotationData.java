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

import com.oculusinfo.factory.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.UUID;


/**
 * This class represents a single annotation
 */
public abstract class AnnotationData<T> implements Serializable {

	private static final long serialVersionUID = 1L;

    public abstract Double getX();
    public abstract Double getY();
    public abstract Double getX0();
    public abstract Double getY0();
    public abstract Double getX1();
    public abstract Double getY1();
    public abstract boolean isRangeBased();
	public abstract Integer getLevel();
	public abstract UUID getUUID();
	public abstract Long getTimestamp();
	public abstract String getGroup();
	public abstract T getData();
    public abstract void updateCertificate();
    public abstract Pair<Integer, Integer> getRange();
	public Pair<String, Long> getCertificate() {
		return new Pair<>( getUUID().toString(), getTimestamp() );
	}
	
	static public AnnotationData<?> fromJSON( JSONObject json ) throws IllegalArgumentException {		
		return null;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject json = new JSONObject();

            json.put("level", getLevel() );

            if ( getX1() == null ) {
                if ( getX() != null ) {
                    json.put("x", getX());
                }
            } else {
                JSONArray x = new JSONArray();
                x.put( getX0() );
                x.put( getX1() );
                json.put( "x", x );
            }

            if ( getY1() == null ) {
                if ( getY() != null ) {
                    json.put( "y", getY() );
                }
            } else {
                JSONArray y = new JSONArray();
                y.put( getY0() );
                y.put( getY1() );
                json.put( "y", y );
            }

            JSONObject range = new JSONObject();
            range.put("min", getRange().getFirst() );
            range.put("max", getRange().getSecond() );
            json.put("range", range );

            json.put("group", getGroup() );
            json.put("data", getData() );

            JSONObject certificate = new JSONObject();
            certificate.put("uuid", getUUID().toString() );
            certificate.put("timestamp", getTimestamp().toString() );
            json.put("certificate", certificate );

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
    				   getX().equals( o.getX() ) &&
    				   getY().equals( o.getY() ) &&
    				   getLevel().equals( o.getLevel() ) &&
    				   getData().equals( o.getData() );
    		}    		
    	}	
    	return false;
    }
}
