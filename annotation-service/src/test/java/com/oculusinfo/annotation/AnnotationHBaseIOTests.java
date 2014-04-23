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
import com.oculusinfo.binning.impl.*;

public class AnnotationHBaseIOTests extends AnnotationTestsBase {
	
	private static final String  TABLE_NAME = "AnnotationTable";
	private static final boolean VERBOSE = false;

	private AnnotationIO _io;
	private AnnotationIndexer _indexer;
	private AnnotationSerializer<AnnotationTile> _tileSerializer;
	private AnnotationSerializer<AnnotationData<?>> _dataSerializer;
	private TilePyramid _pyramid;
	

    @Before
    public void setup () {
    	try {
    		_io = new HBaseAnnotationIO("hadoop-s1.oculus.local",
										"2181",
									    "hadoop-s1.oculus.local:60000");
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {
		}	
    	
    	_pyramid = new WebMercatorTilePyramid();
    	_indexer = new TileAnnotationIndexer( _pyramid );
    	_tileSerializer = new JSONTileSerializer();
    	_dataSerializer = new JSONDataSerializer();  	
	
    }

    @After
    public void teardown () {
    	_io = null;
    }
	
	
    @Test
    public void testHBaseIO() {
    	
    	
        List<AnnotationData<?>> annotations = generateJSONAnnotations( NUM_ENTRIES );
        List<AnnotationTile> tiles = generateTiles( NUM_ENTRIES, _indexer );
    	
        List<TileIndex> tileIndices = tilesToIndices( tiles );
        List<AnnotationReference> dataIndices = dataToIndices( annotations );
        
    	try {
    		
	        /*
	    	 *  Create Table
	    	 */
			System.out.println("Creating table");
	    	_io.initializeForWrite( TABLE_NAME );
	    	
	        /*
	    	 *  Write annotations
	    	 */ 	
	    	System.out.println("Writing "+NUM_ENTRIES+" to table");	
	    	_io.writeTiles(TABLE_NAME, _tileSerializer, tiles );
	    	_io.writeData(TABLE_NAME, _dataSerializer, annotations );
	        
	    	/*
	    	 *  Read and check all annotations
	    	 */
	    	System.out.println( "Reading all annotations" );
	    	List<AnnotationTile> allTiles = _io.readTiles( TABLE_NAME, _tileSerializer, tileIndices );
	    	List<AnnotationData<?>> allData = _io.readData( TABLE_NAME, _dataSerializer, dataIndices );
	    	if (VERBOSE) printTiles( allTiles );
	    	if (VERBOSE) printData( allData );
	    	
	    	System.out.println( "Comparing annotations" );	    	
	    	Assert.assertTrue( compareTiles( allTiles, tiles, true ) );
	    	Assert.assertTrue( compareData( allData, annotations, true ) );

	    	System.out.println( "Complete" );
	
    	} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {
			/*
	    	 * Drop table
	    	 */
	    	System.out.println("Disabling and dropping table");
	    	((HBaseAnnotationIO)_io).dropTable(TABLE_NAME);
		}
    }


	
}
