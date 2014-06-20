---
section: Demos
permalink: demos/index.html
layout: default
---

Aperture Tile Demonstrations
============================

The following demos illustrate the utility of tile based visual analytics and the possibilities of create interactive visualization for big data. All source code for the demos is provided in the tile-examples directory of the project.

Tile-based Heatmap of Twitter Messages
--------------------------------------

This Aperture tile demo shows the scalability of map oriented big data plots using heatmaps. The application shows a heatmap layer of twitter messages by location on a [Google Maps](https://maps.google.com) base layer.

This demo illustrates:

-   Seamless zoom and pan interaction on maps
-   Server side rendering of tiled heatmaps
-   Rich layer controls for interactive filtering of tile data and user color ramp selection

Tile-based Analytics of Twitter Messages
----------------------------------------

This Aperture tile demo shows map oriented tile applications using multiple layers of tile data and the use of tile carousels to flip through multiple per tile analytics. The application shows a heatmap layer of twitter messages by location on a [Google Maps](https://maps.google.com) base layer. An "aggregate marker" layer provides an interactive carousel of visual analytic summaries of each tile region. These aggregate markers summarize and provide interactive visualizations that support analysis and integrate with advanced analytics. Client side rendering of tile data uses the [ApertureJS visualization framework library](http://aperturejs.com/).

This demo illustrates:

-   Seamless zoom and pan interaction on maps
-   Interactive HTML5 tiles
-   Multiple aggregation summaries at tile-level with tile carousels
-   Server and client side rendering of tile visualizations
-   Integration with advanced analytics incorporated in generated tile data

Big Data Plots of Bitcoin for Exploratory Data Analysis
-------------------------------------------------------

This demo illustrates exploratory data analysis using cross-plots of dimensions of interest of large data for the purpose of characterizing and understanding its underlying distribution, data quality and gaps in values. [John Tukey](http://en.wikipedia.org/wiki/John_Tukey) emphasized examining the data before applying a specific model and attempting to harness its information. In this spirit, we have create a demo analyzing the well known digital currency [Bitcoin](http://bitcoin.org/). Using Aperture tiles a zoom and pan cross plot of time by bitcoin transaction amount was created. A heat map is applied to the data points to summarize frequency distribution. The plot makes apparent interesting structural properties of the data such as the "grinding down" of bitcoins through micro-transfers of pools of bitcoins.

This demo illustrates:

-   Seamless zoom and pan interaction on cross-plots
-   Use of Aperture tiles for exploratory data analysis by plotting *all the data*