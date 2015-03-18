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

import org.json.JSONObject;

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.factory.util.Pair;



public interface AnnotationService {

    /**
     * Write an annotation to the storage service
     *
     * @param layer The layer identification string.
     * @param annotation The annotation data object to be written.
     *
     * @throws IllegalArgumentException
     */
	public abstract Pair<String,Long> write( String layer, AnnotationData<?> annotation ) throws IllegalArgumentException;

    /**
     * Modify an annotation in the storage service
     *
     * @param layer The layer identification string.
     * @param annotation The modified annotation data object to be written.
     *
     * @throws IllegalArgumentException
     */
	public abstract Pair<String,Long> modify( String layer, AnnotationData<?> annotation ) throws IllegalArgumentException;

    /**
     * Read annotations from the storage service, if no annotations are in the tile, returns null
     *
     * @param layer The layer identification string.
     * @param tile The tile index to read.
     * @param query Query parameter object.
     *
     * @throws IllegalArgumentException
     */
	public abstract List<List<AnnotationData<?>>> read( String layer, TileIndex tile, JSONObject query ) throws IllegalArgumentException;

    /**
     * Remove an annotation from the storage service
     * @param layer The layer identification string.
     * @param certificate The uuid and timestamp pair that represents the annotation to be removed.
     *
     * @throws IllegalArgumentException
     */
	public abstract void remove( String layer, Pair<String, Long> certificate ) throws IllegalArgumentException;

}
