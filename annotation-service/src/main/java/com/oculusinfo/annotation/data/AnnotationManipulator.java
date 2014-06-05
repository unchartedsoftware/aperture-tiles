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
package com.oculusinfo.annotation.data;

import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;


public class AnnotationManipulator {
	
	public static final int NUM_BINS = 8;
	
	static private class CertificateComparator implements Comparator< Pair<String, Long> > {
	    @Override
	    public int compare( Pair<String, Long> a, Pair<String, Long> b ) {	    	
	    	// java sorts in ascending order, we want descending ( new certificates first )
	    	// so we negate the compareTo
	    	return -a.getSecond().compareTo( b.getSecond() );
	    }
	}
	

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
	    	String group = data.getGroup();
	    	Pair<String, Long> certificate =  data.getCertificate();
	    	List< Pair<String, Long> > entries;
	    	
	    	if ( bin.containsKey( group ) ) {
	    		entries = bin.get( group ); 
	    		if ( !entries.contains( certificate ) ) {
	    			entries.add( certificate );
	    		}   
	    	} else {
	    		entries = new LinkedList<>();
	    		entries.add( certificate );
	    		bin.put( group, entries );
	    	}
	
	    	// sort certificates after insertion... maybe instead use a SortedSet?
	    	Collections.sort( entries, new CertificateComparator() );
    	}
    }
      
    
    static public boolean removeDataFromBin( Map<String, List<Pair<String, Long>>> bin, AnnotationData<?> data ) { 
    	
    	synchronized( bin ) {
    	
	    	String group = data.getGroup();
	    	Pair<String, Long> certificate =  data.getCertificate();
	    	boolean removedAny = false;
	    	
	    	if ( bin.containsKey( group ) ) {
	    		
	    		List< Pair<String, Long> > entries = bin.get( group );	    		
	    		if ( entries.contains( certificate ) ) {
	    			entries.remove( certificate );
	    			removedAny = true;
	    		}     		   		
	    		if ( entries.size() == 0 ) {
		    		// remove certificates for group
	    			bin.remove( group );
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


    static public List<Pair<String, Long>> getCertificatesFromBin( Map<String, List<Pair<String, Long>>> bin, String group ) {
    	
    	synchronized( bin ) {
    		
	    	if ( bin.containsKey( group ) ) {
	    		return bin.get( group );
	    	} else {
	    		return new LinkedList<>();
	    	}
    	}
    	
    }
    
    
    static public List<Pair<String, Long>> getAllCertificatesFromBin( Map<String, List<Pair<String, Long>>> bin ) {
    	
    	synchronized( bin ) {
    		List<Pair<String, Long>> allCertificates = new LinkedList<>();  
        	// for each group group in a bin
    		for ( List<Pair<String, Long>> certificates : bin.values() ) {
    			allCertificates.addAll( certificates );
    		}	
        	return allCertificates;
    	}
    	
    }  
    
    
    
    static public List<Pair<String, Long>> getAllCertificatesFromTile( TileData<Map<String, List<Pair<String, Long>>>> tile ) {
    	
    	synchronized( tile ) {
    		
	    	List<Pair<String, Long>> allCertificates = new LinkedList<>();  
	    	// for each bin
			for ( Map<String, List<Pair<String, Long>>> bin : tile.getData() ) {
				
				if (bin != null) {
					// get all certificates
					allCertificates.addAll( getAllCertificatesFromBin( bin ) );
				}
				
			} 	
	    	return allCertificates;
    	}
    }
       
    
    static public List<Pair<String, Long>> getFilteredCertificatesFromTile( TileData<Map<String, List<Pair<String, Long>>>> tile, Map<String, Integer> filter ) {
    	
    	synchronized( tile ) {
	    	List<Pair<String, Long>> filtered = new LinkedList<>();
	    	// for each bin
	    	for ( Map<String, List<Pair<String, Long>>> bin : tile.getData() ) {
	    		
	    		if (bin != null) {
					// go through filter list get certificates by group and by count
					for (Map.Entry<String, Integer> f : filter.entrySet() ) {
						
						String group = f.getKey();
						Integer count = f.getValue();
						
						List<Pair<String, Long>> certificates = getCertificatesFromBin( bin, group );
						
						// certificates are sorted, so simply cut the tail off to get the n newest
						filtered.addAll( certificates.subList( 0, count < certificates.size() ? count : certificates.size() ) );
					}
	    		}
	    	}
			return filtered;
    	}
    }    
    
    
    
    static public JSONObject certificateToJSON( Pair<String, Long> certificate ) {
    	
    	JSONObject json = new JSONObject();
    	try {		   	
    		json.put( "uuid", certificate.getFirst() );
    		json.put( "timestamp", certificate.getSecond().toString() );		    
    	} catch ( Exception e ) {
    		e.printStackTrace();
    	}
    	
    	return json;
    }
    
    
    static public Pair<String, Long> getCertificateFromJSON( JSONObject json ) throws IllegalArgumentException {
    	
    	try {
			
    		UUID uuid = UUID.fromString( json.getString("uuid") );
    		Long timestamp = Long.parseLong( json.getString("timestamp") );
    		
    		return new Pair<>( uuid.toString(), timestamp );
		    
    	} catch ( Exception e ) {
    		throw new IllegalArgumentException( e );
    	}

    }
    
    
    static public Map<String, List<Pair<String, Long>>> getBinFromJSON( JSONObject json ) throws IllegalArgumentException {
    	
    	try {

    		Map<String, List<Pair<String, Long>>> certificates =  new LinkedHashMap<>();
        	
        	Iterator<?> priorities = json.keys();
            while( priorities.hasNext() ){
            	
                String group = (String)priorities.next();
                
                if( json.get(group) instanceof JSONArray ) {
                
	            	JSONArray jsonCertificates = json.getJSONArray(group);
	            	
	            	List<Pair<String, Long>> certificateList = new LinkedList<>();	            	
	            	for (int i=0; i<jsonCertificates.length(); i++) {
	            		
	            		JSONObject jsonRef = jsonCertificates.getJSONObject( i );
	            		certificateList.add( getCertificateFromJSON( jsonRef ) );            		
	            	}           	
	            	certificates.put( group, certificateList );	
                }
                
            }	        
    	    return certificates;
			
		} catch ( Exception e ) {
			throw new IllegalArgumentException( e );
		}

    }
    
    
    static public JSONObject binToJSON( Map<String, List<Pair<String, Long>>> bin ) {
    	
    	JSONObject binJSON = new JSONObject();
    	try {

	    	// for each group group in a bin
		    for (Map.Entry<String, List<Pair<String, Long>>> certificateEntry : bin.entrySet() ) {		    	
		    	
		    	String group = certificateEntry.getKey();
		    	List<Pair<String, Long>> certificates = certificateEntry.getValue();
		    	
		    	JSONArray certificateJSON = new JSONArray();
		    	for ( Pair<String, Long> certificate : certificates ) {
		    		certificateJSON.put( certificateToJSON( certificate ) );
		    	}
		    	
		    	// add group to bin json object
		    	binJSON.put( group, certificateJSON );		    	
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
