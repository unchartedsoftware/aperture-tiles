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

import com.oculusinfo.tile.rendering.transformations.value.LinearCappedValueTransformer;
import org.junit.Assert;
import org.junit.Test;

public class LinearValueTransformerTests {
	private final static double EPS = 0.00001;

	@Test
	public void testGeneral () {
		LinearCappedValueTransformer t = new LinearCappedValueTransformer(100,200);
		Assert.assertEquals(t.transform(100.0), 0, EPS);
		Assert.assertEquals(t.transform(150.0), 0.5, EPS);
		Assert.assertEquals(t.transform(200.0), 1, EPS);
	}

	@Test
	public void testOutOfBounds () {
		LinearCappedValueTransformer t = new LinearCappedValueTransformer(100,200);
		Assert.assertEquals(t.transform(90.0), 0, EPS);
		Assert.assertEquals(t.transform(5000.0), 1, EPS);
	}

	@Test
	public void testNegativeSpan () {
		LinearCappedValueTransformer t = new LinearCappedValueTransformer(-10,20);
		Assert.assertEquals(t.transform(-10.0), 0, EPS);
		Assert.assertEquals(t.transform(5.0), 0.5, EPS);
		Assert.assertEquals(t.transform(20.0), 1, EPS);
	}
}
