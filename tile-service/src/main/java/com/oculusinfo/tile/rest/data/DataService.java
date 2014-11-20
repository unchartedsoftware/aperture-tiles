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
package com.oculusinfo.tile.rest.data;



import org.json.JSONObject;



/**
 * A simple service for direct retrieval of data from the server.
 * 
 * @author nkronenfeld
 * 
 */
public interface DataService {
	/**
	 * Get raw data based on the input arguments.
	 * 
	 * @param dataset A description of the dataset to query
	 * @param query A description of which data are desired.
	 * @param getCount True if a count of the total records matching the query
	 * @param getData True if data records should be retrieved; if false,
	 *            getCount should be true, and matching records are counted but
	 *            not returned.
	 * @param requestCount The maximum number of records to return, if getData
	 *            is true. If getData is false, this parameter is ignored.
	 */
	JSONObject getData (JSONObject dataset, JSONObject query, boolean getCount,
	                    boolean getData, int requestCount);
}
