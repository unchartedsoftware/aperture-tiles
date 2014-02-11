/**
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
package com.oculusinfo.tile.spi.impl.pyramidio.io;

import java.net.URL;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.ResourceStreamReadOnlyPyramidIO;
import com.oculusinfo.binning.io.impl.ZipResourcePyramidStreamSource;

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
