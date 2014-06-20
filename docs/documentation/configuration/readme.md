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
2. Update the Maven POM (*new-project/***pom.xml**) to change the  \<artifactId\> tag content (line 4) from *tile-client-template* to *new-project" and edit the \<name\> tag content (line 6) from *Aperture-Tiles Web Client Template* to an appropriate project description.

##<a name="server-config"></a>Tile Server Configuration

The Tiler Server in your new template relies on the following configuration files:

- web.xml
- tile.properties
- Maps
- Layers

###<a name="webxml"></a>web.xml

Edit the **web.xml** file in *new-project/src/main/webapp/WEB-INF/*:

1. If you performed a custom tile generation, edit the guice-modules parameter to pass in any custom modules you created (e.g., your custom Tile Serialization Factory).
2. If required, uncomment the relevant Spark lines in the guice-modules to enable live tiling or drill-through to raw data.
		
###<a name="tileproperties"></a>tile.properties

Edit the **tile.properties** file in *new-project/src/main/resources/*. This file specifies:

- The location of your Spark master
- The name under which the Aperture Tiles web service should appear in the Spark web interface.  
-  The home directory of Spark
-  Any JARs you want to add to the Spark context

###<a name="maps"></a>Maps

The maps file describes the base map upon which your source data is projected. Two example maps are provided in the Tile Client Template (`tile-client-template/src/main/resources/maps`):

- **crossplot-maps.json.example**: describes the parameters of an X/Y cross plot 
- **geographic-maps.json.example**: describes the parameters of a world map

Choose the appropriate map and remove the **.example** suffix from the filename.

####<a name="cross-plot-maps"></a>Cross Plot Maps

The following groups of parameters should be configured for your custom cross plot map:

- Metadata
- PyramidConfig
- AxisConfig

#####Metadata

The Metadata parameters uniquely identify the base map.

```json
"id": "Crossplot-Example",
"description": "An example map config for a crossplot-map.",
``` 

#####PyramidConfig

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

#####AxisConfig

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
		Type of interval along the axis "percentage" or "fixed".

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

- Metadata
- PyramidConfig
- MapConfig
- baseLayer
- AxisConfig

#####Metadata

The Metadata parameters uniquely identify the base map.

```json
"id": "Geographic-Map-Example",
"description": "An example map config for a geographic map.",
``` 

#####PyramidConfig

The PyramidConfig `type` parameter should always be set to *WebMercator* for geographic maps.

#####MapConfig

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

#####BaseLayer



#####AxisConfig

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
		Type of interval along the axis "percentage" or "fixed".

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

##<a name="clientconfig"></a>Tile Client Configuration


###<a name="configaperture"></a>Aperture Configuration

The Tile Client uses ApertureJS services and a client-side visualization library. To communicate with the Tile Server, the Tile Client must specify the Aperture services endpoints.  This configuration is specified in *new-project/src/main/resources/**aperture-config.json***. Edit the `aperture.io` setting specifying the `restEndpoint` to use the correct URL for the *new-project*.  For example:

```javascript
"aperture.io" : {
	"rpcEndpoint" : "%host%/aperture/rpc",
	"restEndpoint" : "%host%/new-project/rest"
```

###<a name="server-side-config"></a>Server-Side Tile Rendering Configuration

The Tile Client allows configuration of multiple layers of tiles that can be overlaid in the plot or map viewer. These are defined in the *new-project/src/main/resources/layers/* directory. Note that two layer property file examples are provided: one for cross plot visualizations and one for geographic map visualizations.

This configuration file allows specifying server-side and client-side layers. 

Each server layer contains six editable fields to customize how the tiles are rendered:
- Data
- layer, type, transform, ramp, opacity, renderer. A detailed description of each parameter is found below.

####<a name="layer"></a>Layer

The layer field is used to point to specific tiles which are displayed on the map. For example if tiles are stored in */data/tiles/twitter.lat.lon* the layers field should be set to "twitter.lat.lon". If HBase is being used then layer should be set to the table name in HBase that contains the tileset. 
					
####<a name="type"></a>Type

Currently "tile" is the only option supported.

####<a name="transform"></a>Transform

The transform option allows the user to apply transformations to the data values when determining color. Currently the options are "linear" which does not perform a transformation, and "log10" which will take logarithms of raw values prior to applying a color ramp.
					
####<a name="ramp"></a>Color Ramp

The ramp option is used to determine what color scale is applied to the data points. Currently the ramp supports 3 options: linear, single-gradient, and default color scales.

#####<a name="linear"></a>Linear Ramp

The linear option shows all of the data points with a constant color and can be configured by setting:

```JavaScript
"ramp": {
	"name": "flat",
	"color": "green"
```
#####<a name="grad"></a>Single Gradient Ramp

The single-gradient option will draw the color of each data point as a gradient between two colors. The value where the gradient begins and ends can be manually altered by setting the from-alpha and to-alpha fields. The from-alpha and to-alpha can each take any value between 0-255.

A sample implementation is shown below:

```JavaScript
"ramp": {
    "name": "single-gradient",
    "from": 0xffffff,
    "from-alpha": 0,
    "to": 0x00ff00,
    "to-alpha": 100
}
```

#####<a name="def"></a>Default Color Scales

The default color scales will edit the color of each data
point based on its concentration using a predefined color scale.The current options are:

- 'br': A blue-green ramp.
- 'inv-br': Inverted blue-green ramp.
- 'ware': A red-yellow-green ramp.
- 'inv-ware': Inverted red-yellow-green ramp.
- 'grey': Full greyscale ramp.
- 'inv-grey': Inverse greyscale ramp.

These options can be set directly by:

```JavaScript
"ramp": "ware"
```

####<a name="opacity"></a>Opacity

This setting is used to set the opacity of the rendered tile layer. This can be set to any decimal value ranging from 0 (totally transparent) to 1 (totally opaque).

####<a name="renderer"></a>Renderer

This option defines which renderer the server should use to render tiles. The renderer will be dependent on the type of  tile data.  The current renderer options are:

- 'default': Renders to a heat-map, based on a standard avro double-valued tile
- 'doubleseries': Renders to a series of heat-maps, based on a standard avro double-series-valued tile
- 'doublestatistics': Renders tile's total hit and % coverage as text to the tile
- 'textscore': Renders to an image showing scored words, with bars based on their score

####<a name="multiple"></a>Multiple Layers

Maps with multiple layers can be created by specifying multiple layer descriptions in *layers.json*. An example is shown below.

```JavaScript
{
	"ServerLayers": [
		{
            "layer": "my-layer1",
            "type": "tile",
            "transform": "linear",
            "ramp": "ware",
            "opacity": 0.85,
            "renderer": "default"
        },
        {
            "layer": "my-layer2",
            "type": "tile",
            "transform": "linear",
            "ramp": "ware",
            "opacity": 0.85,
            "renderer": "textscore"
        }
    ]
}
```

###<a name="client-side-rendering"></a>Client-Side Tile Rendering Configuration

Coming Soon.