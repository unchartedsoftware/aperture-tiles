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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Comparator;

import com.oculusinfo.binning.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.binning.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;


public class AnnotationManipulator {
	
	
	static private class ReferenceComparator implements Comparator< Pair<String, Long> > {
	    @Override
	    public int compare( Pair<String, Long> a, Pair<String, Long> b ) {	    	
	    	// java sorts in ascending order, we want descending ( new references first )
	    	// so we negate the compareTo
	    	return -a.getSecond().compareTo( b.getSecond() );
	    }
	}
	
    public static final int NUM_BINS = 8;

    
    static public boolean isTileEmpty( TileData<Map<String, List<Pair<String, Long>>>> tile ) {
    	
    	synchronized( tile ) {
    		
    		for ( Map<String, List<Pair<String, Long>>> bin : tile.getData()  ) {
    			if ( bin != null ) {
    				return false;
    			}
    		}
    		return true;
    	}
    	
    }
    
    static public void addDataToBin( Map<String, List<Pair<String, Long>>> bin, AnnotationData<?> data ) {
    	
    	synchronized( bin ) {
	    	String priority = data.getPriority();
	    	Pair<String, Long> reference =  data.getReference();   	
	    	List< Pair<String, Long> > entries;
	    	
	    	if ( bin.containsKey( priority ) ) {
	    		entries = bin.get( priority ); 
	    		if ( !entries.contains( reference ) ) {
	    			entries.add( reference );
	    		}   
	    	} else {
	    		entries = new LinkedList<>();
	    		entries.add( reference );
	    		bin.put( priority, entries );
	    	}
	
	    	// sort references after insertion... maybe instead use a SortedSet?
	    	Collections.sort( entries, new ReferenceComparator() );
    	}
    }
      
    
    static public boolean removeDataFromBin( Map<String, List<Pair<String, Long>>> bin, AnnotationData<?> data ) { 
    	
    	synchronized( bin ) {
    	
	    	String priority = data.getPriority();
	    	Pair<String, Long> reference =  data.getReference();   	
	    	boolean removedAny = false;
	    	
	    	if ( bin.containsKey( priority ) ) {
	    		
	    		List< Pair<String, Long> > entries = bin.get( priority );	    		
	    		if ( entries.contains( reference ) ) {
	    			entries.remove( reference );
	    			removedAny = true;
	    		}     		   		
	    		if ( entries.size() == 0 ) {
		    		// remove references for priority
	    			bin.remove( priority );
		    	}
	    	} 
	
	    	return removedAny;
    	}
    }

    
    
    
    
    static public void addDataToTile( TileData<Map<String, List<Pair<String, Long>>>> tile, BinIndex binIndex, AnnotationData<?> data ) {   	
    	
    	synchronized( tile ) {
    		
    		Map<String, List<Pair<String, Long>>> bin = tile.getBin( binIndex.getX(), binIndex.getY() );
    		
    		if ( bin != null ) {
    			addDataToBin( bin, data );
    		} else {
    			
    			Map<String, List<Pair<String, Long>>> newBin = new LinkedHashMap<>();
    			addDataToBin( newBin, data );     			 
    			tile.setBin( binIndex.getX(), binIndex.getY(), newBin );
    		}
        	
    	}    		   	
    }
    
    
    static public void removeDataFromTile( TileData<Map<String, List<Pair<String, Long>>>> tile, BinIndex binIndex, AnnotationData<?> data ) { 
    	
    	synchronized( tile ) {
    		
    		Map<String, List<Pair<String, Long>>> bin = tile.getBin( binIndex.getX(), binIndex.getY() );  		
        	if ( bin != null && removeDataFromBin( bin, data ) ) {
    			// remove bin if empty
    			if ( bin.size() == 0 ) {    				   				
    				tile.setBin( binIndex.getX(), binIndex.getY(), null );
    			}
    		}       	
    	}       	
    }

   
    static public List<Pair<String, Long>> getReferencesFromBin( Map<String, List<Pair<String, Long>>> bin, String priority ) {
    	
    	synchronized( bin ) {
    		
	    	if ( bin.containsKey( priority ) ) {
	    		return bin.get( priority );
	    	} else {
	    		return new LinkedList<>();
	    	}
    	}
    	
    }
    
    
    static public List<Pair<String, Long>> getAllReferencesFromBin( Map<String, List<Pair<String, Long>>> bin ) {
    	
    	synchronized( bin ) {
    		List<Pair<String, Long>> allReferences = new LinkedList<>();  
        	// for each priority group in a bin
    		for ( List<Pair<String, Long>> references : bin.values() ) {
    			allReferences.addAll( references );
    		}	
        	return allReferences;
    	}
    	
    }  
    
    
    
    static public List<Pair<String, Long>> getAllReferencesFromTile( TileData<Map<String, List<Pair<String, Long>>>> tile ) {
    	
    	synchronized( tile ) {
    		
	    	List<Pair<String, Long>> allReferences = new LinkedList<>();  
	    	// for each bin
			for ( Map<String, List<Pair<String, Long>>> bin : tile.getData() ) {
				
				if (bin != null) {
					// get all references
					allReferences.addAll( getAllReferencesFromBin( bin ) );
				}
				
			} 	
	    	return allReferences;
    	}
    }
       
    
    static public List<Pair<String, Long>> getFilteredReferencesFromTile( TileData<Map<String, List<Pair<String, Long>>>> tile, Map<String, Integer> filter ) {
    	
    	synchronized( tile ) {
	    	List<Pair<String, Long>> filtered = new LinkedList<>();
	    	// for each bin
	    	for ( Map<String, List<Pair<String, Long>>> bin : tile.getData() ) {
	    		
				// go through filter list get references by priority and by count
				for (Map.Entry<String, Integer> f : filter.entrySet() ) {
					
					String priority = f.getKey();
					Integer count = f.getValue();
					
					List<Pair<String, Long>> references = getReferencesFromBin( bin, priority );
					
					// references are sorted, so simply cut the tail off to get the n newest
					filtered.addAll( references.subList( 0, count < references.size() ? count : references.size() ) );
				}
	    	}
			return filtered;
    	}
    }    
    
    
    
