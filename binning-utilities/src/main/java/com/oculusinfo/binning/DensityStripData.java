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



/**
 * A tile for use in density strips. Density strips are intended for tiling
 * one-dimensional data, but in a way that the standard two-dimensional
 * visualizations that display two-dimensional tiles can use. To do this, they
 * make an equivalency class of each column in the tile - so if a value is set
 * for one point, it is set for the whole column.
 * 
 * @author nkronenfeld
 */
public class DensityStripData<T> extends TileData<T> {
	private static final long serialVersionUID = -6730290410026585691L;



	public DensityStripData (TileIndex definition) {
		super(definition);
	}

	@Override
	public void setBin (int x, int y, T value) {
		for (int i = 0; i < getDefinition().getYBins(); ++i) {
			super.setBin(x, i, value);
		}
	}
}
