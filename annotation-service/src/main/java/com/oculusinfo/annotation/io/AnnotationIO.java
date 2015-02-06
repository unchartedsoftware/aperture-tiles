/*
 * Copyright (c) 2014 Oculus Info Inc. 
 * http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.annotation.io;

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.factory.util.Pair;

import java.io.IOException;
import java.util.List;

public interface AnnotationIO {
   
	
	/**
	 * Write
	 */
	public void initializeForWrite (String tableName) throws IOException;
    public void writeData (String id, 
					       AnnotationSerializer serializer, 
					       Iterable<AnnotationData<?>> data ) throws IOException;

    /**
     * Read
     */
    public void initializeForRead (String tableName);
    public List<AnnotationData<?>> readData (String id,
								             AnnotationSerializer serializer,
								             Iterable<Pair<String,Long>> certificates) throws IOException;
 
    /**
     * Delete
     */
    public void removeData (String id, Iterable<Pair<String,Long>> data ) throws IOException;
  
}
