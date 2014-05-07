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

import org.apache.spark.serializer.KryoRegistrator;

import com.esotericsoftware.kryo.Kryo;
import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.DensityStripData;
import com.oculusinfo.binning.PyramidComparator;
import com.oculusinfo.binning.TileAndBinIndices;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.tilegen.datasets.CSVRecordParser;
import com.oculusinfo.tilegen.datasets.CSVRecordPropertiesWrapper;
import com.oculusinfo.tilegen.spark.DoubleMaxAccumulatorParam;
import com.oculusinfo.tilegen.spark.DoubleMinAccumulatorParam;
import com.oculusinfo.tilegen.spark.MinMaxAccumulableParam;
import com.oculusinfo.tilegen.tiling.BinDescriptor;
import com.oculusinfo.tilegen.tiling.CompatibilityDoubleBinDescriptor;
import com.oculusinfo.tilegen.tiling.FieldExtractor;
import com.oculusinfo.tilegen.tiling.GenericSeriesBinner;
import com.oculusinfo.tilegen.tiling.HBaseTileIO;
import com.oculusinfo.tilegen.tiling.LevelMinMaxAccumulableParam;
import com.oculusinfo.tilegen.tiling.LogDoubleBinDescriptor;
import com.oculusinfo.tilegen.tiling.MaximumDoubleBinDescriptor;
import com.oculusinfo.tilegen.tiling.MinimumDoubleBinDescriptor;
import com.oculusinfo.tilegen.tiling.RecordParser;
import com.oculusinfo.tilegen.tiling.SingleTileToImageConverter;
import com.oculusinfo.tilegen.tiling.StandardDoubleArrayBinDescriptor;
import com.oculusinfo.tilegen.tiling.StandardDoubleBinDescriptor;
import com.oculusinfo.tilegen.tiling.StringScoreBinDescriptor;
import com.oculusinfo.tilegen.tiling.TileIO;
import com.oculusinfo.tilegen.tiling.TileSeriesToImagesConverter;
import com.oculusinfo.tilegen.tiling.TileToImageConverter;
import com.oculusinfo.tilegen.util.PropertiesWrapper;
import com.oculusinfo.tilegen.util.Rectangle;


public class TileRegistrator implements KryoRegistrator {

	//static boolean needToRegister = true;

	public void registerClasses(Kryo kryo) {

		//if (needToRegister) {

		// throw exception if class is being serialized, but has not been registered
		kryo.setRegistrationRequired(true);

		kryo.register(scala.runtime.BoxedUnit.class);

		// com.oculusinfo.tilegen.spark
		kryo.register(MinMaxAccumulableParam.class);
		kryo.register(DoubleMinAccumulatorParam.class);
		kryo.register(DoubleMaxAccumulatorParam.class);

		// com.oculusinfo.tilegen.tiling
		kryo.register(BinDescriptor.class);
		kryo.register(StandardDoubleBinDescriptor.class);
		kryo.register(CompatibilityDoubleBinDescriptor.class);
		kryo.register(MinimumDoubleBinDescriptor.class);
		kryo.register(MaximumDoubleBinDescriptor.class);
		kryo.register(LogDoubleBinDescriptor.class);
		kryo.register(StandardDoubleArrayBinDescriptor.class);
		kryo.register(StringScoreBinDescriptor.class);
		kryo.register(RecordParser.class);
		kryo.register(FieldExtractor.class);
		kryo.register(TileIO.class);
		kryo.register(HBaseTileIO.class);
		kryo.register(LevelMinMaxAccumulableParam.class);
		kryo.register(GenericSeriesBinner.class);
		kryo.register(TileToImageConverter.class);
		kryo.register(SingleTileToImageConverter.class);
		kryo.register(TileSeriesToImagesConverter.class);

		// com.oculusinfo.tilegen.util
		kryo.register(PropertiesWrapper.class);
		kryo.register(Rectangle.class);
		kryo.register(CSVRecordPropertiesWrapper.class);
		kryo.register(CSVRecordParser.class);

		// com.oculusinfo.binning
		kryo.register(BinIndex.class);
		kryo.register(TileAndBinIndices.class);
		kryo.register(PyramidComparator.class);
		kryo.register(TileData.class);
		kryo.register(TileData[].class);
		kryo.register(java.util.ArrayList.class);
		kryo.register(TileIndex.class);
		kryo.register(DensityStripData.class);
        
		// com.oculusinfo.binning.impl
		kryo.register(AOITilePyramid.class);
		kryo.register(WebMercatorTilePyramid.class);

		// com.oculusinfo.binning.util
		kryo.register(Pair.class);

		//needToRegister = false;
		//}
	}
}
