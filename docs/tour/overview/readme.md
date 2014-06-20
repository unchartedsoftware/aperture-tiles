---
section: Tour
subtitle: Overview
permalink: tour/overview/index.html
layout: default
---

Aperture Tiles Overview
=======================

Aperture-Tiles provides a single, consistent tool which effectively leverages tiling to visualize data at any scale. Effectively using a tiling approach has multiple advantages such as visualizing and computing analytics at multiple resolutions, or composing powerful visual analytic capabilities by overlaying multiple layers of information. Integrating tiles into web applications is supported by a host of rich and mature JavaScript libraries for interacting with tiled images. Tiling can provide effective data compression and efficient distribution of computation for extreme scale data. Due to the increasing exposure of web applications such as Google Maps, tile interactions have become familiar and widespread. However, a conventional tiling approach is currently limited by two critical restraints. Tiles can take a long time to generate and data values are baked into pre-rendered tiles, resulting in stale tiles representing static data.

Aperture-Tiles addresses these drawbacks by using distributed computation to generate data for tiles in orders of magnitude less time using defined reproducible data flows which can regenerate tiles from live data, reducing tile staleness. Separation of tile data from rendering allows tile visualization to be altered real-time, such as filtering data values, or applying new visualization techniques. Most importantly, tiles can represent more than static images allowing creation of highly interactive visual analytic applications using web-based client side rendering such as [ApertureJS](http://aperturejs.com/).

Tile-based Visual Analytics for Interactive Visualization
---------------------------------------------------------

Aperture Tiles uses a multi-stage process to develop tile-based visual analytics. The aggregation stage projects and bins data into a predefined grid, such as the Tiling Map Service standard with a (z,x,y) coordinate system where z identifies the zoom level, and x,y identifies a specific tile on the plot for the given zoom level. The summarization stage applies one or more summary statistics or other analytics to the data in each tile region storing the result in a data store. The rendering stage maps the summary to a visual representation, and renders it to an image tile or html at request time. Rendering on demand support interactions such as filtering, or rich dynamic interactions such as brushing and drill-downs if using client side rendering with html and JavaScript.

