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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oculusinfo.annotation.impl.JSONAnnotation;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.index.impl.AnnotationIndexerImpl;
import com.oculusinfo.annotation.util.AnnotationGenerator;
import com.oculusinfo.annotation.util.AnnotationUtil;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;

public class JSONAnnotationTest {

    private static double [] BOUNDS = { 180, 85.05, -180, -85.05};
    private static String [] GROUPS = {"Urgent", "High", "Medium", "Low"};
    static final int NUM_ENTRIES = 50;
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
    public void jsonAnnotationToFromTest () throws Exception {

        AnnotationGenerator generator = new AnnotationGenerator( BOUNDS, GROUPS );
		List<AnnotationData<?>> before = generator.generateJSONAnnotations( NUM_ENTRIES );
		List<AnnotationData<?>> after = new ArrayList<>();

		AnnotationUtil.printData( before );
		
		for ( AnnotationData<?> annotation : before ) {
			JSONObject json = annotation.toJSON();
			after.add( JSONAnnotation.fromJSON(json) );
		}
		
		AnnotationUtil.printData( after );
		
		Assert.assertTrue( AnnotationUtil.compareData( before, after ) );
    }


    @Test
    /**
     * Note: this doesn't test any outside functionality but instead ensures that the comparison functions
     * which other annotation tests rely on actually work.
     */
    public void testTileJSONSerialization () throws Exception {

        AnnotationGenerator generator = new AnnotationGenerator( BOUNDS, GROUPS );

		List< AnnotationTile > before = generator.generateTiles( generator.generateJSONAnnotations( NUM_ENTRIES ), _indexer, _pyramid );
		List< AnnotationTile > after = new ArrayList<>();

		AnnotationUtil.printTiles( before );

		for ( AnnotationTile tile : before ) {
			JSONObject json = AnnotationUtil.tileToJSON( tile );
			after.add( AnnotationUtil.getTileFromJSON( json ) );
		}

		AnnotationUtil.printTiles( after );
		
		Assert.assertTrue( AnnotationUtil.compareTiles( before, after ) );
    }
	
}
