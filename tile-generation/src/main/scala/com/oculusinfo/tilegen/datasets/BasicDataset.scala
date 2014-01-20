package com.oculusinfo.tilegen.datasets

import java.lang.{Double => JavaDouble}
import java.text.SimpleDateFormat
import java.util.Properties
import scala.collection.JavaConverters._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import com.oculusinfo.binning.TilePyramid
import com.oculusinfo.binning.impl.AOITilePyramid
import com.oculusinfo.binning.impl.WebMercatorTilePyramid
import com.oculusinfo.tilegen.tiling.BinDescriptor
import com.oculusinfo.tilegen.tiling.DataSource
import com.oculusinfo.tilegen.tiling.FieldExtractor
import com.oculusinfo.tilegen.tiling.RecordParser
import com.oculusinfo.tilegen.tiling.StandardDoubleBinDescriptor
import com.oculusinfo.tilegen.tiling.ValueOrException
import com.oculusinfo.tilegen.util.PropertiesWrapper
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Time



abstract class BasicDatasetBase[BT: ClassManifest, PT] (
    rawProperties: Properties,
	tileSize: Int,
	binDescriptor: BinDescriptor[BT, PT])
extends Dataset[BT, PT] {
  def manifest = implicitly[ClassManifest[Double]]

  private val properties = new PropertiesWrapper(rawProperties)

  private val description = properties.getOptionProperty("oculus.binning.description")
  private val xVar = properties.getOptionProperty("oculus.binning.xField").get
  private val yVar = properties.getProperty("oculus.binning.yField", "zero")
  private val zVar = properties.getProperty("oculus.binning.valueField", "count")
  private val levels = properties.getSeqProperty("oculus.binning.levels").map(lvlString => {
    lvlString.split(',').map(levelRange => {
      val extrema = levelRange.split('-')

      if (0 == extrema.size) Seq[Int]()
      if (1 == extrema.size) Seq[Int](extrema(0).toInt)
      else Range(extrema(0).toInt, extrema(1).toInt+1).toSeq
    }).reduce(_ ++ _)
  })
  private val consolidationPartitions =
    properties.getIntOptionProperty("oculus.binning.consolidationPartitions")


  //////////////////////////////////////////////////////////////////////////////
  // Section: Dataset implementation
  //
  def getName = {
    val name = properties.getProperty("oculus.binning.name", "unknown")
    val prefix = properties.getOptionProperty("oculus.binning.prefix")
    val pyramidName = if (prefix.isDefined) prefix.get+"."+name
		      else name

    pyramidName+"."+xVar+"."+yVar+(if ("count".equals(zVar)) "" else "."+zVar)
  }

  def getDescription =
    description.getOrElse(
      "Binned "+getName+" data showing "+xVar+" vs. "+yVar)

  def getLevels = levels

  def getTilePyramid = tilePyramid

  override def getBins = tileSize
  
  val tilePyramid: TilePyramid = {
    val projection = properties.getProperty("oculus.binning.projection", "EPSG:4326")
    if ("EPSG:900913" == projection) {
      new WebMercatorTilePyramid()
    } else {
      val minX = properties.getDoubleOptionProperty("oculus.binning.projection.minx").get
      val maxX = properties.getDoubleOptionProperty("oculus.binning.projection.maxx").get
      val minY = properties.getDoubleOptionProperty("oculus.binning.projection.miny").get
      val maxY = properties.getDoubleOptionProperty("oculus.binning.projection.maxy").get
      new AOITilePyramid(minX, minY, maxX, maxY)
    }
  }

  def getBinDescriptor: BinDescriptor[BT, PT] = binDescriptor
}


class BasicDataset[BT: ClassManifest, PT] (
    rawProperties: Properties,
	tileSize: Int,
	binDescriptor: BinDescriptor[BT, PT])
extends BasicDatasetBase[BT, PT](rawProperties, tileSize, binDescriptor) {
 
  type STRAT_TYPE = ProcessingStrategy[BT]
  protected var strategy: STRAT_TYPE = null
  
}

  
class BasicStreamingDataset[BT: ClassManifest, PT] (
    rawProperties: Properties,
	tileSize: Int,
	binDescriptor: BinDescriptor[BT, PT])
extends BasicDatasetBase[BT, PT](rawProperties, tileSize, binDescriptor) with StreamingProcessor[BT] {
 
  type STRAT_TYPE = StreamingProcessingStrategy[BT]
  protected var strategy: STRAT_TYPE = null
  
  def processWithTime[OUTPUT] (fcn: Time => RDD[(Double, Double, BT)] => OUTPUT,
		       completionCallback: Option[Time => OUTPUT => Unit]): Unit = {
    if (null == strategy) {
      throw new Exception("Attempt to process uninitialized dataset "+getName)
    } else {
      strategy.processWithTime(fcn, completionCallback)
    }
  }

}
