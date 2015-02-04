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
package com.oculusinfo.factory.properties;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.JSONNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * A {@link ConfigurationProperty} that can handle {@link JSONArray}s.
 * 
 * @author cregnier
 *
 */
public class JSONArrayProperty implements ConfigurationProperty<JSONArray> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONArrayProperty.class);
	private String _name;
	private String _description;
	private JSONArray _defaultValue;
	private String _uuid;

	public JSONArrayProperty (String name, String description, String defaultValue) {
		_name = name;
		_description = description;
		_uuid = UUID.randomUUID().toString();
		if (null == defaultValue) {
			_defaultValue = null;
		} else {
			try {
				_defaultValue = new JSONArray(defaultValue);
			} catch (JSONException e) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Error reading default value for property "+_name+".  Input default was: \""+defaultValue+"\"", e);
				}
			}
		}
	}

	@Override
	public String getName () {
		return _name;
	}

	@Override
	public String getDescription () {
		return _description;
	}

	@Override
	public Class<JSONArray> getType () {
		return JSONArray.class;
	}

	@Override
	public JSONArray[] getPossibleValues () {
		// Innumerable possibilities.
		return null;
	}

	@Override
	public JSONArray getDefaultValue () {
		return _defaultValue;
	}

	@Override
	public String encode (JSONArray value) {
		return value.toString();
	}

	@Override
	public JSONArray unencode (String value) throws ConfigurationException {
		try {
			return new JSONArray(value);
		} catch (JSONException e) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Error reading value for property "+_name+".  Input was: \""+value+"\"", e);
			}
			return null;
		}
	}

	@Override
	public void encodeJSON (JSONNode propertyNode, JSONArray value) throws JSONException {
		propertyNode.setAsJSONArray(value);
	}

	@Override
	public JSONArray unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
		return propertyNode.getAsJSONArray();
	}

	@Override
	public int hashCode () {
		return _uuid.hashCode();
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof JSONArrayProperty)) return false;
        
		JSONArrayProperty thatP = (JSONArrayProperty) that;
		return thatP._uuid.equals(this._uuid);
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"JSON\"/>", _name);
	}
}
