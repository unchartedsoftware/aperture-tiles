package com.oculusinfo.binning.io.impl.stream;

import java.io.InputStream;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;

public class ResourcePyramidStreamSource implements PyramidStreamSource {
	
	private String _rootPath;
	private String _extension;

	public ResourcePyramidStreamSource (String rootPath, String extension) {
		_rootPath=rootPath;
		_extension = extension;
	}
	
    @Override
    public InputStream getTileStream(String basePath, TileIndex tile) {
    	String tileLocation = String.format("%s/"+PyramidIO.TILES_FOLDERNAME+"/%d/%d/%d." + _extension, _rootPath + basePath, tile.getLevel(), tile.getX(), tile.getY());
    	return ResourcePyramidStreamSource.class.getResourceAsStream(tileLocation);
    }

    @Override
    public InputStream getMetaDataStream (String basePath) {
    	String location = _rootPath + basePath+"/"+PyramidIO.METADATA_FILENAME;
    	return ResourcePyramidStreamSource.class.getResourceAsStream(location);
    }

}
