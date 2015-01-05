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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
            String[] keys = JSONObject.getNames(source);
            if (null != keys) {
	            for (String key: keys) {
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
                        base.put(key, overlay((JSONArray) base.get(key), (JSONArray) value));
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
     * Overlays one JSON array over another, deeply. This does not work in
     * place, but passes back a new array
     * 
     * @param base
     *            The array to alter
     * @param overlay
     *            The array defining how the base will be altered.
     * @return The base array, with the overlay now overlaid upon it.
     */
    public static JSONArray overlay (JSONArray base, JSONArray overlay) {
        if (null == overlay) return base;
        if (null == base) return deepClone(overlay);
        
        try {
        	JSONArray result = new JSONArray();
            
            // Overlay elements in both or just in the overlay
            for (int i=0; i<overlay.length(); ++i) {
                Object value = overlay.get(i);
                if (JSON_NULL.equals(value)) {
                    // Null array element; ignore, don't everlay
                	Object baseValue = base.get(i);
                	if (baseValue instanceof JSONObject) {
                		result.put(i, deepClone((JSONObject) baseValue));
                	} else if (baseValue instanceof JSONArray) {
                		result.put(i, deepClone((JSONArray) baseValue));
                	} else {
                		result.put(i, baseValue);	
                	}                	
                } else if (value instanceof JSONObject) {
                    if (base.length() > i && base.get(i) instanceof JSONObject) {
                        result.put(i, overlayInPlace((JSONObject) base.get(i), (JSONObject) value));
                    } else {
                        result.put(i, deepClone((JSONObject) value));
                    }
                } else if (value instanceof JSONArray) {
                    if (base.length() > i && base.get(i) instanceof JSONArray) {
                        result.put(i, overlay((JSONArray) base.get(i), (JSONArray) value));
                    } else {
                        result.put(i, deepClone((JSONArray) value));
                    }
                } else {
                    result.put(i, value);
                }
            }

            return result;
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



	/**
     * Transform a JSON object into a properties object, concatenating levels
     * into keys using a period.
     * 
     * @param jsonObj
     *            The JSON object to translate
     * @return The same data, in properties form
     */
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



    /**
     * Transform a JSON object into a properties object, concatenating levels
     * into keys using a period.
     * 
     * @param properties The properties object to translate
     *
     * @return The same data, in properties form
     */
    public static JSONObject propertiesObjToJSON (Properties properties) {
        JSONObject json = new JSONObject();

        for (Object keyObj: properties.keySet()) {
            String key = keyObj.toString();
            try {
                addKey(json, key, properties.getProperty(key));
            } catch (JSONException e) {
                LOGGER.warn("Error transfering property {} from properties file to json", key, e);
            }
        }

        return json;
    }

    private static void addKey (JSONObject json, String key, String value) throws JSONException {
        int keyBreak = key.indexOf(".");
        if (-1 == keyBreak) {
            // At leaf object.
            if (json.has(key)) {
                throw new JSONException("Duplicate key "+key);
            }
            json.put(key, value);
        } else {
            String keyCAR = key.substring(0, keyBreak);
            String keyCDR = key.substring(keyBreak+1);
            String keyCADR;

            int cdrBreak = keyCDR.indexOf(".");
            if (-1 == cdrBreak) {
                keyCADR = keyCDR;
            } else {
                keyCADR = keyCDR.substring(0, cdrBreak);
            }

            // See if our next element can be an array element.
            boolean arrayOk;
            try {
                Integer.parseInt(keyCADR);
                arrayOk = true;
            } catch (NumberFormatException e) {
                arrayOk = false;
            }

            if (json.has(keyCAR)) {
                Object elt = json.get(keyCAR);
                if (elt instanceof JSONArray) {
                    JSONArray arrayElt = (JSONArray) elt;
                    if (arrayOk) {
                        addKey(arrayElt, keyCDR, value);
                    } else {
                        JSONObject arrayTrans = new JSONObject();
                        for (int i=0; i<arrayElt.length(); ++i) {
                            arrayTrans.put(""+i, arrayElt.get(i));
                        }
                        json.put(keyCAR, arrayTrans);
                        addKey(arrayTrans, keyCDR, value);
                    }
                } else if (elt instanceof JSONObject) {
                    addKey((JSONObject) elt, keyCDR, value);
                } else {
                    throw new JSONException("Attempt to put both object and value in JSON object at key "+keyCAR);
                }
            } else {
                if (arrayOk) {
                    JSONArray arrayElt = new JSONArray();
                    json.put(keyCAR, arrayElt);
                    addKey(arrayElt, keyCDR, value);
                } else {
                    JSONObject elt = new JSONObject();
                    json.put(keyCAR, elt);
                    addKey(elt, keyCDR, value);
                }
            }
        }
    }

    private static void addKey (JSONArray json, String key, String value) throws JSONException {
        int keyBreak = key.indexOf(".");
        if (-1 == keyBreak) {
            // At leaf object.
            int index = Integer.parseInt(key);
            json.put(index, value);
        } else {
            String keyCAR = key.substring(0, keyBreak);
            String keyCDR = key.substring(keyBreak+1);
            String keyCADR;

            int cdrBreak = keyCDR.indexOf(".");
            if (-1 == cdrBreak) {
                keyCADR = keyCDR;
            } else {
                keyCADR = keyCDR.substring(0, cdrBreak);
            }

            // See if our next element can be an array element.
            boolean arrayOk;
            try {
                Integer.parseInt(keyCADR);
                arrayOk = true;
            } catch (NumberFormatException e) {
                arrayOk = false;
            }

            int index = Integer.parseInt(keyCAR);
            Object raw;
            try {
                raw = json.get(index);
            } catch (JSONException e) {
                raw = null;
            }

            if (raw instanceof JSONArray) {
                JSONArray arrayElt = (JSONArray) raw;
                if (arrayOk) {
                    addKey(arrayElt, keyCDR, value);
                } else {
                    JSONObject arrayTrans = new JSONObject();
                    for (int i=0; i<arrayElt.length(); ++i) {
                        arrayTrans.put(""+i, arrayElt.get(i));
                    }
                    json.put(index, arrayTrans);
                    addKey(arrayTrans, keyCDR, value);
                }
            } else if (raw instanceof JSONObject) {
                addKey((JSONObject) raw, keyCDR, value);
            } else {
                if (arrayOk) {
                    JSONArray arrayElt = new JSONArray();
                    json.put(index, arrayElt);
                    addKey(arrayElt, keyCDR, value);
                } else {
                    JSONObject elt = new JSONObject();
                    json.put(index, elt);
                    addKey(elt, keyCDR, value);
                }
            }
        }
    }
}
