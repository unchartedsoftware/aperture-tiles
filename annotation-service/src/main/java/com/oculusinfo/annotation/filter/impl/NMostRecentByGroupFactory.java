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

import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.JSONProperty;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class NMostRecentByGroupFactory extends ConfigurableFactory<AnnotationFilter> {
	private static final Logger LOGGER = LoggerFactory.getLogger(NMostRecentByGroupFactory.class);

	
	public static JSONProperty COUNTS_BY_GROUP = new JSONProperty("countsByGroup",
	    "Indicates the number of annotations to read for each bin, by priority",
	    null);

	public NMostRecentByGroupFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("n-most-recent-by-group", AnnotationFilter.class, parent, path);
		
		addProperty(COUNTS_BY_GROUP);
	}

	@Override
	protected AnnotationFilter create() {
		try {
			JSONObject countsByGroup = getPropertyValue(COUNTS_BY_GROUP);
			return new NMostRecentByGroupFilter(countsByGroup);
		}
		catch (Exception e) {
			LOGGER.error("Error trying to create NMostRecentByPriorityFactory", e);
		}
		return null;
	}
}
