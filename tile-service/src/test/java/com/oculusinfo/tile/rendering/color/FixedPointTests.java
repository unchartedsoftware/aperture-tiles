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
package com.oculusinfo.tile.rendering.color;

import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.JSONNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class FixedPointTests {
	@Test
	public void testPropertyFileSerialization () throws ConfigurationException {
		FixedPointProperty prop = new FixedPointProperty("", "");
		FixedPoint pt = new FixedPoint(3.0, 4.0);
		String encoded = prop.encode(pt);
		FixedPoint redux = prop.unencode(encoded);
		Assert.assertEquals(pt, redux);
	}

	@Test
	public void testJSONSerialization () throws JSONException, ConfigurationException {
		FixedPointProperty prop = new FixedPointProperty("", "");
		FixedPoint pt = new FixedPoint(3.0, 4.0);
		JSONObject root = new JSONObject();
		JSONNode node = new JSONNode(root, "fixedpoint");
		prop.encodeJSON(node, pt);
		FixedPoint redux = prop.unencodeJSON(node);
		Assert.assertEquals(pt, redux);
	}
}
