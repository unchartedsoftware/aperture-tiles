---
section: Documentation
subtitle: Generation
permalink: documentation/generation/index.html
layout: default
---

Tile Generation
===============

Aperture Tiles provides a distributed framework for processing your large-scale data built on the Apache Spark engine to create a set of tiles that summarize and aggregate your data at various levels in a pyramid structure. 

At the highest level in the tile set pyramid, there is only a single tile summarizing all data. At each subsequent level, there are 2^(z-1) tiles, where z is the level number starting at 1 for the highest level. At each level, the tiles are laid out row wise across the base map or plot, starting at the upper left. Each tile summarizes the data located in that particular tile.

![Tile Layout](../../img/tile-layout.png)

Each tile is an AVRO record object containing an array of values (typically 256 x 256). Each bin element in the array contains an aggregation of all the data points that fall within it.

![Tile Grid](../../img/tile-grid.png)

There are three ways to turn your source data into a set of AVRO tiles:

- Using the built-in CSVBinner tool, which can produce tiles that aggregate numeric data by:
	- Summation
	- Min and max ranges
	- Time series
	- Top keywords 
- Creating custom tile-based analytics using the *RDDBinner* and *ObjectifiedBinner* APIs.
- Using third-party tools, provided they adhere to the Aperture Tiles AVRO schema (documentation coming soon).

##<a name="prerequisites"></a>Prerequisites

###<a name="third-party-tools"></a>Third-Party Tools

See the [Installation documentation](../setup) for full details on the required third-party tools.

- **Languages**:
	- Scala version 2.9.3
- **Cluster Computing**:
	- Hadoop/HDFS (Optional) - Choose your preferred version and use in conjunction with HBase.
	- HBase (Optional) - Use in conjunction with Hadoop/HDFS.
	- Apache Spark version 0.7.2 or greater (version 1.0.0 recommended)

###<a name="source-code"></a>Aperture Tiles Source Code

The tile generation process requires certain Aperture Tiles .jar files to be installed in your local Maven repository. Make sure you have correctly installed the Aperture Tiles project as outlined in the [Installation documentation](../setup).

###<a name="spark-config"></a>Apache Spark Configuration

