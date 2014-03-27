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

import java.awt.image.BufferedImage;
import java.util.UUID;

import oculus.aperture.common.rest.ApertureServerResource;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import com.google.inject.Inject;
import com.oculusinfo.tile.rest.ImageOutputRepresentation;

public class AnnotationResource extends ApertureServerResource {

	
	private AnnotationService _service;
	
	
	@Inject
	public AnnotationResource(AnnotationService service) {
		_service = service;
	}
	
	@Post("json")
	public Representation getLayer(String jsonData) throws ResourceException {

		try {
			
			JSONObject jsonObj = new JSONObject(jsonData);
			
			String host = getRequest().getResourceRef().getPath();
			
			host = host.substring(0, host.lastIndexOf("layer"));
			
			JSONObject layerInfo = _service.getLayer(host, jsonObj);
			
			return new JsonRepresentation(layerInfo);
			
		} catch (JSONException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to create JSON object from supplied options string", e);
		}
	}
	
	@Get
	public Representation getAnnotations() throws ResourceException {

		try {
			// No alternate versions supported. But if we did:
			//String version = (String) getRequest().getAttributes().get("version");
			String id = (String) getRequest().getAttributes().get("id");
			String layer = (String) getRequest().getAttributes().get("layer");
			String levelDir = (String) getRequest().getAttributes().get("level");
			int zoomLevel = Integer.parseInt(levelDir);
			String xAttr = (String) getRequest().getAttributes().get("x");
			int x = Integer.parseInt(xAttr);
			String yAttr = (String) getRequest().getAttributes().get("y");
			int y = Integer.parseInt(yAttr);
			
			UUID uuid = null;
			if( !"default".equals(id) ){ // Special indicator - no ID.
				uuid = UUID.fromString(id);
			}
		
		    // We return an object including the tile index ("index") and 
		    // the tile data ("data").
		    //
		    // The data should include index information, but it has to be 
		    // there for tiles with no data too, so we can't count on it.
		    JSONObject result = new JSONObject();
		    JSONObject tileIndex = new JSONObject();
		    tileIndex.put("level", zoomLevel);
		    tileIndex.put("xIndex", x);
		    tileIndex.put("yIndex", y);
		    result.put("index", tileIndex);
		    
		    TileIndex index = new TileIndex();
		    result.put("tile", _service.getTileObject(uuid, layer, zoomLevel, x, y));

		    setStatus(Status.SUCCESS_CREATED);
		    return new JsonRepresentation(result);

		} catch (Exception e){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to interpret requested tile from supplied URL.", e);
		}
	}
}
