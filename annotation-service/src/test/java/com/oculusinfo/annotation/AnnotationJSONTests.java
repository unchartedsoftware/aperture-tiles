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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oculusinfo.annotation.impl.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.impl.*;
import com.oculusinfo.binning.util.Pair;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.json.JSONObject;

public class AnnotationJSONTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = false;
	
	private AnnotationIndexer _indexer;
	private TilePyramid _pyramid;
	
    @Before
    public void setup () {
	    _pyramid = new WebMercatorTilePyramid();
    	_indexer = new AnnotationIndexerImpl();
    }

    @After
    public void teardown () {
    	_indexer = null;   	
    	_pyramid = null;
    }


    @Test
    public void testDataJSON () throws Exception {
    	
		List<AnnotationData<?>> before = generateJSONAnnotations( NUM_ENTRIES );
		List<AnnotationData<?>> after = new ArrayList<>();
			
		if (VERBOSE) {
			System.out.println( "*** Before ***");
			printData( before );
		}
		
		for ( AnnotationData<?> annotation : before ) {
			
			JSONObject json = annotation.toJSON();
			after.add( JSONAnnotation.fromJSON( json ) );
		}
		
		if (VERBOSE) {
			System.out.println( "*** After ***");
			printData( after );
		}
		
		Assert.assertTrue( compareData( before, after, false ) );
		
    }
	
	
    @Test
    public void testTileJSONSerialization () throws Exception {
    	
		List<TileData< Map<String, List<Pair<String, Long>>>>> before = generateTiles( NUM_ENTRIES, _indexer, _pyramid );
		List<TileData< Map<String, List<Pair<String, Long>>>>> after = new ArrayList<>();

		if (VERBOSE) {
			System.out.println( "*** Before ***");
			printTiles( before );
		}

		
		for ( TileData< Map<String, List<Pair<String, Long>>>> tile : before ) {
			
			JSONObject json = AnnotationManipulator.tileToJSON( tile );
			after.add( AnnotationManipulator.getTileFromJSON( json ) );
		}
		
		
		if (VERBOSE) {
			System.out.println( "*** After ***");
			printTiles( after );
		}
		
		Assert.assertTrue( compareTiles( before, after, false ) );
    }
	
}
