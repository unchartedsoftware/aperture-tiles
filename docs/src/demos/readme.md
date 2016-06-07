---
section: Demos
permalink: demos/
layout: section
---

Aperture Tile Demonstrations
============================

The following demos illustrate the utility of tile-based visual analytics and the possibilities of interactive visualization for big data. These demos illustrate:

-   Seamless zoom and pan interaction on maps and cross-plots
-   Server-side and client-side rendering of tile visualizations
-   Rich layer controls for interactive filtering of tile data and user color ramp selection
-   Interactive HTML5 tile visualizations
-   Multiple aggregation summaries at tile-level with tile carousels
-   Integration with advanced analytics incorporated in generated tile data
-   Use of Aperture tiles for exploratory data analysis by plotting *all the data*

## Tile-Based Metrics of NYC Taxi Trip Data ##

This demo, which was presented at [Strata and Hadoop World 2014](http://strataconf.com/stratany2014), enables the exploration of data for 187 million NYC taxi trips taken between Jan 1, 2013 - Jan 3, 2014. Trip data is displayed over a variety of metrics (both on a [Google Maps](https://maps.google.com) base layer and in cross-plot form) with the goal of answering the following questions:

- Where is the best place to catch a cab?
- How can a cab driver make the most money?

Among the interesting patterns that the demo reveals are:

- Passengers at JFK Airport tip approximately 5% less than passengers at LaGuardia
- Downtown passengers tip less than midtown passengers
- Downtown passengers travel further on average than midtown passengers
- Average hourly wage expected by pickup location
- Patterns in total fare vs. time (quiet Sundays, big snow storms, rate increases)

The source dataset for this demo was acquired from the [NYC Taxi and Limousine Commission](http://www.nyc.gov/html/tlc/html/home/home.shtml) by [Chris Whong](http://chriswhong.com/open-data/foil_nyc_taxi/) through a Freedom of Information Law (FOIL) request. Each trip record contains latitude/longitude coordinates and timestamps for the pickup and drop-off locations, along with other stats related to the fare, tip and taxes.

&gt; Explore the [NYC Taxi Trip demo](http://aperturetiles.com/nyc-taxi/?map=6&baselayer=0)

## Tile-Based Heatmap of Twitter Messages ##

This demo shows the scalability of map-oriented big data plots using heatmaps. The application contains a heatmap layer of Twitter messages by location on a [Google Maps](https://maps.google.com) base layer.

&gt; Explore the [Twitter Heatmap demo](http://aperturetiles.com/twitter-heatmap/)

## Tile-Based Topic Tracking in Twitter Messages ##

This demo shows map-oriented tile applications using multiple layers of tile data and tile carousels to flip through multiple per-tile analytics. The application shows a heatmap layer of Twitter messages by location on a [Google Maps](https://maps.google.com) base layer. An "aggregate marker" layer provides an interactive carousel of visual analytic summaries of each tile region. These aggregate markers summarize and provide interactive visualizations that support analysis and integrate with advanced analytics.

In this demo, topics of interest are extracted from Twitter messages across South America. Word clouds summarize the frequency of each topic in the tiles, while histograms show trends. Integrated [machine translation](https://translate.google.com/) converts from the native language in which tweets are written to English.

&gt; Explore the [Twitter Topics demo](http://aperturetiles.com/twitter-topics/)
<br>&gt; Access the source code in the [tile-examples](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples) directory of the project

## Tile-Based Sentiment Analysis of Twitter Messages ##

This Aperture tile demo shows a heatmap layer of Twitter messages by location on a [Google Maps](https://maps.google.com) base layer. An "aggregate marker" layer provides an interactive carousel of visual analytic summaries of each tile region summarizing the top topics discussed in the Twitter messages and the sentiment of the Tweet (positive, neutral and negative). Histograms of topics show sentiment trends.

&gt; Explore the [Twitter Sentiment demo](http://aperturetiles.com/twitter-sentiment/)

## Big Data Plots of Bitcoin for Exploratory Data Analysis ##

This demo enables exploratory data analysis of [Bitcoin](http://bitcoin.org/) transactions, a well-known digital currency. Using an interactive cross-plot of transaction amounts against time, Aperture Tiles aids the characterization and understanding of the underlying distribution, quality and gaps of the source data. 
A heat map is applied to the data points to summarize frequency distribution, effectively highlighting interesting data structures, such as the "grinding down" of bitcoins through micro-transfers of pools of bitcoins.

&gt; Explore the [Bitcoin demo](http://aperturetiles.com/bitcoin-demo/)

## Julia Set Example ##

This demo is used in the [Quick Start guide](../docs/development/getting-started/quick-start/) to illustrate the process of generating tiles and configuring a tile client to display them in a web browser. 

&gt; Explore the [Julia Set demo](http://aperturetiles.com/julia-demo/)
<br>&gt; Access the source code in the [tile-examples](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples) directory of the project