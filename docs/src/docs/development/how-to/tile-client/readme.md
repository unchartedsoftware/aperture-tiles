---
section: Docs
subsection: Development
chapter: How-To
topic: Configure the Tile Client
permalink: docs/development/how-to/tile-client/
layout: submenu
---

Configure the Tile Client
=========================

Once you have generated a tile set from your source data, you should configure a Tile Client, which is a simple web client to view tiles on a map or plot. The Tile Client can be configured to receive tiles as JSON data from the Server and then render them directly.

## <a name="clientside"></a> Client-Side Rendering ##

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

Lines 180-184 instantiate a render theme object that styles the rendered components.

```javascript
darkRenderTheme = new tiles.RenderTheme( "dark", {
	'color': "#FFFFFF",
	'color:hover': "#09CFFF",
	'text-shadow': "#000"
});
```

Lines 204-216 instantiate a word cloud renderer, attaching the theme to the 'text' render target and appending a 'hook' function to give access to the rendered DOM elements and respective data entry.

Note the 'textKey' and 'countKey' attributes under the 'text' render target. They specify that the renderer will find the 'text' and 'count' values required to build the word cloud under the attributes 'topic' and 'countMonthly' in the data entry.

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

The client-rendered layer is instantiated on lines 234-237, passing the "top-tweets" layer as its source and the word cloud renderer as its renderer.

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

For details on using your Aperture Tiles visual analytic, see the [User Guide](../../../user-guide/) topic.