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
import java.util.Random;

import com.oculusinfo.annotation.query.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.impl.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class AnnotationTestsBase {
	
	static final double   EPSILON = 0.00001;
	static final int      NUM_ENTRIES = 50000;
	static final int      NUM_TESTS = 25;
	static final double[] BOUNDS = {-180.0+EPSILON, -85.05+EPSILON, 180.0-EPSILON, 85.05-EPSILON};
	static final String   TABLE_NAME = "AnnotationTable";
	

	/*
	 * Annotation list printing utility function
	 */
	protected <T> void print( List<AnnotationBin<T>> annotations ) {
		
		for (AnnotationBin<T> annotation : annotations ) {
			
			System.out.println( "{" );
			System.out.println( "\tindex: " + annotation.getIndex().getValue() + ", " );
			System.out.println( "\tdata: [" );
			
			for (T data : annotation.getData() ) {
				try {
					/*
					System.out.println( "\t\t{" );
					System.out.println( "\t\t\tx: " + data.getDouble("x") + "," );
					System.out.println( "\t\t\ty: " + data.getDouble("y") + "," );
					System.out.println( "\t\t\tpriority: " + data.getString("comment") + "," );			
					System.out.println( "\t\t\tcomment: " + data.getString("priority") );					
					System.out.println( "\t\t}," );
					*/
					System.out.println( data.toString() );
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			System.out.println( "\t]" );
			System.out.println( "}" );
		}
	}

	
	/*
	 * Annotation index generation function
	 */
	protected JSONObject generateJSON() {

		final Random rand = new Random();
		
		double x = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
		
		try {
			// generate JSON data array
			JSONObject data = new JSONObject();
			data.put("x", x);
			data.put("y", y);
			data.put("comment", randomComment() );
			data.put("priority", "P0");	
			return data;
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
				
	}
	
	/*
	 * Annotation index generation function
	 */
	protected <T> AnnotationBin<JSONObject> generateJSONAnnotation( AnnotationIndexer<JSONObject> indexer ) {

		final Random rand = new Random();
		
		double x = BOUNDS[0] + (rand.nextDouble() * (BOUNDS[2] - BOUNDS[0]));
		double y = BOUNDS[1] + (rand.nextDouble() * (BOUNDS[3] - BOUNDS[1]));
		
		JSONObject json = new JSONObject();	
		try {
			json.put("x", x);
			json.put("y", y);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// generate bin index
		AnnotationIndex index = indexer.getIndex( json, 0 );
		
		try {
			// generate JSON data array
			JSONObject data = new JSONObject();
			data.put("x", x);
			data.put("y", y);
			data.put("comment", randomComment() );
			data.put("priority", "P0");	
			JSONArray dataArray = new JSONArray();
			dataArray.put( data );
			
			JSONObject bin = new JSONObject();
			// add index to json
			bin.put( "index", index );
			// add data to json
			bin.put( "data", dataArray );
			return new AnnotationBin<JSONObject>( index, data );
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
				
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
	
	protected <T> List<AnnotationIndex> convertToIndices( List<AnnotationBin<T>> annotations ) {
		List<AnnotationIndex> indices = new ArrayList<AnnotationIndex>();
		for ( AnnotationBin<T> annotation : annotations ) {
			indices.add( annotation.getIndex() );
		}
		return indices;
	}

	protected List<AnnotationBin<JSONObject>> generateJSONAnnotations(int numEntries, AnnotationIndexer<JSONObject> indexer ) {

		List<AnnotationBin<JSONObject>> annotations = new ArrayList<>();		
		for (int i=0; i<numEntries; i++) {
				
			annotations.add( generateJSONAnnotation( indexer ) );	
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
