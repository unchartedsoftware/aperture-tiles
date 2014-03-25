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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import com.oculusinfo.binning.*;
import com.oculusinfo.annotation.impl.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileAndBinIndices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class AnnotationTestsBase {
	
	static final double   EPSILON = 0.00001;
	static final int      NUM_ENTRIES = 100;
	static final int      NUM_TESTS = 25;
	static final double[] BOUNDS = {-180.0+EPSILON, -85.05+EPSILON, 180.0-EPSILON, 85.05-EPSILON};

	

	/*
	 * Annotation list printing utility function
	 */
	protected void printData( List<AnnotationData> annotations ) {
		
		for ( AnnotationData annotation : annotations ) {
			
			try {
				System.out.println("{");
				if ( annotation.getX() != null )
					System.out.println("    \"x\":" + annotation.getX() + ",");
				if ( annotation.getY() != null )
					System.out.println("    \"y\":" + annotation.getY() + ",");
				System.out.println("    \"priority\":" + annotation.getPriority() + ",");
				System.out.println("}");
			} catch ( Exception e ) {
				e.printStackTrace();
			}	
		}
	}

	
	protected void printTiles( List<AnnotationTile> tiles ) {
		
		for ( AnnotationTile tile : tiles ) {
			
			System.out.println( "{" );
			System.out.println( "    \"level\":" + tile.getIndex().getLevel() + ",");
			System.out.println( "    \"x\":" + tile.getIndex().getX() + ",");
			System.out.println( "    \"y\":" + tile.getIndex().getY() + ",");
			System.out.println( "    bins: {" );
			
			// for each bin
			for (Map.Entry<BinIndex, AnnotationBin> binEntry : tile.getBins().entrySet() ) {
							
				BinIndex key = binEntry.getKey();
				AnnotationBin bin = binEntry.getValue();
			    System.out.print( "        \"" + key.toString() + "\":{");
				// for each priority group in a bin
			    for (Map.Entry<String, List<Long>> referenceEntry : bin.getReferences().entrySet() ) {
			    	
			    	String priority = referenceEntry.getKey();
			    	List<Long> references = referenceEntry.getValue();
			    	
			    	System.out.print( "\"" + priority + "\":[");
			    	
			    	for ( Long reference : references ) {
			    		System.out.print( reference + ",");				    	
			    	}
			    	System.out.print( "]");		    	
			    }
			    System.out.println( "}" );		
			}
			
			System.out.println( "    }" );
		}
	}
	
	/*
	 * Annotation index generation function
	 */
	protected JSONObject generateJSON() {

		final Random rand = new Random();
		
		double x = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
		
		int priority = (int)(rand.nextDouble() * 3);
		int dimCase = (int)(rand.nextDouble() * 10);
		
		try {
			JSONObject anno = new JSONObject();
			
			switch (dimCase) {
				case 0: 
					anno.put("x", x);
					break;
				case 1: 
					anno.put("y", y);
					break;
				default:				
					anno.put("x", x);
					anno.put("y", y);
					break;
			}
					
			anno.put("priority", "P" + priority);	
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
	protected AnnotationData generateJSONAnnotation() {

		return new JSONAnnotation( generateJSON() );
				
	}
	
	protected List<AnnotationTile> generateTiles( int numEntries, AnnotationIndexer<TileAndBinIndices> indexer ) {
		
		List<AnnotationData> annotations = generateJSONAnnotations( numEntries );
		

		Map<TileIndex, AnnotationTile> tiles = new HashMap<>();
				
		for ( AnnotationData annotation : annotations ) {
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

	protected List<Long> dataToIndices( List<AnnotationData> data ) {
		List<Long> indices = new ArrayList<>();
		for ( AnnotationData d : data ) {
			indices.add( d.getIndex() );
		}
		return indices;
	}
	
	protected List<AnnotationData> generateJSONAnnotations( int numEntries ) {

		List<AnnotationData> annotations = new ArrayList<>();		
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
