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

import com.oculusinfo.binning.*;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * A serializer to serialize tiles whose bins contain a sparse array of values.
 *
 * Avro seems to be very slow at deserializing nested arrays.  Therefore, this serializer flattens the array of
 * bins and the arrays in the bins into a single array.
 *
 * See {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer}
 * for information about what primitives are supported, and how.
 */
abstract public class FlattenedAvroSparseArraySerializer<T> extends GenericAvroSerializer<Map<Integer, T>>  {
    private Schema _entrySchema;

    public FlattenedAvroSparseArraySerializer(CodecFactory compressionCodec, TypeDescriptor elementTypeDescription) {
        super(compressionCodec, new TypeDescriptor(List.class, elementTypeDescription));
    }

    abstract protected String getRecordSchemaFile ();
    abstract protected T getEntryValue (GenericRecord entry);
    abstract protected void setEntryValue (GenericRecord avroEntry, T rawEntry) throws IOException;

    // Not needed, should never be called.
    @Override protected Map<Integer, T> getValue(GenericRecord bin) {
        throw new UnsupportedOperationException("getValue not supported for flattened arrays");
    }
    @Override protected void setValue(GenericRecord bin, Map<Integer, T> value) throws IOException {
        throw new UnsupportedOperationException("setValue not supported for flattened arrays");
    }

    @Override
    protected Schema createTileSchema (TileData.StorageType storage) throws IOException {
        switch (storage) {
            case Sparse:
                return new AvroSchemaComposer().add(getRecordSchema()).addResource("sparseFlatArrayTile.avsc").resolved();
            default:
                return null;
        }
    }

    @Override
    public TileData<Map<Integer, T>> deserialize(TileIndex index, InputStream stream) throws IOException {

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
            GenericData.Array<GenericRecord> entries = (GenericData.Array<GenericRecord>) r.get("values");

            // See if this is a sparse or dense array.
            TileData.StorageType storage = TileData.StorageType.Dense;
            String schemaName = r.getSchema().getName();
            if ("sparseFlatArrayTile".equals(schemaName)) {
                storage = TileData.StorageType.Sparse;
            } else {
                throw new IOException("Illegal tile for flattened sparse array serializer - tile was not a flattened sparse array.");
            }

            TileData<Map<Integer, T>> newTile = null;

            switch (storage) {
                case Sparse: {
                    Map<Integer, Map<Integer, Map<Integer, T>>> data = new HashMap<>();
                    for (GenericRecord entry : entries) {
                        int x = (Integer) (entry.get("xIndex"));
                        int y = (Integer) (entry.get("yIndex"));
                        int z = (Integer) (entry.get("zIndex"));
                        T value = getEntryValue((GenericRecord) entry.get("value"));
                        if (!data.containsKey(x)) data.put(x, new HashMap<Integer, Map<Integer, T>>());
                        Map<Integer, Map<Integer, T>> xEntry = data.get(x);
                        if (!xEntry.containsKey(y)) xEntry.put(y, new HashMap<Integer, T>());
                        xEntry.get(y).put(z, value);
                    }
                    T defaultValue = getEntryValue((GenericRecord) r.get("default"));

                    newTile = new SparseTileData<Map<Integer, T>>(newTileIndex, data, new ConstantMap<T>(defaultValue));
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
    public void serialize (TileData<Map<Integer, T>> tile, OutputStream stream) throws IOException {
        Schema recordSchema = getRecordSchema();
        Schema tileSchema = getTileSchema(TileData.StorageType.Sparse);
        Schema binSchema = tileSchema.getField("values").schema().getElementType();

        SparseTileData<Map<Integer, T>> sparseTile = (SparseTileData<Map<Integer, T>>)tile;

        List<GenericRecord> bins = new ArrayList<GenericRecord>();
        Iterator<Pair<BinIndex, Map<Integer, T>>> i = sparseTile.getData();
        while (i.hasNext()) {
            Pair<BinIndex, Map<Integer, T>> next = i.next();
            BinIndex index = next.getFirst();
            Map<Integer, T> entries = next.getSecond();
            for (Map.Entry<Integer, T> entry: entries.entrySet()) {
                GenericRecord valueRecord = new GenericData.Record(recordSchema);
                setEntryValue(valueRecord, entry.getValue());
                GenericRecord binRecord = new GenericData.Record(binSchema);
                binRecord.put("xIndex", index.getX());
                binRecord.put("yIndex", index.getY());
                binRecord.put("zIndex", entry.getKey());
                binRecord.put("value", valueRecord);
                bins.add(binRecord);
            }
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
        T defaultValue = sparseTile.getDefaultBinValue().values().iterator().next();
        setEntryValue(defaultValueRecord, defaultValue);
        tileRecord.put("default", defaultValueRecord);

        writeRecord(tileRecord, tileSchema, stream);
    }
}
