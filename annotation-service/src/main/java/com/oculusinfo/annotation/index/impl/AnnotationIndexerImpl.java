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

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.index.AnnotationIndexer;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileAndBinIndices;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;

import java.util.LinkedList;
import java.util.List;

public class AnnotationIndexerImpl extends AnnotationIndexer {

    public AnnotationIndexerImpl() {
    	super();
    }
    
	@Override
	public List<TileAndBinIndices> getIndices( AnnotationData<?> data, TilePyramid pyramid ) {
		
		// only generate indices upwards
    	List<TileAndBinIndices> indices = new LinkedList<>();

        int minLevel = Math.min( data.getLevel(), data.getRange().getFirst() );
        int maxLevel = Math.max( data.getLevel(), data.getRange().getSecond() );

		for (int i=minLevel; i<=maxLevel; i++) {
			indices.addAll( getIndicesByLevel( data, i, pyramid ) );
		}
		return indices;
    }

    @Override
    public List<TileAndBinIndices> getIndicesByLevel( AnnotationData<?> data, int level, TilePyramid pyramid ) {

        List<TileAndBinIndices> tileAndBins = new LinkedList<>();

        if ( !data.isRangeBased() ) {
            // point annotation

            Double x = ( data.getX() == null ) ? 0 : data.getX();
            Double y = ( data.getY() == null ) ? 0 : data.getY();

            // map from x and y to tile and bin
            TileIndex tile = pyramid.rootToTile( x, y, level, NUM_BINS, NUM_BINS );
            BinIndex bin = pyramid.rootToBin( x, y, tile );

            tileAndBins.add( new TileAndBinIndices( tile, bin ) );
            return tileAndBins;

        } else {

            // range annotations
            Double x0 = ( data.getX0() == null ) ? 0 : data.getX0();
            Double y0 = ( data.getY0() == null ) ? 0 : data.getY0();
            Double x1 = ( data.getX1() == null ) ? 0 : data.getX1();
            Double y1 = ( data.getY1() == null ) ? 0 : data.getY1();

            // bottom left
            TileIndex tileBL = pyramid.rootToTile(x0, y0, level, NUM_BINS, NUM_BINS);
            // top right
            TileIndex tileTR = pyramid.rootToTile( x1, y1, level, NUM_BINS, NUM_BINS );

            for (int i=tileBL.getX(); i<=tileTR.getX(); i++) {
                for (int j=tileBL.getY(); j <= tileTR.getY(); j++) {
                    tileAndBins.add( new TileAndBinIndices( new TileIndex(level, i, j, NUM_BINS, NUM_BINS ), RANGE_BIN ) );
                }
            }

        }

		return tileAndBins;
    }

    
}
