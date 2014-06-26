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
package com.oculusinfo.binning;



/**
 * An immutable container class containing both a tile and a bin, for use as a
 * return value by methods that need to return both.
 * 
 * This class represents a specific bin within a specific tile, as opposed to
 * BinIndex, which represents a specific bin, but in any tile.
 * 
 * @author nkronenfeld
 */
public class TileAndBinIndices {
	private TileIndex _tile;
	private BinIndex _bin;
	public TileAndBinIndices (TileIndex tile, BinIndex bin) {
		_tile = tile;
		_bin = bin;
	}



	/**
	 * Get the tile containing the bin specifically represented by this object.
	 */
	public TileIndex getTile () {
		return _tile;
	}

	/**
	 * Get the specific bin represented by this object.
	 */
	public BinIndex getBin () {
		return _bin;
	}



	@Override
	public String toString () {
		return _tile.toString()+":"+_bin.toString();
	}

	@Override
	public int hashCode() {
		return _tile.hashCode()+7*_bin.hashCode();
	}

	@Override
	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (null == obj) return false;
		if (!(obj instanceof TileAndBinIndices)) return false;
        
		TileAndBinIndices tb = (TileAndBinIndices) obj;
		if (!_tile.equals(tb.getTile())) return false;
		if (!_bin.equals(tb.getBin())) return false;
		return true;
	}
}
