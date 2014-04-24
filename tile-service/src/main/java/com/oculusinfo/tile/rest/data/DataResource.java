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
package com.oculusinfo.tile.rest.data;

import oculus.aperture.common.rest.ApertureServerResource;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DataResource extends ApertureServerResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataResource.class);


    private DataService _service;
    
    @Inject
    public DataResource (DataService service) {
        _service = service;
    }

    @Post("json:json")
    public Representation dataRequest (String jsonArguments) {
        try {
            JSONObject arguments = new JSONObject(jsonArguments);
            JSONObject dataset = arguments.getJSONObject("dataset");
            int requestCount = arguments.optInt("requestCount", 0);
            boolean getCount = arguments.optBoolean("getCount", false);
            boolean getData = true;
            JSONObject query = arguments.getJSONObject("query");

            if (0 >= requestCount) {
                getData = false;
                getCount = true;
            }

            JSONObject result = _service.getData(dataset, query, getCount, getData, requestCount);
            // Add in request parameters so the requester can recognize which result matches which request.
            result.put("dataset", dataset);
            result.put("requestCount", requestCount);
            result.put("query", query);

            return new JsonRepresentation(result);
        } catch (JSONException e) {
            LOGGER.warn("Bad data request: {}", jsonArguments, e);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "Unable to create JSON object from supplied options string",
                                        e);
        }
    }
}
