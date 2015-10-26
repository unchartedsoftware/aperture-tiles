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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.color.FixedPoint;

/**
 * Ramps between a series of evenly distributed intervals defined as an array.
 * Each colour is a single RGB value that can be specified as either hex, an
 * integer, or by word. There is also an optional corresponding array of alphas.
 *
 * @author djonker
 *
 */
public class SteppedGradientColorRamp extends AbstractColorRamp {

	private static final ColorRamp HOT_ONLIGHT = from(Arrays.asList(new Color[] {
			new Color(0xff,0xcc,0x66,0x7f), new Color(0xff,0xaa,0x33), new Color(0xff,0x11,0x00)}));

	private static final ColorRamp HOT_ONDARK = from(Arrays.asList(new Color[] {
			new Color(0x88,0x22,0x00,0x7f), new Color(0xff,0x44,0x00), new Color(0xff,0xff,0xff)}));


	private static final ColorRamp COOL_ONLIGHT = from(Arrays.asList(new Color[] {
			new Color(0x66,0xcc,0xff,0x7f), new Color(0x33,0xaa,0xff), new Color(0x00,0x11,0xff)}));

	private static final ColorRamp COOL_ONDARK = from(Arrays.asList(new Color[] {
			new Color(0x00,0x22,0x88,0x7f), new Color(0x00,0x88,0xff), new Color(0xff,0xff,0xff)}));


	private static final ColorRamp POLAR_ONDARK = from(Arrays.asList(new Color[] {
			new Color(0x33,0xaa,0xff),
			new Color(0x00,0x11,0x44,0xee),
			new Color(0x44,0x44,0x44,0x7f),
			new Color(0x44,0x11,0x00,0xee),
			new Color(0xff,0x44,0x00)
			}));
	private static final ColorRamp POLAR_ONLIGHT = from(Arrays.asList(new Color[] {
			new Color(0x00,0x11,0xff),
			new Color(0x33,0xaa,0xff,0xee),
			new Color(0xbb,0xbb,0xbb,0x7f),
			new Color(0xff,0x44,0x00,0xee),
			new Color(0xff,0x44,0x00)
			}));
	private static final ColorRamp VALENCE_ONDARK = from(Arrays.asList(new Color[] {
			new Color(0xff,0x44,0x00),
			new Color(0x44,0x11,0x00,0xee),
			new Color(0x44,0x44,0x44,0x7f),
			new Color(0x11,0x44,0x00,0xee),
			new Color(0xaa,0xff,0x33)
			}));
	private static final ColorRamp VALENCE_ONLIGHT = from(Arrays.asList(new Color[] {
			new Color(0xff,0x11,0x00),
			new Color(0xff,0xaa,0x33,0xee),
			new Color(0xbb,0xbb,0xbb,0x7f),
			new Color(0x77,0xff,0x00,0xee),
			new Color(0x11,0xaa,0x00)
			}));

	/**
	 * Constructs a stepped gradient in the ramp-standard part exploded form.
	 *
	 * @param reds
	 * 		List of reds.
	 * @param greens
	 * 		List of greens.
	 * @param blues
	 * 		List of blues.
	 * @param alphas
	 * 		List of alphas, which may be null indicating fully opaque.
	 */
	public SteppedGradientColorRamp (List<FixedPoint> reds, List<FixedPoint> greens, List<FixedPoint> blues, List<FixedPoint> alphas) {
		super(false, reds, greens, blues, alphas, 255);
	}

	/**
	 * Creates a stepped gradient from a list of colors.
	 *
	 * @param colors
	 * 		The list of colors to step through at even intervals.
	 * @return
	 * 		The new ramp.
	 */
	public static SteppedGradientColorRamp from(List<Color> colors) {
		List<FixedPoint> red = new ArrayList<>();
		List<FixedPoint> grn = new ArrayList<>();
		List<FixedPoint> blu = new ArrayList<>();
		List<FixedPoint> alp = new ArrayList<>();

		int n = colors.size()-1;

		int i = 0;
		double point = 0.0;
		double interval = 1.0/n;

		for (Color c: colors) {
			red.add(new FixedPoint(point, c.getRed()/255.0));
			grn.add(new FixedPoint(point, c.getGreen()/255.0));
			blu.add(new FixedPoint(point, c.getBlue()/255.0));
			alp.add(new FixedPoint(point, c.getAlpha()/255.0));

			i++;
			point = i < n? i*interval : 1.0;
		}

		return new SteppedGradientColorRamp(red, grn, blu, alp);
	}

	public static ColorRamp hot(boolean onlight) {
		return onlight? HOT_ONLIGHT : HOT_ONDARK;
	}

	public static ColorRamp cool(boolean onlight) {
		return onlight? COOL_ONLIGHT : COOL_ONDARK;
	}

	public static ColorRamp polar(boolean onlight) {
		return onlight? POLAR_ONLIGHT : POLAR_ONDARK;
	}

	public static ColorRamp valence(boolean onlight) {
		return onlight? VALENCE_ONLIGHT : VALENCE_ONDARK;
	}
}
