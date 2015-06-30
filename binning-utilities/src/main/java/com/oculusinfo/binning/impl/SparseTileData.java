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



import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileDataMetadataImpl;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.factory.util.Pair;

import java.util.*;

/**
 * This class represents a tile's worth of data as a sparse array.
 *
 * It also contains the tile index describing the position of the tile.
 *
 * This object is not necessarily immutable.
 *
 * @author nkronenfeld
 *
 * @param <T> The type of data stored in the bins of this tile.
 */
public class SparseTileData<T> extends TileDataMetadataImpl<T> implements TileData<T> {
	private static final long serialVersionUID = 1L;



	private TileIndex                        _definition;
	// We keep data as a double-map to avoid having to construct a BinIndex each query.
	private Map<Integer, Map<Integer, T>>    _data;
	private T                                _defaultValue;

	// No-argument constructor, really just for use by Kryo, but we call it from
	// the main constructor just to get rid of the warning.
	private SparseTileData () {
		super();
	}

	/**
	 * Construct an empty sparse tile data object for a particular tile with no data.
	 *
	 * @param definition The index of the tile whose data is to be collected by this object.
	 */
	public SparseTileData (TileIndex definition) {
		this(definition, (T) null);
	}

	/**
	 * Construct an empty, sparse tile for a particular tile index.
	 *
	 * @param definition The index of the tile whose data is to be
	 *            collected by this object.
	 * @param defaultValue The default value of each bin
	 */
	public SparseTileData (TileIndex definition, T defaultValue) {
		this();
		_definition = definition;
		_defaultValue = defaultValue;
		_data = new HashMap<>();
	}

	/**
	 * Construct a tile for a particular tile index, with preset data. Note the passed-in preset data is used as is,
	 * not copied.
	 *
	 * @param definition
	 *            The index of the tile whose data is to be represented by this
	 *            object.
	 * @param tileData
	 *            The data for this tile.  The index of the outer map specifies the X coordinate of the bin; the inner
	 *            map, the Y coordinate.
	 */
	public SparseTileData (TileIndex definition, Map<Integer, Map<Integer, T>> tileData, T defaultValue) {
		super();
		_definition = definition;
		_defaultValue = defaultValue;
		_data = tileData;
	}



	/** {@inheritDoc} */
	@Override
	public TileIndex getDefinition () {
		return _definition;
	}

	/** {@inheritDoc} */
	@Override
	public T getDefaultValue () { return _defaultValue; }

	/** {@inheritDoc} */
	@Override
	public void setBin(int x, int y, T value) {
		if (x < 0 || x >= _definition.getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= _definition.getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}

		if (!_data.containsKey(x)) _data.put(x, new HashMap<Integer, T>());
		_data.get(x).put(y, value);
	}

	/** {@inheritDoc} */
	@Override
	public T getBin(int x, int y) {
		if (x < 0 || x >= _definition.getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= _definition.getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}

		if (_data.containsKey(x)) {
			Map<Integer, T> xMap = _data.get(x);
			if (xMap.containsKey(y)) {
				return xMap.get(y);
			}
		}
		return _defaultValue;
	}

	private static List<Integer> getKeys (Map<Integer, ?> map) {
		List<Integer> vals = new ArrayList<Integer>(map.keySet());
		Collections.sort(vals);
		return vals;
	}

	/**
	 * Get an iterator over all our defined data, ignoring defaulted bins
	 */
	public Iterator<Pair<BinIndex, T>> getData () {
		return new DataIterator();
	}

	/**
	 * Get the default value for bins not defined in our sparse array.
	 */
	public T getDefaultBinValue () {
		return _defaultValue;
	}



	private class DataIterator implements Iterator<Pair<BinIndex, T>> {
		private Iterator<Integer> _xVals = getKeys(_data).iterator();
		private int x;
		private Iterator<Integer> _yVals = null;

		private boolean hasNextY () {
			return (null != _yVals && _yVals.hasNext());
		}

		private boolean incrementX () {
			if (!_xVals.hasNext()) return false;

			x = _xVals.next();
			_yVals = getKeys(_data.get(x)).iterator();
			return true;
		}

		@Override
		public boolean hasNext() {
			// If we can increment Y, then we're done - we have another value
			if (hasNextY()) return true;

			// If we can't increment X, then we're done - we don't have another value
			if (!incrementX()) return false;

			// We may or may not be done, we have to check if there is a Y in the next X.
			return hasNext();
		}

		@Override
		public Pair<BinIndex, T> next() {
			if (hasNextY()) {
				int y = _yVals.next();
				return new Pair<BinIndex, T>(new BinIndex(x, y), getBin(x, y));
			} else if (incrementX()) {
				return next();
			} else {
				return null;
			}
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Illegal to remove elements from SparseTileData.getData()");
		}
	}

	@Override
	public String toString () {
		return "<sparse-tile index=\""+getDefinition()+"\", default=\""+_defaultValue+"\"/>";
	}
}
