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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rendering;

import java.awt.image.BufferedImage;

import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.factory.ConfigurationException;

/**
 * A class to encapsulate rendering of tiles into any format potentially used 
 * by the client.  Currently supported formats are: images, and JSON objects.
 * 
 * @author dgray, nkronenfeld
 */
public interface TileDataImageRenderer {

	/**
	 * Render an individual tile based on the given input parameters
	 * 
	 * @param parameter
	 * @return
	 */
	public abstract BufferedImage render (LayerConfiguration config);

	/**
	 * Determine how many images are available to be rendered given a set of
	 * input parameters
	 * 
	 * @param parameter
	 *            The input parameters
	 * @return The number of available images
	 */
	public int getNumberOfImagesPerTile (PyramidMetaData metadata);

	/**
	 * From configuration information, collect metadata and figure out level extrema as needed and possible.
	 * @throws ConfigurationException 
	 */
	public abstract Pair<Double, Double> getLevelExtrema (LayerConfiguration config) throws ConfigurationException;
}
