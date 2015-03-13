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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.file.CodecFactory;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;

public class SerializerSerializabilityTests {
	@Test
	public void testPrimitiveSerializer () throws Exception {
		TileSerializer<Double> serialD = new PrimitiveAvroSerializer<>(Double.class, CodecFactory.nullCodec());

		// Actually use the serializer - the schema doesn't get populated until we do
		ByteArrayOutputStream baosTile = new ByteArrayOutputStream();
		serialD.serialize(new DenseTileData<Double>(new TileIndex(0, 0, 0, 1, 1), 0.0), baosTile);
		baosTile.flush();
		baosTile.close();

		// Now try serializing the serializer
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(serialD);
		oos.flush();
		oos.close();
		baos.flush();
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object result = ois.readObject();
		Assert.assertTrue(result instanceof PrimitiveAvroSerializer);
		PrimitiveAvroSerializer<?> serialR = (PrimitiveAvroSerializer<?>) result;
		Assert.assertEquals(new TypeDescriptor(Double.class), serialR.getBinTypeDescription());
		@SuppressWarnings({ "unchecked", "rawtypes" })
		PrimitiveAvroSerializer<Double> serialRD = (PrimitiveAvroSerializer) result;

		// Make sure they work the same
		TileData<Double> tile = new DenseTileData<>(new TileIndex(0, 0, 0, 4, 4),
		                                            Arrays.asList(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0,
		                                                          8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0));

		ByteArrayOutputStream tbaos1 = new ByteArrayOutputStream();
		serialD.serialize(tile, tbaos1);
		tbaos1.flush();
		tbaos1.close();

		ByteArrayOutputStream tbaos2 = new ByteArrayOutputStream();
		serialRD.serialize(tile, tbaos2);
		tbaos1.flush();
		tbaos1.close();

		ByteArrayInputStream tbais1 = new ByteArrayInputStream(tbaos1.toByteArray());
		TileData<Double> out1 = serialRD.deserialize(tile.getDefinition(), tbais1);

		ByteArrayInputStream tbais2 = new ByteArrayInputStream(tbaos1.toByteArray());
		TileData<Double> out2 = serialD.deserialize(tile.getDefinition(), tbais2);

		Assert.assertEquals(tile.getDefinition(), out1.getDefinition());
		Assert.assertEquals(tile.getDefinition(), out2.getDefinition());
		for (int x=0; x<4; ++x) {
			for (int y=0; y<4; ++y) {
				Assert.assertEquals(tile.getBin(x, y), out1.getBin(x, y), 1E-12);
				Assert.assertEquals(tile.getBin(x, y), out2.getBin(x, y), 1E-12);
			}
        
		}
        
	}

	@Test
	public void testPrimitiveArraySerializer () throws Exception {
		TileSerializer<List<Float>> serialD = new PrimitiveArrayAvroSerializer<>(Float.class, CodecFactory.nullCodec());

		// Actually use the serializer - the schema doesn't get populated until we do
		ByteArrayOutputStream baosTile = new ByteArrayOutputStream();
		serialD.serialize(new DenseTileData<List<Float>>(new TileIndex(0, 0, 0, 1, 1), Arrays.asList(0.0f)), baosTile);
		baosTile.flush();
		baosTile.close();

		// Now try serializing the serializer
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(serialD);
		oos.flush();
		oos.close();
		baos.flush();
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object result = ois.readObject();
		Assert.assertTrue(result instanceof PrimitiveArrayAvroSerializer);
		PrimitiveArrayAvroSerializer<?> serialR = (PrimitiveArrayAvroSerializer<?>) result;
		Assert.assertEquals(new TypeDescriptor(List.class, new TypeDescriptor(Float.class)), serialR.getBinTypeDescription());
		@SuppressWarnings({ "unchecked", "rawtypes" })
		PrimitiveArrayAvroSerializer<Float> serialRD = (PrimitiveArrayAvroSerializer) result;

		// Make sure they work the same
		TileData<List<Float>> tile = new DenseTileData<>(new TileIndex(0, 0, 0, 2, 2),
		                                                 Arrays.asList(Arrays.asList( 0.0f,  1.0f,  2.0f),
		                                                               Arrays.asList( 3.0f,  4.0f,  5.0f,  6.0f),
		                                                               Arrays.asList( 7.0f,  8.0f,  9.0f, 10.0f, 11.0f),
		                                                               Arrays.asList(12.0f, 13.0f, 14.0f, 15.0f, 16.0f, 17.0f)));

		ByteArrayOutputStream tbaos1 = new ByteArrayOutputStream();
		serialD.serialize(tile, tbaos1);
		tbaos1.flush();
		tbaos1.close();

		ByteArrayOutputStream tbaos2 = new ByteArrayOutputStream();
		serialRD.serialize(tile, tbaos2);
		tbaos1.flush();
		tbaos1.close();

		ByteArrayInputStream tbais1 = new ByteArrayInputStream(tbaos1.toByteArray());
		TileData<List<Float>> out1 = serialRD.deserialize(tile.getDefinition(), tbais1);

		ByteArrayInputStream tbais2 = new ByteArrayInputStream(tbaos1.toByteArray());
		TileData<List<Float>> out2 = serialD.deserialize(tile.getDefinition(), tbais2);

		Assert.assertEquals(tile.getDefinition(), out1.getDefinition());
		Assert.assertEquals(tile.getDefinition(), out2.getDefinition());
		for (int x=0; x<2; ++x) {
			for (int y=0; y<2; ++y) {
				List<Float> original = tile.getBin(x, y);
				List<Float> copy1 = out1.getBin(x, y);
				List<Float> copy2 = out2.getBin(x, y);
				Assert.assertEquals(original.size(), copy1.size());
				Assert.assertEquals(original.size(), copy2.size());
				for (int z=0; z<original.size(); ++z) {
					Assert.assertEquals(original.get(z), copy1.get(z), 1E-12);
					Assert.assertEquals(original.get(z), copy2.get(z), 1E-12);
				}
			}
        
		}
	}

