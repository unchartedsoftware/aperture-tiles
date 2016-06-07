---
section: Tour
subsection: Overview
permalink: tour/
layout: submenu
---

Aperture Tiles Overview
=======================

Aperture Tiles provides a single, consistent tile-based framework to visualize and analyze data at any scale, including extremely large datasets. Effective use of a tiling approach has multiple advantages:

- You can *plot all the data* without needing aggregation or sampling.
- In a web browser, users can interactively visualize the data and associated computed analytics at multiple resolutions, from very high overviews to very detailed individual data points.
- Due to the increasing exposure of web-based geographic tile applications such as Google Maps, user interactions have become familiar and widespread.
- You can compose powerful visual analytic capabilities by overlaying multiple facets of information (e.g., alerts, patterns, extracted features) on top of the complete data context.

There are several mature, feature-rich JavaScript libraries that support integration of tile rendering and interaction into web applications. However, these conventional tiling approaches are limited by two critical restraints:

- Tiles can take a long time to generate.
- Data values are baked into pre-rendered tiles, resulting in stale tiles representing static data.

Aperture Tiles addresses these drawbacks by using cloud-distributed computation. This method generates data for tiles in far less time, by using defined, reproducible data flows that can regenerate tiles from live data. This not only speeds the process of generating data for tiles, but serves to reduce the staleness of the data in any given tile. Aperture Tiles also separates tile data apportionment from the visual rendering process. This approach allows tile visualization to be altered in real-time, allowing interactions like filtering of data values or the application of new visualization techniques. Most importantly, tile displays are not limited to static images. Tile-based visual analytics can be the centerpiece of highly interactive visual analytic applications using web-based, client-side rendering, such as [Aperture JS](http://aperturejs.com/).

## Computing Tile-based Visual Analytics ##

Aperture Tiles uses a multi-stage process to develop tile-based visual analytics. 

1. The aggregation stage projects and bins data into a predefined pyramid grid, such as the Tiling Map Service standard. This uses a (z,x,y) coordinate system, where z identifies the zoom level, and x,y identifies a specific tile on the plot for the given zoom level. 
2. The summary and analytical stage applies one or more summary statistics or other analytics to the data in each tile region, storing the result in a data store. 
3. The rendering stage maps the computed summary and analytics to visual representations, and renders them to an image tile or html at request time. This stage is capable of rendering on-demand interactions such as filtering, or rich dynamic interactions such as brushing and drill-downs, if using client-side rendering with html and JavaScript.

## Next Steps ##

For more information on the components that make up Aperture Tiles, see the [System Description](components/) topic.