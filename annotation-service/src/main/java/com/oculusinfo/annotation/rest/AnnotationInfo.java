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
package com.oculusinfo.annotation.rest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationInfo.class);

    private JSONObject _data;
    private JSONObject _dataConfiguration;
    private JSONObject _pyramidConfiguration;
    private JSONArray _groups;
    private JSONObject _filterConfiguration;
    
	public AnnotationInfo( JSONObject data ) {
		_data = data;
	}
	
	/*
     * Get our JSON form - only for use with client communications, through the
     * LayerResource.
     */
    public JSONObject getRawData () {
        return _data;
    }

    /**
     * Get the ID by which the server knows this layer. This is what must be
     * passed back to the server for any layer request requiring an ID.
     */
    public String getID () {
        try {
            return _data.getString("id");
        } catch (JSONException e) {
            LOGGER.warn("Missing ID on annotation {}", _data);
            return null;
        }
    }

    /**
     * Get the human-readable name of the layer, for use by the client in
     * presenting the layer to the user.
     */
    public String getName () {
        try {
            return _data.getString("name");
        } catch (JSONException e) {
            LOGGER.warn("Missing name on annotation {}", _data);
            return null;
        }
    }

    public JSONArray getGroups () {
        if (null == _groups) {
            synchronized (this) {
                if (null == _groups) {
                    try {
                    	_groups = _data.getJSONArray("groups");
                    } catch (JSONException e) {
                    	LOGGER.warn("Missing priorities on annotation {}", _data);
                        return null;
                    }
                }
            }
        }
        return _groups;
    }
    
    
    public JSONObject getFilterConfiguration () {
        if (null == _filterConfiguration) {
            synchronized (this) {
                if (null == _filterConfiguration) {
                    try {
                    	_filterConfiguration = _data.getJSONObject("filter");
                    } catch (JSONException e) {
                    	LOGGER.warn("Missing filter configuration on annotation {}", _data);
                        return null;
                    }
                }
            }
        }
        return _filterConfiguration;
    }
    
    
    public JSONObject getPyramidConfiguration () {
        if (null == _pyramidConfiguration) {
            synchronized (this) {
                if (null == _pyramidConfiguration) {
                    try {
                    	_pyramidConfiguration = _data.getJSONObject("pyramid");
                    } catch (JSONException e) {
                    	LOGGER.warn("Missing pyramid configuration on annotation {}", _data);
                        return null;
                    }
                }
            }
        }
        return _pyramidConfiguration;
    }

    
    public JSONObject getDataConfiguration () {
        if (null == _dataConfiguration) {
            synchronized (this) {
                if (null == _dataConfiguration) {
                    try {
                        _dataConfiguration = _data.getJSONObject("data");
                    } catch (JSONException e) {
                        // If no data configuration, we're toast!
                        LOGGER.error("No data configuration for layer "+getName());
                        return null;
                    }
                }
            }
        }
        return _dataConfiguration;
    }

}
