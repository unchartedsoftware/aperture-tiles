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


public class WareColorRamp extends AbstractColorRamp {

	public WareColorRamp(ColorRampParameter params) {
		super(params);
	}
	
	@Override
	public void initRampPoints() {
        reds.add(new FixedPoint(0, 0.25));
        reds.add(new FixedPoint(0.25, 0.9));
        reds.add(new FixedPoint(0.5, 0.1));
        reds.add(new FixedPoint(0.75, 1));
        blues.add(new FixedPoint(0, 0.25));
        blues.add(new FixedPoint(0.25, 0));
        blues.add(new FixedPoint(0.85, 0));
        blues.add(new FixedPoint(1, 1));
        
        //try to initialize the ramp points from the params in case there's overrides
        initRedRampPointsFromParams();
        initBlueRampPointsFromParams();
        initGreenRampPointsFromParams();
        initAlphasRampPointsFromParams();
	}
	
}