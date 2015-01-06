---
section: Docs
subtitle: Development
chapter: Quick Start
permalink: docs/development/quickstart/index.html
layout: submenu
---

# Quick Start Guide #

The following guide provides a short tutorial that walks you through the process of creating and configuring an Aperture Tiles project. This Quick Start Guide covers the following processes:

1. Generating a sample data set to analyze
2. Tiling and storing the sample data set
3. Configuring a client to serve and display the tiles in a web client

At the end of this guide you will have successfully created an example Aperture Tiles project that displays the points in an example Julia set fractal dataset on an X/Y plot with five zoom levels.

<img src="../../../img/julia-set.png" class="screenshot" alt="Aperture Tiles Julia Set Project" />

## <a name="prerequisites"></a> Prerequisites ##

To begin this Quick Start example, you must perform the following steps:

1. Download and install the necessary [third-party tools](#third-party-tools).
2. Download and install the [Aperture Tiles Packaged Distribution](#aperture-tiles-utilities).
3. Generate the [Julia set data](#julia-set-data-generation), from which you will later create a set of tiles that will be used in your Aperture Tiles project.

### <a name="third-party-tools"></a> Third-Party Tools ###

Aperture Tiles requires the following third-party tools on your local system:

- **Operating System**: The Tile Generation scripts used in this example require the use of a Linux or OS X operating system. <p class="list-paragraph">Windows support is available through [Cygwin](https://cygwin.com/) or the DOS command prompt, but precludes the use of Hadoop/HBase.</p>
- **Cluster Computing**: To create the Aperture Tiles Julia set project, you first need to install [Apache Spark](http://spark.incubator.apache.org/) version 1.0.0 or greater. Spark is a the distributed framework on which Aperture Tiles builds to enable fast data and tile generation at scale.  NOTE: In the latest version of Spark, class path issues may arise if you compile Spark from the source code. For this reason, we recommend using one of the pre-built Spark packages.

If you later intend to create Aperture Tiles projects using particularly large data sets, we recommend you also install each of the following tools:

- Your preferred flavor of Hadoop/HDFS ([Cloudera](http://www.cloudera.com/content/cloudera/en/products/cdh.html) version 4.6 recommended, though other flavors such as [Apache](http://hadoop.apache.org/docs/r1.2.1/index.html), [MapR](http://www.mapr.com/products/apache-hadoop) and [HortonWorks](http://hortonworks.com/) may work), which allows you to configure a cluster of machines across which you can distribute Aperture Tiles analytic jobs.<p class="list-paragraph">NOTE: Some cluster computing software may automatically install Apache Spark. If the automatically installed version is older than 1.0.0, you must upgrade to 1.0.0 or greater.</a>
- [Apache HBase](http://hbase.apache.org/), which acts as a data store for your Hadoop/HDFS cluster

Otherwise, if your data set is sufficiently small (i.e., it can fit in the memory of a single machine) or if wait times are not an issue, you can simply install and run Spark locally.

### <a name="aperture-tiles-utilities"></a> Aperture Tiles Packaged Distribution ###

Save the following Aperture Tiles distributions available on the [Download](../../../download/) section of this website. You will use these utilities to create the Julia set data and provision the example Aperture Tiles project.

- [Tile Generator](../../../download/#tile-generator): Enables you to create the Julia set data and generate a set of tiles that can be viewed in the Tile Quick Start template
- [Tile Quick Start Template](../../../download/#tile-quick-start-template): An example Tile Client that you can quickly copy and deploy to your web server after minimal modification

The full Aperture Tiles source code, available for download from [GitHub](https://github.com/oculusinfo/aperture-tiles/tree/master), is not required for this example. For information on full installations of Aperture Tiles, see the [Installation](../installation/) page.

### <a name="julia-set-data-generation"></a> Julia Set Data Generation ###

For a typical Aperture Tiles project, you will work with your own custom data set. To avoid packaging a large example data set with Aperture Tiles, we have instead provided a simple data set generator. For this demonstration, you will use the provided Tile Generator utility to create the Julia set data.

1. Extract the contents of the [tile-generator.zip](../../../download/#tile-generator).
2. Execute the standard [spark-submit](http://spark.apache.org/docs/1.0.0/submitting-applications.html) script using the following command, changing the output URI (HDFS or local file system) to specify the location in which you want to save the Julia set data. <p class="list-paragraph">The rest of the flags pass in the correct program main class, data set limits, number of output files (5) and total number of data points (10M) to generate in the Julia set.</p>

```bash
$SPARK_HOME/bin/spark-submit --class com.oculusinfo.tilegen.examples.datagen
.JuliaSetGenerator --master local[2] lib/tile-generation-assembly.jar -real 
-0.8 -imag 0.156 -output datasets/julia -partitions 5 -samples 10000000
```

Check your output folder for 5 part files (`part-00000` to `part-00004`) of roughly equal size (2M records and ~88 MB). These files contain the tab-delimited points in the Julia set you will use Aperture Tiles to visualize.

## <a name="tile-generation"></a> Tile Generation ##

The first step in building any Aperture Tiles project is creating a set of Avro tiles that aggregate your source data across the plot/map and its various zoom levels.

For delimited numeric data sources like the Julia set, the included CSVBinner tool can create these tiles. The CSVBinner tool requires two types of input:

- The **base properties** file, which describes the general characteristics of your data
- The **tiling properties** files, each of which describes a specific attribute you want to plot and the number of zoom levels

### <a name="base-property-file-configuration"></a> Base Property File Configuration ###

Access the **julia-base.bd** file in your *tile-generator/examples* folder and edit the properties in the following sections.

Note that for a typical Aperture Tiles project, you will need to edit additional properties to define the types of fields in your source data. For more information on these additional properties, see the [Tile Generation](../generation/) topic on this website. 

#### <a name="general-input"></a> General Input Properties ####

These properties specify the location of your Julia set data.

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">				
				<dt>oculus.binning.source.location</dt>
				<dd>Path of the source data files in your local file system	(ex: /data/julia) or HDFS path (ex: hdfs://hadoop.example.com/data/julia).</dd>
			</dl>
		</ul>
	</div>
</div>

#### <a name="general-output"></a> General Output Properties ####

These properties specify where to save the generated tiles.

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>oculus.tileio.type</dt>
				<dd>Specify whether the tiles should be saved locally (file) or to HBase (hbase). Local tile IO is supported only for standalone Spark installations.</dd>

				<dt>oculus.binning.name</dt>
				<dd>Specify the name of the output tile set. If you are writing to a file system, use a relative path instead of an absolute path. Use <em>julia</em> for this example.</dd>
								
			</dl>
		</ul>
	</div>
</div>

#### <a name="hbase-connection"></a> HBase Connection Details (Optional) ####

These properties should only be included if you are using Hadoop/HDFS and HBase. Note that these optional components must be used if you want to run the tile generation job on a multi-computer cluster. 

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>
					<b>hbase.zookeeper.quorum</b>
				</dt>
				<dd>Zookeeper quorum location needed to connect to HBase.</dd>
				
				<dt>
					<b>hbase.zookeeper.port</b>
				</dt>
				<dd>Port through which to connect to zookeeper. Typically defaults to 2181.</dd>
				
				<dt>
					<b>hbase.master</b>
				</dt>
				<dd>Location of the HBase master to which to save the tiles.</dd>
			</dl>
		</ul>
	</div>
</div>

### <a name="tiling-property-file-configuration"></a> Tiling Property File Configuration ###

The **julia-tiling.bd** file in your *tile-generator/examples* folder should not need to be edited. Note, however, that for a typical Aperture Tiles project, you will need to edit additional properties to define the layout of the map/plot on which to project your data. For more information on these additional properties, see the [Tile Generation](../generation/) topic on this website.

### <a name="execution"></a> Execution ###

When you have configured all of the required properties, execute the standard spark-submit script again. This time you will invoke the CSVBinner and use the `-d` switch to pass your edited base property files (tiling property files can be passed in without a switch).

```bash
$SPARK_HOME/bin/spark-submit --class com.oculusinfo.tilegen.examples.apps
.CSVBinner --master local[2] --driver-memory 1G lib/tile-generation-assembly.jar -d examples
/julia-base.bd examples/julia-tiling.bd
```

When the tile generation is complete, you should have a folder containing six subfolders (0, being the highest, through 5, being the lowest), each of which corresponds to a zoom level in your project. Across all the folders, you should have a total of 1,365 Avro tile files.

Note that for this example, the tile folder will be named `julia.x.y.v`. The output folder is always named using the the following values your property files:

```
[<oculus.binning.prefix>.]<oculus.binning.name>.<oculus.binning.xField>.
<oculus.binning.yField>.<oculus.binning.valueField>
``` 

Note that the `oculus.binning.prefix` value is only included if you set it in the property file. This is useful if you want to run a second tile generation without overwriting the already generated version.

## <a name="tile-server-configuration"></a> Tile Server Configuration ##

For the purposes of this demonstration, a preconfigured example server application has been provided as part of the [tile-quickstart.zip](../../../download/#tile-quick-start-template) distribution.

For typical Aperture Tiles projects, you will also need to edit the **web.xml** and **tile.properties** files in this directory. For more information on editing these files, see the [Configuration](../configuration/) topic on this website.

## <a name="tile-client-configuration"></a> Tile Client Configuration ##

To configure the Tile Client application to display the Avro files containing your source data, you must edit two types of properties, both of which are available in the [tile-quickstart.zip](../../../download/#tile-quick-start-template) distribution. Extract the contents of the file to access them.

- Map properties (within the tile-quickstart.zip at *src/main/webapp/***app.js**) specify the attributes of the base map or plot on which your data is displayed.
- Layer Properties (within the tile-quickstart.zip at *src/main/resources/layers/***julia-layer.json**) specify the layers that can be overlaid on your base map or plot.

### <a name="map-properties"></a> Map Properties ###

To edit the map properties for your project:

1. Open the **app.js** file in the root directory of the extracted Tile Quick Start template.
2. Note how the map instantiation specifies the boundaries of the Julia set values:

	```js
	map = new Map( "map", {
		pyramid : {
			type : "AreaOfInterest",
			minX : -2,
			maxX : 2,
			minY : -2,
			maxY : 2
	    },
	    options: numZoomLevels: 6
	});
	```

	Where:
	- `pyramid` contains definitions for:
		- The map `type` ('AreaOfInterest' denotes a crossplot)
		- The minimum and maximum values for the X (`minX` and `maxX`) and Y (`min` and `maxY`) axes. Points in the Julia set will range from **-2** to **2** along both axes.
	- `numZoomLevels` specifies number of zoom levels (6) you created when generating the Julia set tiles.

3. If desired, you can also instantiate the axes, which are not displayed by default. Replace the layer parser the with the following lines:

	```js
	var map,
	    axis0,
        axis1,
        baseLayer,
        serverLayer;
    ```

 	Add the following `axis0` and `axis1` configurations to adjust the axes for the Julia set data:

	```js
	axis0 = new tiles.Axis({
            position: 'bottom',
            title: 'X',
            isOpen: true,
            repeat: false,
            intervals: {
                type: 'percentage',
                increment: 20
            },
            units: {
                type: 'decimal'
            }
        });

        axis1 =  new tiles.Axis({
            position: 'left',
            title: 'Y',
            isOpen: true,
            repeat: false,
            intervals: {
                type: 'percentage',
                increment: 20
            },
            units: {
                type: 'decimal'
            }
        });
	```

	Where:
	- `title` specifies the axis label
	- `isOpen` indicates whether to show the axes by default for new sessions
	- `repeat` indicates whether the axes repeat (most useful for geographic maps)
	- `intervals` specifies how the axis intervals are incremented
	- `units` specifies the units on the axes

	Finally, update the map instantiation with the following lines:

	```js
	map = new tiles.Map( "map" );
    map.add( serverLayer );
    map.add( axis0 );
    map.add( axis1 );
    map.add( baseLayer );
	```

4. Edit the `serverLayer` to pass in the name given to the directory (file system directory or HBase table name) to which your Avro tiles were generated. For the Julia set example, this will be **julia.x.y.v**.
5. Edit the baseLayer to change the background color of the crossplot, if desired. The default background color is black.
6. Save the file.

### <a name="layer-properties"></a> Layer Properties ###

To edit the layer properties for your project:

1. Access the *src/main/resources/layers/***julia-layer.json** file.
2. Make sure the `id` property under the `private' node matches the name given to the file system directory or HBase table name to which your Avro tiles were generated. For the Julia set example, this should be **julia.x.y.v**.
3. Note how the the `pyramid` section under the public `node` specifies the minimum and maximum values for the X (`minX` and `maxX`) and Y (`min` and `maxY`) axes. Make sure the values you specify here match the range you specified in the map properties.
4. If your Avro tiles are saved to your local machine, add or edit the following values in the `data` section under the `private` node:
	- `type`: Enter *file*
	- `root.path`: Specify the *root* path to which you generated the Avro tiles. Note that the `id` you specified in step 2 is the leaf folder that contains the Avro tiles. Set the `root.path` to the folder above that (e.g., */data/tiles/*).
5. If your Avro tiles are saved to HBase, add or edit the following values in the `data` section:
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

## <a name="deployment"></a> Deployment ##

Once you have finished configuring the map and layer properties, copy the `tile-quickstart/` folder to your web server's (e.g., Apache Tomcat or Jetty) webapps directory.

Access the `/tile-quickstart` web directory on the server from any web browser to view the Julia set data plotted on an X/Y chart with six layers of zoom available. For example if your server were `www.example.com`, the URL would be `http://www.example.com/tile-quickstart`.

## Next Steps ##

For a detailed description of the prerequisites and installation procedures for Aperture Tiles, see the [Installation](../installation/) topic.