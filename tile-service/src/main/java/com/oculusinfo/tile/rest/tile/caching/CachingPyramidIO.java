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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.tile.rest.tile.caching.TileCacheEntry.CacheRequestCallback;

public class CachingPyramidIO implements PyramidIO {
	private static final Logger LOGGER = LoggerFactory.getLogger(CachingPyramidIO.class);

	private Map<String, TileCache<?>>                    _tileCaches;
	private Map<String, PyramidIO>                       _basePyramidIOs;
	private List<LayerDataChangedListener>               _layerListeners;

	public CachingPyramidIO () {
		_tileCaches = new HashMap<>();
		_basePyramidIOs = new HashMap<>();
		_layerListeners = new ArrayList<>();
	}

	public void addLayerListener (LayerDataChangedListener listener) {
		_layerListeners.add(listener);
	}

	public void removeLayerListener (LayerDataChangedListener listener) {
		_layerListeners.remove(listener);
	}

	synchronized private PyramidIO getBasePyramidIO (String pyramidId) {
		return _basePyramidIOs.get(pyramidId);
	}

	synchronized private <T> TileCache<T> getTileCache (String pyramidId) {
		// We rely on configuration to make sure types match here
		@SuppressWarnings({"rawtypes", "unchecked"})
		TileCache<T> cache = (TileCache)_tileCaches.get(pyramidId);
		if (null == cache) {
			cache = new TileCache<>(10000, 100);
			cache.addGlobalCallback(new GlobalCallback<T>(pyramidId));
			_tileCaches.put(pyramidId, cache);
		}
		return cache;
	}

	// Using the callback mechanism in the tile cache, get a tile and hand it
	// back synchronously.
	//
	// This does _not_ handle making the request; that must be done separately
	// with RequestData.
	private <T> TileData<T> getTileData (String pyramidId, TileIndex index) {
		TileCache<T> cache = getTileCache(pyramidId);

		CacheListenerCallback<T> callback = new CacheListenerCallback<>();
		cache.requestTile(index, callback);

		TileData<T> tile = callback.waitForTile();

		return tile;
	}




	@Override
	public void initializeForWrite (String pyramidId) throws IOException {
		throw new UnsupportedOperationException("Caching Pyramid IO only supports reading");
	}

	@Override
	public <T> void writeTiles (String pyramidId,
	                            TileSerializer<T> serializer,
	                            Iterable<TileData<T>> data) throws IOException {
		throw new UnsupportedOperationException("Caching Pyramid IO only supports reading");
	}

	@Override
	public void writeMetaData (String pyramidId, String metaData) throws IOException {
		throw new UnsupportedOperationException("Caching Pyramid IO only supports reading");
	}



	/*
	 * Set up a base pyramid from which to read when we get a cache miss
	 */
	public void setupBasePyramidIO (String pyramidId, ConfigurableFactory<? extends PyramidIO> factory) {
		if (!_basePyramidIOs.containsKey(pyramidId)) {
			synchronized (_basePyramidIOs) {
				if (!_basePyramidIOs.containsKey(pyramidId)) {
					try {
						PyramidIO basePyramidIO = factory.produce(PyramidIO.class);
						_basePyramidIOs.put(pyramidId, basePyramidIO);
					} catch (ConfigurationException e) {
						LOGGER.warn("Error creating base pyramid IO", e);
					}
				}
			}
		}
	}


	@Override
	public void initializeForRead (String pyramidId, int width, int height,
	                               Properties dataDescription) {
		if (!_basePyramidIOs.containsKey(pyramidId)) {
			LOGGER.info("Attempt to initialize unknown pyramid" + pyramidId + "'.");
		} else {
			_basePyramidIOs.get(pyramidId).initializeForRead(pyramidId, width, height, dataDescription);
		}
	}

