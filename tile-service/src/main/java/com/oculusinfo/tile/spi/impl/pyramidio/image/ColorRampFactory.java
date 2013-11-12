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
package com.oculusinfo.tile.spi.impl.pyramidio.image;

import com.oculusinfo.tile.util.AbstractColorRamp;
import com.oculusinfo.tile.util.BRColorRamp;
import com.oculusinfo.tile.util.GreyColorRamp;
import com.oculusinfo.tile.util.WareColorRamp;

public class ColorRampFactory { // TODO: refactor into a more comprehensive/customizable feature.
	/**
	 * @param rampType
	 * @param opacity (0 = transparent, 255 = opaque)
	 * @return
	 */
	public static AbstractColorRamp create(String rampType, int opacity) {
		AbstractColorRamp ramp;
		String validRampType = rampType == null ? "ware" : rampType;
		
		if(validRampType.equalsIgnoreCase("br")){
			ramp = new BRColorRamp(opacity, false);
		}
		else if (validRampType.equalsIgnoreCase("ware")) {
			ramp = new WareColorRamp(opacity, false);
		}
		else if(validRampType != null && rampType.equalsIgnoreCase("inv-br")){
			ramp = new BRColorRamp(opacity, true);
		}
		else if (validRampType.equalsIgnoreCase("inv-ware")) {
			ramp = new WareColorRamp(opacity, true);
		}
		else if (validRampType.equalsIgnoreCase("grey")) {
			ramp = new GreyColorRamp(opacity, false);
		}
		else if (validRampType.equalsIgnoreCase("inv-grey")) {
			ramp = new GreyColorRamp(opacity, true);
		} else {
			ramp = new WareColorRamp(opacity, false);
		}
		return ramp;
	}
}