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
package com.oculusinfo.binning.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Simple, Least Recently Used object cache.  This holds onto at most some 
 * fixed number of objects, dropping any extras according to whichever have 
 * been accessed the least recently. 
 * 
 * @author cbethune, nkronenfeld
 */
public class LRUCache<K, V>  {
	/**
	 * The RemovalPolicy allows the user of a cache to affect which elements 
	 * can be removed - i.e., to make the LRU removal not strict.
	 * 
	 * @author nkronenfeld
	 */
	public interface RemovalPolicy<K, V> {
		/**
		 * Test if the last entry should be removed. Mostly this is used for
		 * side effects (in which case it should return true - it is only called
		 * if the cache is already too big) - but it can determine behavior - if
		 * it returns false, the entry will not be removed. If the removal
		 * policy handles removing the element itself, it must return true (see
		 * {@link LinkedHashMap#removeEldestEntry}
		 * 
		 * @param entry
		 *            The least recently used entry in the cache
		 * @param map
		 *            The data map of the cache
		 * @param suggestedMaxSize
		 *            The intended max size of the LRU cache
		 * @return True if the entry should be, and has not yet been, removed;
		 *         false if it should be left alone, or if the removal policy
		 *         has already handled removing it.
		 */
		public boolean shouldRemove (Map.Entry<K, V> entry, Map<K, V> map, int suggestedMaxSize);

		/**
		 * Called whenever an entry has been removed.
		 * @param key The key of the removed entry 
		 * @param value The value of the removed entry
		 */
		public void onElementRemoved (K key, V value);
	}
	
	protected class LruRemovalHashMap extends LinkedHashMap<K, V> {
		private static final long serialVersionUID = 1L;
		public RemovalPolicy<K, V> removalPolicy;

		public LruRemovalHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
			super(initialCapacity, loadFactor, accessOrder);
		}

		public LruRemovalHashMap(int initialCapacity, float loadFactor, boolean accessOrder, RemovalPolicy<K, V> removalPolicy) {
			super(initialCapacity, loadFactor, accessOrder);
			this.removalPolicy = removalPolicy;
		}
        
		protected boolean removeEldestEntry (Map.Entry<K, V> entry) { 
			boolean result = false;
			if (size() > LRUCache.this._size) {
				result = true;
				if (removalPolicy != null) {
					result = removalPolicy.shouldRemove(entry, this, LRUCache.this._size);
					if (result)
						removalPolicy.onElementRemoved(entry.getKey(), entry.getValue());
				}
			}
			return result;
		}
	}


	protected LruRemovalHashMap _map = null;
	protected int _size;

	public LRUCache() {
		_size = 250;
		_map = new LruRemovalHashMap(_size, 0.75f, true);      
	}

	public LRUCache(int size) {
		_size = size;
		_map = new LruRemovalHashMap(_size, 0.75f, true);      
	}

	public LRUCache(int size, RemovalPolicy<K, V> removalPolicy) {
		_size = size;
		_map = new LruRemovalHashMap(_size, 0.75f, true, removalPolicy);      
	}

    
	public boolean containsKey (Object key) {
		return _map.containsKey(key);
	}


	public void put(K key, V value) {
		V oldValue = _map.put(key, value);
		if (oldValue != null && _map.removalPolicy != null)
			_map.removalPolicy.onElementRemoved(key, oldValue);
	}
    
	public void remove(K key) {
		V value = _map.remove(key);
		if (_map.removalPolicy != null)
			_map.removalPolicy.onElementRemoved(key, value);
	}

	public V get (Object key) {
		return _map.get(key);
	}


	public int getCurrentSize() {
		return _map.size();
	}


	synchronized public void setSize (int size) {
			_size = size;
        
			while (_map.size() > _size) {
				K key = _map.keySet().iterator().next();
				V value = _map.remove(key);
				if (_map.removalPolicy != null)
					_map.removalPolicy.onElementRemoved(key, value);
			}
		}

	public void setRemovalPolicy(RemovalPolicy<K, V> removalPolicy) {
		_map.removalPolicy = removalPolicy;
	}

	synchronized public int getSize() {
			return _size;                
		}
    
    
	public void clear() {
		if (_map.removalPolicy != null) {
			for (Entry<K, V> entry : _map.entrySet()) {
				_map.removalPolicy.onElementRemoved(entry.getKey(), entry.getValue());
			}
		}
		_map.clear();
	}  
    
    
	public Collection<V> values() {
		return Collections.unmodifiableCollection(_map.values());
	}
}
