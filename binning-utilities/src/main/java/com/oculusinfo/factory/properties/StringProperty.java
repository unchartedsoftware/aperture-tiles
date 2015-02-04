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
import org.json.JSONException;

import java.util.UUID;

public class StringProperty implements ConfigurationProperty<String> {

    private String _name;
	private String _description;
	private String _defaultValue;
	private String[] _possibleValues;
	private String _uuid;

	public StringProperty (String name, String description, String defaultValue) {
		_name = name;
		_description = description;
		_defaultValue = defaultValue;
		_possibleValues = null;
		_uuid = UUID.randomUUID().toString();
	}

	public StringProperty (String name, String description, String defaultValue, String[] possibleValues) {
		_name = name;
		_description = description;
		_defaultValue = defaultValue;
		_possibleValues = possibleValues;
		_uuid = UUID.randomUUID().toString();
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
	public Class<String> getType () {
		return String.class;
	}

	@Override
	public String[] getPossibleValues () {
		return _possibleValues;
	}

	@Override
	public String getDefaultValue () {
		return _defaultValue;
	}

	@Override
	public String encode (String value) {
		return value;
	}

	@Override
	public String unencode (String value) throws ConfigurationException {
		return value;
	}

	@Override
	public void encodeJSON (JSONNode propertyNode, String value) throws JSONException {
		propertyNode.setAsString(encode(value));
	}

	@Override
	public String unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
		return unencode(propertyNode.getAsString());
	}

	@Override
	public int hashCode () {
		return _uuid.hashCode();
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof StringProperty)) return false;
        
		StringProperty thatP = (StringProperty) that;
		return thatP._uuid.equals(this._uuid);
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"string\"/>", _name);
	}

	/**
	 * Create a duplicate property, identical in all but the list of possible
	 * values.
	 * 
	 * @param newPossibilities The new list of possible values to use for the
	 *            new property.
	 * @return A duplicate property as described.
	 */
	public StringProperty overridePossibleValues (String[] newPossibilities) {
		return new StringProperty(_name, _description, _defaultValue, newPossibilities);
	}

	/**
	 * A utility function, mostly for use with the above
	 * {@link #overridePossibleValues(String[])}, to get an array that is
	 * essentially the union of two arrays.
	 * 
	 * @param array The old array
	 * @param newValues Any new values to add
	 * @return An array of all the old and new values
	 */
	public static String[] addToArray (String[] array, String... newValues) {
		if (null == array || 0 == array.length) return newValues;
		if (null == newValues || 0 == newValues.length) return array;

		int n = array.length;
		String[] result = new String[n+newValues.length];
		for (int i=0; i<n; ++i) result[i] = array[i];
		for (int i=0; i<newValues.length; ++i) result[i+n] = newValues[i];

		return result;
	}
}
