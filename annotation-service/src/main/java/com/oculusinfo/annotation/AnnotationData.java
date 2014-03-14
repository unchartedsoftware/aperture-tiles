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

public class AnnotationData implements Serializable {
	
    private static final long serialVersionUID = 1L;

    private AnnotationIndex _index;
    private String _priority;
    private String _comment;

    public AnnotationData (double x, double y, double bounds[], String priority, String comment) {
    	_index = new AnnotationIndex(x, y, bounds);
    	_comment = comment;
    }
    
    public AnnotationData (AnnotationIndex index, String priority, String comment) {
    	_index = index;
    	_priority = priority;
    	_comment = comment;
    }
    
    public AnnotationData (byte[] bytes) {
    	
    	try {
	    	String str = new String(bytes, "UTF-8");
	    	
	    	ArrayList<String> data = new ArrayList<>( Arrays.asList(str.split("\\s*,\\s*")) );
	    	
	    	double nx = Double.parseDouble(data.get(0));
	    	double ny = Double.parseDouble(data.get(1));
	    	
	    	double bounds[] = { Double.parseDouble(data.get(2)),
				    			Double.parseDouble(data.get(3)),
				    			Double.parseDouble(data.get(4)),
				    			Double.parseDouble(data.get(5)) };
	    	String priority = data.get(6);
	    	String comment = data.get(7);
	    	
	    	_index = new AnnotationIndex( nx, ny, bounds );
	    	_comment = comment;
	    	_priority = priority;
    	} catch ( Exception e ) {
    		System.out.println("Error: " + e.getMessage());
    	}
    	
    }
    
    
    public String getComment() {
    	return _comment;
    }
    
    public AnnotationIndex getIndex() {
    	return _index;
    }
    
    public byte[] getBytes() {
    	return ( _index.getX() 
    		   + ", "
    		   + _index.getY() 
    		   + ","
    		   + _index.getBounds()[0] 
    		   + ","
    		   + _index.getBounds()[1]
    		   + ","
    		   + _index.getBounds()[2] 
    		   + ","
    		   + _index.getBounds()[3] 
    		   + ","
    		   + _priority
    		   + ","
    		   + _comment ).getBytes();
    }
    
    @Override
    public int hashCode () {
    	return _index.hashCode() + _comment.hashCode() + _priority.hashCode();
    }

    @Override
    public boolean equals (Object that) {   	    	
    	if (that != null && that instanceof AnnotationData)
        {
    		//System.out.println( "comp: " + _index.getIndex() + " vs " + ((AnnotationData)that)._index)
    		return _index.equals( ((AnnotationData)that)._index ); /* &&
    			   _comment.equals( ((AnnotationData)that)._comment ) &&
    			   _priority.equals( ((AnnotationData)that)._priority );*/
        }   	
    	return false;
    }

}
