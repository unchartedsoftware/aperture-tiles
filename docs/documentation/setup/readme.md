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
- See the [Download](../../download) page to access a pre-built distribution designed to help you quickly get started using Aperture Tiles and to understand the high-level process of creating an Aperture Tiles application. Instructions for using the packages to project a Julia set fractal on an X/Y plot are available on the [Quick Start](../quickstart) page. 

##<a name="prerequisites"></a>Prerequisites

This project has the following prerequisites:

- **Operating System**: Linux or OS X.
- **Languages**:
	-   [**Scala**](http://www.scala-lang.org/) version 2.10.3
	-   [**Java**](http://www.java.com/) (JDK version 1.7+)
- **Cluster Computing**: To facilitate large tiling jobs, Aperture Tiles supports a cluster computing framework. Note that if you only intend to run small jobs (data sets that fit in the memory of a single machine) or are willing to take a long time to complete them, you can skip the Hadoop/HDFS/HBase installation and run Spark on a single node and read/write to your local file system.
	-   **Apache Spark** - [Apache Spark](http://spark.incubator.apache.org/) distributed computing framework for distributing tile generation across a cluster of machines.  Aperture Tiles requires version 0.9.0 or greater (version 1.0.0 recommended). Note: when you set up Spark, you need to configure the version of Hadoop with which it will be working (if applicable).
	-   **Hadoop/HDFS/HBase** (Optional) - Distributed computing stack.  HDFS is a file system for storing large data sets. Choose your preferred flavor  ([Cloudera](http://www.cloudera.com/content/cloudera/en/products/cdh.html) version 4.6 recommended, though other flavors such as [Apache](http://hadoop.apache.org/docs/r1.2.1/index.html), [MapR](http://www.mapr.com/products/apache-hadoop) and [HortonWorks](http://hortonworks.com/) may work). HBase is a non-relational database that sits atop Hadoop/HDFS. 
-  **Web Server**: the Tile Server and client are built using the [Restlet](http://restlet.org/) web framework, and require a servlet compatible web server. Choose your preferred implementation ([**Apache Tomcat**](http://tomcat.apache.org/) or [**Jetty**](http://www.eclipse.org/jetty/)).
-   **Build Automation**: All Aperture Tiles projects build with [**Apache Maven**](http://maven.apache.org/) version 3.1.0 (other 3.x versions may work). Ensure Maven is configured properly on the system on which you are building Aperture Tiles.

##<a name="source-code"></a>Source Code

The Aperture Tiles source code is available on [GitHub](https://github.com/oculusinfo/aperture-tiles). Aperture Tiles is dependent on the ApertureJS source code, which you can also download from [GitHub](https://github.com/oculusinfo/aperturejs). To install both projects:

1. Run the `mvn install` command in the `aperture` folder found in the root ApertureJS directory.
2. Run the `mvn install` command in the root Aperture Tiles directory.

###<a name="environment-variables"></a>Environment Variables
Set the following environment variables:

- `SPARK_HOME` - the location of the Spark installation
- `SPARK_MEM` - the amount of memory to allocation to Spark
- `MASTER` - the node on which the cluster is installed

###<a name="project-structure"></a>Project Structure

Aperture Tiles is made up of ten sub-projects:

-   **math-utilities** - Basic, underlying Java utilities (for angles, linear algebra and statistics) to aid in processing data.
-   **geometric-utilities** - Advanced math utilities for processing geometry and geographic problems.
-   **binning-utilities** - Basic substrate of tiling, bin data structures, bin processing and basic bin storage classes.
-   **tile-generation** - Spark-based tools to generate tile pyramids from raw data.
-   **tile-service** - Web service to serve tiles from tile pyramids to web clients.
-   **annotation-service** - Services for adding annotations to Aperture Tiles visualizations
-   **tile-client** - Simple web client to display tiles from tile pyramids.
-   **tile-packaging** - Packaged assembly of the tile generation service for the [Quick Start](../quickstart/) example on this site.
-   **tile-client-template** - Starter template for creating a Tile Client and Server application.
-   **tile-examples** - Example applications.
 
###<a name="building-project"></a>Building the Project

####<a name="hbase-version"></a>Specifying Your Hadoop/HBase Version

Prior to building the project, you need to specify the version of Hadoop and/or HBase installed (if applicable). Edit the `<properties>` section of the *aperture-tiles/pom.xml* build file to select the valid settings for your version. See the comments in the file for more details.
 
If you plan to run Apache Spark only in standalone mode on single machine, you can skip this step.

####<a name="compiling"></a>Compiling the Aperture Tiles Projects

Before you compile the Aperture Tiles source code, you must install the ApertureJS project. Run the `mvn install` command in the `aperture` folder in the aperture subdirectory of the ApertureJS root directory.

Once the ApertureJS installation is complete, run the `mvn install` command again, this time in the root Aperture Tiles directory. This will compile all the project components and install .jar files for each project into your local maven repostitory on your build machine.

##<a name="next-steps"></a>Next Steps

See the [Tile Generation](../generation) or [Configuration](../configuration) topics for specifics on generating tile sets from raw data and configuring a tile server and client to create a tile-based visual analytic application.

