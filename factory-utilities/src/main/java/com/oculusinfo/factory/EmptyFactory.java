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
package com.oculusinfo.factory;

import java.util.List;



/**
 * A standardized implementation of a factory that produces nothing, but sinply
 * contains properties whose values can be retreived.
 */
public class EmptyFactory extends ConfigurableFactory<Object> {

    public EmptyFactory (ConfigurableFactory<?> parent, List<String> path, ConfigurationProperty<?>... properties) {
        super(Object.class, parent, path);
        for (ConfigurationProperty<?> property: properties) addProperty(property);
    }

    public EmptyFactory (String name, ConfigurableFactory<?> parent, List<String> path, ConfigurationProperty<?>... properties) {
        super(name, Object.class, parent, path);
        for (ConfigurationProperty<?> property: properties) addProperty(property);
    }

    @Override
    protected Object create () {
        // No-op
        return null;
    }

}
