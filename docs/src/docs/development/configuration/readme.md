---
section: Docs
subtitle: Development
chapter: Configuration
permalink: docs/development/configuration/index.html
layout: submenu
---

# Configuration #

Once you have generated a tile set from your source data, you should configure and deploy a Tile Server and a Tile Client to make your visual analytic viewable in a web browser. The Tile Server serves tiles from your pyramid, while the Tile Client is a simple web client to view those tiles on a map or plot. 

Note that Aperture Tiles supports two types of tile rendering:

- **Server-side rendering**, in which the Server renders the tiles as image files and passes them on to the Client.
- **Client-side rendering**, in which the Server passes the tiles as JSON data to the Client, which then renders them directly.

The fastest way to create these the Tile Server and Tile Client is with the Tile Client Template, which contains samples of both. You can also look at the source code for the [tile examples](../../../demos) to understand the structure and configuration of the Tile Server and Tile Client.

## <a name="template-setup"></a> Template Setup ##

To begin configuring your Tile Server and Tile Client:

1. Create a copy of the *aperture-tiles/tile-client-template* directory and give it a unique name (e.g., *new-project*).
2. Update the Maven POM (*new-project/pom.xml*) to change the following tags:
	- `<groupId>` (line 5): Enter an appropriate group ID
	- `<version>` (line 6): Enter an appropriate project version number.
	- `<artifactId>` (line 7): Change from *tile-client-template* to *new-project*
	- `<name>` (line 10): Enter an appropriate project description. 

## <a name="server-config"></a> Tile Server Configuration ##

The Tiler Server in your new template relies on the following configuration files:

