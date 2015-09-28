/**
 * Copyright (c) 2013-2014 Oculus Info Inc.
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
package com.oculusinfo.tile.rendering.color;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.JSONNode;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.properties.JSONArrayProperty;
import com.oculusinfo.factory.properties.StringProperty;

/**
 * @author djonker
 *
 */
public class ThemedGradientFactory extends ConfigurableFactory<ThemedGradient> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ColorRampFactory.class);

	/*
	 * Color properties
	 */
	public static final JSONArrayProperty COLORS = new JSONArrayProperty("colors", 
			"An array of hues for a theme in a hue ramp.", "[]");
	
	/*
	 * Alpha properties, currently specified separately from color.
	 */
	public static final JSONArrayProperty ALPHAS = new JSONArrayProperty("alpha", 
			"An array of alpha values in a hue ramp.", "[]");

	/*
	 * The theme this gradient belongs to. 
	 */
	public static final StringProperty THEME = new StringProperty("theme", "The theme that this gradient belongs to.", "dark");
	
	
	// used for reading only
	private static final StringProperty COLOR_READER = new StringProperty("", "", "0xffffff");
	private static final IntegerProperty ALPHA_READER = new IntegerProperty("", "", 255);
	static private final List<Color> DEFAULT_LIGHT = Arrays.asList(new Color[]{Color.WHITE, Color.BLACK});
	static private final List<Color> DEFAULT_DARK = Arrays.asList(new Color[]{Color.BLACK, Color.WHITE});

	
	/**
	 * Constructs a new factory.
	 */
	public ThemedGradientFactory (ConfigurableFactory<?> parent, List<String> path) {
		super(ThemedGradient.class, parent, path);

		addProperty(THEME);
		addProperty(COLORS);
		addProperty(ALPHAS);
	}

	
	@Override
	public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
		
		// read the predictable 
	    super.readConfiguration(rootNode);
	}
	
	/**
	 * Returns the theme name.
	 * @return the theme name
	 */
	public String getTheme() throws ConfigurationException {
		return getPropertyValue(THEME);
	}

	/* (non-Javadoc)
	 * @see com.oculusinfo.factory.ConfigurableFactory#create()
	 */
	@Override
	protected ThemedGradient create() throws ConfigurationException {

		final String theme = getTheme();
		final List<Color> colors = new ArrayList<Color>();
		final JSONArray jcolors = getPropertyValue(COLORS);
		final JSONArray jalphas = getPropertyValue(ALPHAS);
		
		final int n = Math.max(jcolors.length(), jalphas.length());
		
		for (int i=0; i< n; i++) {
			String color = COLOR_READER.getDefaultValue();
			int alpha = ALPHA_READER.getDefaultValue();
			
			if (i < jcolors.length()) {
				try {
					color = COLOR_READER.unencodeJSON(new JSONNode(jcolors, i));
				} catch (Exception e) {
					LOGGER.warn("Error reading one of the color gradient values. Using default...");
				}
			}
			if (i < jalphas.length()) {
				try {
					alpha = ALPHA_READER.unencodeJSON(new JSONNode(jalphas, i));
				} catch (Exception e) {
					LOGGER.warn("Error reading one of the color gradient values. Using default...");
				}
			}
			
			colors.add(ColorRampParameter.getColorWithAlpha(color, alpha));
		}
		
		if (colors.size() < 1) {
			LOGGER.warn("Color gradient configuration has too few values. Added black and red.");
			colors.add(new Color(0,0,0,255));
			colors.add(new Color(255,0,0,255));
		} else if (colors.size() < 2) {
			LOGGER.warn("Color gradient configuration has too few values. Added red.");
			colors.add(new Color(255,0,0,255));
		}
		
		return new ThemedGradient() {
			
			@Override
			public String getTheme() {
				return theme;
			}
			
			@Override
			public List<Color> getColors() {
				return colors;
			}
		};
	}

	/**
	 * Creates a default grayscale gradient.
	 * @param theme the theme name
	 * @return the default array of colors corresponding to a theme
	 */
	public static List<Color> createDefault(String theme) {
		return "light".equalsIgnoreCase(theme)? DEFAULT_LIGHT : DEFAULT_DARK;
	}
}

