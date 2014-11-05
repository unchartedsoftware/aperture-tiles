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


import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;



/**
 * The LayerService is the service driving the LayerResource, and used by many
 * other services and resources throughout the tile server. it keeps track of
 * available layers, and the configurations thereof.
 * 
 * @author nkronenfeld
 */
public interface LayerService {
    /**
     * List all available layers. See {@link LayerResource#layerRequest(String)}
     * for details (though this method returns a list of java objects, rather
     * than a pile of JOSN).
     * 
     * @return
     */
    public List< JSONObject > getLayerConfigs();


    public List< String > getLayers();

    /**
     * For use by other services; the LayerResource doesn't serve this out.
     *
     * Gets a configuration object to be used when rendering a layer.
     *
     * @param layer The layer to be rendered
     * @param requestParams Additional query parameters to override
     */
    public LayerConfiguration getLayerConfiguration( String layer, JSONObject requestParams );

    /**
     * Get the meta-data associated with the given layer (which must be listed
     * by {@link #listLayers()})
     */
    public PyramidMetaData getMetaData (String layerId);
}
