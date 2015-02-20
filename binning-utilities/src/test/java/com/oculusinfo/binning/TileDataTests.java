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

import org.junit.Ignore;
import org.junit.Test;

import com.oculusinfo.binning.impl.DenseTileData;

import java.util.ArrayList;
import java.util.List;

/*
 * Some basic tests to test potential tile addition
 */
@Ignore
public class TileDataTests {
	private TileData<Double> createRandomTile () {
		TileIndex index = new TileIndex(0, 0, 0);
		TileData<Double> datum = new DenseTileData<>(index);
		for (int x=0; x<index.getXBins(); ++x) {
			for (int y=0; y<index.getYBins(); ++y) {
				datum.setBin(x, y, Math.random());
			}
		}
		return datum;
	}
	private TileData<Double> addTiles (TileData<Double> a, TileData<Double> b) {
		int xBins = a.getDefinition().getXBins();
		int yBins = a.getDefinition().getYBins();
		List<Double> dc = new ArrayList<>();
		for (int y = 0; y < yBins; ++y) {
			for (int x = 0; x < xBins; ++x) {
				dc.add(a.getBin(x, y) + b.getBin(x, y));
			}
		}
		return new DenseTileData<Double>(a.getDefinition(), dc);
	}

	@Test
	public void tileDataAggregationTimingTest () {
		int N=10000;
		long cumulativeAddTime = 0L;

        
		TileData<Double> addition = createRandomTile();

		TileData<Double> composite = createRandomTile();
		for (int n=0; n<N; ++n) {
			// Create the data
			long startTime = System.nanoTime();
			composite = addTiles(composite, addition);
			long endTime = System.nanoTime();
			cumulativeAddTime += (endTime - startTime);
		}
		System.out.println("Current system");
		System.out.println("Performed "+N+" tile additions");
		System.out.println(String.format("Time spent adding tiles: %.4f",   (   cumulativeAddTime/1000000000.0)));
	}

	private static final int tileSize = 256*256;
	private double[] createRandomTileArray () {
		double[] data = new double[tileSize];
		for (int i=0; i<tileSize; ++i) {
			data[i] = Math.random();
		}
		return data;
	}
	private double[] addTileArrays (double[] a, double[] b) {
		double[] c = new double[tileSize];
		for (int n=0; n<tileSize; ++n) {
			c[n] = a[n] + b[n];
		}
		return c;
	}

	@Test
	public void tileDataArrayAggregationTimingTest () {
		int N=10000;
		long cumulativeAddTime = 0L;


		double[] addition = createRandomTileArray();

		double[] composite = createRandomTileArray();
		for (int n=0; n<N; ++n) {
			// Create the data
			long startTime = System.nanoTime();
			composite = addTileArrays(composite, addition);
			long endTime = System.nanoTime();
			cumulativeAddTime += (endTime - startTime);
		}
		System.out.println("Java limit");
		System.out.println("Performed "+N+" tile additions");
		System.out.println(String.format("Time spent adding tiles: %.4f",   (   cumulativeAddTime/1000000000.0)));
	}

	private double[] addTileTimeSeriesArrays (double[] a, double[][] b) {
		double[] c = new double[tileSize];
		for (int n=0; n<tileSize; ++n) {
			double ci = c[n];
			for (int t=0; t<b.length; ++t) {
				ci = ci + b[t][n];
			}
			c[n] = ci;
		}
		return c;
	}
	@Test
	public void tileDataTimeSeriesAggregationTimingTest () {
		int N=10000;
		int T=10;
		long cumulativeAddTime = 0L;


		double[][] addition = new double[T][];
		for (int t=0; t<T; ++t) {
			addition[t] = createRandomTileArray();
		}

		double[] composite = createRandomTileArray();
		for (int n=0; n<N; n = n + T) {
			// Create the data
			long startTime = System.nanoTime();
			composite = addTileTimeSeriesArrays(composite, addition);
			long endTime = System.nanoTime();
			cumulativeAddTime += (endTime - startTime);
		}
		System.out.println("Java limit, time series");
		System.out.println("Performed "+N+" tile additions");
		System.out.println(String.format("Time spent adding tiles: %.4f",   (   cumulativeAddTime/1000000000.0)));
	}    
}
