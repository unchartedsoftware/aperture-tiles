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
package com.oculusinfo.tile.rest.tile.caching;


import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.tile.rest.tile.caching.TileCacheEntry.CacheRequestCallback;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;



public class TileCacheTests {
	private TileIndex[]        _indices;
	private int                _N;
	private TileCache<Integer> _cache;


    
	@Before
	public void setupIndices () {
		_indices = new TileIndex[16];
		for (int i=0; i<16; ++i) {
			_indices[i] = new TileIndex(2, (int) Math.floor(i/4.0), i % 4);
		}
	}

	@Before
	public void setupCache () {
		_N = 5;
		// quarter-second, 5-object cache - objects past 5 are removed after 1/4
		// second (or less, if data given), as more data is inserted.
		_cache = new TileCache<>(250, _N);
	}

	@After
	public void cleanup () {
		_cache = null;
		_indices = null;
	}



	private void checkRequest (TileIndex index, boolean expectedNewRequest) {
		List<TileIndex> newRequests = _cache.getNewRequests(Collections.singletonList(index));
		if (expectedNewRequest) {
			// Make sure the request was new
			Assert.assertEquals(1, newRequests.size());
			Assert.assertEquals(index, newRequests.get(0));
		} else {
			// Make sure there was no new request
			Assert.assertEquals(0, newRequests.size());
		}
	}

	// Test first to make sure when all is properly notified, in the expected
	// normal way, the oldest element is removed first.
	@Test
	public void testSimpleCacheRemoval () {
		CacheRequestCallback<Integer> callback = new NoOpCacheRequestCallback();

		int i;
		for (i=0; i<_N; ++i) {
			TileData<Integer> data = new DenseTileData<Integer>(_indices[i], i);

			// provide and listen to each tile
			checkRequest(_indices[i], true);
			_cache.requestTile(_indices[i], callback);
			_cache.provideTile(data);
		}

		// Make another request
		checkRequest(_indices[i], true);

		// Make sure all but our first request is still there
		for (i=1; i<_N; ++i) {
			checkRequest(_indices[i], false);
		}

		// Make sure our first request is gone
		checkRequest(_indices[0], true);
	}

	// Make sure requests don't disappear if data hasn't been provided for them
	@Test
	public void testMissingCacheRemoval () {
		CacheRequestCallback<Integer> callback = new NoOpCacheRequestCallback();

		int i;
		for (i=0; i<_N; ++i) {
			// listen to each tile
			checkRequest(_indices[i], true);
			_cache.requestTile(_indices[i], callback);
		}

		// Make another request
		checkRequest(_indices[i], true);

		// Make sure all requests so far are still there
		for (i=0; i<_N+1; ++i) {
			checkRequest(_indices[i], false);
		}
	}

	// Make sure potential requests don't disappear if they have not yet been 
	// requested.
	@Test
	public void testUnrequestedCacheRemoval () {
		int i;
		for (i=0; i<_N; ++i) {
			TileData<Integer> data = new DenseTileData<Integer>(_indices[i], i);

			// provide and listen to each tile
			checkRequest(_indices[i], true);
			_cache.provideTile(data);
		}

		// Make another request
		checkRequest(_indices[i], true);

		// Make sure all requests so far are still there
		for (i=0; i<_N+1; ++i) {
			checkRequest(_indices[i], false);
		}
	}

	// Make sure potential requests don't disappear if they have not yet been 
	// requested.
	@Test
	public void testUnrequestedCacheRemovalTimeout () throws InterruptedException {
		int i;
		for (i=0; i<_N; ++i) {
			TileData<Integer> data = new DenseTileData<Integer>(_indices[i], i);

			// provide and listen to each tile
			checkRequest(_indices[i], true);
			_cache.provideTile(data);
		}

		Thread.sleep(260);
		// Make another request
		checkRequest(_indices[i], true);

		// Make sure all requests so far are still there
		for (i=1; i<_N+1; ++i) {
			checkRequest(_indices[i], false);
		}
		checkRequest(_indices[0], true);
	}



	// Simple callback to do nothing, but act as if we've done something.
	private class NoOpCacheRequestCallback implements CacheRequestCallback<Integer> {
		@Override
		public boolean onTileReceived (TileIndex index, TileData<Integer> tile) {
			// Noop, but say we did something.
			return true;
		}

		@Override
		public void onTileAbandoned (TileIndex index) {
			// Noop
		}
	}
}
