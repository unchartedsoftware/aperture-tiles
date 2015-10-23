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
package com.oculusinfo.tile.rendering.color.impl;

import java.awt.Color;

import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.color.ColorRampParameter;

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
     *
	 * @param color The color object
	 * @param opacity The opacity level from 0-255.
	 * 	The rgb colour.
	 */
	public FlatColorRamp (Color color, double opacity) {
	    int alpha = (int) Math.floor(255*opacity) & 0xff;
		this.col = (alpha << 24) | (color.getRGB() & 0xffffff);	//merge the opacity and rgb together into argb
	}

	@Override
		 public int getRGB(double scale) {
		return col;
	}

	@Override
	public int getRGBA(double scale, double alphaScale) {
		return col;
	}

}
