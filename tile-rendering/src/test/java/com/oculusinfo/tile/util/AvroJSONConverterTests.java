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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oculusinfo.tile.util;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.PairArrayAvroSerializer;
import com.oculusinfo.binning.util.AvroJSONConverter;
import com.oculusinfo.factory.util.Pair;



public class AvroJSONConverterTests {
	private static final double EPSILON = 1E-12;
	private static InputStream toInputStream (Schema schema, GenericRecord... records) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
		dataFileWriter.create(schema, stream);
		for (GenericRecord record: records) {
			dataFileWriter.append(record);
		}
		dataFileWriter.close();
		stream.flush();

		return new ByteArrayInputStream(stream.toByteArray());
	}

	@Test
	public void testSimpleRecord () throws IOException, JSONException {
		Schema schema = new Parser().parse("{ \"name\": \"test\", \"type\": \"record\", \"fields\": [ { \"name\": \"value\", \"type\": \"double\" } ] }");
		GenericRecord record = new GenericData.Record(schema);
		record.put("value", 3.4);

		JSONObject result = AvroJSONConverter.convert(toInputStream(schema, record));
		Assert.assertEquals(3.4, result.getDouble("value"), EPSILON);
	}

	@Test
	public void testNestedRecord () throws IOException, JSONException {
		Schema schema = new Parser().parse("{ "
		                                   + "\"name\": \"test\", "
		                                   + "\"type\": \"record\", "
		                                   + "\"fields\": "
		                                   +" [ "
		                                   + "  { "
		                                   + "    \"name\": \"rval\", "
		                                   + "    \"type\": {"
		                                   + "      \"name\": \"rvalEntry\", "
		                                   + "      \"type\": \"record\", "
		                                   + "      \"fields\": [ { \"name\": \"a\", \"type\": \"int\" }, "
		                                   + "                    { \"name\": \"b\", \"type\": \"string\" } ] "
		                                   + "    }"
		                                   + "  }, "
		                                   + "  { \"name\": \"dval\", \"type\": \"double\" } "
		                                   + "] "
		                                   +"}");
		GenericRecord record = new GenericData.Record(schema);
		GenericRecord subRecord = new GenericData.Record(schema.getField("rval").schema());
		subRecord.put("a", 2);
		subRecord.put("b", "abc");
		record.put("rval", subRecord);
		record.put("dval", 3.6);

		JSONObject result = AvroJSONConverter.convert(toInputStream(schema, record));
		Assert.assertEquals(3.6, result.getDouble("dval"), EPSILON);
		JSONObject subResult = result.getJSONObject("rval");
		Assert.assertEquals(2, subResult.getInt("a"));
		Assert.assertEquals("abc", subResult.getString("b"));
	}

	@Test
	public void testReadWordScoreTile () throws IOException, JSONException {
		// Create a tile to test
		TileSerializer<List<Pair<String, Double>>> serializer = new PairArrayAvroSerializer<>(String.class, Double.class, CodecFactory.nullCodec());
		TileIndex index = new TileIndex(0, 0, 0, 1, 1);
		DenseTileData<List<Pair<String, Double>>> tile = new DenseTileData<>(index);
		List<Pair<String, Double>> bin = new ArrayList<>();
		bin.add(new Pair<String, Double>("abc", 1.0));
		bin.add(new Pair<String, Double>("def", 1.5));
		bin.add(new Pair<String, Double>("ghi", 2.0));
		bin.add(new Pair<String, Double>("jkl", 2.25));
		tile.setBin(0, 0, bin);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		serializer.serialize(tile, baos);
		baos.flush();
		baos.close();
		byte[] serializedTileData = baos.toByteArray();

		// Now try to convert that to JSON.
		JSONObject result = AvroJSONConverter.convert(new ByteArrayInputStream(serializedTileData));
		System.out.println(result.toString());
	}
}
