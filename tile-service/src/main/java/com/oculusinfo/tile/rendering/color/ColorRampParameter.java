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
package com.oculusinfo.tile.rendering.color;

import java.awt.Color;
import java.lang.reflect.Field;

/**
 * Moved these functions from ColorRampFactory.
 * 
 * @author djonker
 */
public class ColorRampParameter {

	/**
	 * Parse a color from a string and return it. In the case of an error
	 * this function will return the default color.
	 * 
	 * @param color
	 * 		The color definition in string form.
	 * @return
	 * 		The interpreted color or the default.
	 */
	public static Color getColor(String color, Color defaultColor) {
		Color c = null;
		try {
			color = color.trim().toLowerCase();
			Field field = Color.class.getField(color);
			c = (Color)field.get(null);
		} catch (Exception e) {
			c = null;
		}
        
		if (c == null) {
			try {
				c = Color.decode(color);
			}
			catch (NumberFormatException e) {
				c = defaultColor;
			}
		}
		return c;
	}
	
	/**
	 * Parse a color from a string and return it. In the case of an error
	 * this function will return the color white.
	 * 
	 * @param color
	 * 		The color definition in string form.
	 * @return
	 * 		The interpreted color or a default of white.
	 */
	public static Color getColor(String color) {
		return getColor(color, Color.WHITE);
	}

	/**
	 * Parse a color from a string, combine it with separately defined alpha and return
	 * it. In the case of an error this function will return the color white.
	 * 
	 * @param color
	 * 		The color definition in string form.
	 * @param alpha
	 * 		The alpha value (0-255) in int form.
	 * 
	 * @return
	 * 		The interpreted color or a default of white.
	 */
	public static Color getColorWithAlpha (String color, int alpha) {
		return getColorWithAlpha(color, alpha, Color.WHITE);
	}
	
	/**
	 * Parse a color from a string, combine it with separately defined alpha and return
	 * it. In the case of an error this function will return the color white.
	 * 
	 * @param color
	 * 		The color definition in string form.
	 * @param alpha
	 * 		The alpha value (0-255) in int form.
	 * 
	 * @return
	 * 		The interpreted color or a default of white.
	 */
	public static Color getColorWithAlpha (String color, int alpha, Color defaultColor) {
		Color base = getColor(color, defaultColor);
		alpha = Math.min(Math.max(alpha, 0), 255);
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}
}
