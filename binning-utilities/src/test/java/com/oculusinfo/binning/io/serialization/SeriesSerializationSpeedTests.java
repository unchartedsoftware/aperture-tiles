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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.avro.file.CodecFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.junit.Ignore;
import org.junit.Test;
import org.xerial.snappy.SnappyOutputStream;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer;
import com.oculusinfo.binning.io.serialization.impl.KryoSerializer.Codec;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveArrayAvroSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.util.Pair;



/*
 * This is a set of tests to determine the relative speeds of various serializers and/or
 * compression codecs over tiles with multiple elements per bin.
 *
 * It is not a general test, and would never be run as part of a test suite, but it's
 * useful enough when looking at new serializers and codecs that I want it in the code
 * base for future use and reference.
 */
@Ignore
public class SeriesSerializationSpeedTests {
	// Build up a list of pairs of table names/serializers
	private static TypeDescriptor __serializerType            = new TypeDescriptor(List.class, new TypeDescriptor(Double.class));
	private static TileSerializer<List<Double>> __kryoBZIP    = new KryoSerializer<>(__serializerType, Codec.BZIP);
	private static TileSerializer<List<Double>> __kryoGZIP    = new KryoSerializer<>(__serializerType, Codec.GZIP);
	private static TileSerializer<List<Double>> __kryoDEFLATE = new KryoSerializer<>(__serializerType, Codec.DEFLATE);
	private static TileSerializer<List<Double>> __avroBZIP    = new PrimitiveArrayAvroSerializer<>(Double.class, CodecFactory.bzip2Codec());
	private static <S, T> Pair<S, T> p (S first, T second) {
		return new Pair<>(first, second);
	}
	private static List<Pair<String, TileSerializer<List<Double>>>> __tables =
		Arrays.asList(p("kryo-002-BZIP.julia.x.y.series", __kryoBZIP),
		              p("kryo-005-BZIP.julia.x.y.series", __kryoBZIP),
		              p("kryo-010-BZIP.julia.x.y.series", __kryoBZIP),
		              p("kryo-020-BZIP.julia.x.y.series", __kryoBZIP),
		              p("kryo-050-BZIP.julia.x.y.series", __kryoBZIP),
		              p("kryo-100-BZIP.julia.x.y.series", __kryoBZIP),
		              p("kryo-002-GZIP.julia.x.y.series", __kryoGZIP),
		              p("kryo-005-GZIP.julia.x.y.series", __kryoGZIP),
		              p("kryo-010-GZIP.julia.x.y.series", __kryoGZIP),
		              p("kryo-020-GZIP.julia.x.y.series", __kryoGZIP),
		              p("kryo-050-GZIP.julia.x.y.series", __kryoGZIP),
		              p("kryo-100-GZIP.julia.x.y.series", __kryoGZIP),
		              p("kryo-002-DEFLATE.julia.x.y.series", __kryoDEFLATE),
		              p("kryo-005-DEFLATE.julia.x.y.series", __kryoDEFLATE),
		              p("kryo-010-DEFLATE.julia.x.y.series", __kryoDEFLATE),
		              p("kryo-020-DEFLATE.julia.x.y.series", __kryoDEFLATE),
		              p("kryo-050-DEFLATE.julia.x.y.series", __kryoDEFLATE),
		              p("kryo-100-DEFLATE.julia.x.y.series", __kryoDEFLATE),
		              p("avro-002.julia.x.y.series", __avroBZIP),
		              p("avro-005.julia.x.y.series", __avroBZIP),
		              p("avro-010.julia.x.y.series", __avroBZIP),
		              p("avro-020.julia.x.y.series", __avroBZIP),
		              p("avro-050.julia.x.y.series", __avroBZIP),
		              p("avro-100.julia.x.y.series", __avroBZIP)
		              );

	@Test
	public void tileSpeedTests () throws Exception {
		runTests(8, new TimingTestRunner());
	}



	private void runTests (int n, TestRunner runner) throws Exception {
		Configuration config = HBaseConfiguration.create();
		config.set("hbase.zookeeper.quorum", "hadoop-s1.oculus.local");
		config.set("hbase.zookeeper.property.clientPort", "2181");
		config.set("hbase.master", "hadoop-s1.oculus.local:60000");
		config.set("hbase.client.keyvalue.maxsize", "0");
		Connection connection = ConnectionFactory.createConnection(config);

		List<String> rows = new ArrayList<>();
		for (int x=0; x<n; ++x) {
			for (int y=0; y<n; ++y) {
				int digits = (int) Math.floor(Math.log10(1 << 3))+1;
				rows.add(String.format("%02d,%0"+digits+"d,%0"+digits+"d", 3, x, y));
			}
		}

		byte[]      EMPTY_BYTES          = new byte[0];
		byte[]      TILE_FAMILY_NAME     = "tileData".getBytes();

		List<Get> gets = new ArrayList<Get>(rows.size());
		for (String rowId: rows) {
			Get get = new Get(rowId.getBytes());
			get.addColumn(TILE_FAMILY_NAME, EMPTY_BYTES);
			gets.add(get);
		}

		runner.initial();
		for (int i=0; i<__tables.size(); ++i) {
			String tableName = __tables.get(i).getFirst();
			TileSerializer<List<Double>> serializer = __tables.get(i).getSecond();
			Table table = connection.getTable(TableName.valueOf(tableName));

			int tiles = 0;
			runner.preTest();
			long startTime = System.currentTimeMillis();
			Result[] results = table.get(gets);
			for (Result result: results) {
				if (result.containsColumn(TILE_FAMILY_NAME, EMPTY_BYTES)) {
					byte[] rowValue = result.getValue(TILE_FAMILY_NAME, EMPTY_BYTES);
					runner.runTest(tableName, rowValue, serializer);
					tiles++;
				}
			}
			long endTime = System.currentTimeMillis();
			table.close();

			runner.postTest(tableName, endTime-startTime,  tiles);
		}
	}

