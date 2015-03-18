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
package com.oculusinfo.binning.io.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.avro.file.CodecFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.impl.GenericJavaSerializer;
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
/*
 * Some tests to help compare serialization speeds between various schemes
 */
@Ignore
public class SerializationSpeedTests {
	private static final String LEGACY = "Legacy";
	private static final String AVRO = "Avro";
	private static final String JAVA = "Java";
	private static final String KRYO = "Kryo";
	private static final String VECTOR_DATA = "vector";
	private static final String SCALAR_DATA = "scalar";
	private static final int BIN_SIZE = 256;
	private static final int VECTOR_SIZE = 48;
	private static final int ITERATIONS = 20;	

	private TileData<Double>       _scalarData;
	private TileData<List<Double>> _vectorData;

	@Before
	public void setup () {
		// Create some data
		Random random = new Random(15485863);
		_scalarData = new DenseTileData<>(new TileIndex(0, 0, 0, 256, 256));
		for (int x=0; x < BIN_SIZE; ++x) {
			for (int y=0; y < BIN_SIZE; ++y) {
				_scalarData.setBin(x, y, random.nextDouble());
			}
		}

		// Random vector data for more complex types.
		_vectorData = new DenseTileData<>(new TileIndex(0, 0, 0, 256, 256));
		for (int x=0; x < BIN_SIZE; ++x) {
			for (int y=0; y < BIN_SIZE; ++y) {
				List<Double> binData = new ArrayList<Double>(1);
				for (int i = 0; i < VECTOR_SIZE; i++) {
					binData.add(random.nextBoolean() ? random.nextDouble() : 0.0);
				}
				_vectorData.setBin(x, y, binData);
			}
		}
	}



	@SuppressWarnings("deprecation")
	@Test
	public void testLegacyTileSerialization () throws Exception {
		serialize(LEGACY, SCALAR_DATA, _scalarData, new com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testLegacyTileDeSerialization () throws Exception {
		deserialize(LEGACY, SCALAR_DATA, _scalarData, new com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer());
	}



	@Test
	public void testJavaTileSerialization () throws Exception {
		serialize(JAVA, SCALAR_DATA, _scalarData,  new GenericJavaSerializer<Double>(new TypeDescriptor(Double.class)));
	}

	@Test
	public void testJavaTileDeSerialization () throws Exception {
		deserialize(JAVA, SCALAR_DATA, _scalarData, new GenericJavaSerializer<Double>(new TypeDescriptor(Double.class)));
	}

	@Test
	public void testJavaVectorTileSerialization () throws Exception {
		serialize(JAVA, VECTOR_DATA, _vectorData, new GenericJavaSerializer<List<Double>>(new TypeDescriptor(Double.class)));
	}

	@Test
	public void testJavaVectorTileDeSerialization () throws Exception {
		deserialize(JAVA, VECTOR_DATA, _vectorData, new GenericJavaSerializer<List<Double>>(new TypeDescriptor(Double.class)));
	}



	@Test
	public void testAvroTileSerialization () throws Exception {		
		serialize(AVRO, SCALAR_DATA, _scalarData, new PrimitiveAvroSerializer<>(Double.class, CodecFactory.bzip2Codec()));
	}

	@Test
	public void testAvroTileDeSerialization () throws Exception {
		deserialize(AVRO, SCALAR_DATA, _scalarData, new PrimitiveAvroSerializer<>(Double.class, CodecFactory.bzip2Codec()));
	}

	@Test
	public void testAvroVectorTileSerialization () throws Exception {
		serialize(AVRO, VECTOR_DATA, _vectorData, new PrimitiveArrayAvroSerializer<>(Double.class, CodecFactory.bzip2Codec()));
	}

	@Test
	public void testAvroVectorTileDeSerialization () throws Exception {
		deserialize(AVRO, VECTOR_DATA, _vectorData, new PrimitiveArrayAvroSerializer<>(Double.class, CodecFactory.bzip2Codec()));
	}



	@Test
	public void testKryoTileSerialization () throws Exception {
		serialize(KRYO, SCALAR_DATA, _scalarData, new KryoSerializer<Double>(new TypeDescriptor(Double.class)));
	}

	@Test
	public void testKryoTileDeSerialization () throws Exception {
		deserialize(KRYO, SCALAR_DATA, _scalarData, new KryoSerializer<Double>(new TypeDescriptor(Double.class)));
	}

	@Test
	public void testKryoVectorTileSerialization () throws Exception {
		serialize(KRYO, VECTOR_DATA, _vectorData, new KryoSerializer<List<Double>>(new TypeDescriptor(List.class, new TypeDescriptor(Double.class))));
	}

	@Test
	public void testKryoVectorTileDeSerialization () throws Exception {
		deserialize(KRYO, VECTOR_DATA, _vectorData, new KryoSerializer<List<Double>>(new TypeDescriptor(List.class, new TypeDescriptor(Double.class))));
	}



	private <T> void serialize (String serializerType, String dataType, TileData<T> data, TileSerializer<T> serializer) throws IOException {
		// test serialization time
		long startTime = System.currentTimeMillis();
		for (int n=0; n<ITERATIONS; ++n) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.serialize(data, baos);
			baos.flush();
			baos.close();
		}
		long endTime = System.currentTimeMillis();
		System.out.println(serializerType + " Serialization - " + dataType);
		System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Average time: "+(((endTime-startTime)/1000.0)/ITERATIONS)+" seconds");
	}



	private <T> void deserialize (String serializerType, String dataType, TileData<T> tileData, TileSerializer<T> serializer) throws Exception {
		// Get something to deserialize
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(tileData, baos);
		baos.flush();
		baos.close();
		byte[] data = baos.toByteArray();

		// test deserialization time
		long startTime = System.currentTimeMillis();
		for (int n=0; n<ITERATIONS; ++n) {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			serializer.deserialize(tileData.getDefinition(), bais);
			bais.close();
		}
		long endTime = System.currentTimeMillis();
		System.out.println(serializerType + " Deserialization - " + dataType);
		System.out.println("Total time: "+((endTime-startTime)/1000.0)+" seconds");
		System.out.println("Average time: "+(((endTime-startTime)/1000.0)/ITERATIONS)+" seconds");
	}
}
