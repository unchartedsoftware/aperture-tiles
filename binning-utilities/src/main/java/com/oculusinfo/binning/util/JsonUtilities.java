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
package com.oculusinfo.binning.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @author jandre
 *
 */
public class JsonUtilities {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtilities.class);
    private static Object getJSONNull () {
        try {
            return new JSONObject("{a: null}").get("a");
        } catch (JSONException e) {
            LOGGER.error("Can't come up with JSON null value");
            return null;
        }
    }
    private static final Object JSON_NULL = getJSONNull();

    /**
     * Clone a JSON object and all its child objects
     */
    public static JSONObject deepClone (JSONObject source) {
        if (null == source) return null;

        try {
            JSONObject clone = new JSONObject();
            for (String key: JSONObject.getNames(source)) {
                Object value = source.get(key);
                if (value instanceof JSONObject) {
                    JSONObject valueClone = deepClone((JSONObject) value);
                    clone.put(key, valueClone);
                } else if (value instanceof JSONArray) {
                    JSONArray valueClone = deepClone((JSONArray) value);
                    clone.put(key, valueClone);
                } else {
                    clone.put(key, value);
                }
            }
            return clone;
        } catch (JSONException e) {
            LOGGER.error("Weird JSON exception cloning object", e);
            return null;
        }
    }
    
    /**
     * Clone a JSON array and all its child objects
     */
    public static JSONArray deepClone (JSONArray source) {
        if (null == source) return null;

        try {
            JSONArray clone = new JSONArray();
            for (int i=0; i<source.length(); ++i) {
                Object value = source.get(i);
                if (value instanceof JSONObject) {
                    JSONObject valueClone = deepClone((JSONObject) value);
                    clone.put(i, valueClone);
                } else if (value instanceof JSONArray) {
                    JSONArray valueClone = deepClone((JSONArray) value);
                    clone.put(i, valueClone);
                } else {
                    clone.put(i, value);
                }
            }
            return clone;
        } catch (JSONException e) {
            LOGGER.error("Weird JSON exception cloning object", e);
            return null;
        }
    }

    /**
     * Overlays one JSON object, in place, over another, deeply.
     * 
     * @param base The object to alter
     * @param overlay The object defining how the base will be altered.
     * @return The base object, with the overlay now overlaid upon it.
     */
    public static JSONObject overlayInPlace (JSONObject base, JSONObject overlay) {
        if (null == overlay) return base;
        if (null == base) return deepClone(overlay);

        try {
            for (String key: JSONObject.getNames(overlay)) {
                Object value = overlay.get(key);
                if (value instanceof JSONObject) {
                    if (base.has(key) && base.get(key) instanceof JSONObject) {
                        overlayInPlace((JSONObject) base.get(key), (JSONObject) value);
                    } else {
                        base.put(key, deepClone((JSONObject) value));
                    }
                } else if (value instanceof JSONArray) {
                    if (base.has(key) && base.get(key) instanceof JSONArray) {
                        overlayInPlace((JSONArray) base.get(key), (JSONArray) value);
                    } else {
                        base.put(key, deepClone((JSONArray) value));
                    }
                } else {
                    base.put(key, value);
                }
            }
            return base;
        } catch (JSONException e) {
            LOGGER.error("Weird JSON exception cloning object", e);
            return null;
        }
    }

    /**
     * Overlays one JSON array, in place, over another, deeply.
     * 
     * @param base The array to alter
     * @param overlay The array defining how the base will be altered.
     * @return The base array, with the overlay now overlaid upon it.
     */
    public static JSONArray overlayInPlace (JSONArray base, JSONArray overlay) {
        if (null == overlay) return base;
        if (null == base) return deepClone(overlay);

        try {
            for (int i=0; i<overlay.length(); ++i) {
                Object value = overlay.get(i);
                if (JSON_NULL.equals(value)) {
                    // Null array element; ignore, don't everlay
                } else if (value instanceof JSONObject) {
                    if (base.length() > i && base.get(i) instanceof JSONObject) {
                        overlayInPlace((JSONObject) base.get(i), (JSONObject) value);
                    } else {
                        base.put(i, deepClone((JSONObject) value));
                    }
                } else if (value instanceof JSONArray) {
                    if (base.length() > i && base.get(i) instanceof JSONArray) {
                        overlayInPlace((JSONArray) base.get(i), (JSONArray) value);
                    } else {
                        base.put(i, deepClone((JSONArray) value));
                    }
                } else {
                    base.put(i, value);
                }
            }
            return base;
        } catch (JSONException e) {
            LOGGER.error("Weird JSON exception cloning object", e);
            return null;
        }
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
