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
package com.oculusinfo.annotation.manager;

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.factory.util.Pair;

import java.io.IOException;
import java.util.List;


/**
 * This interface provides an API to manage reading, writing, and removing of multiple annotations 
 * 		to the same location.
 */
public interface AnnotationManager {
	
	/**
	 * Initialize the data store to write to
	 */
	public void initializeForWrite() throws IOException;
	
	/**
	 * Write the collection of annotations to the data store
	 * 
	 * @param annotations
	 *            This is a list of AnnotationData objects to write into the data store
	 */
	public void writeAnnotations(List<AnnotationData<?>> annotations) throws IOException;
	
	/**
	 * Remove the collection of annotations from the data store
	 * 
	 * @param dataIndices
	 *            This is a list of Pairs that contain the timestamp(String) and UUID(Long) that correspond to 
	 *            the annotations to be removed from the data store
	 */
	public void removeAnnotations(Iterable<Pair<String,Long>> dataIndices) throws IOException;	
	
	/**
	 * Read a collection of annotations from the data store based on the indices passed in
	 * 
	 * @param dataIndices
	 *            This is a list of Pairs that contain the timestamp(String) and UUID(Long) that correspond to 
	 *            the annotations to be read from the data store 
	 * 
	 * @return a list of AnnotationData objects retrieved from the data store based on the indices passed in
	 */
	public List<AnnotationData<?>> readAnnotations(Iterable<Pair<String,Long>> dataIndices) throws IOException;
}
