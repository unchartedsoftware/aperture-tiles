package com.oculusinfo.tile.util;

/**
 * Transforms a value into a colour.
 * Each colour ramp will transform a 0-1 scale value to its
 * own domain. 
 *  
 * @author cregnier
 */
public interface ColorRamp {

	/**
	 * Transform the scale value into a colour. 
	 * @param scale
	 * 	A value between 0 - 1.
	 * @return
	 * 	The rgb colour value.
	 */
	int getRGB(double scale);
	
}
