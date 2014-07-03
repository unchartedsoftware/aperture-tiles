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
package com.oculusinfo.binning.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class JsonMutationTests {
	private static final double EPSILON = 1E-12;

	private JSONObject getBaseTestObject () throws JSONException {
		return new JSONObject(
		                      "{"
		                      +"'s1':'abc',"
		                      +"'i1':3,"
		                      +"'d1':4.4,"
		                      +"'b1':true,"
		                      +"'b2':false,"
		                      +"'a1':['def', -3, -4.4, false],"
		                      +"'o1':{'s1':'ghi','i1':2,'d1':3.14159,'b1':false,'a1':[],'o1':{}}"
		                      +"}");
	}

	private void testBaseObject (JSONObject base) throws JSONException {
		String[] baseProps = JSONObject.getNames(base);
		Assert.assertEquals(7, baseProps.length);
		Set<String> expectedProps = new HashSet<>(Arrays.asList("s1", "i1", "d1", "b1", "b2", "a1", "o1"));
		for (String prop: baseProps) expectedProps.remove(prop);
		Assert.assertTrue(expectedProps.isEmpty());

		Assert.assertEquals("abc", base.get("s1"));
		Assert.assertEquals(3, base.getInt("i1"));
		Assert.assertEquals(4.4, base.getDouble("d1"), EPSILON);
		Assert.assertEquals(true, base.getBoolean("b1"));
		Assert.assertEquals(false, base.getBoolean("b2"));

		JSONArray a1 = base.getJSONArray("a1");
		Assert.assertEquals(4, a1.length());
		Assert.assertEquals("def", a1.get(0));
		Assert.assertEquals(-3, a1.getInt(1));
		Assert.assertEquals(-4.4, a1.getDouble(2), EPSILON);
		Assert.assertEquals(false, a1.getBoolean(3));

		JSONObject o1 = base.getJSONObject("o1");

		baseProps = JSONObject.getNames(base);
		Assert.assertEquals(7, baseProps.length);
		expectedProps = new HashSet<>(Arrays.asList("s1", "i1", "d1", "b1", "a1", "o1"));
		for (String prop: baseProps) expectedProps.remove(prop);
		Assert.assertTrue(expectedProps.isEmpty());

		Assert.assertEquals("ghi", o1.get("s1"));
		Assert.assertEquals(2, o1.getInt("i1"));
		Assert.assertEquals(3.14159, o1.getDouble("d1"), EPSILON);
		JSONArray o1a1 = o1.getJSONArray("a1");
		Assert.assertEquals(0, o1a1.length());
		JSONObject o1o1 = o1.getJSONObject("o1");
		Assert.assertEquals(0, o1o1.length());
	}

	@Test
	public void baseTest () throws JSONException {
		JSONObject base = getBaseTestObject();
		testBaseObject(base);
	}

	@Test
	public void addRemoveLevel1Test () throws JSONException {
		JSONObject base = getBaseTestObject();
		JsonMutator add = new AddPropertyMutator("jkl", "s2");
		JsonMutator remove = new RemovePropertyMutator("s2");

		// Make sure simple add/remove works
		add.mutateJson(base);
		Assert.assertEquals("jkl", base.get("s2"));
		remove.mutateJson(base);
		Assert.assertFalse(base.has("s2"));
		testBaseObject(base);

		// Make sure add doesn't overwrite existing entry
		base.put("s2", "lmn");
		add.mutateJson(base);
		Assert.assertEquals("lmn", base.get("s2"));
		// Remove won't be able to distinguish original from added; don't bother
		// testing that.
	}

	@Test
	public void addRemoveLevel2Test () throws JSONException {
		JSONObject base = getBaseTestObject();
		JsonMutator add = new AddPropertyMutator("1", "o1", "i2");
		JsonMutator remove = new RemovePropertyMutator("o1", "i2");

		// Make sure add/remove works
		add.mutateJson(base);
		Assert.assertEquals(1, base.getJSONObject("o1").getInt("i2"));
		remove.mutateJson(base);
		Assert.assertFalse(base.getJSONObject("o1").has("i2"));
		testBaseObject(base);

		// Make sure add doesn't overwrite existing entry
		base.getJSONObject("o1").put("i2", 3);
		add.mutateJson(base);
		Assert.assertEquals(3, base.getJSONObject("o1").getInt("i2"));
		// Remove won't be able to distinguish original from added; don't bother
		// testing that.
	}

	@Test
	public void testAddRemoveNewNode () throws JSONException {
		JSONObject base = getBaseTestObject();
		JsonMutator add = new AddPropertyMutator("2.718", "o2", "d1");
		JsonMutator remove = new RemovePropertyMutator("o2", "d1");

		// Make sure add/remove works
		add.mutateJson(base);
		Assert.assertEquals(2.718, base.getJSONObject("o2").getDouble("d1"), EPSILON);
		remove.mutateJson(base);
		Assert.assertFalse(base.has("o2"));
		testBaseObject(base);
	}

	@Test(expected=JSONException.class)
	public void testNonNodeLeafAddition () throws JSONException {
		JSONObject base;
		try {
			base = getBaseTestObject();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		JsonMutator add = new AddPropertyMutator("abc", "s1", "s1");
		add.mutateJson(base);
	}

	@Test
	public void testMoveNode () throws JSONException {
		JSONObject base = getBaseTestObject();

		// Create branch to move (note misspelling)
		new AddPropertyMutator("leaf", "o2", "branche", "twig").mutateJson(base);
		Assert.assertEquals(1, base.getJSONObject("o2").length());
		Assert.assertEquals(1, base.getJSONObject("o2").getJSONObject("branche").length());
		Assert.assertEquals("leaf", base.getJSONObject("o2").getJSONObject("branche").get("twig"));

		// Move branch
		new PropertyRelocationMutator("spelling correction",
		                              new String[] {"o2", "branche"},
		                              new String[] {"o3", "branch"}, true).mutateJson(base);
		Assert.assertFalse(base.has("o2"));
		Assert.assertEquals(1, base.getJSONObject("o3").length());
		Assert.assertEquals(1, base.getJSONObject("o3").getJSONObject("branch").length());
		Assert.assertEquals("leaf", base.getJSONObject("o3").getJSONObject("branch").get("twig"));
        
	}

	@Test
	public void testKeyToArray () throws JSONException {
	    JSONObject base = getBaseTestObject();

	    new PropertyIndexToArrayMutator<String>(new String[] {"o1", "(\\p{Alpha})\\d"},
	                                            new String[] {"a2"},
	                                            "\\1.1") {
	        @Override
	        protected String mutateValue (String value) {
	            return value;
	        }

	        @Override
	        protected void sort (List<String> values) {
	            Collections.sort(values);
	        }
	    }.mutateJson(base);

	    Assert.assertEquals(6, base.getJSONArray("a2").length());
        Set<String> values = new HashSet<>();
        for (int i=0; i<6; ++i) {
            values.add(base.getJSONArray("a2").optString(i));
        }
        Assert.assertTrue(values.contains("s"));
        Assert.assertTrue(values.contains("i"));
        Assert.assertTrue(values.contains("d"));
        Assert.assertTrue(values.contains("b"));
        Assert.assertTrue(values.contains("a"));
        Assert.assertTrue(values.contains("o"));
	}
}
