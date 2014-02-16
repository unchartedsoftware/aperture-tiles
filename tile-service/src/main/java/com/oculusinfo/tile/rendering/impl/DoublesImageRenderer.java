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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.tile.rendering.RenderParameter;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.color.ColorRampFactory;
import com.oculusinfo.tile.rendering.color.ColorRampParameter;
import com.oculusinfo.tile.rendering.transformations.IValueTransformer;
import com.oculusinfo.tile.rendering.transformations.ValueTransformerFactory;

/**
 * @author  dgray
 */
public class DoublesImageRenderer implements TileDataImageRenderer {
	private static final Color COLOR_BLANK = new Color(255,255,255,0);
	
	private final Logger _logger = LoggerFactory.getLogger(getClass());
	
	private PyramidIO _pyramidIo;
	private TileSerializer<Double> _serializer;

	public DoublesImageRenderer(PyramidIO pyramidIo) {
		_pyramidIo = pyramidIo;
		_serializer = createSerializer();
	}
	
	protected TileSerializer<Double> createSerializer() {
		return new DoubleAvroSerializer();
	}

	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.impl.pyramidio.image.renderer.TileDataImageRenderer#render(com.oculusinfo.tile.spi.impl.pyramidio.image.renderer.RenderParameter)
	 */
	@Override
	public BufferedImage render(RenderParameter parameter) {
 		BufferedImage bi;
		try {  // TODO: harden at a finer granularity.
			int outputWidth = parameter.getOutputWidth();
			int outputHeight = parameter.getOutputHeight();
			int rangeMax = parameter.getAsInt("rangeMax");
			int rangeMin = parameter.getAsInt("rangeMin");
			String layer = parameter.getString("layer");
			int coarseness = Math.max(parameter.getAsIntOrElse("coarseness", 1), 1);

			bi = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);

			double minimumValue = parameter.getAsDouble("levelMinimums");
			double maximumValue = parameter.getAsDouble("levelMaximums");
			
			ColorRamp ramp = ColorRampFactory.create(parameter.getObject("rampType", ColorRampParameter.class), 255);
			IValueTransformer t = ValueTransformerFactory.create(parameter.getObject("transform", Object.class), minimumValue, maximumValue);
			int[] rgbArray = new int[outputWidth*outputHeight];
			
			double scaledLevelMaxFreq = t.transform(maximumValue)*rangeMax/100;
			double scaledLevelMinFreq = t.transform(maximumValue)*rangeMin/100;

			int coursenessFactor = (int)Math.pow(2, coarseness - 1);
			
			//get the tile indexes of the requested base tile and possibly the scaling one further up the tree
			TileIndex baseLevelIndex = parameter.getObject("tileCoordinate", TileIndex.class);
			TileIndex scaleLevelIndex = null; 
			
			List<TileData<Double>> tileDatas = null;
			
			//need to get the tile data for the level of the base level minus the courseness
			for (int coursenessLevel = coarseness - 1; coursenessLevel >= 0; --coursenessLevel) {
				scaleLevelIndex = new TileIndex(baseLevelIndex.getLevel() - coursenessLevel, (int)Math.floor(baseLevelIndex.getX() / coursenessFactor), (int)Math.floor(baseLevelIndex.getY() / coursenessFactor));				
				
				tileDatas = _pyramidIo.readTiles(layer, _serializer, Collections.singleton(scaleLevelIndex));
				if (tileDatas.size() >= 1) {
					//we got data for this level so use it
					break;
				}
			}
			
			// Missing tiles are commonplace and we didn't find any data up the tree either.  We don't want a big long error for that.
			if (tileDatas.size() < 1) {
			    _logger.info("Missing tile " + parameter.getObject("tileCoordinate", TileIndex.class) + " for layer " + layer);
			    return null;
			}
			
			TileData<Double> data = tileDatas.get(0);
			int xBins = data.getDefinition().getXBins();
			int yBins = data.getDefinition().getYBins();
			
			//calculate the tile tree multiplier to go between tiles at each level.
			//this is also the number of x/y tiles in the base level for every tile in the scaled level
			int tileTreeMultiplier = (int)Math.pow(2, baseLevelIndex.getLevel() - scaleLevelIndex.getLevel());
			
			int baseLevelFirstTileY = scaleLevelIndex.getY() * tileTreeMultiplier; 

			//the y tiles are backwards, so we need to shift the order around by reversing the counting direction
			int yTileIndex = ((tileTreeMultiplier - 1) - (baseLevelIndex.getY() - baseLevelFirstTileY)) + baseLevelFirstTileY;
			
			//figure out which bins to use for this tile based on the proportion of the base level tile within the scale level tile
			int xBinStart = (int)Math.floor(xBins * (((double)(baseLevelIndex.getX()) / tileTreeMultiplier) - scaleLevelIndex.getX()));
			int xBinEnd = (int)Math.floor(xBins * (((double)(baseLevelIndex.getX() + 1) / tileTreeMultiplier) - scaleLevelIndex.getX()));
			int yBinStart = ((int)Math.floor(yBins * (((double)(yTileIndex) / tileTreeMultiplier) - scaleLevelIndex.getY())) ) ;
			int yBinEnd = ((int)Math.floor(yBins * (((double)(yTileIndex + 1) / tileTreeMultiplier) - scaleLevelIndex.getY())) ) ;
			
			int numBinsWide = xBinEnd - xBinStart;
			int numBinsHigh = yBinEnd - yBinStart;
			double xScale = ((double) bi.getWidth())/numBinsWide;
			double yScale = ((double) bi.getHeight())/numBinsHigh;
			for(int ty = 0; ty < numBinsHigh; ty++){
				for(int tx = 0; tx < numBinsWide; tx++){
					//calculate the scaled dimensions of this 'pixel' within the image
					int minX = (int) Math.round(tx*xScale);
					int maxX = (int) Math.round((tx+1)*xScale);
					int minY = (int) Math.round(ty*yScale);
					int maxY = (int) Math.round((ty+1)*yScale);

					double binCount = data.getBin(tx + xBinStart, ty + yBinStart);
					double transformedValue = t.transform(binCount);
					int rgb;
					if (binCount > 0
							&& transformedValue >= scaledLevelMinFreq
							&& transformedValue <= scaledLevelMaxFreq) {
						rgb = ramp.getRGB(transformedValue);
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
			_logger.debug("Tile is corrupt: " + parameter.getString("layer") + ":" + parameter.getObject("tileCoordinate", TileIndex.class));
			_logger.debug("Tile error: ", e);
			bi = null;
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