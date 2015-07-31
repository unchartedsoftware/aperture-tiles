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
package com.oculusinfo.tile.init.providers;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.factory.providers.StandardUberFactoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class StandardPyramidIOFactoryProvider extends StandardUberFactoryProvider<PyramidIO> {
	private static Logger LOGGER = LoggerFactory.getLogger(StandardPyramidIOFactoryProvider.class);
	private String _customProviders;

	static private Set<FactoryProvider<PyramidIO>> addCustomProviders (Set<FactoryProvider<PyramidIO>> baseProviders, String customProviders) {
		if (null == customProviders) {
			LOGGER.info("Construction Pyramid IO with only base factory providers");
			return baseProviders;
		} else {
			LOGGER.info("Construction Pyramid IO with custom factory providers: "+customProviders);
			Set<FactoryProvider<PyramidIO>> results = new HashSet(baseProviders);

			String[] names = customProviders.split(":");
			for (int n=0; n<names.length; ++n) {
				String name = names[n].trim();
				FactoryProvider<PyramidIO> provider = constructProvider(name);
				if (null != provider) {
					LOGGER.info("Adding custom pyramidIO factory provider "+name);
					results.add(provider);
				}
			}
			LOGGER.info("Got all custom factory providers.");

			return results;
		}
	}

	@SuppressWarnings("unchecked")
	static private FactoryProvider<PyramidIO> constructProvider (String providerClassName) {
		try {
			Class<?> providerClass = Class.forName(providerClassName);
			if (!FactoryProvider.class.isAssignableFrom(providerClass)) {
				throw new IllegalArgumentException("Not a factory provider");
			}
			FactoryProvider<?> provider = (FactoryProvider<?>) providerClass.getConstructor().newInstance();
			// We can't actually tell for sure the generic type of this factory provider; we just have to
			// trust it.
			return (FactoryProvider) provider;
		} catch (Exception e) {
			LOGGER.error("Can't get PyramidIO factory provider for type "+providerClassName, e);
			return null;
		}
	}

	@Inject
	public StandardPyramidIOFactoryProvider (Set<FactoryProvider<PyramidIO>> providers,
											 @Named("com.oculusinfo.pyramidio.custom.providers") String customProviders) {
		super(addCustomProviders(providers, customProviders));
	}

	@Override
	public ConfigurableFactory<PyramidIO> createFactory (String name,
	                                                     ConfigurableFactory<?> parent,
	                                                     List<String> path) {
		return new PyramidIOFactory(name, parent, path, createChildren(parent, path));
	}
}
