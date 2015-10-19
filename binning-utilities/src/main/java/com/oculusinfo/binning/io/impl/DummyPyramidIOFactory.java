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
package com.oculusinfo.binning.io.impl;

import java.util.List;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.properties.DoubleProperty;
import com.oculusinfo.factory.properties.IntegerProperty;

public class DummyPyramidIOFactory extends ConfigurableFactory<PyramidIO> {
	public static final DoubleProperty  MIN_X = new DoubleProperty ("minX", "The minimum x value in the space the dummy pyramid IO represents", 0.0);
	public static final DoubleProperty  MAX_X = new DoubleProperty ("maxX", "The maximum x value in the space the dummy pyramid IO represents", 1.0);
	public static final DoubleProperty  MIN_Y = new DoubleProperty ("minY", "The minimum y value in the space the dummy pyramid IO represents", 0.0);
	public static final DoubleProperty  MAX_Y = new DoubleProperty ("maxY", "The maximum y value in the space the dummy pyramid IO represents", 1.0);
	public static final IntegerProperty MIN_Z = new IntegerProperty("minZ", "The minimum zoom level in the space the dummy pyramid IO represents", 0);
	public static final IntegerProperty MAX_Z = new IntegerProperty("maxZ", "The maximum zoom level in the space the dummy pyramid IO represents", 18);



	public DummyPyramidIOFactory (ConfigurableFactory<?> parent, List<String> path) {
		super("dummy", PyramidIO.class, parent, path);

		addProperty(MIN_X);
		addProperty(MAX_X);
		addProperty(MIN_Y);
		addProperty(MAX_Y);
		addProperty(MIN_Z);
		addProperty(MAX_Z);
	}

	@Override
	protected PyramidIO create() throws ConfigurationException {
		double minX = getPropertyValue(MIN_X);
		double maxX = getPropertyValue(MAX_X);
		double minY = getPropertyValue(MIN_Y);
		double maxY = getPropertyValue(MAX_Y);
		int    minZ = getPropertyValue(MIN_Z);
		int    maxZ = getPropertyValue(MAX_Z);
		return new DummyPyramidIO(minX, maxX, minY, maxY, minZ, maxZ);
	}
}
