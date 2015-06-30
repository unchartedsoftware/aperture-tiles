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


import java.util.*;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileDataMetadataImpl;
import com.oculusinfo.binning.TileIndex;



/**
 * This class represents a tile's worth of data as a dense array.
 *
 * It also contains the tile index describing the position of the tile.
 *
 * This object is not necessarily immutable.
 *
 * @author nkronenfeld
 *
 * @param <T> The type of data stored in the bins of this tile.
 */
public class DenseTileData<T> extends TileDataMetadataImpl<T> implements TileData<T> {
	private static final long serialVersionUID = 1L;



	private TileIndex           _definition;
	private List<T>             _data;
	private T                   _default;



	// No-argument constructor, really just for use by Kryo, but we call it from
	// the main constructor just to get rid of the warning.
	private DenseTileData() {
		super();
	}

	/**
	 * Construct a dense tile data object for a particular tile. All entries are initialized to null.
	 *
	 * @param definition The index of the tile whose data is to be collected by this object.
	 */
	public DenseTileData(TileIndex definition) {
		this(definition, (T) null);
	}

	/**
	 * Construct a dense tile for a particular tile index. All entries are initialized to the given default value.
	 *
	 * @param definition The index of the tile whose data is to be collected by this object.
	 * @param defaultValue The default value of each bin
	 */
	public DenseTileData(TileIndex definition, T defaultValue) {
		this();
		_definition = definition;
		_data = new ArrayList<T>(_definition.getXBins()
		                         * _definition.getYBins());
		_default = defaultValue;
		for (int x = 0; x < _definition.getXBins(); ++x) {
			for (int y = 0; y < _definition.getYBins(); ++y) {
				_data.add(_default);
			}
		}
	}

	/**
	 * Construct a tile for a particular tile index, with preset data. Note the passed-in preset data is used as is,
	 * not copied.
	 *
	 * @param definition
	 *            The index of the tile whose data is to be represented by this
	 *            object.
	 * @param tileData
	 *            The data for this tile
	 */
	public DenseTileData(TileIndex definition, List<T> tileData) {
		this(definition, null, tileData);
	}

	/**
	 * Construct a tile for a particular tile index, with preset data. Note the passed-in preset data is used as is,
	 * not copied.
	 *
	 * @param definition
	 *            The index of the tile whose data is to be represented by this
	 *            object.
	 * @param defaultValue
	 *            The default value to use for undefined bins; not used, since all bins are defined, but needed
	 *            for some edge cases anyway (such as slicing, when a resultant slice ends up being sparse)
	 * @param tileData
	 *            The data for this tile
	 */
	public DenseTileData(TileIndex definition, T defaultValue, List<T> tileData) {

		_definition = definition;
		int requiredLength = _definition.getXBins() * _definition.getYBins();
		if (tileData.size() != requiredLength) {
			throw new IllegalArgumentException(
			                                   "Data was of the wrong length.  Should have been "
			                                   + requiredLength
			                                   + ", was "
			                                   + tileData.size());
		}
		_data = tileData;
		_default = defaultValue;
	}

	/** {@inheritDoc} */
	@Override
	public TileIndex getDefinition() {
		return _definition;
	}

	/** {@inheritDoc} */
	@Override
	public T getDefaultValue () {return _default;}

	public void setDefaultValue (T defaultValue) {
		_default = defaultValue;
	}

	/** {@inheritDoc} */
	@Override
	public void setBin(int x, int y, T value) {
		if (x < 0 || x >= _definition.getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= _definition.getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}

		_data.set(x + y * _definition.getXBins(), value);
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

		return _data.get(x + y * _definition.getXBins());
	}

	/**
	 * Get all the data for this tile. While this data is row-by-row, use of
	 * this method is intended for users using the data as a block (such as for
	 * I/O) without any need to know what the data itself is - the only thing
	 * most users should know is that the format output here is the same one
	 * expected by {@link #DenseTileData(TileIndex, List)}.
	 */
	public List<T> getData () {
		return Collections.unmodifiableList(_data);
	}

	/**
	 * Get all the data for a given tile, in a form that can be used to initialize a dense tile.
	 */
	static public <T> List<T> getData (TileData<T> tile) {
		if (tile instanceof DenseTileData) {
			return ((DenseTileData<T>) tile).getData();
		} else {
			List<T> result = new ArrayList<>();
			TileIndex idx = tile.getDefinition();
			for (int y = 0; y < idx.getYBins(); ++y) {
				for (int x = 0; x < idx.getXBins(); ++x) {
					result.add(tile.getBin(x, y));
				}
			}
			return result;
		}
	}

	@Override
	public String toString () {
		return "<dense-tile index=\""+getDefinition()+"\", default=\""+_default+"\"/>";
	}
}
