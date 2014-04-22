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
package com.oculusinfo.tile.rest;

/**
 * Interface for retrieving a request parameter that was passed to the data
 * query like in a "GET" request. This should act like a set of key/value
 * pairs.
 * 
 * @author cregnier
 *
 */
public interface RequestParams {

	/**
	 * Retrieves the value associated with the key name.
	 * @param name
	 * 	The name of a key within the query
	 * @return
	 * 	Returns the value associated with the key, or null if none exists. 
	 */
	public String getValue(String name);
	
	/**
	 * Retrieves the value associated with the key name or else it uses the default
	 * value.
	 * @param name
	 * 	The name of a key within the query
	 * @param defaultVal
	 * 	The default value to return if the key doesn't exist.
	 * @return
	 * 	Returns the value associated with the key, or the default value if none exists. 
	 */
	public String getValueOrElse(String name, String defaultVal);
	
}
