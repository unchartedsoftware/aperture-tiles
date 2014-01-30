/**
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
package com.oculusinfo.tile.rest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import oculus.aperture.common.rest.ApertureServerResource;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PreRenderedTileResource extends ApertureServerResource {
	private static Map<String, MediaType> MEDIA_TYPES = new HashMap<String, MediaType>();
	static{
		MEDIA_TYPES.put("png", MediaType.IMAGE_PNG);  // Should be an easier way to do this...
		MEDIA_TYPES.put("jpg", MediaType.IMAGE_JPEG);
		MEDIA_TYPES.put("jpeg", MediaType.IMAGE_JPEG);
	}
	
	private String _rootLocation = "";
	
	@Inject
	public PreRenderedTileResource(@Named("com.oculusinfo.tile.prerendered.location") String location) {
		_rootLocation = location;
	}
	
	@Get
	public Representation getTile() throws ResourceException {

		try {
			// No alternate versions supported. But if we did:
			//String version = (String) getRequest().getAttributes().get("version");
			String layer = (String) getRequest().getAttributes().get("layer");
			String levelDir = (String) getRequest().getAttributes().get("level");
			String xDir = (String) getRequest().getAttributes().get("x");
			String yFile = (String) getRequest().getAttributes().get("y");
			String fileExt = yFile.substring(yFile.lastIndexOf('.')+1).toLowerCase();
			MediaType mediaType = MEDIA_TYPES.get(fileExt);
			
			// If there are more places to retrieve than the file system, can
			// create a service interface and impls and delegate to those.
			StringBuilder sb = new StringBuilder(_rootLocation);
			if(!_rootLocation.endsWith("/") && !_rootLocation.endsWith("\\")){
				sb.append(File.separator);
			}
			sb.append(layer).append(File.separator).append("tiles").append(File.separator);
			sb.append(levelDir).append(File.separator);
			sb.append(xDir).append(File.separator);
			sb.append(yFile);
			
			File file = new File(sb.toString());
			if(!file.exists()){
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Tile does not exist at the specified location.");
			}
			FileRepresentation imageFileRep = new FileRepresentation(file, mediaType);
			
			setStatus(Status.SUCCESS_OK);
	
			return imageFileRep;
		} catch (Exception e){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Unable to interpret requested tile from supplied URL.", e);
		}
	}
}
