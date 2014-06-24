/*
 * Copyright (c) 2013 Oculus Info Inc.
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
 
package com.oculusinfo.twitter.binning;



import com.esotericsoftware.kryo.Kryo;

import com.oculusinfo.tilegen.kryo.TileRegistrator;



public class TwitterTopicTileRegistrator extends TileRegistrator {
	@Override
	public void registerClasses (Kryo kryo) {
		super.registerClasses(kryo);
		kryo.register(TwitterDemoTopicRecord.class);
		kryo.register(int[].class);
		kryo.register(long[].class);
		kryo.register(String[].class);
		kryo.register(scala.collection.convert.Wrappers.SeqWrapper.class);
		//		kryo.register(scala.collection.immutable.Map$EmptyMap.class);
		try {
 			kryo.register(Class.forName("scala.collection.immutable.Map$EmptyMap$"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
