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

public class AnnotationHBaseIOTests extends AnnotationTestsBase {
	
	private static final String  TILE_TABLE_NAME = "annotation.hbase.test";
	private static final String  DATA_TABLE_NAME = TILE_TABLE_NAME + ".data";
	private static final boolean VERBOSE = false;

	private AnnotationIO _dataIO;
	private PyramidIO _tileIO;
	
	private TileSerializer<Map<String, List<Pair<String, Long>>>> _tileSerializer;
	private AnnotationSerializer<AnnotationData<?>> _dataSerializer;
	private TilePyramid _pyramid;
	private AnnotationIndexer _indexer;


    @Before
    public void setup () {
    	try {
    		_dataIO = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										"2181",
									    "hadoop-s1.oculus.local:60000");
    		
    		_tileIO = new HBasePyramidIO("hadoop-s1.oculus.local",
										 "2181",
									     "hadoop-s1.oculus.local:60000");
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
        List<TileData< Map<String, List<Pair<String, Long>>>>> tiles = generateTiles( NUM_ENTRIES, _indexer, _pyramid );
		
        List<TileIndex> tileIndices = tilesToIndices( tiles );
        List<Pair<String, Long>> dataIndices = dataToIndices( annotations );
        
    	try {
    		
	        /*
	    	 *  Create Table
	    	 */
			System.out.println("Creating table");
	    	_tileIO.initializeForWrite( TILE_TABLE_NAME );
	    	_dataIO.initializeForWrite( DATA_TABLE_NAME );
	        /*
	    	 *  Write annotations
	    	 */ 	
	    	System.out.println("Writing "+NUM_ENTRIES+" to table");	
	    	_tileIO.writeTiles(TILE_TABLE_NAME, _pyramid, _tileSerializer, tiles );
	    	_dataIO.writeData(DATA_TABLE_NAME, _dataSerializer, annotations );
	        
	    	/*
	    	 *  Read and check all annotations
	    	 */
	    	System.out.println( "Reading all annotations" );
	    	List<TileData< Map<String, List<Pair<String, Long>>>>> allTiles = _tileIO.readTiles( TILE_TABLE_NAME, _tileSerializer, tileIndices );
	    	List<AnnotationData<?>> allData = _dataIO.readData( DATA_TABLE_NAME, _dataSerializer, dataIndices );
	    	if (VERBOSE) printTiles( allTiles );
	    	if (VERBOSE) printData( allData );
	    	
	    	System.out.println( "Comparing annotations" );	    	
	    	Assert.assertTrue( compareTiles( allTiles, tiles, true ) );
	    	Assert.assertTrue( compareData( allData, annotations, true ) );
	    	
	    	System.out.println("Removing "+NUM_ENTRIES+" from table");	
	    	_tileIO.removeTiles(TILE_TABLE_NAME, tileIndices );
	    	_dataIO.removeData(DATA_TABLE_NAME, dataIndices );
	       
	    	allTiles = _tileIO.readTiles( TILE_TABLE_NAME, _tileSerializer, tileIndices );
	    	allData = _dataIO.readData( DATA_TABLE_NAME, _dataSerializer, dataIndices );
	    	
	    	Assert.assertTrue( allTiles.size() == 0 );
	    	Assert.assertTrue( allData.size() == 0 );
	    	
	    	System.out.println( "Complete" );
	
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {
			/*
	    	 * Drop table
	    	 */
	    	System.out.println("Disabling and dropping table");
	    	((HBasePyramidIO)_tileIO).dropTable(TILE_TABLE_NAME);
	    	((HBaseAnnotationIO)_dataIO).dropTable(DATA_TABLE_NAME);
		}
    }


	
}
