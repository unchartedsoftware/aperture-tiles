/**
 * Copyright (C) 2013 Oculus Info Inc. 
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
package com.oculusinfo.tile.spi.impl.pyramidio.image.renderer;

import com.oculusinfo.binning.TileIndex;

/**
 * TODO: just make this a generic property bag maybe.
 * 
 * @author  dgray
 * 
 */
public class RenderParameter {
	/**
	 * 
	 */
	public String layer;

	/**
	 * 
	 */
	public String rampType;

	/**
	 * 
	 */
	public String transformId;

	/**
	 * 
	 */
	public int rangeMin;

	/**
	 * 
	 */
	public int rangeMax;

	/**
	 * 
	 */
	public int outputWidth;

	/**
	 * 
	 */
	public int outputHeight;

	/**
	 * 
	 */
	public String levelMaximums;

	/**
	 * 
	 */
	public TileIndex tileCoordinate;

	/**
	 * 
	 */
	public int currentImage;



	/**
	 * 
	 */
	public RenderParameter(String layer, String rampType, String transformId,
			int rangeMin, int rangeMax, int dimension, String levelMaximums,
			TileIndex tileCoordinate, int currentImage) {
		this.layer = layer;
		this.rampType = rampType;
		this.transformId = transformId;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.outputWidth = dimension;
		this.outputHeight = dimension;
		this.levelMaximums = levelMaximums;
		this.tileCoordinate = tileCoordinate;
		this.currentImage = currentImage;
	}
}