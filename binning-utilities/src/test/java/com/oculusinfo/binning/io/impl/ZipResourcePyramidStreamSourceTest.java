package com.oculusinfo.binning.io.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.TileSerializer;
import com.oculusinfo.binning.io.impl.stream.ZipResourcePyramidStreamSource;

public class ZipResourcePyramidStreamSourceTest {

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
			ZipResourcePyramidStreamSource src = new ZipResourcePyramidStreamSource(filename);
			
			TileIndex tileDef = new TileIndex(0, 0, 0, 1, 1);
			InputStream is = src.getTileStream("test", getSerializer(), tileDef);
			Assert.assertTrue(is!=null);

			tileDef = new TileIndex(2, 3, 3, 1, 1);
			is = src.getTileStream("test", getSerializer(), tileDef);
			Assert.assertTrue(is!=null);
			
			is = src.getMetaDataStream("test");
			Assert.assertTrue(is!=null);

			
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
		
	}

	private File getDummyFile() throws IOException{
		return File.createTempFile("dummy", null);
	}
		
	private TileSerializer<Double> getSerializer() {
		
		return new TileSerializer<Double>() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void serialize(TileData<Double> data, TilePyramid tilePyramid,
					OutputStream ouput) throws IOException {
			}
			
			@Override
			public String getFileExtension() {
				return "dummy";
			}
			
			@Override
			public TileData<Double> deserialize(TileIndex index, InputStream rawData)
					throws IOException {
				return null;
			}
		};
		
	}
}
