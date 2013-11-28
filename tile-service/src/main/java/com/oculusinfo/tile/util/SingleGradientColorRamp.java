package com.oculusinfo.tile.util;

import java.awt.Color;
import java.lang.reflect.Field;

/**
 * Ramps between a 'from' colour and a 'to' colour.
 * Each colour is a single RGB value that can be specified as either hex, an
 * integer, or by word. Each colour also has an alpha value that can be
 * manipulated by adding '-alpha' to the key. ex. 'from-alpha'
 * 
 * @author cregnier
 *
 */
public class SingleGradientColorRamp extends AbstractColorRamp {

	public SingleGradientColorRamp(ColorRampParameter params) {
		super(params);
	}
	
	@Override
	public void initRampPoints() {
		
		Color fromCol = getColorFromParams("from");
		Color toCol = getColorFromParams("to");
		
		reds.add(new FixedPoint(0, (double)fromCol.getRed() / 255));
		reds.add(new FixedPoint(1, (double)toCol.getRed() / 255));
		greens.add(new FixedPoint(0, (double)fromCol.getGreen() / 255));
		greens.add(new FixedPoint(1, (double)toCol.getGreen() / 255));
		blues.add(new FixedPoint(0, (double)fromCol.getBlue() / 255));
		blues.add(new FixedPoint(1, (double)toCol.getBlue() / 255));
		alphas.add(new FixedPoint(0, (double)fromCol.getAlpha() / 255));
		alphas.add(new FixedPoint(1, (double)toCol.getAlpha() / 255));
	}

	private Color getColorFromParams(String key) {
		Color col = Color.white;	//initialize to full white
		int alpha = 0xff;
		
		Object o = rampParams.get(key);
		if (o instanceof String) {
			String str = (String)o;
			
			//check if the string is a field in Color
			try {
				Field field = Color.class.getField(str.trim().toLowerCase());
				col = (Color)field.get(null);
			}
			catch (Exception e) {
				//colour wasn't a colour name, so check if we can decode it as a value
				try {
					col = Color.decode(str);
				}
				catch (NumberFormatException e2) {
					col = Color.white;
				}
			}
			
		}
		else if (o instanceof Number) {
			col = new Color(((Number)o).intValue());
		}
		
		//grab the alpha value if it exists
		o = rampParams.get(key + "-alpha");
		if (o instanceof String) {
			String str = (String)o;
			try {
				//try to parse the number as base 10
				alpha = Integer.parseInt(str, 10);
			}
			catch (NumberFormatException e1) {
				//not base 10, so try to parse as hex
				try {
					alpha = Integer.parseInt(str, 16);
				}
				catch (NumberFormatException e2) {
					//don't know what it is, so just assume full alpha
					alpha = 0xff;
				}
			}
		}
		else if (o instanceof Number) {
			alpha = ((Number)o).intValue();
		}

		//return the colour with the alpha
		return new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha);
	}
	

}
