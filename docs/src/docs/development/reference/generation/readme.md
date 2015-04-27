---
section: Docs
subsection: Development
chapter: Reference
topic: Generation
permalink: docs/development/reference/generation/
layout: submenu
---

Run Standard Tiling Jobs
========================

A typical Aperture Tiles application contains a heatmap layer that illustrates the density of numeric data at several levels over a map or plot. The process of generating the [tile pyramid](../tile-pyramid/) that represents this type of layer is a standard tiling job.

Standard tiling jobs can be executed with the [CSVBinner](#csvbinner), which is available in the Aperture Tiles source code.

For information on creating custom tiling jobs that ingest data that is not character delimited or contains non-numeric fields, see the [Run Custom Tiling Jobs](../custom-tiling/) topic.

## <a name="base-properties"></a> Base Properties Files ##

The base properties file describes the tiling job, the systems on which it will run and the general characteristics of the source data. The following properties must be defined in the file:

- [Source Data Format](#source-data-format)
- [Source Data Manipulation](#source-data-manipulation)

### <a name="source-data-format"></a> Source Data Format ###

**oculus.binning.parsing.&lt;field&gt;.fieldType**. By default columns are treated as containing real, double-precision values. Other possible types are:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<tr>
				<th scope="col" width="20%">Value</th>
				<th scope="col" width="80%">Description</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="value">constant</td>
				<td class="description" rowspan="2"><em>0.0</em>. The column does not need to exist.</td>
			</tr>
			<tr>
				<td class="value">zero</td>
			</tr>
			<tr>
				<td class="value">int</td>
				<td class="description">Integers</td>
			</tr>
			<tr>
				<td class="value">long</td>
				<td class="description">Double-precision integers</td>
			</tr>
			<tr>
				<td class="value">date</td>
				<td class="description">Dates parsed and transformed into milliseconds since the standard Java start date (using SimpleDateFormatter). Defaults to <em>yyMMddHHmm</em>. Override the default format using the <strong>oculus.binning.parsing.&lt;field&gt;.dateFormat</strong> property.</td>
			</tr>
			<tr>
				<td class="value">boolean</td>
				<td class="description">Boolean values (e.g., <em>true/false</em>, <em>yes/no</em>)</td>
			</tr>
			<tr>
				<td class="value">byte</td>
				<td class="description">Bytes</td>
			</tr>
			<tr>
				<td class="value">short</td>
				<td class="description">Short integers</td>
			</tr>
			<tr>
				<td class="value">float</td>
				<td class="description">Floating-point numbers</dd>
			</tr>
			<tr>
				<td class="value">ipv4</td>
				<td class="description">Contains an IP address treated as a four-digit base 256 number turned into a double</td>
			</tr>
			<tr>
				<td class="value">string</td>
				<td class="description">String value</td>
			</tr>
			<tr>
				<td class="value">propertyMap</td>
				<td class="description">Property maps. All of the following properties must be	present	to read the property:</td>
			</tr>
		</tbody>
	</table>
</div>

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
				<td class="property">oculus.binning.parsing.&lt;field&gt;.property</td>
				<td class="description">Name of the property</td>

				<td class="property">oculus.binning.parsing.&lt;field&gt;.propertyType</td>
				<td class="description">Equivalent to fieldType</td>

				<td class="property">oculus.binning.parsing.&lt;field&gt;.propertySeparator</td>
				<td class="description">Character or string used to separate properties</td>

				<td class="property">oculus.binning.parsing.&lt;field&gt;.propertyValueSeparator</td>
				<td class="description">Character or string used to separate property keys from their values</td>
			</tr>
		</tbody>
	</table>
</div>

### Field Scaling ###

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
	<br><br>The default is addition. Other possible aggregation types are:
	<dl>
		<dt>min</dt>
		<dd>Find the minimum value</dd>
		<dt>max</dt>
		<dd>Find the maximum value</dd>
		<dt>log</dt>
		<dd>Treat the number as a logarithmic value; aggregation of a and b is log_base(base^a+base^b). Base is taken from property <strong>oculus.binning.parsing.&lt;field&gt;.fieldBase</strong>, and defaults to <em>e</em>.</dd>
	</dl>
</dd>

## <a name="tiling-properties"></a> Tiling Properties Files ##

		<dt>oculus.binning.consolidationPartitions</dt>
		<dd>The number of partitions into which to consolidate data when binning. If not included, Spark automatically selects the number of partitions.</dd>
	</dl>
</div>

## Next Steps ##

For details on running custom tiling jobs, see the [Run Custom Tiling Jobs](../custom-tiling) topic.