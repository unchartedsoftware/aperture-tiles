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

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io.serialization.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.avro.Schema;

import com.oculusinfo.binning.io.serialization.AvroSchemaComposer;

/**
 * This class stores, retrieves, and constructs standardized, patterned schema
 * based on passed-in arguments, only constructing them as necessary.
 */
public class PatternedSchemaStore {
    private String              _pattern;
    private Map<Object, Schema> _schema;
    private ReadWriteLock       _lock;



    /**
     * Create a schema store based off a given pattern.
     * @param pattern The pattern from which to create schema.  The format
     *                is the same as is accepted by String.format; all
     *                arguments to the pattern should be passed in to
     *                the getSchema method.
     */
    public PatternedSchemaStore(String pattern) {
        _pattern = pattern;
        _schema = new HashMap<>();
        _lock = new ReentrantReadWriteLock();
    }

    /**
     * Get a schema associated with a particular key. This gets a
     * pre-existing schema if one exists, or creates one, if none exist.
     *
     * @param key The key associated with the schema.
     * @param arguments The arguments to the pattern on which this
     *                  PatternedSchemaStore is based.
     * @return A schema based of our pattern and the given arguments.
     */
    public Schema getSchema (Object key, Object... arguments) {
        _lock.readLock().lock();
        try {
            if (!_schema.containsKey(key)) {
                // Can't upgrade lock - must release and re-obtain.
                _lock.readLock().unlock();
                _lock.writeLock().lock();
                try {
                    if (!_schema.containsKey(key)) {
                        String substituted = String.format(_pattern, arguments);
                        Schema resolved = new AvroSchemaComposer().add(substituted).resolved();
                        _schema.put(key, resolved);
                    }
                } finally {
                    // but we can down-grade the lock when we're done, so we leave this if block in the same state we entered.
                    _lock.readLock().lock();
                    _lock.writeLock().unlock();
                }
            }
            return _schema.get(key);
        } finally {
            _lock.readLock().unlock();
        }
    }
}
