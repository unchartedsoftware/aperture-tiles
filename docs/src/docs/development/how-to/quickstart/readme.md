---
section: Docs
subsection: Development
chapter: How-To
topic: Quick Start
permalink: docs/development/how-to/quickstart/
layout: submenu
---

# Quick Start Guide #

This guide, which provides a short tutorial on the process of creating and configuring an Aperture Tiles project, covers the following topics:

1. Generating a sample data set to analyze
2. Tiling and storing the sample data set
3. Configuring a client to serve and display the tiles in a web browser

At the end of this guide, you will have an example Aperture Tiles application that displays the points in an Julia set fractal dataset on an X/Y plot with five zoom levels.

<img src="../../../../img/julia-set.png" class="screenshot" alt="Aperture Tiles Julia Set Project" />

## <a name="prerequisites"></a> Prerequisites ##

To begin this Quick Start example, you must perform the following steps:

1. Download and install the necessary [third-party tools](#third-party-tools).
2. Download and install the [Aperture Tiles Packaged Distribution](#aperture-tiles-utilities).
3. Generate the [Julia set data](#julia-set-data-generation), from which you will later create a set of tiles that will be used in your Aperture Tiles project.

### <a name="third-party-tools"></a> Third-Party Tools ###

Aperture Tiles requires the following third-party tools on your local system:

<div class="props">
    <table class="summaryTable" width="100%">
        <thead>
            <th scope="col" width="20%">Component</th>
            <th scope="col" width="30%">Required</th>
            <th scope="col" width="50%">Notes</th>
        </thead>
        <tbody>
            <tr>
                <td style="vertical-align: text-top" class="description">Operating System</td>
                <td style="vertical-align: text-top" class="description">Linux or OS X</td>
                <td style="vertical-align: text-top" class="description">Windows support available with <a href="https://cygwin.com/">Cygwin</a> or DOS command prompt; precludes the use of Hadoop/HBase.</td>
            </tr>
            <tr>
                <td style="vertical-align: text-top" class="description">Cluster Computing Framework</td>
                <td style="vertical-align: text-top" class="description"><a href="http://spark.incubator.apache.org//">Apache Spark</a><br>v1.0.0+</td>
                <td style="vertical-align: text-top" class="description">The latest version of Spark may cause class path issues if you compile from source code. We recommend using a pre-built Spark package.</td>
            </tr>
        </tbody>
    </table>
</div>

If you intend to work only with datasets that are small enough to fit in the memory of a single machine or if wait times are not an issue, skip ahead to the [Packaged Distribution](#aperture-tiles-utilities) section.

Otherwise, we recommend you also install the following tools, which enable Aperture Tiles to work with particularly large datasets.

<div class="props">
    <table class="summaryTable" width="100%">
        <thead>
            <th scope="col" width="20%">Component</th>
            <th scope="col" width="30%">Required</th>
            <th scope="col" width="50%">Notes</th>
        </thead>
        <tbody>
            <tr>
                <td style="vertical-align: text-top" class="description">Cluster Computing Framework</td>
                <td style="vertical-align: text-top" class="description">
                    A <a href="http://hadoop.apache.org/">Hadoop</a> distribution:
                    <ul class="table">
                        <li><a href="http://www.cloudera.com/content/cloudera/en/products-and-services/cdh.html">Cloudera</a> v4.6 (<em>recommended)</em></li>
                        <li><a href="http://hadoop.apache.org/docs/r1.2.1/index.html">Apache</a></li>
                        <li><a href="http://www.mapr.com/products/apache-hadoop">MapR</a></li>
                        <li><a href="http://hortonworks.com/">HortonWorks</a></li>
                    </ul>
                </td>
                <td style="vertical-align: text-top" class="description">Some Hadoop distributions automatically install Apache Spark. Upgrade to v1.0.0+ if the installation is older.</td>
            </tr>
        </tbody>
    </table>
</div>

### <a name="aperture-tiles-utilities"></a> Aperture Tiles Packaged Distribution ###

The Aperture Tiles packaged distributions enable you to create the Julia set data and provision the web application for this example.

**NOTE**: The full Aperture Tiles [source code](https://github.com/unchartedsoftware/aperture-tiles/tree/master) is not required for this example. For information on full installations of Aperture Tiles, see the [Installation](../installation/) topic.

<h6 class="procedure">To download the packaged distributions</h6>

1. Download and save the following files:
    - [Tile Generator](../../../../download/#tile-generator): Creates the Julia set data and generates a set of tiles
    - [Tile Quick Start Application](../../../../download/#tile-quick-start-application): Serves as an example application that you can quickly deploy after minimal modification
2. Extract the contents of each file.

### <a name="julia-set-data-generation"></a> Julia Set Data Generation ###

<h6 class="procedure">To use the Tile Generator utility to create the Julia set data</h6>

1. Execute the standard [spark-submit](http://spark.apache.org/docs/1.0.0/submitting-applications.html) script using the following command, changing the **output** URI to specify where you want to save the data (HDFS or local file system):

    ```bash
    $SPARK_HOME/bin/spark-submit --class com.oculusinfo.tilegen.examples.datagen
    .JuliaSetGenerator --master local[2] lib/tile-generation-assembly.jar -real 
    -0.8 -imag 0.156 -output datasets/julia -partitions 5 -samples 10000000
    ```

    **NOTE**: The remaining flags pass in the correct program main class, dataset limits, number of output files (5) and total number of data points (10M).

2. Check your output location for 5 part files (**part-00000** to **part-00004**) of roughly equal size (2M records and ~88 MB). These files contain the tab-delimited points in the Julia set.

**NOTE**: For typical Aperture Tiles projects, these steps are unnecessary. You will instead begin with your own custom data set.

## <a name="tile-generation"></a> Tile Generation ##

The first step in building any Aperture Tiles project is to create a set of [Avro](http://avro.apache.org/) tiles that aggregate your source data across the plot/map and its various zoom levels.

For delimited numeric data sources like the Julia set, we use the CSVBinner tool to create these tiles. The CSVBinner tool requires two types of input:

- [Base properties file](#base-property-file-configuration), which describes the general characteristics of your data
- [Tiling properties files](#tiling-property-file-configuration), each of which describes a specific attribute you want to plot and the number of zoom levels

### <a name="base-property-file-configuration"></a> Base Property File Configuration ###

A preconfigured base properties file is available in the Tile Generator utility. You only need to edit this file if you intend to save your Avro tiles to HBase. Otherwise, you can skip ahead to the [execution](#execution) of the tile generation job.

**NOTE**: For a typical Aperture Tiles project, you will need to edit the additional properties files to define the types of fields in your source data. For more information on these properties, see the [Tile Generation](../generation/) topic.

<h6 class="procedure">To edit the base properties file</h6>

1. Open the **julia-base.bd** file in the Tile Generator *examples/* folder.
2. Edit the **oculus.binning.source.location** property to specify the location of your Julia set data:
    - For the local system: */data/julia*
    - For HDFS: *hdfs://hadoop.example.com/data/julia*
3. Edit the following general output properties:
    <div class="props">
        <table class="summaryTable" width="100%">
            <thead>
                <th scope="col" width="20%">Property</th>
                <th scope="col" width="80%">Description</th>
            </thead>
            <tbody>
                <tr>
                    <td class="property">oculus.tileio.type</td>
                    <td class="description">Specify whether the tiles should be saved locally (<em>file</em>) or to HBase (<em>hbase</em>). Local tile IO is supported only for standalone Spark installations.</td>
                </tr>
                <tr>
                    <td class="property">oculus.binning.name</td>
                    <td class="description">Specify the name of the output tile set. If you are writing to a file system, use a relative path instead of an absolute path. Use <em>julia</em> for this example.</td>
                </tr>
            </tbody>
        </table>
    </div>
4. If you are using Hadoop/HDFS and HBase, edit the following HBase connection details:
    <div class="props">
        <table class="summaryTable" width="100%">
            <thead>
                <th scope="col" width="20%">Property</th>
                <th scope="col" width="80%">Description</th>
            </thead>
            <tbody>
                <tr>
                    <td class="property">hbase.zookeeper.quorum</td>
                    <td class="description">Zookeeper quorum location needed to connect to HBase.</td>
                </tr>
                <tr>
                    <td class="property">hbase.zookeeper.port</td>
                    <td class="description">Port through which to connect to zookeeper. Typically defaults to <em>2181</em>.</td>
                </tr>
                <tr>
                    <td class="property">hbase.master</td>
                    <td class="description">Location of the HBase master to which to save the tiles.</td>
                </tr>
            </tbody>
        </table>
    </div>
5. Save the **julia-base.bd** file.

### <a name="tiling-property-file-configuration"></a> Tiling Property File Configuration ###

The Tile Generator utility also contains a tiling properties file (**julia-tiling.bd** in *examples/*). This file should not need to be edited.

**NOTE**: For a typical Aperture Tiles project, you will need to edit this file to define the layout of the map/plot on which to project your data. For more information on these properties, see the [Tile Generation](../generation/) topic.

### <a name="execution"></a> Execution ###

After you have edited the properties files, you can use the Tile Generator utility to create the Avro tile set.

<h6 class="procedure">To execute the tile generation job</h6>

1. Execute the standard **spark-submit** script again, invoking the CSVBinner and using the *-d* switch to pass your edited base properties file. Tiling properties files can be passed in without a switch.

    ```bash
    $SPARK_HOME/bin/spark-submit --class com.oculusinfo.tilegen.examples.apps
    .CSVBinner --master local[2] --driver-memory 1G lib/tile-generation-assembly.jar 
    -d examples/julia-base.bd examples/julia-tiling.bd
    ```
2. Check your output location (*julia.x.y.v*) for six subfolders, each corresponding to a zoom level (0, being the highest, through 5, being the lowest). Across all the folders, you should have a total of 1,365 Avro tile files.

## <a name="tile-server-configuration"></a> Tile Server Configuration ##

The Tile Server renders your generated tiles as layers in your Aperture Tiles visualization and passes them to the client. For this example, a preconfigured example server application has been provided as part of the Tile Quick Start Application ([tile-quickstart.war](../../../../download/#tile-quick-start-application)).

<h6 class="procedure">To make tiles on the local file system available to Tile Quick Start Application</h6>

1. Zip the *julia.x.y.v* directory produced during the Tile Generation stage.
2. Copy the ZIP file to the *WEB-INF/classes/* directory of the Tile Quick Start Application.

**NOTE**: For typical Aperture Tiles projects, you will need to edit the */WEB-INF/***web.xml** and *WEB-INF/classes/***tile.properties** files in the Tile Server. For more information on editing these files, see the [App Configuration](../configuration/) topic.

### Layer Properties ###

Layer properties (within the **tile-quickstart.war** at *WEB-INF/classes/layers/***julia-layer.json**) specify the layers that can be overlaid on your base map or plot. 

For this example, you only need to edit the layer properties file if you saved your Avro tiles to HBase. Otherwise, you can skip ahead to the configuration of the [Tile Client Application](#tile-client-application). 

<h6 class="procedure">To edit the layer properties</h6>

1. Access the *WEB-INF/classes/layers/***julia-layer.json** file.
2. Make sure the **id** property under the *private* node matches the name given to the HBase table containing your Avro tiles (*julia.x.y.v*).
3. Clear the existing attributes under the **pyramidio** node and add the following HBase connection details:
    <div class="props">
        <table class="summaryTable" width="100%">
            <thead>
                <th scope="col" width="20%">Property</th>
                <th scope="col" width="80%">Value</th>
            </thead>
            <tbody>
                <tr>
                    <td class="property">type</td>
                    <td class="value">hbase</td>
                </tr>
                <tr>
                    <td class="property">hbase.zookeeper.quorum</td>
                    <td class="description">Zookeeper quorum location needed to connect to HBase. For example:
                        <ul>
                            <li><em>my-zk-server1.example.com</em></li>
                            <li><em>my-zk-server2.example.com</em></li>
                            <li><em>my-zk-server3.example.com</em></li>
                        </ul>
                    </td>
                </tr>
                <tr>
                    <td class="property">hbase.zookeeper.port</td>
                    <td class="description">Port through which to connect to zookeeper. Typically defaults to <em>2181</em>.</td>
                </tr>
                <tr>
                    <td class="property">hbase.master</td>
                    <td class="description">Location of the HBase master in which the tiles are saved (e.g., <em>my-hbase-master.example.com:60000</em>)</td>
                </tr>
            </tbody>
        </table>
    </div>
4. Save the file.

For information on additional layer properties, see the *Layers* section of the [Configuration](../configuration/#layers) topic.

## <a name="tile-client-application"></a> Tile Client Application ##

For this example, a preconfigured example client application has been provided as part of the Tile Quick Start Application ([tile-quickstart.war](../../../../download/#tile-quick-start-application)). The client displays the base map or plot and any layers passed in from the server.

For information on map properties (e.g., for boundaries and axes), see the *Maps* section of the [Configuration](../configuration/#maps) topic. 

## <a name="deployment"></a> Deployment ##

<h6 class="procedure">To deploy your application</h6>

1. Copy the **tile-quickstart.war** to the *webapps/* directory of your web server (e.g., Apache Tomcat or Jetty).
2. Restart the server, if necessary
3. Access your application in web browser at <em>http://localhost:8080/julia-demo</em>.

The Julia set application data is plotted on an X/Y chart with six layers of zoom available.

## Next Steps ##

For a detailed description of the prerequisites and installation procedures for Aperture Tiles, see the [Installation](../installation/) topic.