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

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;

import java.awt.image.BufferedImage;

/**
 * A class to encapsulate rendering of tiles into any format potentially used
 * by the client.  Currently supported formats are: images, and JSON objects.
 *
 * @author dgray, nkronenfeld
 */
public interface TileDataImageRenderer<T> {

	/**
	 * Retrieves the class of the data type used for the bin data of tiles possible
	 * to render with this renderer. For example, a simple tile renderer may accept
	 * bins of Doubles.
	 * @return class of Bin data type
	 */
	public Class<T> getAcceptedBinClass();

	/**
	 * Returns a type descriptor matching the #getBinClass() results
	 * @return type descriptor for the data accepted by this renderer
	 */
	public TypeDescriptor getAcceptedTypeDescriptor();

	/**
	 * Render an individual tile based on the given input parameters
	 *
	 * @param data The tile data to be rendered
	 * @param config The layer configuration object.
	 * @return The buffered image.
	 */
	public abstract BufferedImage render (TileData<T> data, TileData<T> alphaData, LayerConfiguration config);

	/**
	 * Determine how many images are available to be rendered given a set of
	 * input parameters
	 *
	 * @param metadata The layers meta data pyramid.
	 * @return The number of available images
	 */
	public int getNumberOfImagesPerTile (PyramidMetaData metadata);
}
