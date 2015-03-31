---
section: Docs
subsection: Development
chapter: How-To
topic: Run Standard Tiling Jobs
permalink: docs/development/how-to/standard-tiling/
layout: submenu
---

Run Standard Tiling Jobs
========================

The following sections describe to generate a [tile pyramid](../tile-pyramid/) from your raw data using tools built in to the Aperture Tiles source code.

## <a name="csvbinner"></a> CSVBinner ##

A standard Aperture Tiles tiling job uses the built-in CSVBinner tool to process numeric, character-separated (e.g., CSV) tabular data and generate a set of tiles. To define the tile set you want to create, you must edit two types of properties files and pass them to the CSVBinner:

- A [base properties](#base-properties) file, which describes the general characteristics of the data
- [Tiling properties](#tiling-properties) files, each of which describes the specific attributes you want to tile

During the tiling job, the CSVBinner creates a collection of Avro tile data files in the location (HBase or local file system) specified in the base properties file.

### Executing the CSVBinner ###

To execute the CSVBinner and run a tiling job, use the **spark-run** script and pass in the names of the properties files you want to use. For example:

```bash
spark-submit --class com.oculusinfo.tilegen.examples.apps.CSVBinner 
lib/tile-generation-assembly.jar -d /data/twitter/dataset-base.bd 
/data/twitter/dataset.lon.lat.bd
```

Where the `-d` switch specifies the base properties file path, and each subsequent file path specifies a tiling properties file.

## <a name="base-properties"></a> Base Properties Files ##

The base properties file describes the tiling job, the systems on which it will run and the general characteristics of the source data. The following properties must be defined in the file:

- [Tile Storage](#tile-storage)
- [HBase Connection](#hbase-connection)
- [Source Data](#source-data)

### <a name="tile-storage"></a> Tile Storage ###

The tile storage properties indicate whether the tile set created from your source data should be stored in HBase or your local file system:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="20%">Property</th>
				<th scope="col" width="80%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">oculus.tileio.type</td>
				<td class="description">
					Location to which tiles are written:
					<ul>
						<li><em>hbase</em>: Writes to HBase. See the <a href="#hbase-connection">HBase Connection</a> section below for further HBase configuration properties</li>
						<li><em>file</em>: Writes to the local file system. This is the default.</li>
					</ul>
				</td>
			</tr>

			<tr>
				<td class="property">user</td>
				<td class="description">Optional username passed to the Spark job title. Defaults to the username of the current user.
				</td>
			</tr>
		</tbody>
	</table>
</div>

### <a name="hbase-connection"></a> HBase Connection ###

If you chose to store your tile set in HBase (i.e., **oculus.tileio.type** is set to *hbase*), these properties define how to connect to HBase:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="20%">Property</th>
				<th scope="col" width="80%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">hbase.zookeeper.quorum</td>
				<td class="description">Zookeeper quorum location needed to connect to HBase.</td>
			</tr>

			<tr>
				<td class="property">hbase.zookeeper.port</td>
				<td class="description">Port through which to connect to zookeeper.</td>
			</tr>

			<tr>
				<td class="property">hbase.master</td>
				<td class="description">Location of the HBase master to which to write tiles.</td>
			</tr>
		</tbody>
	</table>
</div>

### <a name="source-data"></a> Source Data ###

The Source Data properties describe the raw data from which you want to create tiles:

<div class="props">
	<dl class="detailList params">
		<dt>oculus.binning.source.location</dt>
		<dd>Path (local file system or HDFS) to the source data file or files to be tiled.</dd>

		<dt>oculus.binning.prefix (Optional)</dt>
		<dd>Prefix to be added to the name of every pyramid location. Used to separate this tile generation from previous runs. If not present, no prefix is used.</dd>

		<dt>oculus.binning.parsing.separator</dt>
		<dd>Character or string used as a separator between columns in the input data files. Default is a tab.</dd>

		<dt>oculus.binning.parsing.&lt;field&gt;.index</dt>
		<dd>Column number of the described field in the input data files. This field is mandatory for every field type to be used.</dd>

		<dt>oculus.binning.parsing.&lt;field&gt;.fieldType</dt>
		<dd>
			Type of value expected in the column specified by:
			<br><strong>oculus.binning.parsing.&lt;field&gt;.index</strong>.

			<br><br>By default columns are treated as containing real, double-precision values. Other possible types are:

			<dl>
				<dt>constant or zero</dt>
				<dd>Contains <em>0.0</em> (the column does not need to exist)</dd>

				<dt>int</dt>
				<dd>Contains integers</dd>

				<dt>long</dt>
				<dd>Contains double-precision integers</dd>

				<dt>date</dt>
				<dd>Contains dates. Dates are parsed and transformed into milliseconds since the standard Java start date (using SimpleDateFormatter). The default format is <em>yyMMddHHmm</em>, but this can be overridden using the <strong>oculus.binning.parsing.&lt;field&gt;.dateFormat</strong> property.</dd>

				<dt>boolean</dt>
				<dd>Contains boolean values (e.g., <em>true/false</em>, <em>yes/no</em>)</dd>

				<dt>byte</dt>
				<dd>Contains bytes</dd>

				<dt>short</dt>
				<dd>Contains short integers</dd>

				<dt>float</dt>
				<dd>Contains floating-point numbers</dd>

				<dt>ipv4</dt>
				<dd>Contains an IP address treated as a four-digit base 256 number turned into a double</dd>

				<dt>string</dt>
				<dd>Contains a string value</dd>

				<dt>propertyMap</dt>
				<dd>
					Contains property maps. All of the following properties must be	present	to read the property:
					<dl>
						<dt>oculus.binning.parsing.&lt;field&gt;.property</dt>
						<dd>Name of the property</dd>

						<dt>oculus.binning.parsing.&lt;field&gt;.propertyType</dt>
						<dd>Equivalent to fieldType</dd>

						<dt>oculus.binning.parsing.&lt;field&gt;.propertySeparator</dt>
						<dd>Character or string used to separate properties</dd>

						<dt>oculus.binning.parsing.&lt;field&gt;.propertyValueSeparator</dt>
						<dd>Character or string used to separate property keys from their values</dd>
					</dl>
				</dd>
			</dl>
		</dd>

		<dt>oculus.binning.parsing.&lt;field&gt;.fieldScaling</dt>
		<dd>
			How field values should be scaled. The default leaves values as they are. Other possibilities are:
			<dl>
				<dt>log</dt>
				<dd>take the log of the value (<strong>oculus.binning.parsing.&lt;field&gt;.fieldBase</strong> is used, just as with fieldAggregation)</dd>
			</dl>
		</dd>

		<dt>oculus.binning.parsing.&lt;field&gt;.fieldAggregation</dt>
		<dd>
			Method of aggregation used on values of the X field. Describes how values from multiple data points in the same bin should be aggregated together to create a single value for the bin.

			<br><br>The default is addition.  Other possible aggregation types are:

			<dl>
				<dt>min</dt>
				<dd>Find the minimum value</dd>

				<dt>max</dt>
				<dd>Find the maximum value</dd>

				<dt>log</dt>
				<dd>Treat the number as a logarithmic value; aggregation of a and b is log_base(base^a+base^b). Base is taken from property <strong>oculus.binning.parsing.&lt;field&gt;.fieldBase</strong>, and defaults to <em>e</em>.</dd>
			</dl>
		</dd>
	</dl>
</div>

## <a name="tiling-properties"></a> Tiling Properties Files ##

The tiling properties files define the tiling job parameters for each layer in your visual analytic, such as which fields to bin on and how values are binned:

<div class="props">
	<dl class="detailList">
		<dt>oculus.binning.name</dt>
		<dd>Name (path) of the output data tile set pyramid. If you are writing to a file system, use a relative path instead of an absolute path. If you are writing to HBase, this is used as a table name. This name is also written to the tile set metadata and used as a plot label.</dd>

		<dt>oculus.binning.projection</dt>
		<dd>
			Type of projection to use when binning data. Possible values are:
			<dl>
				<dt>EPSG:4326</dt>
				<dd>Bin linearly over the whole range of values found (default)</dd>

				<dt>EPSG:900913</dt>
				<dd>Web-mercator projection (used for geographic values only)</dd>
			</dl>
		</dd>

		<dt>oculus.binning.projection.autobounds</dt>
		<dd>
			Indicates whether the tiling job should set the minimum and maximum	bounds automatically (<em>true</em>, which is the default value) or whether you will specify them manually (<em>false</em>). If set to <em>false</em>, you must also specify:
			<dl>
				<dt>oculus.binning.projection.minx</dt>
				<dd>Indicates the lowest value that will be displayed on the x-axis.</dd>

				<dt>oculus.binning.projection.maxx</dt>
				<dd>Indicates the highest value that will be displayed on the x-axis.</dd>

				<dt>oculus.binning.projection.miny</dt>
				<dd>Indicates the lowest value that will be displayed on the y-axis.</dd>

				<dt>oculus.binning.projection.maxy</dt>
				<dd>Indicates the highest value that will be displayed on the y-axis.</dd>
			</dl>
		</dd>

		<dt>oculus.binning.index.type</dt>
		<dd>
			Defines the index scheme used to locate the binning value on the base layer and map them to the corresponding tile bins. The value you select determines the number of index fields you must specify:
			<dl>
				<dt>cartesian (default)</dt>
				<dd>
					Use cartesian (x/y) coordinates:
					<ul>
						<li>oculus.binning.index.field.0=&lt;x_Field&gt;</li>
						<li>oculus.binning.index.field.1=&lt;y_Field&gt;</li>
					</ul>
				</dd>

				<dt>ipv4</dt>
				<dd>
					Use an IP address (v4):
					<ul>
						<li>oculus.binning.index.field.0=&lt;IPv4_Field&gt;</li>
					</ul>
				</dd>

				<dt>timerange</dt>
				<dd>
					Use a standard time range and cartesian point index:
					<ul>
						<li>oculus.binning.index.field.0=&lt;time_Field&gt;</li>
						<li>oculus.binning.index.field.1=&lt;x_Field&gt;</li>
						<li>oculus.binning.index.field.2=&lt;y_Field&gt;</li>
					</ul>
				</dd>

				<dt>segment</dt>
				<dd>
					Use a line segment with two cartesian end points:
					<ul>
						<li>oculus.binning.index.field.0=&lt;x1_Field&gt;</li>
						<li>oculus.binning.index.field.1=&lt;y1_Field&gt;</li>
						<li>oculus.binning.index.field.2=&lt;x2_Field&gt;</li>
						<li>oculus.binning.index.field.3=&lt;y2_Field&gt;</li>
					</ul>
				</dd>
			</dl>
		</dd>

		<dt>oculus.binning.index.field.&lt;order&gt;</dt>
		<dd>
			Field(s) to use as the index that locates the binning value on the base layer and map them to the corresponding tile bins. Your index scheme will determine how many index fields you must provide:
			<dl>
				<dt>ipv4</dt>
				<dd>
					<ul>
						<li>oculus.binning.index.field.0=&lt;IPv4_Field&gt;</li>
					</ul>
				</dd>

				<dt>cartesian</dt>
				<dd>
					<ul>
						<li>oculus.binning.index.field.0=&lt;x_Field&gt;</li>
						<li>oculus.binning.index.field.1=&lt;y_Field&gt;</li>
					</ul>
				</dd>

				<dt>timerange</dt>
				<dd>
					<ul>
						<li>oculus.binning.index.field.0=&lt;time_Field&gt;</li>
						<li>oculus.binning.index.field.1=&lt;x_Field&gt;</li>
						<li>oculus.binning.index.field.2=&lt;y_Field&gt;</li>
					</ul>
				</dd>

				<dt>segment</dt>
				<dd>
					<ul>
						<li>oculus.binning.index.field.0=&lt;x1_Field&gt;</li>
						<li>oculus.binning.index.field.1=&lt;y1_Field&gt;</li>
						<li>oculus.binning.index.field.2=&lt;x2_Field&gt;</li>
						<li>oculus.binning.index.field.3=&lt;y2_Field&gt;</li>
					</ul>
				</dd>
			</dl>
		</dd>

		<dt>oculus.binning.value.field</dt>
		<dd>Field to use as the bin value. Default counts entries only.</dd>

		<dt>oculus.binning.levels.&lt;order&gt;</dt>
		<dd>
			Array property. For example, if you want to generate tile zoom levels in three groups, you should include:
			<ul>
				<li>oculus.binning.levels.0
				<li>oculus.binning.levels.1
				<li>oculus.binning.levels.2
			</ul>

			<p>Each group is a description of the zoom levels to bin simultaneously - a comma-separated list of individual integers, or ranges of integers (described as start-end). For examples, "0-3,5" means levels 0, 1, 2, 3, and 5. If there are multiple level sets, the raw data is parsed once and cached for use with each level set. This property is mandatory, and has no default.</p>

			<p>Which levels you should bin together depends both on the size of your cluster and data.</p>

			<p>Each binning job has an overhead cost and a tiling cost. Generally, the overhead cost is the dominant factor below level 8 and irrelevant above that. Tiling all levels below this point will save the overhead cost and reduce tile generation time. Above this level, you risk job failure out of memory errors if you try to simultaneously bin multiple levels due to the large number of tiles generated at lower levels.</p>

			<p>Our typical use case has:</p>

			<ul>
				<li>binning.level.0=0-8</li>
				<li>binning.level.1=9</li>
				<li>binning.level.2=10</li>
				<li>etc.</li>
			</ul>
		</dd>

		<dt>oculus.binning.consolidationPartitions</dt>
		<dd>The number of partitions into which to consolidate data when binning. If not included, Spark automatically selects the number of partitions.</dd>
	</dl>
</div>

## Next Steps ##

For details on running custom tiling jobs, see the [Run Custom Tiling Jobs](../custom-tiling) topic.