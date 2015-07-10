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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.json.JSONObject;


/**
 * Extends the PyramidSource abstract class for file system (directory) based tiles.
 *
 */
public class FileSystemPyramidSource implements PyramidSource {

	private String _rootPath;
	private String _extension;


	public FileSystemPyramidSource (String rootPath, String extension){
		//if there's no root path, then it should be based on a relative path, so make sure to set root path to '.'
		if (rootPath == null || rootPath.trim().length() == 0) {
			rootPath = "./";
		}

		//make sure the root path ends with a slash
		_rootPath = (rootPath.trim().endsWith("/"))? rootPath : rootPath.trim() + "/";
		_extension = extension;
	}

	@Override
	public void initializeForWrite (String basePath) throws IOException {
		File metaDataFile = getMetaDataFile(basePath);
		File parent = metaDataFile.getParentFile();
		if (!parent.exists()) parent.mkdirs();
	}

	@Override
	public <T> void writeTiles (String basePath, TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException {
		for (TileData<T> tile: data) {
			File tileFile = getTileFile(basePath, tile.getDefinition());
			File parent = tileFile.getParentFile();
			if (!parent.exists()) parent.mkdirs();

			FileOutputStream fileStream = new FileOutputStream(tileFile);
			serializer.serialize(tile, fileStream);
			fileStream.close();
		}
	}

	@Override
	public void writeMetaData (String basePath, String metaData) throws IOException {
		FileOutputStream stream = new FileOutputStream(getMetaDataFile(basePath));
		stream.write(metaData.getBytes());
		stream.close();
	}


	@Override
	public <T> List<TileData<T>> readTiles (String basePath,
	                                        TileSerializer<T> serializer,
	                                        Iterable<TileIndex> tiles) throws IOException {
		List<TileData<T>> results = new LinkedList<TileData<T>>();
		for (TileIndex tile: tiles) {
			File tileFile = getTileFile(basePath, tile);

			if (tileFile.exists() && tileFile.isFile()) {
				FileInputStream stream = new FileInputStream(tileFile);
				TileData<T> data = serializer.deserialize(tile, stream);
				results.add(data);
				stream.close();
			}
		}
		return results;
	}

	@Override
	public <T> InputStream getTileStream (String basePath,
	                                      TileSerializer<T> serializer,
	                                      TileIndex tile) throws IOException {
		File tileFile = getTileFile(basePath, tile);

		if (tileFile.exists() && tileFile.isFile()) {
			return new FileInputStream(tileFile);
		} else {
			return null;
		}
	}

	@Override
	public String readMetaData (String basePath) throws IOException {
		File metaDataFile = getMetaDataFile(basePath);
		if (!metaDataFile.exists()) return null;

		FileInputStream stream = new FileInputStream(metaDataFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String rawMetaData = "";
		String line;
		while (null != (line = reader.readLine())) {
			rawMetaData = rawMetaData + line;
		}
		reader.close();
		return rawMetaData;
	}

	@Override
	public void removeTiles (String basePath, Iterable<TileIndex> tiles ) throws IOException {
		for (TileIndex tile: tiles) {
			// delete tile
			File tileFile = getTileFile(basePath, tile);
			tileFile.delete();
			// if x directory is empty, delete it as well
			File xDir = getXDir(basePath, tile);
			if ( xDir.isDirectory() && xDir.list().length == 0 ) {
				xDir.delete();
			}
			// if level directory is empty, delete it as well
			File levelDir = getLevelDir(basePath, tile);
			if ( levelDir.isDirectory() && levelDir.list().length == 0 ) {
				levelDir.delete();
			}

		}
	}

	@Override
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {
		// Not Implemented
	}

	private File getLevelDir (String basePath, TileIndex tile) {
		return new File(String.format("%s/" + PyramidIO.TILES_FOLDERNAME
		                              + "/%d/",
		                              _rootPath + basePath,
		                              tile.getLevel() ));
	}

	private File getXDir (String basePath, TileIndex tile) {
		return new File(String.format("%s/" + PyramidIO.TILES_FOLDERNAME
		                              + "/%d/%d/",
		                              _rootPath + basePath,
		                              tile.getLevel(), tile.getX() ));
	}

	private File getTileFile (String basePath, TileIndex tile) {
		return new File(String.format("%s/" + PyramidIO.TILES_FOLDERNAME
		                              + "/%d/%d/%d." + _extension,
		                              _rootPath + basePath,
		                              tile.getLevel(), tile.getX(), tile.getY()));
	}

	private File getMetaDataFile (String basePath) {
		return new File(_rootPath + basePath+"/"+PyramidIO.METADATA_FILENAME);
	}

}
