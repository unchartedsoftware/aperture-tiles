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

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.BooleanProperty;
import com.oculusinfo.factory.properties.DoubleProperty;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.tile.rendering.color.impl.*;
import org.json.JSONObject;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;

public class ColorRampFactory extends ConfigurableFactory<ColorRamp> {
	public static final StringProperty           RAMP_TYPE = new StringProperty("ramp",
		      "The desired type of color ramp",
		      "ware",
		      new String[] {"br", "ware", "inv-ware", "grey", "inv-grey", "flat", "single-gradient", "hue"});
	public static final DoubleProperty           OPACITY   = new DoubleProperty("opacity", "The opacity with which a layer is displayed.", 1.0);
	public static final BooleanProperty          INVERTED  = new BooleanProperty("inverted", "Whether this scale is inverted from its normal direction or not", false);
	public static final StringProperty           COLOR1     = new StringProperty("from", "A standard HTML description of the primary color for this ramp.  Used by flat and single-gradient ramp types.", "0xffffff");
	public static final IntegerProperty          ALPHA1     = new IntegerProperty("from-alpha", "The opacity (0-255) of the primary color for this ramp.  Used by single-gradient ramps only.  -1 to use the base opacity.", -1);
	public static final StringProperty           COLOR2     = new StringProperty("to", "A standard HTML description of the secondary color for this ramp.  Used by single-gradient ramps only.", "0x000000");
	public static final IntegerProperty          ALPHA2     = new IntegerProperty("to-alpha", "The opacity (0-255) of the secondary color for this ramp.  Used by single-gradient ramps only.  -1 to use the base opacity.", -1);
	public static final DoubleProperty           HUE1       = new DoubleProperty("from", "The initial hue of a hue ramp (from 0.0 to 1.0).  Used only for hue ramps.", 0.0);
	public static final DoubleProperty           HUE2       = new DoubleProperty("to", "The final hue of a hue ramp (from 0.0 to 1.0).  Used only for hue ramps.", 1.0);

	public ColorRampFactory (ConfigurableFactory<?> parent, List<String> path) {
		super(ColorRamp.class, parent, path);

		addProperty(RAMP_TYPE);
		addProperty(INVERTED);
		addProperty(OPACITY);
		addProperty(COLOR1);
		addProperty(ALPHA1);
		addProperty(COLOR2);
		addProperty(ALPHA2);
		addProperty(HUE1);
		addProperty(HUE2);
	}

	@Override
	public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
	    // TODO Auto-generated method stub
	    super.readConfiguration(rootNode);
	}

	@Override
	protected ColorRamp create () {
		String rampType = getPropertyValue(RAMP_TYPE);
		boolean inverted = getPropertyValue(INVERTED);
		double opacity = 1.0; //getPropertyValue(OPACITY);
        
		ColorRamp ramp;
		if (rampType.equalsIgnoreCase("br")){
			ramp = new BRColorRamp(inverted, opacity);
		} else if (rampType.equalsIgnoreCase("ware")) {
			ramp = new WareColorRamp(inverted, opacity);
		} else if(rampType.equalsIgnoreCase("inv-br")){
			// We're forcing the inverse.
			ramp = new BRColorRamp(true, opacity);
		} else if (rampType.equalsIgnoreCase("inv-ware")) {
			// We're forcing the inverse.
			ramp = new WareColorRamp(true, opacity);
		} else if (rampType.equalsIgnoreCase("grey")) {
			ramp = new GreyColorRamp(inverted, opacity);
		} else if (rampType.equalsIgnoreCase("inv-grey")) {
			// We're forcing the inverse.
			ramp = new GreyColorRamp(true, opacity);
		} else if (rampType.equalsIgnoreCase("flat")) {
			Color color = getColor(getPropertyValue(COLOR1));
			ramp = new FlatColorRamp(color.getRGB(), opacity);
		} else if (rampType.equalsIgnoreCase("single-gradient")) {
			int alpha1 = getPropertyValue(ALPHA1);
			if (-1 == alpha1) alpha1 = (int) Math.floor(255*opacity);
			Color startColor = getColorWithAlpha(getPropertyValue(COLOR1), alpha1);
			int alpha2 = getPropertyValue(ALPHA2);
			if (-1 == alpha2) alpha2 = (int) Math.floor(255*opacity);
			Color endColor = getColorWithAlpha(getPropertyValue(COLOR2), alpha2);
			ramp = new SingleGradientColorRamp(startColor, endColor);
		} else if (rampType.equalsIgnoreCase("hue")) {
			ramp = new HueColorRamp(getPropertyValue(HUE1), getPropertyValue(HUE2));
		} else {
			ramp = new WareColorRamp(inverted, opacity);
		}
		return ramp;
	}

	private Color getColor (String color) {
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
				c = Color.white;
			}
		}
		return c;
	}
	private Color getColorWithAlpha (String color, int alpha) {
		Color base = getColor(color);
		alpha = Math.min(Math.max(alpha, 0), 255);
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}
}
