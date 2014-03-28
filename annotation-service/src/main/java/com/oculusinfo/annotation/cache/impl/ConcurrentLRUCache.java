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
package com.oculusinfo.annotation.cache.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.*;

import com.oculusinfo.annotation.cache.*;
	
public class ConcurrentLRUCache<A, B> implements AnnotationCache<A, B> {
	
	private class LRULinkedHashMap<C, D> extends LinkedHashMap<C, D> {
		
		private static final long serialVersionUID = 1L;
		private final int _maxEntries;
		
	    public LRULinkedHashMap(final int maxEntries) {
	        super(maxEntries + 1, 1.0f, true);
	        this._maxEntries = maxEntries;
	    }
	
	    @Override
	    protected boolean removeEldestEntry(final Map.Entry<C, D> eldest) {
	        return super.size() > _maxEntries;
	    }
	    
	}
	
	private Map<A,B> _map;
	private final ReadWriteLock _lock = new ReentrantReadWriteLock();
			
	public ConcurrentLRUCache( int maxEntries ) {
		_map = new LRULinkedHashMap<>( maxEntries );	
	}
	
	public B get( A key ) {
		_lock.readLock().lock();
		try {
			return _map.get( key );
		} finally {
			_lock.readLock().unlock();
		}
	}
	
	public List<B> get( List<A> keys ) {
		
		_lock.readLock().lock();
		try {
			List<B> bs = new LinkedList<>();
			for ( A key : keys ) {
				bs.add( _map.get( key ) );
			}
			return bs;
			
		} finally {
			_lock.readLock().unlock();
		}
	}
	
	public void put( A key, B value ) {		
		_lock.writeLock().lock();
		try {
			_map.put( key , value );
		} finally {
			_lock.writeLock().unlock();
		}
	}
	
	public void put( List<A> keys, List<B> values ) {		
		_lock.writeLock().lock();
		try {
			for (int i=0; i<keys.size(); i++) {
				_map.put( keys.get(i) , values.get(i) );
			}
			
		} finally {
			_lock.writeLock().unlock();
		}
	}
	
	public void remove( A key ) {
		
		_lock.writeLock().lock();
		try {
			_map.remove( key );
		} finally {
			_lock.writeLock().unlock();
		}			
	}
	
}