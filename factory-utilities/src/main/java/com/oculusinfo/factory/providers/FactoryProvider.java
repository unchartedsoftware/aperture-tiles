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
package com.oculusinfo.factory.providers;


import com.oculusinfo.factory.ConfigurableFactory;

import java.util.List;



/**
 * Provides construction of factories of a particular type of object in an configurable manner
 * 
 * @author nkronenfeld
 */
public interface FactoryProvider<T> {
	/**
	 * Create a new factory of the required type.
	 * 
	 * @param path The path to the factory's parameters from the root configuration node.
	 * @return A new factory ready to initialize its parameters from the root configuration node.
	 */
	public ConfigurableFactory<? extends T> createFactory (List<String> path);

	/**
	 * Create a new factory of the required type.
	 * 
	 * Passing in a null parent is the equivalent of calling the no-parent version of this method.
	 * 
	 * @param parent The parent factory to which this factory will provide its goods
	 * @param path The path from the parent factory's configuration node to this one.
	 * @return A new factory ready to initialize its parameters from the parent factory's configuration node.
	 */
	public ConfigurableFactory<? extends T> createFactory (ConfigurableFactory<?> parent,
	                                                       List<String> path);

	/**
     * Create a new factory of the required type.
     * 
     * Passing in a null parent is the equivalent of calling the no-parent version of this method.
     * 
     * @param name The name to assign to the given factory
     * @param parent The parent factory to which this factory will provide its goods
     * @param path The path from the parent factory's configuration node to this one.
     * @return A new factory ready to initialize its parameters from the parent factory's configuration node.
	 */
	public ConfigurableFactory<? extends T> createFactory (String name,
	                                                       ConfigurableFactory<?> parent,
	                                                       List<String> path);
}
