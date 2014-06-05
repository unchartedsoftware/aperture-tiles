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
package com.oculusinfo.annotation.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;

import com.oculusinfo.annotation.*;
import com.oculusinfo.annotation.config.*;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.util.Pair;



public interface AnnotationService {
	
	/*
	 * Write an annotation to the storage service
	 * 
	 */
	public abstract Pair<String,Long> write( String layer, AnnotationData<?> annotation ) throws IllegalArgumentException;
	
	
	/*
	 * Modify an annotation in the storage service
	 * 
	 */
	public abstract Pair<String,Long> modify( String layer, AnnotationData<?> oldAnnotation, AnnotationData<?> newAnnotation ) throws IllegalArgumentException;
	
	
	/*
	 * Read annotations from the storage service
	 * 
	 */
	public abstract Map<BinIndex, List<AnnotationData<?>>> read( UUID id, String layer, TileIndex tile ) throws IllegalArgumentException;
	
	
	/*
	 * Remove an annotation from the storage service
	 * 
	 */
	public abstract void remove( String layer, AnnotationData<?> annotation );

	
	/*
	 * Configuration interface 
	 */
	public abstract AnnotationConfiguration getConfiguration( String layer );
	public abstract UUID configureFilter (String layerId, JSONObject filters );
    public abstract void unconfigureFilter (String layerId, UUID uuid );

    /*
     * List Layers interface
     */
	public abstract List<AnnotationInfo> list();
}
