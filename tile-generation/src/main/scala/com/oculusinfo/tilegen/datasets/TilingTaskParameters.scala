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

import com.oculusinfo.factory.{ConfigurationProperty, ConfigurableFactory}
import com.oculusinfo.factory.properties.{IntegerProperty, BooleanProperty, StringProperty}
import com.oculusinfo.tilegen.util.OptionsFactoryMixin


/**
 * A consolidated location for the random parameters associated with tiling tasks, so as to make them amenable to
 * factory construction
 *
 * @param name The basic name of the tile pyramid to be tiled
 * @param description A description of the tile pyramid to be tiled
 * @param prefix A prefix to be prepended to the basic name, to differentiate different runs of a tiling task
 * @param consolidationPartitions The number of partitions into which to consolidate data when performign reduce operations
 */
case class TilingTaskParameters (name: String,
                                 description: String,
                                 prefix: Option[String],
                                 consolidationPartitions: Option[Int])
{
}


object TilingTaskParametersFactory {
	var NAME_PROPERTY = new StringProperty("name", "The basic root name of the tile pyramid to be tiled", "")
	var DESC_PROPERTY = new StringProperty("description", "A description of the tile pyramid to be tiled.  This will be put in the pyramid metaData.", "")
	var PREFIX_PROPERTY = new StringProperty("prefix", "A prefix to be prepended to the basic name, so as to differentiate different attempts to tile the same data.", "")
	var PARTITIONS_PROPERTY = new IntegerProperty("consolidationPartitions", "The number of partitions into which to consolidate data when performign reduce operations", 0)
}
class TilingTaskParametersFactory (parent: ConfigurableFactory[_], path: JavaList[String])
		extends ConfigurableFactory[TilingTaskParameters](classOf[TilingTaskParameters], parent, path, true)
		with OptionsFactoryMixin[TilingTaskParameters]
{
	import TilingTaskParametersFactory._

	override protected def create(): TilingTaskParameters = {
		new TilingTaskParameters(getPropertyValue(NAME_PROPERTY),
		                         getPropertyValue(DESC_PROPERTY),
		                         optionalGet(PREFIX_PROPERTY),
		                         optionalGet(PARTITIONS_PROPERTY).map(_.intValue()))
	}
}
