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
package com.oculusinfo.tile.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * @author jandre
 *
 */
public class JsonUtilities {
	private JsonUtilities() {
	}
    
	/**
	 * Converts a {@link JSONObject} into a {@link Map} of key-value pairs.
	 * This iterates through the tree and converts all {@link JSONObject}s
	 * into their equivalent map, and converts {@link JSONArray}s into
	 * {@link List}s.
	 * 
	 * @param jsonObj
	 * @return
	 * 	Returns a map with the same 
	 */
	public static Map<String, Object> jsonObjToMap(JSONObject jsonObj) {
		Map<String, Object> map = new HashMap<String, Object>();
		
		Iterator<?> keys = jsonObj.keys();
		while (keys.hasNext()) {
			String key = keys.next().toString();
			Object obj = jsonObj.opt(key);
			
			if (obj instanceof JSONObject) {
				map.put(key, jsonObjToMap((JSONObject)obj));
			}
			else if (obj instanceof JSONArray) {
				map.put(key, jsonArrayToList((JSONArray)obj));
			}
			else {
				map.put(key, obj);
			}
		}
		
		return map;
	}
	
	/**
	 * Converts a {@link JSONArray} into a {@link List} of values.
	 * @param jsonList
	 * @return
	 * 	Returns a list of values
	 */
	public static List<Object> jsonArrayToList(JSONArray jsonList) {
		int numItems = jsonList.length();
		List<Object> list = new ArrayList<Object>(numItems);
		for (int i = 0; i < numItems; i++) {
			Object obj = jsonList.opt(i);
			if (obj instanceof JSONObject) {
				list.add(jsonObjToMap((JSONObject)obj));
			}
			else if (obj instanceof JSONArray) {
				list.add(jsonArrayToList((JSONArray)obj));
			}
			else {
				list.add(obj);
			}
		}
		
		return list;
	}
	
	/**
	 * Converts an object into a number.
	 * @return
	 * 	If the object is already a number then it just casts it.
	 * 	If the object is a string, then it parses it as a double.
	 * 	Otherwise the number returned is 0. 
	 */
	public static Number getNumber(Object o) {
		Number val = 0;
		if (o instanceof Number) {
			val = (Number)o;
		}
		else if (o instanceof String) {
			val = Double.valueOf((String)o);
		}
		else if (o instanceof JSONArray) {
			//if the object is an array, then assume it only has one element that is the value
			JSONArray arr = (JSONArray)o;
			if (arr.length() == 1) {
				try {
					val = getNumber(arr.get(0));
				}
				catch (JSONException e) {
					val = 0;
				}
			}
		}
		return val;
	}

	
	/**
	 * Gets a name to use from an object.
	 * If the object is a String, then it will treat the string as the name.
	 * If the object is a {@link JSONObject}, then the name must be a parameter within the object.
	 * If the object is a {@link JSONArray}, then there can only be a single element, which should
	 * contain the name.
	 *  
	 * @param params
	 * @return
	 * 	Returns the name for the object, or null if none can be found.
	 */
	public static String getName(Object params) {
		String name = null;
		
		if (params instanceof String) {
			name = (String)params;
		}
		else if (params instanceof JSONObject) {
			JSONObject transformObj = (JSONObject)params;
			try {
				name = (transformObj.has("name"))? transformObj.getString("name") : null;
			}
			catch (JSONException e) {
				name = null;
			}
		}
		else if (params instanceof JSONArray) {
			//if the transform params is an array, then it should only have one parameter.
			JSONArray vals = (JSONArray)params;
			if (vals.length() == 1) {
				try {
					name = getName(vals.get(0));
				}
				catch (JSONException e) {
					name = null;
				}
			}
			else {
				name = null;
			}
		}
		
		return name;
	}

	/**
	 * Simple getter for a {@link JSONObject} that handles exception handling, and
	 * returns a default value in case there are any problems.
	 * 
	 * @param obj
	 * 	The {@link JSONObject} to query
	 * @param keyName
	 * 	The String name to query from the json object.
	 * @param defaultVal
	 * 	The default value to use if there are any problems.
	 * @return
	 * 	Returns the double value for the key name, or the default value.
	 */
	public static double getDoubleOrElse(JSONObject obj, String keyName, double defaultVal) {
		double val;
		try {
			val = (obj.has(keyName))? obj.getDouble(keyName) : defaultVal;
		}
		catch (JSONException e) {
			val = defaultVal;
		}
		return val;
	}

	public static Properties jsonObjToProperties (JSONObject jsonObj) {
		Properties properties = new Properties();

		addProperties(jsonObj, properties, null);

		return properties;
	}

	private static void addProperties (JSONObject object, Properties properties, String keyBase) {
		Iterator<?> keys = object.keys();
		while (keys.hasNext()) {
			String specificKey = keys.next().toString();
			Object value = object.opt(specificKey);
			String key = (null == keyBase ? "" : keyBase+".") + specificKey;

			if (value instanceof JSONObject) {
				addProperties((JSONObject) value, properties, key);
			} else if (value instanceof JSONArray) {
				addProperties((JSONArray) value, properties, key);
			} else if (null != value) {
				properties.setProperty(key, value.toString());
			}
		}
	}

	private static void addProperties (JSONArray array, Properties properties, String keyBase) {
		for (int i=0; i<array.length(); ++i) {
			String key = (null == keyBase ? "" : keyBase+".")+i;
			Object value = array.opt(i);

			if (value instanceof JSONObject) {
				addProperties((JSONObject) value, properties, key);
			} else if (value instanceof JSONArray) {
				addProperties((JSONArray) value, properties, key);
			} else if (null != value) {
				properties.setProperty(key, value.toString());
			}
		}
	}
}
