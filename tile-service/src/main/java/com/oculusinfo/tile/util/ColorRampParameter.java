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
public class ColorRampParameter {
	private static String DEFAULT_RAMP_NAME = "ware";
	private Map<String, Object> data;

	public ColorRampParameter(Object rampObject) {
		if (rampObject instanceof String) {
			data = new HashMap<String, Object>();
			data.put("name", (String)rampObject);
		}
		else if (rampObject instanceof JSONObject) {
			data = JsonUtilities.jsonObjToMap((JSONObject)rampObject);
			
			// Ensure that a name exists
			if (data.get("name") == null) {
				data.put("name", DEFAULT_RAMP_NAME);
			}
		}
		else {
			throw new IllegalArgumentException("Unsupported type for rampObject parameter.");
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
