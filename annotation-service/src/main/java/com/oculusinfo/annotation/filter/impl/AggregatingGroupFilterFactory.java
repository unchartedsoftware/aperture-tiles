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
package com.oculusinfo.annotation.filter.impl;

import java.util.List;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.JSONArrayProperty;

/**
 * A factory for creating the {@link AggregatingGroupFilter}.  It assigns one configuration property, 
 * 'groups', that takes an array of group names as an argument.  If non-null, any annotations that are
 * not part of the included groups will be filtered out pior to aggregation taking place.
 * 
 * In the context of the cyber webapp, groups array should contain the list of variables currently displayed
 * in the application.
 * 
 * @author Chris Bethune
 *
 */
public class AggregatingGroupFilterFactory extends ConfigurableFactory<AnnotationFilter> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AggregatingGroupFilterFactory.class);
	public static final String NAME = "aggregating";

	public static JSONArrayProperty GROUPS = new JSONArrayProperty("groups",
		    "Indicates the number of annotations to read for each bin, by priority",
		    null);
	

	/**
	 * @param parent
	 * @param path
	 */
	public AggregatingGroupFilterFactory(ConfigurableFactory<?> parent, List<String> path) {
		super(NAME, AnnotationFilter.class, parent, path);
		addProperty(GROUPS);
	}

	@Override
	protected AnnotationFilter create() {
		try {
			JSONArray groups = getPropertyValue(GROUPS);
			return new AggregatingGroupFilter(groups);
		}
		catch (Exception e) {
			LOGGER.error("Error trying to create TestFilterFactory", e);
		}		
		return null;
	}
}
