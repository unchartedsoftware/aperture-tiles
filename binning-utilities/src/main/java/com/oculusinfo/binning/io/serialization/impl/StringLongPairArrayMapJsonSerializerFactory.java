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

import java.util.List;
import java.util.Map;

import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.factory.ConfigurableFactory;

public class StringLongPairArrayMapJsonSerializerFactory extends ConfigurableFactory<TileSerializer<Map<String, List<Pair<String, Long>>>>> {
	// This is the only way to get a generified class object, but because of erasure, it's guaranteed to work.
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Class<TileSerializer<Map<String, List<Pair<String, Long>>>>> getGenericSerializerClass () {
		return (Class) TileSerializer.class;
	}

	public StringLongPairArrayMapJsonSerializerFactory (ConfigurableFactory<?> parent, List<String> path) {
		super("string->[(string, long)]-j", getGenericSerializerClass(), parent, path);
	}

	@Override
	protected TileSerializer<Map<String, List<Pair<String, Long>>>> create () {
		return new StringLongPairArrayMapJsonSerializer();
	}
}
