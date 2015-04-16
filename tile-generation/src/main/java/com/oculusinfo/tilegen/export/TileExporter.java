/*
 * Copyright (c) 2015 Oculus Info Inc. 
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

package com.oculusinfo.tilegen.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.avro.file.CodecFactory;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.impl.PrimitiveAvroSerializer;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.FileBasedPyramidIO;
import com.oculusinfo.binning.io.impl.FileSystemPyramidSource;
import com.oculusinfo.binning.io.impl.HBasePyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;

/**
* TileExporter is an application for the exporting aperture-tiles' data from HBase to a local filesystem.
* 
* Command line arguments are as follows:
*  
* -hbase -- HBase master. Default = hadoop-s1.oculus.local:60000
* 
* -zk -- Zookeeper Quorum. Default = zookeeperQuorum
* 
* -zkp -- Zookeeper Port.  Default = 2181
* 
* -tableid -- Name of hbase table to export [required].
* 
* -savepath -- Path on local filesystem to save exported table.  Default is "c:/".   
* 
* -minlevel -- Min aperture-tiles zoom level to export from hbase table.
* 
* -maxlevel -- Max aperture-tiles zoom level to export from hbase table.
*
*
**/

//TODO -- this application assumes tiles to be exported use avro serialization, and have Double bin type.  
// Support for exporting other bin types should be added in the future.

public class TileExporter {

    private PyramidIO _from;
    private PyramidIO _to;

    public TileExporter (String zookeeperQuorum, String zookeeperPort, String hbaseMaster,
                          String rootPath, String extension) throws IOException {
        _from = new HBasePyramidIO(zookeeperQuorum, zookeeperPort, hbaseMaster);
        _to = new FileBasedPyramidIO(new FileSystemPyramidSource(rootPath, extension));
    }

    public <T> void copyPyramid (String pyramidId, int minLevel, int maxLevel, TileSerializer<T> serializer, int blockSize) throws IOException {
        // Parameters 2-4 aren't used except in live tiling, so dummy parameters are fine.
        _from.initializeForRead(pyramidId, 256, 256, null);
        _to.initializeForWrite(pyramidId);

        System.out.println("Writing metadata");
        _to.writeMetaData(pyramidId, _from.readMetaData(pyramidId));
        for (int level = minLevel; level <= maxLevel; ++level) {
        	System.out.println("Copying level " + level);
            copyLevel(pyramidId, level, serializer, blockSize);
        }
    }
    public <T> void copyLevel (String pyramidId, int level, TileSerializer<T> serializer, int blockSize) throws IOException {

        int N = 1 << level;

        List<TileIndex> indices = new ArrayList<>(blockSize);
        for (int x=0; x<N; ++x) {
            for (int y=0; y<N; ++y) {
                boolean last = (x == N-1 && y == N-1);
                indices.add(new TileIndex(level, x, y));
                if (indices.size() >= blockSize || last) {
                    retrieveAndCopy(pyramidId, serializer, indices);
                    indices.clear();
                }
            }
        }
    }

    private <T> void retrieveAndCopy (String pyramidId, TileSerializer<T> serializer, Iterable<TileIndex> indices) throws IOException {
        _to.writeTiles(pyramidId, serializer, _from.readTiles(pyramidId, serializer, indices));
    }

    public static void main(String [] args) {
    	
    	//----- set default values
        String hbaseMaster = "hadoop-s1.oculus.local:60000";    	
        String zookeeperQuorum = "hadoop-s1.oculus.local";
        String zookeeperPort = "2181";
        String rootPath = "c:/";
        String pyramidId = "";
        int minLevel = 0;
        int maxLevel = 0;
        
        String extension = "avro";
        //------
        
        try {
        	
        	//------ parse command line arguments and store in a Map
    		HashMap<String, String> argMap = new HashMap<String, String>();   		
            int i = 0;
            String propValue = "";
            String propName = "";
            
            while (i < args.length) {
            	
            	String newarg = args[i++];
            	if (newarg.startsWith("-")) {
            		//start of a new property, so save previous one
            		if ((propName != "") && (propName != null)) {
            			argMap.put(propName, propValue.trim());
            		}
            		propName = newarg.substring(1);	// to remove the "-" at start
            		propValue = "";
            	}
            	else {
            		propValue += " " + newarg;
            	}
            }
            if ((propName != "") && (propName != null)) {	// save last argument
    			argMap.put(propName, propValue.trim());
    		}
            //------------------
            
            //----- set command line parameters (and error checking)
            if (argMap.containsKey("hbase")) {
            	hbaseMaster = argMap.get("hbase");
            }
            if (argMap.containsKey("zk")) {
            	zookeeperQuorum = argMap.get("zk");
            }            
            if (argMap.containsKey("zkport")) {
            	zookeeperPort = argMap.get("zkport");
            }
            if (argMap.containsKey("zkport")) {
            	zookeeperPort = argMap.get("zkport");
            }
            if (argMap.containsKey("tableid")) {
            	pyramidId = argMap.get("tableid");
            }
            else {
            	throw new IOException("-tableid command line parameter not found!");
            }
            if (argMap.containsKey("savepath")) {
            	rootPath = argMap.get("savepath");
            }
            else {
            	throw new IOException("-savepath command line parameter not found!");
            }
            if (argMap.containsKey("minlevel")) {
            	minLevel = Integer.parseInt(argMap.get("minlevel"));
            }
            else {
            	throw new IOException("-minlevel command line parameter not found!");
            }
            if (argMap.containsKey("maxlevel")) {
            	maxLevel = Integer.parseInt(argMap.get("maxlevel"));
            }
            else {
            	throw new IOException("-maxlevel command line parameter not found!");
            }                      
            if ((minLevel < 0) || (maxLevel < 0) || (minLevel > maxLevel)) {
            	throw new IOException("minlevel and maxlevel parameters must be >=0 and minlevel <= maxlevel!");
            }
            
            System.out.println("------------------------");
            System.out.println("Starting Hbase Tile Exporter...");
            System.out.println("Hbase Master = " + hbaseMaster);
            System.out.println("Zookeeper = " + zookeeperQuorum + ":" + zookeeperPort);
            System.out.println("From hbase table id: " + pyramidId);
            System.out.println("Storing at local path: " + rootPath);
            System.out.println("Extracting levels " + minLevel + " to " + maxLevel);      
            System.out.println("------------------------");

            int blockSize = 100;
            TileSerializer<Double> serializer = new PrimitiveAvroSerializer<Double>(Double.class, CodecFactory.bzip2Codec());
            TileExporter extractor = new TileExporter(zookeeperQuorum, zookeeperPort, hbaseMaster, rootPath, extension);
            extractor.copyPyramid(pyramidId, minLevel, maxLevel, serializer, blockSize);
            
            System.out.println("Done!");
            System.out.println("------------------------");
        }
        catch (Exception e) {
            e.printStackTrace();
            
            System.out.println("");
            System.out.println("---- See comments in TileExporter.java for usage syntax");
        }
    }
}

