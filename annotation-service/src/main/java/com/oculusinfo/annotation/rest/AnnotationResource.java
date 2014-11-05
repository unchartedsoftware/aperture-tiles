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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import oculus.aperture.common.rest.ApertureServerResource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;
import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.data.impl.JSONAnnotation;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.util.Pair;

public class AnnotationResource extends ApertureServerResource {


	private AnnotationService _service;
	
	@Inject
	public AnnotationResource( AnnotationService service ) {
		_service = service;
	}
	

	@Post("json")
	public Representation postAnnotation( String jsonData ) throws ResourceException {

		try {

			JSONObject json = new JSONObject( jsonData );
			
			String requestType = json.getString( "type" ).toLowerCase();	
			JSONObject jsonResult = new JSONObject();
			
			if ( requestType.equals("write") ) {
				
				String layer = json.getString("layer");
				JSONAnnotation annotation = JSONAnnotation.fromJSON( json.getJSONObject("annotation") );
				Pair<String, Long> certificate = _service.write( layer, annotation );
				jsonResult.put("uuid", certificate.getFirst() );
				jsonResult.put("timestamp", certificate.getSecond().toString() );
				
			} else if ( requestType.equals("remove") ) {

				String layer = json.getString("layer");
				JSONObject certificate =  json.getJSONObject("certificate");
				String uuid = certificate.getString("uuid");
				Long timestamp = certificate.getLong("timestamp");
				_service.remove(layer, new Pair<>( uuid, timestamp ) );
				
			} else if ( requestType.equals("modify") ) {
				
				String layer = json.getString("layer");
				JSONAnnotation annotation = JSONAnnotation.fromJSON( json.getJSONObject("annotation") );
				Pair<String, Long> certificate = _service.modify(layer, annotation);
				jsonResult.put("uuid", certificate.getFirst() );
				jsonResult.put("timestamp", certificate.getSecond().toString() );
			}
			
			setStatus(Status.SUCCESS_OK);		
			jsonResult.put("status", "success");
			return new JsonRepresentation(jsonResult);
			
		} catch (JSONException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			                            "Unable to create JSON object from supplied options string", e);
		} catch (IllegalArgumentException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
			                            e.getMessage(), e);
		}
	}
	
	/**
	 * If there's any query params, then they are turned into a {@link JSONObject}.
	 * @param query
	 * 	The query for the resource request.
	 * <code>getRequest().getResourceRef().getQueryAsForm()</code>
	 * @return
	 * 	Returns a {@link JSONObject} that represents all the query parameters,
	 * 	or null if the query doesn't exist
	 */
	private JSONObject createQueryParamsObject(Form query) {
		JSONObject obj = null;
		if (query != null) {
			obj = new JSONObject(query.getValuesMap());
		}
		return obj;
	}


	@Get
	public Representation getAnnotation() throws ResourceException {

		try {

			String layer = (String) getRequest().getAttributes().get("layer");
			String levelDir = (String) getRequest().getAttributes().get("level");
			String xAttr = (String) getRequest().getAttributes().get("x");
			String yAttr = (String) getRequest().getAttributes().get("y");
			JSONObject queryParams = createQueryParamsObject(getRequest().getResourceRef().getQueryAsForm());

			int zoomLevel = Integer.parseInt(levelDir);
			int x = Integer.parseInt(xAttr);
			int y = Integer.parseInt(yAttr);

			TileIndex index = new TileIndex( zoomLevel, x, y, AnnotationIndexer.NUM_BINS, AnnotationIndexer.NUM_BINS );
		    
			Map<BinIndex, List<AnnotationData<?>>> data = _service.read( layer, index, queryParams );

			// annotations by bin
			JSONObject binsJson = new JSONObject();
			for (Map.Entry<BinIndex, List<AnnotationData<?>>> entry : data.entrySet() ) {
				
				BinIndex binIndex = entry.getKey();
				List<AnnotationData<?>> annotations = entry.getValue();
		    	
				JSONArray annotationArray = new JSONArray();
                for ( AnnotationData<?> annotation : annotations ) {
					annotationArray.put( annotation.toJSON() );                    
				}
				binsJson.put( binIndex.toString(), annotationArray );
			}

		    JSONObject tileJson = new JSONObject();
			tileJson.put("level", zoomLevel);
			tileJson.put("xIndex", x);
			tileJson.put("yIndex", y);

			JSONObject result = new JSONObject();
			result.put("tile", tileJson );
			result.put( "annotations", binsJson );

			setStatus(Status.SUCCESS_CREATED);
			return new JsonRepresentation( result );

		} catch (Exception e){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			                            "Unable to interpret requested tile from supplied URL.", e);
		}
	}
}
