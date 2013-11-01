package com.oculusinfo.binning.io.impl.stream;

import java.io.InputStream;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TileSerializer;

public class ResourcePyramidStreamSource implements PyramidStreamSource {
	
	private String _rootPath;

	public ResourcePyramidStreamSource(String rootPath){
		_rootPath=rootPath;
	}
	
    @Override
    public InputStream getTileStream(String basePath, TileSerializer<?> serializer, TileIndex tile) {
    	String fileExtension = serializer.getFileExtension();
    	String tileLocation = String.format("%s/"+PyramidIO.TILES_FOLDERNAME+"/%d/%d/%d." + fileExtension, _rootPath + basePath, tile.getLevel(), tile.getX(), tile.getY());
    	return ResourcePyramidStreamSource.class.getResourceAsStream(tileLocation);
    }

    @Override
    public InputStream getMetaDataStream (String basePath) {
    	String location = _rootPath + basePath+"/"+PyramidIO.METADATA_FILENAME;
    	return ResourcePyramidStreamSource.class.getResourceAsStream(location);
    }

}
