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

import javax.imageio.ImageIO;

import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.util.Base64;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;
import com.oculusinfo.tile.rest.ImageOutputRepresentation;
import com.oculusinfo.tile.rest.QueryParamDecoder;
import org.restlet.resource.ServerResource;

public class LegendResource extends ServerResource {

	private LegendService _service;

    @Inject
	public LegendResource( LegendService service ) {
        _service = service;
	}

    /**
     * Get request. Returns a image or encoded image based on the layer ramp.
     */
	@Get
	public Representation getLegend() throws ResourceException {

        /*
        String version = (String) getRequest().getAttributes().get("version");
        if ( version == null ) {
            version = LayerConfiguration.DEFAULT_VERSION;
        }
        */
        String layer = (String) getRequest().getAttributes().get("layer");

		try {

            String outputType = "uri";
			int width = 128;
            int height = 1;
            String orientationString = "horizontal";
            boolean renderHorizontally = true;

            // decode the query parameters
            JSONObject decodedQueryParams = QueryParamDecoder.decode( getRequest().getResourceRef().getQuery() );
            if ( decodedQueryParams != null ) {
                outputType = decodedQueryParams.optString( "output", outputType );
                width = decodedQueryParams.optInt( "width", width );
                height = decodedQueryParams.optInt( "height", height );
                orientationString = decodedQueryParams.optString( "orientation", orientationString );
                renderHorizontally = orientationString.equalsIgnoreCase( "horizontal" );
            }

            setStatus(Status.SUCCESS_OK);

            if(outputType.equalsIgnoreCase("uri")){
                return generateEncodedImage( layer, width, height, renderHorizontally, decodedQueryParams );
            } else { //(outputType.equalsIgnoreCase("png")){
                return generateImage( layer, width, height, renderHorizontally, decodedQueryParams );
            }

		} catch ( Exception e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			                            "Unable to create legend from supplied string. Check parameters.", e);
		}
	}


	private ImageOutputRepresentation generateImage( String layer,
	                                                 int width,
	                                                 int height,
	                                                 boolean renderHorizontally,
                                                     JSONObject query ) {
		try {
			BufferedImage tile = _service.getLegend( layer, width, height, renderHorizontally, query );
			return new ImageOutputRepresentation(MediaType.IMAGE_PNG, tile);
		} catch (Exception e) {
			throw new ResourceException(Status.CONNECTOR_ERROR_INTERNAL, "Unable to generate legend image.", e);
		}
	}


	private StringRepresentation generateEncodedImage( String layer,
	                                                   int width,
	                                                   int height,
	                                                   boolean renderHorizontally,
                                                       JSONObject query ) {
		try {
			BufferedImage tile = _service.getLegend( layer, width, height, renderHorizontally, query );
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
