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
package com.oculusinfo.annotation.util;


import com.oculusinfo.annotation.AnnotationBin;
import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.AnnotationTile;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.factory.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class AnnotationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger( AnnotationUtil.class );

	public static void printData( Map<BinIndex, List<AnnotationData<?>>> dataMap ) {
		for ( List<AnnotationData<?>> annotations : dataMap.values() ) {
			printData( annotations );
		}
	}

	public static void printData( List<AnnotationData<?>> annotations ) {
		
		for ( AnnotationData<?> annotation : annotations ) {				
			try {
				LOGGER.debug( annotation.toJSON().toString( 4 ) );
			} catch ( Exception e ) {
				e.printStackTrace();
			}	
		}
	}

	public static void printTiles( List<AnnotationTile> tiles ) {
		
		for ( AnnotationTile tile : tiles ) {
			try {
				LOGGER.debug( tileToJSON( tile ).toString( 4 ) );
			} catch ( Exception e ) { e.printStackTrace(); }
					
		}
	}
	
	
	public static boolean compareData( List<AnnotationData<?>> as, List<AnnotationData<?>> bs ) {
		
		for ( AnnotationData<?> a : as ) {
			int foundCount = 0;
			for ( AnnotationData<?> b : bs ) {				
				if ( compareData(a, b ) ) {
					foundCount++;
				}
			}	
			if ( foundCount != 1 ) {
				LOGGER.debug( "Data lists do not match" );
				return false;
			}
		}
		return true;
	}
	
	public static boolean compareTiles( List<AnnotationTile> as, List< AnnotationTile > bs ) {
		
		for ( AnnotationTile a : as ) {
			int foundCount = 0;
			for ( AnnotationTile b : bs ) {
				if ( compareTiles( a, b, false ) ) {
					foundCount++;
				}
			}	
			if ( foundCount != 1 ) {
				LOGGER.debug( "Tile lists do not match" );
				return false;
			}
		}
		return true;
	}

    public static <T> boolean compareAttributes( T a, T b ) {

        if ( a == null || b == null ) {
            return ( a == b );
        } else {
            return a.equals( b );
        }
    }
	
	
	public static boolean compareData( AnnotationData<?> a, AnnotationData<?> b ) {
		
		if ( !a.getUUID().equals( b.getUUID() ) ) {
			LOGGER.debug( "UUID are not equal" );
			return false;
		}

        if ( !compareAttributes( a.getX(), b.getX() ) ) {
            LOGGER.debug( "X values are not equal" );
            return false;
		}

        if ( !compareAttributes( a.getY(), b.getY() ) ) {
            LOGGER.debug( "Y values are not equal" );
            return false;
        }

        if ( !compareAttributes( a.getX0(), b.getX0() ) ) {
            LOGGER.debug( "X0 values are not equal" );
            return false;
        }

        if ( !compareAttributes( a.getY0(), b.getY0() ) ) {
            LOGGER.debug( "Y0 values are not equal" );
            return false;
        }

        if ( !compareAttributes( a.getX1(), b.getX1() ) ) {
            LOGGER.debug( "X1 values are not equal" );
            return false;
        }

        if ( !compareAttributes( a.getY1(), b.getY1() ) ) {
            LOGGER.debug( "Y1 values are not equal" );
            return false;
        }

		if ( !a.getLevel().equals( b.getLevel() ) ) {
			LOGGER.debug( "Level values are not equal" );
			return false;
		}
		
		if ( !a.getData().toString().equals( b.getData().toString() ) ) {
			LOGGER.debug( "Data objects are not equal" );
			return false;
		}		
		return true;
	}
	
	
	public static boolean compareTiles( AnnotationTile a, AnnotationTile b, boolean verbose ) {
		
		List<Pair<String, Long>> aReferences = a.getAllCertificates();
		List<Pair<String, Long>> bReferences = b.getAllCertificates();
		
		if ( !a.getDefinition().equals( b.getDefinition() ) ) {
			LOGGER.debug( "Bin indices are not equal");
			return false;
		}
		
		if ( aReferences.size() != bReferences.size() ) {
			LOGGER.debug( "Reference counts are not equal");
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
				LOGGER.debug( "Reference lists are not equal");
				return false;
			}			
		}

		return true;
	}


	public static List<TileIndex> tilesToIndices( List< AnnotationTile > tiles ) {
		List<TileIndex> indices = new ArrayList<>();
		for ( AnnotationTile tile : tiles ) {
			indices.add( tile.getDefinition() );
		}
		return indices;
	}

	public static List<Pair<String, Long>> dataToIndices( List<AnnotationData<?>> data ) {
		List<Pair<String, Long>> indices = new ArrayList<>();
		for ( AnnotationData<?> d : data ) {
			indices.add( d.getCertificate() );
		}
		return indices;
	}


    public static JSONObject certificateToJSON( Pair<String, Long> certificate ) {

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


    public static AnnotationBin getBinFromJSON( JSONObject json ) throws IllegalArgumentException {

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
            return new AnnotationBin( certificates );

        } catch ( Exception e ) {
            throw new IllegalArgumentException( e );
        }

    }


    static public JSONObject binToJSON( AnnotationBin bin ) {

        JSONObject binJSON = new JSONObject();
        try {

            // for each group group in a bin
            for (Map.Entry<String, List<Pair<String, Long>>> certificateEntry : bin.getData().entrySet() ) {

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


    static public AnnotationTile getTileFromJSON( JSONObject json ) throws IllegalArgumentException {

        try {

            TileIndex index = new TileIndex( json.getInt("level"),
                    json.getInt("x"),
                    json.getInt("y"),
                    AnnotationIndexer.NUM_BINS,
                    AnnotationIndexer.NUM_BINS );

            // create tile with empty bins
            DenseTileData<AnnotationBin> tile = new DenseTileData<>( index );

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

            return new AnnotationTile( tile.getDefinition(), tile.getData() );

        } catch ( Exception e ) {
            throw new IllegalArgumentException( e );
        }

    }


    static public JSONObject tileToJSON( AnnotationTile tile ) {

        JSONObject tileJSON = new JSONObject();

        try {

            tileJSON.put("level", tile.getDefinition().getLevel() );
            tileJSON.put("x", tile.getDefinition().getX() );
            tileJSON.put("y", tile.getDefinition().getY() );

            for (int i=0; i<tile.getDefinition().getXBins(); i++ ) {
                for (int j=0; j<tile.getDefinition().getYBins(); j++ ) {

                    AnnotationBin bin = tile.getBin(i, j);

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
