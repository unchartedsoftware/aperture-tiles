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
 
package com.oculusinfo.tilegen.tiling



import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO



import scala.collection.mutable.{Map => MutableMap}



import org.apache.spark._
import org.apache.spark.SparkContext._



import com.oculusinfo.binning.BinIndex
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.binning.io.serialization.TileSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.DoubleArrayAvroSerializer
import com.oculusinfo.binning.io.serialization.impl.BackwardCompatibilitySerializer

import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.MissingArgumentException



/**
 * Print out the nth values in a single tile in a time series tile set
 */
object TileToTextConverter {
  def main (args: Array[String]): Unit = {
    val argParser = new ArgumentParser(args)

    try {
      // We need a TileIO ...
      val tileIO = TileIO.fromArguments(argParser)

      // ... a source location ...
      val source = argParser.getStringArgument("s", "source location")
      //   (and while we're here, get the tile meta data)
      val metaData = tileIO.readMetaData(source).get

      // ... a tile pyramid ...
      val tilePyramid: TilePyramid = 
        argParser.getStringArgument("p",
                                    "pyramid type\n"
                                    +"EPSG:900913 (mercator)\n"
                                    +"EPSG:4326 (aoi) DEFAULT",
                                    Some("EPSG:4326")) match {
          case "EPSG:900913" => {
            new WebMercatorTilePyramid()
          }
          case "EPSG:4326" => {
            val bounds = metaData.bounds
            new AOITilePyramid(bounds.getMinX(), bounds.getMinY(),
                               bounds.getMaxX(), bounds.getMaxY())
          }
          case _ => {
            throw new MissingArgumentException("Missing or invalid tile pyramid type")
          }
        }

      // ... spark connection details ...
      val connector = argParser.getSparkConnector()

      val level = argParser.getIntArgument("level", "The level of tile to display")
      val x = argParser.getIntArgument("x", "The x index of the tile to display")
      val y = argParser.getIntArgument("y", "The y index of the tile to display")
      val t = argParser.getIntArgument("t", "The number in the series of values to display")

      val sc = connector.getSparkContext("Convert tile pyramid to images")
      val serializer = new DoubleArrayAvroSerializer()
      val tiles = tileIO.readTileSet(sc, serializer, source, List(level))
      val tile = tiles.filter(tile => {
        val index = tile.getDefinition()
        index.getLevel == level && index.getX() == x && index.getY() == y
      }).collect()(0)
      val index = tile.getDefinition()
      Range(0, index.getXBins()).map(x => {
        Range(0, index.getYBins()).map(y => {
          val binValue = tile.getBin(x, y)
          val value = if (binValue.size>t) binValue.get(t) else 0.0
          println("%d, %d: %.4f".format(x, y, value))
        })
      })
    } catch {
      case e: MissingArgumentException => {
        println("TileToTextConverter - display a single element from a series tile")
        println("Argument exception: "+e.getMessage())
        argParser.usage
      }
    }
  }
}



/**
 * Class to convert a tile set to images
 */
