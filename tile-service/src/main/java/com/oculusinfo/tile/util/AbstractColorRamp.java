/**
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
package com.oculusinfo.tile.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.utilities.jsonprocessing.JsonUtilities;

public abstract class AbstractColorRamp implements ColorRamp {

	private static final Logger logger = LoggerFactory.getLogger(AbstractColorRamp.class);
	
	static class FixedPoint {
		double scale;
		double value;
		public FixedPoint(double scale, double value) {
			this.scale = scale;
			this.value = value;
		}
	}
    protected List<FixedPoint> reds = new ArrayList<FixedPoint>();
    protected List<FixedPoint> blues = new ArrayList<FixedPoint>();
    protected List<FixedPoint> greens = new ArrayList<FixedPoint>();
    protected List<FixedPoint> alphas = new ArrayList<FixedPoint>();
    private boolean isInverted = false;
    
    protected ColorRampParameter rampParams;

	
	public AbstractColorRamp(ColorRampParameter params){
		rampParams = params;
		
		this.isInverted = Boolean.parseBoolean(rampParams.getString("inverse"));
		initRampPoints();
		
		if (alphas.size() == 0) {
			//there's no alphas, so initialize them with the single opacity field
			int alpha = rampParams.getInt("opacity");
			alphas.add(new FixedPoint(0, alpha));
			alphas.add(new FixedPoint(1, alpha));
		}
	}

	public abstract void initRampPoints();


	/**
	 * Can be used by subclasses to pull the red ramp points from the {@link ColorRampParameter}
	 */
	protected void initRedRampPointsFromParams() {
		if (rampParams.contains("reds")) {
			try {
				reds = getFixedPointList(rampParams.getList("reds"));
			}
			catch (Exception e) {
				logger.error("Problem initializing red ramp points", e);
			}
		}
	}
	
	/**
	 * Can be used by subclasses to pull the blue ramp points from the {@link ColorRampParameter}
	 */
	protected void initBlueRampPointsFromParams() {
		if (rampParams.contains("blues")) {
			try {
				blues = getFixedPointList(rampParams.getList("blues"));
			}
			catch (Exception e) {
				logger.error("Problem initializing blue ramp points", e);
			}
		}
	}
	
	/**
	 * Can be used by subclasses to pull the green ramp points from the {@link ColorRampParameter}
	 */
	protected void initGreenRampPointsFromParams() {
		if (rampParams.contains("greens")) {
			try {
				greens = getFixedPointList(rampParams.getList("greens"));
			}
			catch (Exception e) {
				logger.error("Problem initializing green ramp points", e);
			}
		}
	}
	
	/**
	 * Can be used by subclasses to pull the alphas ramp points from the {@link ColorRampParameter}
	 */
	protected void initAlphasRampPointsFromParams() {
		if (rampParams.contains("alphas")) {
			try {
				alphas = getFixedPointList(rampParams.getList("alphas"));
			}
			catch (Exception e) {
				logger.error("Problem initializing alphas ramp points", e);
			}
		}
	}
	
	
	public int getRGB(double scale) {
		return smoothBetweenFixedPoints(reds, greens, blues, alphas,
				(this.isInverted ? 1-scale : scale));
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
		if (scale<start.scale) return start.value;
		for (int i=1; i<values.size(); i++) {
			FixedPoint pt = values.get(i);
			if (scale<pt.scale) {
				return ((scale-start.scale)*pt.value + (pt.scale-scale)*start.value)/(pt.scale-start.scale);
			}
			start = pt;
		}
		return start.value;
	}
	
	public static int smoothBetweenFixedPoints(List<FixedPoint> reds, List<FixedPoint> greens,
			List<FixedPoint> blues, List<FixedPoint> alphas, double scale) {
		int r = (int)(valueFromFixedPoints(reds,scale) * 255);
		int b = (int)(valueFromFixedPoints(blues,scale) * 255);
		int g = 0;
		int a = (int)(valueFromFixedPoints(alphas, scale) * 255);
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
