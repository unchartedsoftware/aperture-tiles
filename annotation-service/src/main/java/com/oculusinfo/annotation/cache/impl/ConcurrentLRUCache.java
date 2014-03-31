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
import java.util.HashSet;
import java.util.Map;

import com.oculusinfo.annotation.cache.*;
	
/**
 * Simple Least-Recently-Used cache that extends LinkedHashMap for its internal map. As
 * a LinkedHashMap is not thread-safe, per key locks are implemented.
 * 
 */
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
	private HashSet<A> _locks = new HashSet<>(); 
	
	private void lock( A key ) {
	    synchronized( _locks ) {
	        while( _locks.contains( key ) ) {
	        	// already being locked
	        	try {
	        		 // wait for release
	        		_locks.wait();          
	        	} catch ( Exception e ) {
	        		e.printStackTrace();
	        	}
	        }	     
	        // I lock it
	        _locks.add( key );            
	    }
	}
	
	private void unlock( A key ) {
	    synchronized( _locks ) {
	    	_locks.remove( key );
	    	_locks.notifyAll();
	    }
	}
	
	public ConcurrentLRUCache( int maxEntries ) {
		_map = new LRULinkedHashMap<>( maxEntries );
		
	}
	
	public B get( A key ) {
				
		lock( key );
		try {
			return _map.get( key );
		} finally {
			unlock( key );
		}
	}
	
	
	public List<B> get( List<A> keys ) {
		
		List<B> bs = new LinkedList<>();
		for ( A key : keys ) {
			bs.add( get( key ) );
		}
		return bs;
	}
	
	
	public void put( A key, B value ) {	
		
		lock( key );
		try {
			_map.put( key , value );
		} finally {
			unlock( key );
		}
	}
	
	
	public void remove( A key ) {
		
		lock( key );
		try {
			_map.remove( key );
		} finally {
			unlock( key );
		}			
	}
	
}