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
package com.oculusinfo.tile.rest.layer;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * A representation of all information a server knows about a layer
 * 
 * This information is contained in the embeded JSON object; this class
 * basically functions as a wrapper around that JSON object to make access to it
 * from the server more consistent.
 * 
 * As such, the JSON object also contains any sub-layers, and can return them
 * wrapped in their own LayerInfo wrappers.
 * 
 * @author nkronenfeld
 */
public class LayerInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerInfo.class);



    private JSONObject          _data;
    private JSONArray           _axisConfiguration;
    private JSONArray           _parentalAxisConfiguration;
    private List<JSONObject>    _rendererConfigurations;
    private List<LayerInfo>     _children;



    public LayerInfo (JSONObject data) {
    	this(data, null);
    }

    protected LayerInfo (JSONObject data, JSONArray parentalAxisConfiguration) {
        _data = data;
        _parentalAxisConfiguration = parentalAxisConfiguration;
        _axisConfiguration = null;
        _rendererConfigurations = null;
        _children = null;
    }

    /*
     * Get our JSON form - only for use with client communications, through the
     * LayerResource.
     */
    JSONObject getRawData () {
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
            LOGGER.warn("Missing ID on layer {}", _data);
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
            LOGGER.warn("Missing name on layer {}", _data);
            return null;
        }
    }

    /**
     * Gets the specification of the X and Y axis of this and all sub-layers.
     * This may come from a parent layer info.
     */
    public JSONArray getAxisConfiguration () {
        if (null == _axisConfiguration) {
            synchronized (this) {
                if (null == _axisConfiguration) {
                    try {
                        _axisConfiguration = _data.getJSONArray("axes");
                    } catch (JSONException e) {
                        // If no axis configuration locally, use our parent's
                        _axisConfiguration = _parentalAxisConfiguration;
                    }
                }
            }
        }
        return _axisConfiguration;
    }

    /**
     * Gets a list of possible base renderer configurations that can be used
     * with this layer. The renderer listed in this configuration should not be
     * overridden; anything else may be.
     */
    public List<JSONObject> getRendererConfigurations () {
        if (null == _rendererConfigurations) {
            synchronized (this) {
                if (null == _rendererConfigurations) {
                    try {
                        JSONArray rawConfigurations = _data.getJSONArray("configurations");
                        _rendererConfigurations = new ArrayList<>();
                        for (int i = 0; i < rawConfigurations.length(); ++i) {
                            _rendererConfigurations.add(rawConfigurations.getJSONObject(i));
                        }
                    } catch (JSONException e) {
                        // A lack of configurations is allowed
                        _rendererConfigurations = Collections.emptyList();
                    }
                }
            }
        }
        return _rendererConfigurations;
    }

    /**
     * Gets a list fo layer infos organized under this one. The primary
     * practical use for this is to indicate shared axes - the parent would
     * specify the axes' specification, while the children would list
     * individually the layers that can be shown with those axes - but it can
     * also be used simply for organization, to help in presenting layers to the
     * user in a logical manner.
     */
    public List<LayerInfo> getChildren () {
        if (null == _children) {
            synchronized (this) {
                if (null == _children) {
                    try {
                        JSONArray rawChildren = _data.getJSONArray("children");
                        _children = new ArrayList<>();
                        for (int i = 0; i < rawChildren.length(); ++i) {
                            _children.add(new LayerInfo(
                                                        rawChildren.getJSONObject(i),
                                                        getAxisConfiguration()));
                        }
                    } catch (JSONException e) {
                        // A lack of children is allowed.
                        _children = Collections.emptyList();
                    }
                }
            }
        }
        return _children;
    }
}
