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
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.Date;

import java.sql.Timestamp;

import com.oculusinfo.binning.*;
import com.oculusinfo.annotation.impl.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileAndBinIndices;

import org.json.JSONException;
import org.json.JSONObject;


public class AnnotationTestsBase {
	
	static final String	  TEST_LAYER_NAME = "test.annotations";
	static final double   EPSILON = 0.00001;
	static final int      NUM_ENTRIES = 10;
	static final int      NUM_TESTS = 25;
	static final double[] BOUNDS = {-180.0+EPSILON, -85.05+EPSILON, 180.0-EPSILON, 85.05-EPSILON};


	/*
	 * Annotation list printing utility functions
	 */
	protected void printData( Map<BinIndex, List<AnnotationData<?>>> dataMap ) {
		
		for ( List<AnnotationData<?>> annotations : dataMap.values() ) {	
			printData( annotations );
		}
	}
	protected void printData( List<AnnotationData<?>> annotations ) {
		
		for ( AnnotationData<?> annotation : annotations ) {				
			try {
				System.out.println( annotation.toJSON().toString( 4 ) );	
			} catch ( Exception e ) {
				e.printStackTrace();
			}	
		}
	}

	protected void printTiles( List<AnnotationTile> tiles ) {
		
		for ( AnnotationTile tile : tiles ) {
			try {
				System.out.println( tile.toJSON().toString( 4 ) );	
			} catch ( Exception e ) { e.printStackTrace(); }
					
		}
	}
	
	
	protected boolean compareData( List<AnnotationData<?>> as, List<AnnotationData<?>> bs, boolean verbose ) {
		
		for ( AnnotationData<?> a : as ) {
			int foundCount = 0;
			for ( AnnotationData<?> b : bs ) {				
				if ( compareData(a, b, false ) ) {
					foundCount++;
				}
			}	
			if ( foundCount != 1 ) {
				if ( verbose ) System.out.println( "Data lists do not match" );
				return false;
			}
		}
		return true;
	}
	
