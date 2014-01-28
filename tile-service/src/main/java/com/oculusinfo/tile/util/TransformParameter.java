/**
 * Copyright (c) 2013 Oculus Info Inc.
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
package com.oculusinfo.tile.util;

import java.lang.IllegalArgumentException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.oculusinfo.utilities.jsonprocessing.JsonUtilities;

/**
 * 
 * @author cregnier
 *
 */
public class TransformParameter {
	private static String DEFAULT_TRANSFORM_NAME = "linear";
	private Map<String, Object> data;

	public TransformParameter(Object txObject) {
		if (txObject instanceof String) {
			data = new HashMap<String, Object>();
			data.put("name", (String)txObject);
		}
		else if (txObject instanceof JSONObject) {
			data = JsonUtilities.jsonObjToMap((JSONObject)txObject);
			
			// Ensure that a name exists
			if (data.get("name") == null) {
				data.put("name", DEFAULT_TRANSFORM_NAME);
			}
		}
		else {
			throw new IllegalArgumentException("Unsupported type for transform parameter.");
		}
	}
	
	public String getName() {
		return getString("name");
	}
	
	public Map<String, Object> getRawData() {
		return data;
	}
	
	//-------------------------------------------------------
	// Helpers
	//-------------------------------------------------------
	
	public String getString(String key) {
		return typedGet(key, String.class);
	}
	
	public void setString(String key, String value) {
		data.put(key, value);
	}
	
	public Integer getInt(String key) {
		return typedGet(key, Integer.class);
	}
	
	public void setInt(String key, Integer value) {
		data.put(key, value);
	}
	
	public Double getDouble(String key) {
		return typedGet(key, Number.class).doubleValue();
	}
	
	public Double getDoubleOrElse(String key, Double defaultVal) {
		Number val = typedGet(key, Number.class);
		if (val == null) val = defaultVal;
		return val.doubleValue();
	}
	
	public void setDouble(String key, Double value) {
		data.put(key, value);
	}
	
	public <T> T typedGet(String key, Class<T> clazz) {
		return clazz.cast(data.get(key));
	}
	
	public Object get(String key) {
		return data.get(key);
	}
	
	public void set(String key, Object o) {
		data.put(key, o);
	}
	
	public boolean contains(String key) {
		return data.containsKey(key);
	}
	
	public List<?> getList(String key) {
		List<?> list = null;
		
		Object o = data.get(key);
		if (o instanceof List) {
			list = (List<?>)o;
		}
		
		return list;
	}
	
	public void setList(String key, List<?> list) {
		data.put(key, list);
	}
	
}
