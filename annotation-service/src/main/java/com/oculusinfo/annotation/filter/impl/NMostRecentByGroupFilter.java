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
package com.oculusinfo.annotation.filter.impl;

import com.oculusinfo.annotation.AnnotationBin;
import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.factory.util.Pair;
import org.json.JSONObject;

import java.util.*;

public class NMostRecentByGroupFilter implements AnnotationFilter {

	private Map<String, Integer> _countsByGroup;

	public NMostRecentByGroupFilter( JSONObject filterConfig ) {
		_countsByGroup = getFilterMap( filterConfig );
	}

	private Map<String, Integer> getFilterMap( JSONObject countsByGroupJson ) {
		Map<String, Integer> filters = new HashMap<>();
		try {
			Iterator<?> groups = countsByGroupJson.keys();
			while( groups.hasNext() ) {

				String priority = (String)groups.next();
				int count = countsByGroupJson.getInt(priority);
				filters.put( priority, count );
			}

		} catch (Exception e) {
			throw new IllegalArgumentException( e.getMessage() );
		}
		return filters;
	}

	public FilteredBinResults filterBins( List<AnnotationBin> bins ) {

		List<Pair<String, Long>> filtered = new LinkedList<>();
		for (AnnotationBin bin : bins) {	
			if (bin != null) {				
				// go through filter list get certificates by group and by count
				for (Map.Entry<String, Integer> f : _countsByGroup.entrySet()) {
					
					String group = f.getKey();
					Integer count = f.getValue();
					
					// get all certificates from the bin
					List<Pair<String, Long>> certificates = bin.getCertificates( group );
					
					// certificates are sorted, so simply cut the tail off to get the n newest
					filtered.addAll(certificates.subList(0, count < certificates.size() ? count : certificates.size()));
				}
			}
		}
		return new FilteredBinResults(filtered, null);
	}

	public List<AnnotationData<?>> filterAnnotations( List<AnnotationData<?>> annotations, List<FilteredBinResults> binResults ) {
		return annotations;
	}
}
