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
package com.oculusinfo.annotation.filter;

import com.oculusinfo.annotation.filter.impl.EmptyFilterFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.UberFactory;
import com.oculusinfo.factory.properties.JSONProperty;

import java.util.List;


/**
 * Factory class to create the standard types of AnnotationFilters
 *
 * @author nkronenfeld
 */
public class AnnotationFilterFactory extends UberFactory<AnnotationFilter> {
	public static JSONProperty FILTER_DATA = new JSONProperty("filter",
	    "Data to be passed to the annotation filter for  initialization",
	    null);

	public AnnotationFilterFactory (ConfigurableFactory<?> parent, List<String> path,
	                                List<ConfigurableFactory<? extends AnnotationFilter>> children) {
		this(null, parent, path, children);
	}

	public AnnotationFilterFactory (String name, ConfigurableFactory<?> parent, List<String> path,
	                                List<ConfigurableFactory<? extends AnnotationFilter>> children) {
		super(name, AnnotationFilter.class, parent, path, true, children, EmptyFilterFactory.NAME);

		addProperty(FILTER_DATA);
	}
}
