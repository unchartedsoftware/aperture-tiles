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
import com.oculusinfo.binning.util.Operator;



/**
 * This implementation of TileData takes a TileData whose bins are lists of buckets and presents 
 *  a view to a range of buckets that are compared to an average of another defined range of buckets.
 *  The caller can specify what operation to use for the comparison. 
 *  
 */
public class AverageTileBucketView<T> implements TileData<List<T>> {
    private static final long serialVersionUID = 1234567890L;

    private TileData<List<T>> _base 		= null;
    private Number[][] 		  _averageBins  = null;
    private Operator		  _operator 	= null;
    private Integer			  _startCompare = 0;
    private Integer			  _endCompare 	= 0;


    public AverageTileBucketView (TileData<List<T>> base, int startAv, int endAv, String op, int startComp, int endComp) {
        _base = base;
        _operator = new Operator(op);
        _startCompare = startComp;
        _endCompare = endComp;
        _averageBins = new Number[getDefinition().getXBins()][getDefinition().getYBins()];       

        if ( startAv < 0 || startAv > endAv ){
			throw new IllegalArgumentException("Constructor for AverageTileBucketView: arguments are invalid.  start average bucket: " + startAv + ", end time bucket: " + endAv);
		}
        
        // compute bin averages for selected average range       
        for ( int tx = 0; tx < getDefinition().getXBins(); tx++ ) {
        	for ( int ty = 0; ty < getDefinition().getYBins(); ty++ ) {
        		List<T> binContents = _base.getBin(tx, ty);              
        		int binSize = binContents.size();
                endAv = ( endAv > binSize ) ? endAv : binSize;

                double total = 0;
                int count = 0;
                for(int i = 0; i < binSize; i++) {
                    if ( i >= startAv && i <= endAv ) {
                    	total = total + (Double)binContents.get(i);
                    	count++;
                    } 
                }
                _averageBins[tx][ty] = (total/count);
        	}
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

    
	@SuppressWarnings("unchecked")
	@Override
    public List<T> getBin (int x, int y) {   	
    	if (x < 0 || x >= getDefinition().getXBins()) {
			throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
		}
		if (y < 0 || y >= getDefinition().getYBins()) {
			throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
		}

    	List<T> binContents = _base.getBin(x, y);   	
        int binSize = binContents.size();
		int start = ( _startCompare != null ) ? _startCompare : 0;
		int end = ( _endCompare != null ) ? _endCompare : binSize;

        for(int i = 0; i < binSize; i++) {
            if ( i >= start && i <= end ) {
            	binContents.set(i, (T)_operator.Calculate( (Number)binContents.get(i), _averageBins[x][y]));
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
