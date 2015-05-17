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
import com.oculusinfo.tile.rendering.LayerConfiguration;
import org.json.JSONObject;
import org.restlet.data.CacheDirective;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateResource extends ServerResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateResource.class);

    private LayerService _service;

    @Inject
    public StateResource( LayerService service ) {
        _service = service;
    }

    /**
     * GET request. Returns all configuration states under a particular layer, including default.
     */
    @Get
    public Representation getStates() {
        try {
            String version = (String) getRequest().getAttributes().get("version");
            if ( version == null ) {
                version = LayerConfiguration.DEFAULT_VERSION;
            }
            String layerId = (String) getRequest().getAttributes().get("layer");
            String stateId = (String) getRequest().getAttributes().get("state");
            JSONObject result = new JSONObject();
            if ( stateId != null ) {
                result.put( "state", _service.getLayerState( layerId, stateId ) );
            } else {
                result.put( "states", _service.getLayerStates( layerId ) );
            }
            result.put( "version", version );
            setStatus( Status.SUCCESS_OK );
            getResponse().getCacheDirectives().add( CacheDirective.noCache() );
            return new JsonRepresentation( result );
        } catch ( Exception e ) {
            LOGGER.warn("Bad layer states request: ", e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "Unable to recognize layer id",
                                        e);
        }
    }

    /**
     * POST request. Saves a configuration state on the server. Returns a deterministic
     * SHA-256 hash of the layer configuration.
     */
    @Post
    public JsonRepresentation saveState( String jsonArguments ) {
        try {
            String version = (String) getRequest().getAttributes().get("version");
            if ( version == null ) {
                version = LayerConfiguration.DEFAULT_VERSION;
            }
            String layerId = (String) getRequest().getAttributes().get("layer");
            JSONObject arguments = new JSONObject( jsonArguments );
            String sha = _service.saveLayerState( layerId, arguments );
            JSONObject result = new JSONObject();
            result.put( "state", sha );
            result.put( "version", version );
            setStatus( Status.SUCCESS_CREATED );
            return new JsonRepresentation( result );
        } catch ( Exception e ) {
            LOGGER.warn("Bad layer states request: {}", jsonArguments, e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "Unable to save configuration state",
                                        e);
        }
    }
}
