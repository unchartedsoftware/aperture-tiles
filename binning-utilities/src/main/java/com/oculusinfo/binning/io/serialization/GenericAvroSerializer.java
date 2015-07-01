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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileData.StorageType;
import com.oculusinfo.factory.util.Pair;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;

import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.util.TypeDescriptor;

abstract public class GenericAvroSerializer<T> implements TileSerializer<T> {
	private static final long serialVersionUID = 5775555328063499845L;



	// Functions to encode and decode codecs as strings, so we can serialize
	// them accross the network or accross machines.  Unfortunately, we cannot
	// simply use CodecFactory.toString and CodecFactory.fromString, as they
	// fail to reverse each other for deflate codecs.
	private static CodecFactory descriptionToCodec (String codecDescription) {
		if (codecDescription.startsWith("deflate")) {
			// Knock off the initial "deflate-"
			int deflateLevel = Integer.parseInt(codecDescription.substring(8));
			return CodecFactory.deflateCodec(deflateLevel);
		} else {
			return CodecFactory.fromString(codecDescription);
		}
	}
	private static String codecToDescription (CodecFactory codec) {
		return codec.toString();
	}



	private transient ThreadLocal<Map<StorageType, Schema>> _tileSchema;
	private transient Schema                                _recordSchema;

	private String                                          _compressionCodec;
	private TypeDescriptor                                  _typeDescription;

	protected GenericAvroSerializer (CodecFactory compressionCodec, TypeDescriptor typeDescription) {
		_compressionCodec = codecToDescription(compressionCodec);
		_typeDescription = typeDescription;
		_tileSchema = null;
		_recordSchema = null;
	}

	abstract protected String getRecordSchemaFile ();
	abstract protected T getValue (GenericRecord bin);
	abstract protected void setValue (GenericRecord bin, T value) throws IOException ;

	public String getFileExtension(){
		return "avro";
	}

	protected Schema getRecordSchema () throws IOException {
		if (_recordSchema == null) {
			_recordSchema = createRecordSchema();
		}
		return _recordSchema;
	}

	protected Schema createRecordSchema() throws IOException {
		return new AvroSchemaComposer().addResource(getRecordSchemaFile()).resolved();
	}

	protected Schema getTileSchema (StorageType storage) throws IOException {
		if (null == _tileSchema)
			_tileSchema = new ThreadLocal<Map<StorageType, Schema>>() {
				@Override
				protected Map<StorageType, Schema> initialValue () {
					return new HashMap<TileData.StorageType, Schema>();
				}
			};
		if (!_tileSchema.get().containsKey(storage)) {
			_tileSchema.get().put(storage, createTileSchema(storage));
		}
		return _tileSchema.get().get(storage);
	}

	protected Schema createTileSchema (StorageType storage) throws IOException {
		switch (storage) {
		case Dense:
			return new AvroSchemaComposer().add(getRecordSchema()).addResource("denseTile.avsc").resolved();
		case Sparse:
			return new AvroSchemaComposer().add(getRecordSchema()).addResource("sparseTile.avsc").resolved();
		default:
			return null;
		}
	}

	@Override
	public TypeDescriptor getBinTypeDescription () {
		return _typeDescription;
	}

	protected Map<String, String> getTileMetaData (TileData<T> tile) {
		Collection<String> keys = tile.getMetaDataProperties();
		if (null == keys || keys.isEmpty()) return null;
		Map<String, String> metaData = new HashMap<String, String>();
		for (String key: keys) {
			String value = tile.getMetaData(key);
			if (null != value)
				metaData.put(key, value);
		}
		return metaData;
	}

	@Override
	public TileData<T> deserialize(TileIndex index, InputStream stream) throws IOException {

		DatumReader<GenericRecord> reader = new GenericDatumReader<GenericRecord>();
		DataFileStream<GenericRecord> dataFileReader = new DataFileStream<GenericRecord>(stream, reader);

		try {

			GenericRecord r = dataFileReader.next();

			int level = (Integer) r.get("level");
			int xIndex = (Integer) r.get("xIndex");
			int yIndex = (Integer) r.get("yIndex");
			int xBins = (Integer) r.get("xBinCount");
			int yBins = (Integer) r.get("yBinCount");
			Map<?, ?> meta = (Map<?, ?>) r.get("meta");
			TileIndex newTileIndex = new TileIndex(level, xIndex, yIndex, xBins, yBins);

			// Warning suppressed because Array.newInstance definitionally returns
			// something of the correct type, or throws an exception
			@SuppressWarnings("unchecked")
			GenericData.Array<GenericRecord> bins = (GenericData.Array<GenericRecord>) r.get("values");

			// See if this is a sparse or dense array.
			StorageType storage = StorageType.Dense;
			if (r.getSchema().getName().equals("sparseTile")) {
				storage = StorageType.Sparse;
			}
			TileData<T> newTile = null;

			switch (storage) {
			case Dense: {
				List<T> data = new ArrayList<T>(xBins * yBins);
				int i = 0;
				for (GenericRecord bin : bins) {
					data.add(getValue(bin));
					++i;
					if (i >= xBins * yBins) break;
				}
				T defaultValue = null;
				GenericRecord defaultBin = (GenericRecord) r.get("default");
				if (null != defaultBin) {
					defaultValue = getValue((GenericRecord) r.get("default"));
				}

				newTile = new DenseTileData<T>(newTileIndex, defaultValue, data);
				break;
			}
			case Sparse: {
				Map<Integer, Map<Integer, T>> data = new HashMap<>();
				for (GenericRecord bin : bins) {
					int x = (Integer) (bin.get("xIndex"));
					int y = (Integer) (bin.get("yIndex"));
					T value = getValue((GenericRecord) bin.get("value"));
					if (!data.containsKey(x)) data.put(x, new HashMap<Integer, T>());
					data.get(x).put(y, value);
				}
				T defaultValue = getValue((GenericRecord) r.get("default"));

				newTile = new SparseTileData<T>(newTileIndex, data, defaultValue);
				break;
			}
			default: return null;
			}

			// Add in metaData
			if (null != meta) {
				for (Object key : meta.keySet()) {
					if (null != key) {
						Object value = meta.get(key);
						if (null != value) {
							newTile.setMetaData(key.toString(), value.toString());
						}
					}
				}
			}
			return newTile;
		} finally {
			dataFileReader.close();
			stream.close();
		}
	}

