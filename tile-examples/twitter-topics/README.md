# Twitter Topics Aperture Tiles Sample

This demo displays a Twitter geo-located tweet heatmap and aggregates the top words mentioned in tweets for each tile. A more advanced demo of this dataset can be found at https://aperturetiles.com/demos/.

## Setup and Building

Before running this sample, the tile data needs to be downloaded locally. Download http://assets.oculusinfo.com/tiles/downloads/tilesets/twitter-heatmap.zip and http://assets.oculusinfo.com/tiles/downloads/tilesets/twitter-curated-topics.zip and place both in `src/main/resources`. Ensure the filenames remain as is.

To build the demo first ensure that the entire aperture-tiles project is built by running the following from the root aperture-tiles project directory:

```
mvn clean install
```

Subsequent changes to the Twitter demo can be built individually by running the following from the `twitter-topics/` directory:

```
mvn package
```

## Running

From the the `twitter-topics/` directory, run:

```
mvn jetty:run
```

Once the server has started, navigate to [http://localhost:8080/twitter-topics](http://localhost:8080/twitter-topics) in your favorite modern browser.