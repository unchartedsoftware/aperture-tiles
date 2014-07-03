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

package com.oculusinfo.binning.io.storageutil;


import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TileIterable;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.*;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer;
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer;
import com.oculusinfo.binning.metadata.PyramidMetaData;
import org.apache.avro.file.CodecFactory;
import org.json.JSONObject;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @TODO Document this
 * 
 * @author mkielo
 */
public class TileStorageUtility {
	
    private static final Logger LOGGER = Logger.getLogger(PyramidMetaData.class.getName());
	
	private static PyramidIO getPyramidIO (Properties properties, String prefix) throws Exception {

		try{
			
			String fromtype = properties.getProperty(prefix+"type");
			String rootpath = properties.getProperty(prefix+"path", "");
			
			PyramidIO fromobject = null;
			
			if (fromtype.equals("FileSystem")) {
				fromobject = new FileSystemPyramidIO(rootpath, "avro");
			} else if (fromtype.equals("HBase")) {
				//
				String fromzookeeperQuorum = properties.getProperty(prefix+"zookeeperQuorum");
				String fromzookeeperPort = properties.getProperty(prefix+"zookeeperPort");
				String fromhbaseMaster = properties.getProperty(prefix+"zookeeperMaster");		
				fromobject = new HBasePyramidIO(
						fromzookeeperQuorum, 
						fromzookeeperPort,
						fromhbaseMaster);
				
			} else if (fromtype.equals("Zip")) {
		        PyramidStreamSource newSource = new ZipResourcePyramidStreamSource(rootpath, "tile");
		        fromobject = new ResourceStreamReadOnlyPyramidIO(newSource);
			} else if (fromtype.equals("SQLite")) {
		//		fromobject = new SQLitePyramidIO(rootpath);
			} else if (fromtype.equals("ClassResourceStream")) {
		        PyramidStreamSource newSource = new ResourcePyramidStreamSource(rootpath, "avro");
		        fromobject = new ResourceStreamReadOnlyPyramidIO(newSource);
			}
			
			return fromobject;

		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;	
	}


	// We have no possible way of checking this cast; all we can do is trust the
	// user who set up our parameters.
	@SuppressWarnings("unchecked")
	private static <T> TileSerializer<T> getTileSerializer (Properties properties) {
		String serializerClassName = properties.getProperty("serializer", "com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer");

		try {
			Class<?> c = Class.forName(serializerClassName);
			Constructor<?> cons = c.getConstructor(CodecFactory.class);
			return (TileSerializer<T>) cons.newInstance(CodecFactory.bzip2Codec());

		} catch (Exception e) {
            	LOGGER.log(Level.WARNING,
                    "Exception Found", e);
			return null;
		}
	}

	// Not compiler-checked, but hand-checked - all possiblities in the if
	// statement below are hand-checked before checkin for compatibility.
	@SuppressWarnings("unchecked")
	private static <T> TileSerializer<T> getLegacyConversionSerializer_0_1 (TileSerializer<T> outputSerializer) {
		if (outputSerializer instanceof DoubleAvroSerializer) {
			return (TileSerializer<T>) new BackwardCompatibilitySerializer();
		} else if (outputSerializer instanceof BackwardCompatibilitySerializer) {
			return (TileSerializer<T>) new DoubleAvroSerializer(null);
		} else {
			throw new RuntimeException ("foobar");
		}
	}

	private static TilePyramid getTilePyramid (Properties properties) {
		String projection = properties.getProperty("projection","WebMercator");
		TilePyramid pyramid = null;
		if (projection.equals("WebMercator")){
			pyramid = new WebMercatorTilePyramid();
		}
		else if (projection.equals("AOI")){
			double minx = Double.parseDouble(properties.getProperty("minx"));
			double maxx = Double.parseDouble(properties.getProperty("maxx"));
			double miny = Double.parseDouble(properties.getProperty("miny"));
			double maxy = Double.parseDouble(properties.getProperty("maxy"));
						
			pyramid = new AOITilePyramid(minx, miny, maxx, maxy);
		}
		return pyramid;
	}

	
	private static Integer getBlockSize (Properties properties) {
		Integer blocksize = Integer.parseInt(properties.getProperty("blocksize", "1000"));
		return blocksize;
	}
	
	
	private static <T> void readWrite (PyramidIO inputIO, String inputId,
			                           PyramidIO outputIO, String outputId, 
			                           TileSerializer<T> fromSerializer,
			                           TileSerializer<T> toSerializer,
			                           Iterable<TileIndex> tiles) throws IOException {

		outputIO.writeTiles(outputId, toSerializer,
				            inputIO.readTiles(inputId, fromSerializer, tiles));
	}

	
	private static void logLevels(List<Integer> levels){
		int length = levels.size();
		int maxlevel = levels.get(length-1);
		int minlevel = levels.get(0);
		for(int i = minlevel; i <= maxlevel; i++){
			if(levels.contains(i)){
				LOGGER.log(Level.INFO,
	                    "PREPARING TO COPY LEVEL: " + i);
			}
			else if(!levels.contains(i)){
				LOGGER.log(Level.WARNING,
						"LEVEL " + i + " NOT DETECTED IN METADATA");
			}
		}
	}
							
	private static List<Integer> configureLevels(Properties properties,
												List<Integer> inputlevels) {
		List<Integer> outputlevels = inputlevels;
		if(properties.getProperty("from.zoom") != null){
			Integer fromzoom = Integer.parseInt(properties.getProperty("from.zoom"));
			while(outputlevels.get(0) < fromzoom){
				outputlevels.remove(0);
			}
		}
		if(properties.getProperty("to.zoom") != null){
			Integer tozoom = Integer.parseInt(properties.getProperty("to.zoom"));
			while(outputlevels.get(outputlevels.size()- 1) > tozoom){
				outputlevels.remove(outputlevels.size()- 1);
			}
		}
		return outputlevels;
	}
	
/*	private static void fixMetadata(List<Integer> levels, String metaPath){
		int maxLevel = Collections.max(levels);
		int minLevel = Collections.min(levels);
		
	} */

	// This is supressing the warning for using non-generic versions of
	// generified types; this is done here to match the two property-read
	// serializers, which do have to match, but there's no way for us to know
	// they match.  So this is the best we can do.
	// @SuppressWarnings("unchecked")
	public static <T> void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		
		// The properties file is currently read from: System.out.println(new File(".").getAbsolutePath());
		String propertiesFile = "scripts/storageutil/config.properties";
		if (args.length>0) {
			propertiesFile = args[0];
		}
		
		Properties prop = new Properties();
		InputStream input = new FileInputStream(propertiesFile);
		prop.load(input);
		
		PyramidIO fromObject = getPyramidIO(prop, "from.");
		PyramidIO toObject = getPyramidIO(prop, "to.");
		
		String fromPyramidID = prop.getProperty("from.ID");
		String toPyramidID = prop.getProperty("to.ID");
		
		System.out.println(toObject);
		
		// Set write serializer. This should almost always equal
		TileSerializer<T> toSerializer = getTileSerializer(prop);
		
		// Set read serializer
		TileSerializer<T> fromSerializer = toSerializer;

		if (prop.getProperty("conversion") != null){
			fromSerializer = getLegacyConversionSerializer_0_1(toSerializer);
		}
		
		TilePyramid pyramid = getTilePyramid(prop);

		// read metadata
		PyramidMetaData metadata = new PyramidMetaData(
				fromObject.readMetaData(
						fromPyramidID));

		String toPath = prop.getProperty("to.path");
		
		File folder = new File(toPath + toPyramidID);
		File jsonFile = new File(toPath + toPyramidID + "/metadata.json");
		
		if (!folder.exists()) {
			folder.mkdirs();
			jsonFile.createNewFile();
		}
		
		
		
		else if (!jsonFile.exists()) {

			jsonFile.createNewFile();
		}
		
		// block size 
		Integer blocksize = getBlockSize(prop);
		
		// write metadata
		// @TODO: Make a toString function for the metadata that does this, don't need to know about the JSON object here
		JSONObject rawmetadata = metadata.getRawData();
		String strmetadata = rawmetadata.toString();
		
		toObject.writeMetaData(toPyramidID, strmetadata);
		
		// gets the number of levels in the pyramid
		List<Integer> pyramidlevels = metadata.getLevels();
		Collections.sort(pyramidlevels);
	
		
		pyramidlevels = configureLevels(prop, pyramidlevels);
		
		//log levels	
		logLevels(pyramidlevels);
		
		Rectangle2D area = pyramid.getTileBounds(new TileIndex(0, 0, 0));
			
		List<TileIndex> toLoad = new ArrayList<>(blocksize);
		int count = 0;
		int batch = 1;
		long lastPrint = System.currentTimeMillis();
		long currentTime;
		for (int zoom: pyramidlevels) {
			TileIterable tiles = new TileIterable(pyramid, zoom, area);	
			LOGGER.log(Level.INFO,
                    "COPYING ZOOM LEVEL: " + zoom);
			for (TileIndex tile: tiles) {
				toLoad.add(tile);
				++count;
				if (count > blocksize) {
					readWrite (fromObject, fromPyramidID,
	                           toObject, toPyramidID,
	                           fromSerializer, toSerializer, toLoad);
					currentTime = System.currentTimeMillis();
					if(currentTime - lastPrint > 5000){
						LOGGER.log(Level.INFO,
			                    "CURRENT ZOOM LEVEL: " + zoom 
			                    + ". CURRENT BATCH: " + batch 
			                    + ". TOTAL TIME ELAPSED: " 
			                    + ((currentTime - startTime)/1000) 
			                    + " SECONDS");
						lastPrint = System.currentTimeMillis();
					}
					count = 0;
					toLoad.clear();
					++batch;
				}
			}

			// @TODO: Change to INFO level log messages; put in the if where the read/write is; change to either:
			//  (a) Output list of levels outputting at this iteration, or
			//  (b) hbase-like output - output current key
		}
		
		if (!toLoad.isEmpty()){
			readWrite (fromObject, fromPyramidID,
                    toObject, toPyramidID,
                    fromSerializer, toSerializer, toLoad);
		}
		LOGGER.log(Level.INFO,
                "TILE COPYING COMPLETE");
		// maybe @TODO: Add logging of total time spent, total tiles copied, levels copied, sparseness of tiles
	}



}	