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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.color.ColorRamp;

/**
 * A server side to render Map<String, Double> (well, technically,
 * List<Pair<String, Double>>) tiles.
 * 
 * This renderer by default renders the top scores, rendering up to 10 per bin.
 * To render more, fewer, or different texts, override
 * {@link #getTextsToDraw(List)}.
 * 
 * @author nkronenfeld
 */
public class TopTextScoresImageRenderer implements TileDataImageRenderer {
	private final Logger _logger = LoggerFactory.getLogger(getClass());

	// Best we can do here :-(
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Class<List<Pair<String, Double>>> getRuntimeBinClass () {
		return (Class)List.class;
	}
	public static TypeDescriptor getRuntimeTypeDescriptor () {
		return new TypeDescriptor(List.class,
		                          new TypeDescriptor(Pair.class,
		                                             new TypeDescriptor(String.class),
		                                             new TypeDescriptor(Double.class)));
	}



	private void drawScoredText (Graphics2D g, Pair<String, Double> textScore, double offsetFromCenter,
	                             int minX, int maxX, int minY, int maxY,
	                             int rowHeight, int barHeight, int padding,
	                             ColorRamp ramp, double scale) {
		int centerX = (minX + maxX) / 2;
		int centerY = (minY + maxY) / 2;
		int baseline = (int) Math.round(centerY + offsetFromCenter * rowHeight - padding);

		int barBaseline = baseline - (rowHeight - 2*padding - barHeight)/2;
		// For bar purposes, value should be between -1 and 1
		double value = textScore.getSecond()/scale;
		// For color purposes, value should be between 0 and 1
		double colorValue = (value+1.0)/2.0;
		int barWidth = (int)Math.round((maxX-centerX)*0.8*value);

		String text = textScore.getFirst();
		FontMetrics metrics = g.getFontMetrics();
		int textBaseline = baseline;

		g.setColor(new Color(ramp.getRGB(colorValue)));
		if (barWidth > 0) {
			g.fillRect(centerX+padding, barBaseline, barWidth, barHeight);
		} else {
			g.fillRect(centerX+barWidth-padding, barBaseline, -barWidth, barHeight);
		}

		g.setColor(new Color(255, 255, 128, 192));
		if (barWidth < 0) {
			g.drawString(text, centerX+padding, textBaseline);
		} else {
			int textWidth = metrics.stringWidth(text);
			g.drawString(text, centerX-padding-textWidth, textBaseline);
		}
	}

	@Override
	public Pair<Double, Double> getLevelExtrema (LayerConfiguration config) throws ConfigurationException {
		return new Pair<Double, Double>(0.0, 0.0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BufferedImage render(LayerConfiguration config) {
		BufferedImage bi;
		String layer = config.getPropertyValue(LayerConfiguration.LAYER_NAME);
		TileIndex index = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);
		try {
			int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
			int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
			PyramidIO pyramidIO = config.produce(PyramidIO.class);
			TileSerializer<List<Pair<String, Double>>> serializer = SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
				                         getRuntimeBinClass(),
				                         getRuntimeTypeDescriptor());

			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

			List<TileData<List<Pair<String, Double>>>> tileDatas = pyramidIO.readTiles(layer, serializer, Collections.singleton(index));
			if (tileDatas.isEmpty()) {
				_logger.debug("Layer {} is missing tile ().", layer, index);
				return null;
			}
			TileData<List<Pair<String, Double>>> data = tileDatas.get(0);
			int xBins = data.getDefinition().getXBins();
			int yBins = data.getDefinition().getYBins();

			Graphics2D g = bi.createGraphics();
			// Transparent background
			g.setColor(new Color(0, 0, 0, 0));
			g.fillRect(0, 0, width, height);

			int rowHeight = 16;
			int barHeight = 3;
			int padding = 2;
			ColorRamp colorRamp = config.produce(ColorRamp.class);

			for (int x=0; x<xBins; ++x) {
				for (int y=0; y<yBins; ++y) {
					int xMin = x*width/xBins;
					int xMax = (x+1)*width/xBins;
					int yMin = y*height/yBins;
					int yMax = (y+1)*height/yBins;

					List<Pair<String, Double>> cellData = new ArrayList<Pair<String, Double>>(data.getBin(x, y));
					if (cellData.size()>0) {
						Collections.sort(cellData, new Comparator<Pair<String, Double>>() {
								@Override
								public int compare(Pair<String, Double> p1,
								                   Pair<String, Double> p2) {
									if (p1.getSecond() < p2.getSecond()) return -1;
									else if (p1.getSecond() > p2.getSecond()) return 1;
									else return 0;
								}
							});
						double minVal = cellData.get(0).getSecond();
						double maxVal = cellData.get(cellData.size()-1).getSecond();
						double scaleVal = Math.max(Math.abs(minVal), Math.abs(maxVal));

						int[] toDraw = getTextsToDraw(cellData);
						int n = toDraw.length;

						g.setClip(null);
						g.clipRect(xMin, yMin, xMax-xMin, yMax-yMin);
						for (int i=0; i<n; ++i) {
							double offset = (2*i + 1 - n) / 2.0;
							drawScoredText(g, cellData.get(toDraw[i]), offset,
							               xMin, xMax, yMin, yMax, rowHeight, barHeight, padding, colorRamp, scaleVal);
						}
					}
				}
			}
		} catch (Exception e) {
			_logger.debug("Tile is corrupt: " + layer + ":" + index);
			_logger.debug("Tile error: ", e);
			bi = null;
		}
		return bi;
	}

	/**
	 * This function returns which scored texts to use.  The default prints up to the top 10 texts.
	 */
	protected int[] getTextsToDraw (List<Pair<String, Double>> cellData) {
		int n = Math.min(10, cellData.size());
		int[] result = new int[n];

		for (int i=0; i<n; ++i) {
			result[n-1-i] = i;
		}

		return result;
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
