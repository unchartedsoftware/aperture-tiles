/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.annotation;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.io.*;
import com.oculusinfo.binning.io.impl.*;
import com.oculusinfo.binning.impl.*;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringLongPairArrayMapJSONSerializer;
import com.oculusinfo.binning.util.Pair;

public class AnnotationFileSystemIOTests extends AnnotationTestsBase {
	
	private static final String  ROOT_PATH = "C:\\Users\\kbirk\\Desktop\\";
	private static final String  BASE_PATH = "filesystem-annotations-test\\";
	private static final String  TILE_EXT = "json";
	private static final String  DATA_EXT = "json";
	private static final boolean VERBOSE = false;

	private AnnotationIO _dataIO;
	private PyramidIO _tileIO;
	
	private TileSerializer<Map<String, List<Pair<String, Long>>>> _tileSerializer;
	private AnnotationSerializer _dataSerializer;
	private TilePyramid _pyramid;
	private AnnotationIndexer _indexer;


    @Before
    public void setup () {
    	try {
    		
    		_dataIO = new FileSystemAnnotationIO(ROOT_PATH, DATA_EXT);   		
    		_tileIO = new FileSystemPyramidIO(ROOT_PATH, TILE_EXT);
    		
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {
		}	
    	
    	_pyramid = new WebMercatorTilePyramid();
    	_indexer = new AnnotationIndexerImpl();
    	_tileSerializer = new StringLongPairArrayMapJSONSerializer();
    	_dataSerializer = new JSONAnnotationDataSerializer();  	
	
    }

    @After
    public void teardown () {
    	_tileIO = null;
    	_dataIO = null;
    }
	
	
    @Test
    public void testHBaseIO() {
    	
    	
        List<AnnotationData<?>> annotations = generateJSONAnnotations( NUM_ENTRIES );        
        List<TileData< Map<String, List<Pair<String, Long>>>>> tiles = generateTiles( annotations, _indexer, _pyramid );
		
        List<TileIndex> tileIndices = tilesToIndices( tiles );
        List<Pair<String, Long>> dataIndices = dataToIndices( annotations );
        
    	try {
    		
    		/*
	    	 *  Write annotations
	    	 */ 	
	    	System.out.println("Writing "+NUM_ENTRIES+" to file system");	
	    	_tileIO.writeTiles(BASE_PATH, _pyramid, _tileSerializer, tiles );
	    	_dataIO.writeData(BASE_PATH, _dataSerializer, annotations );
	        
	    	/*
	    	 *  Read and check all annotations
	    	 */
	    	System.out.println( "Reading all annotations" );
	    	List<TileData< Map<String, List<Pair<String, Long>>>>> allTiles = _tileIO.readTiles( BASE_PATH, _tileSerializer, tileIndices );
	    	List<AnnotationData<?>> allData = _dataIO.readData( BASE_PATH, _dataSerializer, dataIndices );
	    	if (VERBOSE) printTiles( allTiles );
	    	if (VERBOSE) printData( allData );
	    	
	    	System.out.println( "Comparing annotations" );	    	
	    	Assert.assertTrue( compareTiles( allTiles, tiles, true ) );
	    	Assert.assertTrue( compareData( allData, annotations, true ) );
	    	
	    	System.out.println("Removing "+NUM_ENTRIES+" from file system");	
	    	_tileIO.removeTiles(BASE_PATH, tileIndices );
	    	_dataIO.removeData(BASE_PATH, dataIndices );
	       
	    	allTiles = _tileIO.readTiles( BASE_PATH, _tileSerializer, tileIndices );
	    	allData = _dataIO.readData( BASE_PATH, _dataSerializer, dataIndices );
	    	
	    	Assert.assertTrue( allTiles.size() == 0 );
	    	Assert.assertTrue( allData.size() == 0 );
	    	
	    	System.out.println( "Complete" );
	    	
	
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		}
    }


	
}
