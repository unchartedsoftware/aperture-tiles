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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rest.legend;

import com.google.inject.Singleton;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.transformations.IValueTransformer;
import com.oculusinfo.tile.rendering.transformations.LinearCappedValueTransformer;
import com.oculusinfo.tile.rendering.transformations.ValueTransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * A service that generates an image coloured using the specified
 * ramp type. Used for legends.
 * 
 * @author dgray
 *
 */
@Singleton
public class LegendServiceImpl implements LegendService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LegendServiceImpl.class);

	LegendServiceImpl () {
	}
	
	/* (non-Javadoc)
	 * @see LegendService#getLegend(Object, ColorRampParameter, String, int, int, int, boolean, boolean)
	 */
	public BufferedImage getLegend (LayerConfiguration config, String layer, 
	                                int zoomLevel, int width, int height, boolean doAxis, boolean renderHorizontally) {
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();	

		try {
			ColorRamp colorRamp = config.produce(ColorRamp.class);
			
			// legend always uses a linear capped value transform - don't use layer config specified transform
			double levelMax = config.getPropertyValue(ValueTransformerFactory.LAYER_MAXIMUM);
			
			double max;
			if (config.hasPropertyValue(ValueTransformerFactory.TRANSFORM_MAXIMUM)) {
				max = config.getPropertyValue(ValueTransformerFactory.TRANSFORM_MINIMUM);
			}
			else {
				max = levelMax;
			}

			double min;
			if (config.hasPropertyValue(ValueTransformerFactory.TRANSFORM_MINIMUM)) {
				min = config.getPropertyValue(ValueTransformerFactory.TRANSFORM_MINIMUM);
			}
			else {
				min = config.getPropertyValue(ValueTransformerFactory.LAYER_MINIMUM);
			}
			
			IValueTransformer t = new LinearCappedValueTransformer(min, max, levelMax);
			
			int fontHeight = 12;
			int barHeight = height - fontHeight;
			int barYOffset = fontHeight/2;
			int labelBarSpacer = 8;
			int barWidth = width - g.getFontMetrics().stringWidth(Integer.toString((int)levelMax)) - labelBarSpacer;
			int barXOffset = width - barWidth + labelBarSpacer;
    		
			if (renderHorizontally){
				for (int i = 0; i < width; i++){
					double v = ((double)(i+1)/(double)width) * levelMax;
					int colorInt = colorRamp.getRGB(t.transform(v));		
					g.setColor(new Color(colorInt));
					g.drawLine(i, 0, i, height);
				}
			} else {
				//Override the above.
				barHeight = height;
				barYOffset = 0;
				barXOffset = 0;
    			
				for(int i = 0; i <= barHeight; i++){
					double v = ((double)(i+1)/(double)barHeight) * levelMax;
					int colorInt = colorRamp.getRGB(t.transform(v));		
					g.setColor(new Color(colorInt));
					int y = barHeight-i+barYOffset;
					g.drawLine(barXOffset, y, width, y);
				}
			}
    		
			if(doAxis){
				// We currently don't support rendering labels for horizontal legends
				if (!renderHorizontally){ 
					// Draw text labels.
					int textRightEdge = barXOffset - labelBarSpacer;
					int numInnerLabels = height/fontHeight/4;
					int labelStep = height/numInnerLabels;
					MathContext mathContext = new MathContext(2, RoundingMode.DOWN);
					g.setColor(Color.black);
    				
					g.drawLine(barXOffset-3, barYOffset, barXOffset-3, barYOffset+barHeight);
					//g.drawString(Integer.toString(levelMaxFreq), 0, fontHeight);
    				
					for(int i = 0; i < height+fontHeight; i+=labelStep){
						BigDecimal value = new BigDecimal(levelMax - levelMax * ( (double)i/(double)height ), mathContext );
						String label = value.toPlainString();
						int labelWidth = g.getFontMetrics().stringWidth(label);
    					
						g.drawString(label, textRightEdge - labelWidth, i+(fontHeight) );
						g.drawLine(barXOffset-6, i+barYOffset, barXOffset-3, i+barYOffset);
					}
				}
    			
				//g.drawString(Integer.toString(levelMinFreq), 0, height);
			}
    		
			g.dispose();
		} catch (ConfigurationException e) {
			LOGGER.warn("Error attempting to get legend - mis-configured layer");
		}
		return bi;
	}
}
