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

import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.data.AnnotationTile;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.index.impl.AnnotationIndexerImpl;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.impl.FileSystemAnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.impl.JSONAnnotationDataSerializer;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.impl.FileSystemPyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringLongPairArrayMapJsonSerializer;
import com.oculusinfo.binning.util.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class AnnotationFileSystemIOTest extends AnnotationTestsBase {
	
	private static final String  ROOT_PATH = ".\\";
	private static final String  BASE_PATH = TEST_LAYER_NAME;
	private static final String  TILE_EXT = "json";
	private static final String  DATA_EXT = "json";
    private static double [] BOUNDS = { 180, 85.05, -180, -85.05};
    private static String [] GROUPS = {"Urgent", "High", "Medium", "Low"};
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
			
		}
    	
		_pyramid = new WebMercatorTilePyramid();
		_indexer = new AnnotationIndexerImpl();
		_tileSerializer = new StringLongPairArrayMapJsonSerializer();
		_dataSerializer = new JSONAnnotationDataSerializer();  	
	
	}

	
	@Test
	public void testFileSystemIO() {


        AnnotationGenerator generator = new AnnotationGenerator( BOUNDS, GROUPS );

		List<AnnotationData<?>> annotations = generator.generateJSONAnnotations( NUM_ENTRIES );
		List<AnnotationTile> tiles = generator.generateTiles( annotations, _indexer, _pyramid );
		
		List<TileIndex> tileIndices = tilesToIndices( tiles );
		List<Pair<String, Long>> dataIndices = dataToIndices( annotations );
        
		try {
    		
			/*
			 *  Write annotations
			 */
			if (VERBOSE)
				System.out.println("Writing "+NUM_ENTRIES+" to file system");
			_tileIO.writeTiles(BASE_PATH, _tileSerializer, AnnotationTile.convertToRaw(tiles) );
			_dataIO.writeData(BASE_PATH, _dataSerializer, annotations );
	        
			/*
			 *  Read and check all annotations
			 */
			if (VERBOSE)
				System.out.println( "Reading all annotations" );
			List<AnnotationTile> allTiles = AnnotationTile.convertFromRaw(_tileIO.readTiles(BASE_PATH, _tileSerializer, tileIndices));
			List<AnnotationData<?>> allData = _dataIO.readData( BASE_PATH, _dataSerializer, dataIndices );
			if (VERBOSE) printTiles( allTiles );
			if (VERBOSE) printData( allData );

			if (VERBOSE)
				System.out.println( "Comparing annotations" );
			Assert.assertTrue( compareTiles( allTiles, tiles, true ) );
			Assert.assertTrue( compareData( allData, annotations, true ) );

			if (VERBOSE)
				System.out.println("Removing "+NUM_ENTRIES+" from file system");
			_tileIO.removeTiles(BASE_PATH, tileIndices );
			_dataIO.removeData(BASE_PATH, dataIndices );
	       
			allTiles = AnnotationTile.convertFromRaw(_tileIO.readTiles(BASE_PATH, _tileSerializer, tileIndices));
			allData = _dataIO.readData( BASE_PATH, _dataSerializer, dataIndices );
	    	
			Assert.assertTrue( allTiles.size() == 0 );
			Assert.assertTrue( allData.size() == 0 );

			if (VERBOSE)
				System.out.println( "Complete" );
	    	
	
		} catch (Exception e) {
    		
			System.out.println("Error: " + e.getMessage());
			
		} finally {

			if (VERBOSE)
				System.out.println("Deleting temporary directories");

			try {
				File testDir = new File( ROOT_PATH + BASE_PATH );
				for ( File f : testDir.listFiles() ) {
					f.delete();
				}
				testDir.delete();
			} catch ( Exception e ) {
				// swallow exception
			}
		}
	}
}
