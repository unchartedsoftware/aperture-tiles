/*
 * Copyright (c) 2015 Uncharted Software Inc.
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
package software.uncharted.tile.source.server.io

import java.util

import com.oculusinfo.binning.io.PyramidIO
import com.oculusinfo.factory.ConfigurableFactory
import com.oculusinfo.factory.providers.AbstractFactoryProvider
import com.uncharted.tile.source.server.app.SparkContextProvider
import software.uncharted.tile.source.server.app.SparkContextProvider

/**
 * Simple FactoryProvider for an OnDemandTilePyramidIOFactory
 */
class OnDemandTilePyramidIOFactoryProvider(contextProvider: SparkContextProvider) extends AbstractFactoryProvider[PyramidIO] {
  override def createFactory(name: String, parent: ConfigurableFactory[_], path: util.List[String]): ConfigurableFactory[_ <: PyramidIO] = {
    new OnDemandTilePyramidIOFactory(parent, path, contextProvider)
  }
}
