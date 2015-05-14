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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.serialization.SerializationTypeChecker;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.ConfigurationException;

public class KryoSerializationTests extends SerializerTestUtils {
	private static final Class<?>[] EMPTY = new Class<?>[0];

	@Test
	public void testMetaDataSerialization () throws Exception {
		TileIndex index = new TileIndex(0, 0, 0, 2, 2);
		TileData<Double> tile = new DenseTileData<>(index);
		tile.setBin(0, 0, 1.0);
		tile.setBin(0, 1, 2.0);
		tile.setBin(1, 0, 3.0);
		tile.setBin(1, 1, 4.0);
		tile.setMetaData("a", "abc");
		tile.setMetaData("b", "bcd");

		TileSerializer<Double> serializer = new KryoSerializer<Double>(new TypeDescriptor(Double.class));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		serializer.serialize(tile, output);
		output.flush();
		output.close();

		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		TileData<Double> received = serializer.deserialize(index, input);

		Assert.assertEquals(2, received.getMetaDataProperties().size());
		Assert.assertTrue(received.getMetaDataProperties().contains("a"));
		Assert.assertTrue(received.getMetaDataProperties().contains("b"));
		Assert.assertEquals("abc", received.getMetaData("a"));
		Assert.assertEquals("bcd", received.getMetaData("b"));
	}


