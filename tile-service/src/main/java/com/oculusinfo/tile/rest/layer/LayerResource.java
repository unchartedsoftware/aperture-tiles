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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class LayerResource extends ServerResource {
	private static final Logger LOGGER = LoggerFactory.getLogger( LayerResource.class );

	private LayerService _service;

	@Inject
	public LayerResource( LayerService service ) {
		_service = service;
	}


	/**
	 * Returns the 'public' node of the layer configuration object, along with the layer id under
	 * the attribute 'id', and the layer meta data under the attribute 'meta'
	 *
	 * @param layerId The layer identification string.
	 */
	private JSONObject getLayerInformation( String layerId, String version ) throws JSONException {
		try {
			// full layer config
			JSONObject layerConfig = JsonUtilities.deepClone( _service.getLayerJSON( layerId ) );
			// get layer config factory
			LayerConfiguration config = _service.getLayerConfiguration( layerId, null );
			// we only need public section
			JSONObject layer = layerConfig.getJSONObject( "public" );
			// append id
			layer.put( "id", layerConfig.getString( "id" ) );
			// append version
			layer.put( "version", version );
			// get host
			String host = getRequest().getResourceRef().getPath();
			host = host.substring( 0, host.lastIndexOf( "layer" ) );
			// get layer metadata
			PyramidMetaData metaDataPyramid = _service.getMetaData( layerId );
			if ( metaDataPyramid != null ) {
				JSONObject metaData = JsonUtilities.deepClone( metaDataPyramid.getRawData() );
				// try to add images per tile to meta
				try {
					TileDataImageRenderer<?> renderer = config.produce( TileDataImageRenderer.class );
					if ( null != renderer ) {
						metaData.put( "imagesPerTile", renderer.getNumberOfImagesPerTile( metaDataPyramid ) );
					}
				} catch ( ConfigurationException e ) {
					// If we have to skip images per tile, it's not a huge deal
					LOGGER.warn( "Couldn't determine images per tile for layer {}", layerId, e );
				}
				// add meta data
				layer.put( "meta", metaData );
			}
			// set tms url with correct endpoint
			layer.put( "tms", host + config.getPropertyValue( LayerConfiguration.REST_ENDPOINT ) + "/" );
			return layer;
		} catch ( JSONException e ) {
			throw new JSONException( "Bad layer request, layer " + layerId + " does not exist" );
		} catch ( ConfigurationException e ) {
			throw new JSONException( "Bad layer request, layer " + layerId + " has no rest endpoint." );
		}
	}


	/**
	 * GET request. If {layerid} URN is specified, will return information for the single
	 * layer. If not, it will return information from all configured layers.
	 */
	@Get
	public Representation getLayer() {
		try {
			String version = ( String ) getRequest().getAttributes().get( "version" );
			if ( version == null ) {
				version = LayerConfiguration.DEFAULT_VERSION;
			}
			String layerURN = ( String ) getRequest().getAttributes().get( "layer" );
			JSONObject result = new JSONObject();
			if ( layerURN == null ) {
				// if not, return all layers
				JSONArray jsonLayers = new JSONArray();
				List<String> layerIds = _service.getLayerIds();
				for ( int i = 0; i < layerIds.size(); ++i ) {
					jsonLayers.put( i, getLayerInformation( layerIds.get( i ), version ) );
				}
				result.put( "layers", jsonLayers );
			} else {
				// if so, return specific layers
				result.put( "layer", getLayerInformation( layerURN, version ) );
			}
			setStatus( Status.SUCCESS_OK );
			result.put( "version", version );
			return new JsonRepresentation( result );
		} catch ( Exception e ) {
			throw new ResourceException( Status.SERVER_ERROR_INTERNAL,
				"Unable to configure layers from layer configuration JSON",
				e );
		}
	}
}