- [Web XML](#webxml), which defines which modules Guice will use.
- [Tile Properties](#tileproperties), which specifies constants used by Guice during initialization, including the location of your Spark installation, and the location of some server configuration files.
- [Maps](#maps), which defines the base maps on which your data is projected.
- [Layers](#layers), which defines the individual layers of data that can be overlaid on your base map. The layers file also indicates whether rendering should be performed by the server or the client.

### <a name="webxml"></a> Web XML ###

Edit the **web.xml** file in *new-project/src/main/webapp/WEB-INF/*:

1. If you performed a custom tile generation, edit the guice-modules parameter to pass in any custom modules you created (e.g., your custom Tile Serialization Factory).
2. If required, uncomment the relevant Spark lines in the guice-modules to enable live tiling or drill-through to raw data.
		
### <a name="tileproperties"></a> Tile Properties ###

Edit the **tile.properties** file in *new-project/src/main/resources/*. This file specifies parameters for use by Guice, such as:

- The location of your Spark master
- The name under which the Aperture Tiles web service should appear in the Spark web interface.  
-  The home directory of Spark
-  Any JARs you want to add to the Spark context
-  The location of your map, layer and annotation directories

### <a name="maps"></a> Maps ###

The maps file describes the base map upon which your source data is projected. Two example map files are provided in the Tile Client Template (`tile-client-template/src/main/resources/maps`):

- **crossplot-maps.json.example**: describes the parameters of an [X/Y cross plot](#cross-plot-maps) 
- **geographic-maps.json.example**: describes the parameters of a [world map](#geographic-maps)

Choose the appropriate map type and remove the **.example** suffix from the filename. The following sections describe how to edit the **maps.json** for each map type.

#### <a name="cross-plot-maps"></a> Cross Plot Maps ####

The following groups of parameters should be configured for your custom cross plot map:

- [Metadata](#cross-metadata)
- [PyramidConfig](#cross-pyramidconfig)
- [AxisConfig](#cross-axisconfig)

##### <a name="cross-metadata"></a> Metadata #####

The Metadata parameters uniquely identify the base map.

```json
"id": "Crossplot-Example",
"description": "An example map config for a crossplot-map.",
``` 

##### <a name="cross-pyramidconfig"></a> PyramidConfig #####

The PyramidConfig parameters describe the minimum and maximum values on the X and Y axes in your cross plot. The values that you provide in this section must match the values in your data source. The `type` should always be set to *AreaOfInterest* for non-geographic cross plot maps. 

```json
"PyramidConfig": {
	"type" : "AreaOfInterest",
	"minX" : -2.0,
	"maxX" : 2.0,
	"minY" : -2.0,
	"maxY" : 2.0
	},
```

Note also that the layer and map pyramid configurations ***must*** match each other.

##### <a name="cross-axisconfig"></a> AxisConfig #####

The AxisConfig parameters determine how the X and Y axes are drawn in your cross plot map.

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>
					title
				</dt>
				<dd>Axis name.</dd>
				
				<dt>
					position
				</dt>
				<dd>Axis type, where "bottom' denotes the X axis and "left" denotes the Y axis.</dd>
				
				<dt>
					repeat
				</dt>
				<dd>Indicates whether the map will repeat when the user scrolls off one end. Most useful for geographic maps.</dd>
	
				<dt>
					intervalSpec
				</dt>
				<dd>
				
					<dl>
						<dt>type</dt>
						<dd>How the following increment value is calculated based on the axis range. Accepted values include "percentage", "%", "value" or "#".</dd>
		
						<dt>increment</dt>
						<dd>Value or percentage of units by which to increment the intervals. How this is applied is dependent on the specified type.</dd>
						
						<dt>pivot</dt>
						<dd>Value or percentage from which all other values are incremented. Typically 0.</dd>
		
						<dt>allowScaleByZoom</dt>
						<dd>Indicates whether the axis should be scaled by the zoom factor.</dd>
					</dl>
				</dd>
				
				<dt>
					<b>unitSpec</b>
				</dt>
				<dd>
				
					<dl>
						<dt>type</dt>
						<dd>Determines the individual axis label strings formats. Options include:
							<ul>
								<li>"billions": 150.25B
								<li>"millions": 34.45M
								<li>"thousands": 323.26K
								<li>"decimal": 234243.32
								<li>"integer": 563554
								<li>"time": MM/DD/YYYY
								<li>"degrees": 34.56&#176;
							</ul>
						</dd>
						
						<dt>decimals</dt>
						<dd>Number of decimals to display for each unit. Applicable to "billions", "millions", "thousands" and "decimal" types.</dd>
		
						<dt>allowStepDown</dt>
						<dd>Indicates whether the units can step down if they are below range. Applicable to "billions", "millions", "thousands" types.</dd>
					</dl>
			</dl>
		</ul>
	</div>
</div>

#### <a name="geographic-maps"></a> Geographic Maps ####

The following groups of parameters should be configured for your custom geographic map:

- [Metadata](#geo-metadata)
- [PyramidConfig](#geo-pyramidconfig)
- [MapConfig](#geo-mapconfig)
- [baseLayer](#geo-baselayer)
- [AxisConfig](#geo-axisconfig)

##### <a name="geo-metadata"></a> Metadata #####

The Metadata parameters uniquely identify the base map.

```json
"id": "Geographic-Map-Example",
"description": "An example map config for a geographic map.",
``` 

##### <a name="geo-pyramidconfig"></a> PyramidConfig #####

The PyramidConfig `type` parameter should always be set to *WebMercator* for geographic maps.

##### <a name="geo-mapconfig"></a> MapConfig #####

The MapConfig parameters determine the allowed zoom level and extent of the geographic map.

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>
					numZoomLevels
				</dt>
				<dd>Number of zoom levels available to users.</dd>
				
				<dt>
					projection
				</dt>
				<dd>"EPSG:900913" indicates that the geographic map is a web mercator or spherical mercator projection.</dd>
				
				<dt>
					displayProjection
				</dt>
				<dd>"EPSG:4326" indicates the identity projection.</dd>
				
				<dt>
					units
				</dt>
				<dd>Indicates the unit of distance in the map.</dd>
				
				<dt>
					maxExtent
				</dt>
				<dd>The coordinates that correspond to the geographic boundaries of the map.</dd>
			</dl>
		</ul>
	</div>
</div>

##### <a name="geo-baselayer"></a> BaseLayer #####

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

The next example shows a TMS layer configuration that uses the Oculus World Graphite map set. You can use these maps in offline mode by first downloading the [map tiles WAR](http://aperturejs.com/downloads/) on [aperturejs.com](http://aperturejs.com/).

```javascript
{
	"type": "TMS",
	"url" : "http://aperture.oculusinfo.com/map-world-graphite/",
	"options" : {
	  "name" : "Open Graphite",
	  "layername": "world-graphite",
	  "osm": 0,
	  "type": "png",
	  "serverResolutions": [156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.96980938,2445.98490469,1222.99245234,611.496226172],
	  "resolutions": [156543.0339,78271.51695,39135.758475,19567.8792375,9783.93961875,4891.96980938,2445.98490469,1222.99245234,611.496226172]
	}
}
```

##### <a name="geo-axisconfig"></a> AxisConfig #####

The AxisConfig parameters determine how the X and Y axes are drawn in your cross plot map.

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>
					title
				</dt>
				<dd>Axis name.</dd>
				
				<dt>
					position
				</dt>
				<dd>Axis type, where "bottom' denotes the X axis and "left" denotes the Y axis.</dd>
				
				<dt>
					repeat
				</dt>
				<dd>Indicates whether the map will repeat when the user scrolls off one end.</dd>
				
				<dt>
					intervalSpec
				</dt>
				<dd>
				
					<dl>
						<dt>type</dt>
						<dd>Type of interval along the axis "percentage", "fixed " or "value".</dd>
		
						<dt>increment</dt>
						<dd>Value or percentage of units by which to increment the intervals.</dd>
						
						<dt>pivot</dt>
						<dd>Value or percentage from which all other values are incremented. Typically 0.</dd>
		
						<dt>allowScaleByZoom</dt>
						<dd>Indicates whether the axis should be scaled by the zoom factor.</dd>
					</dl>
				</dd>
				
				<dt>
					unitSpec
				</dt>
				<dd>
				
					<dl>
						<dt>type</dt>
						<dd>Unit label along the specified axis. Options include "decimal", "time" and "degrees".</dd>
						
						<dt>decimals</dt>
						<dd>Number of decimals to display for each unit, if applicable.</dd>
		
						<dt>allowStepDown</dt>
						<dd>Indicates whether the units can step down if they are below range.</dd>
				
			</dl>
		</ul>
	</div>
</div>

### <a name="layers"></a> Layers ###

The layers file points to the tiles you created and indicates how they should be displayed on the base map. 

Two example layer files are provided in the Tile Client Template (`tile-client-template/src/main/resources/layers`):

- **crossplot-layers.json.example**: describes the parameters of an X/Y cross plot layer
- **geographic-layers.json.example**: describes the parameters of a world map layer

Choose the appropriate layer type and remove the **.example** suffix from the filename. The following sections describe how to edit the **layers.json** for each layer type.

Note that maps with multiple layers can be created by specifying multiple layer descriptions in the *children* section of the **layers.json** file. 
					
#### <a name="layer-metadata"></a> Metadata ####

The Metadata parameters uniquely identify the layer.

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>
					id
				</dt>
				<dd>Must match the name of the folder to which the tiles were saved during the generation process, which is composed of the following parameters from the Tiing Property File: <br><br>&lt;oculus.binning.name&gt;.&lt;oculus.binning.xField&gt;.&lt;oculus.binning.yField&gt;.&lt;oculus.binning.valueField&gt;</dd>
				
				<dt>
					name
				</dt>
				<dd>Name of the layer</dd>
			</dl>
		</ul>
	</div>
</div>

#### <a name="layer-pyramid"></a> Pyramid ####

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

#### <a name="layer-data"></a> Data ####

The data parameters specify the location of the tiles that you created. If you are using HBase, separate parameters are required.

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>pyramidio</dt>
				<dd>
					<dl>
						<dt>
							type
						</dt>
						<dd>Indicates the file format of your tiles:
							<dl>
								<dt>hbase</dt>
								<dd>Tiles are stored in HBase</dd>
								
								<dt>file</dt>
								<dd>Tiles are stored in an uncompressed directory in a local
								filesystem</dd>
					   
								<dt>zip</dt>
								<dd>Tiles are stored in compressed file in a local filesystem</dd>
							</dl>
						</dd>
						
						<dt>
							root.path
						</dt>
						<dd>Root path in which the tiles are stored. Not used for HBase.</dd>
						
						<dt>
							extension
						</dt>
						<dd>Name of the compressed file in which tiles are stored. Only used for ZIP
						files.</dd>
						
						<dt>
							hbase.zookeeper.quorum
						</dt>
						<dd>Zookeeper quorum location needed to connect to HBase.</dd>
						
						<dt>
							hbase.zookeeper.port
						</dt>
						<dd>Port through which to connect to zookeeper. Typically defaults to 2181.</dd>
						
						<dt>
							hbase.master
						</dt>
						<dd>Location of the HBase master on which the tiles are saved.</dd>
						
					</dl>
				</dd>
			</dl>
		</ul>
	</div>
</div>

#### <a name="layer-renderers"></a> Renderers ####

This option defines which renderer the server should use to render tiles. The renderer will be dependent on the type of tile data.  The current renderer options are:

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>
					domain
				</dt>
				<dd>Indicates whether the tiles should be rendered by the server or the client.</dd>
				
				<dt>
					renderer
				</dt>
				<dd>Used for server-side rendering.
					
					<dl>
						<dt>type</dt>
						<dd>
							<dl>
								<dt>'heatmap'</dt>
								<dd>Renders to a heat-map, based on a standard avro double-valued tile</dd>
								
								<dt>'doubleseries'</dt>
								<dd>Renders to a series of heat-maps, based on a standard avro double-series-valued tile</dd>
								
								<dt>'doublestatistics'</dt>
								<dd>Renders tile's total hit and % coverage as text to the tile</dd>
								
								<dt>'textscore'</dt>
								<dd>Renders to an image showing scored words, with bars based  on their score</dd>
							</dl>
						</dd>
					
						<dt>
							ramp
						</dt>
						<dd>Determines the color scale applied to the data points based on their concentration. The default color scales are:
							<ul>
								<li>'br': A blue-green ramp.
								<li>'inv-br': Inverted blue-green ramp.
								<li>'ware': A red-yellow-green ramp.
								<li>'inv-ware': Inverted red-yellow-green ramp.
								<li>'grey': Full greyscale ramp.
								<li>'inv-grey': Inverse greyscale ramp.
							</ul>
						</dd>
						
						<dt>
							opacity
						</dt>
						<dd>Opacity of the rendered tile layer expressed as a decimal ranging from 0 (completely transparent) to 1 (completely opaque).</dd>
					</dl>
				</dd>
				
				<dt>
					renderers
				</dt>
				<dd>List of custom renderers required to perform client-side rendering.</dd>
				
				<dt>
					transform
				</dt>
				<dd>
					<dl>
						<dt>name</dt>
						<dd>Type of transformations that can be applied to the data values when	determining color:
							<ul>
								<li>"linear" does not perform a transformation.
								<li>"log10" takes logarithms of raw values before applying a color ramp.
							</ul>
						</dd>
					</dl>
				</dd>
			</dl>
		</ul>
	</div>
</div>

## <a name="clientconfig"></a> Tile Client Configuration ##

## <a name="clientside"></a> Client-Side Rendering ##

The previous sections focus largely on the process of implementing an Aperture Tiles application using server-side tile rendering (where the Server renders the tiles as image files and passes them to the Client). The process of implementing an application using client-side tile rendering (where the Server passes the tiles as JSON data to the Client, which then renders them directly) requires custom code.

The custom renderers built to support this functionality are based on the following renderers in `/tile-client/src/main/webapp/js/layer/client/renderers/`:

- ApertureRenderer.js, which uses the Aperture JS framework to render tiles
- HtmlRenderer.js, which uses an HTML framework to render tiles

A sample application using this method is available in the Aperture Tiles source code at `/tile-examples/twitter-topics/twitter-topics-client/`. The Twitter Topics application uses client-side rendering to draw the top 5 words occurring in each tile. As multiple renderers are attached to this client-side layer, a carousel interface is activated to allow the user to switch between them. The custom renderers for this application are available in `/src/main/webapp/js/layer/client/renderers/`.

For example, the TopTopicsHtml.js renderer is based on the HtmlRenderer.js framework. Lines 43-48 of this file use the init function to get the raw source data.

```javascript
init: function( map ) {

	this._super( map );
	this.createNodeLayer(); // instantiate the node layer data object
	this.createLayer();     // instantiate the html visualization layer
},
```

The registerLayer method is overriden at line 50. If you are attaching an event listener to the layer state, include it in this section. Call addListener on line 60 (not in the constructor, as the layer state will not have been attached by the time the constructor has been called).

```javascript
 registerLayer: function( layerState ) {

	var that = this; // preserve 'this' context

	this._super( layerState ); // call parent class method

	/*
		Lets attach the layer state listener. This will be called whenever
		the layer state changes.
	*/
	this.layerState.addListener( function(fieldName) {

		var layerState = that.layerState;

		if ( fieldName === "click" ) {
			// if a click occurs, lets remove styling from any previous label
			$(".topic-label, .clicked").removeClass('clicked');
			// in this demo we only want to style this layer if the click comes from this layer
			if ( layerState.get('click').type === "html" ) {
				// add class to the object to adjust the styling
				layerState.get('click').$elem.addClass('clicked');
			}
		}
	});

},
```

At line 74, the HTML node layer is instantiated. This holds the tile data as it comes in from the tile service. The X and Y coordinate mappings are set and used to position the individual nodes on the map. In this example, the data is geospatial and located under the `latitude` and `longitude` keys. The `idKey` attribute is used as a unique identification key for internal managing of the data. In this case, it is the tilekey.

```javascript
createNodeLayer: function() {

	this.nodeLayer = new HtmlNodeLayer({
		map: this.map,
		xAttr: 'longitude',
		yAttr: 'latitude',
		idKey: 'tilekey'
	});
},
```

Then in lines 92-151, the source data is attached to an HTML layer.

```javascript
createLayer : function() {

	var that = this;

	/*
		Utility function for positioning the labels
	*/
	function getYOffset( numTopics, index ) {
		var SPACING =  36;
		return 108 - ( (( numTopics - 1) / 2 ) - index ) * SPACING;
	}

	/*
		Here we create and attach an individual html layer to the html node layer. For every individual node
		of data in the node layer, the html function will be executed with the 'this' context that of the node.
	 */
	this.nodeLayer.addLayer( new HtmlLayer({

		html: function() {

			var MAX_TOPICS = 5,             // we only want to display a maximum of 5 topics
				values = this.values,    // the values associated with the bin (in this example there is only
											// one bin per tile)
				numTopics = Math.min( values.length, MAX_TOPICS ),
				$html = $('<div class="tile"></div>'), // this isn't necessary, but wrapping the tile html in a
													   // 256*256 div lets us use 'bottom' and 'right' positioning
													   // if we so choose
				topic,
				$topic,
				i;

			/*
				Iterate over the top 5 available topics and create the html elements.
			*/
			for (i=0; i<numTopics; i++) {

				topic = values[i].topic;
				$topic = $('<div class="topics-label" style=" top:' +  getYOffset( numTopics, i ) + 'px;">' + topic + '</div>');

				/*
					Attach a mouse click event listener
				*/
				$topic.click( function() {

					var click = {
						$elem: $(this),
						type: "html"
					};
					that.parent.setClick( click );
					event.stopPropagation(); // stop the click from propagating deeper
				});

				$html.append( $topic );
			}

			// return the jQuery object. You can also return raw html as a string.
			return $html;
		}
	}));

}
```

The layer file should then be updated to specify that client-side rendering should be used and to pass in the names of the custom renderers:

```json
"renderers": [
	{
		"domain": "client",
		"renderers": [ "CustomRendererName1",
					   "CustomerRendererName2"
		]
	}
]
``` 

## <a name="deployment"></a>Deployment

Once you have finished configuring the map and layer properties, copy the `/new-project/` folder to your Apache Tomcat or Jetty server.

Access the `/new-project/` directory on the server from any web browser to to view your custom Aperture Tiles visual analytic.

## Next Steps

For details on using your Aperture Tiles visual analytic, see the [User Guide](../../userguide/) topic.