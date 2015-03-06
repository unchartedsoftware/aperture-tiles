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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.util.Pair;

public class PairArrayAvroSerializerTests {
	@SafeVarargs
	final <S, T> void testRoundTrip(Class<? extends S> keyType, Class<? extends T> valueType, int arraySize,
	                                Pair<S, T>... data) throws Exception {
		TileSerializer<List<Pair<S, T>>> serializer = new PairArrayAvroSerializer<>(keyType, valueType, CodecFactory.nullCodec());

		// Create our tile
		int n = (int) Math.ceil(data.length/(double)arraySize);
		int size = (int) Math.ceil(Math.sqrt(n));
		TileData<List<Pair<S, T>>> input = new DenseTileData<>(new TileIndex(0, 0, 0, size, size));
		for (int y=0; y<size; ++y) {
			for (int x=0; x<size; ++x) {
				int i = ((x+size*y) % data.length)*arraySize;
				List<Pair<S, T>> list = new ArrayList<>(arraySize);
				for (int j=0; j<arraySize; ++j)
					if ((i+j) < data.length)
						list.add(data[i+j]);
				input.setBin(x, y, list);
			}
		}

		// Send it round-trip through serialization
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(input, baos);
		baos.flush();
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		TileData<List<Pair<S, T>>> output = serializer.deserialize(new TileIndex(1, 1, 1, size, size), bais);

		// Test to make sure output matches input.
		Assert.assertEquals(input.getDefinition(), output.getDefinition());
		for (int y=0; y<size; ++y) {
			for (int x=0; x<size; ++x) {
				assertListsEqual(input.getBin(x, y), output.getBin(x, y));
			}
		}
	}
	private <T> void assertListsEqual (List<T> a, List<T> b) {
		Assert.assertEquals(a.size(), b.size());
		for (int i=0; i<a.size(); ++i) {
			Assert.assertEquals(a.get(i), b.get(i));
		}
	}



	private <S, T> Pair<S, T> p (S s, T t) {
		return new Pair<>(s, t);
	}

	private ByteBuffer bb (int... bytes) {
		byte[] asBytes = new byte[bytes.length];
		for (int i=0; i<bytes.length; ++i) {
			asBytes[i] = (byte) bytes[i];
		}
		return ByteBuffer.wrap(asBytes);
	}



	@Test
	public void testIntInt() throws Exception {
		testRoundTrip(Integer.class, Integer.class, 3,
		              p(0, 0), p(0, 1), p(0, 2),
		              p(1, 0), p(1, 1), p(1, 2),
		              p(2, 0), p(2, 1), p(2, 2));
	}

	@Test
	public void testIntLong() throws Exception {
		testRoundTrip(Integer.class, Long.class, 3,
		              p(0, 0L), p(0, 1L), p(0, 2L),
		              p(1, 0L), p(1, 1L), p(1, 2L),
		              p(2, 0L), p(2, 1L), p(2, 2L));
	}

	@Test
	public void testIntFloat() throws Exception {
		testRoundTrip(Integer.class, Float.class, 3,
		              p(0, 0.0f), p(0, 1.0f), p(0, 2.0f),
		              p(1, 0.0f), p(1, 1.0f), p(1, 2.0f),
		              p(2, 0.0f), p(2, 1.0f), p(2, 2.0f));
	}

	@Test
	public void testIntDouble() throws Exception {
		testRoundTrip(Integer.class, Double.class, 3,
		              p(0, 0.0), p(0, 1.0), p(0, 2.0),
		              p(1, 0.0), p(1, 1.0), p(1, 2.0),
		              p(2, 0.0), p(2, 1.0), p(2, 2.0));
	}

	@Test
	public void testIntBoolean() throws Exception {
		testRoundTrip(Integer.class, Boolean.class, 2,
		              p(0, false), p(0, true),
		              p(1, true), p(1, false),
		              p(2, false), p(2, true));
	}

	@Test
	public void testIntByteBuffer() throws Exception {
		testRoundTrip(Integer.class, ByteBuffer.class, 3,
		              p(0, bb(0, 1, 2)), p(0, bb(126, 127, 128)), p(0, bb(253, 254, 255)),
		              p(1, bb(1, 2, 3)), p(1, bb(126, 127, 128)), p(1, bb(252, 253, 254)),
		              p(2, bb(2, 3, 4)), p(2, bb(126, 127, 128)), p(2, bb(251, 252, 253)));
	}

	@Test
	public void testIntString() throws Exception {
		testRoundTrip(Integer.class, String.class, 3,
		              p(0, "0"), p(0, "1"), p(0, "2"),
		              p(1, "0"), p(1, "1"), p(1, "2"),
		              p(2, "0"), p(2, "1"), p(2, "2"));
	}

	@Test
	public void testLongInt() throws Exception {
		testRoundTrip(Long.class, Integer.class, 3,
		              p(0L, 0), p(1L, 0), p(2L, 0),
		              p(0L, 1), p(1L, 1), p(2L, 1),
		              p(0L, 2), p(1L, 2), p(2L, 2));
	}

	@Test
	public void testFloatInt() throws Exception {
		testRoundTrip(Float.class, Integer.class, 3,
		              p(0.0f, 0), p(1.0f, 0), p(2.0f, 0),
		              p(0.0f, 1), p(1.0f, 1), p(2.0f, 1),
		              p(0.0f, 2), p(1.0f, 2), p(2.0f, 2));
	}

	@Test
	public void testDoubleInt() throws Exception {
		testRoundTrip(Double.class, Integer.class, 3,
		              p(0.0, 0), p(1.0, 0), p(2.0, 0),
		              p(0.0, 1), p(1.0, 1), p(2.0, 1),
		              p(0.0, 2), p(1.0, 2), p(2.0, 2));
	}

	@Test
	public void testBooleanInt() throws Exception {
		testRoundTrip(Boolean.class, Integer.class, 2,
		              p(false, 0), p(true, 0),
		              p(true, 1), p(false, 1),
		              p(false, 2), p(true, 2));
	}

	@Test
	public void testByteBufferInt() throws Exception {
		testRoundTrip(ByteBuffer.class, Integer.class, 3,
		              p(bb(0, 1, 2), 0), p(bb(126, 127, 128), 0), p(bb(253, 254, 255), 0),
		              p(bb(1, 2, 3), 1), p(bb(126, 127, 128), 1), p(bb(252, 253, 254), 1),
		              p(bb(2, 3, 4), 2), p(bb(126, 127, 128), 2), p(bb(251, 252, 253), 2));
	}

	@Test
	public void testStringInt() throws Exception {
		testRoundTrip(String.class, Integer.class, 3,
		              p("0", 0), p("1", 0), p("2", 0),
		              p("0", 1), p("1", 1), p("2", 1),
		              p("0", 2), p("1", 2), p("2", 2));
	}
}
