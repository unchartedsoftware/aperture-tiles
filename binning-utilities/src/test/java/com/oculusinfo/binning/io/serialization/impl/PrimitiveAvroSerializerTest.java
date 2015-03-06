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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;

// Test that primitive avro serialization works
public class PrimitiveAvroSerializerTest {
	@SafeVarargs
	final <T> void testRoundTrip(Class<? extends T> type, T... data) throws Exception {
		TileSerializer<T> serializer = new PrimitiveAvroSerializer<T>(type, CodecFactory.nullCodec());

		// Create our tile
		int size = (int) Math.ceil(Math.sqrt(data.length));
		TileData<T> input = new DenseTileData<>(new TileIndex(0, 0, 0, size, size));
		for (int y=0; y<size; ++y) {
			for (int x=0; x<size; ++x) {
				int i = (x+size*y) % data.length;
				input.setBin(x, y, data[i]);
			}
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
			}
		}
	}


	@Test
	public void testBoolean () throws Exception {
		testRoundTrip(Boolean.class, true, false, true, true, false, true, false, false, true);
	}

	@Test
	public void testInteger () throws Exception {
		testRoundTrip(Integer.class, 0, 1, 4, 9, 16, 25, 36, 49, 64);
	}

	@Test
	public void testLong () throws Exception {
		testRoundTrip(Long.class, 0L, 1L, 8L, 27L, 64L, 125L, 216L, 343L, 512L);
	}

	@Test
	public void testFloat () throws Exception {
		testRoundTrip(Float.class, 0.0f, 0.5f, 0.333f, 0.25f, 0.2f, 0.166f, 0.142857f, 0.125f);
	}

	@Test
	public void testDouble () throws Exception {
		testRoundTrip(Double.class, 0.0, 1.1, 2.4, 3.9, 4.16, 5.25, 6.36, 7.49, 8.64);
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
		testRoundTrip(Short.class, (short)0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testByte () throws Exception {
		testRoundTrip(Byte.class, (byte)0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testReferenceType () throws Exception {
		List<Integer> sample = new ArrayList<>();
		testRoundTrip(sample.getClass(), sample);
	}

	// Test that a single serializer can be used to serialize both dense and sparse data.
	@Test
	public void testMultiDensitySerialization ()  throws IOException {
		TileIndex index = new TileIndex(0, 0, 0);
		TileData<Double> dense = new DenseTileData<>(index, 0.0);
		TileData<Double> sparse = new SparseTileData<>(index, 0.0);

		for (int x=0; x<256; ++x) {
			for (int y=0; y<256; ++y) {
				if (0 == ((x+y)%2)) {
					double value = Math.random();
					dense.setBin(x, y, value);
					sparse.setBin(x, y, value);
				}
			}
		}

		TileSerializer<Double> serializer = new PrimitiveAvroSerializer<Double>(Double.class, CodecFactory.nullCodec());

		ByteArrayOutputStream baosDense = new ByteArrayOutputStream();
		serializer.serialize(dense, baosDense);
		baosDense.flush();
		baosDense.close();

		ByteArrayOutputStream baosSparse = new ByteArrayOutputStream();
		serializer.serialize(sparse, baosSparse);
		baosSparse.flush();
		baosSparse.close();

		int denseSize = baosDense.toByteArray().length;
		int sparseSize = baosSparse.toByteArray().length;

		// Should be roughly equal in size - there is no compression, and the tile is half full.
		// But within an order of magnitude is close enough.
		Assert.assertTrue(denseSize < sparseSize * 10);
		Assert.assertTrue(sparseSize < denseSize * 10);
	}
}