Apache Spark determines which version of Hadoop to use by looking in *\$SPARK\_HOME/project/**SparkBuild.scala***. Instructions for what to change are contained therein.

####<a name="spark-script"></a>spark-run Script

The Aperture Tiles source code contains a **spark-run** script (*aperture-tiles/tile-generation/scripts/**spark-run***) designed to help you build your own tiles. The script simplifies the process of running Spark jobs by including all the necessary libraries and setting various parameters. 

If you want to use this script, you must first set the following environment variables:

```
SCALA_HOME - the path to the scala installation directory
SPARK_HOME - the path to the spark installation directory
```

If you plan to store your tile set in HBase, you must edit the **spark-run** script to specify the version of HBase you are using. Find the line after `\# framework-related jars` and edit the version accordingly. For example if the version of HBase is Cloudera 4.4.0 edit the line as:

```
# framework-related jars
addToSparkClasspath org.apache.hbase hbase 0.94.6-cdh4.4.0
```

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

user
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
   Path to the source data file or files to be tiled.

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
   Name of the output data tile set pyramid.

oculus.binning.projection
   Type of projection to use when binning data.  Possible values are:
       EPSG:4326 - bin linearly over the whole range of values found (default)
       EPSG:900913 - web-mercator projection (used for geographic values only)

oculus.binning.xField
   Field to use as the X axis value.

oculus.binning.yField
   Field to use as the Y axis value.  
   Defaults to none (i.e., a density strip of x data).

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

If your source data is not character delimited or if it contains non-numeric fields, you may need to create custom code to parse it and perform tile generation. A good example of custom code for the purposes of tile generation can be found in the Twitter Topics project in *tile-examples/twitter-topics/*, which displays a Twitter message heatmap and aggregates the top words mentioned in tweets for each tile and bin.

In general, creating a custom tile generation process involves the following components:

- Record Parser
- Binner
- Serializer

Once you have written each of the required components, you should run your custom Binner to create a set of AVRO tiles that you will use in your Aperture Tiles visual analytic.

####<a name="record-parser"></a>Record Parser

The Record Parser should process, sanitize and transform your source data into a format that can be handled by the Binner, which will create the pyramid for your Aperture Tiles visual analytic. It is important to build error handling and error correction capabilities into the Parser.

See the following file for an example of a custom Record Parser. Record Parsers should be written in Scala. 

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicRecordParser.scala`
```

#####Record File

The Record Parser calls the Record file, which defines the schema of your transformed data. See the following file for an example of a custom Record file. Record files are generally written in Java, but can also be written in Scala.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterDemoTopicRecord.java`
```

####<a name="binner"></a>Binner

The Binner should take your parsed source data, bin it across each of the levels you want in your visual analytic and create a set of pyramid tiles. 

See the following file for an example of a custom Binner. Binners should be written in Scala. 

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala
```

There are two particularly important sections of the Binner code in this example. The first (lines 91 - 104) retrieves the raw data from the Record Parser and creates a mapping of Twitter topics to latitude and longitude coordinates, which determines to what bins the topics will be applied.

```scala
val data = rawDataWithTopics.mapPartitions(i => {     
	val recordParser = new TwitterTopicRecordParser(endTimeSecs)
	i.flatMap(line => {
		try {
			recordParser.getRecordsByTopic(line)
    	}
		catch {
			// Ignore bad records
			case _: Throwable => Seq[((Double, Double), Map[String,
									   TwitterDemoTopicRecord])]()
		}
	})
})
data.cache
```

The second section (lines 112 - 115) specifies the following details about the binning job:
- How to process your transformed data
- How to write the tiles created from your transformed data

```scala
val tiles = binner.processDataByLevel(data, new CartesianIndexScheme, binDesc,
                                      tilePyramid, levelSet, bins=1)
tileIO.writeTileSet(tilePyramid, pyramidId, tiles, binDesc, 
					pyramidName, pyramidDescription)
```

Where binner.processDataByLevel accepts the following properties:

```
data
	Paired index that describes how the data should be handled while being
    processed (processing type) and while being binned (binning type).

cartesianIndexScheme
	Used to convert the index to a set X/Y coordinates that can be plotted.

binDesc
	Determines how aggregate two records of the same processing type and how to
    handle that aggregate value to determine what should be written to the
    tile. Calls the BinDescriptor.
 
tilePyramid
	Type of projection built from the set of bins and levels. Either uses
    the raw data X/Y coordinates directly (linear), or transforms a set of
    longitude/latitude values into caresian coordinates (web mercator).

levelSet
	Specifies the number of levels to process at a time. It is generally
    recommended you process levels 1-9 together, then any additional
    levels one at a time afterwards to ensure proper use of system
    resources.

bins
	Number of bins on each axis.
```

And where tileIO.writeTileSet accepts the following properties:

```
tilePyramid
	Type of projection built from the set of bins and levels. Must match the
    tilePyramid specified in binner.processDataByLevel.

pyramidID
	The local filesystem or Hbase to which to write the tiles.

tiles
	The binned data set produced by binner.processDataByLevel.

binDesc
	Determines what should be written to the tiles. Calls the BinDescriptor.
    Must match the binDesc specified in binner.processDataByLevel.

pyramidName
	Name of the finished pyramid. Stored in the tile metadata.

pyramidDescription
	Description of the finished pyramid. Stored in the tile metadata. 
```

#####Bin Descriptor

The Bin Descriptor is called by the Binner. It determines how to aggregate multiple data points that are saved to the same bin.

See the following file for an example of a custom Bin Descriptor. Bin Descriptors should be written in Scala.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen
```

There are two particularly important sections of the Bin Descriptor code in this example. The first (lines 51 - 64) compares two values with the same map, determines how to aggregate them, then creates a new record.

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

The second section (lines 103 -104) creates the list of the top 10 Twitter words for each tile. It takes the values of a map, sorts them by count and returns the top 10 in the list.

```scala
def convert (value: Map[String, TwitterDemoTopicRecord]): 
JavaList[TwitterDemoTopicRecord] =
    value.values.toList.sortBy(-_.getCountMonthly()).slice(0, 10).asJava
```

Lastly, the binDescriptor call the Serializer to determine how to write tiles.

```
def getSerializer: TileSerializer[JavaList[TwitterDemoTopicRecord]] = 
new TwitterTopicAvroSerializer(CodecFactory.bzip2Codec())
```
  
####<a name="serializer"></a>Serializer

The Serializer determines how how to write topic records to the AVRO tile set format used by Aperture Tiles. The Serializer has four components, each of which is generally written in Java (Scala is also supported):

- Serializer
- Serialization Factory
- Serialization Factory Module
- Serialization Factory Provider 

See the following sections for examples of each custom Serializer component. 

#####Serializer

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/binning/TwitterTopicAvroSerializer.java
```

#####Serialization Factory

The Serialization produces the data, gets configuration information (e.g., changes the AVRO compression codec) and hands back the serializer of choice at right time.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterTileSerializationFactory.java
```

#####Serialization Factory Module

The Factory Module notifies Guice about the factory provided on create calls.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterSerializationFactoryModule.java
```

#####Serialization Factory Provider

The Factory Provider produces the factory.

```
/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterTileSerializationFactoryProvider.java
```