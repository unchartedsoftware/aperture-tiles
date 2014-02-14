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

package com.oculusinfo.tilegen.examples.datagen



import com.oculusinfo.tilegen.tiling.DataSource
import com.oculusinfo.tilegen.tiling.FieldExtractor
import com.oculusinfo.tilegen.tiling.GenericSeriesBinner
import com.oculusinfo.tilegen.tiling.LocalTileIO
import com.oculusinfo.tilegen.tiling.RecordParser
import com.oculusinfo.tilegen.tiling.ValueOrException
import com.oculusinfo.tilegen.util.ArgumentParser
import com.oculusinfo.tilegen.util.MissingArgumentException




// Tile test data
// This is just for binning the artificially created time-series test data, below.
object TestSeriesTiling {
  def main (args: Array[String]): Unit = {
    val source = new TestSeriesDataSource(args(0))
    val parser = new TestSeriesRecordParser
    val extractor = new TestSeriesFieldExtractor

    val argParser = new ArgumentParser(args)
    val sc = argParser.getSparkConnector()
      .getLocalSparkContext("test time series tiling")
    val binner = new GenericSeriesBinner(source, parser, extractor)
    binner.doBinning(sc, new LocalTileIO("avro"),
                     "TestSeries", "x", "y", "z", "w", 4,
                     List(List(0, 1)), Some(1),
                     "Sample time series data",
                     "Sample time series data of an expanding square")
  }
}



// Creates a data file with squares expanding over time out from the center.
object CreateSeriesBinData {
  def main (args: Array[String]): Unit = {
    val argParser = new ArgumentParser(args)
    try {
      val fileName = argParser.getStringArgument("file",
                                                 "The name of the file in which "
                                                 +"to write artificial time "
                                                 +"series data")
      val writer = new java.io.FileWriter(fileName)

      val size = argParser.getIntArgument("size",
                                          "The x and y size of the tiles to be "
                                          +"created")
      val halfSize = size/2
      val lineWidth = argParser.getIntArgument("linewidth",
                                               "The width of the expanding "
                                               +"line to draw on the sample "
                                               +"tiles", Some(2))
      val halfWidthD = lineWidth/2.0
      val halfWidth = lineWidth/2

      val sizeD = size.toDouble

      Range(0, halfSize).map(t => {
        Range(-halfWidth, lineWidth-halfWidth).map(dt => {
          val min = halfSize-t-dt-1
          val max = halfSize+t+dt

          if (0 <= min && min < halfSize &&
              halfSize <= max && max < size) {

            Range(min, max).map(i => {
              val minD = min.toDouble
              val maxD = max.toDouble
              val iD = i.toDouble
              val tD = t.toDouble
              val dtD = dt.toDouble
              writer.write("%.4f\t%.4f\t%.4f\t%.4f\n".format(iD,
                                                             minD,
                                                             tD,
                                                             dtD+halfWidthD+1.0)) 
              writer.write("%.4f\t%.4f\t%.4f\t%.4f\n".format(maxD,
                                                             iD,
                                                             tD,
                                                             dtD+halfWidthD+1.0))
              writer.write("%.4f\t%.4f\t%.4f\t%.4f\n".format(sizeD-1-iD,
                                                             maxD,
                                                             tD,
                                                             dtD+halfWidthD+1.0))
              writer.write("%.4f\t%.4f\t%.4f\t%.4f\n".format(minD,
                                                             sizeD-1-iD,
                                                             tD,
                                                             dtD+halfWidthD+1.0))
            })
          }
        })
      })

      writer.close
    } catch {
      case e: MissingArgumentException => {
        println("CreateSeriesBinData: Argument Exception")
        println(e.getMessage())
        argParser.usage
      }
    }
  }
}



class TestSeriesRecord (val x: Double, val y: Double, val z: Double, val w: Double) extends Serializable {
}



class TestSeriesDataSource (fileName: String) extends DataSource with Serializable {
  def getDataFiles: Seq[String] = List(fileName)
}



class TestSeriesRecordParser extends RecordParser[TestSeriesRecord] {
  def parseRecords (raw: Iterator[String],
                    Variables: String*): Iterator[ValueOrException[TestSeriesRecord]] =
    raw.map(line => {
      val fields = line.split('\t')
      new ValueOrException(Some(new TestSeriesRecord(fields(0).toDouble,
                                                     fields(1).toDouble,
                                                     fields(2).toDouble,
                                                     fields(3).toDouble)),
                           None)
    })
}



class TestSeriesFieldExtractor extends FieldExtractor[TestSeriesRecord] {
  val fields = List("x", "y", "z", "w")
  def getValidFieldList = fields
  def isValidField (field: String) = fields.contains(field)
  def isConstantField (field: String) = false
  def getFieldValue (field: String)(record: TestSeriesRecord) =
    field match {
      case "x" => new ValueOrException(Some(record.x), None)
      case "y" => new ValueOrException(Some(record.y), None)
      case "z" => new ValueOrException(Some(record.z), None)
      case "w" => new ValueOrException(Some(record.w), None)
      case _ => new ValueOrException(None, Some(new Exception("Unknown field "+field)))
    }
}
