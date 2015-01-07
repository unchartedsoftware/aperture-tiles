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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.fail;

public class ZipResourcePyramidSourceTest {

	@Test
	public void test() {
		
		ZipArchiveOutputStream zos = null;
		File archive;
		String filename = "";
		try {
			archive = File.createTempFile("test.", ".zip", null);
			archive.deleteOnExit();
			filename = archive.getAbsolutePath();
			zos = new ZipArchiveOutputStream(archive);
			
			for(int z=0; z<3; z++){
				for(int x=0; x<Math.pow(2, z); x++){
					for(int y=0; y<Math.pow(2, z); y++){
						ZipArchiveEntry entry = new ZipArchiveEntry(getDummyFile(), "test/tiles/" +z+ "/" +x+ "/" +y+ ".dummy");
						zos.putArchiveEntry(entry);						
					}
				}
			}
			
			ZipArchiveEntry entry = new ZipArchiveEntry(getDummyFile(), "test/metadata.json");
			zos.putArchiveEntry(entry);

			zos.closeArchiveEntry();
			zos.close();
			zos=null;
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
		try {
			ZipResourcePyramidSource src = new ZipResourcePyramidSource(filename, "dummy");
			
			TileIndex tileDef = new TileIndex(0, 0, 0, 1, 1);
			InputStream is = src.getSourceTileStream("test", tileDef);
			Assert.assertTrue(is!=null);

			tileDef = new TileIndex(2, 3, 3, 1, 1);
			is = src.getSourceTileStream("test", tileDef);
			Assert.assertTrue(is!=null);
			
			is = src.getSourceMetaDataStream("test");
			Assert.assertTrue(is!=null);

			
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
		
	}

	private File getDummyFile() throws IOException{
		return File.createTempFile("dummy", null);
	}
}
