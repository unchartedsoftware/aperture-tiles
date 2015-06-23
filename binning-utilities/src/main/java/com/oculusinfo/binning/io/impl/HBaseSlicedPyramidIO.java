/*
 * Copyright (c) 2015 Uncharted Software Inc. http://www.uncharted.software/
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
package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileMultiSliceView;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This version of the HBasePyramidIO is specialized for bucketted tiles; it will take a tile whose bins are
 * lists (of buckets) of something - doesn't matter what - and instead of storing the tile as a monolithic tile
 * in the tileData column, as HBasePyramidIO does, it will store the contents in separate columns.
 */
public class HBaseSlicedPyramidIO extends HBasePyramidIO {
	private static final Pattern SLICE_PATTERN = Pattern.compile("(?<table>.*)\\[(?<min>[0-9]+)(?>-(?<max>[0-9]+))?\\]");

	private HBaseTilePutter _putter;
	public HBaseSlicedPyramidIO (String zookeeperQuorum, String zookeeperPort, String hbaseMaster)
		throws IOException {
		super(zookeeperQuorum, zookeeperPort, hbaseMaster);
		_putter = new SlicedHBaseTilePutter();
	}

	@Override public HBaseTilePutter getPutter () {
		return _putter;
	}

	public static HBaseColumn getSliceColumn (int minSlice, int maxSlice) {
		String qualifier;
		if (minSlice == maxSlice) {
			qualifier = ""+minSlice;
		} else {
			qualifier = ""+minSlice+"-"+maxSlice;
		}
		return new HBaseColumn(TILE_FAMILY_NAME, qualifier.getBytes());
	}

	@Override
	public <T> List<TileData<T>> readTiles (String tableName,
											TileSerializer<T> serializer,
											Iterable<TileIndex> tiles) throws IOException {
		Matcher m = SLICE_PATTERN.matcher(tableName);
		if (m.matches()) {
			String realName = m.group("table");
			HBaseColumn c;
			int min = Integer.parseInt(m.group("min"));
			if (null == m.group("max")) {
				c = getSliceColumn(min, min);
			} else {
				int max = Integer.parseInt(m.group("max"));
				c = getSliceColumn(min, max);
			}
			return super.readTiles(realName, serializer, tiles, c);
		} else {
			return super.readTiles(tableName, serializer, tiles);
		}
	}



	static class SlicedHBaseTilePutter extends StandardHBaseTilePutter {
		@Override
		public <T> Put getPutForTile(TileData<T> tile, TileSerializer<T> serializer) throws IOException {
			TypeDescriptor binType = serializer.getBinTypeDescription();

			Put put = super.getPutForTile(tile, serializer);
			if (List.class == binType.getMainType()) {
				put = addSlices(put, (TileSerializer) serializer, (TileData) tile);
			}

			return put;
		}

		private <T> Put addSlices (Put existingPut,
								   TileSerializer<List<T>> serializer,
								   TileData<List<T>> tile) throws IOException {
			// Figure out into how many slices to divide the data
			int slices = numSlices(tile);
			// Divide the tile into slices, storing each of them individually in their own column
			for (int s = 0; s < slices; ++s) {
				TileData<List<T>> slice = new DenseTileMultiSliceView<T>(tile, s, s);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				serializer.serialize(slice, baos);
				existingPut = addToPut(existingPut, rowIdFromTileIndex(tile.getDefinition()),
					getSliceColumn(s, s), baos.toByteArray());
			}
			return existingPut;
		}

		private int numSlices (TileData<?> tile) {
			int slices = 0;
			TileIndex index = tile.getDefinition();
			for (int x=0; x < index.getXBins(); ++x) {
				for (int y = 0; y < index.getYBins(); ++y) {
					try {
						List<?> bin = (List<?>) tile.getBin(x, y);
						int size = bin.size();
						if (size > slices) slices = size;
					} catch (ClassCastException|NullPointerException e) {
						// Swallow it, we don't care here.
					}
				}
			}
			return slices;
		}
	}
}
