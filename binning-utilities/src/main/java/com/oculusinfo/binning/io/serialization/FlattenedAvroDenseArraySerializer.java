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

import com.oculusinfo.binning.DenseTileData;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A serializer to serialize tiles whose bins contain a dense array of values.
 *
 * Avro seems to be very slow at deserializing nested arrays.  Therefore, this serializer flattens the array of
 * bins and the arrays in the bins into a single array.
 *
 * See {@link com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer}
 * for information about what primitives are supported, and how.
 */
abstract public class FlattenedAvroDenseArraySerializer<T> extends GenericAvroSerializer<List<T>> {
    private Schema _entrySchema;

    public FlattenedAvroDenseArraySerializer(CodecFactory compressionCodec, TypeDescriptor elementTypeDescription) {
        super(compressionCodec, new TypeDescriptor(List.class, elementTypeDescription));
    }

    abstract protected String getRecordSchemaFile ();
    abstract protected T getEntryValue (GenericRecord entry);
    abstract protected void setEntryValue (GenericRecord avroEntry, T rawEntry) throws IOException;

    // Not needed, should never be called.
    @Override protected List<T> getValue(GenericRecord bin) {
        throw new UnsupportedOperationException("getValue not supported for flattened arrays");
    }
    @Override protected void setValue(GenericRecord bin, List<T> value) throws IOException {
        throw new UnsupportedOperationException("setValue not supported for flattened arrays");
    }

    @Override
    protected Schema createTileSchema (TileData.StorageType storage) throws IOException {
        switch (storage) {
            case Dense:
                return new AvroSchemaComposer().add(getRecordSchema()).addResource("denseFlatArrayTile.avsc").resolved();
            default:
                return null;
        }
    }

    abstract private class DenseBinTracker {
        private int                        _currentBin;
        private int                        _currentBinSize;
        private int                        _currentEntry;
        private List<List<T>>              _bins;
        private List<T>                    _entries;

        DenseBinTracker () {
            _currentBin = -1;
            _currentBinSize = 0;
            _currentEntry = 0;
            _bins = new ArrayList<>();
            _entries = new ArrayList<>();
        }

        abstract int binSize (int currentBin);
        abstract boolean validBin (int currentBin);

        void parseBinValues (GenericData.Array<GenericRecord> entries) {
            List<List<T>> bins = new ArrayList<>();

            for (GenericRecord entry: entries) {
                checkBinLimit();
                _entries.add(getEntryValue(entry));
                _currentEntry++;
            }
            checkBinLimit();
        }

        List<List<T>> getBinValues () {
            return _bins;
        }

        private void checkBinLimit () {
            while (_currentEntry >= _currentBinSize && validBin(_currentBin)) {
                advanceBin();
            }
        }

        protected void initBin () {
            _currentBin = 0;
            _currentBinSize = binSize(_currentBin);
        }

        private void advanceBin () {
            _bins.add(_entries);
            _entries = new ArrayList<>();
            ++_currentBin;
            _currentBinSize = binSize(_currentBin);
            _currentEntry = 0;
        }
    }
    private class ConstantSizeDenseBinTracker extends DenseBinTracker {
        private int _binSize;
        private int _numBins;
        ConstantSizeDenseBinTracker (int binSize, int numBins) {
            _binSize = binSize;
            _numBins = numBins;
            initBin();
        }

        @Override
        int binSize (int currentBin) {
            return _binSize;
        }

        @Override
        boolean validBin (int currentBin) {
            return currentBin < _numBins;
        }
    }

    private class VariableSizeDenseBinTracker extends DenseBinTracker {
        private GenericData.Array<Integer> _binLengths;

        VariableSizeDenseBinTracker (GenericData.Array<Integer> binLengths) {
            _binLengths = binLengths;
            initBin();
        }

        @Override
        int binSize (int currentBin) {
            if (currentBin < _binLengths.size())
                return _binLengths.get(currentBin);
            else
                return -1;
        }

        @Override
        boolean validBin (int currentBin) {
            return currentBin < _binLengths.size();
        }
    }

    @Override
    public TileData<List<T>> deserialize(TileIndex index, InputStream stream) throws IOException {

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
            if ("denseFlatArrayTile".equals(schemaName)) {
                storage = TileData.StorageType.Dense;
            } else {
                throw new IOException("Illegal tile for flattened dense array serializer - tile was not a flattened demse array.");
            }

            TileData<List<T>> newTile = null;

            switch (storage) {
                case Dense: {
                    GenericData.Array<Integer> binLengths = (GenericData.Array<Integer>) r.get("binLengths");

                    DenseBinTracker tracker;
                    if (1 == binLengths.size()) {
                        tracker = new ConstantSizeDenseBinTracker(binLengths.get(0), xBins*yBins);
                    } else {
                        tracker = new VariableSizeDenseBinTracker(binLengths);
                    }

                    tracker.parseBinValues(entries);

                    newTile = new DenseTileData<List<T>>(newTileIndex, tracker.getBinValues());
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

    private boolean isBinSizeConstant (List<List<T>> tileData) {
        int lastSizeSeen = -1;
        for (List<T> binData: tileData) {
            int binSize = 0;
            if (null != binData) binSize = binData.size();

            if (-1 == lastSizeSeen) {
                lastSizeSeen = binSize;
            } else if (lastSizeSeen != binSize) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void serialize (TileData<List<T>> tile, OutputStream stream) throws IOException {
        Schema recordSchema = getRecordSchema();
        Schema tileSchema = getTileSchema(TileData.StorageType.Dense);
        TileIndex idx = tile.getDefinition();

        GenericRecord tileRecord = new GenericData.Record(tileSchema);
        tileRecord.put("level", idx.getLevel());
        tileRecord.put("xIndex", idx.getX());
        tileRecord.put("yIndex", idx.getY());
        tileRecord.put("xBinCount", idx.getXBins());
        tileRecord.put("yBinCount", idx.getYBins());
        tileRecord.put("default", null);
        tileRecord.put("meta", getTileMetaData(tile));

        // Write bin size.
        List<List<T>> tileData = DenseTileData.getData(tile);
        List<Integer> binSizes = new ArrayList<>();
        if (isBinSizeConstant(tileData)) {
            // The bin size is constant; write out a single-element array of bin sizes.
            binSizes.add(tileData.get(0).size());
        } else {
            for (List<T> binData: tileData) {
                int size = 0;
                if (null != binData) size = binData.size();
                binSizes.add(size);
            }
        }

        tileRecord.put("binLengths", binSizes);

        List<GenericRecord> entries = new ArrayList<GenericRecord>();
        for (List<T> binData: tileData) {
            for (T value: binData) {
                GenericRecord bin = new GenericData.Record(recordSchema);
                setEntryValue(bin, value);
                entries.add(bin);
            }
        }
        tileRecord.put("values", entries);

        writeRecord(tileRecord, tileSchema, stream);
    }
}

