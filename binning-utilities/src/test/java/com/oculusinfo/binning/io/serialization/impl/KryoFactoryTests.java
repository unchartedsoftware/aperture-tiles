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
package com.oculusinfo.binning.io.serialization.impl;

import java.util.Arrays;

import org.json.JSONObject;
import org.junit.Test;

import com.oculusinfo.binning.io.serialization.SerializationTypeChecker;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer.Codec;
import com.oculusinfo.binning.util.TypeDescriptor;

public class KryoFactoryTests extends SerializerTestUtils {
	@Test
	public void testKryoDefaultCodecSpecification () throws Exception {
		TypeDescriptor integerType = new TypeDescriptor(Integer.class);
		KryoSerializerFactory<Integer> factory = new KryoSerializerFactory<Integer>(null, Arrays.asList("factory"), integerType);
		factory.readConfiguration(new JSONObject("{}"));
		TileSerializer<Integer> product = SerializationTypeChecker.checkBinClass(factory.produce(TileSerializer.class),
			   Integer.class, integerType);
		TileSerializer<Integer> expected = new KryoSerializer<Integer>(integerType, Codec.GZIP);
		assertSerializersEqual(expected, product, new IntegerSource());
	}

	@Test
	public void testKryoGZipSpecification () throws Exception {
		TypeDescriptor integerType = new TypeDescriptor(Integer.class);
		KryoSerializerFactory<Integer> factory = new KryoSerializerFactory<Integer>(null, Arrays.asList("factory"), integerType);
		factory.readConfiguration(new JSONObject("{\"factory\": { \"codec\": \"GZIP\"}}"));
		TileSerializer<Integer> product = SerializationTypeChecker.checkBinClass(factory.produce(TileSerializer.class),
			   Integer.class, integerType);
		TileSerializer<Integer> expected = new KryoSerializer<Integer>(integerType, Codec.GZIP);
		assertSerializersEqual(expected, product, new IntegerSource());
	}

	@Test
	public void testKryoBZip2Specification () throws Exception {
		TypeDescriptor integerType = new TypeDescriptor(Integer.class);
		KryoSerializerFactory<Integer> factory = new KryoSerializerFactory<Integer>(null, Arrays.asList("factory"), integerType);
		factory.readConfiguration(new JSONObject("{\"factory\": { \"codec\": \"BZIP\"}}"));
		TileSerializer<Integer> product = SerializationTypeChecker.checkBinClass(factory.produce(TileSerializer.class),
			   Integer.class, integerType);
		TileSerializer<Integer> expected = new KryoSerializer<Integer>(integerType, Codec.BZIP);
		assertSerializersEqual(expected, product, new IntegerSource());
	}

	@Test
	public void testKryoDeflateSpecification () throws Exception {
		TypeDescriptor integerType = new TypeDescriptor(Integer.class);
		KryoSerializerFactory<Integer> factory = new KryoSerializerFactory<Integer>(null, Arrays.asList("factory"), integerType);
		factory.readConfiguration(new JSONObject("{\"factory\": { \"codec\": \"DEFLATE\"}}"));
		TileSerializer<Integer> product = SerializationTypeChecker.checkBinClass(factory.produce(TileSerializer.class),
			   Integer.class, integerType);
		TileSerializer<Integer> expected = new KryoSerializer<Integer>(integerType, Codec.DEFLATE);
		assertSerializersEqual(expected, product, new IntegerSource());
	}

	@Test
	public void testKryoCustomClassSpecification () throws Exception {
		TypeDescriptor testType = new TypeDescriptor(TestClass.class);
		KryoSerializerFactory<TestClass> factory = new KryoSerializerFactory<TestClass>(null, Arrays.asList("factory"), testType);
		factory.readConfiguration(new JSONObject(
		                                         "{\"factory\": {\n"+
		                                         "  \"codec\": \"DEFLATE\",\n"+
		                                         "  \"classes\": [\""+TestClass.class.getName()+"\"]\n"+
		                                         "}}"));
		TileSerializer<TestClass> product = SerializationTypeChecker.checkBinClass(factory.produce(TileSerializer.class),
			     TestClass.class, testType);
		TileSerializer<TestClass> expected = new KryoSerializer<TestClass>(testType, Codec.DEFLATE, TestClass.class);
		assertSerializersEqual(expected, product, new TestClassSource());
	}

	public static class TestClassSource implements DataSource<TestClass> {
		@Override
		public void reset () {}

		@Override
		public TestClass create () {
			return new TestClass((int) Math.floor(Math.random()*1000),
			                     "abc"+(int) Math.floor(Math.random()*1000),
			                     Math.random()*1000);
		}
	}

	public static class TestClass {
		int    _i;
		String _s;
		double _d;
		TestClass () {
			_i = 0;
			_s = null;
			_d = 0.0;
		}
		TestClass (int i, String s, double d) {
			_i = i;
			_s = s;
			_d = d;
		}
		@Override
		public boolean equals (Object objThat) {
			if (this == objThat) return true;
			if (null == objThat) return false;
			if (!(objThat instanceof TestClass)) return false;
			TestClass that = (TestClass) objThat;
			return this._i == that._i && objectsEqual(this._s, that._s) && this._d == that._d;
		}
	}
}
