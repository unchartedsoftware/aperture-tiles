---
section: Demos
permalink: demos/index.html
layout: default
---

Aperture Tile Demonstrations
============================

The following demos illustrate the utility of tile based visual analytics and the possibilities of create interactive visualization for big data.


Tile-based Heatmap of Twitter Messages
--------------------------------------

This Aperture tile demo shows the scalability of map-oriented big data plots using heatmaps. The application shows a heatmap layer of Twitter messages by location on a [Google Maps](https://maps.google.com) base layer.

This demo illustrates:

-   Seamless zoom and pan interaction on maps
-   Server side rendering of tiled heatmaps
-   Rich layer controls for interactive filtering of tile data and user color ramp selection

[> Explore the Twitter Heatmap demo](https://tiles.oculusinfo.com/twitter-heatmap/)

Tile-based Topic Tracking in Twitter Messages
---------------------------------------------

This Aperture tile demo shows map oriented tile applications using multiple layers of tile data and the use of tile carousels to flip through multiple per tile analytics. The application shows a heatmap layer of Twitter messages by location on a [Google Maps](https://maps.google.com) base layer. An "aggregate marker" layer provides an interactive carousel of visual analytic summaries of each tile region. These aggregate markers summarize and provide interactive visualizations that support analysis and integrate with advanced analytics. Client side rendering of tile data uses the [ApertureJS visualization framework library](http://aperturejs.com/).  In this demo, topics of interest are extracted from Twitter messages across South America.  Word clouds summarize the frequency of each topic in the tiles and histograms show trends.  Integrated [machine translation](https://translate.google.com/) converts from native language to english.

This demo illustrates:

-   Seamless zoom and pan interaction on maps
-   Interactive HTML5 tile visualizations
-   Multiple aggregation summaries at tile-level with tile carousels
-   Server and client side rendering of tile visualizations
-   Integration with advanced analytics incorporated in generated tile data

[> Explore the Twitter Topics demo](https://tiles.oculusinfo.com/twitter-topics/)

The source code for this demo is provided in the [tile-examples](https://github.com/oculusinfo/aperture-tiles/tree/master/tile-examples) directory of the project.

Tile-based Sentiment Analysis of Twitter Messages
-------------------------------------------------

This Aperture tile demo shows a heatmap layer of Twitter messages by location on a [Google Maps](https://maps.google.com) base layer. An "aggregate marker" layer provides an interactive carousel of visual analytic summaries of each tile region summarizing the top topics discussed in the Twitter messages and the sentiment of the Tweet (positive, neutral and negative). Histograms of topics show sentiment trends.

This demo illustrates:

-   Seamless zoom and pan interaction on maps
-   Interactive HTML5 tiles
-   Multiple aggregation summaries at tile-level with tile carousels
-   Server and client side rendering of tile visualizations
-   Integration with advanced analytics incorporated in generated tile data

[> Explore the Twitter Sentiment demo](https://tiles.oculusinfo.com/twitter-sentiment/)

Big Data Plots of Bitcoin for Exploratory Data Analysis
-------------------------------------------------------

This demo illustrates exploratory data analysis using cross-plots of dimensions of interest of large data for the purpose of characterizing and understanding its underlying distribution, data quality and gaps in values. [John Tukey](http://en.wikipedia.org/wiki/John_Tukey) emphasized examining the data before applying a specific model and attempting to harness its information. In this spirit, we have create a demo analyzing the well known digital currency [Bitcoin](http://bitcoin.org/). Using Aperture tiles a zoom and pan cross plot of time by bitcoin transaction amount was created. A heat map is applied to the data points to summarize frequency distribution. The plot makes apparent interesting structural properties of the data such as the "grinding down" of bitcoins through micro-transfers of pools of bitcoins.

This demo illustrates:

-   Seamless zoom and pan interaction on cross-plots
-   Use of Aperture tiles for exploratory data analysis by plotting *all the data*

[> Explore the Bitcoin demo](https://tiles.oculusinfo.com/bitcoin-demo/)

Julia Set Example
-----------------

This demo is used in the Quick Start guide to illustrate the process of generating tiles and configuring a tile client to display them in a web browser. 

[> Explore the Julia Set demo](https://tiles.oculusinfo.com/julia-demo/)

The source code for this demo is provided in the [tile-examples](https://github.com/oculusinfo/aperture-tiles/tree/master/tile-examples) directory of the project.