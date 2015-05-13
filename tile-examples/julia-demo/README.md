Julia Set Aperture Tiles Sample
===============================

This demo displays points in an example Julia set fractal dataset on an x/y plot with five zoom levels. It is a complete version of the [quick start](https://aperturetiles.com/docs/development/getting-started/quick-start/) walk-through on <aperturetiles.com>.

## Setup and Building ##

Before you run this demo: 

1. Download the [tile data](http://assets.oculusinfo.com/tiles/downloads/tilesets/julia.x.y.v.zip) and save it to the [src/main/resources/](src/main/resources) directory.
2. Ensure that the filename is **julia.x.y.v.zip**.

To build the demo: 

1. Build the entire aperture-tiles project by executing the following command in the [root aperture-tiles](../../) directory:

	```bash
	../../gradlew install
	```
	
2. Subsequent changes to the Julia Set demo can be built individually by executing the following command in the [tile-examples/julia-demo/](.) directory:

	```bash
	../../gradlew install
	```

## Running ##

To serve the demo on your localhost and test it:

1. Run the following command in the [tile-examples/julia-demo/](.) directory:

	```bash
	../../gradlew jettyrun
	```

2. Navigate to [http://localhost:8080/julia-demo](http://localhost:8080/julia-demo) in any modern browser.