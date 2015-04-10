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
import java.util.List;

import com.oculusinfo.tile.rendering.ImageRendererFactory;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.transformations.tile.TileTransformer;
import com.oculusinfo.tile.rendering.transformations.value.ValueTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.StringProperty;

/**
 * A server side to render List<Pair<String, Int>> tiles.
 *
 * This renderer by default renders the sum of all key values.
 *
 *
 * @author mkielo
 */

public class NumberListHeatMapImageRenderer implements TileDataImageRenderer<List<Number>> {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private static final Color COLOR_BLANK = new Color(255,255,255,0);

    // This is the only way to get a generified class; because of type erasure,
    // it is definitionally accurate.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Class<List<Number>> getAcceptedBinClass () {
        return (Class) List.class;
    }

    public TypeDescriptor getAcceptedTypeDescriptor () {
        return new TypeDescriptor(List.class, new TypeDescriptor(Number.class));
    }


    private double parseExtremum (LayerConfiguration parameter, StringProperty property, String propName, String layer, double def) {
        String rawValue = parameter.getPropertyValue(property);
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException|NullPointerException e) {
            LOGGER.info("Bad "+propName+" value "+rawValue+" for "+layer+", defaulting to "+def);
            return def;
        }
    }


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
    public BufferedImage render (TileData<List<Number>> data, LayerConfiguration config) {
        BufferedImage bi;
        String layerId = config.getPropertyValue(LayerConfiguration.LAYER_ID);
        TileIndex index = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);
        try {
            int outputWidth = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
            int outputHeight = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
            int rangeMax = config.getPropertyValue(ImageRendererFactory.RANGE_MAX);
            int rangeMin = config.getPropertyValue(ImageRendererFactory.RANGE_MIN);

            bi = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);

            @SuppressWarnings("unchecked")
            ValueTransformer<Number> t = config.produce(ValueTransformer.class);
            int[] rgbArray = new int[outputWidth*outputHeight];

            double scaledMax = (double)rangeMax/100;
            double scaledMin = (double)rangeMin/100;
            double oneOverScaledRange = 1.0 / (scaledMax - scaledMin);

            @SuppressWarnings("unchecked")
            TileTransformer<List<Number>> tileTransformer = config.produce(TileTransformer.class);
            TileData<List<Number>> transformedContents = tileTransformer.transform( data );

            int xBins = data.getDefinition().getXBins();
            int yBins = data.getDefinition().getYBins();

            double xScale = ((double) bi.getWidth())/xBins;
            double yScale = ((double) bi.getHeight())/yBins;
            ColorRamp colorRamp = config.produce(ColorRamp.class);

            for(int ty = 0; ty < yBins; ty++){
                for(int tx = 0; tx < xBins; tx++){
                    //calculate the scaled dimensions of this 'pixel' within the image
                    int minX = (int) Math.round(tx*xScale);
                    int maxX = (int) Math.round((tx+1)*xScale);
                    int minY = (int) Math.round(ty*yScale);
                    int maxY = (int) Math.round((ty+1)*yScale);

                    List<Number> binContents = transformedContents.getBin(tx, ty);
                    double binCount = 0;
                    for(int i = 0; i < binContents.size(); i++) {
                    	if ( binContents.get(i) != null ) {
                    		binCount = binCount + binContents.get(i).doubleValue();
                    	}
                    }

                    //log/linear
                    Number transformedValue = t.transform(binCount);
                    int rgb;
                    if (binCount > 0) {
                        rgb = colorRamp.getRGB( ( transformedValue.doubleValue() - scaledMin ) * oneOverScaledRange );
                    } else {
                        rgb = COLOR_BLANK.getRGB();
                    }

                    //'draw' out the scaled 'pixel'
                    for (int ix = minX; ix < maxX; ++ix) {
                        for (int iy = minY; iy < maxY; ++iy) {
                            int i = iy*bi.getWidth() + ix;
                            rgbArray[i] = rgb;
                        }
                    }
                }
            }

            bi.setRGB(0, 0, outputWidth, outputHeight, rgbArray, 0, outputWidth);
        } catch (Exception e) {
            LOGGER.error("Tile error: " + layerId + ":" + index, e);
            bi = null;
        }
        return bi;
    }


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfImagesPerTile (PyramidMetaData metadata) {
		// Text score rendering always produces a single image.
		return 1;
	}
}
