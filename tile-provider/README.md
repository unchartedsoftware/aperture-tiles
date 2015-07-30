Stand-alone Tile Provider
==============

These projects are the infrastructure one needs for a stand-alone service, using
RabbitMQ, that can provide tiles (and _only_ tiles) to users of this service.

By contrast, the tile-service project is a web server that provides visualizations
of tiles, rather than tiles themselves, as a web service.

This tile service is composed of two parts:

* A client component, which contains a PyramidIO that knows how to contact the
  server through RabbitMQ, and read tiles from it.
* A server component, which listens on a RabbitMQ channel for client requests, and
  attempts to fulfil them.
