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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.imageio.ImageIO;

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

import com.google.inject.Inject;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.color.ColorRampFactory;
import com.oculusinfo.tile.rendering.transformations.ValueTransformerFactory;
import com.oculusinfo.tile.rest.ImageOutputRepresentation;

public class LegendResource extends ApertureServerResource {
	
	private LegendService _service;
	
	@Inject
	public LegendResource(LegendService service) {
		this._service = service;
	}
	
	@Post("json")
	public StringRepresentation getLegend(String jsonData) throws ResourceException {

		try {
			JSONObject jsonObj = new JSONObject(jsonData);
			
			Object transform 	= jsonObj.has("transform")? jsonObj.get("transform") : ValueTransformerFactory.DEFAULT_TRANSFORM_NAME;
			String layer 		= jsonObj.getString("layer");
			int zoomLevel		= jsonObj.getInt("level");
			int width = jsonObj.getInt("width");
			int height = jsonObj.getInt("height");
			boolean doAxis = false;
			if(jsonObj.has("doAxis")){
				doAxis = jsonObj.getBoolean("doAxis");
			}
			boolean renderHorizontally = false;
			if(jsonObj.has("orientation")){
				renderHorizontally = jsonObj.getString("orientation").equalsIgnoreCase("horizontal");
			}

			ColorRamp colorRamp = null;
			try {
			    ColorRampFactory factory = new ColorRampFactory(null, Collections.singletonList("ramp"));
			    factory.readConfiguration(jsonObj);
			    colorRamp = factory.getNewGood(ColorRamp.class);
			} catch (ConfigurationException e) {
			    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to create legend - bad color ramp configuration");
			}

			return generateEncodedImage(transform, colorRamp, layer, zoomLevel, width, height, doAxis, renderHorizontally);
		} catch (JSONException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to create JSON object from supplied options string", e);
		}
	}
	
	@Get
	public Representation getLegend() throws ResourceException {
		
		// Get parameters from query
		Form form = getRequest().getResourceRef().getQueryAsForm();
		

		String outputType = form.getFirstValue("output", "uri");
		String transform = form.getFirstValue("transform", "linear").trim();
		String layer = form.getFirstValue("layer").trim();
		String doAxisString = form.getFirstValue("doAxis", "false").trim();
		String orientationString = form.getFirstValue("orientation", "vertical").trim();

		ColorRamp ramp = null;
		try {
    		JSONObject rampConfig = new JSONObject();
            for (ConfigurationProperty<?> property: Arrays.asList(ColorRampFactory.RAMP_TYPE,
                                                                  ColorRampFactory.INVERTED,
                                                                  ColorRampFactory.OPACITY,
                                                                  ColorRampFactory.COLOR1,
                                                                  ColorRampFactory.ALPHA1,
                                                                  ColorRampFactory.COLOR2,
                                                                  ColorRampFactory.ALPHA2)) {
                String value = form.getFirstValue(property.getName(), true);
                if (null != value) rampConfig.put(property.getName(), value);
    		}
            ColorRampFactory factory = new ColorRampFactory(null, new ArrayList<String>());
            ramp = factory.getNewGood(ColorRamp.class);
		} catch (ConfigurationException|JSONException e) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Unable to generate legend image - bad color ramp configuration.", e);
        }
				
		// get the root node ID from the form
		int zoomLevel = 0;
		int width = 50;
		int height = 100;
		boolean doAxis = false;
		boolean renderHorizontally = false;
		try {
			doAxis = Boolean.parseBoolean(doAxisString);
			renderHorizontally = orientationString.equalsIgnoreCase("horizontal");
			zoomLevel = Integer.parseInt(form.getFirstValue("level").trim());
			width = Integer.parseInt(form.getFirstValue("width").trim());
			height = Integer.parseInt(form.getFirstValue("height").trim());
			
		} catch (NumberFormatException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
				"Unable to create Integer from supplied string. Check parameters.", e);
		}
		
		if(outputType.equalsIgnoreCase("uri")){
			return generateEncodedImage(transform, ramp, layer, zoomLevel, width, height, doAxis, renderHorizontally);
		}
		else { //(outputType.equalsIgnoreCase("png")){
			return generateImage(transform, ramp, layer, zoomLevel, width, height, doAxis, renderHorizontally);
		}

	}

	/**
	 * @param transform
	 * @param layer
	 * @param zoomLevel
	 * @param width
	 * @param height
	 * @return
	 */
    private ImageOutputRepresentation generateImage (String transform,
                                                     ColorRamp ramp,
                                                     String layer,
                                                     int zoomLevel, int width,
                                                     int height,
                                                     boolean doAxis,
                                                     boolean renderHorizontally) {
		try {
			BufferedImage tile = _service.getLegend(transform, ramp, layer, zoomLevel, width, height, doAxis, renderHorizontally);
			ImageOutputRepresentation imageRep = new ImageOutputRepresentation(MediaType.IMAGE_PNG, tile);
			
			setStatus(Status.SUCCESS_CREATED);
			
			return imageRep;
			
		} catch (Exception e) {
			throw new ResourceException(Status.CONNECTOR_ERROR_INTERNAL, "Unable to generate legend image.", e);
		}
	}

	/**
	 * @param transform
	 * @param layer
	 * @param zoomLevel
	 * @param width
	 * @param height
	 * @return
	 */
    private StringRepresentation generateEncodedImage (Object transform,
                                                       ColorRamp ramp,
                                                       String layer,
                                                       int zoomLevel,
                                                       int width,
                                                       int height,
                                                       boolean doAxis,
                                                       boolean renderHorizontally) {
		try {
			BufferedImage tile = _service.getLegend(transform, ramp, layer, zoomLevel, width, height, doAxis, renderHorizontally);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(tile, "png", baos);
			baos.flush();
			
			String encodedImage = Base64.encode(baos.toByteArray(), true);
			baos.close();
			encodedImage = "data:image/png;base64," + URLEncoder.encode(encodedImage, "ISO-8859-1");
			setStatus(Status.SUCCESS_CREATED);

			StringRepresentation imageRep = new StringRepresentation(encodedImage);
			
			return imageRep;
			
		} catch (IOException e) {
			throw new ResourceException(Status.CONNECTOR_ERROR_INTERNAL,
					"Unable to encode legend image.", e);
		}
	}
}
