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
package com.oculusinfo.binning.properties;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.JSONNode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class TileIndexProperty implements ConfigurationProperty<TileIndex> {

    private String _name;
	private String _description;
	private TileIndex _defaultValue;
	private String _uuid;

	public TileIndexProperty (String name, String description, TileIndex defaultValue) {
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
	public Class<TileIndex> getType () {
		return TileIndex.class;
	}

	@Override
	public TileIndex[] getPossibleValues () {
		return null;
	}

	@Override
	public TileIndex getDefaultValue () {
		return _defaultValue;
	}

	@Override
	public String encode (TileIndex value) {
		if (null == value) return "null";
		return value.toString();
	}

	@Override
	public TileIndex unencode (String value) throws ConfigurationException {
		return TileIndex.fromString(value);
	}

	@Override
	public void encodeJSON (JSONNode propertyNode, TileIndex value) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("level", value.getLevel());
		result.put("xIndex", value.getX());
		result.put("yIndex", value.getY());
		result.put("xBinCount", value.getXBins());
		result.put("yBinCount", value.getYBins());
		propertyNode.setAsJSONObject(result);
	}

	@Override
	public TileIndex unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
		JSONObject propertyObj = propertyNode.getAsJSONObject();
		int level = propertyObj.getInt("level");
		int xIndex = propertyObj.getInt("xIndex");
		int yIndex = propertyObj.getInt("yIndex");
		int numXBins = propertyObj.getInt("xBinCount");
		int numYBins= propertyObj.getInt("yBinCount");

		return new TileIndex(level, xIndex, yIndex, numXBins, numYBins);
	}

	@Override
	public int hashCode () {
		return _uuid.hashCode();
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof TileIndexProperty)) return false;
        
		TileIndexProperty thatP = (TileIndexProperty) that;
		return thatP._uuid.toLowerCase().equals(this._uuid.toLowerCase());
	}

	@Override
	public String toString () {
		return String.format("<property name=\"%s\" type=\"tile index\"/>");
	}
}
