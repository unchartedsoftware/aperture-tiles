---
section: Docs
subsection: Development
chapter: Advanced
topic: Custom Tiling Jobs
permalink: docs/development/advanced/custom-tiling/
layout: chapter
---

Custom Tiling Jobs
==================

The following sections describe how to generate a tile pyramid from your raw data by creating your own custom generation code.

## Custom Tiling Jobs ##

If your source data is not character delimited or if it contains non-numeric fields, you may need to create custom code to parse it and create a tile pyramid. Creating custom tile generation code involves:

- [Defining Your Data Structure](#defining-your-data-structure) by specifying how to:
    - Structure your source data while it is:
        - Processed by the tiling job
        - Written to the Avro tile set
    - Combine multiple records that fall in the same tile bin
    - Read and write to the tile set
- [Binning Your Data](#binning-your-data) by specifying how to:
    - Parse your source data
    - Transform the parsed data into the Avro tile set
    - Store the Avro tile set to your preferred location

Once you have created the required components for each process, you can run your custom binner to create the Avro tile set you will use in your application.

For an example of custom code written for tile generation, see the Twitter Topics project in the source code ([tile-examples/<wbr>twitter-topics/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics)). This application displays a world map with the following Twitter data layers:

- Heatmap of Twitter messages by location
- Word cloud and bar chart overlays of the top words mentioned in tweets for each tile and bin

## Defining Your Data Structure ##

The following custom modules are required to determine how your data should be structured, aggregated and written:

- [Binning Analytic](#binning-analytic): defines how to process and aggregate your source data 
- [Serializer](#serializer): defines how to read and write your source data to the tile set

### Binning Analytic ###

The Binning Analytic is used throughout the tiling process. It should define:

- Two [data types](#data-types) used during the tiling job:
    1. Processing type
    2. Binning type
- The method of [aggregation](#data-aggregation-and-record-creation) for joining individual data records in a bin
 
For an example of a custom Bin Analytic, see the [TwitterTopicBinningAnalytic.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinningAnalytic.scala) file in [tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>tilegen/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen).

#### Data Types ####

During a tiling job, the Binning Analytic uses two types of data:

1. **Processing type** is used to process tiles and aggregate them together. It should contain all the information needed for aggregations performed during the job, even if it is not needed once the tile is complete.
2. **Binning type** is the final form written to the tile bins.

For example, to record an average, the two types might be used as follows:

1. **Processing type**: Count of records and the sum of their values. Both are continuously updated as new records are examined.
2. **Binning type**: Average calculated as the final processing type sum divided by the total count of records.

A code example is shown in line 40 of [TwitterTopicBinningAnalytic.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinningAnalytic.scala):

```scala
extends BinningAnalytic[Map[String, TwitterDemoTopicRecord], 
                        JavaList[TwitterDemoTopicRecord]]
```

The processing type is a *map* that adds all similar Twitter message topic records together, while the binning type is a *list* that contains only the topics with the highest counts.

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

While the remaining topics are eventually discarded, they are necessary during processing. For example, a topic that is recorded as the 11th most used topic on several different machines is likely to be in the top 10 once all the data is aggregated together.

#### Data Aggregation and Record Creation ####

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

##### Custom Aggregation Methods #####

Lines 85-93 of [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) (found in the [same folder](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen) as the Binning Analytic) are used to calculate the minimum and maximum values and write them to the metadata by level. 

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

### Serializer ###

The Serializer determines how to read and write to the tile set. The Tile Server requires the following supporting classes to use the Serializer:

- [Serializer](#serializer-class)
- [Serialization Factory](#serialization-factory)
- [Serialization Factory Module](#serialization-factory-module)

See the following sections for examples of each custom Serializer component.

#### Serializer Class ####

The Serializer implements the [com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>TileSerializer](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/TileSerializer.java) interface. To read and write the Avro tiles that are most commonly used, it should inherit from:

- [com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>GenericAvroSerializer](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroSerializer.java) if your bin type is a single record
- [com.<wbr>oculusinfo.<wbr>binning.<wbr>io.<wbr>serialization.<wbr>GenericAvroArraySerializer](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroArraySerializer.java) if your bin type is an array of records

For an example of a serializer of tiles whose bins are an array of records, see the [TwitterTopicAvroSerializer.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/binning/TwitterTopicAvroSerializer.java) file in [tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>binning/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/binning).

This class inherits from the [GenericAvroArraySerializer.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroArraySerializer.java) and defines:

- **getEntrySchemaFile**, which points to a file containing the Avro description of a single record
- **setEntryValue**, which sets the value of one entry in the list from the Avro file
- **getEntryValue**, which retrieves the value of one entry in the list from the Avro file

The definition of the Avro schema is located in the [twitterTopicEntry.avsc](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/resources/twitterTopicEntry.avsc) file ([tile-examples/<wbr>twitter-topics<wbr>/twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>resources/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/resources)), where the **name** is set to *entryType*.

For records that aren't list types, inherit from the [GenericAvroSerializer.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/java/com/oculusinfo/binning/io/serialization/GenericAvroSerializer.java) and define:

- **getRecordSchemaFile**
- **getValue**
- **setvalue**

The definition of the Avro schema can be based on the template in the [doubleData.avsc](https://github.com/unchartedsoftware/aperture-tiles/blob/master/binning-utilities/src/main/resources/doubleData.avsc) file ([binning-utilities/<wbr>src/<wbr>main/<wbr>resources/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/binning-utilities/src/main/resources)), where the **name** is set to *recordType*.

#### Serialization Factory ####

The Serialization Factory gets configuration information (e.g., the Avro compression codec) and hands back the serializer of choice when needed. It also produces the factory and can be injected by Guice.

For an example Serialization Factory, see the [TwitterTileSerializationFactory.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterTileSerializationFactory.java) in [tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>init/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init).

#### Serialization Factory Module ####

The Factory Module tells Guice which factory providers to use to create serialization factories.

For an example Serialization Factory, see the [TwitterSerializationFactoryModule.java](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init/TwitterSerializationFactoryModule.java) file in 
[tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>init/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/java/com/oculusinfo/twitter/init).

## Binning Your Data ##

There are three steps in binning your data:

1. Parsing your data into the form required by the Binner
2. Running the Binner to transform the data into tiles
3. Writing the tiles

For an example of a custom Binner, see the [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) file in [tile-examples/<wbr>twitter-topics/<wbr>twitter-topics-utilities/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>twitter/<wbr>tilegen/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/).

### Parsing your Data ###

The Binner expects your data as pairs of **(index, record)**, where:

- **index** is an object indicating where in space the record lies
- **record** is a data record of the processing type your Binning Analytic defines

Several predefined index types are defined in the [IndexingScheme.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/IndexingScheme.scala) file in [tile-generation/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>tilegen/<wbr>tiling/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/).

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

### Binning ###

Lines 190 - 198 of [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) transform the data into tiles:

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

**processDataByLevel** is defined on line 231 in the [RDDBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala) file in [tile-generation/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>tilegen/<wbr>tiling/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling). It accepts the following properties:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:25%;">Property</th>
            <th scope="col" style="width:10%;">Required?</th>
            <th scope="col" style="width:65%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">data</td>
            <td class="description">Yes</td>
            <td class="description">Distributed collection of (index, record) pairs as described above.</td>
        </tr>
        <tr>
            <td class="property">indexScheme</td>
            <td class="description">Yes</td>
            <td class="description">Converts the index to a set of x/y coordinates that can be plotted. When using a CartesianIndexScheme, the coordinates are taken as given.</td>
        </tr>
        <tr>
            <td class="property">binAnalytic</td>
            <td class="description">Yes</td>
            <td class="description">Binning Analytic that, as described above, defines how to aggregate two records, convert them into the form written and determine the extrema of the dataset.</td>
        </tr>
        <tr>
            <td class="property">tileAnalytics</td>
            <td class="description">No</td>
            <td class="description">Analytics used to perform custom aggregations on tile data (e.g., get the minimum and maximum values) and write them to the metadata by level.</td>
        </tr>
        <tr>
            <td class="property">dataAnalytics</td>
            <td class="description">No</td>
            <td class="description">Analytics used to perform custom aggregations on raw data that would otherwise be lost by the processing type (e.g., recording the maximum individual value) and write them to the metadata by level.</td>
        </tr>
        <tr>
            <td class="property">tileScheme</td>
            <td class="description">Yes</td>
            <td class="description">
                Projection used to transform from the raw data index into tiles and bins. Two types are predefined:
                <ul>
                    <li><a href="https://github.com/unchartedsoftware/aperture-tiles/blob/develop/binning-utilities/src/main/java/com/oculusinfo/binning/impl/AOITilePyramid.java">binning-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>binning/<wbr>impl/<wbr>AOITilePyramid</a>, which is a linear transformation into an arbitrarily sized space</li>
                    <li><a href="https://github.com/unchartedsoftware/aperture-tiles/blob/develop/binning-utilities/src/main/java/com/oculusinfo/binning/impl/WebMercatorTilePyramid.java">binning-utilities/<wbr>src/<wbr>main/<wbr>java/<wbr>com/<wbr>oculusinfo/<wbr>binning/<wbr>impl/<wbr>WebMercatorTilePyramid</a>, which is a standard geographical projection</li>
                </ul>
            </td>
        </tr>
        <tr>
            <td class="property">levels</td>
            <td class="description">Yes</td>
            <td class="description">Specifies which levels to process at the same time. It is generally recommended you process levels 1-9 together, then run additional levels one at a time afterward. This typically makes effective use of system resources.</td>
        </tr>
        <tr>
            <td class="property">xBins</td>
            <td class="description">No</td>
            <td class="description">Number of bins on the x-axis. Defaults to 256</td>
        </tr>
        <tr>
            <td class="property">yBins</td>
            <td class="description">No</td>
            <td class="description">Number of bins on the y-axis. Defaults to 256</td>
        </tr>
        <tr>
            <td class="property">consolidationPartitions</td>
            <td class="description">No</td>
            <td class="description">Number of reducers to use when aggregating data records into bins and tiles. Alter if you encounter problems with the tiling job due to lack of resources. Defaults to the same number of partitions as the original dataset.</td>
        </tr>
        <tr>
            <td class="property">tileType</td>
            <td class="description">No</td>
            <td class="description">Specifies how data should be stored: <em>sparse</em> or <em>dense</em>. If not specified, a heuristic will use the optimal type for a double-valued tile. For significantly larger-valued types, <em>sparse</em> is recommended.</td>
        </tr>
    </tbody>
</table>

### Writing Tiles ###

Lines 199 - 206 of [TwitterTopicBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-examples/twitter-topics/twitter-topics-utilities/src/main/scala/com/oculusinfo/twitter/tilegen/TwitterTopicBinner.scala) specify how to write the tiles created from your transformed data.

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

**tileIO.writeTileSet** is defined on line 173 in the [RDDBinner.scala](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling/RDDBinner.scala) file in [tile-generation/<wbr>src/<wbr>main/<wbr>scala/<wbr>com/<wbr>oculusinfo/<wbr>tilegen/<wbr>tiling/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/tiling).

It accepts the following properties:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:25%;">Property</th>
            <th scope="col" style="width:10%;">Required?</th>
            <th scope="col" style="width:65%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">tileScheme</td>
            <td class="description">Yes</td>
            <td class="description">Type of projection built from the set of bins and levels. Must match the tileScheme specified in binner.processDataByLevel.</td>
        </tr>
        <tr>
            <td class="property">writeLocation</td>
            <td class="description">Yes</td>
            <td class="description">
                ID to apply to the tile set when writing it. Value will vary depending on where you write your tile set:
                <ul>
                    <li>For the local filesystem: the base directory into which to write the tiles.</li>
                    <li>For HBase: the name of the table to write.</li>
                </ul>
            </td>
        </tr>
        <tr>
            <td class="property">tiles</td>
            <td class="description">Yes</td>
            <td class="description">Binned dataset produced by binner.processDataByLevel.</td>
        </tr>
        <tr>
            <td class="property">serializer</td>
            <td class="description">Yes</td>
            <td class="description">Serializer that determines how to read and write to the tile set.</td>
        </tr>
        <tr>
            <td class="property">tileAnalytics</td>
            <td class="description">No</td>
            <td class="description">Analytics used to perform custom aggregations on tile data (e.g., get the minimum and maximum values) and write them to the metadata by level.</td>
        </tr>
        <tr>
            <td class="property">dataAnalytics</td>
            <td class="description">No</td>
            <td class="description">Analytics used to perform custom aggregations on raw data that would otherwise be lost by the processing type (e.g., recording the maximum individual value) and write them to the metadata by level.</td>
        </tr>
        <tr>
            <td class="property">name</td>
            <td class="description">Yes</td>
            <td class="description">Name of the finished pyramid. Stored in the tile metadata.</td>
        </tr>
        <tr>
            <td class="property">description</td>
            <td class="description">Yes</td>
            <td class="description">Description of the finished pyramid. Stored in the tile metadata.</td>
        </tr>
    </tbody>
</table>