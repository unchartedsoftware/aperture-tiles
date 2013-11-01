/**
 * Copyright (c) 2013 Oculus Info Inc.
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
package com.oculusinfo.tile.rest;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import oculus.aperture.common.rest.ApertureServerResource;
import com.oculusinfo.tile.spi.ImageTileService;

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

public class ImageTileResource extends ApertureServerResource {
	
	private static Map<String, MediaType> MEDIA_TYPES = new HashMap<String, MediaType>();
	static{
		MEDIA_TYPES.put("png", MediaType.IMAGE_PNG);  // Should be an easier way to do this...
		MEDIA_TYPES.put("jpg", MediaType.IMAGE_JPEG);
		MEDIA_TYPES.put("jpeg", MediaType.IMAGE_JPEG);
	}
	
	private ImageTileService _service;
	
	
	@Inject
	public ImageTileResource(ImageTileService service) {
		this._service = service;
	}
	
	@Post("json")
	public Representation getLayer(String jsonData) throws ResourceException {

		try {
			JSONObject jsonObj = new JSONObject(jsonData);
			
			String host = getRequest().getResourceRef().toString();
			host = host.substring(0, host.lastIndexOf("layer"));

			JSONObject layerInfo = _service.getLayer(host, jsonObj);
			
			return new JsonRepresentation(layerInfo);
			
		} catch (JSONException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to create JSON object from supplied options string", e);
		}
	}
	
	@Get
	public Representation getTile() throws ResourceException {

		try {
			// No alternate versions supported. But if we did:
			//String version = (String) getRequest().getAttributes().get("version");
			String id = (String) getRequest().getAttributes().get("id");
//			String transform = (String) getRequest().getAttributes().get("transform");
//			String ramp = (String) getRequest().getAttributes().get("ramp");
			String layer = (String) getRequest().getAttributes().get("layer");
			String levelDir = (String) getRequest().getAttributes().get("level");
			int zoomLevel = Integer.parseInt(levelDir);
			String xAttr = (String) getRequest().getAttributes().get("x");
			double x = Double.parseDouble(xAttr);
			String yAttr = (String) getRequest().getAttributes().get("y");
			int extIdx = yAttr.lastIndexOf('.');
			double y = Double.parseDouble(yAttr.substring(0, extIdx));
			String fileExt = yAttr.substring(extIdx+1).toLowerCase();
			MediaType mediaType = MEDIA_TYPES.get(fileExt);

			BufferedImage tile = _service.getTile(UUID.fromString(id), layer, zoomLevel, x, y);
			ImageOutputRepresentation imageRep = new ImageOutputRepresentation(mediaType, tile);
			
			setStatus(Status.SUCCESS_CREATED);
	
			return imageRep;
		} catch (Exception e){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to interpret requested tile from supplied URL.", e);
		}
	}
}
