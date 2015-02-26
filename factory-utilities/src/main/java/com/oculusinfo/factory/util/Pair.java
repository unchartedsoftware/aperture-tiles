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
package com.oculusinfo.factory.util;



import java.io.Serializable;



/**
 * Simple java 2-element tuple class
 * 
 * @author nkronenfeld
 */
public class Pair<S, T> implements Serializable {
	private static final long serialVersionUID = -7793621678311661841L;



	private S _s;
	private T _t;
	private Pair () {
		_s = null;
		_t = null;
	}
	public Pair (S s, T t) {
		this();
		_s = s;
		_t = t;
	}
	public S getFirst () {return _s;}
	public T getSecond () {return _t;}



	@Override
	public int hashCode () {
		int h = 0;
		if (null != _s) h = h+_s.hashCode();
		if (null != _t) h = h*7+_t.hashCode();
		return h;
	}

	@Override
	public boolean equals (Object that) {
		if (this == that) return true;
		if (null == that) return false;
		if (!(that instanceof Pair)) return false;

		Pair<?, ?> pThat = (Pair<?,  ?>) that;
		if (!objectsEqual(_s, pThat._s)) return false;
		if (!objectsEqual(_t, pThat._t)) return false;
		return true;
	}

	private boolean objectsEqual (Object a, Object b) {
		if (null == a) return null == b;
		return a.equals(b);
	}

	@Override
	public String toString () {
		return "("+toString(_s)+", "+toString(_t)+")";
	}

	private String toString (Object a) {
		if (null == a) return "null";
		return a.toString();
	}
}
