/**
 * Copyright (c) 2014 Oculus Info Inc. 
 * http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.annotation.io.serialization.impl;

import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.binning.*;


public class JSONTileSerializer extends GenericJSONSerializer<AnnotationTile>{
	
    private static final long serialVersionUID = -6779123604244971240L;

    public JSONTileSerializer() {
		super();
	}

	@Override
    protected JSONObject translateToJSON ( AnnotationTile value ) {
		
		return value.toJSON();
		
		/*
		JSONObject tileJSON = new JSONObject();
		
		try {
			
			tileJSON.put("level", value.getIndex().getLevel() );
			tileJSON.put("x", value.getIndex().getX() );
			tileJSON.put("y", value.getIndex().getY() );
			
			// for each bin
			for (Map.Entry<BinIndex, AnnotationBin> binEntry : value.getBins().entrySet() ) {
								
				BinIndex key = binEntry.getKey();
				AnnotationBin bin = binEntry.getValue();
			    
				JSONObject binJSON = new JSONObject();
				
				// for each priority group in a bin
			    for (Map.Entry<String, List<Long>> referenceEntry : bin.getReferences().entrySet() ) {
			    	
			    	String priority = referenceEntry.getKey();
			    	List<Long> references = referenceEntry.getValue();
			    	
			    	JSONArray referenceJSON = new JSONArray();
			    	for ( Long reference : references ) {
			    		referenceJSON.put( reference );
			    	}
			    	
			    	// add priority to bin json object
			    	binJSON.put( priority, referenceJSON );
			    	
			    }
			    
			    // add bin object to tile
			    tileJSON.put( key.toString(), binJSON );

			}

		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		return tileJSON;
		*/
	}

	
	@Override
	protected AnnotationTile getValue(Object obj) throws JSONException {
		
		
		JSONObject tileJSON = (JSONObject)obj;
		
		Map<BinIndex, AnnotationBin> bins =  new LinkedHashMap<>();		

		try {
			TileIndex index = new TileIndex( tileJSON.getInt("level"),
											 tileJSON.getInt("x"),
											 tileJSON.getInt("y"), 
											 AnnotationTile.NUM_BINS, 
											 AnnotationTile.NUM_BINS );
			// for all binkeys
	        Iterator<?> binKeys = tileJSON.keys();
	        while( binKeys.hasNext() ) {
	        	
	        	String binKey = (String)binKeys.next();
	            
	            if( tileJSON.get(binKey) instanceof JSONObject ){
	            	
	            	JSONObject bin = (JSONObject)tileJSON.get(binKey);
	            	
	            	BinIndex binIndex = BinIndex.fromString( binKey );
	            		            	
	            	Map<String, List<Long>> references =  new LinkedHashMap<>();
	            	
	            	// for all priorities
	            	Iterator<?> priorities = bin.keys();
	     	        while( priorities.hasNext() ){
	     	            String priority = (String)priorities.next();
	     	            if( bin.get(priority) instanceof JSONArray ) {
	     	            	
	     	            	// get references
	     	            	JSONArray referenceArr = bin.getJSONArray( priority );
	     	            	List<Long> referenceList = new LinkedList<>();
	     	            	
	     	            	for (int i=0; i<referenceArr.length(); i++) {
	     	            		referenceList.add( referenceArr.getLong(i) );
	     	            	}
	     	            	references.put( priority, referenceList );
	     	            	
	     	            }
	     	        }
	     	        
	     	        bins.put( binIndex, new AnnotationBin( binIndex, references ) );

	            }
	        }
			
			return new AnnotationTile( index, bins );
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		
		return (AnnotationTile)obj;
	}
}
