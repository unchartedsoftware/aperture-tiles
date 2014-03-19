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

import java.io.IOException;
import java.util.List;




import com.oculusinfo.annotation.query.*;
import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.io.serialization.*;


public interface AnnotationIO {
   
	/*
	 * Write
	 */
    public void initializeForWrite (String id) throws IOException;
    public <T> void writeAnnotations (String tableName, 
    							      AnnotationSerializer<T> serializer, 
    							      List<AnnotationBin<T>> annotations ) throws IOException;
    public void writeMetaData (String tableName, String metaData) throws IOException;;
   
    /*
     * Read
     */
    public void initializeForRead (String id) throws IOException; 
    public <T> List<AnnotationBin<T>> readAnnotations (String tableName, 
		    									       AnnotationSerializer<T> serializer,
		    										   List<AnnotationIndex> annotations) throws IOException;
    public <T> List<AnnotationBin<T>> readAnnotations (String tableName, 
												       AnnotationSerializer<T> serializer,
													   AnnotationIndex from,
													   AnnotationIndex to) throws IOException;

    public String readMetaData (String id) throws IOException;
       
    /*
     * Delete
     */
    public void initializeForRemove (String id) throws IOException;
    public void removeAnnotations (String tableName, 
    							   List<AnnotationIndex> annotations ) throws IOException;
    public void removeMetaData (String tableName, String metaData) throws IOException;
       
}
