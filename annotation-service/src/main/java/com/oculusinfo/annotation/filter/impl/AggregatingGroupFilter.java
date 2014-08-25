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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.annotation.data.AnnotationBin;
import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.binning.util.Pair;

/**
 * An anntotation filter that operatrs based on the configured groups, and aggregates by removing
 * all but one entry in a given bin.  The remaining entry is given a tag that indicates how many
 * items it represents, which clients can use when rendering the annotation visuals.   
 * 
 * @author Chris Bethune
 *
 */
public class AggregatingGroupFilter implements AnnotationFilter {
	
	private Set<String> groups = new HashSet<>();
	
	/**
	 * Creates the filter, storing the list of groups (variables) that should
	 * be processed.
	 * 
	 * @param groupArray
	 * 		The value of the 'groups' property in the config - consists of an array of 
	 * 		variables that are to have annotations displayed.
	 * @throws JSONException
	 */
	public AggregatingGroupFilter(JSONArray groupArray ) throws JSONException {
		for (int i = 0; i < groupArray.length(); i++) {
			groups.add(groupArray.getString(i));
		}
	}

	/**
	 * Removes all but one entry from a supplied bin, and stores any that were removed
	 * for processing by the subsequent {{@link #filterAnnotations(List, List)} call.
	 * Data for any group that is not part of the currently configured group list will
	 * be filtered as well.
	 */
	@Override
	public FilteredBinResults filterBin(AnnotationBin bin) {	
		List<Pair<String, Long>> filtered = new LinkedList<>();
		Map<String, Integer> aggregates = new HashMap<>();
		
		for ( Map.Entry<String, List<Pair<String, Long>>> binEntry : bin.getData().entrySet() ) {
			if (groups.contains(binEntry.getKey())) {
				// Grab the first item in the bin.  If there were more, then we add this to our list
				// of aggregated bins.
				List<Pair<String, Long>> binItems = binEntry.getValue();
				if (binItems.size() > 1) {					
					aggregates.put(binItems.get(0).getFirst(), binItems.size());
				}
				filtered.add( binItems.get(0) );								
			}			
		}
		return new FilteredBinResults(filtered, aggregates);
	}

	
	
	/**
	 * Takes any annotations that were flagged as aggregates by the {@link #filterBin(AnnotationBin)} step, and
	 * adds a new 'aggregated' field to them that stores the number of annotations that are represented by
	 * the aggregating annotation.  The clients can detect the presence of this field and render the aggregate
	 * differently.
	 */
	@Override
	public List<AnnotationData<?>> filterAnnotations(List<AnnotationData<?>> annotations, List<FilteredBinResults> binResults) {
		// Put the annotations into a map for quick lookup in the next step.
		Map<String, AnnotationData<?>> annotationMap = new HashMap<>();
		for (AnnotationData<?> annotation : annotations) {
			annotationMap.put(annotation.getUUID().toString(), annotation);
		}
		
		// Mark annotations detected as aggregates in the previous step and assign the aggregation count
		// to them.
		for (FilteredBinResults binResult : binResults) {
			@SuppressWarnings("unchecked")
			Map<String, Integer> aggregates = (Map<String, Integer>) binResult.getResultData();
			for (Map.Entry<String, Integer> entry : aggregates.entrySet()) {
				AnnotationData<?> annotation = annotationMap.get(entry.getKey());
				try {
					JSONObject jsonData = (JSONObject) annotation.getData();
					jsonData.put("aggregated", entry.getValue());				
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return annotations;
	}	
}
