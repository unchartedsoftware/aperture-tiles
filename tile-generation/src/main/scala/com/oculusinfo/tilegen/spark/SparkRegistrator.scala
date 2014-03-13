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
 
package com.oculusinfo.tilegen.spark

import com.esotericsoftware.kryo.Kryo
import org.apache.spark.serializer.KryoRegistrator
import com.oculusinfo.tilegen.tiling._
import com.oculusinfo.tilegen.util._
import com.oculusinfo.tilegen.datasets._
//import com.oculusinfo.tilegen.binning._
//import com.oculusinfo.tilegen.live._
import com.oculusinfo.binning._
import com.oculusinfo.binning.impl._
import com.oculusinfo.binning.io.serialization._
import com.oculusinfo.binning.io.serialization.impl._
import com.oculusinfo.binning.util._


class SparkRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {

    
    kryo.setRegistrationRequired(true);

    println("User Registration Begin")
    kryo.register(classOf[scala.runtime.BoxedUnit])  

    // com.oculusinfo.tilegen.spark
    kryo.register(classOf[MinMaxAccumulableParam])   
    kryo.register(classOf[DoubleMinAccumulatorParam])
    kryo.register(classOf[DoubleMaxAccumulatorParam])
    println("com.oculusinfo.tilegen.spark")

    // com.oculusinfo.tilegen.tiling
    //kryo.register(classOf[BinDescriptor[_,_]])
    kryo.register(classOf[StandardDoubleBinDescriptor])
    kryo.register(classOf[CompatibilityDoubleBinDescriptor])
    kryo.register(classOf[MinimumDoubleBinDescriptor])
    kryo.register(classOf[MaximumDoubleBinDescriptor])
    kryo.register(classOf[LogDoubleBinDescriptor])
    kryo.register(classOf[StandardDoubleArrayBinDescriptor])
    kryo.register(classOf[StringScoreBinDescriptor])   
    //kryo.register(classOf[RecordParser[_]])
    //kryo.register(classOf[FieldExtractor[_]])
    kryo.register(classOf[TileIO])
    kryo.register(classOf[LevelMinMaxAccumulableParam[_]])
    //kryo.register(classOf[GenericSeriesBinner[_]])
    //kryo.register(classOf[TileToImageConverter[_]])
    kryo.register(classOf[SingleTileToImageConverter])
    kryo.register(classOf[TileSeriesToImagesConverter])
    kryo.register(classOf[ValueOrException[_]]) 
    println("com.oculusinfo.tilegen.tiling")

    // com.oculusinfo.tilegen.util
    kryo.register(classOf[PropertiesWrapper])
    kryo.register(classOf[Rectangle[_]])
    println("com.oculusinfo.tilegen.util")

  
    kryo.register(classOf[CSVRecordPropertiesWrapper])
    kryo.register(classOf[CSVRecordParser])
    println("com.oculusinfo.tilegen.datasets")

    // com.oculusinfo.binning
    kryo.register(classOf[BinIndex])
    kryo.register(classOf[TileAndBinIndices])
    kryo.register(classOf[PyramidComparator])
    kryo.register(classOf[TileData[_]])
    kryo.register(classOf[java.util.ArrayList[_]])
    kryo.register(classOf[TileIndex])
    kryo.register(classOf[DensityStripData[_]])
    println("com.oculusinfo.binning")
    
    // com.oculusinfo.binning.impl
    kryo.register(classOf[AOITilePyramid])
    kryo.register(classOf[WebMercatorTilePyramid])
    println("com.oculusinfo.binning.impl")

    // com.oculusinfo.binning.util
    kryo.register(classOf[Pair[_,_]])
    println("com.oculusinfo.binning.util")

    // com.oculusinfo.binning.io.serialization
    /*
    kryo.register(classOf[TileSerializer[_]])
    kryo.register(classOf[GenericAvroArraySerializer[_]])
    kryo.register(classOf[GenericJSONSerializer[_]])
    kryo.register(classOf[GenericAvroSerializer[_]])
    */
    println("com.oculusinfo.io.serialization")

    // com.oculusinfo.binning.io.serialization.impl
    /*
    kryo.register(classOf[BackwardCompatibilitySerializer])
    kryo.register(classOf[IntegerAvroSerializer])
    kryo.register(classOf[DoubleArrayAvroSerializer])
    kryo.register(classOf[DoubleAvroSerializer])
    kryo.register(classOf[StringArrayAvroSerializer])
    kryo.register(classOf[StringIntPairArrayJSONSerializer])
    kryo.register(classOf[StringDoublePairArrayAvroSerializer])
    kryo.register(classOf[StringIntPairArrayAvroSerializer])
    */
    println("com.oculusinfo.io.serialization.impl")

    println("User Registration Complete")

  }
}