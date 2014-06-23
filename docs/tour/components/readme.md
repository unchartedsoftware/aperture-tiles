---
section: Tour
subtitle: Components
permalink: tour/components/index.html
layout: default
---

Aperture Tiles Components
=========================

Aperture-Tiles implements a data flow which breaks the visualization process into three distinct components: tile generation, tile server, and tile client. This flexible framework allows third parties to leverage or replace the pieces of Aperture Tiles as they see fit. For example, using Aperture Tiles AVRO tile data format third parties can produce their own tile data using their own analytics. Using the Tile Server standards based [Tile Map Services](http://en.wikipedia.org/wiki/Tile_Map_Service) third party tile clients can integrate Aperture Tiles.

Distributed Tile Generation
---------------------------

The Aperture Tiles generation analytic leverages cluster computing with [Apache Spark](http://spark.incubator.apache.org/) to compute tiles from big data rapidly. The tile generation analytic transforms raw data into AVRO data tile files with metadata descriptor files, summarizing all tile content. AVRO data files allow for language neutrality and support multiple summaries per bin allowing a flexible and extensible analytic. Finished tiles are stored in HBase to allow efficient scalability to billions of tiles.

Tile Server
-----------

The Aperture Tile server provides services to serve rendered tiles or raw tile data. The Aperture Tile Server defines a REST API for requesting tiles and rendering properties. The tile server works by retrieving AVRO data tile files and rendering PNG images for display in the tile client or external visualization applications. The Aperture Tile Server also has the ability to directly serve raw tile data via JSON supporting data services or rich client side rendering capabilities.

Tile Client
-----------

The Aperture Tile client is a full featured interactive web application for displaying layered tiles with zoom, pan and filtering interactions. Example client side rendering is provided to demonstrate some of the possibilities of using aggregation marker visualization techniques. The web application is designed to promote exploratory data analysis by displaying high level patterns and summary statistics while preserving all fine detail at the appropriate zoom level.