	@SafeVarargs
	final <T> void testRoundTripDense(TypeDescriptor type, Class<?>[] classesToRegister, T... data) throws Exception {
		TileSerializer<T> serializer = new KryoSerializer<T>(type, classesToRegister);

		// Create our tile
		int size = (int) Math.ceil(Math.sqrt(data.length));
		TileData<T> input = new DenseTileData<T>(new TileIndex(0, 0, 0, size, size));
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


	@SafeVarargs
	final <T> void testRoundTripSparse(TypeDescriptor type, Class<?>[] classesToRegister, T defaultValue, T... data) throws Exception {
		TileSerializer<T> serializer = new KryoSerializer<T>(type, classesToRegister);

		// Create our tile
		int size = (int) Math.ceil(Math.sqrt(data.length*2));
		TileData<T> input = new SparseTileData<T>(new TileIndex(0, 0, 0, size, size), defaultValue);
		int i = 0;
		for (int y=0; y<size; ++y) {
			for (int x=0; x<size; ++x) {
				if (0 == ((x+y)%2) && i < data.length) {
					input.setBin(x, y, data[i]);
					++i;
				}
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
		testRoundTripDense(new TypeDescriptor(Boolean.class), EMPTY, true, false, true, true, false, true, false, false, true);
		testRoundTripSparse(new TypeDescriptor(Boolean.class), EMPTY, true, true, false);
	}

	@Test
	public void testInteger () throws Exception {
		testRoundTripDense(new TypeDescriptor(Integer.class), EMPTY, 0, 1, 4, 9, 16, 25, 36, 49, 64);
		testRoundTripSparse(new TypeDescriptor(Integer.class), EMPTY, -1, 0, 1, 4, 9);
	}

	@Test
	public void testLong () throws Exception {
		testRoundTripDense(new TypeDescriptor(Long.class), EMPTY, 0L, 1L, 8L, 27L, 64L, 125L, 216L, 343L, 512L);
		testRoundTripSparse(new TypeDescriptor(Long.class), EMPTY, -3L, 1L, 8L, 27L, 64L);
	}

	@Test
	public void testFloat () throws Exception {
		testRoundTripDense(new TypeDescriptor(Float.class), EMPTY, 0.0f, 0.5f, 0.333f, 0.25f, 0.2f, 0.166f, 0.142857f, 0.125f);
		testRoundTripSparse(new TypeDescriptor(Float.class), EMPTY, -3.5f, 0.0f, 0.5f, 0.333f, 0.25f);
	}

	@Test
	public void testDouble () throws Exception {
		testRoundTripDense(new TypeDescriptor(Double.class), EMPTY, 0.0, 1.1, 2.4, 3.9, 4.16, 5.25, 6.36, 7.49, 8.64);
		testRoundTripSparse(new TypeDescriptor(Double.class), EMPTY, -3.5, 1.1, 2.4, 3.9, 4.16);
	}

	@Test
	public void testString () throws Exception {
		testRoundTripDense(new TypeDescriptor(String.class), EMPTY, "a", "bb", "ccc", "dddd", "eeeee", "ffffff", "ggggggg", "hhhhhhhh");
		testRoundTripSparse(new TypeDescriptor(String.class), EMPTY, "invalid", "a", "bb", "ccc", "dddd");
	}

	@Test
	public void testCustom () throws Exception {
		testRoundTripDense(new TypeDescriptor(CustomTestData.class),
		                   new Class<?>[] {CustomTestData.class},
		                   new CustomTestData(1, 1.1, "one"),
		                   new CustomTestData(2, 2.2, "two"),
		                   new CustomTestData(3, 3.3, "three"),
		                   new CustomTestData(4, 4.4, "four"));
		testRoundTripSparse(new TypeDescriptor(CustomTestData.class),
		                    new Class<?>[] {CustomTestData.class},
		                    new CustomTestData(-1, -1.1, "empty"),
		                    new CustomTestData(1, 1.1, "one"),
		                    new CustomTestData(2, 2.2, "two"),
		                    new CustomTestData(3, 3.3, "three"),
		                    new CustomTestData(4, 4.4, "four"));
	}


	static class CustomTestData {
		int _i;
		double _d;
		String _s;

		CustomTestData () {
		}
		CustomTestData (int i, double d, String s) {
			_i = i;
			_d = d;
			_s = s;
		}
		@Override
		public boolean equals (Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof CustomTestData)) return false;
			CustomTestData that = (CustomTestData) obj;
			return (this._i == that._i && this._d == that._d && this._s.equals(that._s));
		}
	}



	@Test
	public void testCompression () throws IOException {
		TileData<Double> data = new DenseTileData<Double>(new TileIndex(0, 0, 0), 1.1);
		TileSerializer<Double> serializer = new KryoSerializer<Double>(new TypeDescriptor(Double.class));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		serializer.serialize(data, output);
		output.flush();
		output.close();

		byte[] buffer = output.toByteArray();
		Assert.assertTrue(buffer.length < 256*256);
	}

	// Make sure the serializer itself is serializable.
	@Test
	public void testSerializerSerializability () throws IOException, ClassNotFoundException, ConfigurationException {
		KryoSerializer<CustomTestData> input = new KryoSerializer<>(new TypeDescriptor(CustomTestData.class), CustomTestData.class);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);

		oos.writeObject(input);
		oos.flush();
		oos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object rawOutput = ois.readObject();

		// Make sure our output is as correct as we can.
		Assert.assertTrue(rawOutput instanceof KryoSerializer<?>);
		KryoSerializer<?> genericOutput = (KryoSerializer<?>) rawOutput;
		Assert.assertEquals(new TypeDescriptor(CustomTestData.class), genericOutput.getBinTypeDescription());
		TileSerializer<CustomTestData> output = SerializationTypeChecker.checkBinClass(genericOutput,
			         CustomTestData.class,
			         new TypeDescriptor(CustomTestData.class));

		// Make sure the two versions serialize something identically
		TileData<CustomTestData> inputData = new DenseTileData<CustomTestData>(new TileIndex(0, 0, 0, 1, 1));
		inputData.setBin(0, 0, new CustomTestData(1, 2.0, "3"));

		baos = new ByteArrayOutputStream();
		input.serialize(inputData, baos);
		baos.flush();
		baos.close();
		bais = new ByteArrayInputStream(baos.toByteArray());
		TileData<CustomTestData> outputData1 = output.deserialize(new TileIndex(0, 0, 0, 1, 1), bais);

		baos = new ByteArrayOutputStream();
		output.serialize(inputData, baos);
		baos.flush();
		baos.close();
		bais = new ByteArrayInputStream(baos.toByteArray());
		TileData<CustomTestData> outputData2 = input.deserialize(new TileIndex(0, 0, 0, 1, 1), bais);

		Assert.assertEquals(1,   outputData1.getBin(0, 0)._i);
		Assert.assertEquals(2.0, outputData1.getBin(0, 0)._d, 1E-12);
		Assert.assertEquals("3", outputData1.getBin(0, 0)._s);

		Assert.assertEquals(1,   outputData2.getBin(0, 0)._i);
		Assert.assertEquals(2.0, outputData2.getBin(0, 0)._d, 1E-12);
		Assert.assertEquals("3", outputData2.getBin(0, 0)._s);
	}



