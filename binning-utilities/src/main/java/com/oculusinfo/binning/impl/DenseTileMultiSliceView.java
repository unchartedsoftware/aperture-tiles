/*
 * Copyright (c) 2015 Uncharted Software Inc. http://www.uncharted.software/
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

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This implementation of TileData takes a TileData whose bins are lists of buckets, and presents
 * a view to a subset of those buckets - the same buckets in each bin.
 *
 * @author nkronenfeld
 */
public class DenseTileMultiSliceView<T> implements TileData<List<T>> {
	private TileData<List<T>> _base;
	private List<Integer>     _slices;
	public DenseTileMultiSliceView (TileData<List<T>> base, List<Integer> slices) {
		_base = base;
		_slices = Collections.unmodifiableList(new ArrayList<>(slices));
	}
	public DenseTileMultiSliceView (TileData<List<T>> base, int minSlice, int maxSlice) {
		_base = base;
		List<Integer> slices = new ArrayList<>();
		for (int i=minSlice; i <= maxSlice; ++i) slices.add(i);
		_slices = Collections.unmodifiableList(slices);
	}

	@Override
	public TileIndex getDefinition() {
		return _base.getDefinition();
	}

	@Override
	public List<T> getDefaultValue() {
		List<T> base = _base.getDefaultValue();
		if (null == base) return null;
		List<T> ourDefault = new ArrayList<>();
		for (int slice: _slices) {
			if (base.size() > slice) ourDefault.add(base.get(slice));
			else ourDefault.add(null);
		}
		return ourDefault;
	}

	@Override
	public void setBin(int x, int y, List<T> value) {
		assert(value.size() == _slices.size());
		List<T> binOrig = _base.getBin(x, y);
		try {
			for (int i=0; i < _slices.size(); ++i) {
				binOrig.set(_slices.get(i), value.get(i));
			}
		} catch (UnsupportedOperationException e) {
			// List is probably unmodifiable; just create a new one then.
			// Create a copy
			List<T> newValue = new ArrayList<>(binOrig);
			// Modify our values
			for (int i=0; i < _slices.size(); ++i) {
				newValue.set(_slices.get(i), value.get(i));
			}
			// Replace into the original
			// Original was unmodifiable, make this unmodifiable too
			_base.setBin(x, y, Collections.unmodifiableList(newValue));
		} catch (NullPointerException e) {
			// No original list; make one.
			List<T> newValue = new ArrayList<>();
			for (int i=0; i < _slices.size(); ++i) {
				int slice = _slices.get(i);
				while (newValue.size() <= slice) newValue.add(null);
				newValue.set(slice, value.get(i));
			}
			_base.setBin(x, y, newValue);
		}
	}

	private boolean binNullForUs (List<T> baseValue) {
		if (null == baseValue) return true;
		for (int slice: _slices) {
			if (baseValue.size() > slice && null != baseValue.get(slice)) return false;
		}
		return true;
	}
	@Override
	public List<T> getBin(int x, int y) {
		List<T> origValue = _base.getBin(x, y);
		if (binNullForUs(origValue)) return null;
		List<T> result = new ArrayList<>(_slices.size());
		for (int slice: _slices) {
			if (origValue.size() <= slice) result.add(null);
			else result.add(origValue.get(slice));
		}
		return result;
	}

	/* Determine, if hardened, if this tile should be dense or sparse */
	private boolean isDense () {
		TileIndex index = getDefinition();
		int nullBins = 0;
		int bins = 0;
		for (int x=0; x<index.getXBins(); ++x) {
			for (int y=0; y<index.getYBins(); ++y) {
				if (binNullForUs(_base.getBin(x, y))) ++nullBins;
				++bins;
			}
		}

		return nullBins*2 < bins;
	}

	public TileData<List<T>> harden () {
		TileIndex index = getDefinition();
		if (isDense()) {
			DenseTileData<List<T>> hardened = new DenseTileData<>(index);
			hardened.setDefaultValue(_base.getDefaultValue());

			for (int x=0; x<index.getXBins(); ++x) {
				for (int y=0; y<index.getYBins(); ++y) {
					hardened.setBin(x, y, getBin(x, y));
				}
			}

			return hardened;
		} else {
			SparseTileData<List<T>> hardened = new SparseTileData<>(index, _base.getDefaultValue());

			for (int x=0; x<index.getXBins(); ++x) {
				for (int y=0; y<index.getYBins(); ++y) {
					List<T> value = getBin(x, y);
					if (null != value) {
						hardened.setBin(x, y, getBin(x, y));
					}
				}
			}

			return hardened;
		}
	}

	@Override
	public Collection<String> getMetaDataProperties() {
		return _base.getMetaDataProperties();
	}

	@Override
	public String getMetaData(String property) {
		return _base.getMetaData(property);
	}

	@Override
	public void setMetaData(String property, Object value) {
		_base.setMetaData(property, value);
	}
}
