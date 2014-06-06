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
import com.oculusinfo.annotation.data.AnnotationManipulator;
import com.oculusinfo.annotation.data.impl.JSONAnnotation;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.*;


public class AnnotationTestsBase {
	
	static final String	  TEST_LAYER_NAME = "annotations-unit-test";
	static final double   EPSILON = 0.001;
	static final int      NUM_ENTRIES = 10;
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

	protected void printTiles( List<TileData< Map<String, List<Pair<String, Long>>>>> tiles ) {
		
		for ( TileData< Map<String, List<Pair<String, Long>>>> tile : tiles ) {
			try {
				System.out.println( tileToJSON(tile).toString( 4 ) );
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
	
	protected boolean compareTiles( List<TileData< Map<String, List<Pair<String, Long>>>>> as, List<TileData< Map<String, List<Pair<String, Long>>>>> bs, boolean verbose ) {
		
		for ( TileData< Map<String, List<Pair<String, Long>>>> a : as ) {
			int foundCount = 0;
			for ( TileData< Map<String, List<Pair<String, Long>>>> b : bs ) {				
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
	
	
	protected boolean compareTiles( TileData< Map<String, List<Pair<String, Long>>>> a, TileData< Map<String, List<Pair<String, Long>>>> b, boolean verbose ) {
		
		List<Pair<String, Long>> aReferences = AnnotationManipulator.getAllCertificatesFromTile( a );
		List<Pair<String, Long>> bReferences = AnnotationManipulator.getAllCertificatesFromTile( b );
		
		if ( !a.getDefinition().equals( b.getDefinition() ) ) {
			if ( verbose ) System.out.println( "Bin indices are not equal");
			return false;
		}
		
		if ( aReferences.size() != bReferences.size() ) {
			if ( verbose ) System.out.println( "Reference counts are not equal");
			return false;		
		}
			
		for ( Pair<String, Long> aRef : aReferences ) {
			int foundCount = 0;
			for ( Pair<String, Long> bRef : bReferences ) {
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

            int level = (int)(rand.nextDouble() * 18);
            anno.put("level", level );

            JSONObject range = new JSONObject();
            range.put("min", 0 );
            range.put("max", level );
            anno.put("range", range );

            anno.put("group", randomGroup() );

            //JSONObject certificate = new JSONObject();
            //certificate.put("uuid", UUID.randomUUID() );
            //certificate.put("timestamp", timestamp.toString() );
            //anno.put("certificate", certificate );

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

		return JSONAnnotation.fromJSON(generateJSON());
				
	}
	
	protected List<TileData< Map<String, List<Pair<String, Long>>>>> generateTiles( List<AnnotationData<?>> annotations, AnnotationIndexer indexer, TilePyramid pyramid ) {

		Map<TileIndex, TileData< Map<String, List<Pair<String, Long>>>>> tiles = new HashMap<>();
				
		for ( AnnotationData<?> annotation : annotations ) {
			List<TileAndBinIndices> indices = indexer.getIndices( annotation, pyramid );
			
			for ( TileAndBinIndices index : indices ) {
				
				TileIndex tileIndex = index.getTile();
				BinIndex binIndex = index.getBin();	
				
				if ( tiles.containsKey(tileIndex) ) {
					
					AnnotationManipulator.addDataToTile( tiles.get( tileIndex ), binIndex, annotation );

				} else {
										
					TileData< Map<String, List<Pair<String, Long>>>> tile = new TileData<>( tileIndex );				
					AnnotationManipulator.addDataToTile( tile, binIndex, annotation );
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
		
		/*
		int univariateCase = (int)(rand.nextDouble() * 10);
		switch (univariateCase) {
		
			case 0: 
				xy[1] = -1;
				break;
			case 1: 
				xy[0] = -1;
				break;
		}
		*/			
		return xy;		
	}
	
	
	protected String randomGroup() {
		
		final Random rand = new Random();
		String groups[] = {"Urgent", "High", "Medium", "Low"};
		int index = (int)(rand.nextDouble() * 3);
		return groups[ index ];
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
	
		
	protected List<TileIndex> tilesToIndices( List<TileData< Map<String, List<Pair<String, Long>>>>> tiles ) {
		List<TileIndex> indices = new ArrayList<>();
		for ( TileData< Map<String, List<Pair<String, Long>>>> tile : tiles ) {
			indices.add( tile.getDefinition() );
		}
		return indices;
	}

	protected List<Pair<String, Long>> dataToIndices( List<AnnotationData<?>> data ) {
		List<Pair<String, Long>> indices = new ArrayList<>();
		for ( AnnotationData<?> d : data ) {
			indices.add( d.getCertificate() );
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

            Iterator<?> groups = json.keys();
            while( groups.hasNext() ){

                String group = (String)groups.next();

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
