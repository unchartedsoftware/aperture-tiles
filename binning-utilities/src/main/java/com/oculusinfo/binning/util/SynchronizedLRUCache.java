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

import java.util.ArrayList;
import java.util.Collection;

/**
 * An extension of LRUCache for thread safety
 */
public class SynchronizedLRUCache<K, V> extends LRUCache<K, V> {
	
	public SynchronizedLRUCache() {
		super();
	}

	public SynchronizedLRUCache(int size) {
		super(size);
	}

	public SynchronizedLRUCache(int size, RemovalPolicy<K, V> removalPolicy) {
		super(size, removalPolicy);
	}

	@Override
	public synchronized boolean containsKey(Object key) {
			return super.containsKey(key);
		}

	@Override
	public synchronized void put(K key, V value) {
			super.put(key, value);
		}
	
	@Override
	public synchronized void remove(K key) {
			super.remove(key);
		}

	@Override
	public synchronized V get(Object key) {
			return super.get(key);
		}

	@Override
	public synchronized int getCurrentSize() {
			return super.getCurrentSize();
		}

	@Override
	public synchronized void clear() {
			super.clear();
		}

	@Override
	public Collection<V> values() {
		return new ArrayList<V>(super.values());
	}
}
