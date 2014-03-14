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


import java.io.IOException;
import java.io.Serializable;
import java.lang.Number;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class AggregatedAnnotation implements Serializable {
	
    private static final long serialVersionUID = 3L;

    private AnnotationIndex 	 _aggregatedIndex;
    private List<AnnotationData> _annotations;
    
    
    public AggregatedAnnotation() {
    	_aggregatedIndex = null;
    	_annotations = new ArrayList<AnnotationData>();
    }
    
    
    public boolean add( AnnotationData annotation ) {
    	if ( !hasSameBounds( annotation.getIndex().getBounds() ) ) {
    		return false;
    	}
    	_annotations.add(annotation);
    	aggregateIndex();
    	return true;
    }
    
    
    public boolean join( AggregatedAnnotation aggregate ) {
    	if ( aggregate.getAnnotations().size() == 0 ) {
    		return false;
    	}
    	if ( !hasSameBounds( aggregate.getAnnotations().get(0).getIndex().getBounds() ) ) {
    		return false;
    	}
    	_annotations.addAll( aggregate.getAnnotations() );
    	aggregateIndex();
    	return true;
    }
    
    
    public List<AnnotationData> getAnnotations() {
    	return _annotations;
    }
    
    
    public AnnotationIndex getIndex() {
    	return _aggregatedIndex;
    }
    
    
    private boolean hasSameBounds( double[] otherBounds ) {
    	if ( _annotations.size() == 0 ) {
    		return true;
    	}
    	double bounds[] = _annotations.get(0).getIndex().getBounds();
    	for (int i=0; i<4; i++) {
    		if ( bounds[i] != otherBounds[i] ) {
    			return false;
    		}
    	}
    	return true;
    }
    
    
    private void aggregateIndex() {
    	double bounds[] = _annotations.get(0).getIndex().getBounds();
    	double xAgg = 0;
    	double yAgg = 0;
    	for (AnnotationData annotaton : _annotations) {
    		xAgg += annotaton.getIndex().getX();
    		yAgg += annotaton.getIndex().getY();   		
    	}
    	int size = _annotations.size();
    	_aggregatedIndex = new AnnotationIndex( xAgg/size, 
    											yAgg/size, 
    											bounds);
    }

}
