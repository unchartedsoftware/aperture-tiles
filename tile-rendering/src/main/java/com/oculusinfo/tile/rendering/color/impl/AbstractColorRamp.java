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
package com.oculusinfo.tile.rendering.color.impl;

import com.oculusinfo.binning.util.JsonUtilities;
import com.oculusinfo.tile.rendering.color.ColorRamp;
import com.oculusinfo.tile.rendering.color.FixedPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractColorRamp implements ColorRamp {
	private boolean isInverted;
	protected List<FixedPoint> reds = new ArrayList<FixedPoint>();
	protected List<FixedPoint> greens = new ArrayList<FixedPoint>();
	protected List<FixedPoint> blues = new ArrayList<FixedPoint>();
	protected List<FixedPoint> alphas = new ArrayList<FixedPoint>();

	public AbstractColorRamp (boolean inverted, List<FixedPoint> reds, List<FixedPoint> greens, List<FixedPoint> blues, List<FixedPoint> alphas, double opacity){
		this.isInverted = inverted;
		this.reds = reds;
		this.greens = greens;
		this.blues = blues;
		if (null == alphas) {
			//there's no alphas, so initialize them with the single opacity field
			this.alphas = new ArrayList<>();
			this.alphas.add(new FixedPoint(0, opacity));
			this.alphas.add(new FixedPoint(1, opacity));
		} else {
			this.alphas = alphas;
		}
	}




	public int getRGB(double scale) {
		return smoothBetweenFixedPoints(reds, greens, blues, alphas,
		                                (this.isInverted ? 1-scale : scale), 1.0);
	}

	public int getRGBA(double scale, double alphaScale) {
		return smoothBetweenFixedPoints(reds, greens, blues, alphas,
			(this.isInverted ? 1-scale : scale),
			(this.isInverted ? 1-alphaScale : alphaScale));
	}

	public static double luminosity(int r, int g, int b) {
		return (r*0.2126f + g*0.7152f + b*0.0722f)/0xFF;
	}

	public static double luminosity(int color) {
		return luminosity((color&0xFF0000)>>16, (color&0xFF00)>>8, color&0xFF);
	}

	public static double getGreenForRBL(double r, double b, double l) {
		return (l-r*0.2126f-b*0.0722f)/0.7152f;
	}

	public static double getRedForGBL(double g, double b, double l) {
		return (l-g*0.7152f-b*0.0722f)/0.2126f;
	}

	public static double valueFromFixedPoints(List<FixedPoint> values, double scale) {
		FixedPoint start = values.get(0);
		if (scale<start.getScale()) return start.getValue();
		for (int i=1; i<values.size(); i++) {
			FixedPoint pt = values.get(i);
			if (scale<pt.getScale()) {
				return ((scale-start.getScale())*pt.getValue() + (pt.getScale()-scale)*start.getValue())/(pt.getScale()-start.getScale());
			}
			start = pt;
		}
		return start.getValue();
	}

	public static int smoothBetweenFixedPoints(List<FixedPoint> reds, List<FixedPoint> greens,
	                                           List<FixedPoint> blues, List<FixedPoint> alphas, double scale, double alphaScale) {
		int r = (int)(valueFromFixedPoints(reds,scale) * 255);
		int b = (int)(valueFromFixedPoints(blues,scale) * 255);
		int g = 0;
		int a = (int)(valueFromFixedPoints(alphas, scale)*alphaScale * 255);
		if(greens.size() > 0){
			g = (int)(valueFromFixedPoints(greens,scale) * 255);
		} else {
			g = (int)(getGreenForRBL(r/255.0, b/255.0 , 0.1+(scale*0.9)) * 255);
		}
		r = Math.max(0, Math.min(0xFF, r));
		g = Math.max(0, Math.min(0xFF, g));
		b = Math.max(0, Math.min(0xFF, b));
		a = Math.max(0, Math.min(0xFF, a));

		return (a << 24) | (r << 16) | (g << 8) | b;
	}


	//---------------------------------------------
	// Parsing helpers
	//---------------------------------------------

	/**
	 * Converts a list of items into a {@link FixedPoint}.
	 * Uses the first element in the list as the scale, and the second element
	 * as the value.
	 * If the list doesn't have enough items, or the elements are not numbers,
	 * then it treats them as 0.
	 *
	 * @param list
	 * 	A list of numbers.
	 * @return
	 * 	Returns a {@link FixedPoint} based on the values in the list.
	 */
	protected static FixedPoint getFixedPointFromList(List<?> list) {
		double scale = 0;
		double value = 0;

		int numItems = list.size();
		if (numItems >= 1) {
			//the scale should be the first item
			scale = JsonUtilities.getNumber(list.get(0)).doubleValue();
		}

		if (numItems >= 2) {
			//the value should be the second item
			value = JsonUtilities.getNumber(list.get(1)).doubleValue();
		}

		return new FixedPoint(scale, value);
	}

	/**
	 * Converts a map into a {@link FixedPoint}.
	 * The map should contain a key labelled 'scale' or '0' for scale,
	 * and 'value' or '1' for value.
	 * If anything is missing, then it's considered 0.
	 *
	 * @param map
	 * 	A string->object map to pull objects from.
	 * @return
	 * 	Returns a {@link FixedPoint} constructed from the elements of the map.
	 */
	protected static FixedPoint getFixedPointFromMap(Map<?, ?> map) {
		double scale = 0;
		double value = 0;

		if (map.containsKey("scale")) {
			scale = JsonUtilities.getNumber(map.get("scale")).doubleValue();
		}
		else if (map.containsKey("value")) {
			value = JsonUtilities.getNumber(map.get("value")).doubleValue();
		}
		else if (map.containsKey("0")) {
			scale = JsonUtilities.getNumber(map.get("0")).doubleValue();
		}
		else if (map.containsKey("1")) {
			value = JsonUtilities.getNumber(map.get("1")).doubleValue();
		}

		return new FixedPoint(scale, value);
	}

	protected static List<FixedPoint> getFixedPointList(List<?> list) {
		int numPoints = list.size();
		List<FixedPoint> pointList = new ArrayList<FixedPoint>(numPoints);
		for (int i = 0; i < numPoints; ++i) {
			Object obj = list.get(i);
			if (obj != null) {
				FixedPoint point = null;
				if (obj instanceof List) {
					//the element is a list, so it should be 2 numbers that form a FixedPoint
					point = getFixedPointFromList((List<?>)obj);
				}
				else if (obj instanceof Map) {
					point = getFixedPointFromMap((Map<?, ?>)obj);
				}
				else {
					point = new FixedPoint(0, 0);
				}
				pointList.add(point);
			}
		}

		return pointList;
	}

}
