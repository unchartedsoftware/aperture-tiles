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
package com.oculusinfo.annotation.index.impl;

import java.util.LinkedList;
import java.util.List;

import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.*;
import com.oculusinfo.binning.*;

import com.google.inject.Inject;

public class TileAnnotationIndexer extends AnnotationIndexer<TileAndBinIndices> {

	@Inject
    public TileAnnotationIndexer( TilePyramid pyramid ) {
    	_pyramid = pyramid;
    }
    

    @Override
    public TileAndBinIndices getIndex( AnnotationData data, int level ) {
    	
    	// fill in defaults if dimensions are missing
    	boolean xExists = data.getX() != null;
    	boolean yExists = data.getY() != null;
    	double x = ( xExists ) ? data.getX() : 0;
    	double y = ( yExists ) ? data.getY() : 0;
    	
    	// map from raw x and y to tile and bin
    	TileIndex tile = _pyramid.rootToTile( x, y, level, AnnotationTile.NUM_BINS );
		BinIndex bin = _pyramid.rootToBin( x, y, tile );
				
		if ( xExists && yExists ) {
			return new TileAndBinIndices( tile, bin );
		} else if ( !xExists ) {
			return new TileAndBinIndices( new TileIndex( tile.getLevel(), -1, tile.getY(), AnnotationTile.NUM_BINS, AnnotationTile.NUM_BINS ), 
										  new BinIndex( -1, bin.getY() ) );
		} else {
			return new TileAndBinIndices( new TileIndex( tile.getLevel(), tile.getX(), -1, AnnotationTile.NUM_BINS, AnnotationTile.NUM_BINS ), 
					  					  new BinIndex( bin.getX(), -1 ) );
		}

    }

    
}
