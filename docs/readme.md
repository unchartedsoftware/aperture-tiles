---
section: Home
permalink: index.html
layout: default
---

Aperture Tiles <span class="tagline">tile-based visual analytics for big data</span>
=======================================================

New tools for exploring, characterizing and analyzing 'big data' are required to suggest initial hypotheses for testing. The widespread adoption of web-based maps provides a familiar set of interactions for exploring abstract large data spaces. Building on these techniques, Aperture Tiles provides browser-based interactive visual analytics that scale to billions or more points of data.

### Web Tile Rendering for Big Data Visual Analytics

Aperture tiles provides tools for web-based interactive big data visual analytics that uses tile-based rendering similar to geographic maps and interaction paradigms. Tile-based geographic maps provide a solid framework to construct large scale visualization that feature continuous data along two dimensions, use of layering and legends, axes and scales. Web delivery of maps using tiled rendering has benefit from years of work. With widespread use, map interactions have become familiar and make exploration of an abstract large data space easy, even enjoyable. Using similar techniques, Aperture Tiles provides interactive data exploration with continuous zooming on large scale datasets.

Tile-based visual analytics divides the data into evenly sized hierarchical tiles, and then calculates and overlays a tile-bounded analytic, for example, the top five twitter hashtags for the region. These analytics serve as "aggregation markers". As the user zooms into the map, the aggregation markers shown become increasingly localized to their bounded region.

### Extensible and Open Source

The Aperture Tiles services and API are designed to be extensible, allowing a broad community to leverage and extend capabilities in creative ways. The extensible AVRO tile format allows generation of tiles by third parties, which can be served using the Aperture Tiles server. Aperture Tiles leverages open standards such as [Tile Map Services (TMS)](http://en.wikipedia.org/wiki/Tile_Map_Service) that are widely supported by web mapping clients and servers.

Aperture Tile generation service builds on the [Apache Spark](http://spark.incubator.apache.org/) and [Hadoop](http://hadoop.apache.org/) cluster computing systems providing the ability to scale up to billions or more data points. Tile sets involving billions of tiles are efficiently handled using [Apache Hive](http://hive.apache.org/) for distributed data storage.

Aperture Tiles is under ongoing development and is freely available for download under [The MIT License](http://www.opensource.org/licenses/MIT) (MIT) open source licensing. Unlike GNU General Public License (GPL), MIT freely permits distribution of derivative work under proprietary license, without requiring the release of source code.

