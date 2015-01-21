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
import com.oculusinfo.binning.util.LRUCache.RemovalPolicy;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.SynchronizedLRUCache;
import com.oculusinfo.tile.rest.tile.caching.TileCacheEntry.CacheRequestCallback;

import java.util.*;
import java.util.Map.Entry;



/**
 * An LRU cache of tiles, with its own specific removal policy and cache entry
 * type
 * 
 * @author nkronenfeld
 * 
 */
public class TileCache<T> {
	// The maximum amount of time an unrecieved tile is guaranteed to remain in
	// the cache, in milliseconds
	private long                                                    _maxTileAge;
	// The cache iteself
	private SynchronizedLRUCache<TileIndex, TileCacheEntry<T>> _cache;
	// A list of all keys which have recieved their data, with the time at which they were first requested.
	private TreeSet<Pair<TileIndex, Long>>                          _haveData;
	// A list of all keys, in the order in which they were requested.
	private List<TileIndex>                                         _orderedKeys;
	// A listener to attach to all entries
	private CacheEntryListener                                      _entryListener;
	// A list of global listeners to all requests
	private List<CacheRequestCallback<T>>                           _globalCallbacks;

	public TileCache (long maxAge, int maxSize) {
		_maxTileAge = maxAge;
		_cache = new SynchronizedLRUCache<>(maxSize,
		                                    new TileCacheRemovalPolicy());
		_haveData= new TreeSet<>(new EntryAgeComparator());
		_orderedKeys = new LinkedList<>();
		_entryListener = new CacheEntryListener();
		_globalCallbacks = new ArrayList<>();
	}

	public void addGlobalCallback (CacheRequestCallback<T> callback) {
		_globalCallbacks.add(callback);
	}

	public void removeGlobalCallback (CacheRequestCallback<T> callback) {
		_globalCallbacks.remove(callback);
	}

	/**
	 * Take a list of tiles to request, and return the subset that are new
	 * requests - i.e., ones not already in the cache.
	 * 
	 * Each new request will be placed in the cache.
	 * 
	 * @param requests
	 *            The list of tiles needed
	 * @return A sublist of just those tiles not already requested
	 */
	public List<TileIndex> getNewRequests (Iterable<TileIndex> requests) {
		List<TileIndex> needed = new ArrayList<>();
		synchronized (_cache) {
			for (TileIndex index : requests) {
				if (!_cache.containsKey(index)) {
					// Create the tile request, and listen for its fulfilment
					TileCacheEntry<T> entry = new TileCacheEntry<T>(index);
					entry.requestTile(_entryListener);

					// Add to cache
					_cache.put(index, entry);
					// Add to list of keys in request order
					_orderedKeys.add(index);

					needed.add(index);
				}
			}
		}

		return needed;
	}

	/**
	 * Request a tile.
	 * 
	 * @param index
	 *            The index of the tile being requested.
	 * 
	 * @param callback
	 *            A callback to call when the request is fulfilled.
	 */
	public void requestTile (TileIndex index, CacheRequestCallback<T> callback) {
		_cache.get(index).requestTile(callback);
	}

	public void provideTile (TileData<T> tile) {
		if (null == tile)
			return;

		TileIndex index = tile.getDefinition();
		TileCacheEntry<T> entry = _cache.get(index);
		if (null != entry)
			entry.setTile(tile);
	}

	public void provideEmptyTile (TileIndex index) {
		TileCacheEntry<T> entry = _cache.get(index);
		if (null != entry)
			entry.setTile(null);
	}

	private class CacheEntryListener implements CacheRequestCallback<T> {
		@Override
		public boolean onTileReceived (TileIndex index, TileData<T> tile) {
			TileCacheEntry<T> entry = _cache.get(index);
			if (null != entry) {
				_haveData.add(new Pair<TileIndex, Long>(index,
				                                        entry.initialRequestTime()));
			}
			// Notify any global listeners
			if (null != _globalCallbacks && !_globalCallbacks.isEmpty()) {
				for (CacheRequestCallback<T> callback: _globalCallbacks) {
					callback.onTileReceived(index, tile);
				}
			}
			return false;
		}

		@Override
		public void onTileAbandoned (TileIndex index) {
			// If we're here, everything's already handled, so there is nothing
			// more to do.
		}
	}



	private class TileCacheRemovalPolicy
		implements
		RemovalPolicy<TileIndex, TileCacheEntry<T>> {
		@Override
		public boolean shouldRemove (Entry<TileIndex, TileCacheEntry<T>> entry,
		                             Map<TileIndex, TileCacheEntry<T>> map,
		                             int suggestedMaxSize) {
			if (null == entry) {
				return false;
			} else if (null == entry.getValue()) {
				return true;
			} else {
				TileCacheEntry<T> value = entry.getValue();
				if (value.hasBeenRetrieved() || value.age() > _maxTileAge) {
					// This entry has been received, or is to old for us to
					// care; just remove it.
					return true;
				} else {
					// First see if there is anything which has already been
					// handled, so can be freely deleted.
					for (Pair<TileIndex, Long> hasData: _haveData) {
						TileIndex index = hasData.getFirst();
						TileCacheEntry<T> entryWithData = _cache.get(index);
						if (entryWithData.hasBeenRetrieved()) {
							_cache.remove(index);
							return false;
						}
					}

					// If we get this far, there was nothing already handled.
					//
					// See if there's anything so old we just don't care about
					// its safety any more.
					if (_orderedKeys.size() > 0) {
						TileIndex oldestKey = _orderedKeys.get(0);
						TileCacheEntry<T> oldestEntry = _cache.get(oldestKey);
						if (oldestEntry.age() > _maxTileAge) {
							_cache.remove(oldestKey);
						}
					}
					return false;
				}
			}
		}

		@Override
		public void onElementRemoved (TileIndex key,
		                              TileCacheEntry<T> value) {
			value.abandonTile();
		}
	}



	// Compare two elements in our _haveData list.  This puts the older element first.
	private class EntryAgeComparator implements
		Comparator<Pair<TileIndex, Long>> {
		@Override
		public int compare (Pair<TileIndex, Long> o1, Pair<TileIndex, Long> o2) {
			return -o1.getSecond().compareTo(o2.getSecond());
		}
	}
}
