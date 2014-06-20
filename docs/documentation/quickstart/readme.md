---
section: Documentation
subtitle: Quick Start
permalink: documentation/quickstart/index.html
layout: default
---

# Quick Start Guide

The following instructions walk you through the process of creating an Aperture Tiles project that displays the points in a Julia set fractal on an X/Y plot with five zoom levels.

![Aperture Tiles Julia Set Project](../../img/julia-set.png "Aperture Tiles Julia Set Project")

This Quick Start Guide covers the following processes:

1. Generating a sample data set to analyze
2. Tiling and storing the sample data set
3. Configuring a client to serve and display the tiles in a web client

##<a name="prerequisites"></a>Prerequisites

The Tile Generation scripts used in this example require the use of a Linux or OS X operating system. 

To create the Aperture Tiles Julia set project, you first need to install [Apache Spark](http://spark.incubator.apache.org/) version 0.9.0 or greater (version 1.0.0 recommended). Spark is a distributed cluster computing framework on which Aperture Tiles builds to enable fast data and tile generation at scale.

If you later intend to create Aperture Tiles projects using particularly large data sets, we recommend you also install each of the following tools:

- Your preferred flavor of Hadoop/HDFS ([Cloudera](http://www.cloudera.com/content/cloudera/en/products/cdh.html) version 4.6 recommended, though [Apache](http://hadoop.apache.org/docs/r1.2.1/index.html), [MapR](http://www.mapr.com/products/apache-hadoop) and  [HortonWorks](http://hortonworks.com/), etc. can also be used), which allows you to configure a cluster of machines across which you can distribute Aperture Tiles analytic jobs 
- [Apache HBase](http://hbase.apache.org/), which acts as a data store for your Hadoop/HDFS  cluster

Otherwise, if your data set is sufficiently small (i.e., it can fit in the memory of a single machine) or if wait times are not an issue, you can simply install and run Spark locally.

###<a name="aperture-tiles-utilities"></a>Aperture Tiles Utilities

Save the following Aperture Tiles utilities available on the [Download](../../download/) section of this website. You will use these utilities to create the Julia set data and provision the example Aperture Tiles project.

- [Tile Generator](../../download/tile-generator.zip): Enables you to create the Julia set data and generate a layered set of tiles that can be viewed in the Tile Client template
- [Tile Client Template](../../download/tile-client-template.zip): An example Tile Client that you can quickly copy and deploy to your web server after minimal modification

The full Aperture Tiles source code, available for download from [GitHub](https://github.com/oculusinfo/aperture-tiles), is not required for this example. For information on full installations of Aperture Tiles, see the [Installation](../setup/) page.

###<a name="environment-variables"></a>Environment Variables
Set the following environment variables:

- `SPARK_HOME` - the location of the Spark installation`
- `SPARK_MEM` - the amount of memory to allocation to Spark`
- `MASTER` - the node on which the cluster is installed`

###<a name="julia-set-data-generation"></a>Julia Set Data Generation

For a typical Aperture Tiles project, you will work with your own custom data set. To avoid packaging a large example data set with Aperture Tiles, we have instead provided a simple data set generator. For this demonstration, you must use the provided Tile Generator utility to create the Julia set data.

1. Extract the contents of the [tile-generator.zip](../../download/tile-generator.zip), then browse to the Spark script (`/tile-generator/bin/spark-run.sh`) that has been provided to assist with running Tile Generation jobs on Spark. In the next step, you will use the script to generate the Julia set data.
2. Execute the Spark script using the following command, changing the output URI (HDFS or local file system) to specify the location in which you want to save the Julia set data. The rest of the flags pass in the correct classes, data set limits, number of output files (5) and total number of data points (10M) in the Julia set.

```
./spark-run.sh com.oculusinfo.tilegen.examples.datagen.JuliaSetGenerator -real -0.8 -imag 0.156 -output /data/julia-set -partitions 5 -samples 10000000
```

Check your output folder for five part files (`part-00000` to `part-00004`) of roughly equal size (2M records and ~88 MB). These files contain the tab-delimited points in the Julia set you will use Aperture Tiles to visualize.

##<a name="tile-generation"></a>Tile Generation

The first step in building any Aperture Tiles project is creating a set of AVRO tiles that aggregate your source data across the plot/map and its various zoom levels.

For delimited numeric data sources like the Julia set, the included CSVBinner tool can create these tiles. The CSVBinner tool requires two types of input:

- The **base properties** file, which describes the general characteristics of your data
- The **tiling properties** files, each of which describes a specific attribute you want to plot and the number of zoom levels

###<a name="base-property-file-configuration"></a>Base Property File Configuration

Access the **julia-base.bd** file in your `tile-generator/examples` folder and edit the properties in the following sections.

Note that for a typical Aperture Tiles project, you will need to edit additional properties to define the types of fields in your source data. For more information on these additional properties, see the [Tile Generation](../generation/) topic on this website. 

####<a name="spark-connnection"></a>Spark Connection Details

These properties specify the location of your Spark installation.

```
spark
	URI of the Spark master. Set to "local" for standalone Spark installations.
sparkhome
	File system location of Spark. Defaults to the value of the SPARK_HOME
	environment variable.
```

####<a name="general-output"></a>General Output Properties

These properties specify the location of your Julia set data and where to save the generated tiles.

```
oculus.tileio.type
	Specify whether the tiles should be saved locally (file) or to HBase
	(hbase). Note that local tile IO is supported only for standalone Spark
	installations.
oculus.binning.source.location
	Path of the source data files in your local file system
	(tile-generator/bin/julia) or HDFS.
```

####<a name="hbase-connection"></a>HBase Connection Details (Optional)

These properties should only be included if you are using Hadoop/HDFS and HBase.

```
hbase.zookeeper.quorum
	Zookeeper quorum location needed to connect to HBase
hbase.zookeeper.port
	Port through which to connect to zookeeper. Typically defaults to 2181. 
hbase.master
	Location of the HBase master to which to save the tiles
```

###<a name="tiling-property-file-configuration"></a>Tiling Property File Configuration

Access the **julia-tiling.bd** file in your `tile-generator/examples` folder and edit the `oculus.binning.name` to point to the name of the output tile set. Use `julia` for this example.

Note that for a typical Aperture Tiles project, you will need to edit additional properties to define the layout of the map/plot on which to project your data. For more information on these additional properties, see the [Tile Generation](../generation/) topic on this website.

###<a name="execution"></a>Execution

When you have configured all of the required properties, execute the Spark script (`/tile-generator/bin/spark-run.sh`) again. This time you will invoke the CSVBinner and use the `-d` switch to pass your edited base and tiling property files.

```
tile-generator/bin/spark-run.sh com.oculusinfo.tilegen.examples.apps.CSVBinner -d tile-generator/examples/julia-base.bd tile-generator/examples/julia-tiling.bd
```

When the tile generation is complete, you should have a folder containing six subfolders (0, being the highest, through 5, being the lowest), each of which corresponds to a layer in your project. Across all the folders, you should have a total of 1,365 AVRO tile files.

Note that for this example, the tile folder will be named `julia.x.y.v`. The output folder is always named using the the following values in the **julia.bd** file:

```
<oculus.binning.name>.<oculus.binning.xField>.<oculus.binning.yField>.<oculus.binning.valueField>
``` 

##<a name="tile-server-configuration"></a>Tile Server Configuration

For the purposes of this demonstration, a preconfigured example server application has been provided as part of the [tile-client-template.zip](../../download/Tile Client Template) utility.

For typical Aperture Tiles projects, you will need to edit the **web.xml**  and **tile.properties** files in this directory. For more information on editing these files, see the [Tile Generation](../generation/) topic on this website.

##<a name="tile-client-configuration"></a>Tile Client Configuration

To configure the Tile Client application to display the AVRO files containing your source data, you must edit two types of configuration files:

- Map Properties (`aperture-tiles/tile-client-template/target/tile-client-template/WEB-INF/classes/maps`), which specifies the attributes of the base map or plot on which your data is displayed
- Layer Properties (`aperture-tiles/tile-client-template/target/tile-client-template/WEB-INF/classes/layers`), each of which specifies the attributes of a single layer that can be overlaid on your base map or plot

Both files are available in the [tile-client-template.zip](../../download/Tile Client Template) utility. Extract the contents of the file to access them.

###<a name="map-properties"></a>Map Properties

To edit the map properties for your project:

1. Access the **crossplot-maps.json.example** file in `tile-client-template/WEB-INF/classes/maps`, remove the **.example** extension and open the file for editing.
2. In the `PyramidConfig` section, specify the minimum and maximum values for the X (`minX` and `maxX`) and Y (`min` and `maxY`) axes. Points in the Julia set will range from **-2** to **2** along both axes.
3. In the `MapConfig` > `options` section, set the `numZoomLevels` to **6**, as this is the number of layers you created when generating the Julia set tiles.
4. Save the file.

Note that for typical Aperture Tiles projects, you can also use this file to configure other base map/plot properties, such as:

- Map type (geographic or X/Y plot)
- Axis names
- Unit specifications
- Marker styles

###<a name="layer-properties"></a>Layer Properties

To edit the layer properties for you project:

1. Access the **crossplot-layers.json.example** file in `tile-client-template/WEB-INF/classes/layers`, remove the **.example** extension and open the file for editing.
2. Edit the `id` property so it matches the name given to the folder in which your AVRO tiles were generated. For the Julia set example, this will be **julia.x.y.v**.
3. In the `pyramid` section, specify the minimum and maximum values for the X (`minX` and `maxX`) and Y (`min` and `maxY`) axes. Make sure the values you specify here match the range you specified in the map properties.
4. If your AVRO tiles are saved to your local machine, add or edit the following values in the `data` section:
	- `type`: Enter **file**
	- `root.path`: Specify the path to which you generated the AVRO tiles.  
5. If your AVRO tiles are saved to HBase, add or edit the following values in the `data` section:
	- `hbase.zookeeper.quorum`: Zookeeper quorum location needed 
   to connect to HBase. 
	- `hbase.zookeeper.port`: Port through which to connect 
   to zookeeper. Typically defaults to `2181`. 
	- `hbase.master`: Location of the HBase master in 
   which the tiles are saved. 
6. Save the file.

Note that for typical Aperture Tiles projects, you can also use this file to configure other layer properties, such as:

- Color scale applied to data points
- Layer opacity
- Tile renderer

##<a name="deployment"></a>Deployment

Once you have finished configuring the map and layer properties, copy the `tile-client-template/` folder to your Apache Tomcat or Jetty server.

Access the `/tile-client-template/` directory on the server from any web browser to to view the Julia set data plotted on an X/Y chart with six layers of zoom available.