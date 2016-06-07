---
section: Docs
subsection: Development
chapter: How-To
topic: Configure the Tile Server
permalink: docs/development/how-to/tile-server/
layout: chapter
---

Configure the Tile Server
=========================

Once you have copied an existing Aperture Tiles application as a template, you should customize the Tile Server, which passes your tile data to the Client.

The Tile Server in your new template relies on the following configuration files:

- [Web XML](#web-xml), which defines which modules Guice will use.
- [Tile Properties](#tile-properties), which specifies constants used by Guice during initialization, including the location of your Spark installation, and the location of some server configuration files.
- [Layers](#layers), which defines the individual layers of data that can be overlaid on your base map. The layers file also indicates whether rendering should be performed by the server or the client.

The remainder of the configuration, which includes instantiation of the map, its base features and axis configuration, should be handled in the [Application JavaScript](../tile-client/#application-javascript) (*/src/main/webapp/js/***app.js**).

## Web XML ##

Edit the client **web.xml** file in *new-project/src/main/webapp/WEB-INF/*:

1. If you performed a custom tile generation, edit the guice-modules parameter to pass in any custom modules you created (e.g., your custom Tile Serialization Factory).
2. If required, uncomment the relevant Spark lines in the guice-modules to enable live tiling or drill-through to raw data.

## Tile Properties ##

Edit the client **tile.properties** file in *new-project/src/main/resources/*. This file specifies parameters for use by Guice, such as the location of your layer and annotation directories. Additional Spark parameters are available for on-demand tile generation and data drill down:

- The location of your Spark master
- The name under which the Aperture Tiles web service should appear in the Spark web interface.
- The home directory of Spark
- Any JARs you want to add to the Spark context

## Layers ##

The layers file describes the tile layers to be made available to the server and client application. Parameters in the layers file are split into two sections: those in the public node are accessible from the client, while those under the private note are not.

Layer file examples can be found in in the Julia example at [tile-examples\julia-demo\src\main\resources\layers](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/julia-demo/src/main/resources/layers) and the Twitter Topics example at [tile-examples\twitter-topics\twitter-topics-client\src\main\resources\layers](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-client/src/main/resources/layers).

### ID ###

The ID parameter uniquely identifies the layer.

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Property</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">id</td>
            <td class="description">Layer identification string, which is used in all layer-related REST calls. Must conform to JSON property name format.</td>
        </tr>
    </tbody>
</table>

### Public Parameters ###

Parameters in the **public** node section of the **layers.json** file are accessible from the client.

#### Pyramid ####

The pyramid parameters describe the extent of the data in the layer. The values that you provide in this section must match the values in your data source and in your map configuration.

For cross-plot maps, the `type` should always be set to *AreaOfInterest*. Also include the minimum and maximum values on the X and Y axes in your cross-plot. 

```json
pyramid: {
    type: "AreaOfInterest",
    minX : 1,
    maxX : 6336769,
    minY : 0,
    maxY : 500000
},
```

For geographic maps, the `type` should always be set to *WebMercator*. No minimum and maximum X and Y values are required for this layer type.

```json
pyramid: {
    type: "WebMercator"
},
```

**NOTE**: Your layer and map pyramid configurations much match each other.

#### Renderers ####

The **renderer** defines how tiles are rendered on the server side. Renderers are dependent on the type of tile data. The renderer options are:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Property</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">type</td>
            <td class="description">
                <ul>
                    <li><em>'heatmap'</em>: Renders to a heatmap, based on a standard Avro double-valued tile</li>
                    <li><em>'doubleseries'</em>: Renders to a series of heatmaps, based on a standard Avro double-series-valued tile</li>
                    <li><em>'doublestatistics'</em>: Renders tile's total hit and % coverage as text to the tile</li>
                    <li><em>'textscore'</em>: Renders to an image showing scored words, with bars based on their score</li>
                </ul>
            </td>
        </tr>

        <tr>
            <td class="property">ramp</td>
            <td class="description">
                Determines the color scale applied to the data points based on their concentration. The default color scales are:
                <ul>
                    <li><em>'hot'</em>: Warm orange ramp.</li>
                    <li><em>'neutral'</em>: Black-grey-white ramp.</li>
                    <li><em>'cool'</em>: Cool blue ramp.</li>
                    <li><em>'spectral'</em>: Red-green-yellow ramp.</li>
                    <li><em>'flat'</em>: Single color (white) ramp.</li>
                </ul>
            </td>
        </tr>

        <tr>
            <td class="property">rangeMin</td>
            <td class="description">Minimum percentage to clamp the low end of the color ramp.</td>
        </tr>

        <tr>
            <td class="property">rangeMax</td>
            <td class="description">Maximum percentage to clamp the low end of the color ramp.</td>
        </tr>

        <tr>
            <td class="property">opacity</td>
            <td class="description">Opacity of the rendered tile layer expressed as a decimal ranging from <em>0</em> (completely transparent) to <em>1</em> (completely opaque).</td>
        </tr>

        <tr>
            <td class="property">enabled</td>
            <td class="description">Indicates whether the layer is enabled on load.</td>
        </tr>
    </tbody>
</table>

#### Value Transformer ####

The **valueTransform** defines the **type** of transformation that can be applied to the data values when determining color:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Value</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="value">minmax</td>
            <td class="description">Do not change the value, but allow capping within min and max values.</td>
        </tr>

        <tr>
            <td class="value">sigmoid</td>
            <td class="description">
                Apply a sigmoid function to each value to make them fall between fixed values. With data that can be infinitely large in either direction, this can be used to bring it into finite, displayable bounds. 
                
                <p>Uses the following properties to indicate the minimum and maximum expected values while allowing values infinitely above or below them.</p>
                <ul>
                    <li><strong>layerMin</strong></li>
                    <li><strong>layerMax</strong></li>
                </ul>
                These two values should be symmetric around the desired central value.
            </td>
        </tr>

        <tr>
            <td class="value">half-sigmoid</td>
            <td class="description">
                Apply a sigmoid function to each value to make them fall between fixed values. With data that can be infinitely large in one direction only, this can be used to bring it into finite, displayable bounds.

                <p>Uses the following properties:</p>
                <ul>
                    <li><strong>layerMin</strong> corresponds to the minimum allowed data value</li>
                    <li><strong>layerMax</strong> indicates the maximum expected data value while allowing values infinitely above it</li>
                </ul>
            </td>
        </tr>

        <tr>
            <td class="value">log10</td>
            <td class="description">Take the log (base 10) of each value. Useful when displaying data that can be large (but not infinite) in one direction only. Uses the <em>layerMax</em> property to indicate the maximum.</td>
        </tr>
    </tbody>
</table>

### Private Parameters ###

Parameters in the **private** node section of the **layers.json** file are not accessible from the client.

#### Data ####

The data parameters specify the location of the tiles that you created. If you are using HBase, separate parameters are required.

<div class="props">
    <dl class="detailList">
        <dt>id</dt>
        <dd>
            Must match the name of the folder to which the tiles were saved during the generation process, which is composed of the following parameters from the Tiling Property File:

            <br><br>&lt;oculus.binning.name&gt;.&lt;oculus.binning.xField&gt;.&lt;oculus.binning.yField&gt;.&lt;oculus.binning.valueField&gt;

            <br><br>Where oculus.binning.yField is set to 0 if no yField name is specified.
        </dd>

        <dt>pyramidio</dt>
        <dd>
            <dl>
                <dt>type</dt>
                <dd>Indicates the file format of your tiles:
                    <dl>
                        <dt>hbase</dt>
                        <dd>Tiles are stored in HBase</dd>

                        <dt>file</dt>
                        <dd>Tiles are stored in an uncompressed directory in a local filesystem</dd>

                        <dt>zip</dt>
                        <dd>Tiles are stored in compressed file in a local filesystem</dd>
                    </dl>
                </dd>

                <dt>root.path</dt>
                <dd>Root path in which the tiles are stored. Not used for HBase.</dd>

                <dt>extension</dt>
                <dd>Name of the compressed file in which tiles are stored. Only used for ZIP
                files.</dd>

                <dt>hbase.zookeeper.quorum</dt>
                <dd>Zookeeper quorum location needed to connect to HBase.</dd>

                <dt>hbase.zookeeper.port</dt>
                <dd>Port through which to connect to zookeeper. Typically defaults to 2181.</dd>

                <dt>hbase.master</dt>
                <dd>Location of the HBase master on which the tiles are saved.</dd>
            </dl>
        </dd>
    </dl>
</div>

## Next Steps ##

For details on configuring a Tile Client to display your tile-based visual analytic, see the [Configure the Tile Client](../tile-client/) topic.