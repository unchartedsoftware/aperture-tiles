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
package com.oculusinfo.tile.rendering.impl;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.transformations.value.ValueTransformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * @author  dgray
 */
public class NumberImageRenderer implements TileDataImageRenderer<Number> {

	private static final Logger LOGGER = LoggerFactory.getLogger(NumberImageRenderer.class);
	private static final Color COLOR_BLANK = new Color(255,255,255,0);

	@Override
	public Class<Number> getAcceptedBinClass () {
		return Number.class;
	}

	@Override
	public TypeDescriptor getAcceptedTypeDescriptor () {
		return new TypeDescriptor(getAcceptedBinClass());
	}


	private double parseExtremum (LayerConfiguration parameter, StringProperty property, String propName, String layer, double def) {
		String rawValue = parameter.getPropertyValue(property);
		try {
			return Double.parseDouble(rawValue);
		} catch (NumberFormatException|NullPointerException e) {
			LOGGER.warn("Bad "+propName+" value "+rawValue+" for "+layer+", defaulting to "+def);
			return def;
		}
	}

	/* (non-Javadoc)
	 * @see TileDataImageRenderer#getLevelExtrema()
	 */
	@Override
	public Pair<Double, Double> getLevelExtrema (LayerConfiguration config) throws ConfigurationException {
		String layer = config.getPropertyValue(LayerConfiguration.LAYER_ID);
		double minimumValue = parseExtremum(config, LayerConfiguration.LEVEL_MINIMUMS, "minimum", layer, 0.0);
		double maximumValue = parseExtremum(config, LayerConfiguration.LEVEL_MAXIMUMS, "maximum", layer, 1000.0);
		return new Pair<>(minimumValue,  maximumValue);
	}

	/* (non-Javadoc)
	 * @see TileDataImageRenderer#render(LayerConfiguration)
	 */
	@Override
	public BufferedImage render(TileData<Number> data, LayerConfiguration config) {
		int outputWidth = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
		int outputHeight = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
		BufferedImage bi = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);

		try {
			int rangeMax = config.getPropertyValue(LayerConfiguration.RANGE_MAX);
			int rangeMin = config.getPropertyValue(LayerConfiguration.RANGE_MIN);

			// This is the best we can do; supress the warning and move on.
			@SuppressWarnings("unchecked")
			ValueTransformer<Number> t = config.produce(ValueTransformer.class);
			double scaledMax = (double)rangeMax/100;
			double scaledMin = (double)rangeMin/100;

			ColorRamp colorRamp = config.produce(ColorRamp.class);

			bi = renderImage(data, t, scaledMin, scaledMax, colorRamp, bi);
		} catch (Exception e) {
			LOGGER.warn("Configuration error: ", e);
			return null;
		}

		return bi;
	}

	protected BufferedImage renderImage(TileData<Number> data,
								   ValueTransformer<Number> t, double valueMin, double valueMax,
								   ColorRamp colorRamp, BufferedImage bi) {

		int outWidth = bi.getWidth();
		int outHeight = bi.getHeight();
		int xBins = data.getDefinition().getXBins();
		int yBins = data.getDefinition().getYBins();

		float xScale = outWidth / xBins;
		float yScale = outHeight / yBins;

		double oneOverScaledRange = 1.0 / (valueMax - valueMin);

		int[] rgbArray = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();

		for(int ty = 0; ty < yBins; ty++){
			for(int tx = 0; tx < xBins; tx++){
				//calculate the scaled dimensions of this 'pixel' within the image
				int minX = (int) Math.round(tx*xScale);
				int maxX = (int) Math.round((tx+1)*xScale);
				int minY = (int) Math.round(ty*yScale);
				int maxY = (int) Math.round((ty+1)*yScale);

				double binCount = data.getBin(tx, ty).doubleValue();
				Number transformedValue = t.transform(binCount).doubleValue();
				int rgb;

				if (binCount > 0) {
					rgb = colorRamp.getRGB( ( transformedValue.doubleValue() - valueMin ) * oneOverScaledRange );
				} else {
					rgb = COLOR_BLANK.getRGB();
				}

				//'draw' out the scaled 'pixel'
				for (int ix = minX; ix < maxX; ++ix) {
					for (int iy = minY; iy < maxY; ++iy) {
						int i = iy*outWidth + ix;
						rgbArray[i] = rgb;
					}
				}
			}
		}

		return bi;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfImagesPerTile (PyramidMetaData metadata) {
		// Number tile rendering always produces a single image.
		return 1;
	}

}
