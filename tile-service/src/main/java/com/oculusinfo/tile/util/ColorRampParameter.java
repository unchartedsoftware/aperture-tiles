package com.oculusinfo.tile.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author cregnier
 *
 */
public class ColorRampParameter {

	private Map<String, Object> data;

	public ColorRampParameter(String name) {
		data = new HashMap<String, Object>();
		data.put("name", name);
	}
	
	public ColorRampParameter(Map<String, Object> params) {
		data = new HashMap<String, Object>(params);
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
	
	public <T> List<? extends T> getList(String key, Class<T> clazz) {
		List<? extends T> list = null;
		
		Object o = data.get(key);
		if (o instanceof List) {
			list = (List<? extends T>)o;
		}
		
		return list;
	}
	
	public void setList(String key, List<?> list) {
		data.put(key, list);
	}
	
}
