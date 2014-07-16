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
package com.oculusinfo.annotation.filter.impl;

import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.filter.AnnotationFilter;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.util.Pair;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * This class represents a single annotation
 */
public class EmptyFilter implements AnnotationFilter {

    public EmptyFilter() {}

	public List<Pair<String, Long>> filterTile( TileData<Map<String, List<Pair<String, Long>>>> tile ) {

        List<Pair<String, Long>> filtered = new LinkedList<>();
        // for each bin
        for ( Map<String, List<Pair<String, Long>>> bin : tile.getData() ) {
            if (bin != null) {
                // for each group
                for (Map.Entry<String, List<Pair<String, Long>>> binEntry : bin.entrySet()) {
                    filtered.addAll(binEntry.getValue());
                }
            }
        }
        return filtered;
    }

    public List<AnnotationData<?>> filterAnnotations( List<AnnotationData<?>> annotations ) {
        return annotations;
    }
}
