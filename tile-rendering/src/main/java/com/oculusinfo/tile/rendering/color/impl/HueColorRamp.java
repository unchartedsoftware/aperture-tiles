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

import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.color.ColorRampParameter;

/**
 * Generates colours based on the hue value between 'fromVal' and 'toVal'.
 * This ramp expects the following parameters to exist in {@link ColorRampParameter}
 * "from": A value or string that represents a number to use as the start of
 * 		the hue gradient. Values outside the range (0, 1) are clamped.
 * "to": A value or string that represents a number to use as the end of the
 * 		hue gradient. Values outside the range (0, 1) are clamped.
 *
 *
 * @author cregnier
 *
 */
public class HueColorRamp implements ColorRamp {
	private double fromVal = 0.0;
	private double toVal = 0.0;

	private final double clamp(double v, double min, double max) {
		return (v > min)? ((v < max)? v : max): min;
	}

	public HueColorRamp(double from, double to) {
		fromVal = clamp(from, 0.0, 1.0);
		toVal = clamp(to, 0.0, 1.0);
	}

	@Override
		 public int getRGB(double scale) {
		return hslToRGB((toVal - fromVal) * scale + fromVal, 1.0, 0.5);
	}

	@Override
	public int getRGBA(double scale, double alphaValue) {
		return hslToRGB((toVal - fromVal) * scale + fromVal, 1.0, 0.5);
	}

	protected double hueToRGB(double p, double q, double t) {
		if (t < 0) t += 1;
		if (t > 1) t -= 1;
		if (t < 0.1666667) return p + (q - p) * 6 * t;
		if (t < 0.5) return q;
		if (t < 0.6666667) return p + (q - p) * (0.66666667 - t) * 6;
		return p;
	}

	protected int hslToRGB(double h, double s, double l) {
		double r, g, b;
		if (s != 0) {
			double q = (l < 0.5)? l * (1 + s) : l + s - l * s;
			double p = 2 * l - q;
			r = hueToRGB(p, q, h + 0.333333334);
			g = hueToRGB(p, q, h);
			b = hueToRGB(p, q, h - 0.333333334);
		}
		else {
			r = g = b = 1;
		}

		int ir = (int)(r * 255);
		int ig = (int)(g * 255);
		int ib = (int)(b * 255);

		return (0xff << 24) | (ir << 16) | (ig << 8) | ib;
	}
}
