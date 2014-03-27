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

import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.binning.*;

import org.json.JSONObject;

public interface AnnotationIO {
   
	/*
	 * Write
	 */
    public void initializeForWrite (String id) throws IOException;
    public void writeTiles (String tableName, 
    				        AnnotationSerializer<AnnotationTile> serializer, 
    				        List<AnnotationTile> tiles ) throws IOException;
    public void writeData (String tableName, 
					       AnnotationSerializer<AnnotationData> serializer, 
					       List<AnnotationData> data ) throws IOException;
    //public void writeMetaData (String tableName, String metaData) throws IOException;;
   
    /*
     * Read
     */
    public void initializeForRead (String id) throws IOException;    
    public List<AnnotationTile> readTiles (String tableName, 
						    			   AnnotationSerializer<AnnotationTile> serializer,
						    			   List<TileIndex> indices) throws IOException;  
    public List<AnnotationData> readData (String tableName, 
								          AnnotationSerializer<AnnotationData> serializer,
								          List<Long> indices) throws IOException;
    //public String readMetaData (String id) throws IOException;
       
    /*
     * Delete
     */
    public void initializeForRemove (String id) throws IOException;
    public void removeTiles (String tableName, 
			   				 List<AnnotationTile> tiles ) throws IOException;
    public void removeData (String tableName, 
   							List<AnnotationData> data ) throws IOException;
    //public void removeMetaData (String tableName, String metaData) throws IOException;
       
}
