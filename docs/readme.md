---
section: Home
permalink: index.html
layout: default
---

Aperture Tiles <span class="tagline">tile-based visual analytics for big data</span>
=======================================================

The critical first step in exploring a new dataset is to gain a general understanding of the data that suggests initial hypotheses to be tested.  However, data can come in any format, and often in such volume that gaining this characterization becomes a challenging task.  

There are several classes of user that need this overview of "big data" in order to effectively explore and understand it.

* **Data Scientists:** need to design and apply the correct analytics that best address the character of the data.
* **Visualization Designers:** must select the appropriate visual metaphors and interactions to represent the data.
* **Analysts:** need to understand the data in order to know which datasets to select, and which tools to use to approach a given analytic question.

There is a clear need for new, powerful tools to aid these types of users in exploring, characterizing, and analyzing 'big data.' Aperture Tiles provides this aid with browser-based, interactive visual analytics that scale to data sets with billions or more of data points.

### Web Tile Rendering for Big Data Visual Analytics

The widespread adoption of web-based maps provides a familiar set of interactions for exploring large, abstract data spaces. Aperture Tiles builds on these techniques to provide tools for big data visual analytics that use tile-based rendering.  This provides an interactive experience that is similar to geographic, web-based maps. 

Tile-based geographic maps provide a solid framework to construct large scale visualization:

* Continuous data along two dimensions
* Use of data layering and legends
* Use of axes and scales

Web delivery of maps using tiled rendering has benefitted from years of work. With widespread use, map interactions have become familiar and make exploration of large data spaces easy and even enjoyable. By using this familiar interaction method, Aperture Tiles can provide a big data exploration experience that is just as easy and enjoyable.

Tile-based visual analytics (TBVA) is a technique that divides the data into evenly sized hierarchical tiles, and then calculates and applies an analytic to the data contained in each tile. For example, if examining a large Twitter dataset, the data would be divided into equally sized tiles across the geographic area.  Then, an analytic is applied to each tile, such as the top five Twitter hashtags for the bounded region. As the user zooms into the map, Aperture Tiles presents a new layer of tiles that each contain a smaller portion of the full dataset.  In each new zoom layer, the analytic applied to each tile becomes more localized.

### Extensible and Open Source

The Aperture Tiles services and API are designed to be extensible, allowing a broad community to leverage and extend its capabilities in creative ways. The extensible AVRO tile format allows generation of tiles by third parties, which can be served using the Aperture Tiles server. Aperture Tiles leverages open standards such as [Tile Map Services (TMS)](http://en.wikipedia.org/wiki/Tile_Map_Service) that are widely supported by web mapping clients and servers.

Aperture Tile generation service builds on the [Apache Spark](http://spark.incubator.apache.org/) and [Hadoop](http://hadoop.apache.org/) cluster computing systems, providing the ability to scale up to billions (or more) data points. Tile sets involving billions of tiles are efficiently handled using [Apache HBase](http://hbase.apache.org/) for distributed data storage.

Aperture Tiles is under ongoing development and is freely available for download under [The MIT License](http://www.opensource.org/licenses/MIT) open source licensing. Unlike GNU General Public License (GPL), MIT freely permits distribution of derivative work under proprietary license, without requiring the release of source code.

###Interested in Learning More?

* [Tour](/tour/overview/): Take our tour to learn more about Aperture Tiles.
* [Documentation](/documentation/quickstart/): Learn how to install, implement, and test your Aperture Tiles applications.
	* [Quick Start](/documentation/quickstart/): our Julia data set provides an example of the process for generating tiles and visualizing them using Aperture Tiles.
* [Download](download/): For details on downloading pre-packaged versions or aquiring the Aperture Tiles source code visit our download page.