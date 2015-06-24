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

import com.oculusinfo.binning.util.TypeDescriptor;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



abstract public class GenericAvroArraySerializer<T> extends GenericAvroSerializer<List<T>> {
	private static final long serialVersionUID = 6426634603381096097L;



	public GenericAvroArraySerializer (CodecFactory compressionCodec, TypeDescriptor elementTypeDescription) {
		super(compressionCodec, new TypeDescriptor(List.class, elementTypeDescription));
	}

	transient private Schema _entrySchema;

	abstract protected String getEntrySchemaFile ();
	abstract protected T getEntryValue (GenericRecord entry);
	abstract protected void setEntryValue (GenericRecord avroEntry, T rawEntry) throws IOException;

	@Override
	protected String getRecordSchemaFile () {
		return "arrayData.avsc";
	}

	protected Schema getEntrySchema () throws IOException {
		if (_entrySchema == null) {
			_entrySchema = createEntrySchema();
		}
		return _entrySchema;
	}

	protected Schema createEntrySchema() throws IOException {
		return new AvroSchemaComposer().addResource(getEntrySchemaFile()).resolved();
	}

	@Override
	protected Schema createRecordSchema() throws IOException {
		return new AvroSchemaComposer().add(getEntrySchema()).addResource(getRecordSchemaFile()).resolved();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<T> getValue (GenericRecord bin) {
		GenericData.Array<GenericRecord> avroValues = (GenericData.Array<GenericRecord>) bin.get("value");
		List<T> values = new ArrayList<T>();
		for (GenericRecord entry: avroValues) {
			values.add(getEntryValue(entry));
		}
		return values;
	}

	@Override
	protected void setValue (GenericRecord bin, List<T> values) throws IOException {
		List<GenericRecord> avroValues = new ArrayList<GenericRecord>();

		if (null != values) {
			for (T value : values) {
				GenericRecord avroValue = new GenericData.Record(getEntrySchema());
				setEntryValue(avroValue, value);
				avroValues.add(avroValue);
			}
		}

		bin.put("value", avroValues);
	}
}
