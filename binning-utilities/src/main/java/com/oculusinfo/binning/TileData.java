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
package com.oculusinfo.binning;


import java.io.Serializable;
import java.util.*;



/**
 * This class represents a tile's worth of data.
 * 
 * It also contains the tile index describing the position of the tile.
 * 
 * This object is not necessarily immutable.
 * 
 * @author nkronenfeld
 * 
 * @param <T> The type of data stored in the bins of this tile.
 */
public class TileData<T> implements Serializable {
	private static final long serialVersionUID = 1L;



    private TileIndex           _definition;
    private List<T>             _data;
    private Map<String, String> _metaData;



	// No-argument constructor, really just for use by Kryo, but we call it from
	// the main constructor just to get rid of the warning.
	private TileData () {
	}

	/**
	 * Construct a tile data object for a particular tile. All entries are
	 * initialized to null.
	 * 
	 * @param definition The index of the tile whose data is to be collected by
	 *            this object.
	 */
	public TileData (TileIndex definition) {
		this(definition, (T) null);
	}

	/**
	 * Construct a set of tile data for a particular tile. All entries are
	 * initialized to the given default value.
	 * 
	 * @param definition The index of the tile whose data is to be
	 *            collected by this object.
	 * @param defaultValue The default value of each bin
	 */
	public TileData (TileIndex definition, T defaultValue) {
		this();
		_definition = definition;
		_data = new ArrayList<T>(_definition.getXBins()
		                         * _definition.getYBins());
		for (int x = 0; x < _definition.getXBins(); ++x) {
			for (int y = 0; y < _definition.getYBins(); ++y) {
				_data.add(defaultValue);
			}
		}
		_metaData = null;
	}

    /**
     * Construct a set of tile data for a particular tile, with preset data.
     * Note the passed-in preset data is used as is, not copied.
     * 
     * @param definition
     *            The index of the tile whose data is to be represented by this
     *            object.
     * @param tileData
     *            The data for this tile
     */
	public TileData (TileIndex definition, List<T> tileData) {
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
	}

	/**
	 * Get the tile index defining which tile is associated with this data
	 * 
	 * @return The TileIndex of the tile
	 */
	public TileIndex getDefinition () {
		return _definition;
	}

	/**
	 * Set the value of a particular bin in this tile
	 * 
	 * @param x The x coordinate of the bin to be changed.
	 * @param y The y coordinate of the bin to be changed.
	 * @param value The value to which to set the bin in question.
	 */
	public void setBin (int x, int y, T value) {
		_data.set(x + y * _definition.getXBins(), value);
	}

	/**
	 * Get the value of a particular bin in this tile.
	 * 
	 * @param x The x coordinate of the bin to be changed.
	 * @param y The y coordinate of the bin to be changed.
	 * @return The value of the bin in question.
	 */
	public T getBin (int x, int y) {
		return _data.get(x + y * _definition.getXBins());
	}

	/**
	 * Get all the data for this tile. While this data is row-by-row, use of
	 * this method is intended for users using the data as a block (such as for
	 * I/O) without any need to know what the data itself is - the only thing
	 * most users should know is that the format output here is the same one
	 * expected by {@link #TileData(TileIndex, List)}.
	 */
	public List<T> getData () {
		return Collections.unmodifiableList(_data);
	}

	/**
	 * Get the properties listed in the metadata of this tile.
	 *
	 * @return A collection of listed properties.  This can be null if 
	 *         there are no listed properties.
	 */
	public Collection<String> getMetaDataProperties () {
		if (null == _metaData) return null;
		return _metaData.keySet();
	}

	/**
	 * Get the value of the given metadata property.
	 *
	 * @param property The property of interest
	 * @return The value of the given property, or null if the property
	 *         isn't listed in the tile's metadata.
	 */
	public String getMetaData (String property) {
		if (null != _metaData) return _metaData.get(property);
		return null;
	}

	/**
	 * Sets the value for a given property in the tile's metadata.
	 *
	 * @param property The property of interest
	 * @param value The value of said property
	 */
	public void setMetaData (String property, Object value) {
		if (null == _metaData) {
			_metaData = new HashMap<>();
		}
		if (null == value) {
		    _metaData.put(property, null);
		} else {
		    _metaData.put(property, value.toString());
		}
	}
}
