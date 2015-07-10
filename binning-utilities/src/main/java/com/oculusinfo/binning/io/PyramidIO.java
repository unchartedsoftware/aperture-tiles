/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
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
package com.oculusinfo.binning.io;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.json.JSONObject;


/**
 * A PyramidIO is a class that can read and write tile pyramids.
 *
 * @author nkronenfeld
 */
public interface PyramidIO {
	// TODO: Move these out of here.
	public final static String METADATA_FILENAME = "metadata.json";
	public final static String TILES_FOLDERNAME  = "tiles";



	/**
	 * Initialize the system for writing a pyramid
	 *
	 * @param pyramidId The ID of the pyramid to be written; the meaning of this
	 *            ID is dependent on the I/O type
	 */
	public void initializeForWrite (String pyramidId) throws IOException;

	/**
	 * Write a set of tiles out
	 *
	 * @param pyramidId The ID of the pyramid to be written; the meaning of this
	 *            ID is dependent on the I/O type
	 * @param serializer A serializer class that defines how the specific data
	 *            format will be written
	 * @param data The data to be written
	 */
	public <T> void writeTiles (String pyramidId,
	                            TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException;

	/**
	 * Writes out new metadata for this tile set
	 *
	 * @param pyramidId The ID of the pyramid to be read; the meaning of this ID
	 *            is dependent on the I/O type
	 * @param metaData The metadata to be written
	 */
	public void writeMetaData (String pyramidId, String metaData) throws IOException;

	/**
	 * Initialize a dataset for reading. This rarely has to do anything, but in
	 * live tile generation, must describe the dataset pretty completely.
	 *
	 * @param pyramidId The ID of the data set to be read
	 * @param width The desired number of bins per tile along the X axis. Some
	 *            implementations will need this information (if they are
	 *            generating tiles), and will generate an error if an invalid
	 *            number is given; others, returning pregenerated tiles, will
	 *            ignore this number entirely.
	 * @param height The desired number of bins per tile along the Y axis.
	 * @param dataDescription A description of the pyramid to be read; if tiles
	 *            are pregenerated, this is likely to be ignored.
	 */
	public void initializeForRead (String pyramidId, int width, int height, Properties dataDescription);

	/**
	 * Read in a set of tiles
	 *
	 * @param pyramidId The ID of the pyramid to be read; the meaning of this ID
	 *            is dependent on the I/O type
	 * @param serializer A serializaer class that defines how the specific data
	 *            format will be read
	 * @return A list of tiles
	 */
	public <T> List<TileData<T>> readTiles (String pyramidId, TileSerializer<T> serializer,
	                                        Iterable<TileIndex> tiles, JSONObject properties ) throws IOException;

	/**
	 * Read in a set of tiles
	 *
	 * @param pyramidId The ID of the pyramid to be read; the meaning of this ID
	 *            is dependent on the I/O type
	 * @param serializer A serializaer class that defines how the specific data
	 *            format will be read
	 * @return A list of tiles
	 */
	public <T> List<TileData<T>> readTiles (String pyramidId, TileSerializer<T> serializer,
											Iterable<TileIndex> tiles ) throws IOException;

	/**
	 * Get a stream containing the raw data for one tile.
	 *
	 * @param pyramidId The ID of the pyramid to be read; the meaning of this ID
	 *            is dependent on the I/O type
	 * @param serializer A serializaer class that defines how the specific data
	 *            format will be read
	 * @param tile The coordinates of the tile to fetch
	 * @return A data stream streaming the raw data of the indicated tile, or
	 *         null if the indicated tile is not found
	 * @throws IOException
	 */
	public <T> InputStream getTileStream (String pyramidId,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException;

	/**
	 * Gets the metadata for this tile set
	 *
	 * @param pyramidId An ID of the pyramid to be read; the use of this ID is
	 *            dependent on the I/O type
	 */
	public String readMetaData (String pyramidId) throws IOException;



	public void removeTiles (String id, Iterable<TileIndex> tiles ) throws IOException;



}
