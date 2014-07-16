---
section: Docs
subtitle: Quick Start
permalink: docs/quickstart/index.html
layout: submenu
---

# Quick Start Guide

The following guide provides a short tutorial that walks you through the process of creating and configuring an Aperture Tiles project. This Quick Start Guide covers the following processes:

1. Generating a sample data set to analyze
2. Tiling and storing the sample data set
3. Configuring a client to serve and display the tiles in a web client

At the end of this guide you will have successfully created an example Aperture Tiles project that displays the points in an example Julia set fractal dataset on an X/Y plot with five zoom levels.

<img src="../../img/julia-set.png" class="screenshot" alt="Aperture Tiles Julia Set Project"></img>

##<a name="prerequisites"></a>Prerequisites

There are two ways you can begin this Quick Start example:

1. You can download a [virtual machine](#virtual-machine) (VM) preloaded with the third-party tools needed to perform the tile generation and web application deployment.
2. You can manually install all of the third-party tools on your [local system](#local-system).

Once you have a machine configured with all of the third-party prerequisites, you must perform the following steps:

1. Download and install the [Aperture Tiles Packaged Distribution](#aperture-tiles-utilities).
2. Set the [Spark environment variables](#environment-variables).
3. Generate the [Julia set data](#julia-set-data-generation), from which you will later create a set of tiles that will be used in your Aperture Tiles project.

###<a name="virtual-machine"></a>Virtual Machine

We have created a virtual machine that has been preconfigured with the third-party tools needed to walk through this Quick Start example. To use this virtual machine:

1. Download and install [Oracle VM VirtualBox](https://www.virtualbox.org/).
2. Save the virtual machine on the [Download](../../download/) page to your local system.
3. Open Oracle VM VirtualBox and select **Import Appliance** from the **File** menu.
4. Browse to the location of the virtual machine you downloaded and click **Open**.
5. Click **Next** on the Appliance to import dialog.
6. Click **Import** on the Appliance settings dialog.

You can access your virtual machine in two ways:

- Directly through the Oracle VM VirtualBox Manager
- Via ssh with the following command, using the username **vagrant** and password **vagrant**:

```
ssh -p 2222 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no vagrant@localhost
```

Access port 8080 (`http://{vm-machine-name}:8080/`) on your virtual machine (which is forwarded to port 8888 (`http://localhost:8888/` on your local machine) in any web browser for a brief description of the virtual machine's configuration and links to preloaded demonstrations.

When you are ready to proceed, skip to the [Aperture Tiles Packaged Distribution](#aperture-tiles-utilities) section.

###<a name="local-system"></a>Local System

The Tile Generation scripts used in this example require the use of a Linux or OS X operating system. 

To create the Aperture Tiles Julia set project, you first need to install [Apache Spark](http://spark.incubator.apache.org/) version 0.9.0 or greater (version 1.0.0 recommended). Spark is a distributed cluster computing framework on which Aperture Tiles builds to enable fast data and tile generation at scale.  NOTE: In the latest version of Spark, class path issues may arise if you compile Spark from the source code. For this reason, we recommend using one of the pre-built Spark packages.

If you later intend to create Aperture Tiles projects using particularly large data sets, we recommend you also install each of the following tools:

- Your preferred flavor of Hadoop/HDFS ([Cloudera](http://www.cloudera.com/content/cloudera/en/products/cdh.html) version 4.6 recommended, though other flavors such as [Apache](http://hadoop.apache.org/docs/r1.2.1/index.html), [MapR](http://www.mapr.com/products/apache-hadoop) and [HortonWorks](http://hortonworks.com/) may work), which allows you to configure a cluster of machines across which you can distribute Aperture Tiles analytic jobs 
- [Apache HBase](http://hbase.apache.org/), which acts as a data store for your Hadoop/HDFS cluster

Otherwise, if your data set is sufficiently small (i.e., it can fit in the memory of a single machine) or if wait times are not an issue, you can simply install and run Spark locally.

###<a name="aperture-tiles-utilities"></a>Aperture Tiles Packaged Distribution

Save the following Aperture Tiles distribution available on the [Download](../../download/) section of this website. You will use these utilities to create the Julia set data and provision the example Aperture Tiles project.

- [Tile Generator](http://assets.oculusinfo.com/tiles/downloads/tile-generator-0.3-dist.zip): Enables you to create the Julia set data and generate a set of tiles that can be viewed in the Tile Client template
- [Tile Client Template](http://assets.oculusinfo.com/tiles/downloads/tile-server-0.3-dist.zip): An example Tile Client that you can quickly copy and deploy to your web server after minimal modification

The full Aperture Tiles source code, available for download from [GitHub](https://github.com/oculusinfo/aperture-tiles), is not required for this example. For information on full installations of Aperture Tiles, see the [Installation](../installation/) page.

###<a name="environment-variables"></a>Environment Variables
Set the following environment variables:

- `SPARK_HOME` - the location of the Spark installation
- `SPARK_MEM` - the amount of memory to allocation to Spark
- `MASTER` - the node on which the cluster is installed (set to `local` for running Spark on a single machine)

###<a name="julia-set-data-generation"></a>Julia Set Data Generation

For a typical Aperture Tiles project, you will work with your own custom data set. To avoid packaging a large example data set with Aperture Tiles, we have instead provided a simple data set generator. For this demonstration, you will use the provided Tile Generator utility to create the Julia set data.

1. Extract the contents of the [tile-generator.zip](http://assets.oculusinfo.com/tiles/downloads/tile-generator-0.3-dist.zip), then browse to the Spark script (`/tile-generator/bin/spark-run.sh`) that has been provided to assist with running Tile Generation jobs on Spark. In the next step, you will use the script to generate the Julia set data.
2. Execute the Spark script using the following command, changing the output URI (HDFS or local file system) to specify the location in which you want to save the Julia set data. 
	
	The rest of the flags pass in the correct program main class, data set limits, number of output files (5) and total number of data points (10M) to generate in the Julia set.

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
	(hbase). Local tile IO is supported only for standalone Spark installations.

	NOTE: This parameter is not currently in the example .bd file provided for
	this demo. When this parameter is absent, the tile generator automatically
	writes to HBase. To write to the local filesystem, manually add this
	parameter to the .bd file and set its value to "file".

oculus.binning.source.location
	Path of the source data files in your local file system
	(ex: /data/julia) or HDFS path (ex: hdfs://hadoop.example.com/data/julia).
```

####<a name="hbase-connection"></a>HBase Connection Details (Optional)

These properties should only be included if you are using Hadoop/HDFS and HBase. Note that these optional components must be used if you want to run the tile generation job on a multi-computer cluster. 

```
hbase.zookeeper.quorum
	Zookeeper quorum location needed to connect to HBase.

hbase.zookeeper.port
	Port through which to connect to zookeeper. Typically defaults to 2181.

hbase.master
	Location of the HBase master to which to save the tiles.
```

###<a name="tiling-property-file-configuration"></a>Tiling Property File Configuration

Access the **julia-tiling.bd** file in your `tile-generator/examples` folder and edit the `oculus.binning.name` to specify the name of the output tile set. If you are writing to a file system, use a relative path instead of an absolute path. Use `julia` for this example.

Note that for a typical Aperture Tiles project, you will need to edit additional properties to define the layout of the map/plot on which to project your data. For more information on these additional properties, see the [Tile Generation](../generation/) topic on this website.

###<a name="execution"></a>Execution

When you have configured all of the required properties, execute the Spark script (`/tile-generator/bin/spark-run.sh`) again. This time you will invoke the CSVBinner and use the `-d` switch to pass your edited base and tiling property files.

```
tile-generator/bin/spark-run.sh com.oculusinfo.tilegen.examples.apps.CSVBinner -d tile-generator/examples/julia-base.bd tile-generator/examples/julia-tiling.bd
```

When the tile generation is complete, you should have a folder containing six subfolders (0, being the highest, through 5, being the lowest), each of which corresponds to a zoom level in your project. Across all the folders, you should have a total of 1,365 AVRO tile files.

Note that for this example, the tile folder will be named `julia.x.y.v`. The output folder is always named using the the following values in the **julia.bd** file:

```
[<oculus.binning.prefix>.]<oculus.binning.name>.<oculus.binning.xField>.<oculus.binning.yField>.<oculus.binning.valueField>
``` 

Note that the `oculus.binning.prefix` value is only included if you set it in the property file. This is useful if you want to run a second tile generation without overwriting the already generated version.

##<a name="tile-server-configuration"></a>Tile Server Configuration

For the purposes of this demonstration, a preconfigured example server application has been provided as part of the [tile-client-template.zip](http://assets.oculusinfo.com/tiles/downloads/tile-server-0.3-dist.zip) distribution.

For typical Aperture Tiles projects, you will need to edit the **web.xml** and **tile.properties** files in this directory. For more information on editing these files, see the [Tile Generation](../generation/) topic on this website.

##<a name="tile-client-configuration"></a>Tile Client Configuration

To configure the Tile Client application to display the AVRO files containing your source data, you must edit two types of configuration files:

- Map Properties (within the tile-client-template.zip at `WEB-INF/classes/maps` or within the source at `tile-client-template/src/main/resources/maps`), which specifies the attributes of the base map or plot on which your data is displayed. To include more than one map in your project, create a separate Map Properties file for each.
- Layer Properties (within the tile-client-template.zip at `WEB-INF/classes/layers` or within the source at `tile-client-template/src/main/resources/layers`), which specifies the layers that can be overlaid on your base map or plot.

Both files are available in the [tile-client-template.zip](http://assets.oculusinfo.com/tiles/downloads/tile-server-0.3-dist.zip) distribution. Extract the contents of the file to access them.

###<a name="map-properties"></a>Map Properties

To edit the map properties for your project:

1. Access the **crossplot-maps.json.example** file in `tile-client-template/WEB-INF/classes/maps`, remove the **.example** extension and delete the example geographic json file in the same directory. It is not needed for this example.
2. Open the crossplot json file for editing. In the `PyramidConfig` section, specify the minimum and maximum values for the X (`minX` and `maxX`) and Y (`min` and `maxY`) axes. Points in the Julia set will range from **-2** to **2** along both axes.
3. In the `AxisConfig` section for both axes, it is useful to set the `intervalSpec` `type` to **percentage** and the *increment* value to **20**. 
4. In the `MapConfig` > `options` section, set the `numZoomLevels` to **6**, as this is the number of zoom levels you created when generating the Julia set tiles.
5. Save the file.

Note that for typical Aperture Tiles projects, you can also use this file to configure other base map/plot properties, such as:

- Map type (geographic or X/Y plot)
- Axis names
- Unit specifications
- Marker styles

###<a name="layer-properties"></a>Layer Properties

To edit the layer properties for your project:

1. Access the **crossplot-layers.json.example** file in `tile-client-template/WEB-INF/classes/layers`, remove the **.example** extension and delete the example geographic json file in the same directory. It is not needed for this example.
2. Open the crossplot json file for editing. Edit the children `id` property (*not* the layer `id` property) so it matches the name given to the directory (file system directory or HBase table name) in which your AVRO tiles were generated. For the Julia set example, this will be **julia.x.y.v**.
3. In the `pyramid` section, specify the minimum and maximum values for the X (`minX` and `maxX`) and Y (`min` and `maxY`) axes. Make sure the values you specify here match the range you specified in the map properties.
4. If your AVRO tiles are saved to your local machine, add or edit the following values in the `data` section:
	- `type`: Enter **file**
	- `root.path`: Specify the *root* path to which you generated the AVRO tiles. Note that the `id` you specified in step 2 is the leaf folder that contains the AVRO tiles. Set the `root.path` to the folder above that (e.g., */data/tiles/*).
5. If your AVRO tiles are saved to HBase, add or edit the following values in the `data` section:
	- `type`: Enter *hbase*
	- `hbase.zookeeper.quorum`: Zookeeper quorum location needed 
   to connect to HBase (ex: `my-zk-server1.example.com, my-zk-server2.example.com, my-zk-server3.example.com`). 
	- `hbase.zookeeper.port`: Port through which to connect 
   to zookeeper. Typically defaults to `2181`. 
	- `hbase.master`: Location of the HBase master in 
   which the tiles are saved (ex: `my-hbase-master.example.com:60000`). 
6. Save the file.

Note that for typical Aperture Tiles projects, you can also use this file to configure other layer properties, such as:

- Color scale applied to data points
- Layer opacity
- Tile renderer

##<a name="deployment"></a>Deployment

Once you have finished configuring the map and layer properties, copy the `tile-client-template/` folder to your web server's (e.g., Apache Tomcat or Jetty) webapps directory. If you are using the virtual machine provided on the [Download](../../download/) page, copy the entire directory to the `/opt/jetty/webapps` folder on the VM.

Access the `/tile-client-template` web directory on the server from any web browser to view the Julia set data plotted on an X/Y chart with six layers of zoom available. For example if your server were `www.example.com`, the URL would be `http://www.example.com/tile-client-template`. If you are using the VM, browse to `http://localhost:8888/tile-client-template/` on your local machine or `http://{vm-machine-name}:8080/tile-client-template/` on the virtual machine.