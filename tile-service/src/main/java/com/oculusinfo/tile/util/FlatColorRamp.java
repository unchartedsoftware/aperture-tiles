package com.oculusinfo.tile.util;

/**
 * Creates a single flat colour with no gradients at all.
 * Always returns the initial colour.
 * 
 * @author cregnier
 *
 */
public class FlatColorRamp implements ColorRamp {

	private int col;
	
	/**
	 * Creates the ramp for the given colour and opacity. 
	 * @param opacity
	 * 	The opacity level from 0-255.
	 * @param rgbCol
	 * 	The rgb colour.
	 */
	public FlatColorRamp(int opacity, int rgbCol) {
		this.col = ((opacity & 0xff) << 24) | (rgbCol & 0xffffff);	//merge the opacity and rgb together into argb
	}

	
	@Override
	public int getRGB(double scale) {
		return col;
	}
	
}
