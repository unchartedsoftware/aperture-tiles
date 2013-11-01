package com.oculusinfo.binning.io.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.io.impl.DoubleAvroSerializer;
import com.oculusinfo.binning.io.impl.JDBCPyramidIO;
import com.oculusinfo.binning.io.impl.SQLitePyramidIO;

public class FileSystemAvroToSQLite {

	private String _tileRootPath;
	private String _outputPath;

	public FileSystemAvroToSQLite(String tilesetRootPath, String outputPath) {
		_tileRootPath = tilesetRootPath;
		_outputPath = outputPath;
	}
	
	public void convert() throws Exception {
		JDBCPyramidIO sqlIO = new SQLitePyramidIO(_outputPath);
				
		File rootDir = new File(_tileRootPath);
		for (File pyramidRoot : rootDir.listFiles()) {
			String pyramidId = pyramidRoot.getName();
			sqlIO.initializeForWrite(pyramidId);
			
			// Read metadata.json and add to DB
			File metadata = new File(pyramidRoot, "metadata.json");
			Scanner scanner = new Scanner(metadata);
			scanner.useDelimiter("\\Z");
			String metadataStr = scanner.next();
			scanner.close();
			
			sqlIO.writeMetaData(pyramidId, metadataStr);
			
			// Read tiles directory and add to DB
			DoubleAvroSerializer serializer = new DoubleAvroSerializer();
			TilePyramid tilePyramid = new WebMercatorTilePyramid();
			List<TileData<Double>> tileDataList = new ArrayList<TileData<Double>>();
			
			File tilesDir = new File(pyramidRoot, "tiles");
			for (File z : tilesDir.listFiles()) {
				Integer zCoord = Integer.valueOf(z.getName());
				for (File x : z.listFiles()) {
					Integer xCoord = Integer.valueOf(x.getName());
					tileDataList.clear();
					for (File y : x.listFiles()) {
						String avroFile = y.getName();
						Integer yCoord = Integer.valueOf(avroFile.split("\\.")[0]);
						
						TileIndex tIndex = new TileIndex(zCoord, xCoord, yCoord);						
						FileInputStream fis = new FileInputStream(y);
						TileData<Double> td = null;
						try {
							td = serializer.deserialize(tIndex, fis);
						} catch (Exception e) {
							System.err.println("Error deserializing tile: " + tIndex);
							e.printStackTrace();
						} finally {
							fis.close();
						}
						
						if (td!=null)
							tileDataList.add(td);
					}
					// TODO Note that we're serializing these in AVRO format
					// into the DB--not the most compact serialization scheme!
					sqlIO.writeTiles(pyramidId, tilePyramid, serializer, tileDataList);
				}
			}	
		}
	}
	
	// TODO Network path for DB? Multiple clients hitting the DB?
	public static void main(String[] args) throws Exception {
		FileSystemAvroToSQLite converter = new FileSystemAvroToSQLite("../../data/ibm.twitter.unpacked", 
				"ibm-twitter.db");
		converter.convert();
	}
	
}
