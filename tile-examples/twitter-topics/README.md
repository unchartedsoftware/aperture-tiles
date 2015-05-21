Twitter Topics Aperture Tiles Sample
====================================

This demo displays a heatmap of geo-located Twitter messages with tile-based overlays that aggregate the top words mentioned in tweets. 

**NOTE**: A more advanced version of this dataset can be found at https://aperturetiles.com/demos/.

## Setup and Building ##

Before you run this demo:

1. Download the [Twitter heatmap](http://assets.oculusinfo.com/tiles/downloads/tilesets/twitter-heatmap.zip) and [Twitter Topics](http://assets.oculusinfo.com/tiles/downloads/tilesets/twitter-curated-topics.zip) tile data and save it to the [src/main/resources/](src/main/resources/) directory.
2. Ensure that the filenames remain as is.

To build the demo:

1. Build the entire aperture-tiles project by executing the following command in the [root aperture-tiles](../../) directory:

	```bash
	../../gradlew install
	```

2. Subsequent changes to the Twitter Topics demo can be built individually by executing the following command in the [tile-examples/twitter-topics/](.) directory:

	```bash
	../../gradlew install
	```

## Running ##

To server the demo on your localhost and test it:

1. Run the following command in the [tile-examples/twitter-topics/](.) directory:

	```bash
	../../gradlew jettyrun
	```

2. Navigate to [http://localhost:8080/twitter-topics-client](http://localhost:8080/twitter-topics-client) in any modern browser.