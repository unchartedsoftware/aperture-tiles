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
package com.oculusinfo.tile.rendering.transformations.value;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.properties.DoubleProperty;
import com.oculusinfo.factory.properties.StringProperty;

import java.util.List;

/**
 * A factory for creating {@link ValueTransformer} objects.
 * 
 * @author cregnier
 *
 */
public class ValueTransformerFactory extends ConfigurableFactory<ValueTransformer> {

	public static final StringProperty TRANSFORM_NAME = new StringProperty("type",
	    "The type of transformation to apply to the data.",
		"linear",
		new String[] {"linear", "log10", "minmax", "half-sigmoid", "sigmoid"});
	public static final DoubleProperty TRANSFORM_MAXIMUM = new DoubleProperty("max",
	    "The maximum value to allow in the input data, when using a minmax transformation",
	    Double.MAX_VALUE);
	public static final DoubleProperty TRANSFORM_MINIMUM = new DoubleProperty("min",
	    "The minimum value to allow in the input data, when using a minmax transformation",
	    Double.MIN_VALUE);
	public static final DoubleProperty LAYER_MAXIMUM = new DoubleProperty("layerMax",
        "For use by the server only",
		Double.MAX_VALUE);
	public static final DoubleProperty LAYER_MINIMUM = new DoubleProperty("layerMin",
		"For use by the server only",
		Double.MIN_VALUE);


	public ValueTransformerFactory (ConfigurableFactory<?> parent, List<String> path) {
		this(null, parent, path);
	}

	public ValueTransformerFactory (String name, ConfigurableFactory<?> parent, List<String> path) {
		super(name, ValueTransformer.class, parent, path);

		addProperty(TRANSFORM_NAME);
		addProperty(TRANSFORM_MAXIMUM);
		addProperty(TRANSFORM_MINIMUM);
		addProperty(LAYER_MAXIMUM);
		addProperty(LAYER_MINIMUM);
	}

	private double _layerMaximum;
	private double _layerMinimum;
	// Extrema are calculated properties; we must allow a way to set them.
	public void setExtrema (double min, double max) {
		_layerMaximum = max;
		_layerMinimum = min;
	}

	@Override
	public <PT> PT getPropertyValue (ConfigurationProperty<PT> property) {
		if (LAYER_MAXIMUM.equals(property)) {
			return property.getType().cast(_layerMaximum);
		} else if (LAYER_MINIMUM.equals(property)) {
			return property.getType().cast(_layerMinimum);
		}
		return super.getPropertyValue(property);
	}

	@Override
	protected ValueTransformer create () {
		String name = getPropertyValue(TRANSFORM_NAME);
		double layerMin = getPropertyValue(LAYER_MINIMUM);
		double layerMax = getPropertyValue(LAYER_MAXIMUM);

		if ("log10".equals(name)) {
			return new Log10ValueTransformer(layerMin, layerMax);
		} else if ("minmax".equals(name)) {
			double max;
			if (hasPropertyValue(TRANSFORM_MAXIMUM)) max = getPropertyValue(TRANSFORM_MAXIMUM);
			else max = layerMax;

			double min;
			if (hasPropertyValue(TRANSFORM_MINIMUM)) min = getPropertyValue(TRANSFORM_MINIMUM);
			else min = layerMin;

			return new LinearValueTransformer(min, max);
		} else if ("half-sigmoid".equals(name)) {
            double min = getPropertyValue(LAYER_MINIMUM);
		    return new HalfSigmoidValueTransformer(min, layerMax);
		} else if ("sigmoid".equals(name)) {
            double min = getPropertyValue(LAYER_MINIMUM);
            return new SigmoidValueTransformer(min, layerMax);
		} else {
			// Linear is default, even if passed an unknown type.
			double min = getPropertyValue(LAYER_MINIMUM);
			return new LinearValueTransformer(min, layerMax);
		}
	}

}
