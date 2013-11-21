Aperture Tile-Based Visual Analytics
==============

New tools for raw data characterization of 'big data' are required to suggest initial hypotheses 
for testing. The widespread adoption of web-based maps provides a familiar set of interactions for 
exploring abstract large data spaces. Building on these techniques, Aperture-Tiles provides 
browser-based interactive visual analytics that allow visualization of billions or more points of 
data.

Aperture Tile-Based Visual Analytics Components
==============

The project consists of three main components.

Spark Tile Generation Analytic
--------
* Apache Spark based analytic for computing data tile set
* Converts raw data files into AVRO data tile files with metadata descriptor files
* AVRO data tiles summarize the tile contents

Aperture Tile Server
--------
* Defines a REST API for requesting tiles and rendering properties
* Retrieves AVRO data tile files and renders to PNG files for display in tile clients
* Optional: Apache HBase distributed file system for storage of tile files

Aperture Tile Client
--------
* Web clients that display interactive tiles
* Requests tiles from Aperture Tile Server

To find out more, see the [Distributed Tiling Overview](https://github.com/oculusinfo/aperture-tiles/releases/download/0.1.0/distributed-tiling-overview.pdf) or take a look at our [wiki](../../wiki/Aperture-Tile-Based-Visual-Analytics).
