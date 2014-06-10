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
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.TileDataImageRenderer;
import com.oculusinfo.tile.rendering.filter.StackBlurFilter;
import com.oculusinfo.tile.util.GraphicsUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

/**
 * An image renderer that works off of tile grids, but instead of rendering
 * a heatmap, calculates some statistics and renders them as text.
 * 
 * @author  dgray
 */
public class DoublesStatisticImageRenderer implements TileDataImageRenderer {
	private static final Logger LOGGER = LoggerFactory.getLogger(DoublesStatisticImageRenderer.class);
	private static final Font   FONT   = new Font("Tahoma", Font.PLAIN, 13);



	public static Class<Double> getRuntimeBinClass () {
		return Double.class;
	}
	public static TypeDescriptor getRuntimeTypeDescriptor () {
		return new TypeDescriptor(Double.class);
	}
	

	/* (non-Javadoc)
	 * @see TileDataImageRenderer#getLevelExtrema(LayerConfiguration)
	 */
	@Override
	public Pair<Double, Double> getLevelExtrema (LayerConfiguration config) throws ConfigurationException {
		return new Pair<Double, Double>(0.0, 0.0);
	}

	/* (non-Javadoc)
	 * @see TileDataImageRenderer#render(LayerConfiguration)
	 */
	@Override
	public BufferedImage render(LayerConfiguration config) {
		BufferedImage bi = null;
		TileIndex tileIndex = null;
		String layer = "?";
		int lineNumber = 0;
 		
		try {
			tileIndex = config.getPropertyValue(LayerConfiguration.TILE_COORDINATE);
			layer = config.getPropertyValue(LayerConfiguration.LAYER_NAME);
			String shortName = config.getPropertyValue(LayerConfiguration.SHORT_NAME);
			int width = config.getPropertyValue(LayerConfiguration.OUTPUT_WIDTH);
			int height = config.getPropertyValue(LayerConfiguration.OUTPUT_HEIGHT);
			lineNumber = config.getPropertyValue(LayerConfiguration.LINE_NUMBER);
			PyramidIO pyramidIO = config.produce(PyramidIO.class);
			TileSerializer<Double> serializer = SerializationTypeChecker.checkBinClass(config.produce(TileSerializer.class),
				     getRuntimeBinClass(),
				     getRuntimeTypeDescriptor());

			bi = GraphicsUtilities.createCompatibleTranslucentImage(width, height);
		
			List<TileData<Double>> tileDatas = pyramidIO.readTiles(layer,
			                                                       serializer,
			                                                       Collections.singleton(tileIndex));
			
			// Missing tiles are commonplace.  We don't want a big long error for that.
			if (tileDatas.size() < 1) {
				LOGGER.info("Missing tile " + tileIndex + " for layer " + layer);
				return null;
			}

			TileData<Double> data = tileDatas.get(0);
			int xBins = data.getDefinition().getXBins();
			int yBins = data.getDefinition().getYBins();
			
			double totalBinCount = 0;
			double maxBinCount = 0;
			double totalNonEmptyBins = 0;
			
			for(int ty = 0; ty < yBins; ty++){
				for(int tx = 0; tx < xBins; tx++){

					double binCount = data.getBin(tx, ty);
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
			
			String text = shortName + ": " + formattedTotal + " " + formattedCoverage;
			drawTextGlow(bi, text, 5, 10 + (20*lineNumber), FONT, Color.white, Color.black);
					
		} catch (Exception e) {
			LOGGER.debug("Tile is corrupt: " + layer + ":" + tileIndex);
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
