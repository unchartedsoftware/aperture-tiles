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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericRecord;

import com.oculusinfo.binning.io.serialization.GenericAvroArraySerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;

public class StringDoublePairArrayAvroSerializer
	extends GenericAvroArraySerializer<Pair<String, Double>>
{
	private static final long serialVersionUID = 4626180730261555975L;
	private static final TypeDescriptor TYPE_DESCRIPTOR = new TypeDescriptor(Pair.class,
		   new TypeDescriptor(String.class),
		   new TypeDescriptor(Double.class));



	public static final Map<String,String> META;
	static {
		Map<String,String> map = new HashMap<String, String>();
		map.put("source", "Oculus Binning Utilities");
		map.put("data-type", "string-int pair array");
		META = Collections.unmodifiableMap(map);
	}



	public StringDoublePairArrayAvroSerializer(CodecFactory compressionCodec) {
		super(compressionCodec, TYPE_DESCRIPTOR);
	}

	@Override
	protected String getEntrySchemaFile () {
		return "stringDoublePairEntry.avsc";
	}

	@Override
	protected Map<String, String> getTileMetaData () {
		return META;
	}

	@Override
	protected Pair<String, Double> getEntryValue (GenericRecord entry) {
		return new Pair<String, Double>(entry.get("key").toString(), (Double) entry.get("value"));
	}

	@Override
	protected void setEntryValue (GenericRecord avroEntry,
	                              Pair<String, Double> rawEntry) throws IOException {
		avroEntry.put("key", rawEntry.getFirst());
		avroEntry.put("value", rawEntry.getSecond());
	}
}
