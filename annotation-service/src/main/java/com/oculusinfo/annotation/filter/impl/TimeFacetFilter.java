/*
 * Copyright (c) 2015 Uncharted Software http://www.uncharted.software/
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
package com.oculusinfo.annotation.filter.impl;


import com.oculusinfo.annotation.AnnotationData;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TimeFacetFilter extends EmptyFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger( TimeFacetFilter.class );
	
	private Long _startTime = null;
	private Long _endTime = null;

	public TimeFacetFilter( JSONObject filterConfig ) {
		if ( filterConfig != null ) {
			_startTime = filterConfig.optLong("startTime");
			_endTime = filterConfig.optLong("endTime");
		} 
	}


	public List<AnnotationData<?>> filterAnnotations( List<AnnotationData<?>> annotations, List<FilteredBinResults> binResults ) {
		List<AnnotationData<?>> result = null;
		if ( _startTime == null || _endTime == null ) {
			result = annotations;
		} else {
			result = new LinkedList<>();
			try {
				for ( AnnotationData<?> annotation : annotations  ) {
					JSONObject data = (JSONObject) annotation.getData();	
					long epoch = data.getLong("epoch") * 1000;		// need to get the time in ms
					if ( epoch >= _startTime && epoch <= _endTime ) {
						result.add(annotation);
					}
				}
			} catch (JSONException e) {
				LOGGER.warn("Exception getting epoch from annotation data for TimeFacetFilter", e);				
			}
		}
		return result;
	}
}
