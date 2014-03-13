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

/**
 * <p>
 * This package handles initialization and configuration of the tile server.
 * </p>
 * 
 * <p>
 * There are a few layers to the configuration code, that allow it to be
 * configured to custom tiling jobs:
 * <ul>
 * <li>
 * Guice modules are specified in the web.xml configuration file. Modules must
 * be defined that will provide the following FactoryProvider types:
 * <ul>
 * <li>FactoryProvider&lt;TilePyrmid&gt;</li>
 * <li>FactoryProvider&lt;TileSerializer&gt;</li>
 * <li>FactoryProvider&lt;PyramidIO&gt;</li>
 * <li>FactoryProvider&lt;TileDataImageRenderer&gt;</li>
 * </ul>
 * </li>
 * <li>
 * Each FactoryProvider is used to create ConfigurableFactorys for its object
 * type - a new one is created for each layer.</li>
 * <li>Each ConfigurableFactory reads in the configuration for each layer, and
 * creates the appropriate implementation of that interface to read that layer.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * To create a custom tiling stack, it is suggested that one works in reverse.
 * <ol>
 * <li>Create the custom end component - TileSerializer, TileDataImageRenderer,
 * or perhaps the other two.</li>
 * <li>Subclass the existing factory to extend it to be able to build your type.
 * </li>
 * <li>Subclass the existing factory provider similarly</li>
 * <li>Create a new module that provides/binds your factory provider to the
 * approriate interface</li>
 * <li>Reference your module instead of the standard module in your version of
 * web.xml</li>
 * </ol>
 * </p>
 * <p>
 * We are sorry there are so many levels of indirection here; unfortunately, it
 * is necessary to allow customization.
 * </p>
 */
package com.oculusinfo.tile.init;



