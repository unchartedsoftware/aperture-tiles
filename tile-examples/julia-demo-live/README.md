Live Julia Set Aperture Tiles Sample
====================================

This demo displays points in an example Julia set fractal dataset on an x/y plot. Unlike the standard [Julia set demo](../julia-demo), this demo displays data on demand, with no pre-generation of tiles.

## Setup and Building ##

To build the demo:

1. Open the **julia-layer.json** layer file in [src/main/resources/layers/](src/main/resources/layers/).
2. Set the **oculus.binning.source.location** property to the correct data location in HDFS.
3. Save the changes.
4. Open the **tile-properties** file in [src/main/resources/](src/main/resources/).
5. Ensure that all Spark-related properties (e.g., **org.apache.spark.master** and **org.apache.spark.home**) are properly set.
6. Save the changes.
7. Build the entire aperture-tiles project by executing the following command in the [root aperture-tiles](../../) directory:

	```bash
	../../gradlew install
	```

8. Subsequent changes to the Julia Set demo can be built individually by executing the following command in the [tile-examples/julia-demo-live/](.) directory:

	```bash
	../../gradlew install
	```

## Running ##

To serve the demo on your localhost and test it:

1. Run the following command in the [tile-examples/julia-demo-live/](.) directory:

	```bash
	../../gradlew jettyrun
	```

2. Navigate to [http://localhost:8080/julia-demo-live](http://localhost:8080/julia-demo-live) in any modern browser.
