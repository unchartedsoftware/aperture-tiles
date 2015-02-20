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
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;

// Test that primitive avro serialization works with sparse tile datas
public class SparseAvroSerializationTests {
	@SafeVarargs
	final <T> void testRoundTrip(Class<? extends T> type, T defaultBin, T... data) throws Exception {
		TileSerializer<T> serializer = new PrimitiveAvroSerializer<T>(type, CodecFactory.nullCodec());

		// Create our tile
		int size = data.length;
		TileData<T> input = new SparseTileData<>(new TileIndex(0, 0, 0, size, size), defaultBin);
		for (int i=0; i<size; ++i) {
			input.setBin(i, i, data[i]);
		}

		// Send it round-trip through serialization
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(input, baos);
		baos.flush();
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		TileData<T> output = serializer.deserialize(new TileIndex(1, 1, 1, size, size), bais);

		// Test to make sure output matches input.
		Assert.assertEquals(input.getDefinition(), output.getDefinition());
		for (int y=0; y<size; ++y) {
			for (int x=0; x<size; ++x) {
				Assert.assertEquals(input.getBin(x, y), output.getBin(x, y));
				if (x == y) Assert.assertEquals(output.getBin(x, y), data[x]);
				else Assert.assertEquals(output.getBin(x, y), defaultBin);
			}
		}
	}


	@Test
	public void testBoolean () throws Exception {
		testRoundTrip(Boolean.class, true, false, false, true, false, false);
	}

	@Test
	public void testInteger () throws Exception {
		testRoundTrip(Integer.class, -4, 0, 1, 4, 9, 16, 25, 36, 49, 64);
	}

	@Test
	public void testLong () throws Exception {
		testRoundTrip(Long.class, -4L, 0L, 1L, 8L, 27L, 64L, 125L, 216L, 343L, 512L);
	}

	@Test
	public void testFloat () throws Exception {
		testRoundTrip(Float.class, -4.4f, 0.0f, 0.5f, 0.333f, 0.25f, 0.2f, 0.166f, 0.142857f, 0.125f);
	}

	@Test
	public void testDouble () throws Exception {
		testRoundTrip(Double.class, -4.4, 0.0, 1.1, 2.4, 3.9, 4.16, 5.25, 6.36, 7.49, 8.64);
	}

	@Test
	public void testBytes () throws Exception {
		testRoundTrip(ByteBuffer.class,
		              ByteBuffer.wrap(new byte[] {}),
		              ByteBuffer.wrap(new byte[] {(byte) 1}),
		              ByteBuffer.wrap(new byte[] {(byte) 2, (byte) 4}),
		              ByteBuffer.wrap(new byte[] {(byte) 3, (byte) 9, (byte) 27}));
	}

	@Test
	public void testString () throws Exception {
		testRoundTrip(String.class, "a", "bb", "ccc", "dddd", "eeeee", "ffffff", "ggggggg", "hhhhhhhh");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testShort () throws Exception {
		testRoundTrip(Short.class, (short)0, (short)0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testByte () throws Exception {
		testRoundTrip(Byte.class, (byte)0, (byte)0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testReferenceType () throws Exception {
		List<Integer> sample = new ArrayList<>();
		testRoundTrip(sample.getClass(), sample, sample);
	}
}
