# Julia Set Aperture Tiles Sample

This demo displays the points in an example Julia set fractal dataset on an X/Y plot with five zoom levels. It is a complete version of the [quick start](https://tiles.oculusinfo.com/documentation/quickstart/) walk-through on http://tiles.oculusinfo.com/.

## Setup and Building

Before running this sample, the tile data needs to be downloaded locally. Download http://assets.oculusinfo.com/tiles/downloads/tilesets/julia.x.y.v.zip and place the file in `src/main/resources`. Ensure the filename is `julia.x.y.v.zip`.

To build the demo first ensure that the entire aperture-tiles project is built by running the following from the root aperture-tiles project directory:

```
mvn clean install
```

Subsequent changes to the Julia Set demo can be built individually by running the following from the `julia-demo/` directory:

```
mvn package
```

## Running

From the the `julia-demo/` directory, run:

```
mvn jetty:run
```

Once the server has started, navigate to [http://localhost:8080/julia-demo](http://localhost:8080/julia-demo) in your favorite modern browser.