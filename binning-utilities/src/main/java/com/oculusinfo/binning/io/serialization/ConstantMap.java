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
package com.oculusinfo.binning.io.serialization;

import java.util.*;

/** A map that maps every integer to a constant value */
public class ConstantMap<CT> implements Map<Integer, CT> {
	private CT _value;

	public ConstantMap(CT value) {
		_value = value;
	}
	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof Integer);
	}

	@Override
	public boolean containsValue(Object value) {
		return _value.equals(value);
	}

	@Override
	public CT get(Object key) {
		return _value;
	}

	@Override
	public CT put(Integer key, CT value) {
		throw new UnsupportedOperationException("Can set values in a constant map");
	}

	@Override
	public CT remove(Object key) {
		throw new UnsupportedOperationException("Can remove values from a constant map");
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends CT> m) {
		throw new UnsupportedOperationException("Can set values in a constant map");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("Can clear a constant map");
	}

	@Override
	public Set<Integer> keySet() {
		throw new UnsupportedOperationException("It is impossible to return the set of all integers");
	}

	@Override
	public Collection<CT> values() {
		return Collections.unmodifiableSet(new HashSet<CT>(Arrays.asList(_value)));
	}

	@Override
	public Set<Entry<Integer, CT>> entrySet() {
		throw new UnsupportedOperationException("It is impossible to return the set of all integers");
	}
}
