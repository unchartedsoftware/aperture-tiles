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
			String[] keys = JSONObject.getNames(source);
			if (null != keys) {
				for (String key: keys) {
					Object value = source.opt(key);
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
				if (!source.isNull(i)) {
					Object value = source.opt(i);
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
			String[] names = JSONObject.getNames(overlay);
			if (null == names) names = new String[0];
			for (String key: names) {
				Object value = overlay.opt(key);
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
			LOGGER.error("Weird JSON exception overlaying object", e);
			return null;
		}
	}

	/**
	 * Takes a JSON object, looks for any keys that have periods in them, and expands those into
	 * multi-level objects.
	 *
	 * So:
	 *
	 * <pre>
	 * { "name.first": "Alice", "name.middle": "Barbara", "name.last": "Cavendish" }
	 * </pre>
	 *
	 * becomes:
	 *
	 * <pre>
	 * { "name": { "first": "Alice", "middle": "Barbara", "last": "Cavendish" } }
	 * </pre>
	 *
	 * @param base The JSON object to expand
	 * @return The same JSON object, with keys expanded.
	 * @throws JSONException
	 */
	public static JSONObject expandKeysInPlace (JSONObject base) throws JSONException {
		String[] names = JSONObject.getNames(base);
		if (null == names) names = new String[0];
		for (String key: names) {
			Object value = base.get(key);

			// If our value is a JSON object or array, expend recursively
			if (value instanceof JSONObject) expandKeysInPlace((JSONObject) value);
			if (value instanceof JSONArray) expandKeysInPlace((JSONArray) value);
			// If this key is expandable, expand it
			String[] subKeys = key.split("\\.");
			if (subKeys.length > 1) {
				base.remove(key);
				JSONObject target = base;
				// Put in intermediate objects
				for (int i=0; i<subKeys.length-1; ++i) {
					if (!target.has(subKeys[i]))
						target.put(subKeys[i], new JSONObject());
					if (!(target.get(subKeys[i]) instanceof JSONObject))
						throw new JSONException("Impossible to expand keys - location "+subKeys[i]+" already exists, and is not an object.");
					target = target.getJSONObject(subKeys[i]);
				}
				// Put in our object
				target.put(subKeys[subKeys.length-1], value);
			}
		}
		return base;
	}

	/**
	 * Takes a JSON array, looks for any internal keys that have periods in them, and expands
	 * those into multi-level objects.
	 *
	 * So:
	 *
	 * <pre>
	 * [
	 *   { "name.first": "Alice", "name.middle": "Barbara", "name.last": "Cavendish" },
	 *   { "name.first": "Dave", "name.middle": "Ezekiel", "name.last": "Filmore" }
	 * ]
	 * </pre>
	 *
	 * becomes:
	 *
	 * <pre>
	 * [
	 *   { "name": { "first": "Alice", "middle": "Barbara", "last": "Cavendish" } },
	 *   { "name": { "first": "Dave", "middle": "Ezekiel", "last": "Filmore" } }
	 * ]
	 * </pre>
	 *
	 * @param base The JSON object to expand
	 * @return The same JSON object, with keys expanded.
	 * @throws JSONException
	 */
	public static JSONArray expandKeysInPlace (JSONArray base) throws JSONException {
		for (int i=0; i<base.length(); ++i) {
			if (!base.isNull(i)) {
				Object value = base.get(i);
				// If our value is a JSON object or array, expend recursively
				if (value instanceof JSONObject) expandKeysInPlace((JSONObject) value);
				if (value instanceof JSONArray) expandKeysInPlace((JSONArray) value);
			}

		}
		return base;
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
				Object value = overlay.opt(i);
				Object baseValue = base.opt(i);
				if (JSON_NULL.equals(value)) {
					// Null array element; ignore, don't everlay
					if (baseValue instanceof JSONObject) {
						result.put(i, deepClone((JSONObject) baseValue));
					} else if (baseValue instanceof JSONArray) {
						result.put(i, deepClone((JSONArray) baseValue));
					} else {
						result.put(i, baseValue);
					}
				} else if (value instanceof JSONObject) {
					if (null != baseValue && baseValue instanceof JSONObject) {
						result.put(i, overlayInPlace((JSONObject) baseValue, (JSONObject) value));
					} else {
						result.put(i, deepClone((JSONObject) value));
					}
				} else if (value instanceof JSONArray) {
					if (null != baseValue && baseValue instanceof JSONArray) {
						result.put(i, overlay((JSONArray) baseValue, (JSONArray) value));
					} else {
						result.put(i, deepClone((JSONArray) value));
					}
				} else {
					result.put(i, value);
				}
			}

			return result;
		} catch (JSONException e) {
			LOGGER.error("Weird JSON exception overlaying "+overlay+" on "+base, e);
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

		for (String key: properties.stringPropertyNames()) {
			try {
				addKey(json, key, properties.getProperty(key));
			} catch (JSONException e) {
				LOGGER.warn("Error transfering property {} from properties file to json", key, e);
			}
		}

		return json;
	}

	/**
	 * Transform a string -> string map into a json object, nesting levels
	 * based on a period.
	 *
	 * @param map The string -> string map to translate
	 *
	 * @return The same data, as a JSON object
	 */
	public static JSONObject mapToJSON (Map<String, String> map) {
		Properties properties = new Properties();
		properties.putAll(map);
		return propertiesObjToJSON(properties);
	}

	private static void addKey (JSONObject json, String key, String value) throws JSONException {
		int keyBreak = key.indexOf(".");
		if (-1 == keyBreak) {
			// At leaf object.
			if (json.has(key)) {
				throw new JSONException("Duplicate key "+key);
			}
			// The value string itself may be valid JSON - try to parse it and add the JSON
			// object instead of the string if so.
			try {
				JSONObject obj = new JSONObject(value);
				json.put(key, obj);
			} catch (JSONException e){
				json.put(key, value);
			}
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

	/**
	 * Checks to see if supplied string is valid JSON
	 * @param str candiate JSON string
	 * @return <code>true</code> if valid JSON, <code>false</code> otherwise.
	 */
	public static boolean isJSON(String str) {
		try {
			new JSONObject(str);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
}
