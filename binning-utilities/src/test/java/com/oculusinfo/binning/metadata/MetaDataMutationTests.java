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

import com.oculusinfo.binning.metadata.updaters.MetaDataF0p0T1p0;
import com.oculusinfo.factory.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetaDataMutationTests {
    private static final double EPSILON = 1E-12;

    @Test
	public void testVersionMutationPath () {
		PyramidMetaDataVersionMutator.ALL_MUTATORS.clear();
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v0", "v1"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v1", "v2"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2", "v2a"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2", "v2b"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2", "v2c"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2a", "v3a"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2b", "v3a"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v2c", "v3b"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v3a", "v4"));
		PyramidMetaDataVersionMutator.ALL_MUTATORS.add(new PyramidMetaDataVersionMutator("v4", "v5"));
		
		// Register the real 0.0 -> 1.0 mutator for testing
		MetaDataF0p0T1p0.register();

		List<PyramidMetaDataVersionMutator> path = PyramidMetaDataVersionMutator.getMutators("v2", "v5");
		Assert.assertEquals(4, path.size());
		Assert.assertTrue(new Pair<String, String>("v2", "v2a").equals(path.get(0).getVersionBounds()) ||
		                  new Pair<String, String>("v2", "v2b").equals(path.get(0).getVersionBounds()));
		Assert.assertTrue(new Pair<String, String>("v2a", "v3a").equals(path.get(1).getVersionBounds()) ||
		                  new Pair<String, String>("v2b", "v3a").equals(path.get(1).getVersionBounds()));
		Assert.assertEquals(new Pair<String, String>("v3a", "v4"), path.get(2).getVersionBounds());
		Assert.assertEquals(new Pair<String, String>("v4", "v5"), path.get(3).getVersionBounds());

		Assert.assertNull(PyramidMetaDataVersionMutator.getMutators("v2c", "v5"));
	}

	@Test
	public void testV0_0ToV1_0Update () throws JSONException {
	    String initialText = ("{"+
	            "   \"name\":\"bitcoin.time.amount\","+
	            "   \"description\":\"Binned bitcoin.time.amount data showing time vs. amount\","+
	            "   \"tilesize\":256,"+
	            "   \"scheme\":\"TMS\","+
	            "   \"projection\":\"EPSG:4326\","+
	            "   \"minzoom\":0,"+
	            "   \"maxzoom\":18,"+
	            "   \"bounds\": [ 1231002905000.0, 0.0, 1365618170007.8, 500000.0 ],"+
	            "   \"meta\": {"+
	            "       \"levelMinimums\": {"+
	            "           \"0\": \"0.0\","+
	            "           \"1\": \"0.0\","+
	            "           \"2\": \"0.0\","+
	            "           \"3\": \"0.0\","+
	            "           \"4\": \"0.0\","+
	            "           \"5\": \"0.0\","+
	            "           \"6\": \"0.0\","+
	            "           \"7\": \"0.0\","+
	            "           \"8\": \"0.0\","+
	            "           \"9\": \"0.0\","+
	            "           \"10\": \"0.0\","+
	            "           \"11\": \"0.0\","+
	            "           \"12\": \"0.0\""+
	            "       },"+
	            "       \"levelMaximums\": {"+
	            "           \"0\": \"1016406.0\","+
	            "           \"1\": \"556010.0\","+
	            "           \"2\": \"283705.0\","+
	            "           \"3\": \"154580.0\","+
	            "           \"4\": \"81997.0\","+
	            "           \"5\": \"59372.0\","+
	            "           \"6\": \"48793.0\","+
	            "           \"7\": \"32744.0\","+
	            "           \"8\": \"18850.0\","+
	            "           \"9\": \"15743.0\","+
	            "           \"10\": \"15671.0\","+
	            "           \"11\": \"14388.0\","+
	            "           \"12\": \"12861.0\""+
	            "       }"+
	            "   }"+
	            "}");

	    JSONObject expected = new JSONObject("{"+
	            "   \"name\":\"bitcoin.time.amount\","+
	            "   \"description\":\"Binned bitcoin.time.amount data showing time vs. amount\","+
	            "   \"version\":\"1.0\","+
	            "   \"tilesizex\":256,"+
	            "   \"tilesizey\":256,"+
	            "   \"scheme\":\"TMS\","+
	            "   \"projection\":\"EPSG:4326\","+
	            "   \"zoomlevels\":[0,1,2,3,4,5,6,7,8,9,10,11,12],"+
	            "   \"bounds\": [ 1231002905000.0, 0.0, 1365618170007.8, 500000.0 ],"+
	            "   \"meta\":{"+
	            "       \"global\":{\"minimum\":\"0.0\",\"maximum\":\"1016406.0\"},"+
	            "       \"0\":     {\"minimum\":\"0.0\",\"maximum\":\"1016406.0\"},"+
	            "       \"1\":     {\"minimum\":\"0.0\",\"maximum\":\"556010.0\"},"+
	            "       \"2\":     {\"minimum\":\"0.0\",\"maximum\":\"283705.0\"},"+
	            "       \"3\":     {\"minimum\":\"0.0\",\"maximum\":\"154580.0\"},"+
	            "       \"4\":     {\"minimum\":\"0.0\",\"maximum\":\"81997.0\"},"+
	            "       \"5\":     {\"minimum\":\"0.0\",\"maximum\":\"59372.0\"},"+
	            "       \"6\":     {\"minimum\":\"0.0\",\"maximum\":\"48793.0\"},"+
	            "       \"7\":     {\"minimum\":\"0.0\",\"maximum\":\"32744.0\"},"+
	            "       \"8\":     {\"minimum\":\"0.0\",\"maximum\":\"18850.0\"},"+
	            "       \"9\":     {\"minimum\":\"0.0\",\"maximum\":\"15743.0\"},"+
	            "       \"10\":    {\"minimum\":\"0.0\",\"maximum\":\"15671.0\"},"+
	            "       \"11\":    {\"minimum\":\"0.0\",\"maximum\":\"14388.0\"},"+
	            "       \"12\":    {\"minimum\":\"0.0\",\"maximum\":\"12861.0\"}"+
	            "   }"+
	            "}");

	    // Compare just as JSON
	    JSONObject initial = new JSONObject(initialText);
	    PyramidMetaDataVersionMutator.updateMetaData(initial, "1.0");

	    compareJson("", expected, initial);

	    // Compare as metadata
	    PyramidMetaData metaData = new PyramidMetaData(initialText);
	    compareJson("", expected, metaData.getRawData());

	    metaData = new PyramidMetaData(new JSONObject(initialText));
        compareJson("", expected, metaData.getRawData());
	}

	private <T> void compareSets(String message, Set<T> expected, Set<T> actual) {
	    Assert.assertEquals(message+": length", expected.size(), actual.size());
	    Set<T> extra = new HashSet<>(expected);
	    extra.removeAll(actual);
	    Assert.assertEquals(message+": key mismatches", 0, extra.size());
	}

    private void compareJson (String curPath, JSONObject expected, JSONObject actual) throws JSONException {
        Set<String> expectedKeys = new HashSet<>(Arrays.asList(JSONObject.getNames(expected)));
        Set<String> actualKeys = new HashSet<>(Arrays.asList(JSONObject.getNames(expected)));
        compareSets(curPath+" keys", expectedKeys, actualKeys);
        for (String key: expectedKeys) {
            Object expectedValue = expected.get(key);
            Object actualValue = actual.get(key);
            if (expectedValue instanceof JSONObject) {
                String newPath = (curPath.isEmpty() ? key : curPath+"."+key);
                compareJson(newPath, (JSONObject) expectedValue, (JSONObject) actualValue);
            } else if (expectedValue instanceof JSONArray) {
                String newPath = (curPath.isEmpty() ? key : curPath+"."+key);
                compareJson(newPath, (JSONArray) expectedValue, (JSONArray) actualValue);
            } else {
                if (expectedValue instanceof Double || actualValue instanceof Double) {
                    double de = expected.getDouble(key);
                    double da = actual.getDouble(key);
                    Assert.assertEquals(curPath+" double value", de, da, EPSILON);
                } else if (expectedValue instanceof Long || actualValue instanceof Long) {
                    long le = expected.getLong(key);
                    long la = actual.getLong(key);
                    Assert.assertEquals(curPath+" long value", le, la);
                } else if (expectedValue instanceof Integer || actualValue instanceof Integer) {
                    int ie = expected.getInt(key);
                    int ia = actual.getInt(key);
                    Assert.assertEquals(curPath+" int value", ie, ia);
                } else if (expectedValue instanceof Boolean || actualValue instanceof Boolean) {
                    boolean be = expected.getBoolean(key);
                    boolean ba = actual.getBoolean(key);
                    Assert.assertEquals(curPath+" boolean value", be, ba);
                } else if (expectedValue instanceof String || actualValue instanceof String) {
                    String se = expected.getString(key);
                    String sa = actual.getString(key);
                    Assert.assertEquals(curPath+" string value", se, sa);
                } else {
                    Assert.assertEquals(curPath+" value", expected, actual);
                }
            }
        }
    }

    private void compareJson (String curPath, JSONArray expected, JSONArray actual) throws JSONException {
        Assert.assertEquals(curPath+" length", expected.length(), actual.length());
        for (int key=0; key<expected.length(); ++key) {
            Object expectedValue = expected.get(key);
            Object actualValue = actual.get(key);
            if (expectedValue instanceof JSONObject) {
                String newPath = (curPath.isEmpty() ? ""+key : curPath+"."+key);
                compareJson(newPath, (JSONObject) expectedValue, (JSONObject) actualValue);
            } else if (expectedValue instanceof JSONArray) {
                String newPath = (curPath.isEmpty() ? ""+key : curPath+"."+key);
                compareJson(newPath, (JSONArray) expectedValue, (JSONArray) actualValue);
            } else {
                if (expectedValue instanceof Double || actualValue instanceof Double) {
                    double de = expected.getDouble(key);
                    double da = actual.getDouble(key);
                    Assert.assertEquals(curPath+" double value", de, da, EPSILON);
                } else if (expectedValue instanceof Long || actualValue instanceof Long) {
                    long le = expected.getLong(key);
                    long la = actual.getLong(key);
                    Assert.assertEquals(curPath+" long value", le, la);
                } else if (expectedValue instanceof Integer || actualValue instanceof Integer) {
                    int ie = expected.getInt(key);
                    int ia = actual.getInt(key);
                    Assert.assertEquals(curPath+" int value", ie, ia);
                } else if (expectedValue instanceof Boolean || actualValue instanceof Boolean) {
                    boolean be = expected.getBoolean(key);
                    boolean ba = actual.getBoolean(key);
                    Assert.assertEquals(curPath+" boolean value", be, ba);
                } else if (expectedValue instanceof String || actualValue instanceof String) {
                    String se = expected.getString(key);
                    String sa = actual.getString(key);
                    Assert.assertEquals(curPath+" string value", se, sa);
                } else {
                    Assert.assertEquals(curPath+" value", expected, actual);
                }
            }
        }
    }
}
