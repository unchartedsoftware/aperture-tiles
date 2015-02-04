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
package com.oculusinfo.tile.rendering.value;

import com.oculusinfo.tile.rendering.transformations.value.Log10ValueTransformer;
import org.junit.Assert;
import org.junit.Test;

public class Log10ValueTransformerTests {
	private final static double EPS = 0.00001;

	@Test
	public void testSimple () {
		Log10ValueTransformer t = new Log10ValueTransformer(1,100);
		Assert.assertEquals(0, t.transform(1.0), EPS);
		Assert.assertEquals(0.5, t.transform(10.0), EPS);
		Assert.assertEquals(1, t.transform(100.0), EPS);
	}


	@Test
	public void testGeneral () {
		Log10ValueTransformer t = new Log10ValueTransformer(123,456);
		Assert.assertEquals(0, t.transform(123.0), EPS);
		Assert.assertEquals(1, t.transform(456.0), EPS);
	}


	@Test
	public void testOutOfBounds () {
		Log10ValueTransformer t = new Log10ValueTransformer(100,200);
		Assert.assertEquals(0, t.transform(90.0), EPS);
		Assert.assertEquals(1, t.transform(5000.0), EPS);
	}

}