object TileToImageConverter {
  def main (args: Array[String]): Unit = {
    val argParser = new ArgumentParser(args)

    try {
      // We need a TileIO ...
      val tileIO = TileIO.fromArguments(argParser)

      // ... a source location ...
      val source = argParser.getStringArgument("s", "source location")

      //   (and while we're here, get the tile meta data)
      val metaData = tileIO.readMetaData(source).get

      // ... a destination location ...
      val destination = argParser.getStringArgument("d", "destination location")
    
      // ... a tile pyramid ...
      val tilePyramid: TilePyramid = 
        argParser.getStringArgument("p",
                                    "pyramid type\n"
                                    +"EPSG:900913 (mercator)\n"
                                    +"EPSG:4326 (aoi) DEFAULT",
                                    Some("EPSG:4326")) match {
          case "EPSG:900913" => {
            new WebMercatorTilePyramid()
          }
          case "EPSG:4326" => {
            val bounds = metaData.bounds
            new AOITilePyramid(bounds.getMinX(), bounds.getMinY(),
                               bounds.getMaxX(), bounds.getMaxY())
          }
          case _ => {
            throw new MissingArgumentException("Missing or invalid tile pyramid type")
          }
        }

      // ... spark connection details ...
      val connector = argParser.getSparkConnector()

      // ... a scale to apply to bin values ...
      val scale = argParser.getStringArgument(
        "scale",
        "Scale to apply to bin values.  Can be:\n"
        +"\tlinear - to leave them unchanged (DEFAULT)\n"
        +"\tlog - to take the base-10 logarithm of the values",
        Some("linear")) match {
        case "log" => (min: Double, x: JavaDouble, max: Double) => {
            if (x.isNaN) 0
            else ((math.log10(1.0+x.doubleValue)/math.log10(1.0+max))*255.999).toInt
        }
        case _ => (min: Double, x: JavaDouble, max: Double) => {
          if (x.isNaN) 0
          else (((x.doubleValue-min)/(max-min))*255.999).toInt
        }
      }

      // ... and a list of levels
      val levels = argParser.getIntSeqArgument("levels",
                                               "comma-spearated list of levels, with no spaces",
                                               ',')


      val sc = connector.getSparkContext("Convert tile pyramid to images")

      // And, finally, the serializer determines the converter type
      argParser.getStringArgument("ser",
                                  "serializer type\n"
                                  +"\tcompatibility - for an old style "
                                  +"backwards-compatibility serializer\n"
                                  +"\tdouble - for an up-to-date avro-based "
                                  +"serializer of double tiles (DEFAULT)\n"
                                  +"\tdoublearray - for an up-to-date avro-based "
                                  +"serializer of double vector tiles.",
                                  Some("double")) match {
        case "compatibility" =>
          convertDoubleTiles(sc, tileIO, tilePyramid, new BackwardCompatibilitySerializer(),
                             source, destination, scale, levels, metaData)

        case "double" =>
          convertDoubleTiles(sc, tileIO, tilePyramid, new DoubleAvroSerializer(),
                             source, destination, scale, levels, metaData)

        case "doublearray" =>
          convertVectorTiles(sc, tileIO, tilePyramid, new DoubleArrayAvroSerializer(),
                             source, destination, scale, levels, metaData)
      }
    } catch {
      case e: MissingArgumentException => {
        println("TileToImageConverter: Argument error")
        println(e.getMessage())
        argParser.usage
      }
    }
  }

  def convertDoubleTiles (sc: SparkContext,
                          tileIO: TileIO,
                          tilePyramid: TilePyramid,
                          serializer: TileSerializer[JavaDouble],
                          source: String,
                          destination: String,
                          scale: (Double, JavaDouble, Double) => Int,
                          levels: Seq[Int],
                          metaData: TileMetaData): Unit = {
    val levelMins = metaData.levelMins.map(p => (p._1 -> p._2.toDouble)).toMap
    val levelMaxes = metaData.levelMaxes.map(p => (p._1 -> p._2.toDouble)).toMap
    def colorFcn (tile: TileIndex, bin: BinIndex, image: Int, value: Option[JavaDouble]): Int = {
      val level = tile.getLevel()
      val min = levelMins(level)
      val max = levelMaxes(level)
      val result = scale(min, value.getOrElse(new JavaDouble(Double.NaN)), max)
      new java.awt.Color(result, result, result).getRGB()
    }

    new SingleTileToImageConverter(tileIO, tilePyramid, colorFcn(_, _, _, _), serializer)
      .convertTiles(sc, source, destination, levels)
  }

  def convertVectorTiles (sc: SparkContext,
                          tileIO: TileIO,
                          tilePyramid: TilePyramid,
                          serializer: TileSerializer[JavaList[JavaDouble]],
                          source: String,
                          destination: String,
                          scale: (Double, JavaDouble, Double) => Int,
                          levels: Seq[Int],
                          metaData: TileMetaData): Unit = {
    val levelMins = metaData.levelMins.map(p =>
      (p._1 -> p._2.drop("list(".size).dropRight(")".size).split(",").map(_.trim.toDouble))
    ).toMap
    val levelMaxes = metaData.levelMaxes.map(p =>
      (p._1 -> p._2.drop("list(".size).dropRight(")".size).split(",").map(_.trim.toDouble))
    ).toMap

    def colorFcn (tile: TileIndex, bin: BinIndex, image: Int, value: Option[JavaDouble]): Int = {
      val level = tile.getLevel()
      val min = levelMins(level)(image)
      val max = levelMaxes(level)(image)
      val result = scale(min, value.getOrElse(new JavaDouble(Double.NaN)), max)
      val problem = "%.4f, %.4f, %.4f: %d".format(min, value.getOrElse(min), max, result)
      try {
        new java.awt.Color(result, result, result).getRGB()
      } catch {
        case e: Exception => throw new Exception(problem, e)
      }
    }

    new TileSeriesToImagesConverter(tileIO, tilePyramid, colorFcn(_, _, _, _), serializer)
      .convertTiles(sc, source, destination, levels)
  }
}



