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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io.serialization.impl;



import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.ListProperty;
import com.oculusinfo.factory.properties.StringProperty;



/**
 * This serializer factory constructs a
 * {@link com.oculusinfo.binning.io.serialization.impl.KryoSerializer}
 */
public class KryoSerializerFactory<T> extends ConfigurableFactory<TileSerializer<T>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(KryoSerializerFactory.class);
	private static final ListProperty<String> NEEDED_CLASSES = new ListProperty<>(
		        new StringProperty("class",
		                           "A class that needs to be registered with Kryo for this serializer to work",
		                           null),
		        "classes",
		        "A list of fully-specified classes (as read by Class.forName()) that will need to be registered with Kryo for this serializer to work properly.");



	// This is the only way to get a generified class object, but because of erasure, it's guaranteed to work.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static <ST> Class<TileSerializer<ST>> getGenericSerializerClass () {
		return (Class) TileSerializer.class;
	}



	private TypeDescriptor _type;

	public KryoSerializerFactory (ConfigurableFactory<?> parent, List<String> path, TypeDescriptor type) {
		super("kryo", KryoSerializerFactory.<T>getGenericSerializerClass(), parent, path, true);
		_type = type;
	}

	@Override
	protected TileSerializer<T> create () {
		// Check class registrations
		List<String> classNames = getPropertyValue(NEEDED_CLASSES);
		if (!classNames.isEmpty()) {
			List<Class<?>> neededClasses = new ArrayList<>();
			for (String name: classNames) {
				try {
					neededClasses.add(Class.forName(name));
				} catch (ClassNotFoundException e) {
					LOGGER.warn("Class {} not found", name);
				}
			}
			KryoSerializer.registerClasses(neededClasses.toArray(new Class<?>[neededClasses.size()]));
		}
		return new KryoSerializer<>(_type);
	}
}