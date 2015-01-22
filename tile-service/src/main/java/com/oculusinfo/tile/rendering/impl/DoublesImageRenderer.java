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
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.transformations.value.ValueTransformer;
import com.oculusinfo.tile.util.TileDataView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Collections;
import java.util.List;

/**
 * @author  dgray
 */
public class DoublesImageRenderer implements TileDataImageRenderer {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoublesImageRenderer.class);
	private static final Color COLOR_BLANK = new Color(255,255,255,0);
	public static Class<Double> getRuntimeBinClass () {
		return Double.class;
	}
	public static TypeDescriptor getRuntimeTypeDescriptor () {
		return new TypeDescriptor(Double.class);
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
	public BufferedImage render (LayerConfiguration config) {
		BufferedImage bi;
		String layerId = config.getPropertyValue(LayerConfiguration.LAYER_ID);
		String dataId = config.getPropertyValue(LayerConfiguration.DATA_ID);
		TileIndex index = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);
		try {
			int coarseness = config.getPropertyValue(LayerConfiguration.COARSENESS);
			int coarsenessFactor = (int)Math.pow(2, coarseness - 1);

			PyramidIO pyramidIO = config.produce(PyramidIO.class);
			TileSerializer<Double> serializer = SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
			                                                                           getRuntimeBinClass(),
			                                                                           getRuntimeTypeDescriptor());

			List<TileData<Double>> tileDatas = null;

			// Get the coarseness-scaled true tile index
			TileIndex scaleLevelIndex = null;
			// need to get the tile data for the level of the base level minus the coarseness
			for (int coarsenessLevel = coarseness - 1; coarsenessLevel >= 0; --coarsenessLevel) {
				scaleLevelIndex = new TileIndex(index.getLevel() - coarsenessLevel,
				                                (int)Math.floor(index.getX() / coarsenessFactor),
				                                (int)Math.floor(index.getY() / coarsenessFactor));

				tileDatas = pyramidIO.readTiles(dataId, serializer, Collections.singleton(scaleLevelIndex));
				if (tileDatas.size() >= 1) {
					//we got data for this level so use it
					break;
				}
			}

			// Missing tiles are commonplace and we didn't find any data up the tree either.  We don't want a big long error for that.
			if (tileDatas.size() < 1) {
				LOGGER.info("Missing tile " + index + " for layer " + layerId);
				return null;
			}

			TileData<Double> data = tileDatas.get(0);

			if (scaleLevelIndex != null) {
				data = new TileDataView<Double>(data, index);
			}

			bi = render(config, data);

		} catch (Exception e) {
			LOGGER.warn("Tile is corrupt: " + layerId + ":" + index);
			LOGGER.warn("Tile error: ", e);
			bi = null;
		}
		return bi;
	}


	private BufferedImage render(LayerConfiguration config, TileData<Double> data) {
		int outputWidth = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
		int outputHeight = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
		BufferedImage bi = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);

		try {
			int rangeMax = config.getPropertyValue(LayerConfiguration.RANGE_MAX);
			int rangeMin = config.getPropertyValue(LayerConfiguration.RANGE_MIN);

			double maximumValue = getLevelExtrema(config).getSecond();

			ValueTransformer<Double> t = config.produce(ValueTransformer.class);
			double scaledLevelMaxFreq = t.transform(maximumValue)*rangeMax/100;
			double scaledLevelMinFreq = t.transform(maximumValue)*rangeMin/100;

			ColorRamp colorRamp = config.produce(ColorRamp.class);

			bi = renderImage(data, t, scaledLevelMinFreq, scaledLevelMaxFreq, colorRamp, bi);
		} catch (Exception e) {
			LOGGER.warn("Configuration error: ", e);
			return null;
		}

		return bi;
	}

	private BufferedImage renderImage(TileData<Double> data,
								   ValueTransformer<Double> t, double valueMin, double valueMax,
								   ColorRamp colorRamp, BufferedImage bi) {

		int outWidth = bi.getWidth();
		int outHeight = bi.getHeight();
		int xBins = data.getDefinition().getXBins();
		int yBins = data.getDefinition().getYBins();

		float xScale = outWidth / xBins;
		float yScale = outHeight / yBins;

		System.out.println(xScale);

		double oneOverScaledRange = 1.0 / (valueMax - valueMin);

		int[] rgbArray = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();

		for(int ty = 0; ty < yBins; ty++){
			for(int tx = 0; tx < xBins; tx++){
				//calculate the scaled dimensions of this 'pixel' within the image
				int minX = (int) Math.round(tx*xScale);
				int maxX = (int) Math.round((tx+1)*xScale);
				int minY = (int) Math.round(ty*yScale);
				int maxY = (int) Math.round((ty+1)*yScale);

				double binCount = data.getBin(tx, ty);
				double transformedValue = t.transform(binCount);
				// Clamp to [0,1], values out of range get ramp end values
				transformedValue = Math.max(Math.min(transformedValue, 1), 0);
				int rgb;

				if (binCount > 0) {
					rgb = colorRamp.getRGB( ( transformedValue - valueMin ) * oneOverScaledRange );
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
		// Double tile rendering always produces a single image.
		return 1;
	}

}
