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
package com.oculusinfo.tile.rest.legend;

import com.google.inject.Inject;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.ImageOutputRepresentation;
import com.oculusinfo.tile.rest.layer.LayerService;
import oculus.aperture.common.rest.ApertureServerResource;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.util.Base64;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.UUID;

public class LegendResource extends ApertureServerResource {

	@Inject
	private LegendService _service;
	@Inject
    private LayerService  _layerService;


	
	public LegendResource () {
	}


	/**
	 * If there's any request params, then they are turned into a {@link JSONObject}.
	 * @param query
	 * 	The query for the resource request.
	 * <code>getRequest().getResourceRef().getQueryAsForm()</code>
	 * @return
	 * 	Returns a {@link JSONObject} that represents all the query parameters,
	 * 	or null if the query doesn't exist
	 */
	private JSONObject createRequestParamsObject(Form query) {
		JSONObject obj = null;
		if (query != null) {
			obj = new JSONObject(query.getValuesMap());
		}
		return obj;
	}


    /*
	@Post("json")
	public StringRepresentation getLegend( String jsonData ) throws ResourceException {

		try {

			JSONObject jsonObj = new JSONObject(jsonData);
			String layer = jsonObj.getString("layer");
			int zoomLevel = jsonObj.getInt("level");
			int width = jsonObj.getInt("width");
			int height = jsonObj.getInt("height");
			boolean renderHorizontally = false;
			if (jsonObj.has("orientation")){
				renderHorizontally = jsonObj.getString("orientation").equalsIgnoreCase("horizontal");
			}

			LayerConfiguration config = _layerService.getLayerConfiguration( layer, new TileIndex(zoomLevel, 0, 0), null );

			return generateEncodedImage( config, width, height, renderHorizontally );
		} catch (JSONException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			                            "Unable to create JSON object from supplied options string", e);
		}
	}
	*/
	
	@Get
	public Representation getLegend() throws ResourceException {

        // get layer
        String layer = (String) getRequest().getAttributes().get("layer");

		// get parameters from query
		Form form = getRequest().getResourceRef().getQueryAsForm();
		String outputType = form.getFirstValue("output", "uri");
		String orientationString = form.getFirstValue("orientation", "horizontal").trim();

		// get the root node ID from the form
		int width;
		int height;
		boolean renderHorizontally;

		try {

			renderHorizontally = orientationString.equalsIgnoreCase("horizontal");
			width = Integer.parseInt(form.getFirstValue("width", "128").trim());
			height = Integer.parseInt(form.getFirstValue("height", "1").trim());

		} catch (NumberFormatException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			                            "Unable to create Integer from supplied string. Check parameters.", e);
		}

		JSONObject requestParams = createRequestParamsObject(form);
	    LayerConfiguration config = _layerService.getLayerConfiguration( layer, requestParams );

        setStatus(Status.SUCCESS_OK);
		if(outputType.equalsIgnoreCase("uri")){
			return generateEncodedImage( config, width, height, renderHorizontally );
		} else { //(outputType.equalsIgnoreCase("png")){
			return generateImage( config, width, height, renderHorizontally );
		}
	}

	/**
	 * @param width
	 * @param height
	 * @return
	 */
	private ImageOutputRepresentation generateImage( LayerConfiguration config,
	                                                 int width,
	                                                 int height,
	                                                 boolean renderHorizontally) {
		try {

			BufferedImage tile = _service.getLegend( config, width, height, renderHorizontally );
			ImageOutputRepresentation imageRep = new ImageOutputRepresentation(MediaType.IMAGE_PNG, tile);
			return imageRep;

		} catch (Exception e) {
			throw new ResourceException(Status.CONNECTOR_ERROR_INTERNAL, "Unable to generate legend image.", e);
		}
	}

	/**
	 * @param width
	 * @param height
	 * @return
	 */
	private StringRepresentation generateEncodedImage( LayerConfiguration config,
	                                                   int width,
	                                                   int height,
	                                                   boolean renderHorizontally) {
		try {

			BufferedImage tile = _service.getLegend( config, width, height, renderHorizontally );
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(tile, "png", baos);
			baos.flush();
			String encodedImage = Base64.encode(baos.toByteArray(), true);
			baos.close();
			encodedImage = "data:image/png;base64," + URLEncoder.encode(encodedImage, "ISO-8859-1");
			return new StringRepresentation( encodedImage );
			
		} catch (IOException e) {
			throw new ResourceException(Status.CONNECTOR_ERROR_INTERNAL,
			                            "Unable to encode legend image.", e);
		}
	}
}
