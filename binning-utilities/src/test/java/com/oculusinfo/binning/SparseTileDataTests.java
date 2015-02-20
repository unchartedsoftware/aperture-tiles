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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning;



import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.factory.util.Pair;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;


public class SparseTileDataTests {
    @Test
    public void testSparseTileIterator () {
        SparseTileData<Integer> tile = new SparseTileData<>(new TileIndex(0, 0, 0));
        tile.setBin(20, 2, 12);
        tile.setBin(10, 1, 3);
        tile.setBin(10, 5, 2);
        tile.setBin(120, 25, -2);
        tile.setBin(10, 4, 1);
        tile.setBin(120, 10, 10);
        tile.setBin(120, 9, 11);

        Iterator<Pair<BinIndex, Integer>> i = tile.getData();
        Assert.assertEquals(new Pair<BinIndex, Integer>(new BinIndex(10, 1), 3), i.next());
        Assert.assertEquals(new Pair<BinIndex, Integer>(new BinIndex(10, 4), 1), i.next());
        Assert.assertEquals(new Pair<BinIndex, Integer>(new BinIndex(10, 5), 2), i.next());
        Assert.assertEquals(new Pair<BinIndex, Integer>(new BinIndex(20, 2), 12), i.next());
        Assert.assertEquals(new Pair<BinIndex, Integer>(new BinIndex(120, 9), 11), i.next());
        Assert.assertEquals(new Pair<BinIndex, Integer>(new BinIndex(120, 10), 10), i.next());
        Assert.assertEquals(new Pair<BinIndex, Integer>(new BinIndex(120, 25), -2), i.next());
        Assert.assertFalse(i.hasNext());
    }

    @Test
    public void testDefaultDefaultValue () {
        SparseTileData<Integer> tile = new SparseTileData<>(new TileIndex(0, 0, 0, 4, 4));
        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 4; ++y) {
                Assert.assertNull(tile.getBin(x, y));
            }
        }
    }

    @Test
    public void testPresetDefaultValue () {
        SparseTileData<Integer> tile = new SparseTileData<>(new TileIndex(0, 0, 0, 4, 4), 27);
        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 4; ++y) {
                Assert.assertEquals(27, tile.getBin(x, y).intValue());
            }
        }
    }
}
