---
section: Docs
subsection: Development
chapter: How-To
topic: Run Custom Tiling Jobs
permalink: docs/development/how-to/custom-tiling/
layout: submenu
---

Run Custom Tiling Jobs
======================

The following sections describe how you can generate a tile pyramid from your raw data by creating your own custom generation code.

## <a name="custom-tiling"></a> Custom Tiling Jobs ##

If your source data is not character delimited or if it contains non-numeric fields, you may need to create custom code to parse it and create a tile set. In general, creating custom tile generation code involves the following processes:

- [Defining Your Data Structure](#define-data-structure), which includes some components of the Bin Analytics and Tile Creation stages of the [tile generation process](../tile-pyramid/#tile-gen-process):
	- Structure of your source data while it is being processed by the tiling job and while it is being written to the Avro tile set
	- Aggregation methods used to combine multiple records from your source data that fall in the same tile bin
	- Methods of reading and writing to the tile set
- [Binning Your Data](#binning-your-data), which includes the remaining components of the Bin Analytics and Tile Creation stages of the [tile generation process](../tile-pyramid/#tile-gen-process):
	- Method of parsing your source data into the Avro tile set structure you defined
	- Transformation of your source data into the Avro tile set
	- Storage of the Avro tile set to your preferred location

Once you have created the required components to perform each process, you should run your custom binner. The binner will create the Avro tile set you will use in your Aperture Tiles visual analytic.

An example of custom code written for tile generation can be found in the Twitter Topics project ([tile-examples/<wbr>twitter-topics/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics)), which displays a Twitter message heatmap and aggregates the top words mentioned in tweets for each tile and bin.

## <a name="define-data-structure"></a> Defining Your Data Structure ##

The first step in creating custom code for the tile generation process is to decide how your data should be structured, aggregated and written. The following modules are required for this step:

- [Binning Analytic](#binning-analytic), which defines how to process and aggregate your source data 
- [Serializer](#serializer), which defines how to read and write your source data to the tile set

### <a name="binning-analytic"></a> Binning Analytic ###

The Binning Analytic is used throughout the tiling process. It should define:

- The [data types](#data-types) used during the tiling job, of which there are two formats: a processing type and a binning type.
- The method of [aggregation](#data-aggregation) for joining individual data records in a bin
 
For an example of a custom Bin Analytic, see the [TwitterTopicBinningAnalytic.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinningAnalytic.scala) file in [tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>tilegen/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen).

#### <a name="data-types"></a> Data Types ####

During a tiling job, the Binning Analytic uses two types of data:

- A **processing type**, which is used when processing the tiles and aggregating them together. It should contain all the information needed for calculations performed during the job. The processing type allows you to keep information that may be needed for aggregation, but not needed once the tile is complete.
- A **binning type**, which is the final form written to the tile bins.

For example, to record an average, the two types might be used as follows:

1. The processing type would include the count of records and the sum of their values, both of which would be continuously updated as new records are examined.
2. The binning type would simply be the average, or the final processing type sum divided by the total count of records.

A code example is shown in line 40 of [TwitterTopicBinningAnalytic.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinningAnalytic.scala):

```scala
extends BinningAnalytic[Map[String, TwitterDemoTopicRecord], 
					    JavaList[TwitterDemoTopicRecord]]
```

Here the processing type is a *map* used to add all similar Twitter message topic records together, while the binning type is a *list* containing only the topics with the highest counts.

##### Transformation #####

The Binning Analytic should also describe how to convert the processing type into the binning type. In [TwitterTopicBinningAnalytic.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinningAnalytic.scala), this is accomplished with a **finish** function (lines 64-65):

```scala
def finish (value: Map[String, TwitterDemoTopicRecord]): JavaList[TwitterDemoTopicRecord] =
	value.values.toList.sortBy(-_.getCountMonthly()).slice(0, 10).asJava
```

The **finish** function:

1. Examines the processing type, which maps Twitter message topics to topic records.
2. Finds the 10 most used topics from the complete processing type map.
3. Stores the 10 most used topic records in an ordered list for each tile.

While the rest of the topics are discarded, they were necessary during processing (e.g., so as to not lose a topic that was eleventh on several different machines -- and hence in the top ten overall).

#### <a name="data-aggregation"></a> Data Aggregation and Record Creation ####

The Binning Analytic defines how data is aggregated. For example, lines 42-47 of [TwitterTopicBinningAnalytic.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinningAnalytic.scala) compare two maps and creates a new map that contains:

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

##### <a name="custom-aggregation"></a> Custom Aggregation Methods #####

Lines 85-93  of [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) (found in the [same folder](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen) as the Binning Analytic) are used to calculate the minimum and maximum values and write them to the metadata by level. 

```scala
val minAnalysis:
		AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]],
							List[TwitterDemoTopicRecord]] =
	new TwitterTopicListAnalysis(new TwitterMinRecordAnalytic)

val maxAnalysis:
		AnalysisDescription[TileData[JavaList[TwitterDemoTopicRecord]],
							List[TwitterDemoTopicRecord]] =
	new TwitterTopicListAnalysis(new TwitterMaxRecordAnalytic)
```

Standard Bin Analytics are available in the [Analytics.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/analytics/Analytics.scala) file in [tile-generation/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>tilegen/<wbr>tiling/<wbr>analytics/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/analytics).

### <a name="serializer"></a> Serializer ###

The Serializer determines how to read and write to the tile set. The Tile Server requires the following supporting classes to use the Serializer:

- [Serializer](#serializer-class)
- [Serialization Factory](#serialization-factory)
- [Serialization Factory Module](#serialization-factory-module)

See the following sections for examples of each custom Serializer component.

#### <a name="serializer-class"></a> Serializer ####

The Serializer implements the [com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>TileSerializer](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/TileSerializer.java) interface. To read and write the Avro tiles that are most commonly used, it should inherit from:

- [com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>GenericAvroSerializer](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroSerializer.java) if your bin type is a single record 
- [com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>GenericAvroArraySerializer](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroArraySerializer.java) if your bin type is an array of records record. 

For an example of a serializer of tiles whose bins are an array of records, see the [TwitterTopicAvroSerializer.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/binning/TwitterTopicAvroSerializer.java) file in [tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>binning/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/binning).

This class inherits from the [GenericAVROArraySerializer.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroArraySerializer.java) ([binning-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>binning/<wbr>io/<wbr>serialization/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization)) and defines:

- **getEntrySchemaFile**, which points to a file containing the Avro description of a single record
- **setEntryValue**, which sets the value of one entry in the list from the Avro file
- **getEntryValue**, which retrieves the value of one entry in the list from the Avro file

The definition of the Avro schema is located in the [twitterTopicEntry.avsc](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/resources/twitterTopicEntry.avsc) file ([tile-examples/<wbr>twitter-topics<wbr>/twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>resources/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/resources)), where the **name** is set to *entryType*.

For records that aren't list types, inherit from the [GenericAvroSerializer.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroSerializer.java) ([binning-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>binning/<wbr>io/<wbr>serialization/](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroSerializer.java)) and define:

- **getRecordSchemaFile**
- **getValue**
- **setvalue**

The definition of the Avro schema can be based on the template in the [doubleData.avsc](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/resources/doubleData.avsc) file ([binning-utilities/<wbr>src/<wbr>main/<wbr>resources/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/binning-utilities/src/main/resources)), where the **name** is set to *recordType*.

#### <a name="serialization-factory"></a> Serialization Factory ####

The Serialization Factory gets configuration information (e.g., the Avro compression codec) and hands back the serializer of choice when needed. It also produces the factory and can be injected by Guice.

For an example Serialization Factory, see the [TwitterTileSerializationFactory.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterTileSerializationFactory.java) in [tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>init/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init).

#### <a name="serialization-factory-module"></a> Serialization Factory Module ####

The Factory Module tells Guice which factory providers to use to create serialization factories.

For an example Serialization Factory, see the [TwitterSerializationFactoryModule.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterSerializationFactoryModule.java) file in 
[tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>init/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init).

## <a name="binning-your-data"></a> Binning Your Data ##

There are three steps in binning your data:

1. Parsing your data into the form required by the Binner
2. Running the Binner to transform the data into tiles
3. Writing the tiles

For an example of a custom Binner, see the [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) file in [tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>tilegen/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen).

### <a name="parsing-data"></a> Parsing your Data ###

The Binner expects your data as pairs of **(index, record)**, where:

- **index** is an object indicating where in space the record lies
- **record** is a data record of the processing type your Binning Analytic defines

Several predefined index types are defined in the [IndexingScheme.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/IndexingScheme.scala) file in [tile-generation/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>tilegen/<wbr>tiling/](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/IndexingScheme.scala).

- Cartesian: the index type is a pair of doubles.
- IPv4: the index type is an array of 4 bytes: the 4 values in an IPv4 address.
- Time Range: the index type is three doubles, one that represents the time and two that represent cartesian coordinates

The end result of your parsing will therefore be:

```scala
val data: RDD[((Double, Double), PROCESSING_TYPE)]
```

Where **PROCESSING_TYPE** is the processing type from your [Binning Analytic](#binning-analytic).

Lines 149 - 164 in [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) retrieve the raw data from the Record Parser and create a mapping from (longitude, latitude) pairs to Twitter topic records.

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
).map(record => (record._1, record._2, dataAnalytics.map(_.convert(record))))
data.cache
```

### <a name="binning"></a> Binning ###

Lines 191 - 199 of [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) transform the data into tiles:

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

**Binner.processDataByLevel** is defined on line 230 in the [RDDBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala) file in [tile-generation/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>tilegen/<wbr>tiling/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling).

It accepts the following properties:

<div class="props">
	<dl class="detailList">
		<dt>data</dt>
		<dd>A distributed collection of (index, record) pairs as described above.</dd>

		<dt>indexScheme</dt>
		<dd>Used to convert the index to a set X/Y coordinates that can be plotted.	When using a CartesianIndexScheme, the coordinates are taken as given.</dd>

		<dt>binAnalytic</dt>
		<dd>A Binning Analytic that, as described above, defines how to aggregate two records, convert them into the form written and determine the extrema of the dataset.</dd>

		<dt>tileAnalytics</dt>
		<dd>Analytics used to perform custom aggregations on tile data (e.g., get the minimum and maximum values) and write them to the metadata by level.</dd>

		<dt>dataAnalytics</dt>
		<dd>Analytics used to perform custom aggregations on raw data that would otherwise be lost by the processing type (e.g., recording the maximum individual value) and write them to the metadata by level.</dd>

		<dt>tileScheme</dt>
		<dd>
			The projection to use to transform from the raw data index into tiles and bins. Two types are predefined:
			<ul>
				<li><a href="https://github.com/unchartedsoftware/aperture-tiles/blob/develop/binning-utilities/src/main/java/com/oculusinfo/binning/impl/AOITilePyramid.java">/binning-utilities/src/main/java/com/oculusinfo/binning/impl/AOITilePyramid</a>, which is a linear transformation into an arbitrarily sized space</li>
				<li><a href="https://github.com/unchartedsoftware/aperture-tiles/blob/develop/binning-utilities/src/main/java/com/oculusinfo/binning/impl/WebMercatorTilePyramid.java">/binning-utilities/src/main/java/com/oculusinfo/binning/impl/WebMercatorTilePyramid</a>, which is a standard geographical projection</li>
			</ul>
		</dd>

		<dt>levels</dt>
		<dd>Specifies which levels to process at the same time. It is generally recommended you process levels 1-9 together, then run additional levels	one	at a time afterward. This arrangement typically makes effective use of system resources.</dd>

		<dt>xBins (Optional)</dt>
		<dd>Number of bins on the x-axis. Defaults to 256</dd>

		<dt>yBins (Optional)</dt>
		<dd>Number of bins on the y-axis. Defaults to 256</dd>

		<dt>consolidationPartitions (Optional)</dt>
		<dd>The number of reducers to use when aggregating data records into bins and tiles. Defaults to the same number of partitions as the original data set. Alter if you encounter problems with the tiling job due to lack of resources.</dd>

		<dt>tileType (Optional)</dt>
		<dd>A specification of how data should be stored, <em>sparse</em> or <em>dense</em>. If not specified, a heuristic will use the optimal type for a double-valued tile. For significantly larger-valued types, <em>sparse</em> is recommended.</dd>
	</dl>
</div>

### <a name="writing-tiles"></a> Writing Tiles ###

Lines 200 - 207 of [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) specify how to write the tiles created from your transformed data.

```scala
tileIO.writeTileSet(tilePyramid,
				    pyramidId,
				    tiles,
				    new TwitterTopicAvroSerializer(CodecFactory.bzip2Codec()),
				    tileAnalytics,
				    dataAnalytics,
				    pyramidName,
				    pyramidDescription)
```

**tileIO.writeTileSet** is defined on line 172 in the [RDDBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala) file in [tile-generation/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>tilegen/<wbr>tiling/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling).

It accepts the following properties:

<div class="props">
	<dl class="detailList">
		<dt>tileScheme</dt>
		<dd>Type of projection built from the set of bins and levels. Must match the tileScheme specified in binner.processDataByLevel.</dd>

		<dt>writeLocation</dt>
		<dd>The ID to apply to the tile set when writing it. If writing to the local filesystem, this will be the base directory into which to write the tiles.	If writing to HBase, it will be the name of the table to write.</dd>

		<dt>tiles</dt>
		<dd>The binned data set produced by binner.processDataByLevel.</dd>

		<dt>serializer</dt>
		<dd>The serializer that determines how to read and write to the tile set.</dd>

		<dt>tileAnalytics (Optional)</dt>
		<dd>Analytics used to perform custom aggregations on tile data (e.g., get the minimum and maximum values) and write them to the metadata by level.</dd>

		<dt>dataAnalytics (Optional)</dt>
		<dd>Analytics used to perform custom aggregations on raw data that would otherwise be lost by the processing type (e.g., recording the maximum individual value) and write them to the metadata by level.</dd>

		<dt>name</dt>
		<dd>Name of the finished pyramid. Stored in the tile metadata.</dd>

		<dt>description</dt>
		<dd>Description of the finished pyramid. Stored in the tile metadata.</dd>
	</dl>
</div>

## Next Steps ##

For details on testing the output of your tiling job, see the [Testing Tiling Job Output](../test-output/) topic.