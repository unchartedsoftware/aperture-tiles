/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rendering;

import java.util.HashMap;
import java.util.Map;

/**
 * A general property bag that guarantees certain methods
 * will be available. This does not necessarily mean that the property bag
 * will contain all the properties retrieved in each method.
 * 
 * @author  dgray
 * @author cregnier
 * 
 */
public class RenderParameter {
	
	private Map<String, Object> data;
	
	public RenderParameter() {
		data = new HashMap<String, Object>();
	}
	
	public RenderParameter(Map<String, Object> params) {
		data = new HashMap<String, Object>(params);
	}
	
	public Map<String, Object> getRawData() {
		return data;
	}
	
	
	public int getOutputWidth() {
		return getInt(RenderParameterFactory.OUTPUT_WIDTH.getName());
	}



	public void setOutputWidth(int outputWidth) {
		setInt(RenderParameterFactory.OUTPUT_WIDTH.getName(), outputWidth);
	}



	public int getOutputHeight() {
		return getInt(RenderParameterFactory.OUTPUT_HEIGHT.getName());
	}



	public void setOutputHeight(int outputHeight) {
		setInt(RenderParameterFactory.OUTPUT_HEIGHT.getName(), outputHeight);
	}


	//-------------------------------------------------------
	// Helpers
	//-------------------------------------------------------
	
	public String getString(String key) {
		return typedGet(key, String.class);
	}
	
	public String getAsString(String key) {
		String ret = null;
		try {
			ret = typedGet(key, String.class);
		}
		catch (ClassCastException e) {
			Object o = data.get(key);
			ret = (o != null)? o.toString() : null;
		}
		return ret;
	}
	
	public String getAsStringOrElse(String key, String defVal) {
		String val = getAsString(key);
		return (val != null)? val : defVal;
	}
	
	public void setString(String key, String value) {
		data.put(key, value);
	}

	public Integer getInt(String key) {
		return typedGet(key, Integer.class);
	}
	
	public Integer getAsInt(String key) {
		Integer ret = null;
		try {
			ret = typedGet(key, Integer.class);
		}
		catch (ClassCastException e) {
			//value wasn't an integer, so see if its a string and we can parse it
			ret = Integer.parseInt(typedGet(key, String.class));
		}
	
		return ret;
	}
	
	public Integer getAsIntOrElse(String key, Integer defVal) {
		Integer val = getAsInt(key);
		return (val != null)? val : defVal;
	}
	
	public void setInt(String key, Integer value) {
		data.put(key, value);
	}
	
	public Double getDouble(String key) {
		return typedGet(key, Double.class);
	}
	
	public Double getAsDouble(String key) {
		Double ret = null;
		try {
			ret = typedGet(key, Double.class);
		}
		catch (ClassCastException e) {
			//value wasn't a double, so see if its a string and we can parse it
			ret = Double.parseDouble(typedGet(key, String.class));
		}
		
		return ret;
	}
	
	public Double getAsDoubleOrElse(String key, Double defVal) {
		Double val = getAsDouble(key);
		return (val != null)? val : defVal;
	}
	
	public void setDouble(String key, Double value) {
		data.put(key, value);
	}
	
	public Boolean getBoolean(String key) {
		return typedGet(key, Boolean.class);
	}
	
	public Boolean getAsBoolean(String key) {
		Boolean ret = null;
		try {
			ret = typedGet(key, Boolean.class);
		}
		catch (ClassCastException e) {
			//value wasn't a double, so see if its a string and we can parse it
			ret = Boolean.parseBoolean(typedGet(key, String.class));
		}
		
		return ret;
	}
	
	public Boolean getAsBooleanOrElse(String key, Boolean defVal) {
		Boolean val = getAsBoolean(key);
		return (val != null)? val : defVal;
	}
	
	public void setBoolean(String key, Boolean value) {
		data.put(key, value);
	}

	public <T> T getObject(String key, Class<T> clazz) {
		return typedGet(key, clazz);
	}
	
	public void setObject(String key, Object value) {
		data.put(key, value);
	}
	
	public <T> T typedGet(String key, Class<T> clazz) {
		return clazz.cast(data.get(key));
	}
	
}