---
section: Documentation
subtitle: Installation
permalink: documentation/setup/index.html
layout: default
---

Installation and Compilation
============================

The instructions on this page are intended for developers who want to install the Aperture Tiles source code and build their own custom projects. For quick examples of the capabilities of Aperture Tiles:

- See the [Demos](../../demos/) page to access fully functional demonstrations of Aperture Tiles from your web browser.
- See the [Download](../../download) page to access a set of streamlined source code packages designed to help you understand the high-level process of creating an Aperture Tiles. Instructions for using the packages to project a Julia set fractal on an X/Y plot are available on the [Quick Start](../quickstart) page. 

##<a name="prerequisites"></a>Prerequisites

This project has the following prerequisites:

- **Operating System**: Linux or OS X.
- **Languages**:
	-   [**Scala**](http://www.scala-lang.org/) version 2.9.3
	-   [**Java**](http://www.java.com/) (JDK version 1.7+)
- **Cluster Computing**: To facilitate large tiling jobs, Aperture Tiles supports a cluster computing framework. Note that if you only intend to run small jobs (data sets that fit in the memory of a single machine) or are willing to take a long time to complete them, you can skip the Hadoop/HDFS/HBase installation and run Spark on a single node and read/write to your local file system.
	-   **Hadoop/HDFS** (Optional) - Choose your preferred flavor  ([Cloudera](http://www.cloudera.com/content/cloudera/en/products/cdh.html), [Apache](http://hadoop.apache.org/docs/r1.2.1/index.html), [MapR](http://www.mapr.com/products/apache-hadoop), [HortonWorks](http://hortonworks.com/), etc.). Use in conjunction with HBase.
	-   **HBase** (Optional) - [Apache HBase](http://hbase.apache.org/) Non-relational database for Hadoop/HDFS that lets you store particularly large data sources and tile sets.
	-   **Apache Spark** - [Apache Spark](http://spark.incubator.apache.org/) version 0.9.0 or greater (version 1.0.0 recommended). When you set up Spark, you have to configure the version of Hadoop with which it will be working (if applicable).
-  **Web Server**: the Tile Server and client are built using the [Restlet](http://restlet.org/) web framework, and require a servlet compatible web server. Choose your preferred implementation ([**Apache Tomcat**](http://tomcat.apache.org/) or [**Jetty**](http://www.eclipse.org/jetty/)).
-   **Build Automation**: All Aperture Tiles projects build with [**Apache Maven**](http://maven.apache.org/) version 3.1.0 (other versions may work). Ensure Maven is configured properly on the system on which you are building Aperture Tiles.

###<a name="environment-variables"></a>Environment Variables
Set the following environment variables:

- `SPARK_HOME` - the location of the Spark installation`
- `SPARK_MEM` - the amount of memory to allocation to Spark`
- `MASTER` - the node on which the cluster is installed`

##<a name="source-code"></a>Source Code

The Aperture Tiles source code is available on [GitHub](https://github.com/oculusinfo/aperture-tiles). Aperture Tiles is dependent on the ApertureJS source code, which you can also download from [GitHub](http://aperturejs.com/). To install both projects:

1. Run the `mvn install` command in the `aperture` folder found in the root ApertureJS directory.
2. Run the `mvn install` command in the root Aperture Tiles directory.

###<a name="environment-variables"></a>Environment Variables
Set the following environment variables:

- `SPARK_HOME` - the location of the Spark installation`
- `SPARK_MEM` - the amount of memory to allocation to Spark`
- `MASTER` - the node on which the cluster is installed`

###<a name="project-structure"></a>Project Structure

The Aperture Tiles is made up of eight sub-projects:

-   **math-utilities** - Basic, underlying Java utilities (for angles, linear algebra and statistics) to aid in processing data.
-   **geometric-utilities** - Advanced math utilities for processing geometry and geographic problems.
-   **binning-utilities** - Basic substrate of tiling, bin data structures, bin processing and basic bin storage classes.
-   **tile-generation** - Spark-based tools to generate tile pyramids from raw data.
-   **tile-service** - Web service to serve tiles from tile pyramids to web clients.
-   **annotation-service** - Services for adding annotations to Aperture Tiles visualizations
-   **tile-client** - Simple web client to display tiles from tile pyramids.
-   **tile-packaging** - Packaged assembly of the tile generation service for the [Quick Start](../quickstart/) example on this site.
-   **tile-client-template** - Starter template for creating a Tile Client and Server application.
-   **tile-examples** - Example applications and documentation website.
 
###<a name="building-project"></a>Building the Project

####<a name="hbase-version"></a>Specifying Your Hadoop/HBase Version

Prior to building the project, you specify the version of Hadoop and/or HBase you installed (if applicable). Edit the `<properties>` section of the *aperture-tiles/pom.xml* build file to select the valid settings for your version. See the comments in the file for more details.
 
If you plan to run only Spark in standalone mode, you can skip this step.

####<a name="compiling"></a>Compiling the Aperture Tiles Projects

Before you compile the Aperture Tiles source code, you must install the ApertureJS project. Run the `mvn install` command in the `aperture` folder in the ApertureJS root directory.

Once the ApertureJS installation is complete, run the `mvn install` command again, this time in the root Aperture Tiles directory. This will compile all the project components and install .jar files for each project into your local maven repostitory on your build machine.

##<a name="next-steps"></a>Next Steps

See the [Tile Generation](../generation) or [Configuration](../configuration) topics for specifics on generating tile sets from raw data and configuring a tile server and client to create a tile-based visual analytic application.