	protected boolean compareTiles( List<AnnotationTile> as, List<AnnotationTile> bs, boolean verbose ) {
		
		for ( AnnotationTile a : as ) {
			int foundCount = 0;
			for ( AnnotationTile b : bs ) {				
				if ( compareTiles(a, b, false) ) {
					foundCount++;
				}
			}	
			if ( foundCount != 1 ) {
				if ( verbose ) System.out.println( "Tile lists do not match" );
				return false;
			}
		}
		return true;
	}
	
	
	protected boolean compareData( AnnotationData<?> a, AnnotationData<?> b, boolean verbose ) {
		
		if ( !a.getUUID().equals( b.getUUID() ) ) {
			if ( verbose ) System.out.println( "UUID are not equal" );
			return false;
		}
		
		if ( a.getX() != null && b.getX() != null ) {
			if ( !a.getX().equals( b.getX() ) ) {
				if ( verbose ) System.out.println( "X values are not equal" );
				return false;
			}
		}		
		if ( a.getY() != null && b.getY() != null ) {
			if ( !a.getY().equals( b.getY() ) ) {
				if ( verbose ) System.out.println( "Y values are not equal" );
				return false;
			}
		}
		
		if ( !a.getLevel().equals( b.getLevel() ) ) {
			if ( verbose ) System.out.println( "Level values are not equal" );
			return false;
		}
		
		if ( !a.getData().toString().equals( b.getData().toString() ) ) {
			if ( verbose ) System.out.println( "Data objects are not equal" );
			return false;
		}		
		return true;
	}
	
	
	protected boolean compareTiles( AnnotationTile a, AnnotationTile b, boolean verbose ) {
		
		List<UUID> aReferences = a.getAllReferences();
		List<UUID> bReferences = b.getAllReferences();
		
		if ( !a.getIndex().equals( b.getIndex() ) ) {
			if ( verbose ) System.out.println( "Bin indices are not equal");
			return false;
		}
		
		if ( aReferences.size() != bReferences.size() ) {
			if ( verbose ) System.out.println( "Reference counts are not equal");
			return false;		
		}
			
		for ( UUID aRef : aReferences ) {
			int foundCount = 0;
			for ( UUID bRef : bReferences ) {
				if ( aRef.equals( bRef ) ) {
					foundCount++;
				}
			}		
			if ( foundCount != 1 ) {
				if ( verbose ) System.out.println( "Reference lists are not equal");
				return false;
			}			
		}

		return true;
	}
	
	
	/*
	 * Annotation index generation function
	 */
	protected JSONObject generateJSON() {

		final Random rand = new Random();
		
		double [] xy = randomPosition();
		
		Date date = new Date();
		Long timestamp = new Timestamp( date.getTime() ).getTime();
		
		try {
			JSONObject anno = new JSONObject();			
			anno.put("x", xy[0]);
			anno.put("y", xy[1]);	
			anno.put("level", (int)(rand.nextDouble() * 18) );
			anno.put("priority", randomPriority() );
			anno.put("uuid", UUID.randomUUID() );
			anno.put("timestamp", timestamp.toString() );
			JSONObject data = new JSONObject();
			data.put("comment", randomComment() );
			anno.put("data", data);
			return anno;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
				
	}
	
	/*
	 * Annotation index generation function
	 */
	protected AnnotationData<?> generateJSONAnnotation() {

		return JSONAnnotation.fromJSON( generateJSON() );
				
	}
	
	protected List<AnnotationTile> generateTiles( int numEntries, AnnotationIndexer indexer ) {
		
		List<AnnotationData<?>> annotations = generateJSONAnnotations( numEntries );
		

		Map<TileIndex, AnnotationTile> tiles = new HashMap<>();
				
		for ( AnnotationData<?> annotation : annotations ) {
			List<TileAndBinIndices> indices = indexer.getIndices( annotation );
			
			for ( TileAndBinIndices index : indices ) {
				
				TileIndex tileIndex = index.getTile();
				BinIndex binIndex = index.getBin();	
				
				if ( tiles.containsKey(tileIndex) ) {
					tiles.get( tileIndex ).add( binIndex, annotation );
				} else {
					AnnotationTile tile = new AnnotationTile( tileIndex );
					tile.add( binIndex, annotation );
					tiles.put( tileIndex, tile );
				}
			}
		
		}
		return new ArrayList<>( tiles.values() );
	}
	
	
	protected double[] randomPosition() {
		
		final Random rand = new Random();
		double [] xy = new double[2];
		xy[0] = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		xy[1] = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));		
		
		int univariateCase = (int)(rand.nextDouble() * 10);
		switch (univariateCase) {
		
			case 0: 
				xy[1] = -1;
				break;
			case 1: 
				xy[0] = -1;
				break;
		}				
		return xy;		
	}
	
	
	protected String randomPriority() {
		
		final Random rand = new Random();
		String priorities[] = {"Urgent", "High", "Medium", "Low"};		
		int priorityIndex = (int)(rand.nextDouble() * 3);
		return priorities[ priorityIndex ];
	}
	
	
	protected String randomComment() {
		int LENGTH = 256;		
		Random rng = new Random();
		String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	    char[] text = new char[LENGTH];
	    for (int i = 0; i < LENGTH; i++)
	    {
	        text[i] = CHARACTERS.charAt(rng.nextInt(CHARACTERS.length()));
	    }
	    return new String(text);
	}
	
		
	protected List<TileIndex> tilesToIndices( List<AnnotationTile> tiles ) {
		List<TileIndex> indices = new ArrayList<>();
		for ( AnnotationTile tile : tiles ) {
			indices.add( tile.getIndex() );
		}
		return indices;
	}

	protected List<UUID> dataToIndices( List<AnnotationData<?>> data ) {
		List<UUID> indices = new ArrayList<>();
		for ( AnnotationData<?> d : data ) {
			indices.add( d.getUUID() );
		}
		return indices;
	}
	
	protected List<AnnotationData<?>> generateJSONAnnotations( int numEntries ) {

		List<AnnotationData<?>> annotations = new ArrayList<>();		
		for (int i=0; i<numEntries; i++) {
				
			annotations.add( generateJSONAnnotation() );	
		}
		return annotations;
	}
	
	protected List<JSONObject> generateJSONs( int numEntries ) {

		List<JSONObject> annotations = new ArrayList<>();		
		for (int i=0; i<numEntries; i++) {
				
			annotations.add( generateJSON() );	
		}
		return annotations;
	}

	
}
