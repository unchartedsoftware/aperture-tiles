/**
 * Copyright (c) 2013 Oculus Info Inc. http://www.oculusinfo.com/
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
package com.oculusinfo.tile.spi.impl.pyramidio.image;



import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;



public class TileCacheTests {
    @Test
    public void testSimpleCacheRemoval () {
        int n = 5;
        TileCache<Integer> cache = new TileCache<>(1000, n);

        TileIndex[] indices = new TileIndex[16];
        for (int i=0; i<16; ++i) {
            indices[i] = new TileIndex(2, (int) Math.floor(i/4.0), i % 4);
        }

        // Test first to make sure when all is properly notified, the oldest
        // element is removed first.

        // Make our base n requests, and fulfil them
        for (int i=0; i<n; ++i) {
            TileData<Integer> data = new TileData<Integer>(indices[i], i);

            cache.getNewRequests(Collections.singletonList(indices[i]));
            cache.provideTile(data);
        }

        // Make another request
        List<TileIndex> newIndices = cache.getNewRequests(Collections.singletonList(indices[n]));
        // Make sure it was new
        Assert.assertEquals(1, newIndices.size());
        Assert.assertEquals(indices[n], newIndices.get(0));

        // Make sure all but our first request is still there
        for (int i=1; i<n; ++i) {
            newIndices = cache.getNewRequests(Collections.singletonList(indices[n]));
            Assert.assertEquals(0, newIndices.size());
        }
        // Make sure our first request is gone
        newIndices = cache.getNewRequests(Collections.singletonList(indices[0]));
        Assert.assertEquals(1, newIndices.size());
        Assert.assertEquals(indices[0], newIndices.get(0));
    }

    @Test
    public void testMissingCacheRemoval () {
        int n = 5;
        TileCache<Integer> cache = new TileCache<>(1000, n);

        TileIndex[] indices = new TileIndex[16];
        for (int i=0; i<16; ++i) {
            indices[i] = new TileIndex(2, (int) Math.floor(i/4.0), i % 4);
        }

        // Test first to make sure when all is properly notified, the oldest
        // element is removed first.

        // Make our base n requests, and fulfil them
        for (int i=0; i<n; ++i) {
            cache.getNewRequests(Collections.singletonList(indices[i]));
        }

        // Make another request
        List<TileIndex> newIndices = cache.getNewRequests(Collections.singletonList(indices[n]));
        // Make sure it was new
        Assert.assertEquals(1, newIndices.size());
        Assert.assertEquals(indices[n], newIndices.get(0));

        // Make sure all requests so far are still there
        for (int i=1; i<n+1; ++i) {
            newIndices = cache.getNewRequests(Collections.singletonList(indices[n]));
            Assert.assertEquals(0, newIndices.size());
        }
    }
}
