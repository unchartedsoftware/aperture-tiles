/**
 * Copyright (c) 2013 Oculus Info Inc.
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
package com.oculusinfo.tile.spi.impl.pyramidio.image.renderer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TileSerializer;
import com.oculusinfo.binning.io.impl.DoubleArrayAvroSerializer;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.tile.spi.impl.IValueTransformer;
import com.oculusinfo.tile.spi.impl.pyramidio.image.ColorRampFactory;
import com.oculusinfo.tile.spi.impl.pyramidio.image.renderer.DoublesImageRenderer.ValueTransformerFactory;
import com.oculusinfo.tile.util.ColorRamp;

/**
 * A renderer that renders tiles of series of doubles
 * 
 * @author nkronenfeld
 */
public class DoublesSeriesImageRenderer implements TileDataImageRenderer {
	private static final Color COLOR_BLANK = new Color(255,255,255,0);
	
	private final Logger _logger = LoggerFactory.getLogger(getClass());
	
	private PyramidIO _pyramidIo;
	private TileSerializer<List<Double>> _serializer;

	/**
	 * 
	 */
	@Inject
	public DoublesSeriesImageRenderer (PyramidIO pyramidIo) {
		_pyramidIo = pyramidIo;
		_serializer = new DoubleArrayAvroSerializer();
	}

	@Override
	public BufferedImage render (RenderParameter parameter) {
 		BufferedImage bi;
		try {  // TODO: harden at a finer granularity.
			bi = new BufferedImage(parameter.outputWidth, parameter.outputWidth, BufferedImage.TYPE_INT_ARGB);
			
			ColorRamp ramp = ColorRampFactory.create(parameter.rampType, 255);
			double maximumValue;
			try {
			    maximumValue = Double.parseDouble(parameter.levelMaximums);
			} catch (NumberFormatException e) {
			    _logger.warn("Expected a numeric maximum for level, got {}", parameter.levelMaximums);
			    maximumValue = 1000.0;
			}
			IValueTransformer t = ValueTransformerFactory.create(parameter.transformId, maximumValue);
			int[] rgbArray = new int[parameter.outputWidth*parameter.outputWidth];
			
			double scaledLevelMaxFreq = t.transform(maximumValue)*parameter.rangeMax/100;
			double scaledLevelMinFreq = t.transform(maximumValue)*parameter.rangeMin/100;
			
			List<TileData<List<Double>>> tileDatas = _pyramidIo.readTiles(parameter.layer, _serializer, Collections.singleton(parameter.tileCoordinate));
			// Missing tiles are commonplace.  We don't want a big long error for that.
			if (tileDatas.size() < 1) {
			    _logger.info("Missing tile "+parameter.tileCoordinate+" for layer "+parameter.layer);
			    return null;
			}

			TileData<List<Double>> data = tileDatas.get(0);
			int xBins = data.getDefinition().getXBins();
			int yBins = data.getDefinition().getYBins();
			
			double xScale = ((double) parameter.outputWidth)/xBins;
			double yScale = ((double) parameter.outputHeight)/yBins;
			for(int ty = 0; ty < yBins; ty++){
				for(int tx = 0; tx < xBins; tx++){
					int minX = (int) Math.round(tx*xScale);
					int maxX = (int) Math.round((tx+1)*xScale);
					int minY = (int) Math.round(ty*yScale);
					int maxY = (int) Math.round((ty+1)*yScale);

					List<Double> binCounts = data.getBin(tx, ty);
					double binCount = 0;
					if (binCounts != null && binCounts.size() > parameter.currentImage) {
						binCount= binCounts.get(parameter.currentImage);
					}
					double transformedValue = t.transform(binCount);
					int rgb;
					if (binCount > 0
							&& transformedValue >= scaledLevelMinFreq
							&& transformedValue <= scaledLevelMaxFreq) {
						rgb = ramp.getRGB(transformedValue);
					} else {
						rgb = COLOR_BLANK.getRGB();
					}

					for (int ix = minX; ix < maxX; ++ix) {
						for (int iy = minY; iy < maxY; ++iy) {
							int i = iy*parameter.outputWidth + ix;
							rgbArray[i] = rgb;
						}
					}
				}
			}
			
			bi.setRGB(0, 0, parameter.outputWidth, parameter.outputWidth, rgbArray, 0, parameter.outputWidth);
					
		} catch (Exception e) {
			_logger.debug("Tile is corrupt: " + parameter.layer + ":" + parameter.tileCoordinate);
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
