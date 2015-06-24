/*
 * Copyright (c) 2015 Uncharted Software Inc. http://www.uncharted.software/
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
package com.oculusinfo.binning;

import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.impl.DenseTileMultiSliceView;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MultiSliceViewTest {
	@Test
	public void testNullBinReplacement () {
		TileData<List<Integer>> base = new DenseTileData<List<Integer>>(new TileIndex(0, 0, 0, 1, 1), (List<Integer>) null);
		TileData<List<Integer>> slice = new DenseTileMultiSliceView<Integer>(base, Arrays.asList(2, 0));
		slice.setBin(0, 0, Arrays.asList(4, 2));
		Assert.assertEquals(2, base.getBin(0, 0).get(0).intValue());
		Assert.assertNull(     base.getBin(0, 0).get(1));
		Assert.assertEquals(4, base.getBin(0, 0).get(2).intValue());
	}
	@Test
	public void testUnmodifiableBinReplacement () {
		List<Integer> binVal = new ArrayList<>();
		binVal.add(3);
		binVal.add(4);
		binVal.add(5);
		TileData<List<Integer>> base = new DenseTileData<List<Integer>>(new TileIndex(0, 0, 0, 1, 1));
		base.setBin(0, 0, Collections.unmodifiableList(binVal));
		TileData<List<Integer>> slice = new DenseTileMultiSliceView<Integer>(base, Arrays.asList(2, 0));
		slice.setBin(0, 0, Arrays.asList(6, 2));
		Assert.assertEquals(2, base.getBin(0, 0).get(0).intValue());
		Assert.assertEquals(4, base.getBin(0, 0).get(1).intValue());
		Assert.assertEquals(6, base.getBin(0, 0).get(2).intValue());

		Assert.assertEquals(3, binVal.get(0).intValue());
		Assert.assertEquals(4, binVal.get(1).intValue());
		Assert.assertEquals(5, binVal.get(2).intValue());
	}

	@Test
	public void testModifiableBinReplacement () {
		List<Integer> binVal = new ArrayList<>();
		binVal.add(3);
		binVal.add(4);
		binVal.add(5);
		TileData<List<Integer>> base = new DenseTileData<List<Integer>>(new TileIndex(0, 0, 0, 1, 1));
		base.setBin(0, 0, binVal);
		TileData<List<Integer>> slice = new DenseTileMultiSliceView<Integer>(base, Arrays.asList(2, 0));
		slice.setBin(0, 0, Arrays.asList(6, 2));
		Assert.assertEquals(2, base.getBin(0, 0).get(0).intValue());
		Assert.assertEquals(4, base.getBin(0, 0).get(1).intValue());
		Assert.assertEquals(6, base.getBin(0, 0).get(2).intValue());

		Assert.assertEquals(2, binVal.get(0).intValue());
		Assert.assertEquals(4, binVal.get(1).intValue());
		Assert.assertEquals(6, binVal.get(2).intValue());
	}

	@Test
	public void testBinRetrieval () {
		TileData<List<Integer>> base = new DenseTileData(new TileIndex(0, 0, 0, 1, 1));
		base.setBin(0, 0, Arrays.asList(-0, -1, -2, -3, -4, -5, -6, -7));
		TileData<List<Integer>> slice1 = new DenseTileMultiSliceView<Integer>(base, Arrays.asList(1, 3, 5, 7));
		TileData<List<Integer>> slice2 = new DenseTileMultiSliceView<Integer>(base, Arrays.asList(6, 4, 2, 0));

		List<Integer> b1 = slice1.getBin(0, 0);
		Assert.assertEquals(4, b1.size());
		Assert.assertEquals(-1, b1.get(0).intValue());
		Assert.assertEquals(-3, b1.get(1).intValue());
		Assert.assertEquals(-5, b1.get(2).intValue());
		Assert.assertEquals(-7, b1.get(3).intValue());

		List<Integer> b2 = slice2.getBin(0, 0);
		Assert.assertEquals(4, b2.size());
		Assert.assertEquals(-6, b2.get(0).intValue());
		Assert.assertEquals(-4, b2.get(1).intValue());
		Assert.assertEquals(-2, b2.get(2).intValue());
		Assert.assertEquals(-0, b2.get(3).intValue());
	}
}
