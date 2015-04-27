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

A typical Aperture Tiles application contains a heatmap layer that illustrates the density of numeric data at several levels over a map or plot. The process of generating the [tile pyramid](../tile-pyramid/) that represents this type of layer is a standard tiling job.

Standard tiling jobs can be executed with the [CSVBinner](#csvbinner), which is available in the Aperture Tiles source code.

For information on creating custom tiling jobs that ingest data that is not character delimited or contains non-numeric fields, see the [Run Custom Tiling Jobs](../custom-tiling/) topic.

## <a name="csvbinner"></a> CSVBinner ##

The CSVBinner ingests numeric, character-separated (e.g., CSV) tabular data. To define the tile set you want to create, you must pass two types of properties files to the CSVBinner:

- A [base properties](#base-properties) file, which describes the general characteristics of the data
- [Tiling properties](#tiling-properties) files, each of which describes the specific attributes you want to tile

During the tiling job, the CSVBinner writes a set of Avro tile data files to the location of your choice (HBase or local file system).

<h6 class="procedure">To execute the CSVBinner and run a standard tiling job</h6>

- Use the **spark-run** script and pass in the names of the properties files you want to use. For example:

	```bash
	spark-submit --class com.oculusinfo.tilegen.examples.apps.CSVBinner 
	lib/tile-generation-assembly.jar -d /data/twitter/dataset-base.bd 
	/data/twitter/dataset.lon.lat.bd
	```

	Where the `-d` switch specifies the base properties file path, and each subsequent file path specifies a tiling properties file.

## <a name="base-properties"></a> Base Properties Files ##

The base properties file describes the tiling job, the systems on which it will run and the general characteristics of the source data. The following properties must be defined in the file:

- [Connection Details](#connection-details)
- [Source Data Format](#source-data-format)
- [Source Data Manipulation](#source-data-manipulation)
- [Tile Storage](#tile-storage)

Advanced properties are described in the [Tile Generation](../../reference/generation/) reference topic.

### <a name="connection-details"></a> Connection Details ###

Indicate where your source data is stored.

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="32%">Property</th>
				<th scope="col" width="68%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="property">oculus.binning.source.location</td>
				<td class="description">Path (local file system or HDFS) to the source data files.</td>
			</tr>
		</tbody>
	</table>
</div>

### <a name="source-data-format"></a> Source Data Format ###

1. Indicate how the columns in your source data are separated:
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
					<td class="property">oculus.binning.parsing.separator</td>
					<td class="description">Character or string used to separate columns in the source data. Defaults to tab character (<em>\t</em>).</td>
				</tr>
			</tbody>
		</table>
	</div>
2. Pass in the fields used to write your data to the tile pyramid. The tile generation job requires two types of fields:
	- [Index](#index) fields indicate where on the map/plot your data points are located
	- A [value](#value) field contains the value of the data points at the corresponding indexes

	For example:

	```
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
					<td class="property">oculus.binning.parsing.&lt;field&gt;.index</td>
					<td class="description">Column number of the described field in the source data files. This property is mandatory for every field type to be used.</td>
				</tr>
				<tr>
					<td class="property">oculus.binning.parsing.&lt;field&gt;.fieldType</td>
					<td class="description">
						Type of value expected in the column specified by <strong>oculus.binning.parsing.&lt;field&gt;.index</strong>

						<br><br>By default, columns are treated as containing real, double-precision values. Other possible types are:
						<table width="100%">
							<tbody>
								<tr>
									<td width="50%">
										<ul>
											<li>constant</li>
											<li>zero</li>
											<li>int</li>
											<li>long</li>
											<li><a href="../../reference/generation/">date</a></li>
											<li>boolean</li>
										</ul>
									</td>
									<td width="50%">
										<ul>
											<li>byte</li>
											<li>short<lit>
											<li>float<lit>
											<li>ipv4</li>
											<li>string</li>
											<li><a href="../../reference/generation/">propertyMap</a></li>
										</ul>
									</td>
								</tr>
							</tbody>
						</table>
						<br>For more information on the supported field types, see the <a href="../../reference/generation/">Generation</a> reference topic.
					</td>
				</tr>
			</tbody>
		</table>
	</div>

### <a name="source-data-manipulation"></a> Source Data Manipulation ###

Additional properties are available for scaling fields logarithmically before they are used in the tile generation job. For more information, see the <a href="../../reference/generation/">Generation</a> reference topic.

### <a name="tile-storage"></a> Tile Storage ###

1. Specify where to save your tile set:
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
			</tbody>
		</table>
	</div>
2. If you are saving your tile set to HBase, specify the connection properties. Otherwise, skip to the next step.

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
3. Edit the tile set name and metadata values:
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
					<td class="property">oculus.binning.name</td>
					<td class="description">Name (path) of the output data tile set pyramid. If you are writing to a file system, use a relative path instead of an absolute path. If you are writing to HBase, this is used as a table name. This name is also written to the tile set metadata and used as a plot label.</td>
				</tr>
				<tr>
					<td class="property">oculus.binning.prefix</td>
					<td class="description">Optional prefix to be added to the name of every pyramid location. Used to distinguish different tile generation runs. If not present, no prefix is used.</td>
				</tr>
					<td class="property">oculus.binning.description</td>
					<td class="description">Description to put in the tile metadata.</td>
				</tr>
			</tbody>
		</table>
	</div>

## <a name="tiling-properties"></a> Tiling Properties Files ##

The tiling properties files define the tiling job parameters for each layer in your visual analytic, such as which fields to bin on and how values are binned:

- [Projection](#projection)
- [Index](#index)
- [Value](#value)
- [Levels](#levels)

Advanced properties are described in the [Tile Generation](../../reference/generation/) reference topic.

### <a name="projection"></a> Projection ###

The projection properties define the area on which your data points are plotted.

1. Choose the map or plot over which to project your data points:
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
					<td class="property">oculus.binning.projection.type</td>
					<td class="description">
						Type of projection to use when binning data. Possible values are:
						<ul>
							<li><em>EPSG:4326</em> (default) - Bin linearly over the whole range of values found.</li>
							<li><em>EPSG:900913</em> - Web-mercator projection. Used for geographic values only.</li>
						</ul>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
2. Decide whether to manually set the bounds of the projection:
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
					<td class="property">oculus.binning.projection.autobounds</td>
					<td class="description">
						Indicates how the minimum and maximum bounds should be set:
						<ul>
							<li><em>true</em> (default) - Automatically</li>
							<li><em>false</em> - Manually</li>
						</ul>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
3. If you chose to set the bounds manually, specify the minimum and maximum x- and y-axis values:
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
	</div>

### <a name="index"></a> Index ###

The index properties specify the fields used to locate the binning value on the projection and map them to the corresponding tile bins.

1. Specify the index scheme:
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
					<td class="property">oculus.binning.index.type</td>
					<td class="description">
						Scheme used to indicate how the binning values are mapped to the projection:
						<ul>
							<li><em>cartesian</em> (default) - Cartesian (x/y) coordinates</li>
							<li><em>ipv4</em> - IP address (v4)</li>
							<li><em>timerange</em> - Standard time range and cartesian point index</li>
							<li><em>segment</em> - Line segment with two cartesian end points</li>
						</ul>
						<br>The scheme also determines the number of index fields you must specify.
					</td>
				</tr>
			</tbody>
		</table>
	</div>
2. Map the index fields you specified in your base properties file to the fields required by the scheme you selected.

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

							<dt><em>segment</em>:</dt>
							<dd>
								oculus.binning.index.field.0=<em>&lt;x1_Field&gt;</em>
								oculus.binning.index.field.1=<em>&lt;y1_Field&gt;</em>
								oculus.binning.index.field.2=<em>&lt;x2_Field&gt;</em>
								oculus.binning.index.field.3=<em>&lt;y2_Field&gt;</em>
							</dd>
						</dl>
					</td>
				</tr>
			</tbody>
		</table>
	</div>

### <a name="value"></a> Value ###

The value properties specify the field used as the binning value.

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
				<td class="property">oculus.binning.value.field</td>
				<td class="description">Field to use as the bin value. Default counts entries only.</td>
			</tr>
			<tr>
				<td class="property">oculus.binning.value.type</td>
				<td class="description">
					<ul>
						<li>field</li>
						<li>series</li>
						<li>count</li>
					</ul>
				</td>
			</tr>
			<tr>
				<td class="property">oculus.binning.value.aggregation</td>
				<td class="description">
					Method of aggregation used on the bin values. Describes how values from multiple data points in the same bin should be aggregated together to create a single value for the bin:
					<ul>
						<li><em>min</em>: Select the minimum value</li>
						<li><em>max</em>: Select the maximum value</li>
						<li><em>log</em>: Treat the number as a logarithmic value. Aggregation of <em>a</em> and <em>b</em> is <em>log_base(base^a+base^b)</em>. Base is taken from property <strong>oculus.binning.parsing.&lt;field&gt;.fieldBase</strong> and defaults to <em>e</em>.</li>
					</ul>
				</td>
			</tr>
		</tbody>
	</table>
</div>

### <a name="levels"></a> Levels ###

The **oculus.binning.levels.&lt;order&gt;** array property describes how the tiling job executes the generation of the various zoom levels. For example, if you want to generate levels in three groups, you should include:

```
oculus.binning.levels.0
oculus.binning.levels.1
oculus.binning.levels.2
```

Each group should describe the zoom levels to bin simultaneously as a comma-separated list of individual integers or a range of integers (described as start-end). For example, "0-3,5" means levels 0, 1, 2, 3, and 5. 

This property is mandatory, and has no default.

#### Defining Level Sets ####

Which levels you should bin together depends both on the size of your cluster and your data. Note that if you include multiple level sets, the raw data is parsed once and cached for use with each level set.

Each binning job has two costs:

- **Overhead cost** is generally dominant from levels 0-8. Tiling these levels together will reduce job time.
- **Tiling cost** is dominant above level 8. You risk job failure out of memory errors when simultaneously binning these levels together due to the large number of tiles generated.

Therefore, our typical use case has:

```
binning.level.0=0-8
binning.level.1=9
binning.level.2=10
...
```

## Next Steps ##

For details on running custom tiling jobs, see the [Run Custom Tiling Jobs](../custom-tiling) topic.