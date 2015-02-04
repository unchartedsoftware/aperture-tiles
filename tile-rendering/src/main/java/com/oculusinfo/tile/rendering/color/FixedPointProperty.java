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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rendering.color;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.JSONNode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class FixedPointProperty implements ConfigurationProperty<FixedPoint> {
	private String _name;
	private String _description;
	private String _uuid;

	public FixedPointProperty (String name, String description) {
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
	public Class<FixedPoint> getType () {
		return FixedPoint.class;
	}

	@Override
	public FixedPoint[] getPossibleValues () {
		// Describing continuous values; no enumeration possible.
		return null;
	}

	@Override
	public FixedPoint getDefaultValue () {
		// Similarly, no default value possible.
		return null;
	}

	@Override
	public String encode (FixedPoint value) {
		return String.format("%fby%f", value.getScale(), value.getValue());
	}

	@Override
	public FixedPoint unencode (String string) throws ConfigurationException {
		String[] parts = string.split("by");
		return new FixedPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
	}

	@Override
	public void encodeJSON (JSONNode propertyNode, FixedPoint value) throws JSONException {
		JSONObject jsonValue = new JSONObject();
		jsonValue.put("scale", value.getScale());
		jsonValue.put("value", value.getValue());
		propertyNode.setAsJSONObject(jsonValue);
	}

	@Override
	public FixedPoint unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
		JSONObject node = propertyNode.getAsJSONObject();
		return new FixedPoint(node.getDouble("scale"), node.getDouble("value"));
	}

	@Override
	public int hashCode () {
		return _uuid.hashCode();
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof FixedPointProperty)) return false;
        
		FixedPointProperty thatP = (FixedPointProperty) that;
		return thatP._uuid.equals(this._uuid);
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"fixed point\"/>", _name);
	}
}
