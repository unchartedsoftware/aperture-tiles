---
section: Docs
subsection: Development
chapter: How-To
topic: Run Standard Tiling Jobs
permalink: docs/development/how-to/standard-tiling/
layout: chapter
---

Run Standard Tiling Jobs
========================

A typical Aperture Tiles application contains a heatmap layer that illustrates the density of numeric data at several levels over a map or plot. The process of generating the [tile pyramid](../../getting-started/tile-pyramid/) that represents this type of layer is a standard tiling job.

Standard tiling jobs can be executed with the [CSVBinner](#csvbinner), which is available in the Aperture Tiles source code.

For information on creating custom tiling jobs that ingest data that is not character delimited or contains non-numeric fields, see the [Run Custom Tiling Jobs](../../advanced/custom-tiling/) topic.

## CSVBinner ##

The CSVBinner ingests numeric, character-separated (e.g., CSV) tabular data. To define the tile set you want to create, you must pass two types of properties files to the CSVBinner:

- A [base properties](#base-properties-files) file, which describes the general characteristics of the data
- [Tiling properties](#tiling-properties-files) files, each of which describes the specific attributes you want to tile

During the tiling job, the CSVBinner writes a set of Avro tile data files to the location of your choice (HBase or local file system).

<h6 class="procedure">To execute the CSVBinner and run a standard tiling job</h6>

-   Use the **spark-submit** script and pass in the names of the properties files you want to use. For example:

    ```bash
    spark-submit --class com.oculusinfo.tilegen.examples.apps.CSVBinner 
    lib/tile-generation-assembly.jar -d /data/twitter/dataset-base.bd 
    /data/twitter/dataset.lon.lat.bd
    ```

    Where the `-d` switch specifies the base properties file path, and each subsequent file path specifies a tiling properties file.

## Base Properties Files ##

The base properties file describes the tiling job, the systems on which it will run and the general characteristics of the source data. The following properties must be defined in the file:

- [Source Location](#source-location)
- [Source Data Format](#source-data-format)
- [Source Data Manipulation](#source-data-manipulation)
- [Tile Storage](#tile-storage)

Additional properties are described in the advanced [Standard Tiling](../../advanced/standard-tiling/) topic.

### Source Location ###

-   Indicate where your source data is stored:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:32%;">Property</th>
                <th scope="col" style="width:68%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.source.location</td>
                <td class="description">
                    Filename or directory containing the source data. If set to a directory, all of its contents will be ingested. Example values:
                    <ul>
                        <li>file://data/test.data</li>
                        <li>hdfs://hadoop-s1/data/julia/500by200</li>
                        <li>datasets/julia</li>
                    </ul>
                </td>
            </tr>
        </tbody>
    </table>

### Source Data Format ###

1.  Indicate how the columns in your source data are separated:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.parsing.separator</td>
                <td class="description">Character or string used to separate columns in the source data. Defaults to tab character (<em>\t</em>).</td>
            </tr>
        </tbody>
    </table>

2.  Pass in the fields used to write your data to the tile pyramid. The tile generation job requires two types of fields:
    -   [Index](#index) fields indicate where on the map/plot your data points are located
    -   A [value](#value) field contains the value of the data points at the corresponding indexes

    For example:

    ```properties
    # Index Fields
    oculus.binning.parsing.longitude.index=0
    oculus.binning.parsing.longitude.fieldType=double
    oculus.binning.parsing.latitude.index=1
    oculus.binning.parsing.latitude.fieldType=double

    # Value Field
    oculus.binning.parsing.value.index=2
    oculus.binning.parsing.value.fieldType=double
    ```

    Where:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.parsing.&lt;field&gt;.index</td>
                <td class="description">
                    Column number of the described field in the source data files. This property is mandatory for every field type to be used.
                    <p><strong>NOTE</strong>: The <a href="#index">oculus.binning.index.type</a> you select in your Tiling properties file determines the number of index files you must include.</p>
                </td>
            </tr>
            <tr>
                <td class="property">oculus.binning.parsing.&lt;field&gt;.fieldType</td>
                <td class="description">
                    Type of value expected in the column specified by <strong>oculus.binning.parsing.&lt;field&gt;.index</strong>:
                    <table style="width:100%;">
                        <tbody>
                            <tr>
                                <td style="width:50%;">
                                    <ul>
                                        <li>double (*default*)</li>
                                        <li>constant</li>
                                        <li>zero</li>
                                        <li>int</li>
                                        <li>long</li>
                                        <li><a href="../../advanced/standard-tiling/#dateformat">date</a></li>
                                        <li>boolean</li>
                                    </ul>
                                </td>
                                <td style="width:50%;">
                                    <ul>
                                        <li>byte</li>
                                        <li>short</li>
                                        <li>float</li>
                                        <li>ipv4</li>
                                        <li>string</li>
                                        <li><a href="../../advanced/standard-tiling/#propertymap">propertyMap</a></li>
                                    </ul>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                    <br>For more information on the supported field types, see the advanced <a href="../../advanced/standard-tiling/#source-data-format">Standard Tiling</a> topic.
                </td>
            </tr>
        </tbody>
    </table>

### Source Data Manipulation ###

Additional properties are available for scaling fields logarithmically before they are used in the tile generation job. For more information, see the advanced <a href="../../advanced/standard-tiling/#field-scaling">Standard Tiling</a> topic.

### Tile Storage ###

1.  Specify where to save your tile set:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.tileio.type</td>
                <td class="description">
                    Location to which tiles are written:
                    <ul>
                        <li><em>file</em> (<em>default</em>): Writes .avro files to the local file system. <strong>NOTE</strong>: If this type is used on a distributed cluster, the file is saved to the worker node, not the machine that initiates the tiling job.</li>
                        <li><em>hbase</em>: Writes to HBase. For further HBase configuration properties, see the following step.</li>
                    </ul>
                </td>
            </tr>
        </tbody>
    </table>

2.  If you are saving your tile set to HBase, specify the connection properties. Otherwise, skip to the next step.

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">hbase.zookeeper.quorum</td>
                <td class="description">Zookeeper quorum location needed to connect to HBase.</td>
            </tr>

            <tr>
                <td class="property">hbase.zookeeper.port</td>
                <td class="description">Port through which to connect to zookeeper. Defaults to <em>2181</em>.</td>
            </tr>

            <tr>
                <td class="property">hbase.master</td>
                <td class="description">Location of the HBase master to which to write tiles.</td>
            </tr>
        </tbody>
    </table>

3.  Edit the tile set name and metadata values:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.name</td>
                <td class="description">Name (path) of the output data tile set pyramid. If you are writing to a file system, use a relative path instead of an absolute path. If you are writing to HBase, this is used as a table name. This name is also written to the tile set metadata and used as a plot label.</td>
            </tr>
            <tr>
                <td class="property">oculus.binning.prefix</td>
                <td class="description">Optional prefix to be added to the name of every pyramid location. Used to distinguish different tile generation runs. If not present, no prefix is used.</td>
            </tr>
            <tr>
                <td class="property">oculus.binning.description</td>
                <td class="description">Description to put in the tile metadata.</td>
            </tr>
        </tbody>
    </table>

## Tiling Properties Files ##

The tiling properties files define the tiling job parameters for each layer in your visual analytic, such as which fields to bin on and how values are binned:

- [Projection](#projection)
- [Index](#index)
- [Value](#value)
- [Levels](#levels)

Additional properties are described in the advanced [Standard Tiling](../../advanced/standard-tiling/) topic.

### Projection ###

The projection properties define the area on which your data points are plotted.

1.  Choose the map or plot over which to project your data points:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.projection.type</td>
                <td class="description">
                    Type of projection to use when binning data. Possible values are:
                    <ul>
                        <li><em>areaofinterest</em> or <em>EPSG:4326</em> (default) - Bin linearly over the whole range of values found.</li>
                        <li><em>webmercator</em>, <em>EPSG:900913</em> or <em>EPSG:3857</em> - Web-mercator projection. Used for geographic values only.</li>
                    </ul>
                </td>
            </tr>
        </tbody>
    </table>

2.  If your projection type is *areaofinterest*, decide whether to manually set the bounds of the projection.

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.projection.autobounds</td>
                <td class="description">
                    Indicates how the minimum and maximum bounds should be set:
                    <ul>
                        <li><em>true</em> (default) - Automatically</li>
                        <li><em>false</em> - Manually</li>
                    </ul>
                    Note that this property is not applicable to <em>webmercator</em> projection.
                </td>
            </tr>
        </tbody>
    </table>

3.  If you chose to set the bounds manually, specify the minimum and maximum x- and y-axis values:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.projection.minx</td>
                <td class="description">Lowest x-axis value</td>
            </tr>
            <tr>
                <td class="property">oculus.binning.projection.maxx</td>
                <td class="description">Highest x-axis value</td>
            </tr>
            <tr>
                <td class="property">oculus.binning.projection.miny</td>
                <td class="description">Lowest y-axis value</td>
            </tr>
            <tr>
                <td class="property">oculus.binning.projection.maxy</td>
                <td class="description">Highest y-axis value</td>
            </tr>
        </tbody>
    </table>

### Index ###

The index properties specify the fields used to locate the binning value on the projection and map them to the corresponding tile bins.

1.  Specify the index scheme:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.index.type</td>
                <td class="description">
                    Scheme used to indicate how the binning values are mapped to the projection:
                    <ul>
                        <li><em>cartesian</em> (default) - Cartesian (x/y) coordinates</li>
                        <li><em>ipv4</em> - IP address (v4)</li>
                        <li><em>timerange</em> - Standard time range and cartesian point index</li>
                    </ul>
                    <br>The scheme also determines the number of index fields you must specify.
                </td>
            </tr>
        </tbody>
    </table>

2.  Map the index fields you specified in your base properties file to the fields required by the scheme you selected.

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Property</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">oculus.binning.index.field.&lt;order&gt;</td>
                <td class="description">List of fields that satisfy the chosen index scheme:
                    <dl>
                        <dt><em>cartesian</em> (default):</dt>
                        <dd>
                            oculus.binning.index.field.0=<em>&lt;x_Field&gt;</em>
                            oculus.binning.index.field.1=<em>&lt;y_Field&gt;</em>
                        </dd>

                        <dt><em>ipv4</em>:</dt>
                        <dd>oculus.binning.index.field.0=<em>&lt;IPv4_Field&gt;</em></dd>

                        <dt><em>timerange</em>:</dt>
                        <dd>
                            oculus.binning.index.field.0=<em>&lt;time_Field&gt;</em>
                            oculus.binning.index.field.1=<em>&lt;x_Field&gt;</em>
                            oculus.binning.index.field.2=<em>&lt;y_Field&gt;</em>
                        </dd>
                    </dl>
                </td>
            </tr>
        </tbody>
    </table>

**NOTE**: An additional index scheme (*segment*) is available for tiling node and edge graph data. See the [Graph Tiling](../../advanced/graph-tiling/) topic for more information.

### Value ###

The value properties specify the field to use as the binning value and how multiple values in the same bin should be combined.

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Property</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">oculus.binning.value.type</td>
            <td class="description">
                Specifies how to determine the values written to each bin: 
                <ul>
                    <li><em>count</em> (<em>default</em>) - Count the number of records in a bin. If selected, no value field is required.</li>
                    <li><em>field</em> - Perform an aggregation on the values of all records in a bin to create a single value for the bin</li>
                    <li><em>series</em> - Save the values of all records in a bin to a dense array</li>
                </ul>
            </td>
        </tr>
        <tr>
            <td class="property">oculus.binning.value.field</td>
            <td class="description">Field to use as the bin value. If no value field is provided, the tiling job will write the count of records in each bin as the bin value.</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.value.valueType</td>
            <td class="description">
                Type of values stored in the value field:
                <ul>
                    <li><em>double</em> (<em>default</em>) - Real, double-precision floating-point numbers</li>
                    <li><em>int</em> - Integers</li>
                    <li><em>long</em> - Double-precision integers</li>
                    <li><em>float</em> - Floating-point numbers</li>
                </ul>
            </td>
        </tr>
        <tr>
            <td class="property">oculus.binning.value.aggregation</td>
            <td class="description">
                Method of aggregation used on the values of all records in a bin when <strong>oculus.binning.value.type</strong> = <em>field</em>. Creates a single value for the bin:
                <ul>
                    <li><em>sum</em> (<em>default</em>)- Sum the numeric values of all records in the bin.</li>
                    <li><em>min</em> - Select the minimum numeric value from the records in the bin.</li>
                    <li><em>max</em> - Select the maximum numeric value from the records in the bin.</li>
                    <li><em>mean</em> - Calculate the mean numeric value of all records in the bin.</li>
                    <li><em>stats</em> - Calculates the mean and standard deviation numeric values of all records in the bin.</li>
                </ul>
            </td>
        </tr>
    </tbody>
</table>

### Levels ###

The **oculus.binning.levels.&lt;order&gt;** array property describes how the tiling job executes the generation of the various zoom levels. For example, if you want to generate levels in three groups, you should include:

```properties
oculus.binning.levels.0
oculus.binning.levels.1
oculus.binning.levels.2
```

Each group should describe the zoom levels to bin simultaneously as a comma-separated list of individual integers or a range of integers (described as start-end). For example, "0-3,5" means levels 0, 1, 2, 3, and 5. 

This property is mandatory, and has no default.

#### Defining Level Sets ####

Which levels you should bin together depends both on the size of your cluster and your data. Note that if you include multiple level sets, the raw data is parsed once and cached for use with each level set.

Each binning job has two costs: **overhead** and **tiling**. In our cluster:

- **Overhead** cost is generally dominant from levels 0-8. Tiling these levels together will reduce job time.
- **Tiling** cost is dominant above level 8. There is a risk of out of memory job failure errors when simultaneously binning these levels together due to the large number of tiles generated.

Therefore, our typical use case has:

```properties
binning.level.0=0-8
binning.level.1=9
binning.level.2=10
```

## Next Steps ##

For details on testing the output of your tiling job, see the [Testing Tiling Job Output](../test-output/) topic.