	private <T, S> Pair<T, S> p (T t, S s) {
		return new Pair<T, S>(t, s);
	}
	@Test
	public void testPairArraySerializer () throws Exception {
		TileSerializer<List<Pair<String, Integer>>> serialD = new PairArrayAvroSerializer<>(String.class, Integer.class, CodecFactory.nullCodec());

		// Actually use the serializer - the schema doesn't get populated until we do
		ByteArrayOutputStream baosTile = new ByteArrayOutputStream();
		serialD.serialize(new DenseTileData<List<Pair<String, Integer>>>(new TileIndex(0, 0, 0, 1, 1), Arrays.asList(new Pair<String, Integer>("a", 1))), baosTile);
		baosTile.flush();
		baosTile.close();

		// Now try serializing the serializer
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(serialD);
		oos.flush();
		oos.close();
		baos.flush();
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Object result = ois.readObject();
		Assert.assertTrue(result instanceof PairArrayAvroSerializer);
		PairArrayAvroSerializer<?, ?> serialR = (PairArrayAvroSerializer<?, ?>) result;
		Assert.assertEquals(new TypeDescriptor(List.class, new TypeDescriptor(Pair.class, new TypeDescriptor(String.class), new TypeDescriptor(Integer.class))), serialR.getBinTypeDescription());
		@SuppressWarnings({ "unchecked", "rawtypes" })
		PairArrayAvroSerializer<String, Integer> serialRD = (PairArrayAvroSerializer) result;

		// Make sure they work the same
		TileData<List<Pair<String, Integer>>> tile = new DenseTileData<>(new TileIndex(0, 0, 0, 2, 2),
		                                                                 Arrays.asList(Arrays.asList(p("a", 1)),
			         Arrays.asList(p("b", 2), p("c", 3)),
			         Arrays.asList(p("d", 4), p("e", 5), p("f", 6)),
			         Arrays.asList(p("g", 7), p("h", 8), p("i", 9), p("j", 10))));

		ByteArrayOutputStream tbaos1 = new ByteArrayOutputStream();
		serialD.serialize(tile, tbaos1);
		tbaos1.flush();
		tbaos1.close();

		ByteArrayOutputStream tbaos2 = new ByteArrayOutputStream();
		serialRD.serialize(tile, tbaos2);
		tbaos1.flush();
		tbaos1.close();

		ByteArrayInputStream tbais1 = new ByteArrayInputStream(tbaos1.toByteArray());
		TileData<List<Pair<String, Integer>>> out1 = serialRD.deserialize(tile.getDefinition(), tbais1);

		ByteArrayInputStream tbais2 = new ByteArrayInputStream(tbaos1.toByteArray());
		TileData<List<Pair<String, Integer>>> out2 = serialD.deserialize(tile.getDefinition(), tbais2);

		Assert.assertEquals(tile.getDefinition(), out1.getDefinition());
		Assert.assertEquals(tile.getDefinition(), out2.getDefinition());
		for (int x=0; x<2; ++x) {
			for (int y=0; y<2; ++y) {
				List<Pair<String, Integer>> original = tile.getBin(x, y);
				List<Pair<String, Integer>> copy1 = out1.getBin(x, y);
				List<Pair<String, Integer>> copy2 = out2.getBin(x, y);
				Assert.assertEquals(original.size(), copy1.size());
				Assert.assertEquals(original.size(), copy2.size());
				for (int z=0; z<original.size(); ++z) {
					Assert.assertEquals(original.get(z), copy1.get(z));
					Assert.assertEquals(original.get(z), copy2.get(z));
				}
			}
        
		}
	}
}
