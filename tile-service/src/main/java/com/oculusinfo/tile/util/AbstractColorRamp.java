/**
 * Copyright (c) 2013 Oculus Info Inc.
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

public abstract class AbstractColorRamp implements ColorRamp {

	class FixedPoint {
		double scale;
		double value;
		public FixedPoint(double scale, double value) {
			this.scale = scale;
			this.value = value;
		}
	}
    protected ArrayList<FixedPoint> reds = new ArrayList<FixedPoint>();
    protected ArrayList<FixedPoint> blues = new ArrayList<FixedPoint>();
    protected ArrayList<FixedPoint> greens = new ArrayList<FixedPoint>();
    protected int alpha = 255;
    private boolean isInverted = false;

	
	public AbstractColorRamp(int alpha, boolean invert){
		this.alpha = alpha;
		this.isInverted = invert;
		initRampPoints();
	}

	public abstract void initRampPoints();
	
	public int getRGB(double scale) {
		return smoothWareFixedPoints(reds, blues, greens, 
				(this.isInverted ? 1-scale : scale), alpha);
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
	
	public static double valueFromFixedPoints(ArrayList<FixedPoint> values, double scale) {
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
	
	public static int smoothWareFixedPoints(ArrayList<FixedPoint> reds, ArrayList<FixedPoint> blues, 
			ArrayList<FixedPoint> greens, double scale, int alpha) {
		double r = valueFromFixedPoints(reds,scale);
		double b = valueFromFixedPoints(blues,scale);
		double g = 0;
		if(greens.size() > 0){
			g = valueFromFixedPoints(greens,scale);
		} else {
			g = getGreenForRBL(r, b , 0.1+(scale*0.9));
		}
		r = (int)Math.max(0, Math.min(0xFF,r*0xFF));
		g = (int)Math.max(0, Math.min(0xFF,g*0xFF));
		b = (int)Math.max(0, Math.min(0xFF,b*0xFF));
		return (int)(0x1000000*alpha + 0x10000*r + 0x100*g + b);
	}
  
}	
