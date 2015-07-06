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
import java.util.Collection;

/**
 * Created by nkronenfeld on 1/27/2015.
 */
public interface TileData<T> extends Serializable {
	/**
	 * A catalog of available types of ways to store tiles available in the system.
	 */
	public static enum StorageType {
		/** Tiles stored as dense arrays */
		Dense,
		/** Tiles stored as sparse maps, indexed by bin */
		Sparse;
	}



	/**
	 * Get the tile index defining which tile is associated with this data
	 *
	 * @return The TileIndex of the tile
	 */
	TileIndex getDefinition();

	/**
	 * Get the default value to use for any unset bins
	 */
	T getDefaultValue ();

	/**
	 * Set the value of a particular bin in this tile
	 *
	 * @param x The x coordinate of the bin to be changed.
	 * @param y The y coordinate of the bin to be changed.
	 * @param value The value to which to set the bin in question.
	 */
	void setBin (int x, int y, T value);

	/**
	 * Get the value of a particular bin in this tile.
	 *
	 * @param x The x coordinate of the bin to be changed.
	 * @param y The y coordinate of the bin to be changed.
	 * @return The value of the bin in question.
	 */
	T getBin(int x, int y);

	/**
	 * Get the properties listed in the metadata of this tile.
	 *
	 * @return A collection of listed properties.  This can be null if
	 *         there are no listed properties.
	 */
	Collection<String> getMetaDataProperties();

	/**
	 * Get the value of the given metadata property.
	 *
	 * @param property The property of interest
	 * @return The value of the given property, or null if the property
	 *         isn't listed in the tile's metadata.
	 */
	String getMetaData(String property);

	/**
	 * Sets the value for a given property in the tile's metadata.
	 *
	 * @param property The property of interest
	 * @param value The value of said property
	 */
	void setMetaData(String property, Object value);
}
