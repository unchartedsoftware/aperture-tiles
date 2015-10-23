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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.filter.StackBlurFilter;
import com.oculusinfo.tile.util.GraphicsUtilities;

/**
 * An image renderer that works off of tile grids, but instead of rendering
 * a heatmap, calculates some statistics and renders them as text.
 *
 * @author  dgray
 */
public class NumberStatisticImageRenderer implements TileDataImageRenderer<Number> {
	private static final Logger LOGGER = LoggerFactory.getLogger(NumberStatisticImageRenderer.class);
	private static final Font   FONT   = new Font("Tahoma", Font.PLAIN, 13);


	@Override
	public Class<Number> getAcceptedBinClass () {
		return Number.class;
	}

	@Override
	public TypeDescriptor getAcceptedTypeDescriptor() {
		return new TypeDescriptor(getAcceptedBinClass());
	}


	/* (non-Javadoc)
	 * @see TileDataImageRenderer#render(LayerConfiguration)
	 */
	@Override
	public BufferedImage render(TileData<Number> data, TileData<Number> alphaData, LayerConfiguration config) {
		BufferedImage bi;
		String layerId = null;
		TileIndex index = null;

		try {
			layerId = config.getPropertyValue(LayerConfiguration.LAYER_ID);
			index = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);
			int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
			int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);

			bi = GraphicsUtilities.createCompatibleTranslucentImage(width, height);

			int xBins = data.getDefinition().getXBins();
			int yBins = data.getDefinition().getYBins();

			double totalBinCount = 0;
			double maxBinCount = 0;
			double totalNonEmptyBins = 0;

			for(int ty = 0; ty < yBins; ty++){
				for(int tx = 0; tx < xBins; tx++){

					double binCount = data.getBin(tx, ty).doubleValue();
					if (binCount > 0 ){

						totalNonEmptyBins += 1;

						if(binCount > maxBinCount){
							maxBinCount = binCount;
						}
						totalBinCount += binCount;
					}
				}
			}

			double coverage = totalNonEmptyBins/(xBins*yBins);

			DecimalFormat decFormat = new DecimalFormat("");
			String formattedTotal 		= decFormat.format(totalBinCount) + " events   ";
			decFormat = new DecimalFormat("##.##");
			String formattedCoverage 	= decFormat.format(coverage * 100) + "% coverage";

			String text = layerId + ": " + formattedTotal + " " + formattedCoverage;
			drawTextGlow(bi, text, 5, 10, FONT, Color.white, Color.black);

		} catch (Exception e) {
			LOGGER.debug("Tile is corrupt: " + layerId + ":" + index);
			LOGGER.debug("Tile error: ", e);
			bi = null;
		}
		return bi;
	}

	/**
	 * Draw a line of text with a glow around it. Uses fast blurring approximation of gaussian.
	 * TODO: Support calling this multiple times! currently wipes out anything that was there before.
	 *
	 * @param destination
	 * @param text
	 * @param xOffset
	 * @param yOffset
	 * @param font
	 * @param textColor
	 * @param glowColor
	 */
	private static void drawTextGlow (BufferedImage destination, String text, int xOffset, int yOffset, Font font, Color textColor, Color glowColor) {
		Graphics2D g = destination.createGraphics();
		g.setFont(font);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D bounds = fm.getStringBounds(text, g);
		FontRenderContext frc = g.getFontRenderContext();

		TextLayout layout = new TextLayout(text, g.getFont(), frc);
		float sw = (float) layout.getBounds().getWidth();
		float sh = (float) layout.getBounds().getHeight();
		Shape shape = layout.getOutline(AffineTransform.getTranslateInstance(
		                                                                     bounds.getWidth()/2-sw/2 + xOffset,
		                                                                     bounds.getHeight()*0.5+sh/2 + yOffset));

		BufferedImage biText = GraphicsUtilities.createCompatibleImage(destination);
		Graphics2D gText = biText.createGraphics(); // { gText
		gText.setFont(g.getFont());
		gText.setColor(glowColor);
		gText.setStroke(new BasicStroke(2));
		gText.draw(shape);
		gText.dispose(); // } End gText

		StackBlurFilter blur = new StackBlurFilter(3, 3);
		blur.filter(biText, destination);

		g.setColor(textColor);
		g.fill(shape);
		g.dispose();
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
