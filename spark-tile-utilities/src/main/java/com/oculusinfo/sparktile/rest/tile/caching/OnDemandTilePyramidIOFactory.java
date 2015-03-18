/**
 * Copyright (c) 2013 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.sparktile.rest.tile.caching;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.sparktile.spark.SparkContextProvider;
import com.oculusinfo.tilegen.binning.OnDemandAccumulatorPyramidIO;

public class OnDemandTilePyramidIOFactory extends ConfigurableFactory<PyramidIO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(OnDemandTilePyramidIOFactory.class);

	@Inject
	private SparkContextProvider _contextProvider;

    public OnDemandTilePyramidIOFactory (ConfigurableFactory<?> parent, List<String> path, SparkContextProvider contextProvider) {
        this("live", parent, path, contextProvider);
    }

    public OnDemandTilePyramidIOFactory (String name, ConfigurableFactory<?> parent, List<String> path, SparkContextProvider contextProvider) {
        super(name, PyramidIO.class, parent, path);
        _contextProvider = contextProvider;
        addProperty(PyramidIOFactory.INITIALIZATION_DATA);
    }

	@Override
	protected PyramidIO create () {
		try {
			JSONObject config = getPropertyValue(PyramidIOFactory.INITIALIZATION_DATA);
			return new OnDemandAccumulatorPyramidIO(_contextProvider.getSQLContext(config));
		}
		catch (Exception e) {
			LOGGER.error("Error trying to create FileBasedPyramidIO", e);
		}
		return null;
	}
}
