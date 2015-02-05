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

public class UUIDProperty implements ConfigurationProperty<UUID> {

    private String _name;
	private String _description;
	private String _uuid;

	public UUIDProperty (String name, String description) {
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
	public Class<UUID> getType () {
		return UUID.class;
	}

	@Override
	public UUID[] getPossibleValues () {
		// UUIDs are inherently uninnumerable
		return null;
	}

	@Override
	public UUID getDefaultValue () {
		// Being unique, UUIDs should never have a default value
		return null;
	}

	@Override
	public String encode (UUID value) {
		long msb = value.getMostSignificantBits();
		long lsb = value.getLeastSignificantBits();
		return String.format("%s%x:%s%x",
		                     (msb >= 0 ? "" : "-"), Math.abs(msb),
		                     (lsb >= 0 ? "" : "-"), Math.abs(lsb));
	}

	@Override
	public UUID unencode (String value) throws ConfigurationException {
		try {
			String[] parts = value.split(":");
			long msb = Long.parseLong(parts[0], 16);
			long lsb = Long.parseLong(parts[1], 16);
			return new UUID(msb, lsb);
		} catch (NullPointerException|NumberFormatException|ArrayIndexOutOfBoundsException e) {
			throw new ConfigurationException("Error parsing stored UUID", e);
		}
	}

	@Override
	public void encodeJSON (JSONNode propertyNode, UUID value) throws JSONException {
		propertyNode.setAsString(encode(value));
	}

	@Override
	public UUID unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
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
		if (!(that instanceof UUIDProperty)) return false;

		UUIDProperty thatP = (UUIDProperty) that;
		return thatP._uuid.equals(this._uuid);
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"UUID\"/>", _name);
	}
}
