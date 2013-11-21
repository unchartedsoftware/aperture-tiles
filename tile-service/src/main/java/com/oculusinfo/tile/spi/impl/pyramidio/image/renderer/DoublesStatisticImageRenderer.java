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
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TileSerializer;
import com.oculusinfo.binning.io.impl.DoubleAvroSerializer;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.tile.spi.impl.IValueTransformer;
import com.oculusinfo.tile.spi.impl.LinearCappedValueTransformer;
import com.oculusinfo.tile.spi.impl.Log10ValueTransformer;
import com.oculusinfo.tile.spi.impl.pyramidio.image.ColorRampFactory;
import com.oculusinfo.tile.util.ColorRamp;

/**
 * @author  dgray
 */
public class DoublesStatisticImageRenderer implements TileDataImageRenderer {
	private static final Color COLOR_BLANK = new Color(255,255,255,0);
	//private static final Font FONT = new Font("Verdana", Font.BOLD, 16);
	
	private final Logger _logger = LoggerFactory.getLogger(getClass());
	
	private PyramidIO _pyramidIo;
	private TileSerializer<Double> _serializer;


	public DoublesStatisticImageRenderer(PyramidIO pyramidIo) {
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
			bi = new BufferedImage(parameter.outputWidth, parameter.outputWidth, BufferedImage.TYPE_INT_ARGB);

			double maximumValue = Double.parseDouble(parameter.levelMaximums);
			
			ColorRamp ramp = ColorRampFactory.create(parameter.rampType, 50);
			IValueTransformer t = ValueTransformerFactory.create(parameter.transformId, maximumValue);
			
			double scaledLevelMaxFreq = t.transform(maximumValue)*parameter.rangeMax/100;
			double scaledLevelMinFreq = t.transform(maximumValue)*parameter.rangeMin/100;
			
			List<TileData<Double>> tileDatas = _pyramidIo.readTiles(parameter.layer, _serializer, Collections.singleton(parameter.tileCoordinate));
			// Missing tiles are commonplace.  We don't want a big long error for that.
			if (tileDatas.size() < 1) {
			    _logger.info("Missing tile "+parameter.tileCoordinate+" for layer "+parameter.layer);
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
					double transformedValue = t.transform(binCount);
					
					// Only including the binCount in the total if it hasn't been
					// cut out by the range slider (clipped).
					if (binCount > 0
							&& transformedValue >= scaledLevelMinFreq
							&& transformedValue <= scaledLevelMaxFreq) {
						
						totalNonEmptyBins += 1;
						
						if(binCount > maxBinCount){
							maxBinCount = binCount;
						}
						totalBinCount += binCount;
					}					
				}
			}
			
			double coverage = totalNonEmptyBins/(xBins*yBins);
			
			Graphics2D g = bi.createGraphics(); // { Start Graphics2D
			
				Font FONT = new Font("Consolas", Font.PLAIN, 14); // TODO: static-fy this when finalized.
			
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//				double transformedMax = t.transform(maxBinCount);
//				int rgb;
//				
//				rgb = ramp.getRGB(transformedMax);
//				Color scaledColorOfTotal = new Color(rgb);
//				
//				rgb = ramp.getRGB(coverage);
//				Color scaledColorOfCoverage = new Color(rgb);
				
				DecimalFormat decFormat = new DecimalFormat("");
				String formattedTotal 		= "Total   : " + decFormat.format(totalBinCount);
				decFormat = new DecimalFormat("##.##");
				String formattedCoverage 	= "Coverage: " + decFormat.format(coverage * 100) + "%";
				
				g.setFont(FONT);
				Color unscaledColor = Color.yellow.brighter();
				drawText(g, "Sensor Statistics", 	10, 10, unscaledColor);
				drawText(g, formattedTotal, 		20, 30, unscaledColor); //scaledColorOfTotal);
				drawText(g, formattedCoverage, 		20, 50, unscaledColor); //scaledColorOfCoverage);
			
			g.dispose(); // } End Graphics2D
					
		} catch (Exception e) {
			_logger.debug("Tile is corrupt: " + parameter.layer + ":" + parameter.tileCoordinate);
			_logger.debug("Tile error: ", e);
			bi = null;
		}
		return bi;
	}

	/**
	 * @param g
	 * @param text
	 * @param yOffset 
	 * @param xOffset 
	 * @param textColor
	 */
	private void drawText(Graphics2D g, String text, int xOffset, int yOffset, Color textColor) {
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D bounds = fm.getStringBounds(text, g);
		FontRenderContext frc = g.getFontRenderContext();
		
		TextLayout layout = new TextLayout(text, g.getFont(), frc);			
		float sw = (float) layout.getBounds().getWidth();
		float sh = (float) layout.getBounds().getHeight();
		Shape shape = layout.getOutline(AffineTransform.getTranslateInstance(
				bounds.getWidth()/2-sw/2 + xOffset, 
				bounds.getHeight()*0.5+sh/2 + yOffset));
		g.setStroke(new BasicStroke(2.0f));
		//g.setColor(Color.black);
		g.setColor(contrastColor(textColor));
		g.draw(shape);
		//g.setColor(Color.white);
		g.setColor(textColor);
		g.fill(shape);
	}
	
	private Color contrastColor(Color color){
        int d = 0;

        // Counting the perceptive luminance - human eye favors green color... 
        double a = 1 - ( 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue())/255;

        if (a < 0.8)
            d = 0; // bright colors - black
        else
            d = 150; // dark colors - light

        return  new Color(d, d, d);
    }


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNumberOfImagesPerTile (PyramidMetaData metadata) {
		// Double tile rendering always produces a single image.
		return 1;
	}

	static class ValueTransformerFactory {
		/**
		 * @param transform
		 * @param levelMaxFreq
		 * @return
		 */
		public static IValueTransformer create(String transform, double levelMaxFreq) {
			IValueTransformer t;
			if("log10".equalsIgnoreCase(transform)){ // TODO: make a factory
				t = new Log10ValueTransformer(levelMaxFreq);
			}else{
				t = new LinearCappedValueTransformer(levelMaxFreq);
			}
			return t;
		}
	}
}