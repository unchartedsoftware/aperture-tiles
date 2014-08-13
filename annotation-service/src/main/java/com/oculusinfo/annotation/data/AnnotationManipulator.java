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

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.util.Pair;

import java.util.*;


public class AnnotationManipulator {


	static private class CertificateComparator implements Comparator< Pair<String, Long> > {
	    @Override
	    public int compare( Pair<String, Long> a, Pair<String, Long> b ) {	    	
	    	// java sorts in ascending order, we want descending ( new certificates first )
	    	// so we negate the compareTo
	    	return -a.getSecond().compareTo( b.getSecond() );
	    }
	}
	

    static public boolean isTileEmpty( TileData<Map<String, List<Pair<String, Long>>>> tile ) {

        for ( Map<String, List<Pair<String, Long>>> bin : tile.getData()  ) {
            if ( bin != null ) {
                return false;
            }
        }
        return true;

    }
    
    static public void addDataToBin( Map<String, List<Pair<String, Long>>> bin, AnnotationData<?> data ) {

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
      
    
    static public boolean removeDataFromBin( Map<String, List<Pair<String, Long>>> bin, AnnotationData<?> data ) { 

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


    static public void addDataToTile( TileData<Map<String, List<Pair<String, Long>>>> tile, BinIndex binIndex, AnnotationData<?> data ) {

        Map<String, List<Pair<String, Long>>> bin = tile.getBin( binIndex.getX(), binIndex.getY() );

        if ( bin != null ) {
            addDataToBin( bin, data );
        } else {

            Map<String, List<Pair<String, Long>>> newBin = new LinkedHashMap<>();
            addDataToBin( newBin, data );
            tile.setBin( binIndex.getX(), binIndex.getY(), newBin );
        }
    }
    
    
    static public void removeDataFromTile( TileData<Map<String, List<Pair<String, Long>>>> tile, BinIndex binIndex, AnnotationData<?> data ) { 

        Map<String, List<Pair<String, Long>>> bin = tile.getBin( binIndex.getX(), binIndex.getY() );
        if ( bin != null && removeDataFromBin( bin, data ) ) {
            // remove bin if empty
            if ( bin.size() == 0 ) {
                tile.setBin( binIndex.getX(), binIndex.getY(), null );
            }
        }
    }


    static public List<Pair<String, Long>> getCertificatesFromBin( Map<String, List<Pair<String, Long>>> bin, String group ) {

        if ( bin.containsKey( group ) ) {
            return bin.get( group );
        } else {
            return new LinkedList<>();
        }
    }
    
    
    static public List<Pair<String, Long>> getAllCertificatesFromBin( Map<String, List<Pair<String, Long>>> bin ) {

        List<Pair<String, Long>> allCertificates = new LinkedList<>();
        // for each group group in a bin
        for ( List<Pair<String, Long>> certificates : bin.values() ) {
            allCertificates.addAll( certificates );
        }
        return allCertificates;
    }  
    

    static public List<Pair<String, Long>> getAllCertificatesFromTile( TileData<Map<String, List<Pair<String, Long>>>> tile ) {

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
