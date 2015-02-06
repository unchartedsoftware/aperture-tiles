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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import com.oculusinfo.factory.util.Pair;

public class PyramidMetaDataTests {
	private static final double EPSILON = 1E-10;

	@Test
	public void testParsing () throws JSONException {
		String text = 
			("{\n" +
			 "    \"name\":\"Foobar\",\n" +
			 "    \"description\":\"Binned foobar data\",\n" +
			 "    \"tilesize\":255,\n" +
			 "    \"scheme\":\"TMS\",\n" +
			 "    \"projection\":\"web mercator\",\n" +
			 "    \"zoomlevels\":[0,1,2],\n" +
			 "    \"bounds\": [ -180.000000, -85.051129, 180.000000, 85.051129 ],\n" +
			 "    \"meta\": {\n" +
			 "        \"levelMaximums\": {\n" +
			 "            \"0\": \"1497547\",\n" +
			 "            \"1\": \"748773\",\n" +
			 "            \"2\": \"374386\"\n" +
			 "        },\n" +
			 "        \"levelMinimums\": {\n" +
			 "            \"0\": \"0\",\n" +
			 "            \"1\": \"2\",\n" +
			 "            \"2\": \"4\"\n" +
			 "        }\n" +
			 "    }\n" +
			 "}\n");
		PyramidMetaData metaData = new PyramidMetaData(text);
		Assert.assertEquals("Foobar", metaData.getName());
		Assert.assertEquals("Binned foobar data", metaData.getDescription());
		Assert.assertEquals(255, metaData.getTileSizeX());
		Assert.assertEquals(255, metaData.getTileSizeY());
		Assert.assertEquals("TMS", metaData.getScheme());
		Assert.assertEquals("web mercator", metaData.getProjection());
		Assert.assertEquals(0, metaData.getMinZoom());
		Assert.assertEquals(2, metaData.getMaxZoom());
		Assert.assertEquals(-180.0, metaData.getBounds().getMinX(), EPSILON);
		Assert.assertEquals(180.0, metaData.getBounds().getMaxX(), EPSILON);
		Assert.assertEquals(-85.051129, metaData.getBounds().getMinY(), EPSILON);
		Assert.assertEquals(85.051129, metaData.getBounds().getMaxY(), EPSILON);
//		Assert.assertEquals(3, metaData.getLevelMaximums().size());
//		Assert.assertEquals("1497547", metaData.getLevelMaximums().get(0));
//		Assert.assertEquals("1497547", metaData.getLevelMaximum(0));
//		Assert.assertEquals("748773", metaData.getLevelMaximums().get(1));
//		Assert.assertEquals("748773", metaData.getLevelMaximum(1));
//		Assert.assertEquals("374386", metaData.getLevelMaximums().get(2));
//		Assert.assertEquals("374386", metaData.getLevelMaximum(2));
//		Assert.assertEquals(3, metaData.getLevelMinimums().size());
//		Assert.assertEquals("0", metaData.getLevelMinimums().get(0));
//		Assert.assertEquals("0", metaData.getLevelMinimum(0));
//		Assert.assertEquals("2", metaData.getLevelMinimums().get(1));
//		Assert.assertEquals("2", metaData.getLevelMinimum(1));
//		Assert.assertEquals("4", metaData.getLevelMinimums().get(2));
//		Assert.assertEquals("4", metaData.getLevelMinimum(2));
	}

	@Test
	public void testMetaDataLevelList () throws JSONException {
		PyramidMetaData pmd = new PyramidMetaData("name", "description", 256, 256, 
		                                          "scheme", "projection",
		                                          new ArrayList<Integer>(),
		                                          new Rectangle2D.Double(0, 0, 1, 2),
		                                          null, null);
		Assert.assertTrue(pmd.getValidZoomLevels().isEmpty());
		pmd.addValidZoomLevels(Arrays.asList(3, 1));
		Assert.assertEquals(1, pmd.getValidZoomLevels().get(0).intValue());
		Assert.assertEquals(3, pmd.getValidZoomLevels().get(1).intValue());
		pmd.addValidZoomLevels(Arrays.asList(5, 2));
		Assert.assertEquals(1, pmd.getValidZoomLevels().get(0).intValue());
		Assert.assertEquals(2, pmd.getValidZoomLevels().get(1).intValue());
		Assert.assertEquals(3, pmd.getValidZoomLevels().get(2).intValue());
		Assert.assertEquals(5, pmd.getValidZoomLevels().get(3).intValue());
	}

	@Test
	public void testMetaDataWriting () throws JSONException {
		PyramidMetaData original = new PyramidMetaData("n", "d", 13, 13, "s", "p",
		                                               Arrays.asList(1, 2),
		                                               new Rectangle2D.Double(0, 0, 1, 2),
		                                               Arrays.asList(new Pair<Integer, String>(0, "12"),
		                                                             new Pair<Integer, String>(1, "9")),
		                                               Arrays.asList(new Pair<Integer, String>(0, "100"),
		                                                             new Pair<Integer, String>(1, "101")));
		String encoded = original.toString();
		PyramidMetaData copy = new PyramidMetaData(encoded);

		Assert.assertEquals(original.getName(), copy.getName());
		Assert.assertEquals(original.getDescription(), copy.getDescription());
		Assert.assertEquals(original.getTileSizeX(), copy.getTileSizeX());
		Assert.assertEquals(original.getTileSizeY(), copy.getTileSizeY());
		Assert.assertEquals(original.getScheme(), copy.getScheme());
		Assert.assertEquals(original.getProjection(), copy.getProjection());
		Assert.assertEquals(original.getMinZoom(), copy.getMinZoom());
		Assert.assertEquals(original.getMaxZoom(), copy.getMaxZoom());
		Assert.assertEquals(original.getBounds().getMinX(), copy.getBounds().getMinX(), EPSILON);
		Assert.assertEquals(original.getBounds().getMaxX(), copy.getBounds().getMaxX(), EPSILON);
		Assert.assertEquals(original.getBounds().getMinY(), copy.getBounds().getMinY(), EPSILON);
		Assert.assertEquals(original.getBounds().getMaxY(), copy.getBounds().getMaxY(), EPSILON);

//		Map<Integer, String> lvlMinsIn = original.getLevelMinimums();
//		Map<Integer, String> lvlMinsOut = copy.getLevelMinimums();
//		Assert.assertEquals(lvlMinsIn.size(), lvlMinsOut.size());
//		for (Integer k: lvlMinsIn.keySet()) {
//			Assert.assertTrue(lvlMinsOut.containsKey(k));
//			Assert.assertEquals(lvlMinsIn.get(k), lvlMinsOut.get(k));
//		}
//
//		Map<Integer, String> lvlMaxesIn = original.getLevelMaximums();
//		Map<Integer, String> lvlMaxesOut = copy.getLevelMaximums();
//		Assert.assertEquals(lvlMaxesIn.size(), lvlMaxesOut.size());
//		for (Integer k: lvlMaxesIn.keySet()) {
//			Assert.assertTrue(lvlMaxesOut.containsKey(k));
//			Assert.assertEquals(lvlMaxesIn.get(k), lvlMaxesOut.get(k));
//		}
	}
}
