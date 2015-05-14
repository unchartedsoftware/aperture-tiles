/**
 * Copyright (c) 2013-2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 * <p/>
 * Released under the MIT License.
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.servlet;

import java.util.Map;

import org.restlet.resource.ServerResource;
import org.restlet.routing.Variable;

import com.google.common.collect.Maps;

/**
 * Defines a class of restlet ServerResource.
 *
 * @author djonker
 */
public class ResourceDefinition {

	final private Map<String, Variable> variables = Maps.newHashMap();
	final private Class<? extends ServerResource> resource;

	/**
	 * Constructs a basic resource definition with only the implementing class.
	 * @param resource
	 */
	public ResourceDefinition( Class<? extends ServerResource> resource ) {
		this.resource = resource;
	}

	/**
	 * Builder method: adds a variable.
	 */
	public ResourceDefinition setVariable( String key, Variable variable ) {
		variables.put( key, variable );

		return this;
	}

	/**
	 * @return the class of resource
	 */
	public Class<? extends ServerResource> getResource() {
		return resource;
	}

	/**
	 * Returns any specialized variables for a resource. If not
	 * specialized variables can still be specified in the route and
	 * will appear as attributes in the restlet resource, but they will
	 * have default behavior, which is an /undecoded_string/ path part.
	 */
	public Map<String, Variable> getVariables() {
		return variables;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return resource.getName();
	}


}
