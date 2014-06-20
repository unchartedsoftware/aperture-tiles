---
section: Documentation
subtitle: Configuration
permalink: documentation/configuration/index.html
layout: default
---

#Configuration

Once you have generated a tile set from your source data, you should configure a Tile Server and a Tile Client to make your visual analytic viewable in a web browser. The Tiler Server serves tiles from your pyramid while the Tile Client is a simple web client to view those tiles on a map or plot. 

The fastest way to create these components is with the Tile Client Template, which contains a samples of both. You can also look at the source code for the [tile examples](../../demos) to understand the structure and configuration of the Tile Server and Tile Client.

##<a name="template-setup"></a>Template Setup

To begin configuring your Tile Server and Tile Client:

1. Create a copy of the *aperture-tiles/tile-client-template* directory and give it the name *new-project*.
2. Update the Maven POM (*new-project/***pom.xml**) to change the  \<artifactId\> tag content (line 4) from *tile-client-template* to *new-project" and edit the \<name\> tag content (line 6) from *Aperture-Tiles Web Client Template* to an appropriate project description.

##<a name="server-config"></a>Tile Server Configuration

The Tiler Server in your new template relies on two configuration files:

- web.xml
- tile.properties

###<a name="webxml"></a>web.xml

Edit the **web.xml** file in *new-project/src/main/webapp/WEB-INF/*. Lines 17-29 specify which storage module is being used to hold the tiles. Uncomment the relevant lines based on how the tiles are stored and ensure that all other lines from 17-30 are commented out. Aperture Tiles currently supports tiles stored as File System Based, Jar files, Zip files, and in HBase or a SQLite database.
		
###<a name="tileproperties"></a>tile.properties

Next, edit the file *new-project/src/main/resources/**tile.properties***. This file is responsible for pointing the server to the location of the tiles. Lines 10-70 provide detailed instructions regarding how to configure each storage module. Uncomment the relevant lines so that the location information matches the module type chosen in web.xml and set the required location parameters appropriately.
					
It is important to note that the information in *tile.properties* should point to the root location of the tiles, not to the actual tile set. For example if the full address of the tile set is: *data/tiles/twitter.lat.lon* then tile.properties should point to *data/tiles/*. The tile client configuration in the next section will point to the specific tile sets using the layers configuration file.

##<a name="clientconfig"></a>Tile Client Configuration

Now that the server is pointed to the tiles, it is time to configure the tile client to properly display the tiles.

- Aperture Configuration
- Server-Side Tile Rendering: The Server renders the tiles as image files and passes them on to the Client
- Client-Side Tile Rendering: The Server passes the tiles as JSON data to the Client, which then renders them directly.

###<a name="configaperture"></a>Aperture Configuration

The Tile Client uses ApertureJS services and a client-side visualization library. To communicate with the Tile Server, the Tile Client must specify the Aperture services endpoints.  This configuration is specified in *new-project/src/main/webapp/data/**map.json***. Edit the `aperture.io` setting specifying the `restEndpoint` to use the correct URL for the *new-project*.  For example:

```javascript
"aperture.io" : {
	"rpcEndpoint" : "%host%/aperture/rpc",
	"restEndpoint" : "%host%/new-project/rest"
```

Additional configuration allows customizing properties such as the x and y axis, max zoom levels allowed, and base layer properties.  The tile-client-template provides example settings for tile-based maps and cross plots.  See documentation in **map.json** for further details.

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