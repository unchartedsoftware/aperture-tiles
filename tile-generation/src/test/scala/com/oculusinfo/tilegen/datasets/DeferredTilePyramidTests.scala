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
package com.oculusinfo.tilegen.datasets



import java.util.{List => JavaList}
import org.scalatest.FunSuite
import java.util.Arrays
import org.json.JSONObject
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.TileIndex




/**
 * Make sure that deferred tile pyramid construction works as intended.
 */
class DeferredTilePyramidTests extends FunSuite {
	test("Tile Pyramid construction - web mercator, default") {
		val factory = new DeferredTilePyramidFactory(null, Arrays.asList("pyramid"))
		factory.readConfiguration(new JSONObject("{\"pyramid\": {}}"))
		val pyramid = factory.produce(classOf[DeferredTilePyramid]).getTilePyramid { () => (0.0, 0.0, 0.0, 0.0) }
		assert(pyramid.isInstanceOf[WebMercatorTilePyramid])
	}

	test("Tile Pyramid construction - web mercator, explicit, WebMercator") {
		val factory = new DeferredTilePyramidFactory(null, Arrays.asList("pyramid"))
		factory.readConfiguration(new JSONObject("{\"pyramid\": {\"type\": \"WebMercator\"}}"))
		val pyramid = factory.produce(classOf[DeferredTilePyramid]).getTilePyramid { () => (0.0, 0.0, 0.0, 0.0) }
		assert(pyramid.isInstanceOf[WebMercatorTilePyramid])
	}

	test("Tile Pyramid construction - web mercator, explicit, EPSG:900913") {
		val factory = new DeferredTilePyramidFactory(null, Arrays.asList("pyramid"))
		factory.readConfiguration(new JSONObject("{\"pyramid\": {\"type\": \"EPSG:900913\"}}"))
		val pyramid = factory.produce(classOf[DeferredTilePyramid]).getTilePyramid { () => (0.0, 0.0, 0.0, 0.0) }
		assert(pyramid.isInstanceOf[WebMercatorTilePyramid])
	}

	test("Tile Pyramid construction - area of interest, preset bounds") {
		val factory = new DeferredTilePyramidFactory(null, Arrays.asList("pyramid"))
		factory.readConfiguration(new JSONObject("{\"pyramid\": {\"type\": \"AreaOfInterest\", \"autobounds\": \"false\", \"minX\": 0.1, \"maxX\": 1.1, \"minY\": 2.1, \"maxY\": 3.1}}"))
		val pyramid = factory.produce(classOf[DeferredTilePyramid]).getTilePyramid { () => (0.0, 0.0, 200.0, 300.0) }
		assert(pyramid.isInstanceOf[AOITilePyramid])
		val bounds = pyramid.getTileBounds(new TileIndex(0, 0, 0))
		assert(0.1 === bounds.getMinX)
		assert(1.1 === bounds.getMaxX)
		assert(2.1 === bounds.getMinY)
		assert(3.1 === bounds.getMaxY)
	}

	test("Tile Pyramid construction - area of interest, autobounds") {
		val factory = new DeferredTilePyramidFactory(null, Arrays.asList("pyramid"))
		factory.readConfiguration(new JSONObject("{\"pyramid\": {\"type\": \"EPSG:4326\", \"autobounds\": \"true\", \"minX\": 0.1, \"maxX\": 1.1, \"minY\": 2.1, \"maxY\": 3.1}}"))
		val pyramid = factory.produce(classOf[DeferredTilePyramid]).getTilePyramid { () => (0.2, 0.3, 200.0, 300.0) }
		assert(pyramid.isInstanceOf[AOITilePyramid])
		val bounds = pyramid.getTileBounds(new TileIndex(0, 0, 0))
		assert(0.2 === bounds.getMinX)
		assert(0.3 === bounds.getMaxX)
		assert(200.0 === bounds.getMinY)
		assert(300.0 === bounds.getMaxY)
	}
}
