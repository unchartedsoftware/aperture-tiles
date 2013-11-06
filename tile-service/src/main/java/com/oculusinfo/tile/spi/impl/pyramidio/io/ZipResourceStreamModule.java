package com.oculusinfo.tile.spi.impl.pyramidio.io;

import java.net.URL;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.ResourceStreamReadOnlyPyramidIO;
import com.oculusinfo.binning.io.impl.stream.ZipResourcePyramidStreamSource;

public class ZipResourceStreamModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(PyramidIO.class).to(ResourceStreamReadOnlyPyramidIO.class);
	}
	
	@Provides
	ResourceStreamReadOnlyPyramidIO provideFileSystemIo(@Named("com.oculusinfo.tile.pyramidio.resource.location")String location,
	                                                    @Named("com.oculusinfo.tile.pyramidio.resource.extension")String extension){
		
		URL zipFile = ZipResourceStreamModule.class.getResource(location);
		return new ResourceStreamReadOnlyPyramidIO( new ZipResourcePyramidStreamSource(zipFile.getFile(), extension) );
	}
}
