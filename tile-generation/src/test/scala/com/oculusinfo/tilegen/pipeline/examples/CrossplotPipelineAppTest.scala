package com.oculusinfo.tilegen.pipeline.examples

import java.io.File

import com.oculusinfo.tilegen.tiling.LocalTileIO
import org.scalatest.FunSuite


class CrossplotPipelineAppTest extends FunSuite {

	 def removeRecursively (file: File): Unit = {
		 if (file.isDirectory) {
			 file.listFiles().map(removeRecursively)
		 }
		 file.delete()
	 }

	 test("Test crossplot pipeline basic sanity") {

		 val sourcePath = classOf[CrossplotPipelineAppTest].getResource("/tweets.csv").toURI.getPath
		 val columnPath = classOf[CrossplotPipelineAppTest].getResource("/tweets-columns-cross.properties").toURI.getPath
		 val properties = System.getProperties
		 properties.setProperty("spark.master", "local[4]")

		 val args = Array("-source", s"$sourcePath",
			 "-columnMap", s"$columnPath",
			 "-levels", "0,1",
			 "-name", "crossplot-test",
			 "-description", "test_crossplot_tiles")
		 CrossplotPipelineApp.main(args)

		 // Load the metadata and validate its contents - gives us an indication of whether or not the
		 // job completed successfully.
		 val tileIO = new LocalTileIO("avro")
		 val metaData = tileIO.readMetaData("crossplot-test").getOrElse(fail("Metadata not created"))
		 val customMeta = metaData.getAllCustomMetaData()
		 assertResult(0)(customMeta.get("0.minimum"))
		 assertResult(103)(customMeta.get("0.maximum"))
		 assertResult(0)(customMeta.get("1.minimum"))
		 assertResult(103)(customMeta.get("1.maximum"))
		 assertResult(0)(customMeta.get("global.minimum"))
		 assertResult(103)(customMeta.get("global.maximum"))

		 removeRecursively(new File("crossplot-test"))
	 }
 }
