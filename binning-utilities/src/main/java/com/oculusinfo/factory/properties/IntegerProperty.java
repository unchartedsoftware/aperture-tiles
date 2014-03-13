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

import org.json.JSONException;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.JSONNode;

public class IntegerProperty implements ConfigurationProperty<Integer> {
    private String _name;
    private String _description;
    private int    _defaultValue;



    public IntegerProperty (String name, String description, int defaultValue) {
        _name = name;
        _description = description;
        _defaultValue = defaultValue;
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
    public Class<Integer> getType () {
        return Integer.class;
    }

    @Override
    public Integer[] getPossibleValues () {
        return null;
    }

    @Override
    public Integer getDefaultValue () {
        return _defaultValue;
    }

    @Override
    public String encode (Integer value) {
        return value.toString();
    }

    @Override
    public Integer unencode (String value) throws ConfigurationException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Unparsable int value "+value, e);
        }
    }

    @Override
    public void encodeJSON (JSONNode propertyNode, Integer value) throws JSONException {
        propertyNode.setAsInt(value.intValue());
    }

    @Override
    public Integer unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException {
        return propertyNode.getAsInt();
    }
}
