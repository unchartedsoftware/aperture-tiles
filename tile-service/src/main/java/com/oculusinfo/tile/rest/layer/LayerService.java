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



import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.tile.rendering.LayerConfiguration;



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
    public List<LayerInfo> listLayers ();

    /**
     * Get the meta-data associated with the given layer (which must be listed
     * by {@link #listLayers()})
     */
    public PyramidMetaData getMetaData (String layerId);

    /**
     * Configure a layer for rendering.
     * 
     * @param configuration
     *            The configuration of the layer to set.
     * @return A unique UUID by which this configuration should be known.
     */
    public UUID configureLayer (JSONObject configuration);

    /**
     * For use by other services; the LayerResource doesn't serve this out.
     * 
     * @param layer
     *            the layer to be rendered
     * @param tile
     *            The level to be rendered. A negative value indicates that the
     *            configuration should not be specialized for any particular
     *            layer.
     */
    public LayerConfiguration getRenderingConfiguration (String layerId, int level);

    /**
     * Indicates to the service that all users are done with a given
     * configuration.
     * 
     * @param uuid
     *            The id of the configuration that is no longer needed.
     */
    public void forgetConfiguration (UUID uuid);
}
