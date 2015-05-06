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
package com.oculusinfo.binning.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

	@Test
	public void testArrayOverlayTruncation () throws Exception {
		JSONArray base = new JSONArray("['a', 'b', 'c', 'd', 'e', 'f']");
		JSONArray overlay = new JSONArray("[1, 2, null, 4]");
		JSONArray result = JsonUtilities.overlay(base, overlay);

		Assert.assertEquals(4, result.length());
		Assert.assertEquals(1, result.getInt(0));
		Assert.assertEquals(2, result.getInt(1));
		Assert.assertEquals("c", result.getString(2));
		Assert.assertEquals(4, result.getInt(3));
	}

	@Test
	public void testPropertyClass () throws Exception {
		JSONObject base = new JSONObject("{a: 'a', b: {c: 'c', d:['a', 'a1', 1, 2, 1.1, 2.2]}}");
		Properties props = JsonUtilities.jsonObjToProperties(base);
		Assert.assertEquals("a", props.getProperty("a"));
		Assert.assertEquals("c", props.getProperty("b.c"));
		Assert.assertEquals("a", props.getProperty("b.d.0"));
		Assert.assertEquals("a1", props.getProperty("b.d.1"));
		Assert.assertEquals("1", props.getProperty("b.d.2"));
		Assert.assertEquals("2", props.getProperty("b.d.3"));
		Assert.assertEquals("1.1", props.getProperty("b.d.4"));
		Assert.assertEquals("2.2", props.getProperty("b.d.5"));
		Assert.assertEquals(Properties.class, props.getClass());
	}

	@Test
	public void testPropertyToJSONConversion () throws Exception {
		Properties p = new Properties();
		p.setProperty("a", "aval");
		p.setProperty("b.0", "bval0");
		p.setProperty("b.2", "bval2");
		p.setProperty("c.1", "cval1");
		p.setProperty("c.a", "cvala");
		p.setProperty("c.b.a", "cbaval");
		p.setProperty("c.b.b", "cbbval");
		p.setProperty("d", "{\"x\":\"xval\", \"y\": 10}");

		JSONObject expected = new JSONObject("{\"a\": \"aval\", \"b\": [\"bval0\", null, \"bval2\"], " +
			"\"c\": {\"1\": \"cval1\", \"a\": \"cvala\", \"b\": {\"a\": \"cbaval\", \"b\": \"cbbval\"}}, " +
			"\"d\" : { \"x\" : \"xval\", \"y\" : 10 }}");
		JSONObject actual = JsonUtilities.propertiesObjToJSON(p);
		assertJsonEqual(expected, actual);
	}

	@Test
	public void testStringMapToJSONConversion () throws Exception {
		Map<String, String> p = new HashMap<>();
		p.put("a", "aval");
		p.put("b.0", "bval0");
		p.put("b.2", "bval2");
		p.put("c.1", "cval1");
		p.put("c.a", "cvala");
		p.put("c.b.a", "cbaval");
		p.put("c.b.b", "cbbval");

		JSONObject expected = new JSONObject("{\"a\": \"aval\", \"b\": [\"bval0\", null, \"bval2\"], \"c\": {\"1\": \"cval1\", \"a\": \"cvala\", \"b\": {\"a\": \"cbaval\", \"b\": \"cbbval\"}}}");
		JSONObject actual = JsonUtilities.mapToJSON(p);
		assertJsonEqual(expected, actual);
	}

	@Test
	public void testJSONObjectKeyExpansion () throws Exception {
		JSONObject source = new JSONObject("{\n"+
		                                   "  \"person.name.first\": \"Alice\",\n"+
		                                   "  \"person.name.middle\": \"Barbara\",\n"+
		                                   "  \"person.name.last\": \"Cavendish\",\n"+
		                                   "  \"person.school.location.state\": \"Delaware\",\n"+
		                                   "  \"person\": {\n"+
		                                   "    \"school.location.country\": \"US\",\n"+
		                                   "    \"school.grades\": [\n"+
		                                   "      {\"year\": \"freshman\",\n"+
		                                   "       \"bySemester.semester1\": \"A\",\n"+
		                                   "       \"bySemester.semester2\": \"B\"},\n"+
		                                   "      {\"year\":\"sophomore\",\n"+
		                                   "       \"bySemester.semester1\": \"C\",\n"+
		                                   "       \"bySemester\": {\"semester2\": \"D\", \"semester3\": \"E\"}}\n"+
		                                   "    ]\n"+
		                                   "  }\n"+
		                                   "}");
		JSONObject target = new JSONObject("{\n"+
		                                   "  \"person\": {\n"+
		                                   "    \"name\": { \"first\": \"Alice\",\n"+
		                                   "                \"middle\": \"Barbara\",\n"+
		                                   "                \"last\": \"Cavendish\" },\n"+
		                                   "    \"school\": {\n"+
		                                   "      \"location\": { \"state\": \"Delaware\",\n"+
		                                   "                      \"country\": \"US\" },\n"+
		                                   "      \"grades\": [\n"+
		                                   "        { \"year\": \"freshman\",\n"+
		                                   "          \"bySemester\": { \"semester1\": \"A\",\n"+
		                                   "                            \"semester2\": \"B\" } },\n"+
		                                   "        { \"year\": \"sophomore\",\n"+
		                                   "          \"bySemester\": { \"semester1\": \"C\",\n"+
		                                   "                            \"semester2\": \"D\",\n"+
		                                   "                            \"semester3\": \"E\" } }\n"+
		                                   "      ]\n"+
		                                   "    }\n"+
		                                   "  }\n"+
		                                   "}");

		assertJsonEqual(target, JsonUtilities.expandKeysInPlace(source));
	}

	@Test
	public void testIsJson() throws Exception {
		Assert.assertFalse(JsonUtilities.isJSON("{343493043 }"));
		Assert.assertTrue(JsonUtilities.isJSON("{\"a\": { \"b\": [{\"c\":1},{\"d\":2}] } }"));
	}

	public static void assertJsonEqual (JSONObject expected, JSONObject actual) {
		Map<String, Object> mapE = JsonUtilities.jsonObjToMap(expected);
		Map<String, Object> mapA = JsonUtilities.jsonObjToMap(actual);

		assertEquals(mapE, mapA);
	}

	public static void assertJsonEqual (JSONArray expected, JSONArray actual) {
		List<Object> listE = JsonUtilities.jsonArrayToList(expected);
		List<Object> listA = JsonUtilities.jsonArrayToList(actual);

		assertEquals(listE, listA);
	}

	private static void assertEquals (Object expected, Object actual) {
		if (expected instanceof List) {
			Assert.assertTrue("Expected a list, but didn't get one", actual instanceof List);
			List<?> eList = (List<?>) expected;
			List<?> aList = (List<?>) actual;
			Assert.assertEquals(eList.size(), aList.size());
			for (int i=0; i<eList.size(); ++i) {
				assertEquals(eList.get(i), aList.get(i));
			}
		} else if (expected instanceof Map) {
			Assert.assertTrue("Expected a map, but didn't get one", actual instanceof Map);
			Map<?, ?> eMap = (Map<?, ?>) expected;
			Map<?, ?> aMap = (Map<?, ?>) actual;
			Assert.assertEquals(eMap.size(), aMap.size());
			for (Object key: eMap.keySet()) {
				Assert.assertTrue(aMap.containsKey(key));
				assertEquals(eMap.get(key), aMap.get(key));
			}
		} else {
			Assert.assertEquals(expected, actual);
		}
	}
}
