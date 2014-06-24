---
section: Tour
subtitle: Overview
permalink: tour/overview/index.html
layout: default
---

Aperture Tiles Overview
=======================

Aperture Tiles provides a single, consistent tool that uses a tile-based framework to visualize data at any scale. Effective use of a tiling approach has multiple advantages:

* the ability to visualize and compute analytics at multiple resolutions.
* the ability to compose powerful visual analytic capabilities by overlaying multiple layers of information.

There are many mature, feature-rich JavaScript libraries that support integration of tile interactions into web applications. Tiling is useful for large-scale data analysis because it can provide effective data compression and efficient distribution of analytic computation. Due to the increasing exposure of web applications such as Google Maps, tile interactions have become familiar and widespread. 

However, a conventional tiling approach is currently limited by two critical restraints:

* Tiles can take a long time to generate.
* Data values are baked into pre-rendered tiles, resulting in stale tiles representing static data.

Aperture Tiles addresses these drawbacks by using distributed computation. This method generates data for tiles in far less time, by using defined, reproducible data flows that can regenerate tiles from live data. This not only speeds the process of generating data for tiles, but serves to reduce the staleness of the data in any given tile. Aperture Tiles also separates tile data apportionment from the visual rendering process. This approach allows tile visualization to be altered real-time, allowing interactions like filtering of data values or the application of new visualization techniques. Most importantly, tile displays are not limited to static images. This feature allows creation of highly interactive visual analytic applications using web-based, client-side rendering, such as [ApertureJS](http://aperturejs.com/).

Tile-based Visual Analytics for Interactive Visualization
---------------------------------------------------------

Aperture Tiles uses a multi-stage process to develop tile-based visual analytics. 

1. The aggregation stage projects and bins data into a predefined grid, such as the Tiling Map Service standard.  This uses a (z,x,y) coordinate system, where z identifies the zoom level, and x,y identifies a specific tile on the plot for the given zoom level. 

2. The summarization stage applies one or more summary statistics or other analytics to the data in each tile region, storing the result in a data store. 

3. The rendering stage maps the summary to a visual representation, and renders it to an image tile or html at request time. This stage is capable of rendering on-demand support interactions such as filtering, or rich dynamic interactions such as brushing and drill-downs, if using client-side rendering with html and JavaScript.