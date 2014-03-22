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
package com.oculusinfo.tile.rest.tile.caching;

import java.util.List;

import org.apache.spark.api.java.JavaSparkContext;

import com.google.inject.Inject;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.properties.StringProperty;
import com.oculusinfo.tilegen.binning.LiveStaticTilePyramidIO;

public class LiveTilePyramidIOFactory extends PyramidIOFactory {
    public static StringProperty PYRAMID_IO_TYPE =
            PyramidIOFactory.PYRAMID_IO_TYPE.overridePossibleValues(StringProperty.addToArray(PyramidIOFactory.PYRAMID_IO_TYPE.getPossibleValues(), "live"));

    @Inject
    private JavaSparkContext _context;

    public LiveTilePyramidIOFactory (ConfigurableFactory<?> parent, List<String> path, JavaSparkContext context) {
        this(null, parent, path, context);
    }
    public LiveTilePyramidIOFactory (String name, ConfigurableFactory<?> parent, List<String> path, JavaSparkContext context) {
        super(name, parent, path);
        _context = context;
    }

    @Override
    protected List<ConfigurationProperty<?>> getPyramidIOPropertyList () {
        List<ConfigurationProperty<?>> result = super.getPyramidIOPropertyList();
        // Replace the type property with our own
        int n = result.indexOf(PyramidIOFactory.PYRAMID_IO_TYPE);
        result.set(n, PYRAMID_IO_TYPE);
        return result;
    }

    @Override
    protected PyramidIO create () {
        String pyramidIOType = getPropertyValue(PYRAMID_IO_TYPE);

        if ("live".equals(pyramidIOType)) {
            return new LiveStaticTilePyramidIO(JavaSparkContext.toSparkContext(_context));
        } else {
            return super.create();
        }
    }
}
