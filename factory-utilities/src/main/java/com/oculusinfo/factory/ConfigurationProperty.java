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
package com.oculusinfo.factory;

import org.json.JSONException;

/**
 * This class provides a description of a property to be read from JSON files or
 * java property files, and used in configuring manufacture of a needed object
 * by some factory.
 * 
 * @param <T> The type of object provided
 * 
 * @author nkronenfeld
 */
public interface ConfigurationProperty<T> {
	public String getName ();

	public String getDescription ();

	public Class<T> getType ();

	public T[] getPossibleValues ();

	public T getDefaultValue ();

	/**
	 * Give a string version of a value for this property.
	 */
	public String encode (T value);

	/**
	 * Convert a string version of a value for this property back into the
	 * proper value type.
	 */
	public T unencode (String string) throws ConfigurationException;

	/**
	 * <p>
	 * Store a value for this property into the factory's JSON node. Typically,
	 * this can be just
	 * </p>
	 * 
	 * <code>propertyNode.setAsString(encode(value))</code>
	 * 
	 * <p>
	 * but in the case of complicated property types, this can be made more
	 * complex.
	 * </p>
	 * 
	 * @param propertyNode The type-independent representation of the location
	 *            in the JSON tree with the value of this property.
	 * @param value The value to set
	 * @throws JSONException
	 */
	public void encodeJSON (JSONNode propertyNode, T value) throws JSONException;

	/**
	 * <p>
	 * Read a value for this property from the factory's JSON node. Typically,
	 * this can be just
	 * </p>
	 * 
	 * <code>return unencode(propertyNode.getAsString())</code>
	 * 
	 * <p>
	 * but in the case of complicated property types, this can be made more
	 * complex.
	 * </p>
	 * 
	 * If a value is missing, the JSONException can be passed through - if a
	 * JSONException is thrown, the default value will be used automatically.
	 * 
	 * @param propertyNode The type-independent representation fo the location
	 *            in the JSON tree with the value of this property.
	 */
	public T unencodeJSON (JSONNode propertyNode) throws JSONException, ConfigurationException;
}
