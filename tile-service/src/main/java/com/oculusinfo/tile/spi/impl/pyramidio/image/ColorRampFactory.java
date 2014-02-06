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

import com.oculusinfo.tile.util.BRColorRamp;
import com.oculusinfo.tile.util.ColorRamp;
import com.oculusinfo.tile.util.ColorRampParameter;
import com.oculusinfo.tile.util.FlatColorRamp;
import com.oculusinfo.tile.util.HueColorRamp;
import com.oculusinfo.tile.util.SingleGradientColorRamp;
import com.oculusinfo.tile.util.GreyColorRamp;
import com.oculusinfo.tile.util.WareColorRamp;

public class ColorRampFactory { // TODO: refactor into a more comprehensive/customizable feature.
	
	/**
	 * @param rampType
	 * @param opacity (0 = transparent, 255 = opaque)
	 * @return
	 */
	public static ColorRamp create(ColorRampParameter rampType, int opacity) {
		ColorRamp ramp;
		String validRampType = rampType.getName() != null ? rampType.getName() : "ware";
		
		rampType.setInt("opacity", opacity);
		
		if(validRampType.equalsIgnoreCase("br")){
			ramp = new BRColorRamp(rampType);
		}
		else if (validRampType.equalsIgnoreCase("ware")) {
			ramp = new WareColorRamp(rampType);
		}
		else if(validRampType.equalsIgnoreCase("inv-br")){
			//we're forcing the inverse here so lets set it
			rampType.setString("inverse", "true");
			ramp = new BRColorRamp(rampType);
		}
		else if (validRampType.equalsIgnoreCase("inv-ware")) {
			//we're forcing the inverse here so lets set it
			rampType.setString("inverse", "true");
			ramp = new WareColorRamp(rampType);
		}
		else if (validRampType.equalsIgnoreCase("grey")) {
			ramp = new GreyColorRamp(rampType);
		}
		else if (validRampType.equalsIgnoreCase("inv-grey")) {
			//we're forcing the inverse here so lets set it
			rampType.setString("inverse", "true");
			ramp = new GreyColorRamp(rampType);
		}
		else if (validRampType.equalsIgnoreCase("flat")) {
			ramp = new FlatColorRamp(rampType);
		}
		else if (validRampType.equalsIgnoreCase("single-gradient")) {
			ramp = new SingleGradientColorRamp(rampType);
		}
		else if (validRampType.equalsIgnoreCase("hue")) {
			ramp = new HueColorRamp(rampType);
		}
		else {
			ramp = new WareColorRamp(rampType);
		}
		return ramp;
	}
}