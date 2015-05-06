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
package com.oculusinfo.binning.util;


import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.FileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;



/**
 * Simple utility class to convert Avro files to JSON (and, maybe, vice versa)
 *
 * @author nkronenfeld
 */
public class AvroJSONConverter {
	/**
	 * Convert an Avro input stream into a JSON object
	 *
	 * @param stream The input data
	 * @return A JSON representation of the input data
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject convert (InputStream stream) throws IOException, JSONException {
		SeekableInput input = new SeekableByteArrayInput(IOUtils.toByteArray(stream));
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		// Conversion code taken from org.apache.avro.tool.DataFileReadTool
		GenericDatumReader<Object> reader = new GenericDatumReader<>();
		FileReader<Object> fileReader = DataFileReader.openReader(input, reader);
		try {
			Schema schema = fileReader.getSchema();
			DatumWriter<Object> writer = new GenericDatumWriter<>(schema);
			JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, output);
			for (Object datum: fileReader) {
				encoder.configure(output);
				writer.write(datum, encoder);
				encoder.flush();
				// For some reason, we only contain one record, but the
				// decoding thinks we contain more and fails; so just break
				// after our first one.
				break;
			}
			output.flush();
		} finally {
			fileReader.close();
		}
		String jsonString = output.toString("UTF-8");
		JSONObject json = new JSONObject(jsonString);

		// check meta data node of tile, typically it is serialized as a string
		// however this string will be valid JSON and can thus be parsed further
		JSONObject meta = json.optJSONObject("meta");
		if ( meta != null ) {
			JSONObject map = meta.optJSONObject("map");
			if ( map != null ) {
				String bins = map.optString("bins", null);
				if ( bins != null ) {
					map.put( "bins", new JSONArray( bins ) );
				}
			}
		}
		return json;
	}
}