	@Test
	public void testBZip () throws Exception {
		// Create our serializer
		KryoSerializer<List<Integer>> serializer =
			new KryoSerializer<>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)),
			                     KryoSerializer.Codec.BZIP);

		// Create a large tile
		int xN = 256;
		int yN = 256;
		int zN = 100;
		TileIndex index = new TileIndex(0, 0, 0, xN, yN);
		TileData<List<Integer>> input = new DenseTileData<List<Integer>>(index);
		for (int x=0; x<xN; ++x) {
			for (int y=0; y<yN; ++y) {
				List<Integer> bin = new ArrayList<>(zN);
				for (int z=0; z<zN; ++z)  bin.add((int) Math.floor(Math.random()*1024));
				input.setBin(x, y, bin);
			}
		}

		// Write it out
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(input, baos);
		baos.flush();
		baos.close();

		// Read it back in
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		TileData<List<Integer>> output = serializer.deserialize(index, bais);

		assertListTilesEqual(input, output);
	}


	@Test
	public void testGZip () throws Exception {
		// Create our serializer
		KryoSerializer<List<Integer>> serializer =
			new KryoSerializer<>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)),
			                     KryoSerializer.Codec.GZIP);

		// Create a large tile
		int xN = 256;
		int yN = 256;
		int zN = 100;
		TileIndex index = new TileIndex(0, 0, 0, xN, yN);
		TileData<List<Integer>> input = new DenseTileData<List<Integer>>(index);
		for (int x=0; x<xN; ++x) {
			for (int y=0; y<yN; ++y) {
				List<Integer> bin = new ArrayList<>(zN);
				for (int z=0; z<zN; ++z)  bin.add((int) Math.floor(Math.random()*1024));
				input.setBin(x, y, bin);
			}
		}

		// Write it out
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(input, baos);
		baos.flush();
		baos.close();

		// Read it back in
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		TileData<List<Integer>> output = serializer.deserialize(index, bais);

		assertListTilesEqual(input, output);
	}


	@Test
	public void testDeflate () throws Exception {
		// Create our serializer
		KryoSerializer<List<Integer>> serializer =
			new KryoSerializer<>(new TypeDescriptor(List.class, new TypeDescriptor(Integer.class)),
			                     KryoSerializer.Codec.DEFLATE);

		// Create a large tile
		int xN = 256;
		int yN = 256;
		int zN = 100;
		TileIndex index = new TileIndex(0, 0, 0, xN, yN);
		TileData<List<Integer>> input = new DenseTileData<List<Integer>>(index);
		for (int x=0; x<xN; ++x) {
			for (int y=0; y<yN; ++y) {
				List<Integer> bin = new ArrayList<>(zN);
				for (int z=0; z<zN; ++z)  bin.add((int) Math.floor(Math.random()*1024));
				input.setBin(x, y, bin);
			}
		}

		// Write it out
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(input, baos);
		baos.flush();
		baos.close();

		// Read it back in
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		TileData<List<Integer>> output = serializer.deserialize(index, bais);

		assertListTilesEqual(input, output);
	}
}
