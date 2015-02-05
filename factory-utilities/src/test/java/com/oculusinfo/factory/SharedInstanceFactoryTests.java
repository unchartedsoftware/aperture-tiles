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
package com.oculusinfo.factory;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class SharedInstanceFactoryTests {
	static private int _next = 0;
	class TestSingletonConfigFac extends SharedInstanceFactory<Integer> {
		public TestSingletonConfigFac () {
			super(Integer.class, null, null);
		}
		@Override
		protected Integer createInstance () {
			return _next++;
		}
	}

	@Test
	public void testSingletonCreation () throws Exception {
		TestSingletonConfigFac factory1 = new TestSingletonConfigFac();
		JSONObject config1 = new JSONObject("{'a':1, 'b':{'c':2, 'd':3}}");
		factory1.readConfiguration(config1);
		int product1 = factory1.produce(Integer.class);

		TestSingletonConfigFac factory2 = new TestSingletonConfigFac();
		JSONObject config2 = new JSONObject("{'a':1, 'b':{'c':2, 'd':3}}");
		factory2.readConfiguration(config2);
		int product2 = factory2.produce(Integer.class);

		Assert.assertEquals(product1, product2);

		TestSingletonConfigFac factory3 = new TestSingletonConfigFac();
		JSONObject config3 = new JSONObject("{'a':1, 'b':{'c':2, 'd':3, 'e': 4}}");
		factory3.readConfiguration(config3);
		int product3 = factory3.produce(Integer.class);

		Assert.assertNotSame(product1, product3);
	}
}
