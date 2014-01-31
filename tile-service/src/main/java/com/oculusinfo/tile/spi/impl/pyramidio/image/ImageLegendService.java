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
package com.oculusinfo.tile.spi.impl.pyramidio.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.tile.spi.ImageTileLegendService;
import com.oculusinfo.tile.spi.impl.IValueTransformer;
import com.oculusinfo.tile.spi.impl.LinearCappedValueTransformer;
import com.oculusinfo.tile.util.ColorRamp;
import com.oculusinfo.tile.util.ColorRampParameter;

/**
 * A service that generates an image coloured using the specified
 * ramp type. Used for legends.
 * 
 * @author dgray
 *
 */
@Singleton
public class ImageLegendService implements ImageTileLegendService {
	
	private final Logger _logger = LoggerFactory.getLogger(getClass());
	
	private PyramidIO _pyramidIo;
	private Map<String, JSONObject> _metadataCache = 
			Collections.synchronizedMap(new HashMap<String, JSONObject>());
	
	@Inject
	ImageLegendService (PyramidIO pyramidIo) {
		_pyramidIo = pyramidIo;	
	}
	
	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.TileLegendService#getLegend(int, double, double, int, int)
	 */
	public BufferedImage getLegend(Object transform, ColorRampParameter rampType, String layer, 
			int zoomLevel, int width, int height, boolean doAxis, boolean renderHorizontally) {

		//int levelMinFreq = 0;
		int levelMaxFreq = height;
		
		// For now, commented out. Don't need metadata unless we're rendering labels and need
		// to know the max frequency.
		try {
			JSONObject metadata = getMetadata(layer);
			JSONObject levelMaximums = metadata.getJSONObject("meta").getJSONObject("levelMaximums");
			String zoomLevelString = Integer.toString(zoomLevel);
			Object levelMaximumObject = levelMaximums.get(zoomLevelString);
			
			// Verify we have an actual number. The string "[]" is a valid value we should ignore
			// but not throw an exception for.
			if (levelMaximumObject instanceof Double || levelMaximumObject instanceof Integer || levelMaximumObject instanceof Long) {
				levelMaxFreq = levelMaximums.getInt(zoomLevelString);
			}
		} catch (Exception e1) {
			_logger.error("Failed to extract max frequency from metadata for legend.");
			_logger.debug("Legend exception:", e1);
		}
		
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();	

		ColorRamp ramp = ColorRampFactory.create(rampType, 255);
		
		IValueTransformer t;
		
		//if("log10".equalsIgnoreCase(transform)){
		//	t = new Log10ValueTransformer(levelMaxFreq);
		//}else{
			t = new LinearCappedValueTransformer(levelMaxFreq);
		//}
		
		int fontHeight = 12;
		int barHeight = height - fontHeight;
		int barYOffset = fontHeight/2;
		int labelBarSpacer = 8;
		int barWidth = width - g.getFontMetrics().stringWidth(Integer.toString((int)levelMaxFreq)) - labelBarSpacer;
		int barXOffset = width - barWidth + labelBarSpacer;
		
		if (renderHorizontally){
			for (int i = 0; i < width; i++){
				double v = ((double)(i+1)/(double)width) * levelMaxFreq;
				int colorInt = ramp.getRGB(t.transform(v));		
				g.setColor(new Color(colorInt));
				g.drawLine(i, 0, i, height);
			}
		}
		else{
			//Override the above.
			barHeight = height;
			barYOffset = 0;
			barXOffset = 0;
			
			for(int i = 0; i <= barHeight; i++){
				double v = ((double)(i+1)/(double)barHeight) * levelMaxFreq;
				int colorInt = ramp.getRGB(t.transform(v));		
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
					BigDecimal value = new BigDecimal(levelMaxFreq - levelMaxFreq * ( (double)i/(double)height ), mathContext );
					String label = value.toPlainString();
					int labelWidth = g.getFontMetrics().stringWidth(label);
					
					g.drawString(label, textRightEdge - labelWidth, i+(fontHeight) );
					g.drawLine(barXOffset-6, i+barYOffset, barXOffset-3, i+barYOffset);
				}
			}
			
			//g.drawString(Integer.toString(levelMinFreq), 0, height);
		}
		
		g.dispose();
		
		return bi;
	}

	/**
	 * @param layer
	 * @param pyramidIo 
	 * @return
	 */
	private JSONObject getMetadata(String layer) {
		JSONObject metadata = _metadataCache.get(layer);
		if(metadata == null){
			try {
				String s = _pyramidIo.readMetaData(layer);
				metadata = new JSONObject(s);
				_metadataCache.put(layer, metadata);
			} catch (Exception e) {
				_logger.error("Metadata file for layer '" + layer + "' is missing or corrupt.");
				_logger.debug("Metadata error: ", e);
			}
		}
		return metadata;
	}

}
