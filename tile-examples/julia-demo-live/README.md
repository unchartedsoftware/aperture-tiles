# Julia Set Aperture Tiles Sample

This demo displays the points in an example Julia set fractal dataset on an X/Y plot, directly from the data, on demand, with no pre-generation of tiles.

## Setup and Building
To build the demo first ensure that the entire aperture-tiles project is built by running the following from the root aperture-tiles project directory:

```
mvn clean install
```

Subsequent changes to the Julia Set demo can be built individually by running the following from the `julia-demo-live/` directory:

Make sure that the layer file, `in src/main/resources/layers/julia-layer.json`, points to the correct data location in hdfs (using the `oculus.binning.source.location` property).  Also make sure that the spark-related properties in `src/main/resources/tile.properties` are properly set (notably `org.apache.spark.master` and `org.apache.spark.home`).

```
mvn package
```

## Running

From the the `julia-demo-live/` directory, run:

```
mvn jetty:run
```

Once the server has started, navigate to [http://localhost:8080/julia-demo-live](http://localhost:8080/julia-demo-live) in your favorite modern browser.