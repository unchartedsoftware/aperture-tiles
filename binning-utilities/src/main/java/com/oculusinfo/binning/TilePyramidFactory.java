/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
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
package com.oculusinfo.binning;



import java.util.List;

import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.DoubleProperty;
import com.oculusinfo.factory.StringProperty;



public class TilePyramidFactory extends ConfigurableFactory<TilePyramid> {
    private StringProperty PYRAMID_TYPE = new StringProperty("type",
                                                             "The type of tile pyramid to be created",
                                                             "web-mercator",
                                                             new String[] {"area-of-interest", "epsg:4326", "web-mercator", "epsg:900913", "epsg:3857"});
    private DoubleProperty MINIMUM_X = new DoubleProperty("minimum-x-value",
                                                          "The lower bound for the X axis in an area-of-interest tile pyramid",
                                                          0.0);
    private DoubleProperty MAXIMUM_X = new DoubleProperty("maximum-x-value",
                                                          "The upper bound for the X axis in an area-of-interest tile pyramid",
                                                          0.0);
    private DoubleProperty MINIMUM_Y = new DoubleProperty("minimum-y-value",
                                                          "The lower bound for the Y axis in an area-of-interest tile pyramid",
                                                          0.0);
    private DoubleProperty MAXIMUM_Y = new DoubleProperty("maximum-y-value",
                                                          "The upper bound for the Y axis in an area-of-interest tile pyramid",
                                                          0.0);



    public TilePyramidFactory (ConfigurableFactory<?> parent, List<String> path) {
        this(null, parent, path);
    }

    public TilePyramidFactory (String name, ConfigurableFactory<?> parent,
                               List<String> path) {
        super(name, TilePyramid.class, parent, path);

        addProperty(PYRAMID_TYPE);
        addProperty(MINIMUM_X);
        addProperty(MAXIMUM_X);
        addProperty(MINIMUM_Y);
        addProperty(MAXIMUM_Y);
    }

    @Override
    protected TilePyramid create () {
        String pyramidType = getPropertyValue(PYRAMID_TYPE);

        if ("web-mercator".equals(pyramidType) || "epsg:900913".equals(pyramidType) || "epsg:3857".equals(pyramidType)) {
            return new WebMercatorTilePyramid();
        } else if ("area-of-interest".equals(pyramidType) || "epsg:4326".equals(pyramidType)) {
            double minX = getPropertyValue(MINIMUM_X);
            double maxX = getPropertyValue(MAXIMUM_X);
            double minY = getPropertyValue(MINIMUM_Y);
            double maxY = getPropertyValue(MAXIMUM_Y);
            return new AOITilePyramid(minX, minY, maxX, maxY);
        } else {
            return null;
        }
    }

}
