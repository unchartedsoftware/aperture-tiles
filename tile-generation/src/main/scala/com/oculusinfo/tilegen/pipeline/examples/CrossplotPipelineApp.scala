/**
 * Copyright © 2013-2015 Uncharted Software Inc.
 *
 * Property of Uncharted™, formerly Oculus Info Inc.
 * http://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tilegen.pipeline.examples

import com.oculusinfo.tilegen.datasets.TilingTaskParameters
import com.oculusinfo.tilegen.pipeline.PipelineOperations.{KeyValuePassthrough, _}
import com.oculusinfo.tilegen.pipeline.{OperationType, PipelineApp, PipelineStage, PipelineTree}
import com.oculusinfo.tilegen.util.MissingArgumentException
import org.apache.spark.sql.SQLContext

/**
 * Pipeline to generate a geo heatmap with a time filter applied prior to tiling.  Requires
 * columns named x, y and value.  See PipelineOperations for additional operations that can
 * be tied into the pipeline.
 */
class CrossplotPipelineApp(args: Array[String]) extends PipelineApp("Crossplot Pipeline", args) {
	try {
		// Stage that load and maps the input CSV data to field name/type tuples.  Only take those that
		// are needed.
		val loadStage = PipelineStage("load", loadCsvDataOp(source, new KeyValuePassthrough(columnMap)))

		// Stage to cache data after filtering operations have been applied.
		val cacheStage = PipelineStage("cache", cacheDataOp())

		// Stage to generate heatmap tiles.  Needs columns in CSV defined as "x", "y", "value".
		val tilingParams = new TilingTaskParameters(name, description, None, levelSets, 256, 256, Some(partitions), None)
		val heatmapStage = PipelineStage("heatmap",
			crossplotHeatMapOp("x", "y", tilingParams, hbaseParameters, OperationType.SUM, Some("value"), Some("double")))

		// Instantiate and execute pipeline
		loadStage.addChild(new PipelineStage("debug0", takeAndPrintOp(10, "LOADED ==> ")))
			.addChild(cacheStage)
			.addChild(heatmapStage)
		PipelineTree.execute(loadStage, new SQLContext(sc))

		// Cleanup the spark context.
		sc.stop()
	} catch {
		case e: MissingArgumentException =>
			logger.warn("Argument exception: " + e.getMessage, e)
			argParser.usage
	}
}

object CrossplotPipelineApp {
	def main(args: Array[String]) {
		new CrossplotPipelineApp(args)
	}
}