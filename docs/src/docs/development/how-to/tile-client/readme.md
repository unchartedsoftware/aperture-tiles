---
section: Docs
subsection: Development
chapter: How-To
topic: Configure the Tile Client
permalink: docs/development/how-to/tile-client/
layout: chapter
---

Configure the Tile Client
=========================

Once you have customized your Tile Server, you should customize the Tile Client, which is a simple web client that receives tile data from the Tile Server and displays them on a map or plot. 

**NOTE**: The Tile Client can also be configured to receive tiles as JSON data from the Server and then render them directly.

## Application JavaScript ##

The application JavaScript file (*/src/main/webapp/js/***app.js**) should request layers from the server and return an array of layer configuration objects. Among the objects it should instantiate are the map, its baselayer and axis configurations.

### Map ###

The map describes the base map upon which your source data is projected:

- Geographic maps: the pyramid `type` parameter is *WebMercator*. If no `type` is specified, the map defaults to *WebMercator* and no other configuration options are required.
- Non-geographic cross-plot maps: the `type` should always be set to *AreaOfInterest*. Additional pyramid parameters are required to describe the minimum and maximum values on the X and Y axes. The values that you provide in this section must match the values in your data source.

```json
pyramid: {
    type : "AreaOfInterest",
    minX : -2,
    maxX : 2,
    minY : -2,
    maxY : 2
    },
```

**NOTE**: Your layer and map pyramid configurations must match each other.

### BaseLayer ###

The BaseLayer parameters use map provider APIs to determine what features to include on the base map. In the following example, the Google Maps API (OpenLayers.Layer.Google) is used to define the style of the base map.

```json
baseLayer = new tiles.BaseLayer({
    type: "Google",
    options : {
        styles : [
            { featureType: "all",
              stylers : [ { invert_lightness : true },
                          { saturation : -100 },
                          { visibility : "simplified" } ] },
            { featureType: "administrative",
              elementType: "geometry",
              stylers: [ { visibility: "off" } ] },
            { featureType : "landscape.natural.landcover",
              stylers : [ { visibility : "off" } ] },
            { featureType : "road",
              stylers : [ { visibility : "on" } ] },
            { featureType : "landscape.man_made",
              stylers : [ { visibility : "off" } ] },
            { featureType : "landscape",
              stylers : [ { lightness : "-100" } ] },
            { featureType : "poi",
              stylers : [ { visibility : "off" } ] },
            { featureType : "administrative.country",
              elementType : "geometry",
              stylers : [ { visibility : "on" },
                          { lightness : -56 } ] },
            { elementType : "labels",
              stylers : [ { lightness : -46 },
                          { visibility : "on" } ] }
        ]
    }
});
```

The next example shows a TMS layer configuration (standard OpenLayers.Layer.TMS) that uses the Uncharted World Graphite map set. You can use these maps in offline mode by first downloading the [map tiles WAR](http://aperturejs.com/downloads/) on [aperturejs.com](http://aperturejs.com/).

```javascript
{
    "type": "TMS",
    "url" : "http://aperture.oculusinfo.com/map-world-graphite/",
    "options" : {
        "name" : "Open Graphite",
        "layername": "world-graphite",
        "osm": 0,
        "type": "png",
        "serverResolutions": [156543.0339,78271.51695,39135.758475,19567.8792375,
                              9783.93961875,4891.96980938,2445.98490469,
                              1222.99245234,611.496226172],
        "resolutions": [156543.0339,78271.51695,39135.758475,19567.8792375,
                        9783.93961875,4891.96980938,2445.98490469,
                        1222.99245234,611.496226172]
    }
}
```

### Axes ###

The AxisConfig parameters determine how the X and Y axes are drawn in your cross-plot map.

<div class="props">
    <dl class="detailList">
        <dt>position</dt>
        <dd>Axis type, where <em>bottom</em> denotes the x-axis and <em>left</em> denotes the y-axis.</dd>

        <dt>title</dt>
        <dd>Axis name (e.g., <em>Longitude</em> or <em>Latitude</em>).</dd>

        <dt>enabled</dt>
        <dd>Indicates whether the axes are displayed (<em>true</em>) or hidden (<em>false</em>) when a new session begins.</dd>

        <dt>repeat</dt>
        <dd>Indicates whether the map will repeat when the user scrolls off one end. Most useful for geographic maps.</dd>

        <dt>intervals</dt>
        <dd>
            <dl>
                <dt>type</dt>
                <dd>How the following increment value is calculated based on the axis range. Accepted values include <em>percentage</em>, <em>%</em>, <em>value</em> or <em>#</em>.</dd>

                <dt>increment</dt>
                <dd>Value or percentage of units by which to increment the intervals. How this is applied is dependent on the specified type.</dd>
                
                <dt>pivot</dt>
                <dd>Value or percentage from which all other values are incremented. Typically <em>0</em>.</dd>

                <dt>scaleByZoom</dt>
                <dd>Indicates whether the axis should be scaled by the zoom factor (<em>true</em>/<em>false</em>).</dd>
            </dl>
        </dd>

        <dt>units</dt>
        <dd>
            <dl>
                <dt>type</dt>
                <dd>
                    Determines the individual axis label strings formats. Options include:
                    <ul>
                        <li>"billions": 150.25B</li>
                        <li>"millions": 34.45M</li>
                        <li>"thousands": 323.26K</li>
                        <li>"decimal": 234243.32</li>
                        <li>"integer": 563554</li>
                        <li>"time": MM/DD/YYYY</li>
                        <li>"degrees": 34.56&#176;</li>
                    </ul>
                </dd>
                
                <dt>decimals</dt>
                <dd>Number of decimals to display for each unit. Applicable to <em>billions</em>, <em>millions</em>, <em>thousands</em> and <em>decimal</em> types.</dd>

                <dt>stepDown</dt>
                <dd>Indicates whether the units can step down if they are below range (<em>true</em>/<em>false</em>). Applicable to <em>billions</em>, <em>millions</em>, <em>thousands</em> types.</dd>
            </dl>
        </dd>
    </dl>
</div>

## Client-Side Rendering ##

The previous sections focus largely on the process of implementing an Aperture Tiles application using server-side tile rendering (where the Server renders the tiles as image files and passes them to the Client). The process of implementing an application using client-side tile rendering (where the Server passes the tiles as JSON data to the Client, which then renders them directly) requires custom code.

A sample application using this method is available in the Aperture Tiles source code at [tile-examples/twitter-topics/twitter-topics-client/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics/twitter-topics-client). The Twitter Topics application uses client-side rendering to draw the top words occurring in each tile. As multiple renderers are attached to this client-side layer. The custom renderers for this application are available in [tile-client/src/js/layer/renderer/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-client/src/js/layer/renderer).

Line 33 requests the layer configuration objects from the server, passing them to the supplied callback function as arguments.

```javascript
tiles.LayerService.getLayers( function( layers ) { 
    ... 
});
```

Line 39 organizes the layer configuration object array into a map keyed by layer ID. It also parses the metadata JSON strings into their respective runtime objects. This ensures support for legacy layer metadata.

```javascript
layers = tiles.LayerUtil.parse( layers.layers );
```

Lines 132-136 instantiate a render theme object that styles the rendered components.

```javascript
darkRenderTheme = new tiles.RenderTheme( "dark", {
    'color': "#FFFFFF",
    'color:hover': "#09CFFF",
    'text-shadow': "#000"
});
```

Lines 156-168 instantiate a word cloud renderer, attaching the theme to the **text** render target and appending a **hook** function to give access to the rendered DOM elements and respective data entry.

Note the **textKey** and **countKey** attributes under the **text** render target. They specify that the renderer will find the *text* and *count* values required to build the word cloud under the attributes **topic** and **countMonthly** in the data entry.

```javascript
wordCloudRenderer = new tiles.WordCloudRenderer({
    text: {
        textKey: "topic",
        countKey: "countMonthly",
        themes: [ darkRenderTheme ]
    },
    hook: function( elem, entry, entries, data ) {
        elem.onclick = function() {
            console.log( elem );
            console.log( entry );
        };
    }
});
```

The client-rendered layer is instantiated on lines 186-189, passing the "top-tweets" layer as its source and the word cloud renderer as its renderer.

```javascript
clientLayer = new tiles.ClientLayer({
    source: layers["top-tweets"],
    renderer: wordCloudRenderer
});
```

Finally the map is instantiated, along with all its components, including the client layer.

```javascript
map = new tiles.Map( "map" );
map.add( clientLayer );
```

## Next Steps ##

For details on building and deploying your Aperture Tiles application, see the [Deploy the Application](../deploy/) topic.