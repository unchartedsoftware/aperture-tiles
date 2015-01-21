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

import scala.collection.JavaConverters._

import com.oculusinfo.binning.impl.{AOITilePyramid, WebMercatorTilePyramid}
import com.oculusinfo.binning.{TilePyramid, TilePyramidFactory}
import com.oculusinfo.factory.ConfigurableFactory
import com.oculusinfo.factory.properties.BooleanProperty


/**
 * A small wrapper class for a tile pyramid that allows for auto-bounds when appropriate
 *
 * This small bit of indirection allows us to defer the auto-bounds decision until such a
 * time as we are actually capable of calculating the auto-bounds.
 * @param base The base tile pyramid to be provided when auto-bounds is not appropriate
 * @param autoBounds Whether using auto-bounds is appropriate, when possible
 */
class DeferredTilePyramid (base: TilePyramid, autoBounds: Boolean) {
	def getTilePyramid (boundsFcn: () => (Double, Double, Double, Double)): TilePyramid = {
		if (autoBounds && base.isInstanceOf[AOITilePyramid]) {
			val (minX, maxX, minY, maxY) = boundsFcn()
			return new AOITilePyramid(minX, minY, maxX, maxY)
		} else {
			base
		}
	}
}

/**
 * A factory for deferred tile pyramids
 */
object DeferredTilePyramidFactory {
	var AUTOBOUNDS_PROPERTY = new BooleanProperty("autobounds",
	                                              "If true, calculate tile pyramid bounds automatically; if false, use values given by properties",
	                                              true)
}
class DeferredTilePyramidFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ConfigurableFactory[DeferredTilePyramid](classOf[DeferredTilePyramid], parent, path)
{
	import DeferredTilePyramidFactory._
	addProperty(AUTOBOUNDS_PROPERTY)
	addChildFactory(new TilePyramidFactory(this, Seq[String]().asJava))

	override protected def create: DeferredTilePyramid = {
		new DeferredTilePyramid(produce(classOf[TilePyramid]), getPropertyValue(AUTOBOUNDS_PROPERTY))
	}
}
