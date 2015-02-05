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

import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.JSONNode;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.UUID;

/**
 * Created by nkronenfeld on 1/7/2015.
 */
public class PairProperty<K, V> implements ConfigurationProperty<Pair<K, V>> {

    private String _name;
	private String _description;
	private ConfigurationProperty<K> _keyDescriptor;
	private ConfigurationProperty<V> _valueDescriptor;
	private Pair<K, V> _defaultValue;
	private String _uuid;

	public PairProperty(ConfigurationProperty<K> keyDescriptor, ConfigurationProperty<V> valueDescriptor,
	                    String name, String description,
	                    Pair<K, V> defaultValue) {
		_keyDescriptor = keyDescriptor;
		_valueDescriptor = valueDescriptor;
		_name = name;
		_description = description;
		_defaultValue = defaultValue;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class<Pair<K, V>> getType () {
		return (Class) Pair.class;
	}

	@Override
	public Pair<K, V>[] getPossibleValues () {
		return null;
	}

	@Override
	public Pair<K, V> getDefaultValue () {
		return _defaultValue;
	}

	private String escape (String value) {
		return value.replace("\\", "\\\\").replace(",", "\\,");
	}
	@Override
	public String encode (Pair<K, V> value) {
		// escape internal commas, so when we separate, we can tell them from our delimiting comma
		String keyCode = escape(_keyDescriptor.encode(value.getFirst()));
		String valueCode = escape(_valueDescriptor.encode(value.getSecond()));
		return "("+keyCode+", "+valueCode+")";
	}

	private String unescape (String value) {
		return value.replace("\\,", ",").replace("\\\\", "\\");
	}
	@Override
	public Pair<K, V> unencode (String value) throws ConfigurationException {
		// Find our split location
		int commaIndex = -1;
		boolean found = false;
		do {
			commaIndex = value.indexOf(',', commaIndex+1);
			int firstEscape = commaIndex;
			while (firstEscape > 0 && value.charAt(firstEscape-1) == '\\')
				firstEscape--;
			int numEscapes = commaIndex - firstEscape;
			if (0 == (numEscapes%2)) {
				found = true;
			}
		} while (!found && -1 != commaIndex);

		// Split there
		String keyCode = value.substring(1, commaIndex);
		String valueCode = value.substring(commaIndex+2, value.length()-1);
		return new Pair<K, V>(_keyDescriptor.unencode(unescape(keyCode)),
		                      _valueDescriptor.unencode(unescape(valueCode)));
	}

	@Override
	public void encodeJSON (JSONNode propertyNode, Pair<K, V> value) throws JSONException {
		JSONArray a = new JSONArray();
		_keyDescriptor.encodeJSON(new JSONNode(a, 0), value.getFirst());
		_valueDescriptor.encodeJSON(new JSONNode(a, 1), value.getSecond());
	}

	@Override
	public Pair<K, V> unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
		K key = _keyDescriptor.unencodeJSON(propertyNode.getIndexedChildNode(0));
		V value = _valueDescriptor.unencodeJSON(propertyNode.getIndexedChildNode(1));
		return new Pair<K, V>(key, value);
	}

	@Override
	public int hashCode () {
		return _uuid.hashCode();
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof PairProperty)) return false;

		PairProperty<?, ?> thatP = (PairProperty<?, ?>) that;
		return thatP._uuid.equals(this._uuid);
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"pair<%s, %s>\"/>",
		                     _name, _keyDescriptor.getName(), _valueDescriptor.getName());
	}
}
