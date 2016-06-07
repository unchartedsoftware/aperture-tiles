---
section: Home
permalink: /
layout: section
---

Aperture Tiles <span class="tagline">tile-based visual analytics for big data</span>
====================================================================================

<h6 class="attribution">Aperture Tiles is powered by <a href="http://uncharted.software/salt/">Salt</a>, an open <br> source library for scalable, analytic layered tiles.</h6>

Aperture Tiles provides the ability to create interactive, browser-based tools to explore datasets containing billions of data points or more.
 
To make informed decisions, business and government leaders, operators and analysts increasingly rely on more data than ever. This information can come from various sources, including social media, sensor data, financial transactions, cyber and open-source data. Making sense of such massive amounts of data is extremely challenging. As a result, data is often not used to its full potential.
 
Aperture Tiles uses a pyramid of tiles to structure, analyze, visualize and interact with a user interface similar to web-based geographic map applications. This approach allows analysts to easily see and navigate all of the data without losing detail, and seamlessly "zoom in" to a more localized portion of that data.
 
Tailored analytic overlays (e.g., alerts, pattern detection, feature extraction) can be applied to the data at every scale, ranging from the entire dataset to a very small portion of it. Seeing all of the data reveals informative patterns and provides important context to understanding insights identified by computational analytics. 

Aperture Tiles is an open source software product managed by Uncharted Software Inc. The MIT License is used.

### Easy-to-Use Big Data Visual Analytics ###

The widespread adoption of web-based geographic maps provides a familiar set of zoom/pan interactions that can be similarly used for working with extremely large, abstract data spaces. Aperture Tiles builds on these techniques to provide tools for big data visual analytics that use tile-based rendering and analytics. 

Tile-based geographic maps provide an interactive experience and solid framework to construct large scale visualization, including:

* Continuous data along two dimensions
* Use of data layering and legends
* Use of axes and scales

With widespread use, map interactions have become familiar and make exploration of large data spaces easy and even enjoyable.

### Tile-Based Visual Analytics ###

Tile-based visual analytics (TBVA) is a technique that divides the data into evenly sized hierarchical tiles, and then calculates and applies an analytic to the data contained in each tile. For example, if examining a large Twitter dataset, the data would be divided into equally sized tiles across the geographic area. Then, an analytic is applied to each tile, such as the top five Twitter hashtags for the bounded region. As the user zooms into the map, Aperture Tiles presents a new layer of tiles that each contain a smaller portion of the full dataset. In each new zoom layer, the analytic applied to each tile becomes more localized.

### Extensible and Open Source ###

The Aperture Tiles services and API are designed to be extensible, allowing a broad community to leverage and extend its capabilities in creative ways. The extensible Avro tile format allows generation of tiles by third parties, which can be served using the Aperture Tiles server. Aperture Tiles leverages open standards such as [Tile Map Services (TMS)](http://en.wikipedia.org/wiki/Tile_Map_Service) that are widely supported by web mapping clients and servers.

Aperture Tile generation service builds on the [Apache Spark](http://spark.incubator.apache.org/) and [Hadoop](http://hadoop.apache.org/) cluster computing systems, providing the ability to scale up to billions (or more) data points. Tile sets involving billions of tiles are efficiently handled using [Apache HBase](http://hbase.apache.org/) for distributed data storage.

Aperture Tiles is freely available for download under [The MIT License](http://www.opensource.org/licenses/MIT) open source licensing. Unlike GNU General Public License (GPL), MIT freely permits distribution of derivative work under proprietary license, without requiring the release of source code.

### Interested in Learning More? ###

* [Tour](tour/): Take our tour to learn more about Aperture Tiles.
* [Documentation](docs/development/getting-started/quick-start/): Learn how to install, implement and test your Aperture Tiles applications.
    * [Quick Start](docs/development/getting-started/quick-start/): Our Julia dataset provides an example of the process for generating tiles and visualizing them using Aperture Tiles.
* [Live Examples](demos/): See our demos page to see live examples of the capabilities of Aperture Tiles.
* [Download](download/): For details on downloading pre-packaged versions or acquiring the Aperture Tiles source code visit our download page.