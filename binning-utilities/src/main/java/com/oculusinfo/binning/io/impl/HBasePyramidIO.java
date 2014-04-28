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
package com.oculusinfo.binning.io.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Delete;
//import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Row;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;

public class HBasePyramidIO implements PyramidIO {
	private static final String META_DATA_INDEX      = "metadata";

	public static class HBaseColumn {
		byte[] family;
		byte[] qualifier;
		HBaseColumn (byte[] family, byte[] qualifier) {
			this.family = family;
			this.qualifier = qualifier;
		}
		public byte[] getFamily () {return family;}
		public byte[] getQualifier () {return qualifier;}
	}



	private static final byte[]      EMPTY_BYTES          = new byte[0];
	private static final byte[]      TILE_FAMILY_NAME     = "tileData".getBytes();
	public static final HBaseColumn  TILE_COLUMN          = new HBaseColumn(TILE_FAMILY_NAME, EMPTY_BYTES);
	private static final byte[]      METADATA_FAMILY_NAME = "metaData".getBytes();
	public static final HBaseColumn  METADATA_COLUMN      = new HBaseColumn(METADATA_FAMILY_NAME, EMPTY_BYTES);


	private Configuration       _config;
	private HBaseAdmin          _admin;

	public HBasePyramidIO (String zookeeperQuorum, String zookeeperPort, String hbaseMaster)
		throws IOException {
		_config = HBaseConfiguration.create();
		_config.set("hbase.zookeeper.quorum", zookeeperQuorum);
		_config.set("hbase.zookeeper.property.clientPort", zookeeperPort);
		_config.set("hbase.master", hbaseMaster);
		_admin = new HBaseAdmin(_config);
	}



	/**
	 * Determine the row ID we use in HBase for a given tile index
	 */
	public static String rowIdFromTileIndex (TileIndex tile) {
		// Use the minimum possible number of digits for the tile key
		int digits = (int) Math.floor(Math.log10(1 << tile.getLevel()))+1;
		return String.format("%02d,%0"+digits+"d,%0"+digits+"d",
		                     tile.getLevel(), tile.getX(), tile.getY());
	}

	/**
	 * Determine tile index given a row id
	 */
	public static TileIndex tileIndexFromRowId (String rowId) {
		String[] fields = rowId.split(",");
		return new TileIndex(Integer.parseInt(fields[0]),
		                     Integer.parseInt(fields[1]),
		                     Integer.parseInt(fields[2]));
	}

	/**
	 * Get the configuration used to connect to HBase.
	 */
	public Configuration getConfiguration () {
		return _config;
	}


	/*
	 * Gets an existing table (without creating it)
	 */
	private HTable getTable (String tableName) throws IOException {
		return new HTable(_config, tableName);
	}

	/*
	 * Given a put request (a request to put data into a table), add a single
	 * entry into the request
	 * 
	 * @param existingPut
	 *            The existing request. If null, a request will be created for
	 *            the given row. If non-null, no check will be performed to make
	 *            sure the put request is for the right row - this is the
	 *            responsibility of the caller.
	 * @param rowId
	 *            The id of the row to put. This is only used if the existingPut
	 *            is null.
	 * @param column
	 *            The column defining the entry in this row into which to put
	 *            the data
	 * @param data
	 *            the data to put into the described entry.
	 * @return The put request - the same as is passed in, or a new request if
	 *         none was passed in.
	 */
	private Put addToPut (Put existingPut, String rowId, HBaseColumn column, byte[] data) {
		if (null == existingPut) {
			existingPut = new Put(rowId.getBytes());
		}

		existingPut.add(column.family, column.qualifier, data);

		return existingPut;
	}

	/*
	 * Write a series of rows out to the given table
	 * 
	 * @param table
	 *            The table to which to write
	 * @param rows
	 *            The rows to write
	 */
	private void writeRows (String tableName, List<Row> rows) throws InterruptedException, IOException {
		HTable table = getTable(tableName);
		table.batch(rows);
		table.flushCommits();
		table.close();
	}

	private Map<HBaseColumn, byte[]> decodeRawResult (Result row, HBaseColumn[] columns) {
		Map<HBaseColumn, byte[]> results = null;
		for (HBaseColumn column: columns) {
			if (row.containsColumn(column.family, column.qualifier)) {
				if (null == results) results = new HashMap<HBaseColumn, byte[]>(); 
				results.put(column, row.getValue(column.family, column.qualifier));
			}
		}
		return results;
	}

	/*
	 * Read several rows of data.
	 * 
	 * @param table
	 *            The table to read
	 * @param rows
	 *            The rows to read
	 * @param columns
	 *            The columns to read
	 * @return A list, in the same order as the input rows of maps from column
	 *         id to value. Columns missing from the data are also missing from
	 *         the map. Rows which returned no data have a null instead of a
	 *         map.
	 */
	private List<Map<HBaseColumn, byte[]>> readRows (String tableName, List<String> rows, HBaseColumn... columns) throws IOException {
		HTable table = getTable(tableName);

		List<Get> gets = new ArrayList<Get>(rows.size());
		for (String rowId: rows) {
			Get get = new Get(rowId.getBytes());
			for (HBaseColumn column: columns) {
				get.addColumn(column.family, column.qualifier);
			}
			gets.add(get);
		}

		Result[] results = table.get(gets);
		List<Map<HBaseColumn, byte[]>> allResults = new ArrayList<Map<HBaseColumn,byte[]>>(rows.size());
		for (Result result: results) {
			allResults.add(decodeRawResult(result, columns));
		}
		return allResults;
	}



