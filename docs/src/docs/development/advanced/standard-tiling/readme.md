---
section: Docs
subsection: Development
chapter: Advanced
topic: Standard Tiling Jobs
permalink: docs/development/advanced/standard-tiling/
layout: chapter
---

Standard Tiling Jobs
====================

The following sections describe advanced properties available for [standard tiling jobs](../../how-to/standard-tiling/) executed with the [CSVBinner](../../how-to/standard-tiling/#csvbinner).

- [Source Data Format](#source-data-format)
- [Field Scaling](#field-scaling)
- [Consolidation Partitions](#consolidation-partitions)

## Source Data Format ##

The **oculus.<wbr>binning.<wbr>parsing.<wbr>&lt;field&gt;.<wbr>fieldType** property indicates the type of values stored in a column that the CSVBinner will parse. Possible types include:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Value</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="value">double</td>
            <td class="description">Real, double-precision floating-point numbers. This is the default type.</td>
        </tr>
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
            <td class="description">Dates parsed and transformed into milliseconds since the standard Java start date (using SimpleDateFormatter). Expects the format <em>yyMMddHHmm</em>. Override the default format using the <a href="#dateformat">oculus.binning.parsing.&lt;field&gt;.dateFormat</a> property.</td>
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
            <td class="description">Floating-point numbers</td>
        </tr>
        <tr>
            <td class="value">ipv4</td>
            <td class="description">Contains an IP address treated as a four-digit base 256 number turned into an array of four bytes</td>
        </tr>
        <tr>
            <td class="value">string</td>
            <td class="description">String value</td>
        </tr>
        <tr>
            <td class="value">propertyMap</td>
            <td class="description">Property maps. Requires the presence of an additional set of <a href="#propertymap">propertyMap fields</a>.</td>
        </tr>
    </tbody>
</table>

### dateFormat ###

The following property overrides the default date format (*yyMMddHHmm*) expected when you specify a field type to be *date*.

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Property</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">oculus.binning.parsing.&lt;field&gt;.dateFormat</td>
            <td class="description">Specifies the date format expected by the corresponding field (e.g., <em>yyyy-MM-dd HH:mm:ss</em>).</td>
        </tr>
    </tbody>
</table>

### propertyMap ###

The following properties are required when you specify a field type to be *propertyMap*:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Property</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">oculus.binning.parsing.&lt;field&gt;.property</td>
            <td class="description">Name of the property</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.parsing.&lt;field&gt;.propertyType</td>
            <td class="description">Equivalent to fieldType</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.parsing.&lt;field&gt;.propertySeparator</td>
            <td class="description">Character or string used to separate properties</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.parsing.&lt;field&gt;.propertyValueSeparator</td>
            <td class="description">Character or string used to separate property keys from their values</td>
        </tr>
    </tbody>
</table>

For example, consider the following propertyMap setup:

```properties
oculus.binning.parsing.&lt;field&gt;.property=name
oculus.binning.parsing.&lt;field&gt;.propertyType=double
oculus.binning.parsing.&lt;field&gt;.propertyTypeSeparator=;
oculus.binning.parsing.&lt;field&gt;.propertyValueSeparator=,
```

In this case, a field value of `id=123;name=foo;description=bar` would yield the value *foo*.

## Field Scaling ##

The following properties enable you modify the values of a field before it is used for binning:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Property</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">oculus.binning.parsing.&lt;field&gt;.fieldScaling</td>
            <td class="description">
                How field values should be scaled. The default leaves values as they are. Other possibilities are:
                <ul>
                    <li><em>log</em>: take the log of the value. The base of the logarithm is taken from <strong>oculus.binning.parsing.&lt;field&gt;.fieldBase</strong>.</li>
                </ul>
            </td>
        </tr>
        <tr>
            <td class="property">oculus.binning.parsing.&lt;field&gt;.fieldBase</td>
            <td class="description">Base of the logarithm used to scale field values.</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.parsing.&lt;field&gt;.fieldAggregation</td>
            <td class="description">
                Method of aggregation used on values of field. Describes how values from multiple data points in the same bin should be aggregated together to create a single value for the bin.

                <p>The default is addition. Other possible aggregation types are:</p>
                <ul>
                    <li><em>min</em>: find the minimum value</li>
                    <li><em>max</em>: find the maximum value</li>
                    <li><em>log</em>: treat the number as a logarithmic value; aggregation of a and b is log_base(base^a+base^b). Base is taken from property <strong>oculus.binning.parsing.&lt;field&gt;.fieldBase</strong>, and defaults to <em>e</em>.</li>
                </ul>
            </td>
        </tr>
    </tbody>
</table>

## Consolidation Partitions ##

The **oculus.binning.consolidationPartitions** property controls the number of partitions into which data is consolidated when binning.

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Property</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">oculus.binning.consolidationPartitions</td>
            <td class="description">The number of partitions into which to consolidate data when binning. If not included, Spark automatically selects the number of partitions.</td>
        </tr>
    </tbody>
</table>