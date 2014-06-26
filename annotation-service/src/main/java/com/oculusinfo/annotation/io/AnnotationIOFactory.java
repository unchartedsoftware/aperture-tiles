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
package com.oculusinfo.annotation.io;

import com.google.common.collect.Lists;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.JSONProperty;
import com.oculusinfo.factory.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Factory class to create the standard types of AnnotationIOs
 *
 * @author nkronenfeld
 */
public class AnnotationIOFactory extends ConfigurableFactory<AnnotationIO> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationIOFactory.class);

    public static StringProperty PYRAMID_IO_TYPE        = new StringProperty("type",
            "The location to and from which to read tile annotations",
            null,
            null);
    public static JSONProperty INITIALIZATION_DATA    = new JSONProperty("data",
            "Data to be passed to the AnnotationIO for read initialization",
            null);

    private AnnotationIO _product;
    public AnnotationIOFactory (ConfigurableFactory<?> parent, List<String> path, List<ConfigurableFactory<?>> children) {
        this(null, parent, path, children);
    }

    public AnnotationIOFactory (String name, ConfigurableFactory<?> parent, List<String> path, List<ConfigurableFactory<?>> children) {
        super(name, AnnotationIO.class, parent, path);

        _product = null;
        List<String> annotationTypes = getPyramidTypes(children);

        //use the first factory name for the first child as the default type
        String defaultType = null;
        if (annotationTypes.size() > 0) {
            defaultType = annotationTypes.get(0);
        }

        //set up the PYRAMID_IO_TYPE property to use all the associated children factory names.
        PYRAMID_IO_TYPE = new StringProperty("type",
                "The location to and from which to read tile annotations",
                defaultType,
                annotationTypes.toArray(new String[0]));

        addProperty(PYRAMID_IO_TYPE);
        addProperty(INITIALIZATION_DATA);

        //add any child factories
        if (children != null) {
            for (ConfigurableFactory<?> factory : children) {
                addChildFactory(factory);
            }
        }

    }

    private static List<String> getPyramidTypes(List<ConfigurableFactory<?>> childFactories) {
        List<String> annotationTypes = Lists.newArrayListWithCapacity(childFactories.size());

        //add any child factories
        if (childFactories != null) {
            for (ConfigurableFactory<?> factory : childFactories) {
                String factoryName = factory.getName();
                if (factoryName != null) {
                    annotationTypes.add(factoryName);
                }
            }
        }

        return annotationTypes;
    }

    @Override
    protected AnnotationIO create () {
        if (null == _product) {
            synchronized (this) {
                if (null == _product) {
                    String annotationIOType = getPropertyValue(PYRAMID_IO_TYPE);

                    try {
                        _product = produce(annotationIOType, AnnotationIO.class);
                    } catch (Exception e) {
                        LOGGER.error("Error trying to create AnnotationIO", e);
                    }
                }
            }
        }
        return _product;
    }
}