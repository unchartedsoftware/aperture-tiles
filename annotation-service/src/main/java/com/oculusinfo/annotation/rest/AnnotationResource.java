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
import java.util.List;
import java.util.UUID;

import oculus.aperture.common.rest.ApertureServerResource;

import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.impl.*;
import com.oculusinfo.binning.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;

public class AnnotationResource extends ApertureServerResource {
	
	private AnnotationService _service;
	
	@Inject
	public AnnotationResource( AnnotationService service ) {
		_service = service;
	}
	

	@Post("json")
	public Representation postAnnotation( String jsonData ) throws ResourceException {

		try {
			JSONObject json = new JSONObject(jsonData);

			_service.writeAnnotation( json.getString("layer"), new JSONAnnotation( json.getJSONObject("annotation") ) );
			
			return new JsonRepresentation(json);
			
		} catch (JSONException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to create JSON object from supplied options string", e);
		}
	}
	
	
	@Get
	public Representation getAnnotation() throws ResourceException {

		try {
			
			// No alternate versions supported. But if we did:
			//String version = (String) getRequest().getAttributes().get("version");
			String layer = (String) getRequest().getAttributes().get("layer");
			String levelDir = (String) getRequest().getAttributes().get("level");
			int zoomLevel = Integer.parseInt(levelDir);
			String xAttr = (String) getRequest().getAttributes().get("x");
			int x = Integer.parseInt(xAttr);
			String yAttr = (String) getRequest().getAttributes().get("y");
			int y = Integer.parseInt(yAttr);
			
			/*
			UUID uuid = null;
			if( !"default".equals(id) ){ // Special indicator - no ID.
				uuid = UUID.fromString(id);
			}
			*/
		
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
		    result.put("index", tileIndex );
		    TileIndex index = new TileIndex( zoomLevel, x, y, AnnotationTile.NUM_BINS, AnnotationTile.NUM_BINS );
		    
		    List<AnnotationData> data = _service.readAnnotations( layer, index );

		    JSONArray dataArray = new JSONArray();
		    for ( AnnotationData d : data ) {
		    	System.out.println("\n\n\n" + d.toJSON().toString() + "\n\n\n");
		    	dataArray.put( d.toJSON() );
		    }
		    
		    result.put("annotations", dataArray );

		    setStatus(Status.SUCCESS_CREATED);
		    return new JsonRepresentation( result );

		} catch (Exception e){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to interpret requested tile from supplied URL.", e);
		}
	}
}
