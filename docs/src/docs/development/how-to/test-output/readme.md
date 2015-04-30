---
section: Docs
subsection: Development
chapter: How-To
topic: Test Tiling Job Output
permalink: docs/development/how-to/test-output/
layout: submenu
---

Testing Tiling Job Output
=========================

A utility for testing the output of tiling jobs is included in the Aperture Tiles source code. The Bin Visualizer, which can be run as a Java application, displays basic visual representations of the individual Avro tiles in your tile set pyramid. This can help you quickly identify problems with your tiling job before you deploy your application. The Bin Visualizer currently only supports heatmap layers.

<h6 class="procedure">To use the Bin Visualizer</h6>

1. In an integrated development environment, browse to the [BinVisualizer.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/visualization/BinVisualizer.java) file in [binning-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>binning/<wbr>visualization/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/binning-utilities/src/main/java/com/oculusinfo/binning/visualization).
2. Debug the utility as a Java application.
3. Specify the location (e.g., HBase or local file system) in which you stored your Avro tiles using the **I/O** type drop-down list.
	- For HBase connections, enter the appropriate **Zookeeper quorum**, **Zookeeper port** and **HBase master** information.
	- For the local file system, specify the **Root path** and **Tile extension** (defaults to *avro*).
4. Enter the name of the tile pyramid you want to view in the **Pyramid id** field.
5. Set the following coordinates to choose the individual tile you want to view:
	- **Zoom level**, where 0 is the highest level (most zoomed out)
	- **Tile x coordinate**, where 0 is the leftmost column of tiles
	- **Tile y coordinate**, where 0 is the bottommost row of tiles
6. Click **Show tile**.

<img src="../../../../img/bin-visualizer.png" class="screenshot" alt="Bin Visualizer" />

## Next Steps ##

For details on configuring a tile server to create a tile-based visual analytic application, see the [Configure the Tile Server](../tile-server/) topic.