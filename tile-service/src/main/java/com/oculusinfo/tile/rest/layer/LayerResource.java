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


import com.google.inject.Inject;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.JsonUtilities;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import oculus.aperture.common.rest.ApertureServerResource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;



public class LayerResource extends ApertureServerResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerResource.class);



    private LayerService        _service;



    @Inject
    public LayerResource (LayerService service) {
        _service = service;
    }

    /**
     * <p>
     * Fulfill a request relating to layer setup.
     * </p>
     * 
     * <p>
     * The type of request should be in the requestType parameter. Possible
     * request types (specified in the <b>request</b> property of the post data)
     * are:
     * <dl>
     * <dt>
     * list</dt>
     * <dd>
     * <p>
     * Returns a list of hierarchically organized layers that are available to
     * the client. This request has no parameters. The following properties may
     * be at each level of the tree:
     * <dl>
     * <dt>id</dt>
     * <dd>The ID by which the server knows this layer. This is what must be
     * passed back to the server for any layer request requiring an ID.</dd>
     * <dt>name</dt>
     * <dd>The human-readable name of the layer, for use by the client in
     * presenting the layer to the user.</dd>
     * <dt>axes</dt>
     * <dd>An object specifying the X and Y axis of this and all sub-layers.
     * Only one axis set can pertain to a given leaf node - so if a given level
     * specifies this property, no lower levels will</dd>
     * <dt>children</dt>
     * <dd>The children of this level - any layers grouped under this label</dd>
     * <dt>configurations</dt>
     * <dd>A list of possible renderer configurations that can be used with this
     * layer. These renderer configurations should form the basis of any
     * configuration request made by the client.</dd>
     * </dl>
     * Only final leaf nodes are true layers - all higher entries are
     * placeholders for organizational purposes only.</dd>
     * 
     * <dt>metadata</dt>
     * <dd>
     * retrieves the metadata for the specified layer.  This request has one input parameter:
     * <dl>
     * <dt>layer</dt>
     * <dd>The ID of the layer whose metadata is desired.</dd>
     * </dl>
     * </dd>
     * 
     * <dt>
     * configure</dt>
     * <dd>
     * Configure a layer for display by the client. This request has one input
     * parameters:
     * <dl>
     * <dt>configuration</dt>
     * <dd>An object specifying the configuration to be applied to said layer,
     * defining how it will be rendered. This configuration includes the layer
     * name.</dd>
     * </dl>
     * A <em>configure</em> request will return a string containing a UUID with
     * which the client can later reference this particular configuration</dd>
     * 
     * <dt>unconfigure</dt>
     * <dd>Tells the server to forget a particular configuration, as it will no
     * longer be needed. This request has but one parameter:
     * <dl>
     * <dt>configuration</dt>
     * <dd>The UUID of the configuration to be forgotten (as returned from the
     * corresponding <em>configure</em> request)</dd>
     * </dl>
     * </dd>
     * </dl>
     * </p>
     */
    @Post("json:json")
    public Representation layerRequest (String jsonArguments) {
        try {
            JSONObject arguments = new JSONObject(jsonArguments);

            String requestType = arguments.getString("request");

            if (null == requestType) {
                throw new IllegalArgumentException("No request type given to layer request.");
            }
            requestType = requestType.toLowerCase();

            if ("list".equals(requestType)) {
                // Layer list request
                List<LayerInfo> layers = _service.listLayers();
                JSONArray jsonLayers = new JSONArray();
                for (int i=0; i<layers.size(); ++i) {
                    jsonLayers.put(i, layers.get(i).getRawData());
                }
                return new JsonRepresentation(jsonLayers);
            } else if ("metadata".equals(requestType)) {
                // Metadata request
                String layer = arguments.getString("layer");
                PyramidMetaData metaData = _service.getMetaData(layer);
                if (null == metaData) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unknown layer "+layer);
                } else {
                    return new JsonRepresentation(metaData.getRawData());
                }
            } else if ("configure".equals(requestType)) {
                // Configuration request
                String layerId = arguments.getString("layer");
                JSONObject configuration = arguments.getJSONObject("configuration");
                UUID uuid = _service.configureLayer(layerId, configuration);
                String host = getRequest().getResourceRef().getPath();
                host = host.substring(0, host.lastIndexOf("layer"));

                // Figure out the number of images per tile
                PyramidMetaData metaData = _service.getMetaData(layerId);

                JSONObject result = JsonUtilities.deepClone(metaData.getRawData());
                result.put("layer", layerId);
                result.put("id", uuid);
                result.put("tms", host + "tile/" + uuid.toString() + "/");
                result.put("apertureservice", "/tile/" + uuid.toString() + "/");
                try {
                    LayerConfiguration config = _service.getRenderingConfiguration(uuid, null, null);
                    TileDataImageRenderer renderer = config.produce(TileDataImageRenderer.class);
                    if (null != renderer) {
                        result.put("imagesPerTile", renderer.getNumberOfImagesPerTile(metaData));
                    }
                } catch (ConfigurationException e) {
                    // If we have to skip images per tile, it's not a huge deal
                    LOGGER.info("Couldn't determine images per tile for layer {}", layerId, e);
                }

                return new JsonRepresentation(result);
            } else if ("unconfigure".equals(requestType)) {
                String uuidRep = arguments.getString("configuration");
                UUID uuid = UUID.fromString(uuidRep);
                _service.forgetConfiguration(uuid);
            } else {
                throw new IllegalArgumentException("Illegal request type "+requestType);
            }
            return null; // TODO: remove, placeholder until the above is done.
        } catch (JSONException e) {
            LOGGER.warn("Bad layers request: {}", jsonArguments, e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "Unable to create JSON object from supplied options string",
                                        e);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Error in layer request {}", jsonArguments, e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "Error in layer request: "+e.getMessage(),
                                        e);
        }
    }
}
