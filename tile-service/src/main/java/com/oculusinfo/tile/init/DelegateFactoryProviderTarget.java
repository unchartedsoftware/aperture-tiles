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
package com.oculusinfo.tile.init;

import java.util.List;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.tile.init.providers.StandardUberFactoryProvider;




/**
 * Provides construction of delegate factories of a particular type of object in
 * an xml-configurable manner. Delegate factories should always have names set
 * by default, and therefore don't need the named createFactory call from
 * {@link FactoryProvider}. Used in tandem with a
 * {@link StandardUberFactoryProvider}, this allows a {@link FactoryProvider} to
 * use guice and inject a set of these targets, so the associated factories can
 * be registered appropriately together in a general way.
 * 
 * @author cregnier
 */
public interface DelegateFactoryProviderTarget<T> {
	/**
	 * Create a new factory of the required type.
	 * 
	 * @param path The path to the factory's parameters from the root
	 *            configuration node.
	 * @return A new factory ready to initialize its parameters from the root
	 *         configuration node.
	 */
	public ConfigurableFactory<? extends T> createFactory (List<String> path);

	/**
	 * Create a new factory of the required type.
	 * 
	 * Passing in a null parent is the equivalent of calling
	 * {@link #createFactory(String[])}
	 * 
	 * @param parent The parent factory to which this factory will provide its
	 *            goods
	 * @param path The path from the parent factory's configuration node to this
	 *            one.
	 * @return A new factory ready to initialize its parameters from the parent
	 *         factory's configuration node.
	 */
	public ConfigurableFactory<? extends T> createFactory (ConfigurableFactory<?> parent,
	                                                       List<String> path);
}
