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

package com.oculusinfo.tile.rendering;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.DoubleProperty;
import com.oculusinfo.factory.properties.ListProperty;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.tile.rendering.color.ColorRampFactory;
import com.oculusinfo.tile.rendering.impl.*;

import java.util.ArrayList;
import java.util.List;

public class ImageRendererFactory extends ConfigurableFactory<TileDataImageRenderer> {

    public static StringProperty RENDERER_TYPE = new StringProperty("type",
        "The type of renderer that will be used to render the data on the server",
        "heatmap",
        new String[] {"heatmap", "listheatmap", "toptextscores", "textscores", "doublestatistics", "metadata"});


	public ImageRendererFactory (ConfigurableFactory<?> parent,
	                             List<String> path) {
		this(null, parent, path);
	}

	public ImageRendererFactory (String name, ConfigurableFactory<?> parent,
	                             List<String> path) {
		super(name, TileDataImageRenderer.class, parent, path);

		addProperty(RENDERER_TYPE);

		addChildFactory(new ColorRampFactory(this, new ArrayList<String>()));
	}


	@Override
	protected TileDataImageRenderer create () {
		String rendererType = getPropertyValue(RENDERER_TYPE);

		rendererType = rendererType.toLowerCase();

		if ("heatmap".equals(rendererType)) {
            return new DoublesImageRenderer();
        } else if ("listheatmap".equals(rendererType)) {
            return new DoubleListHeatMapImageRenderer();
		} else if ("toptextscores".equals(rendererType)) {
			return new TopTextScoresImageRenderer();
		} else if ("textscores".equals(rendererType)) {
			return new TopAndBottomTextScoresImageRenderer();
		} else if ("doublestatistics".equals(rendererType)) {
			return new DoublesStatisticImageRenderer();
		} else {
			return null;
		}
	}
}
