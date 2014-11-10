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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.transformations.ValueTransformer;
import com.oculusinfo.tile.rendering.transformations.LinearCappedValueTransformer;
import com.oculusinfo.tile.rendering.transformations.ValueTransformerFactory;
import com.oculusinfo.tile.rest.layer.LayerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

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

    private LayerService _layerService;

    @Inject
	LegendServiceImpl( LayerService layerService ) {
        _layerService = layerService;
	}
	
	/* (non-Javadoc)
	 * @see LegendService#getLegend(Object, ColorRampParameter, String, int, int, int, boolean, boolean)
	 */
	public BufferedImage getLegend( String layer, int width, int height, boolean renderHorizontally, JSONObject query ) {

        LayerConfiguration config = _layerService.getLayerConfiguration( layer, query );
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
			
			ValueTransformer t = new LinearCappedValueTransformer(min, max, levelMax);

			if ( renderHorizontally ) {
				for (int i = 0; i < width; i++){
					double v = ((double)(i+1)/(double)width) * levelMax;
					int colorInt = colorRamp.getRGB(t.transform(v));		
					g.setColor(new Color(colorInt, true));
					g.drawLine(i, 0, i, height);
				}
			} else {
				//Override the above.
				int barHeight = height;
				int barYOffset = 0;
				int barXOffset = 0;
    			
				for(int i = 0; i <= barHeight; i++){
					double v = ((double)(i+1)/(double)barHeight) * levelMax;
					int colorInt = colorRamp.getRGB(t.transform(v));		
					g.setColor(new Color(colorInt, true));
					int y = barHeight-i+barYOffset;
					g.drawLine(barXOffset, y, width, y);
				}
			}
			g.dispose();

		} catch (ConfigurationException e) {
			LOGGER.warn("Error attempting to get legend - mis-configured layer");
		} catch (IllegalArgumentException e) {
            LOGGER.info( "Renderer configuration not recognized." );
        }
		return bi;
	}
}
