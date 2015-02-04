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

public class DoubleProperty implements ConfigurationProperty<Double> {

    private String _name;
	private String _description;
	private double _defaultValue;
	private String _uuid;

	public DoubleProperty (String name, String description, double defaultValue) {
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

	@Override
	public Class<Double> getType () {
		return Double.class;
	}

	@Override
	public Double[] getPossibleValues () {
		return null;
	}

	@Override
	public Double getDefaultValue () {
		return _defaultValue;
	}

	@Override
	public String encode (Double value) {
		return value.toString();
	}

	@Override
	public Double unencode (String value) throws ConfigurationException {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new ConfigurationException("Unparsable double value "+value, e);
		}
	}

	@Override
	public void encodeJSON (JSONNode propertyNode, Double value) throws JSONException {
		propertyNode.setAsDouble(value.doubleValue());
	}

	@Override
	public Double unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
		return propertyNode.getAsDouble();
	}

	@Override
	public int hashCode () {
		return _uuid.hashCode();
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof DoubleProperty)) return false;
        
		DoubleProperty thatP = (DoubleProperty) that;
		return thatP._uuid.equals(this._uuid);
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"double\"/>", _name);
	}
}
