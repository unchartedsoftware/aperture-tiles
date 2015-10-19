/*
 * Copyright (c) 2015 Uncharted Software
 * https://uncharted.software/
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
package com.oculusinfo.tile.rest.translation;

import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.inject.Inject;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.QueryParamDecoder;

public class TileTranslationResource extends ServerResource {

	private TileTranslationService _service;


	@Inject
	public TileTranslationResource(TileTranslationService service) {
		this._service = service;
	}


	@Post("json")
	public Representation translate(JsonRepresentation jsonRepresentation) throws ResourceException {
		try {
			// No alternate versions supported. But if we did:
			String version = (String) getRequest().getAttributes().get("version");
			if ( version == null ) {
				version = LayerConfiguration.DEFAULT_VERSION;
			}
			JSONObject arguments = jsonRepresentation.getJsonObject();
			return new JsonRepresentation( _service.getTranslation(arguments) );

		} catch ( Exception e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
				"Unable to translate text. Check parameters.", e);
		}
	}
    /**
     * GET request. Returns a JSON response from the translation service specified
     */
	@Get
	public Representation translate() throws ResourceException {
		// get the params from
		try {
			// No alternate versions supported. But if we did:
			String version = (String) getRequest().getAttributes().get("version");
            if ( version == null ) {
                version = LayerConfiguration.DEFAULT_VERSION;
            }
            // decode and build JSONObject from request parameters
            JSONObject decodedQueryParams = QueryParamDecoder.decode( getRequest().getResourceRef().getQuery() );
            return new JsonRepresentation( _service.getTranslation(decodedQueryParams) );

		} catch ( Exception e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			                            "Unable to translate text. Check parameters.", e);
		}
	}
}
