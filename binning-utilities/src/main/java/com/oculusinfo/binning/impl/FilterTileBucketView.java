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
 *  a view to a range of buckets.
 *
 */
public class FilterTileBucketView<T> implements TileData<List<T>> {
	private static final long serialVersionUID = 1234567890L;

	private TileData<List<T>> _base = null;
	private Integer	_startBucket = null;
	private Integer	_endBucket = null;


	public FilterTileBucketView (TileData<List<T>> base, Integer startBucket, Integer endBucket) {
		_base = base;
		_startBucket = startBucket;
		_endBucket = endBucket;
		if ( _startBucket != null && _endBucket != null ) {
			if ( _startBucket > _endBucket ) {
				throw new IllegalArgumentException( "Constructor for FilterTileBucketView: arguments are invalid.  start bucket: " + _startBucket + ", end bucket: " + _endBucket );
			}
		}
	}


	@Override
	public TileIndex getDefinition () {
		return _base.getDefinition();
	}

	@Override
	public List<T> getDefaultValue () {
		List<T> baseDefault = _base.getDefaultValue();
		if (null == baseDefault) return null;
		else {
			List<T> ourDefault = new ArrayList<>();
			int binSize = baseDefault.size();
			
			// if the bucket range falls outside of the available bin range, return empty list.  null values assume full range
			boolean inRange =  ( _endBucket == null && _startBucket == null )
						    || ( _endBucket >= 0 && _startBucket <= binSize );
			
			// If start or end (but not both) fall outside the bin range, constrain the range to available bin range
			int start = ( _startBucket != null && _startBucket >= 0 ) ? _startBucket : 0;
			int end = ( _endBucket != null && _endBucket < binSize ) ? _endBucket : binSize;

			for(int i = 0; i < binSize; i++) {
				if (i >= start && i <= end && inRange ) {
					ourDefault.add(baseDefault.get(i));
				} else {
					ourDefault.add(null);
				}
			}
			return ourDefault;
		}
	}

	@Override
	public void setBin(int x, int y, List<T> value)  {
		if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}
		_base.setBin( x, y, value );
	}


	@Override
	public List<T> getBin (int x, int y) {
		if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}

		List<T> binContents = _base.getBin(x, y);
		int binSize = binContents.size();
		
		// if the bucket range falls outside of the available bin range, return empty list.  null values assume full range
		boolean inRange =  ( _endBucket == null && _startBucket == null )
					    || ( _endBucket >= 0 && _startBucket <= binSize );
		
		int start = ( _startBucket != null && _startBucket >= 0 ) ? _startBucket : 0;
		int end = ( _endBucket != null && _endBucket < binSize ) ? _endBucket : binSize;

		for(int i = 0; i < binSize; i++) {
			if ( i >= start && i <= end && inRange ) {
				binContents.set(i, binContents.get(i));
			} else {
				binContents.set(i, null);
			}
		}
		return binContents;
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