	/**
	 * Request a set of tiles, retrieving some of them immediately, and setting
	 * the rest up for eventual retrieval
	 *
	 * @param pyramidId the pyramid io
	 * @param serializer the serializer
	 * @param indices Indices of tiles to be requested.  May not be null.
	 * @throws IOException
	 */
	public <T> void requestTiles (String pyramidId,
	                              TileSerializer<T> serializer,
	                              Iterable<TileIndex> indices) throws IOException {
		TileCache<T> cache = getTileCache(pyramidId);

		synchronized (cache) {
			// First, request and retrieve all tiles needed over the long term
			// Only request those we don't already have
			List<TileIndex> newIndices = new ArrayList<>(cache.getNewRequests(indices));
			if (newIndices.isEmpty())
				return;

			PyramidIO base = getBasePyramidIO(pyramidId);
			List<TileData<T>> tiles = base.readTiles(pyramidId, serializer, newIndices);

			// Cache recieved tiles...
			for (TileData<T> tile: tiles) {
				cache.provideTile(tile);
				newIndices.remove(tile.getDefinition());
			}
			// And the fact that some were empty
			for (TileIndex index: newIndices) {
				cache.provideEmptyTile(index);
			}
		}
	}

	@Override
	public <T> List<TileData<T>> readTiles (String pyramidId,
	                                        TileSerializer<T> serializer,
	                                        Iterable<TileIndex> indices) throws IOException {
		TileCache<T> cache = getTileCache(pyramidId);
		synchronized (cache) {
			List<TileData<T>> tiles = new ArrayList<>();
			for (TileIndex index: indices) {
				// We rely on configuration to make sure types match here
				@SuppressWarnings({"unchecked", "rawtypes"})
				TileData<T> tile = (TileData) getTileData(pyramidId, index);

				if (null != tile)
					tiles.add(tile);
			}

			return(tiles);
		}
	}

	@Override
	public <T> List<TileData<T>> readTiles (String pyramidId,
											TileSerializer<T> serializer,
											Iterable<TileIndex> tiles,
											JSONObject properties ) throws IOException {
		return readTiles( pyramidId, serializer, tiles );
	}

	@Override
	public <T> InputStream getTileStream (String pyramidId,
	                                      TileSerializer<T> serializer,
	                                      TileIndex index) throws IOException {
		// We cache tiles, not streams, so we need to serialize the tile into a
		// stream, in order to return a stream.
		TileData<T> tile = getTileData(pyramidId, index);

		if (null == tile) {
			return null;
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			serializer.serialize(tile, baos);
			baos.flush();
			baos.close();
			return new ByteArrayInputStream(baos.toByteArray());
		}
	}

	@Override
	public String readMetaData (String pyramidId) throws IOException {
		return getBasePyramidIO(pyramidId).readMetaData(pyramidId);
	}

	@Override
	public void removeTiles (String id, Iterable<TileIndex> tiles ) throws IOException {
		throw new IOException("removeTiles not currently supported for CachingPyramidIO");
	}

	private class CacheListenerCallback<T> implements CacheRequestCallback<T> {
		private TileData<T> _tile;
		private boolean     _waiting;
		private boolean     _notified;



		public CacheListenerCallback () {
			_tile = null;
			_waiting = false;
			_notified = false;
		}

		synchronized public TileData<T> waitForTile () {
				if (!_notified)
					try {
						_waiting = true;
						wait(1000);
					} catch (InterruptedException e) {
						LOGGER.warn("Error waiting for return for tile.", e);
						return null;
					} finally {
						_waiting = false;
					}

				return _tile;
			}

		@Override
		synchronized public boolean onTileReceived (TileIndex index, TileData<T> tile) {
				_tile = tile;
				_notified = true;
				if (_waiting)
					this.notify();
				return true;
			}

		@Override
		public void onTileAbandoned (TileIndex index) {
			if (_waiting)
				this.notify();
		}
	}

	private class GlobalCallback<T> implements TileCacheEntry.CacheRequestCallback<T> {
		private String _layer;
		GlobalCallback (String layer) {
			_layer = layer;
		}
		@Override
		public boolean onTileReceived (TileIndex index, TileData<T> tile) {
			for (LayerDataChangedListener listener: _layerListeners) {
				listener.onLayerDataChanged(_layer);
			}
			return false;
		}

		@Override
		public void onTileAbandoned (TileIndex index) {
		}
	}
	public interface LayerDataChangedListener {
		public void onLayerDataChanged (String layer);
	}
}
