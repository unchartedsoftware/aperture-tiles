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

import java.io.Serializable;



/**
 * A simple immutable index of a bin within some tile. There is no knowledge, in
 * this index, of which tile it is that contains the bin - the same bin index
 * could, if needed, be used in multiple tiles.
 * 
 * @author Nathan Kronenfeld
 */
public class BinIndex implements Serializable, Comparable<BinIndex> {
	private static final long serialVersionUID = -1L;



	private int _x;
	private int _y;

	/**
	 * Create a bin index
	 * 
	 * @param x
	 *            The x coordinate of the bin within its tile, 0 indicating the
	 *            left-most bin
	 * @param y
	 *            The y coordinate of the bin within its tile, 0 indicating the
	 *            bottom-most bin
	 */
	public BinIndex (int x, int y) {
		_x = x;
		_y = y;
	}

	/**
	 * Get the x coordinate of the bin
	 */
	public int getX () {
		return _x;
	}

	/**
	 * Get the y coordinate of the bin
	 */
	public int getY () {
		return _y;
	}



	/**
	 * {@inheritDoc}
	 * 
	 * This bin is less than that bin if it is above that bin or directly left
	 * from it.
	 */
	@Override
	public int compareTo (BinIndex that) {
		if (_y < that._y)
			return -1;
		if (_y > that._y)
			return 1;
		if (_x < that._x)
			return -1;
		if (_x > that._x)
			return 1;
		return 0;
	}



	@Override
	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (null == obj) return false;
		if (!(obj instanceof BinIndex)) return false;

		BinIndex b = (BinIndex) obj;
		if (b._x != _x) return false;
		if (b._y != _y) return false;
		return true;
	}

	@Override
	public int hashCode () {
		// First prime over 256 - guarantees uniqueness of hashcodes in the typical case
		return _x+257*_y;
	}

	@Override
	public String toString () {
		return String.format("[%d, %d]", _x, _y);
	}

	/**
	 * Converts from a string to a bin index. This takes in strings of exactly
	 * the form output by {@link #toString()} - i.e., formatted with "[%d, %d]"
	 */
	public static BinIndex fromString (String string) {
		try {
			int a = string.indexOf('[')+1;
			int b = string.indexOf(',', a);
			int x = Integer.parseInt(string.substring(a, b).trim());
			a = b+1;
			b = string.indexOf(']', a);
			int y = Integer.parseInt(string.substring(a+1, b).trim());

			return new BinIndex(x, y);
		} catch (NumberFormatException e) {
			return null;
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
}
