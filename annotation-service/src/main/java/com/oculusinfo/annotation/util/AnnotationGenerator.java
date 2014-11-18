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

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.AnnotationTile;
import com.oculusinfo.annotation.impl.JSONAnnotation;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileAndBinIndices;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;


public class AnnotationGenerator {

	static final double EPSILON = 0.001;
    static final int MAX_LEVEL_POINT = 10;
    static final int MAX_LEVEL_RANGE = 4;
    private List<String> _groups;
	private List<Double> _bounds;
    final Random _rand = new Random();


    public AnnotationGenerator( double[] bounds, String[] groups ) {
        _bounds = new ArrayList<>();
        _bounds.add( bounds[0] + EPSILON );
        _bounds.add( bounds[1] + EPSILON );
        _bounds.add( bounds[2] - EPSILON );
        _bounds.add( bounds[3] - EPSILON );
        _groups = Arrays.asList( groups );
    }


    public AnnotationData<?> generateJSONAnnotation() {
        return JSONAnnotation.fromJSON(generateJSON());
    }


    public List<AnnotationData<?>> generateJSONAnnotations( int numEntries ) {

        List<AnnotationData<?>> annotations = new ArrayList<>();
        for (int i=0; i<numEntries; i++) {

            annotations.add( generateJSONAnnotation() );
        }
        return annotations;
    }


    public JSONObject generateJSON() {


        if ( _rand.nextBoolean() ) {

            // range based
            if ( _rand.nextBoolean() ) {
                // uni-variate
                return generateUnivariateRangeJSON();
            } else {
                // bi-variate
                return generateBivariateRangeJSON();
            }
        } else {

            // point based
            if ( _rand.nextBoolean() ) {
                // uni-variate
                return generateUnivariatePointJSON();
            } else {
                // bi-variate
                return generateBivariatePointJSON();
            }
        }
    }


    public List<AnnotationTile> generateTiles( List<AnnotationData<?>> annotations, AnnotationIndexer indexer, TilePyramid pyramid ) {

        Map<TileIndex, AnnotationTile> tiles = new HashMap<>();

        for ( AnnotationData<?> annotation : annotations ) {
            List<TileAndBinIndices> indices = indexer.getIndices( annotation, pyramid );

            for ( TileAndBinIndices index : indices ) {

                TileIndex tileIndex = index.getTile();
                BinIndex binIndex = index.getBin();

                if ( tiles.containsKey( tileIndex ) ) {

                    tiles.get( tileIndex ).addDataToBin(binIndex, annotation);

                } else {

                    AnnotationTile tile = new AnnotationTile( tileIndex );
                    tile.addDataToBin(binIndex, annotation);
                    tiles.put( tileIndex, tile );
                }
            }

        }
        return new ArrayList<>( tiles.values() );
    }


    public JSONObject generateUnivariatePointJSON() {

        JSONObject anno = generateJSONBody( (int)(_rand.nextDouble() * MAX_LEVEL_POINT) );

        double [] xy = randomPosition();

        try {

            if ( _rand.nextBoolean() ) {
                anno.put("x", xy[0]);
            } else {
                anno.put("y", xy[1]);
            }
            return anno;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public JSONObject generateUnivariateRangeJSON() {

        JSONObject anno = generateJSONBody( (int)(_rand.nextDouble() * MAX_LEVEL_RANGE) );

        double [] xy0 = randomPosition();
        double [] xy1 = randomPosition();

        try {

            if ( _rand.nextBoolean() ) {
                JSONArray x = new JSONArray();
                x.put( Math.min( xy0[0], xy1[0] ) );
                x.put( Math.max( xy0[0], xy1[0] ) );
                anno.put( "x", x );
            } else {
                JSONArray y = new JSONArray();
                y.put( Math.min( xy0[1], xy1[1] ) );
                y.put( Math.max( xy0[1], xy1[1] ) );
                anno.put( "y", y );
            }
            return anno;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


    public JSONObject generateBivariatePointJSON() {

        JSONObject anno = generateJSONBody( (int)(_rand.nextDouble() * MAX_LEVEL_POINT) );

        double [] xy = randomPosition();

        try {
            anno.put("x", xy[0]);
            anno.put("y", xy[1]);
            return anno;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


    public JSONObject generateBivariateRangeJSON() {

        JSONObject anno = generateJSONBody( (int)(_rand.nextDouble() * MAX_LEVEL_RANGE) );

        double [] xy0 = randomPosition();
        double [] xy1 = randomPosition();

        try {

            JSONArray x = new JSONArray();
            x.put( Math.min( xy0[0], xy1[0] ) );
            x.put( Math.max( xy0[0], xy1[0] ) );
            anno.put( "x", x );

            JSONArray y = new JSONArray();
            y.put( Math.min( xy0[1], xy1[1] ) );
            y.put( Math.max( xy0[1], xy1[1] ) );
            anno.put( "y", y );
            return anno;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


    private JSONObject generateJSONBody( int level ) {

        try {

            JSONObject anno = new JSONObject();

            anno.put("level", level );

            JSONObject range = new JSONObject();
            range.put("min", 0 );
            range.put("max", level );
            anno.put("range", range );

            anno.put("group", randomGroup() );

            JSONObject data = new JSONObject();
            data.put("comment", randomComment() );
            anno.put("data", data);
            return anno;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


	public double[] randomPosition() {

		double [] xy = new double[2];
		xy[0] = _bounds.get(0) + ( _rand.nextDouble() * (_bounds.get(2) - _bounds.get(0)) );
		xy[1] = _bounds.get(1) + ( _rand.nextDouble() * (_bounds.get(3) - _bounds.get(1)) );
		return xy;		
	}


    public String randomGroup() {

		int index = (int)( _rand.nextDouble() * _groups.size() );
		return _groups.get( index );
	}


    public String randomComment() {
		int LENGTH = 256;
		String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	    char[] text = new char[LENGTH];
	    for (int i = 0; i < LENGTH; i++)
	    {
	        text[i] = CHARACTERS.charAt(_rand.nextInt(CHARACTERS.length()));
	    }
	    return new String(text);
	}

	
}
