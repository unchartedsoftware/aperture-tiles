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



import java.lang.{Double => JavaDouble}
import java.util.{List => JavaList}
import java.util.ArrayList

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.convert.Wrappers.SeqWrapper
import scala.collection.mutable.Buffer
import scala.reflect.ClassTag

import org.apache.spark.SparkContext

import com.oculusinfo.binning.TileData
import com.oculusinfo.binning.TileIndex
import com.oculusinfo.tilegen.tiling.AnalysisDescription
import com.oculusinfo.tilegen.tiling.AnalysisDescriptionTileWrapper
import com.oculusinfo.tilegen.tiling.CompositeAnalysisDescription
import com.oculusinfo.tilegen.tiling.CustomGlobalMetadata
import com.oculusinfo.tilegen.tiling.IPv4Analytics
import com.oculusinfo.tilegen.tiling.MinimumDoubleTileAnalytic
import com.oculusinfo.tilegen.tiling.MaximumDoubleTileAnalytic
import com.oculusinfo.tilegen.tiling.MinimumDoubleArrayTileAnalytic
import com.oculusinfo.tilegen.tiling.MaximumDoubleArrayTileAnalytic
import com.oculusinfo.tilegen.util.PropertiesWrapper



object CSVDataAnalyticExtractor {
	def fromProperties[IT, PT] (properties: PropertiesWrapper,
	                            indexType: ClassTag[IT],
	                            processingType: ClassTag[PT]):
			AnalysisWithTag[(IT, PT), _] = {
		val analysis: Option[AnalysisDescription[(IT, PT), Int]] = None
		new AnalysisWithTag[(IT, PT), Int](analysis)
	}
}

object CSVTileAnalyticExtractor {
	def fromProperties[IT, PT, BT] (sc: SparkContext,
	                                properties: PropertiesWrapper,
	                                indexer: CSVIndexExtractor[IT],
	                                valuer: CSVValueExtractor[_, BT],
	                                levels: Seq[Seq[Int]]):
			AnalysisWithTag[TileData[BT], _] =
	{
		val metaDataKeys = (levels.flatMap(lvls => lvls).toSet.map((level: Int) =>
			                    (""+level -> ((index: TileIndex) => (index.getLevel == level)))
		                    ) + ("global" -> ((index: TileIndex) => true))
		).toMap

		val binType = ClassTag.unapply(valuer.valueTypeTag).get

		val analyses = Buffer[AnalysisDescription[TileData[BT], _]]()

		if (indexer.isInstanceOf[IPv4IndexExtractor]) {
			analyses += IPv4Analytics.getCIDRBlockAnalysis[BT](sc)
			analyses += IPv4Analytics.getMinIPAddressAnalysis[BT](sc)
			analyses += IPv4Analytics.getMaxIPAddressAnalysis[BT](sc)
		}
		if (binType == classOf[Double]) {
			val convertFcn: BT => Double = bt => bt.asInstanceOf[Double]
			val minAnalytic =
				new AnalysisDescriptionTileWrapper[BT, Double](sc,
				                                               convertFcn,
				                                               new MinimumDoubleTileAnalytic,
				                                               metaDataKeys)
			val maxAnalytic =
				new AnalysisDescriptionTileWrapper[BT, Double](sc,
				                                               convertFcn,
				                                               new MaximumDoubleTileAnalytic,
				                                               metaDataKeys)

			analyses += minAnalytic
			analyses += maxAnalytic
		} else if (valuer.isInstanceOf[SeriesValueExtractor]
			           || valuer.isInstanceOf[MultiFieldValueExtractor]) {
			val convertFcn: BT => Seq[Double] = { bt =>
				for (b <- bt.asInstanceOf[JavaList[JavaDouble]]) yield b.asInstanceOf[Double]
			}
			val minAnalytic =
				new AnalysisDescriptionTileWrapper[BT, Seq[Double]](sc,
				                                                    convertFcn,
				                                                    new MinimumDoubleArrayTileAnalytic,
				                                                    metaDataKeys)
			val maxAnalytic =
				new AnalysisDescriptionTileWrapper[BT, Seq[Double]](sc,
				                                                    convertFcn,
				                                                    new MaximumDoubleArrayTileAnalytic,
				                                                    metaDataKeys)
			analyses += minAnalytic
			analyses += maxAnalytic

			if (valuer.isInstanceOf[SeriesValueExtractor]) {
				val seriesValuer = valuer.asInstanceOf[SeriesValueExtractor]
				analyses += new CustomGlobalMetadata(
					Map("variables" -> seriesValuer.fields.toSeq.asJava))
			}
		}

		if (analyses.isEmpty) {
			new AnalysisWithTag[TileData[BT], Int](None)
		} else {
			new AnalysisWithTag(Some(analyses.reduce((a, b) =>
				                         new CompositeAnalysisDescription(a, b))))
		}
	}
}

class AnalysisWithTag[BT, AT: ClassTag] (val analysis: Option[AnalysisDescription[BT, AT]]) {
	val analysisTypeTag: ClassTag[AT] = implicitly[ClassTag[AT]]
}
