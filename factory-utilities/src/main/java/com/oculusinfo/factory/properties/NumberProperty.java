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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

import org.json.JSONException;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.JSONNode;

/**
 * A property type that can read any numeric type, and does so holding accuracy as most important, then size.
 * 
 * @author nkronenfeld
 */
public class NumberProperty implements ConfigurationProperty<Number> {

    private String _name;
    private String _description;
    private Number _defaultValue;
    private String _uuid;

    public NumberProperty (String name, String description, Number defaultValue) {
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
    public Class<Number> getType () {
        return Number.class;
    }

    @Override
    public Number[] getPossibleValues () {
        return null;
    }

    @Override
    public Number getDefaultValue () {
        return _defaultValue;
    }

    @Override
    public String encode (Number value) {
        return value.toString();
    }

    @Override
    public Number unencode (String value) throws ConfigurationException {
        // A bit slow, but it's only configuration reading.
        if (value.contains("."))
        try {
            return Byte.parseByte(value);
        } catch (NumberFormatException e) {
            // Fall through to the next size
        }
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            // Fall through to the next size
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Fall through to the next size
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // Fall through to the next size
        }
        try {
            return new BigInteger(value);
        } catch (NumberFormatException e2) {
            // Fall through to rationals
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            // Fall through to the next size
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Fall through to the next size
        }
        // Don't catch exceptions here - no other options
        return new BigDecimal(value);
    }

    @Override
    public void encodeJSON (JSONNode propertyNode, Number value) throws JSONException {
        propertyNode.setAsString(encode(value));
    }

    @Override
    public Number unencodeJSON (JSONNode propertyNode) throws JSONException,
                                                      ConfigurationException {
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
        if (!(that instanceof NumberProperty)) return false;
        
        NumberProperty thatP = (NumberProperty) that;
        return thatP._uuid.equals(this._uuid);
    }

    @Override
    public String toString () {
        return String.format("<property name=\"%s\" type=\"number\"/>", _name);
    }
}
