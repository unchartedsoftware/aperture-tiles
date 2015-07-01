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
import java.util.Arrays;
import java.util.List;

import com.oculusinfo.binning.impl.DenseTileMultiSliceView;
import com.oculusinfo.binning.impl.SparseTileData;
import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;

public class PrimitiveArrayAvroSerializerTest {
	@SafeVarargs
	final <T> void testRoundTrip(Class<? extends T> type, int arraySize, T... data) throws Exception {
		TileSerializer<List<T>> serializer = new PrimitiveArrayAvroSerializer<T>(type, CodecFactory.nullCodec());

		// Create our tile
		int n = (int) Math.ceil(data.length/(double)arraySize);
		int size = (int) Math.ceil(Math.sqrt(n));
		TileData<List<T>> input = new DenseTileData<>(new TileIndex(0, 0, 0, size, size));
		for (int y=0; y<size; ++y) {
			for (int x=0; x<size; ++x) {
				int i = ((x+size*y) % data.length)*arraySize;
				List<T> list = new ArrayList<>(arraySize);
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
		TileData<List<T>> output = serializer.deserialize(new TileIndex(1, 1, 1, size, size), bais);

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

	@Test
	public void testBoolean () throws Exception {
		testRoundTrip(Boolean.class, 3,
		              true, false, true,
		              true, false, true,
		              false, false, true,
		              false, false, false);
	}

	@Test
	public void testInteger () throws Exception {
		testRoundTrip(Integer.class, 2,
			0, 0,
			1, 1,
			2, 4,
			3, 9);
	}

	@Test
	public void testLong () throws Exception {
		testRoundTrip(Long.class, 3,
		              0L, 1L, 8L,
		              27L, 64L, 125L,
		              216L, 343L, 512L,
		              729L, 1000L, 1331L);
	}

	@Test
	public void testFloat () throws Exception {
		testRoundTrip(Float.class, 1,
		              0.0f, 0.5f, 0.333f, 0.25f, 0.2f, 0.166f, 0.142857f, 0.125f);
	}

	@Test
	public void testDouble () throws Exception {
		testRoundTrip(Double.class, 1,
		              0.0, 1.1, 2.4, 3.9, 4.16, 5.25, 6.36, 7.49, 8.64);
	}

	@Test
	public void testBytes () throws Exception {
		testRoundTrip(ByteBuffer.class, 1,
		              ByteBuffer.wrap(new byte[] {}),
		              ByteBuffer.wrap(new byte[] {(byte) 1}),
		              ByteBuffer.wrap(new byte[] {(byte) 2, (byte) 4}),
		              ByteBuffer.wrap(new byte[] {(byte) 3, (byte) 9, (byte) 27}));
	}

	@Test
	public void testString () throws Exception {
		testRoundTrip(String.class, 4,
		              "a", "aa", "aaa", "aaaa",
		              "b", "bb", "bbb", "bbbb",
		              "c", "cc", "ccc", "cccc",
		              "d", "dd", "ddd", "dddd");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testShort () throws Exception {
		testRoundTrip(Short.class, 1, (short)0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testByte () throws Exception {
		testRoundTrip(Byte.class, 1, (byte)0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testReferenceType () throws Exception {
		List<Integer> sample = new ArrayList<>();
		testRoundTrip(sample.getClass(), 1, sample);
	}

	// Test reading and writing of dense tiles
	@Test
	public void testNullBinsInDenseTile () throws Exception {
		TileData<List<Integer>> inputTile = new DenseTileData<>(new TileIndex(0, 0, 0, 4, 4));
		inputTile.setBin(0, 0, Arrays.asList( 1,  2,  3,  4));
		inputTile.setBin(1, 1, Arrays.asList( 2,  4,  6,  8));
		inputTile.setBin(2, 2, Arrays.asList( 3,  6,  9, 12));
		inputTile.setBin(3, 3, Arrays.asList( 4,  8, 12, 16));
		TileSerializer<List<Integer>> serializer = new PrimitiveArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(inputTile, baos);
		baos.flush();
		baos.close();
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		TileData<List<Integer>> outputTile = serializer.deserialize(new TileIndex(0, 0, 0, 4, 4), bais);
		for (int x=0; x<4; ++x) {
			for (int y=0; y<4; ++y) {
				String binName = "Bin ["+x+", "+y+"]";
				List<Integer> input = inputTile.getBin(x, y);
				List<Integer> output = outputTile.getBin(x, y);
				if (null == input) {
					Assert.assertTrue(binName, null == output || output.size() == 0);
				} else {
					Assert.assertEquals(binName, input.size(), output.size());
					for (int z=0; z<input.size(); ++z) {
						Assert.assertEquals(input.get(z), output.get(z));
					}
				}
			}
		}
	}

	// Test reading and writing of slices of dense tiles
	@Test
	public void testNullBinsInDenseTileSlice () throws Exception {
		TileData<List<Integer>> inputTile = new DenseTileData<>(new TileIndex(0, 0, 0, 4, 4));
		inputTile.setBin(0, 0, Arrays.asList(1));
		inputTile.setBin(1, 1, Arrays.asList( 2,  4));
		inputTile.setBin(2, 2, Arrays.asList( 3,  6,  9));
		inputTile.setBin(3, 3, Arrays.asList( 4,  8, 12, 16));
		TileSerializer<List<Integer>> serializer = new PrimitiveArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());

		// Test a fifth slice too, even though it should be totally empty
		for (int i=0; i<5; ++i) {
			TileData<List<Integer>> inputSlice = new DenseTileMultiSliceView<Integer>(inputTile, i, i);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.serialize(inputSlice, baos);
			baos.flush();
			baos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TileData<List<Integer>> outputTile = serializer.deserialize(new TileIndex(0, 0, 0, 4, 4), bais);
			for (int x = 0; x < 4; ++x) {
				for (int y = 0; y < 4; ++y) {
					String binName = "Bin [" + x + ", " + y + "]";
					List<Integer> input = inputSlice.getBin(x, y);
					List<Integer> output = outputTile.getBin(x, y);
					if (null == input) {
						Assert.assertTrue(binName, null == output || output.size() == 0);
					} else {
						Assert.assertEquals(binName, input.size(), output.size());
						for (int z = 0; z < input.size(); ++z) {
							Assert.assertEquals(input.get(z), output.get(z));
						}
					}
				}
			}
		}
	}

	@Test
	public void testDenseDefaults () throws Exception {
		TileSerializer<List<Integer>> serializer = new PrimitiveArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());
		DenseTileData<List<Integer>> denseBase = new DenseTileData<List<Integer>>(new TileIndex(0, 0, 0, 2, 2), Arrays.asList(1, 2, 3));

		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.serialize(denseBase, baos);
			baos.flush();
			baos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TileData<List<Integer>> out = serializer.deserialize(denseBase.getDefinition(), bais);

			Assert.assertTrue(out instanceof DenseTileData<?>);

			List<Integer> baseDefault = denseBase.getDefaultValue();
			List<Integer> readDefault = ((DenseTileData<List<Integer>>) out).getDefaultValue();

			Assert.assertEquals(baseDefault.size(), readDefault.size());
			for (int i=0; i<baseDefault.size(); ++i) {
				Assert.assertEquals(baseDefault.get(i), readDefault.get(i));
			}
		}
	}

	@Test
	public void testSparseDefaults () throws Exception {
		TileSerializer<List<Integer>> serializer = new PrimitiveArrayAvroSerializer<>(Integer.class, CodecFactory.nullCodec());
		SparseTileData<List<Integer>> sparseBase = new SparseTileData<List<Integer>>(new TileIndex(0, 0, 0, 2, 2), Arrays.asList(4, 5, 6));

		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.serialize(sparseBase, baos);
			baos.flush();
			baos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TileData<List<Integer>> out = serializer.deserialize(sparseBase.getDefinition(), bais);

			Assert.assertTrue(out instanceof SparseTileData<?>);

			List<Integer> baseDefault = sparseBase.getDefaultValue();
			List<Integer> readDefault = ((SparseTileData<List<Integer>>) out).getDefaultValue();

			Assert.assertEquals(baseDefault.size(), readDefault.size());
			for (int i=0; i<baseDefault.size(); ++i) {
				Assert.assertEquals(baseDefault.get(i), readDefault.get(i));
			}
		}
	}
}
