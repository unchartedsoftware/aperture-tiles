---
section: Documentation
subtitle: Configuration
permalink: documentation/configuration/index.html
layout: default
---

#Configuration

Once you have generated a tile set from your source data, you should configure and deploy a Tile Server and a Tile Client to make your visual analytic viewable in a web browser. The Tile Server serves tiles from your pyramid, while the Tile Client is a simple web client to view those tiles on a map or plot. 

Note that Aperture Tiles supports two types of tile rendering:

- **Server-side rendering**, in which the Server renders the tiles as image files and passes them on to the Client.
- **Client-side rendering**, in which the Server passes the tiles as JSON data to the Client, which then renders them directly.

The fastest way to create these the Tile Server and Tile Client is with the Tile Client Template, which contains samples of both. You can also look at the source code for the [tile examples](../../demos) to understand the structure and configuration of the Tile Server and Tile Client.

##<a name="template-setup"></a>Template Setup

To begin configuring your Tile Server and Tile Client:

1. Create a copy of the *aperture-tiles/tile-client-template* directory and give it a unique name (e.g., *new-project*).
2. Update the Maven POM (*new-project/***pom.xml**) to change the following tags:
	- \<artifactId\> (line 4): Change from *tile-client-template* to *new-project*
	- \<name\> (line 6): Enter an appropriate project description.
	- \<version\> (line 14): Enter an appropriate project version number.

##<a name="server-config"></a>Tile Server Configuration

The Tiler Server in your new template relies on the following configuration files:

- [Web XML](#webxml), which passes modules to Guice.
- [Tile Properties](#tileproperties), which specifies the location of your Spark installation.
- [Maps](#maps), which defines the base maps on which your data is projected.
- [Layers](#layer), which defines the individual layers of data that can be overlaid on your base map. The layers file also indicates whether rendering should be performed by the server or the client.

###<a name="webxml"></a>Web XML

Edit the **web.xml** file in *new-project/src/main/webapp/WEB-INF/*:

1. If you performed a custom tile generation, edit the guice-modules parameter to pass in any custom modules you created (e.g., your custom Tile Serialization Factory).
2. If required, uncomment the relevant Spark lines in the guice-modules to enable live tiling or drill-through to raw data.
		
###<a name="tileproperties"></a>Tile Properties

Edit the **tile.properties** file in *new-project/src/main/resources/*. This file specifies parameters for use by Guice, such as:

- The location of your Spark master
- The name under which the Aperture Tiles web service should appear in the Spark web interface.  
-  The home directory of Spark
-  Any JARs you want to add to the Spark context
-  The location of your map, layer and annotation directories

###<a name="maps"></a>Maps

The maps file describes the base map upon which your source data is projected. Two example map files are provided in the Tile Client Template (`tile-client-template/src/main/resources/maps`):

- **crossplot-maps.json.example**: describes the parameters of an [X/Y cross plot](#cross-plot-maps) 
- **geographic-maps.json.example**: describes the parameters of a [world map](#geographic-maps)

Choose the appropriate map type and remove the **.example** suffix from the filename. The following sections describe how to edit the **maps.json** for each map type.

####<a name="cross-plot-maps"></a>Cross Plot Maps

The following groups of parameters should be configured for your custom cross plot map:

- [Metadata](#cross-metadata)
- [PyramidConfig](cross-pyramidconfig)
- [AxisConfig](#cross-axisconfig)

#####<a name="cross-metadata"></a>Metadata

The Metadata parameters uniquely identify the base map.

```json
"id": "Crossplot-Example",
"description": "An example map config for a crossplot-map.",
``` 

#####<a name="cross-pyramidconfig"></a>PyramidConfig

The PyramidConfig parameters describe the minimum and maximum values on the X and Y axes in your cross plot. The values that you provide in this section must match the values in your data source. The `type` should always be set to *AreaOfInterest* for cross plot maps. 

```json
"PyramidConfig": {
	"type" : "AreaOfInterest",
	"minX" : -2.0,
	"maxX" : 2.0,
	"minY" : -2.0,
	"maxY" : 2.0
	},
```

Note also that the layer and map pyramid configurations much match each other.

#####<a name="cross-axisconfig"></a>AxisConfig

The AxisConfig parameters determine how the X and Y axes are drawn in your cross plot map.

```
title
	Axis name.

position
	Axis type, where "bottom' denotes the X axis and "left" denotes the Y axis.

repeat
	Indicates whether the map will repeat when the user scrolls off one end.
	Most useful for geographic maps.

intervalSpec

	type
		Type of interval along the axis "percentage", "fixed" or "value".

	increment
		Value or percentage of units by which to increment the intervals.

	pivot
		Value or percentage from which all other values are incremented.
		Typically 0.

	allowScaleByZoom
		Indicates whether the axis should be scaled by the zoom factor.

unitSpec

	type
		Unit label along the specified axis. Options include "decimal", "time"
		and "degrees".

	decimals
		Number of decimals to display for each unit, if applicable.
	
	allowStepDown
		Indicates whether the units can step down if they are below range.	
```

####<a name="geographic-maps"></a>Geographic Maps

The following groups of parameters should be configured for your custom geographic map:

- [Metadata](#geo-metadata)
- [PyramidConfig](#geo-pyramidconfig)
- [MapConfig](#geo-mapconfig)
- [baseLayer](#geo-baselayer)
- [AxisConfig](#geo-axisconfig)

#####<a name="geo-metadata"></a>Metadata

The Metadata parameters uniquely identify the base map.

```json
"id": "Geographic-Map-Example",
"description": "An example map config for a geographic map.",
``` 

#####<a name="geo-pyramid"></a>PyramidConfig

The PyramidConfig `type` parameter should always be set to *WebMercator* for geographic maps.

#####<a name="geo-mapconfig"></a>MapConfig

The MapConfig parameters determine the allowed zoom level and extent of the geographic map.

```
numZoomLevels
	Number of zoom levels available to users.

projection
	"EPSG:900913" indicates that the geographic map is a web mercator or
	spherical mercator projection.
 
displayProjection
	"EPSG:4326" indicates the identity projection. 

units
	Indicates the unit of distance in the map.

maxExtent
	The coordinates that correspond to the geographic boundaries of the map.
```

#####<a name="geo-baselayer"></a>BaseLayer

The BaseLayer parameters use map provider APIs to determine what features to include on the base map. In the following example, the Google Maps API is used to define the style of the base map.

```json 
"baseLayer" : {
            "Google" : {
                "options" : {
                    "name" : "Google Black",
                    "type" : "styled",
                    "style" : [
                        { "stylers" : [ { "invert_lightness" : true },
                                        { "saturation" : -100 },
                                        { "visibility" : "simplified" } ] },
                        { "featureType" : "landscape.natural.landcover",
                          "stylers" : [ { "visibility" : "off" } ] },
                        { "featureType" : "road",
                          "stylers" : [ { "visibility" : "on" } ] },
                        { "featureType" : "landscape.man_made",
                          "stylers" : [ { "visibility" : "off" } ] },
                        { "featureType" : "transit",
                          "stylers" : [ { "visibility" : "off" } ] },
                        { "featureType" : "poi",
                          "stylers" : [ { "visibility" : "off" } ] },
                        { "featureType" : "administrative.country",
                          "elementType" : "geometry",
                          "stylers" : [ { "visibility" : "on" },
                                        { "lightness" : -56 } ] },
                        { "elementType" : "labels",
                          "stylers" : [ { "lightness" : -46 },
                                        { "visibility" : "on" } ] }
                    ]
                }
            }
        }
```

#####<a name="geo-axisconfig"></a>AxisConfig

The AxisConfig parameters determine how the X and Y axes are drawn in your cross plot map.

```
title
	Axis name.

position
	Axis type, where "bottom' denotes the X axis and "left" denotes the Y axis.

repeat
	Indicates whether the map will repeat when the user scrolls off one end.

intervalSpec

	type
		Type of interval along the axis "percentage", "fixed " or "value".

	increment
		Value or percentage of units by which to increment the intervals.

	pivot
		Value or percentage from which all other values are incremented.
		Typically 0.

	allowScaleByZoom
		Indicates whether the axis should be scaled by the zoom factor.

unitSpec

	type
		Unit label along the specified axis. Options include "decimal", "time"
		and "degrees".

	decimals
		Number of decimals to display for each unit, if applicable.
	
	allowStepDown
		Indicates whether the units can step down if they are below range.	
```

###<a name="layers"></a>Layers

The layers file points to the tiles you created and indicates how they should be displayed on the base map. 

Two example layer files are provided in the Tile Client Template (`tile-client-template/src/main/resources/layers`):

- **crossplot-layers.json.example**: describes the parameters of an X/Y cross plot layer
- **geographic-layers.json.example**: describes the parameters of a world map layer

Choose the appropriate layer type and remove the **.example** suffix from the filename. The following sections describe how to edit the **layers.json** for each layer type.

Note that maps with multiple layers can be created by specifying multiple layer descriptions in the *children* section of the **layers.json** file. 
					
####<a name="layer-metadata"></a>Metadata

The Metadata parameters uniquely identify the layer.

```
id
	Must match the name of the folder to which the tiles were saved during the  generation process, which is composed of the following parameters from the Tiing Property File:
	<oculus.binning.name>.<oculus.binning.xField>.<oculus.binning.yField>.<oculus.binning.valueField>  

description
	Description of the layer
``` 

####<a name="layer-pyramid"></a>Pyramid

The pyramid parameters describe the extent of the data in the layer.  The values that you provide in this section must match the values in your data source and in your map configuration.

For cross plot maps, the `type` should always be set to *AreaOfInterest*. Also include the minimum and maximum values on the X and Y axes in your cross plot. 

```json
"pyramid": {
	"type": "AreaOfInterest",
	"minX" : 1,
	"maxX" : 6336769,
	"minY" : 0,
	"maxY" : 500000
},
```

For geographic maps, the `type` should always be set to *WebMercator*. No minimum and maximum X and Y values are required for this layer type.

```json
"pyramid": {
	"type": "WebMercator"
},
```

Note also that the layer and map pyramid configurations much match each other.

####<a name="layer-data"></a>Data

The data parameters specify the location of the tiles that you created. If you are using HBase, separate parameters are required.

```
pyramidio

	type
		Indicates the file format of your tiles:
		hbase - tiles are stored in HBase
		file - tiles are stored in an uncompressed directory in a local
               filesystem
		zip - tiles are stored in compressed file in a local filesystem

	root.path
		Root path in which the tiles are stored. Not used for HBase.

	extension
		Name of the compressed file in which tiles are stored. Only used for ZIP files.

	hbase.zookeeper.quorum
		Zookeeper quorum location needed to connect to HBase.

	hbase.zookeeper.port
		 Port through which to connect to zookeeper. Typically defaults to 2181.

	hbase.master
		Location of the HBase master on which the tiles are saved.
	
```

####<a name="layer-renderers"></a>Renderers

This option defines which renderer the server should use to render tiles. The renderer will be dependent on the type of tile data.  The current renderer options are:

```
domain
	Indicates whether the tiles should be rendered by the server or the client.

renderer
	Used for server-side rendering.

	type
		'heatmap': Renders to a heat-map, based on a standard avro double-valued tile
		'doubleseries': Renders to a series of heat-maps, based on a standard avro double-series-valued tile
		'doublestatistics': Renders tile's total hit and % coverage as text to the tile
		'textscore': Renders to an image showing scored words, with bars based on their score

	ramp
		Determines the color scale applied to the data points based on their concentration. The default color scales are:
		'br': A blue-green ramp.
		'inv-br': Inverted blue-green ramp.
		'ware': A red-yellow-green ramp.
		'inv-ware': Inverted red-yellow-green ramp.
		'grey': Full greyscale ramp.
		'inv-grey': Inverse greyscale ramp.

	opacity
		Opacity of the rendered tile layer expressed as a decimal ranging from 0 (completely transparent) to 1 (completely opaque).

renderers
	List of custom renderers required to perform client-side rendering. 

transform

	name
		Type of transformations that can be applied to the data values when determining color: 
		"linear" does not perform a transformation.
		"log10" takes logarithms of raw values before applying a color ramp.
```

##<a name="clientconfig"></a>Tile Client Configuration


##<a name="clientside"></a>Client-Side Rendering

The previous sections focus largely on the process of implementing an Aperture Tiles application using server-side tile rendering (where the Server renders the tiles as image files and passes them to the Client). The process of implementing an application using client-side tile rendering (where the Server passes the tiles as JSON data to the Client, which then renders them directly) requires custom code.

The custom renderers built to support this functionality are based on the following renderers in `/tile-client/src/main/webapp/js/layer/view/client/`:

- ApertureRenderer.js, which uses the ApertureJS framework to render tiles
- HtmlRenderer.js, which uses an HTML framework to render tiles

A sample application using this method is available in the Aperture Tiles source code at `/tile-examples/twitter-topics/twitter-topics-client/`. The Twitter Topics application uses client-side rendering to draw carousels on each tile that contain multiple ways to view the top 10 Twitter topics used in the geographic area that they cover. The custom renderers for this application are available in `/src/main/webapp/js/layer/view/client/impl`.

For example, the TagsByTimeHtml.js renderer is based on the HtmlRenderer.js framework. Lines 45-55 of this file use the init function to get the raw source data.

```javascript
init: function( map) {

            this._super( map );
            this.nodeLayer = new ClientNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey'
            });
            this.createLayer();
        },
```

Then in lines 93-150, the source data is attached to an HTML layer.

```
this.nodeLayer.addLayer( new HtmlLayer({

                html: function() {

                    var tilekey = this.tilekey,
                        html = '',
                        $html = $('<div id="'+tilekey+'" class="aperture-tile"></div>'),
                        $elem,
                        $translate,
                        values = this.bin.value,
                        value,
                        maxPercentage, relativePercent,
                        visibility,
                        i, j,
                        tag,
                        count = TwitterUtil.getTagCount( values, NUM_TAGS_DISPLAYED );

                    // create translate button
                    $translate = that.createTranslateLabel( tilekey );

                    $html.append( $translate );

                    for (i=0; i<count; i++) {

                        value = values[i];
                        tag = TwitterUtil.trimLabelText( that.getTopic( value, tilekey ), NUM_LETTERS_IN_TAG );
                        maxPercentage = TwitterUtil.getMaxPercentageByType( value, 'PerHour' );

                        html = '<div class="tags-by-time" style="top:' +  getYOffset( values, i ) + 'px;">';

                        // create count chart
                        html += '<div class="tags-by-time-left">';
                        for (j=0; j<24; j++) {
                            relativePercent = ( TwitterUtil.getPercentageByType( value, j, 'PerHour' ) / maxPercentage ) * 100;
                            visibility = (relativePercent > 0) ? '' : 'hidden';
                            html += '<div class="tags-by-time-bar" style="visibility:'+visibility+';height:'+relativePercent+'%; top:'+(100-relativePercent)+'%;"></div>';
                        }
                        html += '</div>';

                        // create tag label
                        html += '<div class="tags-by-time-right">';
                        html +=     '<div class="tags-by-time-label">'+tag+'</div>';
                        html += '</div>';

                        html += '</div>';

                        $elem = $(html);

                        that.setMouseEventCallbacks( $elem, this, value );
                        that.addClickStateClasses( $elem, value.topic );

                        $html.append( $elem );
                    }

                    return $html;
                }
            }));
```

The layer file should then be updated to specify that client-side rendering should be used and to pass in the names of the custom renderers:

```
"renderers": [
                        {
                            "domain": "client",
                            "renderers": [ "CustomRendererName1",
                                           "CustomerRendererName2"
                            ]
                        }
                    ]
``` 

##<a name="deployment"></a>Deployment

Once you have finished configuring the map and layer properties, copy the `/new-project/` folder to your Apache Tomcat or Jetty server.

Access the `/new-project/` directory on the server from any web browser to to view your custom Aperture Tiles visual analytic.