	@Override
	public void initializeForWrite (String tableName) throws IOException {
		if (!_admin.tableExists(tableName)) {
			//            HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));
			HTableDescriptor tableDesc = new HTableDescriptor(tableName);
            
			HColumnDescriptor metadataFamily = new HColumnDescriptor(METADATA_FAMILY_NAME);
			tableDesc.addFamily(metadataFamily);
			HColumnDescriptor tileFamily = new HColumnDescriptor(TILE_FAMILY_NAME);
			tableDesc.addFamily(tileFamily);
			_admin.createTable(tableDesc);
		}
	}

	@Override
	public <T> void writeTiles (String tableName, TilePyramid tilePyramid,  TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException {
		List<Row> rows = new ArrayList<Row>();
		for (TileData<T> tile: data) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.serialize(tile, tilePyramid, baos);

			rows.add(addToPut(null, rowIdFromTileIndex(tile.getDefinition()),
			                  TILE_COLUMN, baos.toByteArray()));
		}
		try {
			writeRows(tableName, rows);
		} catch (InterruptedException e) {
			throw new IOException("Error writing tiles to HBase", e);
		}
	}

	@Override
	public void writeMetaData (String tableName, String metaData) throws IOException {
		try {
			List<Row> rows = new ArrayList<Row>();
			rows.add(addToPut(null, META_DATA_INDEX, METADATA_COLUMN, metaData.getBytes()));
			Put put = new Put(META_DATA_INDEX.getBytes());
			put.add(METADATA_FAMILY_NAME, EMPTY_BYTES, metaData.getBytes());
			writeRows(tableName, rows);
		} catch (InterruptedException e) {
			throw new IOException("Error writing metadata to HBase", e);
		}
	}

	@Override
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {
		try {
    		initializeForWrite( pyramidId );
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
	}

	@Override
	public <T> List<TileData<T>> readTiles (String tableName,
	                                        TileSerializer<T> serializer,
	                                        Iterable<TileIndex> tiles) throws IOException {
		List<String> rowIds = new ArrayList<String>();
		for (TileIndex tile: tiles) {
			rowIds.add(rowIdFromTileIndex(tile));
		}
        
		List<Map<HBaseColumn, byte[]>> rawResults = readRows(tableName, rowIds, TILE_COLUMN);

		List<TileData<T>> results = new LinkedList<TileData<T>>();

		Iterator<Map<HBaseColumn, byte[]>> iData = rawResults.iterator();
		Iterator<TileIndex> indexIterator = tiles.iterator();

		while (iData.hasNext()) {
			Map<HBaseColumn, byte[]> rawResult = iData.next();
			TileIndex index = indexIterator.next();
			if (null != rawResult) {
				byte[] rawData = rawResult.get(TILE_COLUMN);
				ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
				TileData<T> data = serializer.deserialize(index, bais);
				results.add(data);
			}
		}

		return results;
	}

	@Override
	public <T> InputStream getTileStream (String tableName,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException {
		List<String> rowIds = new ArrayList<String>();
		rowIds.add(rowIdFromTileIndex(tile));
        
		List<Map<HBaseColumn, byte[]>> rawResults = readRows(tableName, rowIds, TILE_COLUMN);
		Iterator<Map<HBaseColumn, byte[]>> iData = rawResults.iterator();

		if (iData.hasNext()) {
			Map<HBaseColumn, byte[]> rawResult = iData.next();
			if (null != rawResult) {
				byte[] rawData = rawResult.get(TILE_COLUMN);
				return new ByteArrayInputStream(rawData);
			}
		}

		return null;
	}

	@Override
	public String readMetaData (String tableName) throws IOException {
		List<Map<HBaseColumn, byte[]>> rawData = readRows(tableName, Collections.singletonList(META_DATA_INDEX), METADATA_COLUMN);

		if (null == rawData) return null;
		if (rawData.isEmpty()) return null;
		if (null == rawData.get(0)) return null;
		if (!rawData.get(0).containsKey(METADATA_COLUMN)) return null;

		return new String(rawData.get(0).get(METADATA_COLUMN));
	}
	
	@Override
    public void removeTiles (String tableName, Iterable<TileIndex> tiles) throws IOException {
    	
    	List<String> rowIds = new ArrayList<>();
        for (TileIndex tile: tiles) {
            rowIds.add( rowIdFromTileIndex( tile ) );
        }        
        deleteRows(tableName, rowIds, TILE_COLUMN);
    }
	
	private void deleteRows (String tableName, List<String> rows, HBaseColumn... columns) throws IOException {
        
    	HTable table = getTable(tableName);
        List<Delete> deletes = new LinkedList<Delete>();
        for (String rowId: rows) {
        	Delete delete = new Delete(rowId.getBytes());
            deletes.add(delete);
        }
        table.delete(deletes);
    }
	
	public void dropTable( String tableName ) {
    	
    	try {
    		_admin.disableTable( /*TableName.valueOf(*/ tableName /*)*/ );
    		_admin.deleteTable( /*TableName.valueOf(*/ tableName /*)*/ );
        } catch (Exception e) {}
 	
    }
}
