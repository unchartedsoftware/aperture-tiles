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
 
package com.oculusinfo.tilegen.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.oculusinfo.binning.*;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.tilegen.spark.DoubleMaxAccumulatorParam;
import com.oculusinfo.tilegen.spark.DoubleMinAccumulatorParam;
import com.oculusinfo.tilegen.spark.MinMaxAccumulableParam;
import com.oculusinfo.tilegen.tiling.HBaseTileIO;
import com.oculusinfo.tilegen.tiling.TileIO;
import com.oculusinfo.tilegen.util.PropertiesWrapper;
import com.oculusinfo.tilegen.util.Rectangle;
import org.apache.spark.serializer.KryoRegistrator;


public class TileRegistrator implements KryoRegistrator {

	public void registerClasses(Kryo kryo) {

		// throw exception if class is being serialized, but has not been registered
		kryo.setRegistrationRequired(true);

		kryo.register(scala.runtime.BoxedUnit.class);
		kryo.register(scala.None.class);
		kryo.register(scala.None$.class);
		kryo.register(scala.Some.class);
		kryo.register(scala.Some$.class);

		kryo.register(char[].class);
		kryo.register(char[][].class);
		kryo.register(char[][][].class);
		kryo.register(char[][][][].class);
		kryo.register(byte[].class);
		kryo.register(byte[][].class);
		kryo.register(byte[][][].class);
		kryo.register(byte[][][][].class);
		kryo.register(short[].class);
		kryo.register(short[][].class);
		kryo.register(short[][][].class);
		kryo.register(short[][][][].class);
		kryo.register(int[].class);
		kryo.register(int[][].class);
		kryo.register(int[][][].class);
		kryo.register(int[][][][].class);
		kryo.register(long[].class);
		kryo.register(long[][].class);
		kryo.register(long[][][].class);
		kryo.register(long[][][][].class);
		kryo.register(float[].class);
		kryo.register(float[][].class);
		kryo.register(float[][][].class);
		kryo.register(float[][][][].class);
		kryo.register(double[].class);
		kryo.register(double[][].class);
		kryo.register(double[][][].class);
		kryo.register(double[][][][].class);
		kryo.register(boolean[].class);
		kryo.register(boolean[][].class);
		kryo.register(boolean[][][].class);
		kryo.register(boolean[][][][].class);

		kryo.register(java.util.HashMap.class);

		kryo.register(scala.collection.immutable.Range.class);

		// com.oculusinfo.tilegen.spark
		kryo.register(MinMaxAccumulableParam.class);
		kryo.register(DoubleMinAccumulatorParam.class);
		kryo.register(DoubleMaxAccumulatorParam.class);

		// com.oculusinfo.tilegen.tiling
		kryo.register(TileIO.class);
		kryo.register(HBaseTileIO.class);

		// com.oculusinfo.tilegen.util
		kryo.register(PropertiesWrapper.class);
		kryo.register(Rectangle.class);

		// com.oculusinfo.binning
		kryo.register(BinIndex.class);
		kryo.register(TileAndBinIndices.class);
		kryo.register(PyramidComparator.class);
		kryo.register(TileData.class);
		kryo.register(TileData[].class);
		kryo.register(java.util.ArrayList.class);
		kryo.register(TileIndex.class);

		// com.oculusinfo.binning.impl
		kryo.register(AOITilePyramid.class);
		kryo.register(WebMercatorTilePyramid.class);

		// com.oculusinfo.binning.util
		kryo.register(Pair.class);
	}
}
