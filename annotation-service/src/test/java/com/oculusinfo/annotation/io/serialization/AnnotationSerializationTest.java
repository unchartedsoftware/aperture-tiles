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
package com.oculusinfo.annotation.io.serialization;

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.AnnotationTile;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.annotation.index.impl.AnnotationIndexerImpl;
import com.oculusinfo.annotation.util.AnnotationGenerator;
import com.oculusinfo.annotation.util.AnnotationUtil;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.io.serialization.impl.StringLongPairArrayMapJsonSerializer;
import com.oculusinfo.factory.util.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AnnotationSerializationTest {

    static final double [] BOUNDS = { 180, 85.05, -180, -85.05};
    static String [] GROUPS = {"Urgent", "High", "Medium", "Low"};
    static final int NUM_ENTRIES = 50;
	private TileSerializer<Map<String, List<Pair<String, Long>>>> _tileSerializer;
	private AnnotationSerializer _dataSerializer;
	private TilePyramid _pyramid;
	private AnnotationIndexer _indexer;
	
	@Before
	public void setup () {
		_pyramid = new WebMercatorTilePyramid();
		_indexer = new AnnotationIndexerImpl();
		_tileSerializer = new StringLongPairArrayMapJsonSerializer();
		_dataSerializer = new JSONAnnotationDataSerializer();
	}

	@After
	public void teardown () {
		_indexer = null;
		_tileSerializer = null;
		_dataSerializer = null;    	
	}

	@Test
	public void annotationDataJSONSerializationTest () throws Exception {

        AnnotationGenerator generator = new AnnotationGenerator( BOUNDS, GROUPS );

		List<AnnotationData<?>> before = generator.generateJSONAnnotations( NUM_ENTRIES );
		List<AnnotationData<?>> after = new ArrayList<>();
			
		AnnotationUtil.printData( before );

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
            
			Assert.assertTrue( AnnotationUtil.compareData( annotation, anno ) );
		}

        AnnotationUtil.printData( after );
	}
	
	
	@Test
	public void annotationTileJSONSerializationTest () throws Exception {

        AnnotationGenerator generator = new AnnotationGenerator( BOUNDS, GROUPS );

		List<AnnotationTile> before = generator.generateTiles( generator.generateJSONAnnotations( NUM_ENTRIES ), _indexer, _pyramid );
		List<AnnotationTile> after = new ArrayList<>();

		AnnotationUtil.printTiles( before );

		for ( AnnotationTile tile : before ) {
			
			// serialize
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			_tileSerializer.serialize( tile.getRawData(), baos );
			baos.close();
			baos.flush();
            
			// de-serialize
			byte[] data = baos.toByteArray();

			ByteArrayInputStream bais = new ByteArrayInputStream(data);
            AnnotationTile t = new AnnotationTile( _tileSerializer.deserialize( null, bais ) );
			after.add( t );
			bais.close();   
		}
				
		AnnotationUtil.printTiles( after );
		
		Assert.assertTrue( AnnotationUtil.compareTiles( before, after ) );
	}
}
