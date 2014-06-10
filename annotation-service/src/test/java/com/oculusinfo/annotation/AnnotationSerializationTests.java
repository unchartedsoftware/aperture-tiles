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
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.index.impl.AnnotationIndexerImpl;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.impl.JSONAnnotationDataSerializer;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringLongPairArrayMapJSONSerializer;
import com.oculusinfo.binning.util.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AnnotationSerializationTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;
	
	private TileSerializer<Map<String, List<Pair<String, Long>>>> _tileSerializer;
	private AnnotationSerializer _dataSerializer;
	private TilePyramid _pyramid;
	private AnnotationIndexer _indexer;
	
    @Before
    public void setup () {
	    _pyramid = new WebMercatorTilePyramid();
    	_indexer = new AnnotationIndexerImpl();
    	_tileSerializer = new StringLongPairArrayMapJSONSerializer();
    	_dataSerializer = new JSONAnnotationDataSerializer();
    }

    @After
    public void teardown () {
    	_indexer = null;
    	_tileSerializer = null;
    	_dataSerializer = null;    	
    }

    @Test
    public void testDataJSONSerialization () throws Exception {
    	
		List<AnnotationData<?>> before = generateJSONAnnotations( NUM_ENTRIES );
		List<AnnotationData<?>> after = new ArrayList<>();
			
		if (VERBOSE) {
			System.out.println( "*** Before ***");
			printData( before );
		}
		
		for ( AnnotationData<?> annotation : before ) {
			
			// serialize
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			_dataSerializer.serialize( annotation, baos );
			baos.close();
            baos.flush();
            
            // deserialize
            byte[] data = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            AnnotationData<?> anno = _dataSerializer.deserialize( bais );
            after.add( anno );
            bais.close();
            
            Assert.assertTrue( compareData( annotation, anno, true ) );
		}
		
		
		if (VERBOSE) {
			System.out.println( "*** After ***");
			printData( after );
		}
    }
	
	
    @Test
    public void testTileJSONSerialization () throws Exception {
    	
    	List<TileData< Map<String, List<Pair<String, Long>>>>> before = generateTiles( generateJSONAnnotations( NUM_ENTRIES ), _indexer, _pyramid );
		List<TileData< Map<String, List<Pair<String, Long>>>>> after = new ArrayList<>();

		if (VERBOSE) {
			System.out.println( "*** Before ***");
			printTiles( before );
		}
		
		for ( TileData< Map<String, List<Pair<String, Long>>>> tile : before ) {
			
			// serialize
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			_tileSerializer.serialize( tile, baos );
			baos.close();
            baos.flush();
            
            // deserialize
            byte[] data = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            TileData< Map<String, List<Pair<String, Long>>>> t = _tileSerializer.deserialize( (TileIndex)null, bais );
            after.add( t );
            bais.close();   
		}
				
		if (VERBOSE) {
			System.out.println( "*** After ***");
			printTiles( after );
		}
		
		Assert.assertTrue( compareTiles( before, after, true ) );
    }
	
}
