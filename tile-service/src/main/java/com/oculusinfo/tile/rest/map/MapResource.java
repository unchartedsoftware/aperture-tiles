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
package com.oculusinfo.tile.rest.map;

import oculus.aperture.common.rest.ApertureServerResource;

import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import com.google.inject.Inject;

public class MapResource extends ApertureServerResource {
    private MapService _service;

    @Inject
    public MapResource (MapService service) {
        _service = service;
    }

    @Get("json")
    public Representation mapRequest (String jsonArguments) {
        String id = (String) getRequest().getAttributes().get("id");
        if (null == id) {
            // No id given; pass back the full set
            JsonRepresentation result = new JsonRepresentation(_service.getMaps());
            setStatus(Status.SUCCESS_CREATED);
            return result;
        } else {
            // Get the map configuration with the given id.
            JsonRepresentation result = new JsonRepresentation(_service.getMap(id));
            setStatus(Status.SUCCESS_CREATED);
            return result;
        }
    }
}
