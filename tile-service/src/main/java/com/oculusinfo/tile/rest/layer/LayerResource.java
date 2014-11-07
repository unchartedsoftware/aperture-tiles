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
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;



public class LayerResource extends ApertureServerResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerResource.class);

    private LayerService        _service;

    @Inject
    public LayerResource (LayerService service) {
        _service = service;
    }


    private JSONObject getLayerInformation( String layerId ) throws JSONException {
        try {
            // clone layer config
            JSONObject layer = JsonUtilities.deepClone( _service.getLayerJSON( layerId ) );
            // get host
            String host = getRequest().getResourceRef().getPath();
            host = host.substring( 0, host.lastIndexOf("layer") );
            // get layer metadata
            PyramidMetaData metaDataPyramid = _service.getMetaData( layerId );
            JSONObject metaData = JsonUtilities.deepClone( metaDataPyramid.getRawData() );
            // try to add images per tile to meta
            try {
                LayerConfiguration config = _service.getLayerConfiguration( layerId, null );
                TileDataImageRenderer renderer = config.produce( TileDataImageRenderer.class );
                if (null != renderer) {
                    metaData.put("imagesPerTile", renderer.getNumberOfImagesPerTile( metaDataPyramid ));
                }
            } catch (ConfigurationException e) {
                // If we have to skip images per tile, it's not a huge deal
                LOGGER.warn("Couldn't determine images per tile for layer {}", layerId, e);
            }
            layer.put("meta", metaData );
            layer.put("tms", host + "tile/");
            layer.put("apertureservice", "/tile/");
            return layer;
        } catch (JSONException e) {
            throw new JSONException( "Bad layer request, layer " + layerId + " does not exist" );
        }
    }


    @Get
    public Representation getLayer() {
        try {
            // see if resource is specified
            String layerURN = (String) getRequest().getAttributes().get("layer");
            JSONObject jsonLayers = new JSONObject();
            if ( layerURN == null ) {
                 // if not, return all layers
                List<String> layerIds = _service.getLayerIds();
                for (int i=0; i<layerIds.size(); ++i) {
                    jsonLayers.put( layerIds.get(i), getLayerInformation( layerIds.get(i) ) );
                }
            } else {
                 // if so, return specific layers
                jsonLayers = getLayerInformation( layerURN );
            }
            setStatus(Status.SUCCESS_OK);
            return new JsonRepresentation( jsonLayers );
        } catch (JSONException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                                        "Unable to create JSON object from supplied options string",
                                        e);
        }
    }
}