abstract class TileToImageConverter[T] (io: TileIO,
                                        pyramid: TilePyramid,
                                        binColorFcn: (TileIndex, BinIndex, Int, Option[JavaDouble]) => Int,
                                        serializer: TileSerializer[T]) 
extends Serializable {
  def convertTile (tile: TileData[T]): Seq[(Option[String], BufferedImage)]


  def convertTiles (sc: SparkContext,
                    baseLocation: String,
                    destinationLocation: String,
                    levels: Seq[Int]): Unit = {

    val tiles = io.readTileSet(sc, serializer, baseLocation, levels)
    val numTiles = tiles.count()

    // Split them up into sets of ~100 tiles
    val partitions = (numTiles/100).toInt+1
    val partitionedTiles = tiles.coalesce(partitions)
    val setMarkedTiles = partitionedTiles.mapPartitionsWithIndex((partitionIndex, iterator) =>
      iterator.map(elt => (partitionIndex, elt)))

    setMarkedTiles.cache()

    Range(0, partitions).foreach(n => {
      val tileSubset = setMarkedTiles.filter(_._1 == n).map(_._2).collect()
      tileSubset.foreach(tile => {
        val index = tile.getDefinition()
        val images = convertTile(tile)
        images.foreach(idImage => {
          val id = idImage._1
          val image = idImage._2
          ImageIO.write(image, "png",
                        new File("%s/tiles/%d/%d/%d%s.png".format(destinationLocation, 
                                                                  index.getLevel(),
                                                                  index.getX(),
                                                                  index.getY(),
                                                                  if (id.isDefined) "-"+id.get else "")))
        })
      })
    })
  }
}


class SingleTileToImageConverter (io: TileIO,
                                  pyramid: TilePyramid,
                                  binColorFcn: (TileIndex, BinIndex, Int, Option[JavaDouble]) => Int,
                                  serializer: TileSerializer[JavaDouble])
extends TileToImageConverter[JavaDouble](io, pyramid, binColorFcn, serializer) {
  def convertTile (tile: TileData[JavaDouble]): Seq[(Option[String], BufferedImage)] = {
    val index = tile.getDefinition()
    val width = index.getXBins()
    val height = index.getYBins()

    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    Range(0, width).foreach(x =>
      Range(0, height).foreach(y => {
        val value = tile.getBin(x, y)
        image.setRGB(x, y, binColorFcn(index, new BinIndex(x, y), 0, Some(value)))
      })
    )

    List((None, image))
  }

}

class TileSeriesToImagesConverter (io: TileIO,
                                   pyramid: TilePyramid,
                                   binColorFcn: (TileIndex, BinIndex, Int, Option[JavaDouble]) => Int,
                                   serializer: TileSerializer[JavaList[JavaDouble]]) 
extends TileToImageConverter[JavaList[JavaDouble]](io, pyramid, binColorFcn, serializer) {
  def convertTile (tile: TileData[JavaList[JavaDouble]]): Seq[(Option[String], BufferedImage)] = {
    val index = tile.getDefinition()
    val width = index.getXBins()
    val height = index.getYBins()
    println("\n\n\nRead in tile with "+width+" x bins and "+height+" y bins")

    // Find out how many images we need
    val timeBins = Range(0, width).map(x =>
      Range(0, height).map(y =>
        tile.getBin(x,y).size()
      ).reduce(_ max _)
    ).reduce(_ max _)

    println("Found "+timeBins+" time bins\n\n\n")

    val images = Range(0, timeBins).map(n =>
      (Some(n.toString), new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB))
     )

    Range(0, width).foreach(x =>
      Range(0, height).foreach(y => {
        val value = tile.getBin(x, y)
        Range(0, timeBins).foreach(n =>
          images(n)._2.setRGB(x, y, binColorFcn(index, new BinIndex(x, y), n,
                                                if (value.size>n) Some(value.get(n).doubleValue)
                                                else None))
        )
      })
    )

    images
  }
}


                                  
