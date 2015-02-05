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
package com.oculusinfo.annotation.io.impl;


import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.AnnotationTile;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.index.impl.AnnotationIndexerImpl;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.JSONAnnotationDataSerializer;
import com.oculusinfo.annotation.util.AnnotationGenerator;
import com.oculusinfo.annotation.util.AnnotationUtil;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.HBasePyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringLongPairArrayMapJsonSerializer;
import com.oculusinfo.factory.util.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Ignore
public class AnnotationHBaseIOTests {

    private static final Logger LOGGER = LoggerFactory.getLogger( AnnotationHBaseIOTests.class );

	private static final String  TABLE_NAME = "annotation.hbase.test";
    private static double [] BOUNDS = { 180, 85.05, -180, -85.05};
    private static String [] GROUPS = {"Urgent", "High", "Medium", "Low"};
    private int NUM_ENTRIES = 50;
	private AnnotationIO _dataIO;
	private PyramidIO _tileIO;
	
	private TileSerializer<Map<String, List<Pair<String, Long>>>> _tileSerializer;
	private AnnotationSerializer _dataSerializer;
	private TilePyramid _pyramid;
	private AnnotationIndexer _indexer;

	@Before
	public void setup () {
		try {
			_dataIO = new HBaseAnnotationIO("MUST",
			                                "SET",
			                                "THESE");
    		
			_tileIO = new HBasePyramidIO("MUST",
			                             "SET",
			                             "THESE");
		} catch (Exception e) {
    		
			LOGGER.error("Error: " + e.getMessage());
			
		}
    	
		_pyramid = new WebMercatorTilePyramid();
		_indexer = new AnnotationIndexerImpl();
		_tileSerializer = new StringLongPairArrayMapJsonSerializer();
		_dataSerializer = new JSONAnnotationDataSerializer();  	
	
	}

	@Test
	public void testHBaseIO() {

    	AnnotationGenerator generator = new AnnotationGenerator( BOUNDS, GROUPS );

		List<AnnotationData<?>> annotations = generator.generateJSONAnnotations( NUM_ENTRIES );
		List<AnnotationTile> tiles = generator.generateTiles( annotations, _indexer, _pyramid );
		
		List<TileIndex> tileIndices = AnnotationUtil.tilesToIndices( tiles );
		List<Pair<String, Long>> dataIndices = AnnotationUtil.dataToIndices( annotations );

		try {
    		
			/*
			 *  Create Table
			 */
			LOGGER.debug("Creating table");
			_tileIO.initializeForWrite( TABLE_NAME );
			_dataIO.initializeForWrite( TABLE_NAME );
			/*
			 *  Write annotations
			 */ 	
			LOGGER.debug("Writing "+NUM_ENTRIES+" to table");
			_tileIO.writeTiles(TABLE_NAME, _tileSerializer, AnnotationTile.convertToRaw( tiles ) );
			_dataIO.writeData(TABLE_NAME, _dataSerializer, annotations );
	        
			/*
			 *  Read and check all annotations
			 */
			LOGGER.debug( "Reading all annotations" );
			List<AnnotationTile> allTiles = AnnotationTile.convertFromRaw(_tileIO.readTiles(TABLE_NAME, _tileSerializer, tileIndices));
			List<AnnotationData<?>> allData = _dataIO.readData( TABLE_NAME, _dataSerializer, dataIndices );
			AnnotationUtil.printTiles( allTiles );
			AnnotationUtil.printData( allData );
	    	
			LOGGER.debug( "Comparing annotations" );
			Assert.assertTrue( AnnotationUtil.compareTiles( allTiles, tiles ) );
			Assert.assertTrue( AnnotationUtil.compareData( allData, annotations ) );
	    	
			LOGGER.debug("Removing "+NUM_ENTRIES+" from table");
			_tileIO.removeTiles(TABLE_NAME, tileIndices );
			_dataIO.removeData(TABLE_NAME, dataIndices );
	       
			allTiles = AnnotationTile.convertFromRaw(_tileIO.readTiles(TABLE_NAME, _tileSerializer, tileIndices));
			allData = _dataIO.readData( TABLE_NAME, _dataSerializer, dataIndices );
	    	
			Assert.assertTrue( allTiles.size() == 0 );
			Assert.assertTrue( allData.size() == 0 );
	    	
			LOGGER.debug( "Complete" );
	
		} catch (Exception e) {
    		
			LOGGER.error( "Error: " + e.getMessage() );
			
		} finally {
			/*
			 * Drop table
			 */
			LOGGER.debug("Disabling and dropping table");
			((HBasePyramidIO)_tileIO).dropTable(TABLE_NAME);
			((HBaseAnnotationIO)_dataIO).dropTable(TABLE_NAME);
		}
	}
}
