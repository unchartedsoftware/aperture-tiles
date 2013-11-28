package com.oculusinfo.tile.util;

import java.awt.Color;
import java.lang.reflect.Field;

/**
 * Creates a single flat colour with no gradients at all.
 * Always returns the initial colour.
 * 
 * @author cregnier
 *
 */
public class FlatColorRamp implements ColorRamp {

	protected ColorRampParameter params;
	private int col;
	
	
	/**
	 * Creates the ramp for the given colour and opacity. 
	 * @param opacity
	 * 	The opacity level from 0-255.
	 * @param rgbCol
	 * 	The rgb colour.
	 */
	public FlatColorRamp(ColorRampParameter params) {
		this.params = params;

		int opacity = params.getInt("opacity");
		int rgbCol = getRGBFromParams();
		
		this.col = ((opacity & 0xff) << 24) | (rgbCol & 0xffffff);	//merge the opacity and rgb together into argb
	}

	private int getRGBFromParams() {
		int col = 0xffffff;	//initialize to full white
		
		Object o = params.get("color");
		if (o instanceof String) {
			String str = (String)o;
			Color c = null;
			
			//check if the string is a field in Color
			try {
				Field field = Color.class.getField(str.trim().toLowerCase());
				c = (Color)field.get(null);
			}
			catch (Exception e) {
				c = null;
			}
			
			if (c == null) {
				try {
					c = Color.decode(str);
				}
				catch (NumberFormatException e) {
					c = Color.white;
				}
			}
			col = c.getRGB();
		}
		else if (o instanceof Number) {
			col = ((Number)o).intValue();
		}
		
		return col;
	}
	
	
	@Override
	public int getRGB(double scale) {
		return col;
	}
	
}
