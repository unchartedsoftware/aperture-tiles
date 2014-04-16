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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class JSONUtilitiesTests {
    @Test
    public void testObjectCloning () throws JSONException {
        JSONObject source = new JSONObject("{name: 'abc', int: 3, double: 3.3, object: {a: 'a', b: 'b'}, array: [1, 2, 3]}");
        JSONObject clone = JsonUtilities.deepClone(source);

        Assert.assertFalse(source == clone);
        Assert.assertEquals(source.toString(), clone.toString());

        Assert.assertEquals("abc", source.getString("name"));
        Assert.assertEquals(3, source.getInt("int"));
        Assert.assertEquals(3.3,  source.getDouble("double"), 1E-12);

        Assert.assertTrue(source.getJSONObject("object") == source.getJSONObject("object"));
        Assert.assertFalse(source.getJSONObject("object") == clone.getJSONObject("object"));
        Assert.assertEquals(source.getJSONObject("object").toString(), clone.getJSONObject("object").toString());

        Assert.assertTrue(source.getJSONArray("array") == source.getJSONArray("array"));
        Assert.assertFalse(source.getJSONArray("array") == clone.getJSONArray("array"));
        Assert.assertEquals(source.getJSONArray("array").toString(), clone.getJSONArray("array").toString());
    }

    @Test
    public void testArrayCloning () throws JSONException {
        JSONArray source = new JSONArray("['abc', 3, 3.3, {a: 'a', b: 'b'}, [1, 2, 3]]");
        JSONArray clone = JsonUtilities.deepClone(source);

        Assert.assertFalse(source == clone);
        Assert.assertEquals(source.toString(), clone.toString());

        Assert.assertEquals("abc", source.getString(0));
        Assert.assertEquals(3, source.getInt(1));
        Assert.assertEquals(3.3, source.getDouble(2), 1E-12);

        Assert.assertTrue(source.getJSONObject(3) == source.getJSONObject(3));
        Assert.assertFalse(source.getJSONObject(3) == clone.getJSONObject(3));
        Assert.assertEquals(source.getJSONObject(3).toString(), clone.getJSONObject(3).toString());

        Assert.assertTrue(source.getJSONArray(4) == source.getJSONArray(4));
        Assert.assertFalse(source.getJSONArray(4) == clone.getJSONArray(4));
        Assert.assertEquals(source.getJSONArray(4).toString(), clone.getJSONArray(4).toString());
    }

    @Test
    public void testOverlaying () throws JSONException {
        JSONObject base = new JSONObject("{a: 'a', a1: 'a1', b: 1, b1: 2, c: 1.1, c1: 2.2, d: {a: 'aa', a1: 'aa1'}, e: ['a', 'a1', 1, 2, 1.1, 2.2, {b: 'bb', b1: 'bb1'}]}");
        JSONObject overlay = new JSONObject("{a1: 'a3', b1: 3, c1: 3.3, d: {a1: 'aa3'}, e:[null, 'a3', null, 3, null, 3.3, {b1: 'bb3'}]}");
        JSONObject result = JsonUtilities.overlayInPlace(base, overlay);

        Assert.assertTrue(base == result);

        // Make sure overlay is unchanged
        Assert.assertEquals(5, overlay.length());

        // Make sure unchanged entries remain unchanged
        Assert.assertEquals("a", result.getString("a"));
        Assert.assertEquals(1, result.getInt("b"));
        Assert.assertEquals(1.1, result.getDouble("c"), 1E-12);
        Assert.assertEquals("aa", result.getJSONObject("d").getString("a"));
        Assert.assertEquals("a", result.getJSONArray("e").getString(0));
        Assert.assertEquals(1, result.getJSONArray("e").getInt(2));
        Assert.assertEquals(1.1, result.getJSONArray("e").getDouble(4), 1E-12);
        Assert.assertEquals("bb", result.getJSONArray("e").getJSONObject(6).getString("b"));

        // Make sure changed entries have changed
        Assert.assertEquals("a3", result.getString("a1"));
        Assert.assertEquals(3, result.getInt("b1"));
        Assert.assertEquals(3.3, result.getDouble("c1"), 1E-12);
        Assert.assertEquals("aa3", result.getJSONObject("d").getString("a1"));
        Assert.assertEquals("a3", result.getJSONArray("e").getString(1));
        Assert.assertEquals(3, result.getJSONArray("e").getInt(3));
        Assert.assertEquals(3.3, result.getJSONArray("e").getDouble(5), 1E-12);
        Assert.assertEquals("bb3", result.getJSONArray("e").getJSONObject(6).getString("b1"));
    }
}
