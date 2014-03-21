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

import java.util.List;

import com.oculusinfo.binning.*;

public abstract class AnnotationIndexer<T> {

	protected static final String TABLE_NAME = "AnnotationTable";
	
	protected static final long BITS[] = { 0x5555555555555555L, 
										   0x3333333333333333L, 
										   0x0F0F0F0F0F0F0F0FL,
										   0x00FF00FF00FF00FFL, 
										   0x0000FFFF0000FFFFL,
										   0x00000000FFFFFFFFL};
    protected static final long SHIFTS[] = { 1, 2, 4, 8, 16 };	
    protected static final int BINS = 8;
    protected static final int BINS_EXP = (int)(Math.log(BINS) / Math.log(2));
    protected static final int LEVELS = 20;
    
	
    protected TilePyramid _pyramid;
    
    public AnnotationIndexer() {
    }
    
    public abstract List<AnnotationIndex> getIndices( T data );
    public abstract AnnotationIndex getIndex( T data, int level );

}
