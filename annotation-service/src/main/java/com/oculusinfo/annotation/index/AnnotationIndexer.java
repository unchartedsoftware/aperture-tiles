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

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileAndBinIndices;
import com.oculusinfo.binning.TilePyramid;

import java.util.List;

public abstract class AnnotationIndexer {

	public static final int NUM_BINS = 16;
	public static final BinIndex RANGE_BIN = new BinIndex(0, 0);

    public AnnotationIndexer() {}
    
    public abstract List<TileAndBinIndices> getIndices( AnnotationData<?> data, TilePyramid pyramid );
    public abstract List<TileAndBinIndices> getIndicesByLevel( AnnotationData<?> data, int level, TilePyramid pyramid );

    
}
