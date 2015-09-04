package com.oculusinfo.tilegen.pipeline.examples

import com.oculusinfo.tilegen.tiling.LocalTileIO
import java.io.File
import org.scalatest.FunSuite

class GeoHeatmapPipelineAppTest extends FunSuite {

	def removeRecursively (file: File): Unit = {
		if (file.isDirectory) {
			file.listFiles().map(removeRecursively)
		}
		file.delete()
	}

	test("Test heatmap pipeline basic sanity") {

		val sourcePath = classOf[GeoHeatmapPipelineAppTest].getResource("/tweets.csv").toURI.getPath
		val columnPath = classOf[GeoHeatmapPipelineAppTest].getResource("/tweets-columns-geo.properties").toURI.getPath
		val properties = System.getProperties
		properties.setProperty("spark.master", "local[4]")

		val args = Array("-source", s"$sourcePath",
			"-columnMap", s"$columnPath",
			"-levels", "0,1",
			"-start", "2014/11/01.00:00:00.+0000",
			"-end", "2014/12/01.00:00:00.+0000",
			"-name", "heatmap-test",
			"-description", "test_heatmap_tiles")
		GeoHeatmapPipelineApp.main(args)

		// Load the metadata and validate its contents - gives us an indication of whether or not the
		// job completed successfully.
		val tileIO = new LocalTileIO("avro")
		val metaData = tileIO.readMetaData("heatmap-test").getOrElse(fail("Metadata not created"))
		val customMeta = metaData.getAllCustomMetaData()
		assertResult(0)(customMeta.get("0.minimum"))
		assertResult(12)(customMeta.get("0.maximum"))
		assertResult(0)(customMeta.get("1.minimum"))
		assertResult(12)(customMeta.get("1.maximum"))
		assertResult(0)(customMeta.get("global.minimum"))
		assertResult(12)(customMeta.get("global.maximum"))

		removeRecursively(new File("heatmap-test"))
	}
}
