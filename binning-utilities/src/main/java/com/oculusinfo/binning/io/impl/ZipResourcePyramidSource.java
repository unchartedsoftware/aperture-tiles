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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.PyramidIOFactory;
import com.oculusinfo.factory.util.Pair;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * Extends the PyramidSource abstract class for zip based tiles.
 *  
 */
public class ZipResourcePyramidSource extends PyramidSourceStream {

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private ZipFile      _tileSetArchive;
	private String       _tileExtension;

	// We meed a global cache of zip stream sources - zip files are very slow to
	// read, so the source is slow to initialize, so creating a new one each
	// time we run isn't feasible.
	private static Map<Pair<String, String>, ZipResourcePyramidSource> _zipfileCache = new HashMap<>();
	
	static ZipResourcePyramidSource getZipSource (String rootpath, String extension) {
		Pair<String, String> key = new Pair<>(rootpath, extension);
		if (!_zipfileCache.containsKey(key)) {
			synchronized (_zipfileCache) {
				if (!_zipfileCache.containsKey(key)) {
					if (rootpath.startsWith("file://")) {
						rootpath = rootpath.substring(7);
					} else if (rootpath.startsWith("res://")) {
						rootpath = rootpath.substring(6);
						URL zipFile = PyramidIOFactory.class.getResource(rootpath);
						rootpath = zipFile.getFile();
					}

					ZipResourcePyramidSource source = new ZipResourcePyramidSource(rootpath, extension);
					_zipfileCache.put(key, source);
				}
			}
		}
		return _zipfileCache.get(key);
	}
	
	public ZipResourcePyramidSource (String zipFilePath, String tileExtension) {
		try {
			_tileSetArchive = new ZipFile(zipFilePath);
			_tileExtension = tileExtension;
		} catch (IOException e) {
			LOGGER.warn("Could not create zip file for " + zipFilePath, e);
		}
	}
	
	@Override
	protected InputStream getSourceTileStream (String basePath, TileIndex tile) throws IOException {
		String tileLocation = String.format("%s/"+PyramidIO.TILES_FOLDERNAME+"/%d/%d/%d." + _tileExtension, basePath, tile.getLevel(), tile.getX(), tile.getY());
		ZipArchiveEntry entry = _tileSetArchive.getEntry(tileLocation);
		return _tileSetArchive.getInputStream(entry);
	}

	@Override
	protected InputStream getSourceMetaDataStream (String basePath) throws IOException {
		String location = basePath+"/"+PyramidIO.METADATA_FILENAME;
		ZipArchiveEntry entry = _tileSetArchive.getEntry(location);
		return _tileSetArchive.getInputStream(entry);
	}
}
