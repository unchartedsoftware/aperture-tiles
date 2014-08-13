---
section: Docs
subtitle: Generation
permalink: docs/generation/index.html
layout: submenu
---

Tile Generation
===============

Using a distributed framework built on the Apache Spark engine, Aperture Tiles enables you to create a set of visual tiles that summarize and aggregate your large-scale data at various levels in a pyramid structure. 

At the highest level in the tile set pyramid (level 0), a single tile summarizes all of your data. On each lower level, there are up to 4<sup>z</sup> tiles, where z is the zoom level (with lower numbers indicating higher levels). At each level, the tiles are laid out row wise across the base map or plot, starting at the lower left. Each tile summarizes the data it comprises.

<img src="../../img/tile-layout.png" class="screenshot" alt="Tile Layout"></img>

Each tile is an AVRO record object containing an array of bins (typically 256 x 256). Each bin contains an aggregation of all the data points that fall within it.

<img src="../../img/tile-grid.png" class="screenshot" alt="Tile Grid"></img>

The process of creating a set of AVRO tiles from your raw source data is called a tiling job. These tiles created by the job can then be served and browsed in any modern Web browser. You can transform your source data into a set of AVRO tiles using:

- The [**CSVBinner**](#csvbinner), which is a built-in tool that can produce tiles that aggregate numeric data by summation or take the minimum or maximum value.
- The [**RDDBinner**](#custom-tiling), which consists of a set of APIs for creating custom tile-based analytics on non-numeric or non-delimited data. See the Twitter Topics demo (<code>tile-examples/<wbr>twitter-topics</code>) for an example of an example implementation for a data set requiring custom tiling.
- **Third-party tools**, provided they adhere to the Aperture Tiles AVRO schema. Basic schema files are in the Aperture Tiles source code (<code>binning-utilities/<wbr>src/<wbr>main/<wbr>resources</code>). Note that this approach is not discussed below.

Before you run a tiling job, make sure you meet all of the prerequisites listed in the following section.

## <a name="prerequisites"></a>Prerequisites

### <a name="third-party-tools"></a>Third-Party Tools

See the [Installation documentation](../installation) for full details on the required third-party tools.

- **Languages**:
	<p/>
	- *Scala* version 2.10.3
	<p/>
- **Cluster Computing**:
	- *Apache Spark* version 0.9.0 or greater (version 1.0.0 recommended).
      
	  NOTE: In the latest version of Spark, class path issues may arise if you compile from the source code. For this reason, we recommend using one of the pre-built Spark packages.
	  
	- *Hadoop/HDFS/HBase* (Optional) - Choose your preferred version

### <a name="spark-config"></a>Apache Spark Configuration

To configure Apache Spark for your installed version of Hadoop, perform one of the following actions:

- [Download](http://spark.apache.org/downloads.html) the correct version directly.
- If no version is listed for your flavor of Hadoop, [build](http://spark.apache.org/docs/latest/building-with-maven.html) Spark to support it.

#### <a name="spark-script"></a>spark-run Script

The Tile Generator distribution package and the Aperture Tiles source code each contain a **spark-run** script designed to help you build your own tiles:

- Tile Generator (<code>bin/<wbr>spark-run.sh</code>)
- Aperture Tiles Source Code (<code>aperture-tiles/<wbr>tile-generation/<wbr>scripts/<wbr>spark-run</code>)

Note that this script only exists in the source code *after* you build Aperture Tiles, as it requires tile generation JAR files to be in your local maven repository. Therefore, to use this script from the source code, you must first run the following command:

```bash
mvn install
```

The script simplifies the process of running Spark jobs by including all the necessary libraries and setting various parameters. To use this script, you must first set the following environment variables:

```
SCALA_HOME
	Path to the Scala installation directory
	
SPARK_HOME
	Path to the Spark installation directory
```

## <a name="csvbinner"></a>CSVBinner

The Aperture Tiles source code contains a CSVBinner tool designed to process your numeric, character-separated (e.g., CSV) tabular data and generate a set of tiles from it. To define the tile set you want to create, you must edit two types of properties files and pass them to the CSVBinner:

- A [Base properties file](#base-properties), which describes the general characteristics of the data
- [Tiling properties files](#tiling-properties), each of which describes the specific attributes you want to tile

To execute the CSVBinner and run a tiling job, use the **spark-run** script and pass in the names of the properties files you want to use. For example:

<pre class="command">spark-run com.oculusinfo.tilegen.examples.apps.CSVBinner -d /data/twitter/<wbr>dataset-base.bd /data/twitter/dataset.lon.lat.bd</pre>

Where the `-d` switch specifies the Base properties file path, and each subsequent file path specifies a Tiling properties file.

During the tiling job, the CSVBinner creates a collection of AVRO tile data files in the location (HBase or local file system) specified in the Base properties file. The following sections describe the configurable components of the Base properties and Tiling properties files. For a list of sample properties files in the Aperture Tiles source code, see the [Examples](#examples) section.

### <a name="base-properties"></a>Base Properties Files

The Base properties file describes the tiling job, the systems on which it will run and the general characteristics of the source data. The following properties must be defined in the base properties file:

- [Spark Connection](#spark-connection)
- [Tile Storage](#tile-storage)
- [HBase Connection](#hbase-connection)
- [Source Data](#source-data)

#### <a name="spark-connection"></a>Spark Connection

The Spark connection properties define the location of the Spark installation that will run the tiling job:

```
spark
   Location of the Spark master.  Use "local" for standalone Spark.
   Defaults to "local".

sparkhome
   Location of Spark in the remote location (or on the local machine if using 
   standalone). Defaults to the value of the environment variable, SPARK_HOME.

user	(Optional)
   Username passed to the Spark job title. Defaults to the username of the 
   current user.
```

#### <a name="tile-storage"></a>Tile Storage

The tile storage properties indicate whether the tile set created from your source data should be stored in HBase on a local file system:

```
oculus.tileio.type
   Location to which tiles are written:
   
   - hbase 
		See HBase Connection below for further HBase configuration properties
		
   - file
		Writes to the local file system. This is the default.
```

#### <a name="hbase-connection"></a>HBase Connection

If you chose to store your tile set in HBase (i.e., **oculus.tileio.type** is set to *hbase*), these properties define how to connect to HBase:

```
hbase.zookeeper.quorum
   Zookeeper quorum location needed to connect to HBase.

hbase.zookeeper.port
   Port through which to connect to zookeeper.

hbase.master
   Location of the HBase master to which to write tiles.
```

#### <a name="source-data"></a>Source Data

The Source Data properties describe the raw data set to be tiled:

```
oculus.binning.source.location
   Path (local file system or HDFS) to the source data file or files to be 
   tiled.

oculus.binning.prefix
   Prefix to be added to the name of every pyramid location. Used to separate
   this tile generation from previous runs. 
   If not present, no prefix is used.

oculus.binning.parsing.separator
   Character or string used as a separator between columns in the input data
   files. Default is a tab.

oculus.binning.parsing.<field>.index
   Column number of the described field in the input data files.
   This field is mandatory for every field type to be used.

oculus.binning.parsing.<field>.fieldType
   Type of value expected in the column specified by:
   oculus.binning.parsing.<field>.index.

   By default columns are treated as containing real, double-precision values.
   Other possible types are:

       - constant or zero
			Contains 0.0 (the column does not need to exist)

       - int
       		Contains integers

       - long
	       	Contains double-precision integers

       - date
	       	Contains dates. Dates are parsed and transformed into milliseconds
			since the standard Java start date (using SimpleDateFormatter).
			The default format is yyMMddHHmm, but this can be overridden using
			the	oculus.binning.parsing.<field>.dateFormat property.

       	- propertyMap
	       	Contains property maps. All of the following properties must be 
			present	to read the property:
          	
			- oculus.binning.parsing.<field>.property 
             	Name of the property

          	- oculus.binning.parsing.<field>.propertyType
             	Equivalent to fieldType

          	- oculus.binning.parsing.<field>.propertySeparator
             	Character or string used to separate properties

          	- oculus.binning.parsing.<field>.propertyValueSeparator
             	Character or string used to separate property keys from their
             	values

oculus.binning.parsing.<field>.fieldScaling
   How field values should be scaled. The default leaves values as they are.
   Other possibilities are:

   	- log
	   	take the log of the value (oculus.binning.parsing.<field>.fieldBase is
		used, just as with fieldAggregation)

oculus.binning.parsing.<field>.fieldAggregation
   Method of aggregation used on values of the X field. Describes how values
   from multiple data points in the same bin should be aggregated together to
   create a single value for the bin.

   The default is addition.  Other possible aggregation types are:

	- min
		Find the minimum value

	- max
		Find the maximum value

	- log
		Treat the number as a logarithmic value; aggregation of a and b is
		log_base(base^a+base^b). Base is taken from property
		oculus.binning.parsing.<field>.fieldBase, and defaults to e.
```

###<a name="tiling-properties"></a>Tiling Properties Files

The Tiling properties files define the tiling job parameters for each layer in your visual analytic, such as which fields to bin on and how values are binned:

```
oculus.binning.name
   Name (path) of the output data tile set pyramid. If you are writing to a
   file system, use a relative path instead of an absolute path. If you are
   writing to HBase, this is used as a table name. This name is also written to
   the tile set metadata and used as a plot label.

oculus.binning.projection
   Type of projection to use when binning data. Possible values are:

   - EPSG:4326
   		Bin linearly over the whole range of values found (default)

   - EPSG:900913
	   Web-mercator projection (used for geographic values only)

oculus.binning.xField
   Field to use as the X axis value.

oculus.binning.yField
   Field to use as the Y axis value. Defaults to none.

oculus.binning.valueField
   Field to use as the bin value. Default counts entries only.

oculus.binning.levels.<order>
   Array property. For example, if you want to bin levels in three groups, 
   you should include:
   - oculus.binning.levels.0
   - oculus.binning.levels.1
   - oculus.binning.levels.2
   
   Each is a description of the levels to bin in that group - a comma-separated
   list of individual integers, or ranges of integers (described as start-end).
   So "0-3,5" would mean levels 0, 1, 2, 3, and 5. If there are multiple level
   sets, the parsing of the raw data is only done once, and is cached for use
   with each level set. This property is mandatory, and has no default.

oculus.binning.consolidationPartitions
   The number of partitions into which to consolidate data when binning. If not
   included, Spark automatically selects the number of partitions.
```

###<a name="examples"></a>Properties File Examples

Several example properties files can be found in the <code>aperture-tiles/<wbr>tile-generation/<wbr>data</code> directory.

- **twitter-local-base.bd** is an example Base properties file for a source dataset (of ID, time, latitude and longitude) where:
	- Raw data is stored in the local file system
	- Tiles are output to the local file system
- **twitter-hdfs-base.bd** is an example Base properties file for the same source dataset where:
	- Raw data is stored in HDFS
	- Tiles are output to HBase
- **twitter-lon-lat.bd** is an example Tiling properties file that, in conjunction with either of the base files above, defines a tile set of longitude and latitude data aggregated at 10 levels (0-9).

## <a name="custom-tiling"></a>Custom Tiling

If your source data is not character delimited or if it contains non-numeric fields, you may need to create custom code to parse it and create a tile set. In general, creating custom tile generation code involves the following processes:

- [Defining Your Data Structure](#define-data-structure), which includes the following components:
	- Structure of your source data while it is being processed by the tiling job and while it is being written to the AVRO tile set
	- Aggregation methods used to combine multiple records from your source data that fall in the same tile set bin
	- Methods of reading and writing to the tile set
- [Binning Your Data](#binning-your-data), which includes the following components:
	- Method of parsing your source data into the AVRO tile set structure you defined
	- Transformation of your source data into the AVRO tile set
	- Storage of the AVRO tile set to your preferred location

Once you have created the required components to perform each process, you should run your custom Binner. The Binner will create a set of AVRO tiles that you will use in your Aperture Tiles visual analytic.

An example of custom code written for tile generation can be found in the Twitter Topics project (<code>tile-examples/<wbr>twitter-topics/</code>), which displays a Twitter message heatmap and aggregates the top words mentioned in tweets for each tile and bin.

### <a name="define-data-structure"></a>Defining Your Data Structure

The first step in creating custom code for the tile generation process is to decide how your data should be structured, aggregated and written. The following modules are required for this step:

- [Binning Analytic](#binning-analytic), which defines how to process and aggregate your source data 
- [Serializer](#serializer), which defines how to read and write your source data to the tile set

#### <a name="binning-analytic"></a>Binning Analytic

The Binning Analytic is used throughout the tiling process. It should define:

- The data formats used during the tiling job, of which there are two [types](#data-types): a processing type and a binning type.
- How individual data records in a bin are [aggregated](#data-aggregation)
 
See the following file for an example of a custom Bin Analytic.

<pre class="path">/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/<wbr>oculusinfo/twitter/tilegen/TwitterTopicBinningAnalytic.scala</pre>

##### <a name="data-types"></a>Data Types

During a tiling job, the Binning Analytic uses two types of data:

- A **processing type**, which is used when processing the tiles and aggregating them together. It should contain all the information needed for calculations performed during the job. The processing type allows you to keep information that may be needed for aggregation, but not needed once the tile is complete.
- A **binning type**, which is the final form written to the tiles. 

For example, to record an average the two types might be used as follows:

- The processing type would include the number of records and the sum of their values, both of which would be continuously updated as new records were examined
- The binning type would simply be the average (the final processing type sum divided by the total number of records)

A code example is shown in line 41 of **TwitterTopicBinningAnalytic.scala**:

```scala
extends BinningAnalytic[Map[String, TwitterDemoTopicRecord], 
					    JavaList[TwitterDemoTopicRecord]]
```

Here the processing type is a *map* used to add all similar Twitter message topic records together, while the binning type is a *list* containing only the topics with the highest counts.

The Binning Analytic should also describe how to convert the processing type into the binning type. In **TwitterTopicBinningAnalytic.scala**, this is accomplished with a `finish` function (lines 65-66):

```scala
def finish (value: Map[String, TwitterDemoTopicRecord]): JavaList[TwitterDemoTopicRecord] =
	value.values.toList.sortBy(-_.getCountMonthly()).slice(0, 10).asJava
```

The `finish` function:

1. Examines the processing type, which maps Twitter message topics to topic records.
2. Finds the 10 most used topics from the complete processing type map.
3. Stores the 10 most used topic records in an ordered list for each tile.

While the rest of the topics are discarded, they were necessary during processing (e.g., so as to not lose a topic that was eleventh on several different machines -- and hence in the top ten overall).

##### <a name="data-aggregation"></a>Data Aggregation and Record Creation

The Binning Analytic defines how data is aggregated. For example, lines 43-48 of **TwitterTopicBinningAnalytic.scala** compare two maps and creates a new map that contains:

- Keys that exist in either map 
- The sum of the their values

```scala
def aggregate (a: Map[String, TwitterDemoTopicRecord],
	           b: Map[String, TwitterDemoTopicRecord]): Map[String, TwitterDemoTopicRecord] = {
	a ++ b.map{case (k, v) =>
		k -> a.get(k).map(TwitterDemoTopicRecord.addRecords(_, v)).getOrElse(v)
	}
}
```

#### <a name="custom-aggregation"></a>Custom Aggregation Methods

Lines 91-109  of **TwitterTopicBinner.scala** (found in the same folder as the Binning Analytic) are used to calculate the minimum and maximum values and write them to the metadata by level. 

```scala
val minAnalysis:
		AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]],
					 		List[TwitterDemoTopicRecord]] =
	new TwitterTopicListAnalysis(
		sc, new TwitterMinRecordAnalytic,
		Range(levelBounds._1, levelBounds._2+1).map(level =>
			(level+".min" -> ((index: TileIndex) => (level == index.getLevel())))
		).toMap + ("global.min" -> ((index: TileIndex) => true))
	)

val maxAnalysis:
		AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]],
		                    List[TwitterDemoTopicRecord]] =
	new TwitterTopicListAnalysis(
		sc, new TwitterMaxRecordAnalytic,
		Range(levelBounds._1, levelBounds._2+1).map(level =>
			(level+".max" -> ((index: TileIndex) => (level == index.getLevel())))
		).toMap + ("global.max" -> ((index: TileIndex) => true))
	)
```

Standard Bin Analytics are available in:

```
tile-generation\src\main\scala\com\oculusinfo\tilegen\tiling\Analytics.scala
```

#### <a name="serializer"></a>Serializer

The Serializer determines how to read and write to the tile set. The Tile Server requires the following supporting classes to use the Serializer:

- [Serializer](#serializer-class)
- [Serialization Factory](#serialization-factory)
- [Serialization Factory Module](#serialization-factory-module)

See the following sections for examples of each custom Serializer component.

##### <a name="serializer-class"></a>Serializer

The Serializer implements the <strong>com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>TileSerializer</strong> interface. To read and write the AVRO tiles that are most commonly used, it should inherit from:

- <strong>com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>GenericAvroSerializer</strong> if your bin type is a single record 
- <strong>com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>GenericAvroArraySerializer</strong> if your bin type is an array of records record. 

An example of a serializer of tiles whose bins are an array of records is available in:

<pre class="path">/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/<wbr>oculusinfo/twitter/binning/TwitterTopicAvroSerializer.java</pre>

This class inherits from the **GenericAVROArraySerializer.java** (<code>/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/</code>) and defines:

- **getEntrySchemaFile**, which points to a file containing the AVRO description of a single record
- **setEntryValue**, which sets the value of one entry in the list from the AVRO file
- **getEntryValue**, which retrieves the value of one entry in the list from the AVRO file

The definition of the AVRO schema is located in the following folder, where the **name** is set to *entryType*.

<pre class="path">/tile-examples/twitter-topics/twitter-topics-utilities/src/main/resources/<wbr>twitterTopicEntry.avsc</pre>

For records that aren't list types, inherit from the **GenericAvroSerializer.java** (<code>/binning-utilities/src/main/java/com/oculusinfo/binning/io/<wbr>serialization/</code>) and define:

- **getRecordSchemaFile**
- **getValue**
- **setvalue**

The definition of the AVRO schema can be based on the template in the following folder, where the **name** is set to *recordType*.

```
/binning-utilities/src/main/resources/doubleData.avsc
```

##### <a name="serialization-factory"></a>Serialization Factory

The Serialization Factory gets configuration information (e.g., the AVRO compression codec) and hands back the serializer of choice when needed. It also produces the factory and can be injected by Guice.

<pre class="path">/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/<wbr>oculusinfo/twitter/init/TwitterTileSerializationFactory.java</pre>


##### <a name="serialization-factory-module"></a>Serialization Factory Module

The Factory Module tells Guice which factory providers to use to create serialization factories.

<pre class="path">/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/<wbr>oculusinfo/twitter/init/TwitterSerializationFactoryModule.java</pre>

### <a name="binning-your-data"></a>Binning Your Data

There are three steps in binning your data:

1. Parsing your data into the form required by the Binner
2. Running the Binner to transform the data into tiles
3. Writing the tiles

See the following file for an example of a custom Binner.

<pre class="path">/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/<wbr>oculusinfo/twitter/tilegen/TwitterTopicBinner.scala</pre>

#### <a name="parsing-data"></a>Parsing your Data

The Binner expects your data as pairs of **(index, record)**, where:

- **index** is an object indicating where in space the record lies
- **record** is a data record of the processing type your Binning Analytic defines

There are two predefined index types defined by **com.<wbr>oculusinfo.<wbr>tilegen.<wbr>tiling.<wbr>CartesianIndexScheme** and **com.<wbr>oculusinfo.<wbr>tilegen.<wbr>tiling.<wbr>IPv4ZCurveIndexScheme** found in:

```
/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala
```

Unless you are tiling against computer addresses, the Cartesian type is preferable.

- With a Cartesian index, the index type is a pair of doubles. 
- With the IPv4 index, the index type is an array of 4 bytes: the 4 values in an IPv4 address. 

The end result of your parsing will therefore be:

```scala
val data: RDD[((Double, Double), PROCESSING_TYPE)]
```

Where **PROCESSING_TYPE** is the processing type from your [Binning Analytic](#binning-analytic).

Lines 165 - 178 in **TwitterTopicBinner.scala** retrieve the raw data from the Record Parser and create a mapping from (longitude, latitude) pairs to Twitter topic records.

```scala
val data = rawDataWithTopics.mapPartitions(i =>
	{
		val recordParser = new TwitterTopicRecordParser(endTimeSecs)
		i.flatMap(line =>
			{
				try {
					recordParser.getRecordsByTopic(line)
				} catch {
					// Just ignore bad records, there aren't many
					case _: Throwable => Seq[((Double, Double), Map[String, TwitterDemoTopicRecord])]()
				}
			}
		)
	}
)
data.cache
```

#### <a name="binning"></a>Binning

Lines 193 - 201 of **TwitterTopicBinner.scala** transform the data into tiles:

```scala
val tiles = binner.processDataByLevel(data,
				                      new CartesianIndexScheme,
				                      new TwitterTopicBinningAnalytic,
				                      tileAnalytics,
				                      dataAnalytics,
				                      tilePyramid,
				                      levelSet,
				                      xBins=1,
				                      yBins=1)
```

**Binner.processDataByLevel** is defined in the following file on line 237.

```
/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala
```

It accepts the following properties:

```
bareData
	A distributed collection of (index, record) pairs as described above.

indexScheme
	Used to convert the index to a set X/Y coordinates that can be plotted.
	When using a CartesianIndexScheme, the coordinates are taken as given.

binAnalytic
	A Binning Analytic that, as described above, defines how to aggregate two
	records, convert them into the form written and determine the extrema of
	the dataset.

tileAnalytics
	Analytics used to perform custom aggregations on tile data (e.g., get the
	minimum and maximum values) and write them to the metadata by level.

dataAnalytics
	Analytics used to perform custom aggregations on raw data that would
	otherwise be lost by the processing type (e.g., recording the maximum
	individual value) and write them to the metadata by level.

tileScheme
	The projection to use to transform from the raw data index into tiles and
	bins. Two types are predefined:
	- /binning-utilities/src/main/java/com/oculusinof/binning/impl/
	  AOITilePyramid,
	  which is a linear transformation into an arbitrarily sized space
	- /binning-utilities/src/main/java/com/oculusinof/binning/impl/
	  WebMercatorTilePyramid, which is a standard geographical projection.

levels
	Specifies which levels to process at the same time. It is generally
    recommended you process levels 1-9 together, then run additional levels	one
	at a time afterward. This arrangement typically makes effective use of
	system resources.

xBins		(Optional)
	Number of bins on the X axis.  Defaults to 256

yBins		(Optional)
	Number of bins on the Y axis.  Defaults to 256

consolidationPartitions		(Optional)
	The number of reducers to use when aggregating data records into bins and
	tiles. Defaults to the same number of partitions as the original data set.
	Alter if you encounter problems with the tiling job due to lack	of
	resources.

isDensityStrip
	Set to true if running a one-dimentional tiling job. Defaults to false.
```

#### <a name="writing-tiles"></a>Writing Tiles

Lines 202 - 209 of **TwitterTopicBinner.scala** specify how to write the tiles created from your transformed data.

```scala
tileIO.writeTileSet(tilePyramid,
				    pyramidId,
				    tiles,
				    new TwitterTopicValueDescription,
				    tileAnalytics,
				    dataAnalytics,
				    pyramidName,
				    pyramidDescription)
```

**tileIO.writeTileSet** is defined in the following file on line 180.

```
/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala
```

It accepts the following properties:

```
tileScheme
	Type of projection built from the set of bins and levels. Must match the
    tileScheme specified in binner.processDataByLevel.

writeLocation
	The ID to apply to the tile set when writing it. If writing to the local
	filesystem, this will be the base directory into which to write the tiles.
	If writing to HBase, it will be the name of the table to write.

tiles
	The binned data set produced by binner.processDataByLevel.

valueScheme
	The bin descriptor describing the dataset.  This must match the bin
	descriptor used when creating the tiles.

tileAnalytics	(Optional)
	Analytics used to perform custom aggregations on tile data (e.g., get the
	minimum and maximum values) and write them to the metadata by level.

dataAnalytics	(Optional)
	Analytics used to perform custom aggregations on raw data that would
	otherwise be lost by the processing type (e.g., recording the maximum
	individual value) and write them to the metadata by level.

name
	Name of the finished pyramid. Stored in the tile metadata.

description
	Description of the finished pyramid. Stored in the tile metadata. 
```