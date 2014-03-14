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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.transformations.IValueTransformer;
import com.oculusinfo.tile.rendering.transformations.ValueTransformerFactory;

/**
 * A renderer that renders tiles of series of doubles
 * 
 * @author nkronenfeld
 */
public class DoublesSeriesImageRenderer implements TileDataImageRenderer {
	private static final Color COLOR_BLANK = new Color(255,255,255,0);


	// Best we can do here :-(
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Class<List<Double>> getRuntimeBinClass () {
        return (Class)List.class;
    }
    public static TypeDescriptor getRuntimeTypeDescriptor () {
        return new TypeDescriptor(List.class,
                                  new TypeDescriptor(Double.class));
    }



	private final Logger _logger = LoggerFactory.getLogger(getClass());
	


	/**
	 * 
	 */
    public DoublesSeriesImageRenderer () {
	}

	@Override
	public BufferedImage render (LayerConfiguration config) {
 		BufferedImage bi;
        String layer = config.getPropertyValue(LayerConfiguration.LAYER_NAME);
        TileIndex index = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);
		try {  // TODO: harden at a finer granularity.
			int outputWidth = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
			int outputHeight = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
			int rangeMax = config.getPropertyValue(LayerConfiguration.RANGE_MAX);
			int rangeMin = config.getPropertyValue(LayerConfiguration.RANGE_MIN);

			bi = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);

			double minimumValue;
            String rawValue = config.getPropertyValue(LayerConfiguration.LEVEL_MINIMUMS);
			try {
			    minimumValue = Double.parseDouble(rawValue);
			} catch (NumberFormatException e) {
			    _logger.warn("Expected a numeric minimum for level, got {}", rawValue);
			    minimumValue = 0.0;
			}
			
			double maximumValue;
			rawValue = config.getPropertyValue(LayerConfiguration.LEVEL_MAXIMUMS);
			try {
			    maximumValue = Double.parseDouble(rawValue);
			} catch (NumberFormatException e) {
			    _logger.warn("Expected a numeric maximum for level, got {}", rawValue);
			    maximumValue = 1000.0;
			}
			IValueTransformer t = ValueTransformerFactory.create(config.getPropertyValue(LayerConfiguration.TRANSFORM), minimumValue, maximumValue);
			int[] rgbArray = new int[outputWidth*outputHeight];
			
			double scaledLevelMaxFreq = t.transform(maximumValue)*rangeMax/100;
			double scaledLevelMinFreq = t.transform(maximumValue)*rangeMin/100;

			PyramidIO pyramidIO = config.produce(PyramidIO.class);
			TileSerializer<List<Double>> serializer = SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
			                                                                                 getRuntimeBinClass(),
			                                                                                 getRuntimeTypeDescriptor());
			List<TileData<List<Double>>> tileDatas = pyramidIO.readTiles(layer, serializer, Collections.singleton(index));
			// Missing tiles are commonplace.  We don't want a big long error for that.
			if (tileDatas.size() < 1) {
			    _logger.info("Missing tile " + index + " for layer " + layer);
			    return null;
			}

			// Default to 0 if the default of -1 is given
			int currentImage = Math.max(0, config.getPropertyValue(LayerConfiguration.CURRENT_IMAGE));
			
			TileData<List<Double>> data = tileDatas.get(0);
			int xBins = data.getDefinition().getXBins();
			int yBins = data.getDefinition().getYBins();

			double xScale = ((double) outputWidth)/xBins;
			double yScale = ((double) outputHeight)/yBins;

			ColorRamp colorRamp = config.produce(ColorRamp.class);
			for(int ty = 0; ty < yBins; ty++){
				for(int tx = 0; tx < xBins; tx++){
					int minX = (int) Math.round(tx*xScale);
					int maxX = (int) Math.round((tx+1)*xScale);
					int minY = (int) Math.round(ty*yScale);
					int maxY = (int) Math.round((ty+1)*yScale);

					List<Double> binCounts = data.getBin(tx, ty);
					double binCount = 0;
					if (binCounts != null && binCounts.size() > currentImage) {
						binCount= binCounts.get(currentImage);
					}
					double transformedValue = t.transform(binCount);
					int rgb;
					if (binCount > 0
							&& transformedValue >= scaledLevelMinFreq
							&& transformedValue <= scaledLevelMaxFreq) {
						rgb = colorRamp.getRGB(transformedValue);
					} else {
						rgb = COLOR_BLANK.getRGB();
					}

					for (int ix = minX; ix < maxX; ++ix) {
						for (int iy = minY; iy < maxY; ++iy) {
							int i = iy*outputWidth + ix;
							rgbArray[i] = rgb;
						}
					}
				}
			}
			
			bi.setRGB(0, 0, outputWidth, outputHeight, rgbArray, 0, outputWidth);
					
		} catch (Exception e) {
			_logger.debug("Tile is corrupt: " + layer + ":" + index);
			_logger.debug("Tile error: ", e);
			bi = null;
		}
		return bi;
	}

	@Override
	public int getNumberOfImagesPerTile(PyramidMetaData metadata) {
	    int minFrames = Integer.MAX_VALUE;
	    Map<Integer, String> levelMaximums = metadata.getLevelMaximums();
	    for (String value: levelMaximums.values()) {
	        String lvlMax = value.toLowerCase();
	        if (lvlMax.startsWith("list(") && lvlMax.endsWith(")")) {
	            lvlMax = lvlMax.substring(5, lvlMax.length()-1);
	        }
	        String[] maxesByFrame = lvlMax.split(",");
	        int frames = maxesByFrame.length;
	        if (frames < minFrames) minFrames = frames;
	    }
	    if (minFrames == Integer.MAX_VALUE) return 0;
	    return minFrames;
	}

}
