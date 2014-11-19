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

import com.oculusinfo.binning.util.Pair;

import java.util.List;

/**
 * A class to hold information generated by the bin filtering portion of a filter.  This data
 * is forwarded to the annotation filter to provide additional context.
 * 
 * @author Chris Bethune
 *
 */
public class FilteredBinResults {
	
	private Object resultData;
	private List<Pair<String,Long>> filteredBins;
	
	/**
	 * @param filteredBins
	 * 		The list of ID/timestamp tuples representing the annotations that are present in the filtered
	 * 		bin.
	 * 
	 * @param resultData
	 * 		Filter-specific information that can be passed from the bin filter step to the annotation
	 * 		filter step.
	 * 		
	 */
	public FilteredBinResults(List<Pair<String,Long>> filteredBins, Object resultData) {
		this.resultData = resultData;
		this.filteredBins = filteredBins;
	}
	
	
	
	/**
	 * @return
	 * 		The list of ID/timestamp tuples representing the annotations that are present in the filtered
	 * 		bin.
	 */
	public List<Pair<String, Long>> getFilteredBins() {
		return filteredBins;
	}
	
	
	
	/**
	 * @return
	 * 		Filter-specific information that can be passed from the bin filter step to the annotation
	 * 		filter step.
	 */
	public Object getResultData() {
		return resultData;
	}
}
