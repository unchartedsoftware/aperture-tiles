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

/**
 * Extends the {@link FactoryProvider} interface with an attached factory name, and
 * the config lookup path for the factory. This allows a {@link DelegateFactoryProvider}
 * to use guice and inject a set of these targets, so the associated factories can be
 * registered appropriately together in a general way. 
 * 
 * @author cregnier
 *
 */
public interface DelegateFactoryProviderTarget<T> extends FactoryProvider<T> {
	
	/**
	 * Returns the factory name to be associated with the factory provider.
	 * @return Returns the associated {@link String} name, or null if not set. 
	 */
	public String getFactoryName();
	
	/**
	 * Returns a relative path that can be used when creating a factory.
	 * @return Returns the associated {@link List}, or null if not set.
	 */
	public List<String> getPath();
	
}
