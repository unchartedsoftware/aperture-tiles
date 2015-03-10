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
package com.oculusinfo.tile.rest.layer;


import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.json.JSONObject;
import java.util.List;



/**
 * The LayerService is the service driving the LayerResource, and used by many
 * other services and resources throughout the tile server. it keeps track of
 * available layers, and the configurations thereof.
 *
 * @author nkronenfeld, kbirk
 */
public interface LayerService {

    /**
     * Return a list of all layer configuration JSON objects.
     */
    public List< JSONObject > getLayerJSONs();

    /**
     * Return a specific layer configuration JSON object.
     * @param layerId The layer identification string
     */
    public JSONObject getLayerJSON( String layerId );

    /**
     * Return a list of layer identification strings
     */
    public List< String > getLayerIds();

    /**
     * Returns the layer configuration object for a given layer id. Request parameters
     * will override any default config attributes.
     * @param layerId The layer identification string
     * @param requestParams Additional query parameters to override
     */
    public LayerConfiguration getLayerConfiguration( String layerId, JSONObject requestParams );

    /**
     * Returns a SHA-256 hex string containing the state of the layer.
     * @param layerId The layer identification string
     * @param overrideConfiguration Additional query parameters to override
     */
    public String saveLayerState( String layerId, JSONObject overrideConfiguration ) throws Exception;

    /**
     * Returns a JSONObject containing the default configuration and all saved configurations
     * under the provided layer id.
     * @param layerId The layer identification string
     */
    public JSONObject getLayerStates( String layerId );

    /**
     * Returns a JSONObject containing the specific configuration state under the provided
     * layer id.
     * @param layerId The layer identification string
     * @param stateId The state identification string
     */
    public JSONObject getLayerState( String layerId, String stateId );

    /**
     * Returns the meta-data associated with the given layer.
     * @param layerId The layer identification string
     */
    public PyramidMetaData getMetaData (String layerId);
}
