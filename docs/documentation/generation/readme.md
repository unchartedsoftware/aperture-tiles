        ---
section: Documentation
subtitle: Generation
permalink: documentation/generation/index.html
layout: default
---

Tile Generation
===============

Aperture Tiles provides a distributed framework for processing your large-scale data built on the Apache Spark engine to create a set of tiles that summarize and aggregate your data at various levels in a pyramid structure. 

At the highest level (level 0) in the tile set pyramid, there is only a single tile summarizing all data. On each lower level, there are up to 4^z tiles, where z is the zoom level (with lower numbers indicating higher levels. At each level, the tiles are laid out row wise across the base map or plot, starting at the lower left. Each tile summarizes the data located in that particular tile.

![Tile Layout](../../img/tile-layout.png)

Each tile is an AVRO record object containing an array of values (typically 256 x 256). Each bin element in the array contains an aggregation of all the data points that fall within it.

![Tile Grid](../../img/tile-grid.png)

There are three ways to turn your source data into a set of AVRO tiles:

- Using the built-in CSVBinner tool, which can produce tiles that aggregate numeric data by summation, min, or max.
- Creating custom tile-based analytics using the *RDDBinner* APIs.
- Using third-party tools, provided they adhere to the Aperture Tiles AVRO schema. Basic schema files are available in `binning-utilities/src/main/resources`.

##<a name="prerequisites"></a>Prerequisites

###<a name="third-party-tools"></a>Third-Party Tools

See the [Installation documentation](../setup) for full details on the required third-party tools.

- **Languages**:
	- Scala version 2.10.3
- **Cluster Computing**:
	- Apache Spark version 0.9.0 or greater (version 1.0.0 recommended)
	- Hadoop/HDFS/HBase (Optional) - Choose your preferred version

###<a name="spark-config"></a>Apache Spark Configuration

Apache Spark must be configured to use the same version of Hadoop that you have installed.  Either [download the correct version directly](http://spark.apache.org/downloads.html), or if no version is listed for the correct flavour of hadoop, you can [build spark](http://spark.apache.org/docs/latest/building-with-maven.html) to support your hadoop version.   

####<a name="spark-script"></a>spark-run Script

The Tile Generator distribution package, and the Aperture Tiles source code, both contain a **spark-run** script (*bin/**spark-run.sh*** and *aperture-tiles/tile-generation/scripts/**spark-run***, respectively) designed to help you build your own tiles. The script simplifies the process of running Spark jobs by including all the necessary libraries and setting various parameters. 

If you want to use this script, you must first set the following environment variables:

```
SCALA_HOME - the path to the scala installation directory
SPARK_HOME - the path to the spark installation directory
```
Note that in the source code, this script only exists after building the project, and that it looks for tile-generation jar files in your local maven repository.  Therefore, in order to use this script from the source code, you first needs to run **mvn install**.

##<a name="tiling-job"></a>Running a Tiling Job

###<a name="csvbinner"></a>CSVBinner

The Aperture Tiles project includes a CSVBinner tool designed to process numeric, character-separated (e.g., CSV) tabular data. The CSVBinner accepts two types of property files to define the tile set you want to create:

- A Base property file, which describes the general characteristics of the data. 
- Tiling property files, each of which describes the specific attributes you want to tile

Run the tile generation using the **spark-run** script by using a command similar to:

```
spark-run com.oculusinfo.tilegen.examples.apps.CSVBinner -d /data/twitter/dataset-base.bd /data/twitter/dataset.lon.lat.bd
```

Where the -d switch specifies the base property file path, and each subsequent file path specifies a tiling property file.

The CSVBinner creates a collection of AVRO tile data files in the specified location (HBase or your local filesystem).

####<a name="base-properties"></a>Base Property Files

The following properties must be defined in the base property file:

#####Spark Connection Details

```
spark
   Location of the spark master.  Use "local" for spark standalone.
   Defaults to "local".

sparkhome
   Location of Spark in the remote location (and, necessarily, on the local
   machine too).
   Defaults to the value of the environment variable, SPARK_HOME.

user (optional)
   Username passed to the job title so people know who is running the job.
   Defaults to the username of the current user.
```

#####Tile Storage Properties

```
oculus.tileio.type
   Location to which tiles are written:
   - hbase (see HBase properties below for further HBase configuration
     properties)
   - file, to write to the local file system. This is the default.
```

#####HBase Connection Properties

If **oculus.tileio.type** is set to *hbase*, specify the HBase connection configuration properties:

```
hbase.zookeeper.quorum
   Zookeeper quorum location needed to connect to HBase.

hbase.zookeeper.port
   Port through which to connect to zookeeper.

hbase.master
   Location of the HBase master to which to write tiles.
```

#####Source Data Properties

The rest of the configuration properties describe the data set to be tiled.

```
oculus.binning.source.location
   Path (local file system or HDFS) to the source data file or files to be tiled. 

oculus.binning.prefix
   Prefix to be added to the name of every pyramid location. Used to separate
   this tile generation from previous runs. 
   If not present, no prefix is used.

oculus.binning.parsing.separator
   Character or string used as a separator between columns in the input
   data files. Default is a tab.

oculus.binning.parsing.<field>.index
   Column number of the described field in the input data files
   This field is mandatory for every field type to be used

oculus.binning.parsing.<field>.fieldType
   Type of value expected in the column specified by 
   oculus.binning.parsing.<field>.index.
   Default is to treat the column as containing real, double-precision values.
   Other possible types are:
       constant or zero - contains 0.0 (the column does not need to exist)
       int  - contains integers
       long - containe double-precision integers
       date - contains dates.  Date are parsed and transformed into
              milliseconds since the standard Java start date (using
			  SimpleDateFormatter).
              Default format is yyMMddHHmm, but this can be overridden using
              the oculus.binning.parsing.<field>.dateFormat
       propertyMap - contains property maps.  Further information
                     required to retrieve the specific property.  All 
                     of the following properties must be present to 
                     read the property:
          oculus.binning.parsing.<field>.property 
             Name of the property
          oculus.binning.parsing.<field>.propertyType
             Equivalent to fieldType
          oculus.binning.parsing.<field>.propertySeparator
             Character or string used to separate properties
          oculus.binning.parsing.<field>.propertyValueSeparator
             Character or string used to separate property keys from their
             values

oculus.binning.parsing.<field>.fieldScaling
   How field values should be scaled. Default is to leave values as they are.
   Other possibilities are:
       log - take the log of the value (oculus.binning.parsing.<field>.fieldBase
             is used, just as with fieldAggregation)

oculus.binning.parsing.<field>.fieldAggregation
   Method of aggregation to be used on values of the X field. Describes how values from multiple data points in the same bin should be aggregated together to create a single value for the bin.
   Default is addition.  Other possible aggregation types are:
       min - find the minimum value
       max - find the maximum value
       log - treat the number as a  logarithmic value; aggregation of a and b is 
             log_base(base^a+base^b).  Base is taken from property
             oculus.binning.parsing.<field>.fieldBase, and defaults to e
```

####<a name="tiling-properties"></a>Tiling Properties File

The properties file defines the tiling job parameters for each layer in your visual analytic, which fields to bin on and how values are binned.

```
oculus.binning.name
   Metadata name of the output data tile set pyramid. Used a plot label.

oculus.binning.projection
   Type of projection to use when binning data.  Possible values are:
       EPSG:4326 - bin linearly over the whole range of values found (default)
       EPSG:900913 - web-mercator projection (used for geographic values only)

oculus.binning.xField
   Field to use as the X axis value.

oculus.binning.yField
   Field to use as the Y axis value.  
   Defaults to none.

oculus.binning.valueField
   Field to use as the bin value.
   Default is to count entries only.

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
   The number of partitions into which to consolidate data when binning it. 
   If not included, Spark automatically selects the number of partitions.
```

####<a name="examples"></a>Examples

Several example property files can be found in the *aperture-tiles/tile-generation/data* directory.

- **twitter-local-base.bd** is an example base property file for a locally stored dataset of ID, TIME, LATITUDE, LONGITUDE, with the tiles also output to the local file system.
- **twitter-hdfs-base.bd** is an example base property file for the same dataset, but stored in hdfs, and output to hbase.
- **twitter-lon-lat.bd** is an example tiling file that takes either of the base files above, and tells the system to tile levels 0-9 of the longitude and latitude data contained therein.

###<a name="custom-tiling"></a>Custom Tiling

If your source data is not character delimited or if it contains non-numeric fields, you may need to create custom code to parse it and perform tile generation. A good example of custom code for the purposes of tile generation can be found in the Twitter Topics project in *tile-examples/twitter-topics-sample/*, which displays a Twitter message heatmap and aggregates the top words mentioned in tweets for each tile and bin.

In general, creating custom tile generation code involves the following processes:

- Describing Your Data
- Binning Your Data

Once you have written each of the required components, you should run your custom Binner to create a set of AVRO tiles that you will use in your Aperture Tiles visual analytic.

####<a name="record-parser"></a>Describing Your Data

The first step in creating a custom tile generation process is deciding how your data should be structured so it can be handled by the Binner. There are several modules required for this step:

- Bin Descriptor
- Serializer

#####Bin Descriptor

The Bin Descriptor is used throughout the tiling process. It defines the data format used, how individual data records in a bin are combined, and how the maximum and minimum values are determined (the latter is often needed when rendering data to tiles). 

See the following file for an example of a custom Bin Descriptor.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinDescriptor.scala
```

######Types

The Bin Descriptor describes the type of data using two types:

- A processing type, which is the record type used when processing the tiles and aggregating them together. It should contain all the information needed for calculations performed during the binning job.
- A binning type, which is the final form that gets written to the tiles. 

This separation allows one to keep extra information when tiling that may be needed for aggregation, but is no longer needed once the tile is complete.

For example, if one wanted to record an average, the processing type might include both the number of records, and the total sum of their values, while the binning type would be simply the average.

Another example is shown in line 47 of `TwitterTopicBinDescriptor`, the processing type is a map used add to add all similar topic records together. The binning type is a list containing only the topics with the highest counts.

```scala
extends BinDescriptor[Map[String, TwitterDemoTopicRecord], JavaList[TwitterDemoTopicRecord]] {
```

The Bin Descriptor describes how to convert from the processing type into the binning type via the `convert` function.  In `TwitterTopicBinDescriptor`, this is found on lines 103 and 104.  In this case, it takes the processing type, a map from topic to topic records, finds the 10 most used topics, and records their topic records, in order.  All but the top ten are thrown out - but were necessary during processing, so that a topic that was eleventh on several different machines (and hence in the top ten overall) wasn't lost.

```scala
def convert (value: Map[String, TwitterDemoTopicRecord]): 
JavaList[TwitterDemoTopicRecord] =
    value.values.toList.sortBy(-_.getCountMonthly()).slice(0, 10).asJava
```

######Data Aggregation and Record Creation

The Bin Descriptor defines how data is aggregated.  For instance, the example BinDescriptor (lines 51 - 64 of `TwitterTopicBinDescriptor`) compares two maps, creating a new map with keys that exist in either, and the sum of the values of both.

```scala
def aggregateBins (a: Map[String, TwitterDemoTopicRecord],
	  b: Map[String, 
      TwitterDemoTopicRecord]): Map[String, TwitterDemoTopicRecord] = {
  a.keySet.union(b.keySet).map(tag => {
    val aVal = a.get(tag)
    val bVal = b.get(tag)
    if (aVal.isEmpty) {
  (tag -> bVal.get)
    } else if (bVal.isEmpty) {
  (tag -> aVal.get)
    } else {
  (tag -> addRecords(aVal.get, bVal.get))
    }
  }).toMap
}
```

######Calculating Custom Aggregation Methods

Lines 66-80  of `TwitterTopicBinDescriptor` are used to calculate the minimum and maximum values and write them to the metadata by level. 

```scala
def defaultMin: JavaList[TwitterDemoTopicRecord] = new ArrayList()
  def min (a: JavaList[TwitterDemoTopicRecord],
	   b: JavaList[TwitterDemoTopicRecord]): JavaList[TwitterDemoTopicRecord] = {
    val both = a.asScala.toList ++ b.asScala.toList
    val min = minOfRecords(both.toArray : _*)
    List(min).asJava
  }

  def defaultMax: JavaList[TwitterDemoTopicRecord] = new ArrayList()
  def max (a: JavaList[TwitterDemoTopicRecord],
	   b: JavaList[TwitterDemoTopicRecord]): JavaList[TwitterDemoTopicRecord] = {
    val both = a.asScala.toList ++ b.asScala.toList
    val max = maxOfRecords(both.toArray : _*)
    List(max).asJava
  }
```

Standard Bin Descriptors are available in:

```
tile-generation\src\main\scala\com\oculusinfo\tilegen\tiling\BinDescriptor.scala
```

######Serializer

Lastly, the binDescriptor defines the Serializer to determine how to write tiles.

```
def getSerializer: TileSerializer[JavaList[TwitterDemoTopicRecord]] = 
new TwitterTopicAvroSerializer(CodecFactory.bzip2Codec())
```

####<a name="serializer"></a>Serializer

The Serializer determines how to read and write tiles in a tile set. The Serializer requires some supporting classes for the tile server to use it.

- Serializer
- Serialization Factory
- Serialization Factory Module
- Serialization Factory Provider 

See the following sections for examples of each custom Serializer component.

#####Serializer

The Serializer implements the `com.oculusinfo.binning.io.serialization.TileSerializer` interface.  More specifically, to read and write the AVRO tiles that are most commonly used, it should inherit from either `com.oculusinfo.binning.io.serialization.GenericAvroSerializer` or`com.oculusinfo.binning.io.serialization.GenericAvroArraySerializer`.  Use the latter if your bin type is an array of records, the former if it is a single record. 

An example of a serializer of tiles whose bins are an array of records is available in:

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/binning/TwitterTopicAvroSerializer.java
```

This class inherits from the GenericAVROArraySerializer.java (`/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/`) and defines:

- getEntrySchemaFile - which points to a file containing the AVRO description of a single record
- setEntryValue - which sets the value of one entry in the list from the AVRO file
- getEntryValue - which retrieves the value of one entry in the list from the AVRO file

The definition of the AVRO schema is located in the following folder, where the *name* is set to **entryType**.

```
/tile-examples/twitter-topics-sample/twitter-sample-utilities/src/main/resources/twitterTopicEntry.avsc
```

For records that aren't list types, inherit from the GenericAvroSerializer.java (`/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/`) and define:

-getRecordSchemaFile
-getValue
-setvalue

The definition of the AVRO schema can be based on the template in the following folder, where the *name* is set to **recordType**.

```
/binning-utilities/src/main/resources/doubleData.avsc
```

#####Serialization Factory

The Serialization Factory gets configuration information (e.g., the AVRO compression codec) and hands back the serializer of choice when needed.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterTileSerializationFactory.java
```

#####Serialization Factory Provider

The Factory Provider is an object that can be injected by Guice, and that produces the factory.

```
/tile-examples/twitter-topics-sample/twitter-sample-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterTileSerializationFactoryProvider.java
```

#####Serialization Factory Module

The Factory Module tells Guice which factory provider(s) to use to create serialization factories.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterSerializationFactoryModule.java
```

####<a name="binner"></a>Binning Your Data

There are three steps in binning your data:

- Transforming the data into the form required by the binner
- Running the binner to transform the data into tiles
- Writing the tiles

See the following file for an example of a custom Binner.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala
```

#####<a name="parsing-data"></a>Parsing your data

The binner expects your data as pairs of `(index, record)`, where `index` is an object indicating where in space the record lies, and `record` is a data record, of the  processing type your bin descriptor defines.

There are two predefined index types, defined by `com.oculusinfo.tilegen.tiling.CartesianIndexScheme` and `com.oculusinfo.tilegen.tiling.IPv4ZCurveIndexScheme` (both found in `/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala`).  With a Cartesian index, the index type is a pair of doubles.  With the IPv4 index, the index type is an array of 4 bytes - the 4 values in an IPv4 address.  Generally, unless you are specifically tiling against computer addresses, the cartesian type will be prefferable.  
The end result of your parsing will therefore be:

```val data: RDD[((Double, Double), PROCESSING_TYPE)]```
where `PROCESSING_TYPE` is the processing type from your bin descriptor.

Lines 92 - 107 in the `TwitterTopicBinner` retrieve the raw data from the Record Parser and creates a mapping from (longitude, latitude) pairs to Twitter topic records.

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

#####Binning

Lines 116 - 117 of `TwitterTopicBinner` transforms the data into tiles.

```scala
val tiles = binner.processDataByLevel(data, new CartesianIndexScheme, binDesc,
                                      tilePyramid, levelSet, bins=1)
```

Binner.processDataByLevel is defined in `/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala` on line 229.  It accepts the following properties:

```
data
	A distributed collection of (index, record) pairs, as described above.

indexScheme
	Used to convert the index to a set X/Y coordinates that can be plotted.  When using a CartesianIndexScheme, the coordinates are taken as given.

binDescriptor
	A Bin Descriptor, as described above, which defines how to aggregate two records, how to convert them into the form written, and how to determine the extrema of the dataset.
 
tilePyramid
	The projection to use to transform from the raw data index into tiles and bins.  Two types are predefined, an `/binning-utilities/src/main/java/com/oculusinof/binning/impl/AOITilePyramid`, which is a linear transformation into an arbitrarily sized space, and `/binning-utilities/src/main/java/com/oculusinof/binning/impl/WebMercatorTilePyramid`, which is a standard geographical projection.

levelSet
	Specifies which levels to process at the same time. It is generally
    recommended you process levels 1-9 together, then any additional
    levels one at a time afterwards.  This arrangement typically 
	makes effective use of system resources.

bins
	Number of bins on each axis.  Optional, defaults to 256

consolidationPartitions
	The number of reducers to use when aggregating data records into bins and tiles.  Optiona, defaults to the same number of partitions as the original data set, but can be altered if one encounters problems with the tiling job due to lack of resources.

isDensityStrip
	This should be true if doing a one-dimentional tiling job.  Defaults to false.
```

#####Writing tiiles

Lines 118 - 119 of `TwitterTopicBinner` specify how to write the tiles created from your transformed data.

```
tileIO.writeTileSet(tilePyramid, pyramidId, tiles, binDesc, 
					pyramidName, pyramidDescription)
```

Where tileIO.writeTileSet accepts the following properties:

```
tilePyramid
	Type of projection built from the set of bins and levels. Must match the
    tilePyramid specified in binner.processDataByLevel.

pyramidID
	The ID to apply to the tile set when writing it.  If writing to the local filesystem, this will be the base directory into which to write the tiles.  If writing to HBase, it will be the name of the table to write.

tiles
	The binned data set produced by binner.processDataByLevel.

binDescriptor
	The bin descriptor describing the dataset.  This must match the bin descriptor used when creating the tiles.

pyramidName
	Name of the finished pyramid. Stored in the tile metadata.

pyramidDescription
	Description of the finished pyramid. Stored in the tile metadata. 
```