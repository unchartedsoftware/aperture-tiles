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

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.factory.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * This class represents a tile's worth of annotations. As the annotation API uses
 * the binning-utilities package, this class is used to encapsulate the unique behaviour
 * needed for adding and removing annotations from each TileData<> object
 *
 * This class wraps a DenseTileData< AnnotationBin > object, but massages the data into
 * a more generic DenseTileData< Map<String, List<Pair<String,Long>>> > that is used by
 * the serializer when writing / reading the data from the PyramidIO
 */

public class AnnotationTile extends DenseTileData< AnnotationBin > {
    private static final long serialVersionUID = 1L;


    public AnnotationTile( TileIndex index ) {
        super( index );
    }


    public AnnotationTile( TileIndex index , List< AnnotationBin > data ) {
        super( index, data );
    }


    public AnnotationTile( TileData< Map<String, List<Pair<String,Long>>> > rawTile ) {
        super( rawTile.getDefinition(),  AnnotationBin.convertFromRaw( rawTile ) );
    }


    public static List< AnnotationTile > convertFromRaw( List<TileData<Map<String, List<Pair<String,Long>>>>> rawTiles ) {

        List< AnnotationTile > tiles = new ArrayList<>();
        for ( TileData<Map<String, List<Pair<String,Long>>>> rawTile : rawTiles ) {
            tiles.add( new AnnotationTile( rawTile ) );
        }
        return tiles;
    }


    public static List<TileData<Map<String, List<Pair<String,Long>>>>> convertToRaw( List< AnnotationTile > tiles ) {

        List<TileData<Map<String, List<Pair<String,Long>>>>> rawTiles = new ArrayList<>();
        for ( AnnotationTile tile : tiles ) {
            rawTiles.add( tile.getRawData() );
        }
        return rawTiles;
    }


    public TileData<Map<String, List<Pair<String,Long>>>> getRawData() {
        List< Map<String, List<Pair<String,Long>>> > rawBins = AnnotationBin.convertToRaw( getData() );
        return new DenseTileData<>( getDefinition(), rawBins );
    }

    public boolean isEmpty() {

        for ( AnnotationBin bin : getData()  ) {
            if ( bin != null ) {
                return false;
            }
        }
        return true;
    }


    public void addDataToBin( BinIndex binIndex, AnnotationData<?> data ) {

        AnnotationBin bin = getBin( binIndex.getX(), binIndex.getY() );

        if ( bin == null ) {
            bin = new AnnotationBin();
            setBin( binIndex.getX(), binIndex.getY(), bin );
        }

        bin.addData( data );
    }


    public void removeDataFromBin( BinIndex binIndex, AnnotationData<?> data ) {

        AnnotationBin bin = getBin( binIndex.getX(), binIndex.getY() );

        bin.removeData( data );

        // remove bin if empty
        if ( bin.isEmpty() ) {
            setBin( binIndex.getX(), binIndex.getY(), null );
        }
    }


    public List<Pair<String, Long>> getAllCertificates() {

        List<Pair<String, Long>> allCertificates = new LinkedList<>();
        // for each bin
        for ( AnnotationBin bin : getData() ) {
            if (bin != null) {
                // get all certificates
                allCertificates.addAll( bin.getAllCertificates() );
            }
        }
        return allCertificates;
    }

}
