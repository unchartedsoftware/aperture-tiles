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

public class StringProperty implements ConfigurationProperty<String> {
    private String   _name;
    private String   _description;
    private String   _defaultValue;
    private String[] _possibleValues;



    public StringProperty (String name, String description, String defaultValue) {
        _name = name;
        _description = description;
        _defaultValue = defaultValue;
        _possibleValues = null;
    }

    public StringProperty (String name, String description, String defaultValue, String[] possibleValues) {
        _name = name;
        _description = description;
        _defaultValue = defaultValue;
        _possibleValues = possibleValues;
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
}
