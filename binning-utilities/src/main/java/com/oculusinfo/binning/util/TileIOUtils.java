/*
 * Copyright (c) 2015 Uncharted Software. http://www.uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.SubTileDataView;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

/**
 * Utility class for reading tile data
 */
public class TileIOUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(TileIOUtils.class);

	/**
	 * Fetch a desired tile
	 *
	 * @param index The TileIndex of the tile to fetch
	 * @param dataId The id of the tile set
	 * @param serializer The TileSerializer for tile source
	 * @param pyramidIO The PyramidIO used to fetch the tiles
	 * @param coarseness The coarseness/resolution of the fetched bins
	 * @param tileProperties Additional tile properties used to read the tile
	 *
	 * @return TileData object for the requested TileIndex
	 */
	public static <T> TileData<T> tileDataForIndex(TileIndex index, String dataId, TileSerializer<T> serializer, PyramidIO pyramidIO, int coarseness, JSONObject tileProperties) throws IOException {
		TileData<T> data = null;
		if ( coarseness > 1 ) {
			int coarsenessFactor = ( int ) Math.pow( 2, coarseness - 1 );

			// Coarseness support:
			// Find the appropriate tile data for the given level and coarseness
			java.util.List<TileData<T>> tileDatas = null;
			TileIndex scaleLevelIndex = null;

			// need to get the tile data for the level of the base level minus the coarseness
			for ( int coarsenessLevel = coarseness - 1; coarsenessLevel >= 0; --coarsenessLevel ) {
				scaleLevelIndex = new TileIndex( index.getLevel() - coarsenessLevel,
					( int ) Math.floor( index.getX() / coarsenessFactor ),
					( int ) Math.floor( index.getY() / coarsenessFactor ) );

				tileDatas = pyramidIO.readTiles( dataId, serializer, Collections.singleton(scaleLevelIndex), tileProperties );

				if ( tileDatas.size() >= 1 ) {
					//we got data for this level so use it
					break;
				}
			}

			// Missing tiles are commonplace and we didn't find any data up the tree either.  We don't want a big long error for that.
			if ( tileDatas.size() < 1 ) {
				LOGGER.info( "Missing tile " + index + " for layer data id " + dataId );
				return null;
			}

			// We're using a scaled tile so wrap in a view class that will make the source data look like original tile we're looking for
			data = SubTileDataView.fromSourceAbsolute(tileDatas.get(0), index);
		} else {
			// No coarseness - use requested tile
			java.util.List<TileData<T>> tileDatas;

			tileDatas = pyramidIO.readTiles( dataId, serializer, Collections.singleton( index ), tileProperties );

			if ( !tileDatas.isEmpty() ) {
				data = tileDatas.get( 0 );
			}
		}

		return data;
	}
}
