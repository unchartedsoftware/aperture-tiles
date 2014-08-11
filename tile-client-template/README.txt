Aperture Tiles Web Client

In order to run a web client, one needs to set a few configuration details.  
These can be set in your client project, but their descriptions are grouped by 
the component they affect, not where they are found.


== Server ==

The server only needs configuration if one intends to use custom data classes 
or server-side renderers.

  * src/main/resources/tile.properties
    This file contains a list of all modules to load.  Four modules in 
    particular allow points at which tiling can be customized.  None of this 
    customization can be done without java code - so one should be comfortable
    looking at source if one intends to use custom binning and tiling classes.

    * com.oculusinfo.tile.init.TilePyramidFactoryModule sets up 
      com.oculusinfo.tile.init.providers.StandardTilePyramidFactoryProvider to 
      be the pyramid factory provider.  This provider allows the use of an AoI
      (Area of Interest) tile pyramid, and a Web Mercator tile pyramid.  We 
      expect it to be rare that one needs a different, custom mapping from the
      data space to tile space - generally, one would simply map the raw values
      somehow - but if one needs such a custom mapping, one will need to 
      override this module with an extension that serves up one's custom tile 
      pyramids. 
    * com.oculusinfo.tile.init.PyramidIOFactoryModule sets up 
      com.oculusinfo.tile.init.providers.StandardPyramidIOFactoryProvider to
      be the pyramid IO factory provider.  This provider allows reading tile 
      pyramids from the file system, from HBase, from a .jar file, from a zip 
      file embedded in a .jar file, through a JDBC connection, or from sqlite 
      database.  If one needs tile pyramids stored in other locations, one 
      will need to override this module.
    * com.oculusinfo.tile.init.TileSerializerFactoryModule sets up
      com.oculusinfo.tile.init.providers.StandardTileSerializerFactoryModule.
      This factory provider serves up a factory that will create the standard 
      serialization types defined in the binning-utilities project.  If one 
      has a custom data type as the bin value in ones tiles, one will need to 
      override this module with one that serves up an extended factory that 
      can also create a serializer for one's custom data type.
    * com.oculusinfo.tile.init.ImageRendererRactoryModule sets up
      com.oculusinfo.tile.init.providers.StandardImageRendererFactoryProvider,
      which serves up a factory that can create the server-side renderers we 
      have created in tile-service.  If one has written custom server-side 
      renderers, this module will have to be overridden with an extension that
      serves up a factory that can also produce one's custom rendering types.

== Client ==

Once the server is configured, the client must be told exactly what layers to 
display, and how.  All client configuration files are in src/main/webapp/data.

  * geomap.json
    A description of the base layer of a geographical map on which to display 
    data.

  * emptymap.json
    A description of an empty base layer, for use when the tiles don't 
    represent geographic data (coming soon).

  * layers.json
    A description of the layers to display.  This contains a JSON array of 
    objects, each of which describes a single layer.  Layer properties are as
    follows:
      * Name - The human-readable name of the layer (defaults to the same value 
        as 'Layer')
      * Layer - The id by which the server knows the layer.
      * Transform - What transform to apply to the data values when determining
        color.
          * name - the name of the transform.  Options are:
              * linear - no transformation
              * log10 - take the logarithm of raw values
      * Ramp - The color ramp to apply to transformed data values to determine 
        how it is dispayed.  Options are described, where appropriate, as 
        applying from the minimum data value to the maximum.
          * br - A blue to red scale
          * inv-br - A red to blue scale
          * ware - A scale, designed by Colin Ware, intended to scale in 
            luminance, as well as changing hue, according to value
          * inv-ware - The same scale in reverse
          * grey - A greyscale scale, going from black to white
          * inv-grey - A greyscale scale, going from white to black
      * Type - the type of tile.  Currently only the value "tile", indicating 
        server-rendered tiles, is supported; direct image pyramids and 
        client-rendered should be supported soon.
      * Opacity - The starting opacity of the layer.  This is a value from 0 
        (totally transparent) to 1 (totally opaque)
      * Renderer The renderer the server should use to render tiles.  This also 
        specifies how the server will read the tiles, as the renderer and 
        serializer are intimitaly linked.
          * heatmap - render to a heat-map.  Requires a serializer that reads 
            tiles of doubles.  This is the default.
          * toptextscores - Render to an image showing the top 10 scored 
            strings, with bars based on their score.  Requires a serializer 
            that reads tiles of lists of string/double pairs.
          * textscores - similar to toptextscores, but renders the top 5 and 
            bottom 5 scored strings.
          * doubleeseries - renders a series of images.  Requires a serializer 
            that reads tiles of arrays of doubles.
          * doublestatistics - renders statistics about a tile as text
      * pyramidio - specifies from where tile are read
          * type - hbase, file-system, zip, resource, jdbc, or sqlite.  
            Further properties depend on the type.
      * serializer - specifies how tiles are read.
          * type - defines the type of serializer.  This generally consists of 
            a short-hand for the bin type of the tiles, followed by a suffix 
            determining the file format - "-j" for json, or "-a" for avro.  
            The type short-hand is the java type, all lower-case, with [] 
            indicating lists, nad () indicating tuples.  So 
            "[(string, double)]-a" indicates a serializer that reads avro 
            tiles, whose bins are lists of string/double pairs.
      * 