    static public JSONObject referenceToJSON( Pair<String, Long> reference ) {
    	
    	JSONObject json = new JSONObject();
    	try {		   	
    		json.put( "uuid", reference.getFirst().toString() );
    		json.put( "timestamp", reference.getSecond().toString() );		    
    	} catch ( Exception e ) {
    		e.printStackTrace();
    	}
    	
    	return json;
    }
    
    
    static public Pair<String, Long> getReferenceFromJSON( JSONObject json ) throws IllegalArgumentException {
    	
    	try {
			
    		UUID uuid = UUID.fromString( json.getString("uuid") );
    		Long timestamp = Long.parseLong( json.getString("timestamp") );
    		
    		return new Pair<String, Long>( uuid.toString(), timestamp );
		    
    	} catch ( Exception e ) {
    		throw new IllegalArgumentException( e );
    	}

    }
    
    
    static public Map<String, List<Pair<String, Long>>> getBinFromJSON( JSONObject json ) throws IllegalArgumentException {
    	
    	try {

    		Map<String, List<Pair<String, Long>>> references =  new LinkedHashMap<>();
        	
        	Iterator<?> priorities = json.keys();
            while( priorities.hasNext() ){
            	
                String priority = (String)priorities.next();
                
                if( json.get(priority) instanceof JSONArray ) {
                
	            	JSONArray jsonReferences = json.getJSONArray(priority);
	            	
	            	List<Pair<String, Long>> referenceList = new LinkedList<>();	            	
	            	for (int i=0; i<jsonReferences.length(); i++) {
	            		
	            		JSONObject jsonRef = jsonReferences.getJSONObject( i );
	            		referenceList.add( getReferenceFromJSON( jsonRef ) );            		
	            	}           	
	            	references.put( priority, referenceList );	
                }
                
            }	        
    	    return references;
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e );
		}

    }
    
    
    static public JSONObject binToJSON( Map<String, List<Pair<String, Long>>> bin ) {
    	
    	JSONObject binJSON = new JSONObject();
    	try {

	    	// for each priority group in a bin
		    for (Map.Entry<String, List<Pair<String, Long>>> referenceEntry : bin.entrySet() ) {		    	
		    	
		    	String priority = referenceEntry.getKey();
		    	List<Pair<String, Long>> references = referenceEntry.getValue();
		    	
		    	JSONArray referenceJSON = new JSONArray();
		    	for ( Pair<String, Long> reference : references ) {
		    		referenceJSON.put( referenceToJSON( reference ) );
		    	}
		    	
		    	// add priority to bin json object
		    	binJSON.put( priority, referenceJSON );		    	
		    }
		    
    	} catch ( Exception e ) {
    		e.printStackTrace();
    	}
    	
    	return binJSON;
    }
    
    
    
    
    
    static public TileData<Map<String, List<Pair<String, Long>>>> getTileFromJSON( JSONObject json ) throws IllegalArgumentException {
    	
		try {
			
			TileIndex index = new TileIndex( json.getInt("level"),
											 json.getInt("x"),
											 json.getInt("y"), 
											 AnnotationIndexer.NUM_BINS, 
											 AnnotationIndexer.NUM_BINS );
			
			// create tile with empty bins
			TileData<Map<String, List<Pair<String, Long>>>> tile = new TileData<>( index );
			
			// for all binkeys
	        Iterator<?> binKeys = json.keys();
	        while( binKeys.hasNext() ) {
	        	
	        	String binKey = (String)binKeys.next();
	            
	            if( json.get(binKey) instanceof JSONObject ){
	            	
	            	JSONObject bin = (JSONObject)json.get(binKey);            	
	            	BinIndex binIndex = BinIndex.fromString( binKey );	
	            	tile.setBin( binIndex.getX(), binIndex.getY(), getBinFromJSON( bin ));
	            }
	        }
			
			return tile;
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e );
		}		
    	
    }
    
    
    static public JSONObject tileToJSON( TileData<Map<String, List<Pair<String, Long>>>> tile ) {
    	
    	JSONObject tileJSON = new JSONObject();
		
		try {
			
			tileJSON.put("level", tile.getDefinition().getLevel() );
			tileJSON.put("x", tile.getDefinition().getX() );
			tileJSON.put("y", tile.getDefinition().getY() );
			
			for (int i=0; i<tile.getDefinition().getXBins(); i++ ) {
				for (int j=0; j<tile.getDefinition().getYBins(); j++ ) {
					
					Map<String, List<Pair<String, Long>>> bin = tile.getBin( i, j );
					
					if ( bin != null) {
						// add bin object to tile
					    tileJSON.put( new BinIndex(i, j).toString(), binToJSON( bin ) );
					}
					
				}
			}

		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		return tileJSON;
    	
    }
	
    
}
