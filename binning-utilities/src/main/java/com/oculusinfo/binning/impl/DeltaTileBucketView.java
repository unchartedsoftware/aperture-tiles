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
package com.oculusinfo.binning.impl;



import java.util.Collection;
import java.util.List;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.util.BinaryOperator;



/**
 * This implementation of TileData takes a TileData whose bins are lists of buckets and presents
 *  a view to a range of buckets that are compared to the same range of buckets from another
 *  TileData object that is compatible. The caller can specify what operation to use for the comparison.
 */
public class DeltaTileBucketView<T> implements TileData<List<T>> {
	private static final long serialVersionUID = 1234567890L;

	private TileData<List<T>> _base = null;
	private TileData<List<T>> _delta = null;
	private BinaryOperator _operator = null;
	private Integer	_startCompare = null;
	private Integer	_endCompare = null;


	public DeltaTileBucketView (TileData<List<T>> base, TileData<List<T>> delta, BinaryOperator.OPERATOR_TYPE op,
								int startComp, int endComp) {
		_base = base;
		_delta = delta;
		_operator = new BinaryOperator(op);
		_startCompare = startComp;
		_endCompare = endComp;

		List<T> sourceData = _base.getBin(0, 0);
		List<T> deltaData = _delta.getBin(0, 0);

		if (   getDefinition().getXBins() != delta.getDefinition().getXBins()
			|| getDefinition().getYBins() != delta.getDefinition().getYBins()
			|| sourceData.size() != deltaData.size() ){
			throw new IllegalArgumentException("Constructor for DeltaTileBucketView: arguments are invalid. Tiles to compare are incompatible");
		}
	}


	@Override
	public TileIndex getDefinition () {
		return _base.getDefinition();
	}


	@Override
	public void setBin (int x, int y, List<T> value) {
		if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}
		_base.setBin(x, y, (List<T>)value);
	}


	@SuppressWarnings("unchecked")
	@Override
	public List<T> getBin (int x, int y) {
		if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}
		List<T> sourceData = _base.getBin(x, y);
		List<T> deltaData = _delta.getBin(x, y);

		int binSize = sourceData.size();
		int start = ( _startCompare != null ) ? _startCompare : 0;
		int end = ( _endCompare != null && _endCompare < binSize ) ? _endCompare : binSize;

		for(int i = 0; i < binSize; i++) {
			if ( i >= start && i <= end ) {
				sourceData.set(i, (T)_operator.calculate( (Number)sourceData.get(i), (Number)deltaData.get(i)));
			} else {
				sourceData.set(i, null);
			}
		}
		return sourceData;
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
