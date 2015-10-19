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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.oculusinfo.tile.rendering.color.impl.FlatColorRamp;
import com.oculusinfo.tile.rendering.color.impl.HueColorRamp;
import com.oculusinfo.tile.rendering.color.impl.SingleGradientColorRamp;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.DoubleProperty;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.properties.JSONArrayProperty;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.tile.rendering.color.impl.BRColorRamp;
import com.oculusinfo.tile.rendering.color.impl.GreyColorRamp;
import com.oculusinfo.tile.rendering.color.impl.SteppedGradientColorRamp;
import com.oculusinfo.tile.rendering.color.impl.WareColorRamp;

public class ColorRampFactory extends ConfigurableFactory<ColorRamp> {

	public static final StringProperty RAMP_TYPE = new StringProperty("ramp", "The desired type of color ramp", "spectral");
	public static final DoubleProperty OPACITY = new DoubleProperty("opacity", "The opacity with which a layer is displayed.", 1.0);
	public static final StringProperty COLOR1 = new StringProperty("from", "A standard HTML description of the primary color for this ramp.  Used by flat and single-gradient ramp types.", "0xffffff");
	public static final IntegerProperty ALPHA1 = new IntegerProperty("from-alpha", "The opacity (0-255) of the primary color for this ramp.  Used by single-gradient ramps only.  -1 to use the base opacity.", -1);
	public static final StringProperty COLOR2 = new StringProperty("to", "A standard HTML description of the secondary color for this ramp.  Used by single-gradient ramps only.", "0x000000");
	public static final IntegerProperty ALPHA2 = new IntegerProperty("to-alpha", "The opacity (0-255) of the secondary color for this ramp.  Used by single-gradient ramps only.  -1 to use the base opacity.", -1);
	public static final DoubleProperty HUE1  = new DoubleProperty("from", "The initial hue of a hue ramp (from 0.0 to 1.0).  Used only for hue ramps.", 0.0);
	public static final DoubleProperty HUE2 = new DoubleProperty("to", "The final hue of a hue ramp (from 0.0 to 1.0).  Used only for hue ramps.", 1.0);
	public static final StringProperty THEME = new StringProperty("theme", "The active theme.", "dark");
	public static final JSONArrayProperty GRADIENTS = new JSONArrayProperty("gradients", "A set of themed gradient definitions.", "[]");

	
	private List<ThemedGradientFactory> gradients = new ArrayList<>();

	
	public ColorRampFactory (ConfigurableFactory<?> parent, List<String> path) {
		super(ColorRamp.class, parent, path);

		addProperty(RAMP_TYPE);
		addProperty(OPACITY);
		addProperty(COLOR1);
		addProperty(ALPHA1);
		addProperty(COLOR2);
		addProperty(ALPHA2);
		addProperty(HUE1);
		addProperty(HUE2);
		addProperty(GRADIENTS);
        addProperty(THEME);
	}

	@Override
	public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
	    super.readConfiguration(rootNode);

	    gradients.clear();
		
		try {
			readGradients(getPropertyValue(GRADIENTS), gradients);
		} catch (JSONException e) {
			throw new ConfigurationException("Error configuring factory "+ this.getClass().getName(), e);
		}
	}
	
	private void readGradients(JSONArray gradients, List<ThemedGradientFactory> into) throws JSONException, ConfigurationException {
		for (int i=0; i<gradients.length(); i++) {
			final JSONObject node = gradients.getJSONObject(i);
			final List<String> nopath = Collections.emptyList();
			final ThemedGradientFactory factory = new ThemedGradientFactory(null, nopath);
			
			factory.readConfiguration(node);
			into.add(factory);
		}	 
	}

	@Override
	protected ColorRamp create () throws ConfigurationException {
		final String rampType = getPropertyValue(RAMP_TYPE);
		final double opacity = 1.0; //getPropertyValue(OPACITY);
		final String theme = getPropertyValue(THEME);
		final boolean islight = theme.equalsIgnoreCase("light");
		
		ColorRamp ramp;

		// ANY CUSTOM GRADIENT
		if (!gradients.isEmpty()) {
			for (ThemedGradientFactory factory : gradients) {
				final String scope[] = factory.getTheme().split(":");
				switch (scope.length) {
				case 2:
					if (!scope[0].equalsIgnoreCase(rampType)) continue;
				case 1:
					if (!scope[scope.length-1].equalsIgnoreCase(theme)) continue;
				case 0:
					break;
					
				default:
					continue;
				}
				
				return SteppedGradientColorRamp.from(factory.create().getColors());
			}
		}
		
		// CONSTRAINED HUE DEFAULTS
		if (rampType.equalsIgnoreCase("hot")){
			ramp = SteppedGradientColorRamp.hot(islight);
		} else if (rampType.equalsIgnoreCase("cool")){
			ramp = SteppedGradientColorRamp.cool(islight);
		} else if (rampType.equalsIgnoreCase("polar")){
			ramp = SteppedGradientColorRamp.polar(islight);
		} else if (rampType.equalsIgnoreCase("valence")){
			ramp = SteppedGradientColorRamp.valence(islight);
			
		// LEGACY BLUE / RED
		} else if (rampType.equalsIgnoreCase("br")){
			ramp = new BRColorRamp(islight, opacity);
		} else if(rampType.equalsIgnoreCase("inv-br")){
			// We're forcing the inverse.
			ramp = new BRColorRamp(true, opacity);

		// NEUTRAL GRAY
		} else if (rampType.equalsIgnoreCase("neutral") || rampType.equalsIgnoreCase("grey")) {
			ramp = new GreyColorRamp(islight, opacity);
		} else if (rampType.equalsIgnoreCase("inv-grey")) { // legacy
			// We're forcing the inverse.
			ramp = new GreyColorRamp(true, opacity);
			
		// FLAT
		} else if (rampType.equalsIgnoreCase("flat")) {
			Color color = hasPropertyValue(COLOR1)?
					ColorRampParameter.getColor(getPropertyValue(COLOR1)) :
						islight? new Color(55,55,55) : Color.WHITE;

			ramp = new FlatColorRamp(color, opacity);

		// LEGACY GRADIENT
		} else if (rampType.equalsIgnoreCase("single-gradient")) {
			int alpha1 = getPropertyValue(ALPHA1);
			if (-1 == alpha1) alpha1 = (int) Math.floor(255*opacity);
			Color startColor = ColorRampParameter.getColorWithAlpha(getPropertyValue(COLOR1), alpha1);
			int alpha2 = getPropertyValue(ALPHA2);
			if (-1 == alpha2) alpha2 = (int) Math.floor(255*opacity);
			Color endColor = ColorRampParameter.getColorWithAlpha(getPropertyValue(COLOR2), alpha2);
			ramp = new SingleGradientColorRamp(startColor, endColor);
			
		// LEGACY HUE
		} else if (rampType.equalsIgnoreCase("hue")) {
			ramp = new HueColorRamp(getPropertyValue(HUE1), getPropertyValue(HUE2));
			
		// SPECTRAL
		} else {

			// default
			ramp = new WareColorRamp(islight, opacity);
		}
		
		return ramp;
	}
	
}
