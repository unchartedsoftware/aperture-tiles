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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ListProperty<T> implements ConfigurationProperty<List<T>> {

    private ConfigurationProperty<T> _baseProperty;
	private String _name;
	private String _description;
	private String _uuid;

	/**
	 * Construct a list property
	 * @param baseProperty A base property of a type describing individual list elements (and how to encode/decode them).
	 * @param name The name of this property
	 * @param description A description of this property
	 */
	public ListProperty (ConfigurationProperty<T> baseProperty, String name,
	                     String description) {
		_baseProperty = baseProperty;
		_name = name;
		_description = description;
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
	public Class<List<T>> getType () {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T>[] getPossibleValues () {
		// List properties really shouldn't be enumerable.
		return null;
	}

	@Override
	public List<T> getDefaultValue () {
		// Default to an empty list if not overridden.
		return new ArrayList<>();
	}

	private String escape (String value) {
		return value.replace("\\", "\\\\").replace(",", "\\,");
	}

	@Override
	public String encode (List<T> value) {
		String res = "";
		for (int i=0; i<value.size(); ++i) {
			if (0 == i) res = escape(_baseProperty.encode(value.get(i)));
			else res = res + "," + escape(_baseProperty.encode(value.get(i)));
		}

		return res;
	}

	private String unescape (String value) {
		return value.replace("\\,", ",").replace("\\\\", "\\");
	}

	@Override
	public List<T> unencode (String value) throws ConfigurationException {
		List<T> res = new ArrayList<>();

		// Find our split location
		int commaIndex = -1;
		do {
			int startIndex = commaIndex+1;
			boolean found = false;
			do {
				commaIndex = value.indexOf(',', commaIndex+1);
				int firstEscape = commaIndex;
				while (firstEscape > 0 && value.charAt(firstEscape - 1) == '\\')
					firstEscape--;
				int numEscapes = commaIndex - firstEscape;
				if (0 == (numEscapes % 2)) {
					found = true;
				}
			} while (!found && -1 != commaIndex);

			String subValue;
			if (-1 == commaIndex) subValue = unescape(value.substring(startIndex));
			else subValue = unescape(value.substring(startIndex, commaIndex));

			res.add(_baseProperty.unencode(subValue));
		} while (-1 != commaIndex);

		return res;
	}

	public T unencodeEntry (String value) throws ConfigurationException {
		return _baseProperty.unencode(value);
	}

	public String encodeEntry (T value) throws ConfigurationException {
		return _baseProperty.encode(value);
	}

	@Override
	public void encodeJSON (JSONNode propertyNode, List<T> value) throws JSONException {
		JSONArray valueToPut = new JSONArray();
		int i = 0;
		for (T v: value) {
			_baseProperty.encodeJSON(new JSONNode(valueToPut, i), v);
			++i;
		}
		propertyNode.setAsJSONArray(valueToPut);
	}

	@Override
	public List<T> unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
		JSONArray array = propertyNode.getAsJSONArray();
		int length = array.length();
		List<T> result = new ArrayList<>(length);
		for (int i=0; i<length; ++i) {
			result.add(_baseProperty.unencodeJSON(new JSONNode(array, i)));
		}
		return result;
	}

	@Override
	public int hashCode () {
		return _name.hashCode() + _baseProperty.hashCode();
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof ListProperty)) return false;

		ListProperty<?> thatP = (ListProperty<?>) that;
		return thatP._uuid.equals(this._uuid);
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"List[%s]\"/>", _name, _baseProperty.getType().getSimpleName());
	}
}
