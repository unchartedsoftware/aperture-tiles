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
import com.oculusinfo.binning.impl.MultiSliceTileView;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.util.Pair;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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



	private boolean         _doPyramidding;
	private HBaseTilePutter _putter;

	public HBaseSlicedPyramidIO (String zookeeperQuorum, String zookeeperPort, String hbaseMaster)
		throws IOException {
		super(zookeeperQuorum, zookeeperPort, hbaseMaster);
		setPyramidding(true);
	}

	@Override public HBaseTilePutter getPutter () {
		return _putter;
	}

	public void setPyramidding (boolean doPyramidding) {
		_doPyramidding = doPyramidding;
		_putter = new SlicedHBaseTilePutter(_doPyramidding);
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

	private <T> TileData<List<T>> compose (List<TileData<List<T>>> candidates,
										   int startIndex, int numComponents, int increment) {
		List<TileData<List<T>>> components = new ArrayList<>();
		for (int c=0; c<numComponents; ++c) {
			components.add((TileData) candidates.get(startIndex + c * increment));
		}
		return new MultiSliceTileView<T>(components);
	}

	@Override
	public <T> List<TileData<T>> readTiles (String tableName,
											TileSerializer<T> serializer,
											Iterable<TileIndex> tiles) throws IOException {
		Matcher m = SLICE_PATTERN.matcher(tableName);
		TypeDescriptor binType = serializer.getBinTypeDescription();
		if (List.class == binType.getMainType() && m.matches()) {
			String realName = m.group("table");
			HBaseColumn[] columns;

			int min = Integer.parseInt(m.group("min"));
			if (null == m.group("max")) {
				columns = new HBaseColumn[] { getSliceColumn(min, min) };
			} else {
				int max = Integer.parseInt(m.group("max"));
				List<Pair<Integer, Integer>> sliceRanges;
				if (_doPyramidding) {
					sliceRanges = decomposeRange(min, max);
				} else {
					sliceRanges = new ArrayList<>();
					for (int n=min; n<=max; ++n) {
						sliceRanges.add(new Pair<>(n, n));
					}
				}
				columns = new HBaseColumn[sliceRanges.size()];
				for (int i = 0; i < sliceRanges.size(); ++i) {
					Pair<Integer, Integer> sliceRange = sliceRanges.get(i);
					columns[i] = getSliceColumn(sliceRange.getFirst(), sliceRange.getSecond());
				}
			}
			List<TileData<T>> rawResults = super.readTiles(realName, serializer, tiles, columns);

			if (1 == columns.length) return rawResults;
			else {
				// Consolidate the columns from each tile.
				int numRaw = rawResults.size();
				int numReal = numRaw/columns.length;
				List<TileData<T>> realResults = new ArrayList<>(numReal);
				for (int i=0; i<numReal; ++i) {
					// We know this cast is correct because of our guard condition up top, that
					// List is the main type of T.
					realResults.add(compose((List) rawResults, i, columns.length, numReal));
				}
				return realResults;
			}
		} else {
			return super.readTiles(tableName, serializer, tiles);
		}
	}

	// Convert a number to its base 2 representation, as an array of 0's and 1's
	private static int[] numberToBits (int number) {
		// Count how many bits there are
		int numBits = 0;
		for (int n = number; n > 0; n = n >> 1) ++numBits;
		int[] bits = new int[numBits];
		for (int n=0; n<numBits; ++n) {
			bits[n] = 1 & (number >> n);
		}
		return bits;
	}

	/**
	 * Take a range of buckets, and convert it into the best set of requests for retrieving that set of buckets from a
	 * fully pyramidded bucket tile set.
	 *
	 * @param start The first bucket needed
	 * @param end The last bucket needed
	 * @return A series of pairs, each of which indicates a single stored bucket range to be retrieved, with its
	 *         start end end bucket number.
	 */
	public static List<Pair<Integer, Integer>> decomposeRange (int start, int end) {
		if (start > end) return decomposeRange(end, start);

		// Get the most significant bit at which the endpoints differ.
		int differences = start ^ (end+1); // xor
		int msb = 1;
		while ((msb << 1) <= differences) msb = msb << 1;
		int midPoint = start - (start % msb) + msb;

		int[] startRanges = numberToBits(midPoint - start);;
		int[] endRanges = numberToBits(end + 1 - midPoint);

		// Construct our ranges.
		List<Pair<Integer, Integer>> ranges = new ArrayList<>();
		int curRangeMin = start;

		// Ranges before our midpoint go from smallest to largest
		for (int i=0; i<startRanges.length; ++i) {
			if (1 == startRanges[i]) {
				int curRangeSize = 1 << i;
				ranges.add(new Pair<Integer, Integer>(curRangeMin, curRangeMin+curRangeSize-1));
				curRangeMin += curRangeSize;
			}
		}
		// Ranges after our midpoint go from largest to smallest
		for (int i=endRanges.length-1; i>=0; --i) {
			if (1 == endRanges[i]) {
				int curRangeSize = 1 << i;
				ranges.add(new Pair<Integer, Integer>(curRangeMin, curRangeMin+curRangeSize-1));
				curRangeMin += curRangeSize;
			}
		}

		return ranges;
	}

	public static class SlicedHBaseTilePutter extends StandardHBaseTilePutter {
		private boolean _doPyramidding;
		public SlicedHBaseTilePutter (boolean doPyramidding) {
			_doPyramidding = doPyramidding;
		}

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
			// Store the whole thing pyramidded.
			int slicesPerWrite = 1;
			while (slicesPerWrite < slices) {
				// Divide the tile into slices, storing each of them individually in their own column
				for (int startSlice = 0; startSlice < slices; startSlice = startSlice + slicesPerWrite) {
					int endSlice = startSlice + slicesPerWrite - 1;
					TileData<List<T>> slice = new DenseTileMultiSliceView<T>(tile, startSlice, endSlice).harden();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					serializer.serialize(slice, baos);
					existingPut = addToPut(existingPut, rowIdFromTileIndex(tile.getDefinition()),
						getSliceColumn(startSlice, endSlice), baos.toByteArray());
				}

				// If not pyramidding, bail out after our first time through.
				if (_doPyramidding) slicesPerWrite = slicesPerWrite * 2;
				else slicesPerWrite = slices;
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
