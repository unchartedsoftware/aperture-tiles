Aperture Tiles Web Client

In order to run a web client, one needs to set a few configuration details.


== Server ==

First, the server must know where and how to get the data to display.

  * tile-server/src/main/webapp/WEB-INF/web.xml
    This file determines how the server locates tiles.  Details can be found
    within the file - look for "Storage type".

  * tile-server/src/main/resources/tile.properties
    This file describes where the server will find tile pyramids.  Again, 
    details are contained therein - look for the name of the storage module
    you enabled in web.xml.

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
        color.  Options are:
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
          * default - render to a heat-map, based on a standard avro 
            double-valued tile
          * doubleseries - render to a series of heat-maps, based on a
            standard avro double-series-valued tile
          * textscore - Render to an image showing the scored words, with bars
            based on their score
          * legacy - identical rendere to the default, but reading an old tile
            format that no one should now need.