	interface TestRunner {
		void initial ();
		void preTest ();
		void runTest (String tableName, byte[] rowValue, TileSerializer<List<Double>> serializer) throws Exception;
		void postTest (String tableName, long elapsedTime, int iterations);
	}

	class TimingTestRunner implements TestRunner {
		long parseTime;
		double totalSize;
		TileIndex deserializationIndex = new TileIndex(3, 0, 0);

		@Override
		public void initial () {
			System.out.println("Fetched all 64 tiles from level 3");
			System.out.println("table\ttiles\taverage size\ttotal time\tparse time\tfetch time\tparse time/tile\tfetch time/tile");
		}

		@Override
		public void preTest () {
			parseTime = 0L;
			totalSize = 0;
		}

		@Override
		public void runTest (String tableName,
		                     byte[] rowValue,
		                     TileSerializer<List<Double>> serializer) throws Exception {

			long st2 = System.currentTimeMillis();
			ByteArrayInputStream bais = new ByteArrayInputStream(rowValue);
			serializer.deserialize(deserializationIndex, bais);
			long et2 = System.currentTimeMillis();

			totalSize += rowValue.length;
			parseTime += (et2-st2);
		}

		@Override
		public void postTest (String tableName, long elapsedTime, int iterations) {
			double time = elapsedTime/1000.0;
			double pTime = parseTime/1000.0;
			double fTime = time-pTime;
			System.out.println(String.format("%s\t%d\t%.4f\t%.4fs\t%.4fs\t%.4fs\t%.4fs\t%.4fs",
			                                 tableName, iterations, totalSize/iterations,
			                                 time, pTime, fTime, pTime/iterations, fTime/iterations));
		}

	}

	class SizeTestRunner implements TestRunner {
		long rawSize;

		long deflateSize;
		long zipSize;
		long gzipSize;
		long gzip2Size;
		long bzipSize;
		long snappySize;

		@Override
		public void initial () {
			System.out.println("set\tn\traw\tdeflate\tzip\tgzip\tgzip commons\tbzip\tsnappy");
		}

		@Override
		public void preTest () {
			rawSize = 0L;
			deflateSize = 0L;
			zipSize = 0L;
			gzipSize = 0L;
			gzip2Size = 0L;
			bzipSize = 0L;
			snappySize = 0L;
		}

		@Override
		public void runTest (String tableName, byte[] rowValue,
		                     TileSerializer<List<Double>> serializer) throws Exception {
			deflateSize += rowValue.length;

			ByteArrayInputStream bais = new ByteArrayInputStream(rowValue);
			InflaterInputStream iis = new InflaterInputStream(bais);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int input;
			while ((input = iis.read()) >= 0) {
				baos.write(input);
			}
			baos.flush();
			baos.close();
			byte[] raw = baos.toByteArray();
			rawSize += raw.length;

			baos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(baos);
			zos.putNextEntry(new ZipEntry("abc"));
			zos.write(raw);
			baos.flush();
			baos.close();
			zipSize += baos.toByteArray().length;
			bais = new ByteArrayInputStream(raw);

			baos = new ByteArrayOutputStream();
			GZIPOutputStream gos = new GZIPOutputStream(baos);
			gos.write(raw);
			baos.flush();
			baos.close();
			gzipSize += baos.toByteArray().length;

			baos = new ByteArrayOutputStream();
			GzipCompressorOutputStream gos2 = new GzipCompressorOutputStream(baos);
			gos2.write(raw);
			baos.flush();
			baos.close();
			gos2.close();
			gzip2Size += baos.toByteArray().length;

			baos = new ByteArrayOutputStream();
			BZip2CompressorOutputStream bos = new BZip2CompressorOutputStream(baos);
			bos.write(raw);
			baos.flush();
			baos.close();
			bos.close();
			bzipSize += baos.toByteArray().length;

			baos = new ByteArrayOutputStream();
			SnappyOutputStream sos = new SnappyOutputStream(baos);
			sos.write(raw);
			baos.flush();
			baos.close();
			sos.close();
			snappySize += baos.toByteArray().length;
		}

		@Override
		public void postTest (String tableName, long elapsedTime, int iterations) {
			System.out.println(String.format("%s\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f",
			                                 tableName, iterations,
			                                 (((double) rawSize)/iterations),
			                                 (((double) deflateSize)/iterations),
			                                 (((double) zipSize)/iterations),
			                                 (((double) gzipSize)/iterations),
			                                 (((double) gzip2Size)/iterations),
			                                 (((double) bzipSize)/iterations),
			                                 (((double) snappySize)/iterations)));
		}
	}
}
