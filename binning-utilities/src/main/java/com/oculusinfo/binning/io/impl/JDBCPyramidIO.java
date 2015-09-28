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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.json.JSONObject;

/**
 * JDBC-based implementation of PyramidIO.
 *
 * @author rcameron
 *
 */
public class JDBCPyramidIO implements PyramidIO {
	private static final String TABLE_METADATA = "metadata";
	private static final int BATCH_SIZE = 10000;
	private static final String COL_ZOOM_LVL = "zoom_level";
	private static final String COL_TILE_COLUMN = "tile_column";
	private static final String COL_TILE_ROW = "tile_row";
	private static final String COL_TILE_DATA = "tile_data";
	private static final String COL_METADATA = "metadata";
	private static final String COL_PYRAMID_ID = "pyramid_id";

	private Connection _connection;

	public JDBCPyramidIO(String driverClassName, String dbUrl) throws ClassNotFoundException, SQLException {
		Class.forName(driverClassName);
		_connection = DriverManager.getConnection(dbUrl);
	}

	public void shutdown() {
		try {
			_connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initializeForWrite(String pyramidId) throws IOException {
		// Create the table and columns if necessary.
		Statement stmt = null;
		try {
			if (!tableExists(pyramidId)) {
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE ");
				sb.append(toTableName(pyramidId));
				sb.append(" (");
				sb.append(COL_ZOOM_LVL);
				sb.append(" INTEGER NOT NULL, ");
				sb.append(COL_TILE_COLUMN);
				sb.append(" INTEGER NOT NULL, ");
				sb.append(COL_TILE_ROW);
				sb.append(" INTEGER NOT NULL, ");
				sb.append(COL_TILE_DATA);
				sb.append(" BLOB,");
				sb.append(" CONSTRAINT pk_TileIndex PRIMARY KEY (");
				sb.append(COL_ZOOM_LVL);
				sb.append(",");
				sb.append(COL_TILE_COLUMN);
				sb.append(",");
				sb.append(COL_TILE_ROW);
				sb.append("))");

				stmt = _connection.createStatement();
				stmt.executeUpdate(sb.toString());
			}

			if (!tableExists(TABLE_METADATA)) {
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE ");
				sb.append(TABLE_METADATA);
				sb.append(" (");
				sb.append(COL_PYRAMID_ID);
				sb.append(" TEXT PRIMARY KEY, metadata TEXT)");

				if (stmt == null) stmt = _connection.createStatement();
				stmt.executeUpdate(sb.toString());
			}
		} catch (Exception e) {
			throw new IOException("Error initializing for write: ", e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// We wrap pyramid ids in [] so we can use a wider range of characters in
	// pyramid IDs.
	private String toTableName(String pyramidId) {
		StringBuffer sb = new StringBuffer("\"");
		sb.append(pyramidId).append("\"");
		return sb.toString();
	}

	protected boolean tableExists(String pyramidId) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT name FROM 'sqlite_master' WHERE type='table' AND name=");
		sb.append(toTableName(pyramidId));

		Statement stmt = null;
		boolean exists = false;

		try {
			stmt = _connection.createStatement();
			stmt.execute(sb.toString());
			exists = stmt.getResultSet().next();
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}

		return exists;
	}

	@Override
	public <T> void writeTiles(String pyramidId,
	                           TileSerializer<T> serializer, Iterable<TileData<T>> data)
		throws IOException {
		PreparedStatement ps = null;

		try {
			_connection.setAutoCommit(false);

			StringBuilder sb = new StringBuilder();
			sb.append("INSERT INTO ");
			sb.append(toTableName(pyramidId));
			sb.append(" (");
			sb.append(COL_ZOOM_LVL);
			sb.append(",");
			sb.append(COL_TILE_COLUMN);
			sb.append(",");
			sb.append(COL_TILE_ROW);
			sb.append(",");
			sb.append(COL_TILE_DATA);
			sb.append(") ");
			sb.append("VALUES (?,?,?,?)");
			ps = _connection.prepareStatement(sb.toString());

			int count = 0;
			for (TileData<T> tile : data) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				serializer.serialize(tile, baos);

				TileIndex index = tile.getDefinition();

				ps.setInt(1, index.getLevel());
				ps.setInt(2, index.getX());
				ps.setInt(3, index.getY());
				ps.setBytes(4, baos.toByteArray());

				ps.addBatch();

				++count;
				if (count % BATCH_SIZE == 0) {
					ps.executeBatch();
				}
			}

			ps.executeBatch();
			_connection.commit();
		} catch (Exception e) {
			throw new IOException("Error writing tiles.", e);
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
				_connection.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	protected boolean metaDataExistsFor(String pyramidId) {
		String metadata = null;
		try {
			metadata = readMetaData(pyramidId);
		} catch (IOException e) {
			metadata = null;
		}
		return metadata != null;
	}

	@Override
	public void writeMetaData(String pyramidId, String metaData)
		throws IOException {
		Statement stmt = null;
		try {
			StringBuilder sb = new StringBuilder();
			if (metaDataExistsFor(pyramidId)) {
				sb.append("UPDATE ");
				sb.append(TABLE_METADATA);
				sb.append(" SET ");
				sb.append(COL_METADATA);
				sb.append(" = '");
				sb.append(metaData);
				sb.append("' WHERE ");
				sb.append(COL_PYRAMID_ID);
				sb.append(" = '");
				sb.append(toTableName(pyramidId));
				sb.append("';");
			}
			else {
				sb.append("INSERT INTO ");
				sb.append(TABLE_METADATA);
				sb.append(" (");
				sb.append(COL_PYRAMID_ID);
				sb.append(", ");
				sb.append(COL_METADATA);
				sb.append(") VALUES('");
				sb.append(toTableName(pyramidId));
				sb.append("','");
				sb.append(metaData);
				sb.append("')");
			}

			stmt = _connection.createStatement();
			stmt.execute(sb.toString());
		} catch (SQLException e) {
			throw new IOException("Error writing metadata.", e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {
		// Noop
	}

	@Override
	public <T> List<TileData<T>> readTiles(String pyramidId,
	                                       TileSerializer<T> serializer, Iterable<TileIndex> tiles)
		throws IOException {
		PreparedStatement ps = null;
		try {
			if (!tableExists(pyramidId)) {
				// TODO: Right thing to return when the table doesn't exist?
				return null;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT ");
			sb.append(COL_TILE_DATA);
			sb.append(" FROM ");
			sb.append(toTableName(pyramidId));
			sb.append(" WHERE ");
			sb.append(COL_ZOOM_LVL);
			sb.append(" = ? AND ");
			sb.append(COL_TILE_COLUMN);
			sb.append(" = ? AND ");
			sb.append(COL_TILE_ROW);
			sb.append(" = ?");

			ps = _connection.prepareStatement(sb.toString());

			List<TileData<T>> results = new LinkedList<TileData<T>>();
			for (TileIndex tile : tiles) {
				ps.setInt(1, tile.getLevel());
				ps.setInt(2, tile.getX());
				ps.setInt(3, tile.getY());

				ResultSet resultSet = ps.executeQuery();
				if (!resultSet.next())
					continue;

				byte[] tileBytes = resultSet.getBytes(COL_TILE_DATA);

				TileData<T> data = serializer.deserialize(tile,
				                                          new ByteArrayInputStream(tileBytes));
				results.add(data);
			}
			return results;

		} catch (Exception e) {
			throw new IOException("Error reading tiles.", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					throw new IOException(e);
				}
			}
		}
	}

	@Override
	public <T> List<TileData<T>> readTiles (String pyramidId,
											TileSerializer<T> serializer,
											Iterable<TileIndex> tiles,
											JSONObject properties ) throws IOException {
		return readTiles( pyramidId, serializer, tiles );
	}

	@Override
	public <T> InputStream getTileStream (String pyramidId,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException {
		PreparedStatement ps = null;
		try {
			if (!tableExists(pyramidId)) {
				// TODO: Right thing to return when the table doesn't exist?
				return null;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT ");
			sb.append(COL_TILE_DATA);
			sb.append(" FROM ");
			sb.append(toTableName(pyramidId));
			sb.append(" WHERE ");
			sb.append(COL_ZOOM_LVL);
			sb.append(" = ? AND ");
			sb.append(COL_TILE_COLUMN);
			sb.append(" = ? AND ");
			sb.append(COL_TILE_ROW);
			sb.append(" = ?");

			ps = _connection.prepareStatement(sb.toString());
			ps.setInt(1, tile.getLevel());
			ps.setInt(2, tile.getX());
			ps.setInt(3, tile.getY());

			ResultSet resultSet = ps.executeQuery();
			if (resultSet.next()) {
				byte[] tileBytes = resultSet.getBytes(COL_TILE_DATA);
				return new ByteArrayInputStream(tileBytes);
			}
		} catch (Exception e) {
			throw new IOException("Error reading tiles.", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					throw new IOException(e);
				}
			}
		}
		return null;
	}

	@Override
	public String readMetaData(String pyramidId) throws IOException {
		Statement stmt = null;
		try {
			if (!tableExists(TABLE_METADATA)) {
				return null;
			}

			StringBuilder sb = new StringBuilder();
			sb.append("SELECT ");
			sb.append(COL_METADATA);
			sb.append(" FROM ");
			sb.append(TABLE_METADATA);
			sb.append(" WHERE ");
			sb.append(COL_PYRAMID_ID);
			sb.append(" = '");
			sb.append(toTableName(pyramidId));
			sb.append("'");

			stmt = _connection.createStatement();
			ResultSet resultSet = stmt.executeQuery(sb
			                                        .toString());
			if (!resultSet.next())
				return null;

			return resultSet.getString(TABLE_METADATA);
		} catch (SQLException e) {
			throw new IOException("Error reading tiles.", e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void removeTiles (String id, Iterable<TileIndex> tiles ) throws IOException {
		throw new IOException("removeTiles not currently supported for JDBCPyramidIO");
	}

}