	@Override
	public void serialize (TileData<T> tile, OutputStream stream) throws IOException {
		if (tile instanceof SparseTileData<?>) {
			serializeSparse((SparseTileData<T>) tile, stream);
		} else {
			serializeDense(tile, stream);
		}
	}

	private void serializeSparse (SparseTileData<T> tile, OutputStream stream) throws IOException {
		Schema recordSchema = getRecordSchema();
		Schema tileSchema = getTileSchema(StorageType.Sparse);
		Schema binSchema = tileSchema.getField("values").schema().getElementType();

		List<GenericRecord> bins = new ArrayList<GenericRecord>();

		Iterator<Pair<BinIndex, T>> i = tile.getData();
		while (i.hasNext()) {
			Pair<BinIndex, T> next = i.next();
			BinIndex index = next.getFirst();
			T value = next.getSecond();
			GenericRecord valueRecord = new GenericData.Record(recordSchema);
			setValue(valueRecord, value);
			GenericRecord binRecord = new GenericData.Record(binSchema);
			binRecord.put("xIndex", index.getX());
			binRecord.put("yIndex", index.getY());
			binRecord.put("value", valueRecord);
			bins.add(binRecord);
		}

		GenericRecord tileRecord = new GenericData.Record(tileSchema);
		TileIndex idx = tile.getDefinition();
		tileRecord.put("level", idx.getLevel());
		tileRecord.put("xIndex", idx.getX());
		tileRecord.put("yIndex", idx.getY());
		tileRecord.put("xBinCount", idx.getXBins());
		tileRecord.put("yBinCount", idx.getYBins());
		tileRecord.put("values", bins);
		tileRecord.put("meta", getTileMetaData(tile));

		GenericRecord defaultValueRecord = new GenericData.Record(recordSchema);
		setValue(defaultValueRecord, tile.getDefaultValue());
		tileRecord.put("default", defaultValueRecord);

		writeRecord(tileRecord, tileSchema, stream);
	}

	private void serializeDense (TileData<T> tile, OutputStream stream) throws IOException {
		Schema recordSchema = getRecordSchema();
		Schema tileSchema = getTileSchema(StorageType.Dense);
		TileIndex idx = tile.getDefinition();

		List<GenericRecord> bins = new ArrayList<GenericRecord>();

		List<T> denseData = DenseTileData.getData(tile);
		for (T value: denseData) {
			GenericRecord bin = new GenericData.Record(recordSchema);
			setValue(bin, value);
			bins.add(bin);
		}

		GenericRecord tileRecord = new GenericData.Record(tileSchema);
		tileRecord.put("level", idx.getLevel());
		tileRecord.put("xIndex", idx.getX());
		tileRecord.put("yIndex", idx.getY());
		tileRecord.put("xBinCount", idx.getXBins());
		tileRecord.put("yBinCount", idx.getYBins());
		tileRecord.put("values", bins);
		tileRecord.put("meta", getTileMetaData(tile));

		T defaultValue = tile.getDefaultValue();
		if (null == defaultValue) {
			tileRecord.put("default", null);
		} else {
			GenericRecord defaultValueRecord = new GenericData.Record(recordSchema);
			setValue(defaultValueRecord, tile.getDefaultValue());
			tileRecord.put("default", defaultValueRecord);
		}

		writeRecord(tileRecord, tileSchema, stream);
	}

	private void writeRecord (GenericRecord record, Schema schema, OutputStream stream) throws IOException {
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
		try {
			dataFileWriter.setCodec(descriptionToCodec(_compressionCodec));
			dataFileWriter.create(schema, stream);
			dataFileWriter.append(record);
			dataFileWriter.close();
			stream.close();
		} catch (IOException e) {throw new RuntimeException("Error serializing",e);}
	}
}
