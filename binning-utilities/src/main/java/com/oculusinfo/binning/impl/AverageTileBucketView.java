/*
 * Copyright (c) 2015 Uncharted Software. http://www.uncharted.software/
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
package com.oculusinfo.binning.impl;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;




/**
 * This implementation of TileData takes a TileData whose bins are lists of buckets and presents
 *  a view to the average value of a range of buckets
 *
 */
public class AverageTileBucketView<T extends Number> implements TileData<List<T>> {
	private static final long serialVersionUID = 1234567890L;

	private TileData<List<T>> _base = null;
	private Integer	_startCompare = null;
	private Integer	_endCompare = null;


	public AverageTileBucketView (TileData<List<T>> base, int startComp, int endComp) {
		_base = base;
		_startCompare = startComp;
		_endCompare = endComp;
	}


	@Override
	public TileIndex getDefinition () {
		return _base.getDefinition();
	}

	@Override
	public List<T> getDefaultValue () { return _base.getDefaultValue(); }

	@Override
	// method not implemented as this view is to be read only
	public void setBin(int x, int y, List<T> value)  {}


	@SuppressWarnings("unchecked")
	@Override
	public List<T> getBin (int x, int y) {
		List<T> resultList = new ArrayList<>(1);
		List<T> binContents = _base.getBin(x, y);
		int binSize = binContents.size();
		Number result = 0.0;
		
		// if the bucket range falls outside of the available bin range, return empty list.  null values assume full range
		boolean inRange =  ( _endCompare == null && _startCompare == null )
					    || ( _endCompare >= 0 && _startCompare <= binSize );

		// If start or end (but not both) fall outside the bin range, constrain the range to available bin range
		int start = ( _startCompare != null && _startCompare >= 0 ) ? _startCompare : 0;
		int end = ( _endCompare != null && _endCompare < binSize ) ? _endCompare : binSize;
		
		// compute bin averages for selected average range. 	
		double total = 0;
		int count = 0;
		for(int i = start; i < binSize; i++) {
			if ( i >= start && i <= end && inRange ) {
				Number value = binContents.get(i);
				total = total + value.doubleValue();
				count++;
			}
		}
		if ( count != 0 ) {
			result = (total/count);
		}
		resultList.add((T) result);
		return resultList;
	}


	@Override
	public Collection<String> getMetaDataProperties () {
		return _base.getMetaDataProperties();
	}


	@Override
	public String getMetaData (String property) {
		return _base.getMetaData(property);
	}


	@Override
	public void setMetaData (String property, Object value) {
		_base.setMetaData(property, value);
	}

}
