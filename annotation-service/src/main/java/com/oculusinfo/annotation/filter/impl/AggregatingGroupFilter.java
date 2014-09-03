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

import java.util.ArrayList;
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
		if (groupArray != null) {
			for (int i = 0; i < groupArray.length(); i++) {
				groups.add(groupArray.getString(i));
			}			
		}
	}

	/**
	 * Removes all but one entry from a supplied bin, and stores any that were removed
	 * for processing by the subsequent {{@link #filterAnnotations(List, List)} call.
	 * Data for any group that is not part of the currently configured group list will
	 * be filtered as well.
	 * 
	 * This is not scalable past a few thousand annotations.
	 */
	@Override
	public FilteredBinResults filterBins(List<AnnotationBin> bins) {
		
		long start = System.nanoTime();
		
		List<Pair<String, Long>> filtered = new LinkedList<>();
		Map<String, Integer> aggregates = new HashMap<>();
		
		// First pass - create a new list of annotation bins that only contain only annotations
		// that are in our variable list.
		List<AnnotationBin> filteredAnnotationBins = new ArrayList<>();
		for (AnnotationBin bin : bins) {			
			if (bin != null) {
				Map<String, List<Pair<String, Long>>> groupAnnotations = new HashMap<>();
				for ( Map.Entry<String, List<Pair<String, Long>>> binEntry : bin.getData().entrySet() ) {
					if (groups.contains(binEntry.getKey()) || groups.size() == 0) {
						groupAnnotations.put(binEntry.getKey(), binEntry.getValue());
					}
				}
				filteredAnnotationBins.add(new AnnotationBin(groupAnnotations));
			}
		}
		
		// Second pass - track uuid counts within bins.  Any uuid that appears more than once
		// in a bin is a range annotation that is constrained to a single bin.  Those can be
		// aggregated.
		Set<String> singleBinRangeUuids = new HashSet<>();		
		for (AnnotationBin bin : filteredAnnotationBins) {			
			Map<String, Integer> binAnnotationCounts = new HashMap<>();				
			for ( Map.Entry<String, List<Pair<String, Long>>> binEntry : bin.getData().entrySet() ) {										
				for (Pair<String, Long> annotation : binEntry.getValue()) {
					String annotationUuid = annotation.getFirst();
					if (binAnnotationCounts.get(annotationUuid) == null) {
						binAnnotationCounts.put(annotationUuid, 0);								
					}
					binAnnotationCounts.put(annotationUuid, binAnnotationCounts.get(annotationUuid) + 1);							
					if (binAnnotationCounts.get(annotationUuid) >= 2) {
						singleBinRangeUuids.add(annotationUuid);
					}
				}											
			}
		}			
			
		// Third pass - figure out which annotations are range based (UUID appears more than once in the tile) 
		// and span more than one bin.
		Set<String> multiBinRangeUuids = new HashSet<>();
		Map<String, Integer> tileAnnotationCounts = new HashMap<>();
		for (AnnotationBin bin : filteredAnnotationBins) {			
			for ( Map.Entry<String, List<Pair<String, Long>>> binEntry : bin.getData().entrySet() ) {										
				for (Pair<String, Long> annotation : binEntry.getValue()) {
					String annotationUuid = annotation.getFirst();
					if (tileAnnotationCounts.get(annotationUuid) == null && !singleBinRangeUuids.contains(annotationUuid)) {
						tileAnnotationCounts.put(annotationUuid, 0);								
					}
					tileAnnotationCounts.put(annotationUuid, tileAnnotationCounts.get(annotationUuid) + 1);
					if (tileAnnotationCounts.get(annotationUuid) >= 2) {
						multiBinRangeUuids.add(annotationUuid);
					}
				}				
			}
		}
		
		// Now do the actual aggregate determination.  We allow point based and single bin range annotations to have
		// aggregation applied.  Mult-bin range annotations are exempt.
		for (AnnotationBin bin : filteredAnnotationBins) {			
			for ( Map.Entry<String, List<Pair<String, Long>>> binEntry : bin.getData().entrySet() ) {										
				if (binEntry.getValue().size() > 1) {
					int aggregateCount = 0;
					Pair<String, Long> aggregate = null;						
					for (Pair<String, Long> annotation : binEntry.getValue()) {
						if (multiBinRangeUuids.contains(annotation.getFirst())) {
							filtered.add(annotation);						
						} else {
							// Save the first annotation we come across that is not part of
							// a multi range annotation.
							if (aggregate == null) {							
								aggregate = annotation;
							}
							aggregateCount++;						
						}
						
					}
					if (aggregateCount > 1) {
						aggregates.put(aggregate.getFirst(), aggregateCount);
						filtered.add(aggregate);
					}
				} else {
					filtered.addAll(binEntry.getValue());
				}
			}
		}
		
		long diff = System.nanoTime() - start;
		System.out.println("ANNOTATION PROCESSING TIME: " + diff / 1000000.0);
		
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
		
		// Compute aggregates.
		
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
