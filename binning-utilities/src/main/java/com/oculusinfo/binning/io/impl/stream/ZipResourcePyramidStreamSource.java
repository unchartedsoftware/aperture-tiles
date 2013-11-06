package com.oculusinfo.binning.io.impl.stream;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;

public class ZipResourcePyramidStreamSource implements PyramidStreamSource {

	private final Logger _logger = LoggerFactory.getLogger(getClass());

    private ZipFile      _tileSetArchive;
    private String       _tileExtension;


	
	public ZipResourcePyramidStreamSource (String zipFilePath, String tileExtension) {
		try {
			_tileSetArchive = new ZipFile(zipFilePath);
			_tileExtension = tileExtension;
		} catch (IOException e) {
        	_logger.warn("Could not create zip file for " + zipFilePath, e);
		}
	}
	
	@Override
	public InputStream getTileStream(String basePath, TileIndex tile) throws IOException {
    	String tileLocation = String.format("%s/"+PyramidIO.TILES_FOLDERNAME+"/%d/%d/%d." + _tileExtension, basePath, tile.getLevel(), tile.getX(), tile.getY());
    	ZipArchiveEntry entry = _tileSetArchive.getEntry(tileLocation);
    	return _tileSetArchive.getInputStream(entry);
	}

	@Override
	public InputStream getMetaDataStream(String basePath) throws IOException {
    	String location = basePath+"/"+PyramidIO.METADATA_FILENAME;
    	ZipArchiveEntry entry = _tileSetArchive.getEntry(location);
    	return _tileSetArchive.getInputStream(entry);
	}

}
