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
package com.oculusinfo.annotation.index;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oculusinfo.annotation.query.*;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class AnnotationIndex implements Serializable {
	
    private static final long serialVersionUID = 2L;

    private long _index;
    
    
    public AnnotationIndex ( long index ) {
    	_index = index;
    }

    
    public long getValue() {
    	return _index;
    }

    
    public final byte[] getBytes() {
    	
    	/*
    	try {
	    	ByteArrayOutputStream b = new ByteArrayOutputStream();
	        ObjectOutputStream o = new ObjectOutputStream( b );
	        o.writeObject(_index);
	        return b.toByteArray();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return new byte[0];
    	*/
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(_index);
        byte [] bytes = buf.array();
        return bytes;
    }
    
    @Override
    public int hashCode () {
    	/*
    	byte [] bytes = getBytes();
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;
        }
        return value;
    	*/
    	return (int)_index;
    }

    @Override
    public boolean equals (Object that) {
    	if (that != null)
    	{
    		if (that instanceof AnnotationBin) {
    			return _index == ((AnnotationBin)that).getIndex().getValue();
    		} else if (that instanceof AnnotationIndex) {
    			return _index == ((AnnotationIndex)that)._index;
    		}    		
    	}	
    	return false;
    }
    
}
