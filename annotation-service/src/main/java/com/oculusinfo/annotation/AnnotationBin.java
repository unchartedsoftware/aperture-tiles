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

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.factory.util.Pair;

import java.io.Serializable;
import java.util.*;


/**
 * This class represents a bin's worth of annotations. As the annotation API uses
 * the binning-utilities packages generic types for serialization, this class is
 * used to encapsulate the unique behaviour needed for adding and removing annotations
 * from each bin object.
 *
 * An annotation bin is a map from each group string to a list of annotation
 * certificates. Each certificate is represented as a uuid and a timestamp in
 * the form of a Pair<String, Long>
 *
 * {
 *     "Urgent" : [ (UUID, TIMESTAMP), ... ],
 *     "High"   : [ (UUID, TIMESTAMP), ... ],
 *     "Medium" : [ (UUID, TIMESTAMP), ... ],
 *     "Low"    : [ (UUID, TIMESTAMP), ... ]
 * }
 *
 */

public class AnnotationBin implements Serializable {

	private static final long serialVersionUID = 1L;
    private static class CertificateComparator implements Comparator< Pair<String, Long> > {
        @Override
        public int compare( Pair<String, Long> a, Pair<String, Long> b ) {
            // java sorts in ascending order, we want descending ( new certificates first )
            // so we negate the compareTo
            return -a.getSecond().compareTo( b.getSecond() );
        }
    }
    private Map<String, List<Pair<String, Long>>> _bin;


    public AnnotationBin(){
        _bin = new LinkedHashMap<>();
    }


    public AnnotationBin( Map<String, List<Pair<String, Long>>> rawBin ) {
        if ( rawBin != null ) {
            _bin = rawBin;
        } else {
            _bin = new LinkedHashMap<>();
        }
    }


    public static List< AnnotationBin > convertFromRaw( TileData<Map<String, List<Pair<String, Long>>>> rawTile ) {
        List<Map<String, List<Pair<String, Long>>>> rawData = DenseTileData.getData(rawTile);
        List< AnnotationBin > bins = new ArrayList<>();
        for (Map<String, List<Pair<String, Long>>> rawBin: rawData) {
            if ( rawBin != null ) {
                bins.add( new AnnotationBin( rawBin ) );
            } else {
                bins.add( null );
            }
        }
        return bins;
    }


    public static List< Map<String, List<Pair<String, Long>>> > convertToRaw( List< AnnotationBin > bins ) {

        List< Map<String, List<Pair<String, Long>>> > rawBins = new ArrayList<>();
        for ( AnnotationBin bin : bins) {
            if ( bin != null ) {
                rawBins.add( bin.getData() );
            } else {
                rawBins.add( null );
            }
        }
        return rawBins;
    }


    public boolean isEmpty() {
        return _bin.isEmpty();
    }


    public Map<String, List<Pair<String, Long>>> getData() {
        return _bin;
    }


    public void addData( AnnotationData<?> data ) {

        String group = data.getGroup();
        Pair<String, Long> certificate =  data.getCertificate();
        List< Pair<String, Long> > entries;

        if ( _bin.containsKey( group ) ) {
            entries = _bin.get( group );
            if ( !entries.contains( certificate ) ) {
                entries.add( certificate );
            }
        } else {
            entries = new LinkedList<>();
            entries.add( certificate );
            _bin.put( group, entries );
        }

        // sort certificates after insertion... maybe instead use a SortedSet?
        Collections.sort( entries, new CertificateComparator() );
    }


    public void removeData( AnnotationData<?> data ) {

        String group = data.getGroup();
        Pair<String, Long> certificate = data.getCertificate();

        if ( _bin.containsKey(group) ) {

            List<Pair<String, Long>> entries = _bin.get(group);
            if (entries.contains(certificate)) {
                entries.remove(certificate);
            }
            if (entries.isEmpty()) {
                // remove certificates for group
                _bin.remove( group );
            }
        }
    }


    public List<Pair<String, Long>> getCertificates( String group ) {

        if ( _bin.containsKey( group ) ) {
            return _bin.get( group );
        } else {
            return new LinkedList<>();
        }
    }


    public List<Pair<String, Long>> getAllCertificates() {

        List<Pair<String, Long>> allCertificates = new LinkedList<>();
        // for each group group in a bin
        for ( List<Pair<String, Long>> certificates : _bin.values() ) {
            allCertificates.addAll(certificates);
        }
        return allCertificates;
    }
}
