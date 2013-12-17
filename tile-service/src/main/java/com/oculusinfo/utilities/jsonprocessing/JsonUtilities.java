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
package com.oculusinfo.utilities.jsonprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
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
	
	private static List<Object> jsonArrayToList(JSONArray jsonList) {
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
}
