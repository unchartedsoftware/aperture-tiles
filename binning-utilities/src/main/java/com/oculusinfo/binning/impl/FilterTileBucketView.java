/*
 * Copyright (c) 2015 Uncharted Software. http://www.uncharted.software/
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
package com.oculusinfo.binning.impl;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;



/**
 * This implementation of TileData takes a TileData whose bins are lists of buckets and presents 
 *  a view to a range of buckets. 
 *  
 */
public class FilterTileBucketView<T> implements TileData<List<T>> {
    private static final long serialVersionUID = 1234567890L;

    private TileData<List<T>> _base 		= null;
    private Integer			  _startBucket 	= 0;
    private Integer			  _endBucket 	= 0;


    public FilterTileBucketView (TileData<List<T>> base, int startBucket, int endBucket) {
        _base = base;
        _startBucket = startBucket;
        _endBucket = endBucket;    

        if ( _startBucket < 0 || _startBucket > _endBucket ){
			throw new IllegalArgumentException("Constructor for AverageTileBucketView: arguments are invalid.  start average bucket: " + _startBucket + ", end time bucket: " + _endBucket);
		}
     }


    @Override
    public TileIndex getDefinition () {
        return _base.getDefinition();
    }

    
	@Override
	public void setBin(int x, int y, List<T> value)  {
    	if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}
		_base.setBin( x, y, value );
    }

    
	@Override
    public List<T> getBin (int x, int y) {   	
    	if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}
		//List<T> newBin = new ArrayList<>();
    	List<T> binContents = _base.getBin(x, y);   	
        int binSize = binContents.size();
		int start = ( _startBucket != null ) ? _startBucket : 0;
		int end = ( _endBucket != null && _endBucket < binSize ) ? _endBucket : binSize;

        for(int i = 0; i < binSize; i++) {
            if ( i >= start && i <= end ) {
            	binContents.set(i, binContents.get(i));
            } else {
            	binContents.set(i, null);
            }
        }
        return binContents;
    }
    

    @Override
    public Collection<String> getMetaDataProperties () {
        return _base.getMetaDataProperties();
    }
    

    @Override
    public String getMetaData (String property) {
        return _base.getMetaData(property);
    }
    

    @Override
    public void setMetaData (String property, Object value) {
        _base.setMetaData(property, value);
    }

}
