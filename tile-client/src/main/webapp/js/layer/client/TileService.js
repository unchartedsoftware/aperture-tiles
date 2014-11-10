/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */


/**
 * This module defines a TileService class that is to be injected into a
 * TileTracker instance. This class is responsible for RESTful requests
 * from the server
 */
define(function (require) {
    "use strict";



    var Class = require('../../class'),
        serviceRegistry = {},
        tileQueryParser,
        TileService;



    tileQueryParser = function( obj, path ) {
        var i, parse, results = [];
        // base case, no more parsing, return value
        if ( path.length === 0 ) {
            return [ obj ];
        }
        // pop next sub path and parse it
        parse = path.shift().split(/[\[\]]/);
        // determine type of path
        switch ( parse.length ) {
            case 1:

                // search object
                results = tileQueryParser( obj[ parse[0] ], path.slice(0) );
                break;
            case 3:
                if ( parse[1] === '' ) {
                    // search entire array
                    obj = obj[ parse[0] ];
                    for ( i=0; i<obj.length; i++ ) {
                        $.merge( results, tileQueryParser( obj[i], path.slice(0) ) );
                    }
                } else {
                    // search specific index
                    results = tileQueryParser( obj[ parse[0] ][ parse[1] ], path.slice(0) );
                }
                break;
        }
        return results;
    };



    TileService = Class.extend({
        ClassName: "TileService",

        /**
         * Construct a TileService
         */
        init: function ( source, tilepyramid ) {
            // current tile data
            this.data = {};
            // tiles flagged as actively requested and waiting on
            this.pendingData = {};
            // callbacks
            this.dataCallback = {};
            // layer info
            this.source = source;
            // set tile pyramid type
            this.tilePyramid = tilepyramid;

            if ( !serviceRegistry[ source.id ] ) {
                serviceRegistry[ source.id ] = [];
            }
            serviceRegistry[ source.id ].push( this );
        },


        /**
         * Convert local data map into an array and return it.
         */
        getDataArray: function( tiles ) {
            var data = this.data,
                tilekey, tile,
                allData = [];
            for ( tilekey in data ) {
                if ( data.hasOwnProperty(tilekey) ) {
                    tile = data[tilekey];
                    // check format of data
                    if ( $.isArray( tile ) ) {
                        // for each tile, data is an array, merge it together
                        $.merge( allData, tile );
                    } else {
                        // for each tile, data is an object
                        allData.push( tile );
                    }
                }
            }
            return allData;
        },


        /**
         * Create a universal unique key for a given tile
         *
         * @param tilekeys Array of tile identification keys to merge into node data
         */
        createTileKey: function (tile) {
            return tile.level + "," + tile.xIndex + "," + tile.yIndex;
        },


        /**
         * Create a universal unique key for a given bin in a given tile
         *
         * @param tileKey The universal tile key, as calculated by the
         *                createTileKey method.
         * @param bin The bin; assumed to be in {x: #, y: #} format.
         */
        createBinKey: function (tileKey, bin) {
            return tileKey + ":" + bin.x + "," + bin.y;
        },


        /**
         * Clears unneeded data from memory
         */
        releaseData: function(tilekey) {
            delete this.data[tilekey];
            delete this.pendingData[tilekey];
            delete this.dataCallback[tilekey];
        },


        /**
         * Requests tile data. If tile is not in memory, send GET request to server and
         * set individual callback function on receive. Callback is not called if tile
         * is already in memory
         *
         * @param requestedTiles array of requested tilekeys
         * @param callback callback function
         */
        requestData: function( requestedTiles, tileSetBounds, callback ) {

            var currentTiles = this.data,
                pendingTiles = this.pendingData,
                defunctTiles = {},
                neededTiles = [],
                i, tile, tilekey;

            // track the tiles we have
            for ( tilekey in currentTiles ) {
                if ( currentTiles.hasOwnProperty(tilekey) ) {
                    defunctTiles[ tilekey ] = true;
                }
            }
            // and the tiles we are waiting on
            for ( tilekey in pendingTiles ) {
                if ( pendingTiles.hasOwnProperty(tilekey) ) {
                    defunctTiles[ tilekey ] = true;
                }
            }

            // Go through, seeing what we need.
            for (i=0; i<requestedTiles.length; ++i) {
                tile = requestedTiles[i];
                tilekey = this.createTileKey(tile);

                if ( defunctTiles[ tilekey ] ) {
                    // Already have the data, or waiting for it remove from defunct list
                    delete defunctTiles[ tilekey ];
                } else {
                    // New data.  Mark for fetch.
                    neededTiles.push( tilekey );
                }
            }

            // Remove all old defunct tiles references
            for ( tilekey in defunctTiles ) {
                if ( defunctTiles.hasOwnProperty( tilekey ) ) {
                    this.releaseData( tilekey );
                }
            }

            // send requests to server
            for (i=0; i<neededTiles.length; ++i) {
                this.getRequest( neededTiles[i], tileSetBounds, callback );
            }
        },


        /**
         * If data is not in memory, flag tilekey as 'loading', request data from server and return false.
         * If data is flagged as 'loading', add callback to callback array and return false.
         * If data is already in memory, returns true.
         *
         * @param tilekey tile identification key
         * @param callback callback function called upon tile received
         * The data for each tile comes across from the server as follows:
         * {
         *   tileIndex: {
         *     x: <tile x coordinate>
         *     y: <tile y coordinate>
         *     level: <tile zoom level>
         *   },
         *   tile: { // Optional; absence means the tile wasn't in the pyramid
         *     // Properties from tile.avsc
         *     level: <tile zoom level>,
         *     xIndex: <tile x coordinate>,
         *     yIndex: <tile y coordinate>,
         *     xBinCount: <number of bins per tile along the x axis>,
         *     yBinCount: <number of bins per tile along the y axis>,
         *     default: <value to use as a default for missing bins>,
         *     meta: <creator-defined metadata>,
         *     values: [
         *       <bin contents>,
         *       <bin contents>,
         *       ....
         *       <bin contents>
         *     ]
         *   }
         * }
         */
        getRequest: function( tilekey, tileSetBounds, callback ) {

            var parsedValues = tilekey.split(','),
                level = parseInt(parsedValues[0], 10),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10);

            // ensure we only send a request once
            if (this.pendingData[tilekey] === undefined) {

                // flag tile as loading and stash callback
                this.pendingData[tilekey] = true;
                this.dataCallback[tilekey] = callback;

                // request data from server
                aperture.io.rest(
                    (this.source.apertureservice+'1.0.0/'+
                     this.source.id+'/'+
                     level+'/'+
                     xIndex+'/'+
                     yIndex+'.json'),
                    'GET',
                    $.proxy(this.getCallback, this),
                    // Add in the list of all needed tiles
                    {'params': tileSetBounds }
                );
            }
        },


        /**
         * Called when a tile is received from server, flags tilekey as 'loaded' and
         * calls every callback function is respective array
         *
         * @param tileData received tile data from server
         */
        getCallback: function(tileData) {

            // create tile key: "level, xIndex, yIndex"
            var tilekey = this.createTileKey( tileData.index );

            // ensure we still need the tile
            if (this.pendingData[tilekey] === true) {

                // convert tile data into data by bin
                this.data[tilekey] = this.transformTileToBins( tileData.tile, tilekey );

                if (tileData.tile !== undefined) {
                    // only call callback function if the tile actually has data associated with it
                    this.dataCallback[tilekey]( tilekey );
                }

                // clear callbacks and 'waiting on' status
                delete this.pendingData[tilekey];
                delete this.dataCallback[tilekey];
            }
        },


        /**
         * Transforms a tile's worth of data into a series of bins of data
         *
         * This can be overridden; most of the result is use-specific - There are,
         * however, a few properties used by the ClientLayer themselves, which must
         * be correctly set here in each bin object:
         *
         * <dl>
         *   <dt> binkey </dt>
         *   <dd> A key uniquely identifying each bin.  This must be generated
         *        using the createBinKey method, passing in the bin in question
         *        and the passed-in tile key. </dd>
         *   <dt> longitude </dt>
         *   <dd> The longitude within this bin where the data will be
         *        displayed </dd>
         *   <dt> latitude </dt>
         *   <dd> The latitude within this bin where the data will be
         *        displayed </dd>
         *   <dt> visible </dt>
         *   <dd> Whether or not this bin is visible.  Defaults to true. </dd>
         * </dl>
         *
         * In addition, the default behaviour is to put the data for the bin
         * itself into a property on the node labeled, "bin".
         *
         * @param tileData The data associated with this tile.  This may be
         *                 null or undefined.
         * @param tileKey A key uniquely identifying this tile, which
         *                <em>must</em> be attached to all output records.
         *
         * @return An array of output records, each of which will be considered
         *         a node by the MapLayer's MapNodeLayer.
         */
        transformTileToBins: function (tileData, tilekey) {

            var x, y, binNum, bin, binRect, binData, results;

            results = [];

            if (tileData) {

                binNum = 0;
                for (y=0; y<tileData.yBinCount; ++y) {
                    for (x=0; x<tileData.xBinCount; ++x) {
                        bin =  {x: x, y: y};

                        binRect = this.tilePyramid.getBinBounds( tileData, bin );
                        
                        binData = {
                            binkey: this.createBinKey(tilekey, bin),
                            tilekey: tilekey,
                            longitude: binRect.minX, // top left of tile
                            latitude: binRect.maxY,
                            values: ( tileData.values[binNum] ) ? tileData.values[binNum].value : [],
                            meta: tileData.meta
                        };
                        results.push( binData );
                        ++binNum;
                    }
                }
            }
            return results;
        }

    });


    /**
     * Query all services instances for a given layer id by a property path. Returns an
     * array containing all values, including duplicates.
     * Valid search queries includes:
     *      'singleAttributes',
     *      'sub.attributes',
     *      'entireArrays[]',
     *      'specificArrayIndices[2]'
     *      'combinations.of[].the[0].above'
     */
    TileService.queryTiles = function( layerId, query, options ) {
        var services = serviceRegistry[ layerId ],
            tiles = [], results = [], nonDups = [],
            i;
        if ( !services || !query ) {
            return [];
        }
        options = options || {};
        // get all tile data in a single array
        for ( i=0; i<services.length; i++ ) {
            $.merge( tiles, services[i].getDataArray() );
        }
        // search tile data for query
        for ( i=0; i<tiles.length; i++ ) {
            $.merge( results, tileQueryParser( tiles[i], query.split('.') ) );
        }
        // remove duplicates if specified
        if ( options.removeDuplicates ) {
            $.each( results, function( i, element ) {
                if ( $.inArray( element, nonDups ) === -1) {
                    nonDups.push( element );
                }
            });
            results = nonDups;
        }
        return results;
    };


    /**
     * Checks all service instances for a given layer id and returns all data
     * held in memory, keyed by tilekey;
     */
    TileService.getTilesInMemory = function( layerId ) {
        var services = serviceRegistry[ layerId ],
            tiles = {},
            i;

        function assignData( data, key ) {
            tiles[key] = data;
        }

        if ( !services ) {
            return [];
        }
        // get all tile data in a single array
        for ( i=0; i<services.length; i++ ) {
            _.forIn( services[i].data, assignData );
        }
        return tiles;
    };


    /**
     * Checks all service instances for the given layer id, merges all tiles that are
     * in memory and returns them. Any missing tiles are requested and passed
     * to the given callback function.
     */
    TileService.requestTilesGlobal = function( layerId, tiles, callback ) {

        var tilesInMemory = this.getTilesInMemory( layerId ),
            services = serviceRegistry[ layerId ],
            service, tilesWithData = [],
            i;

        if ( !services ) {
            return [];
        }

        // doesn't matter which service instance we use
        service = services[0];

        function requestTile( tile ) {
            // request data from server
            aperture.io.rest(
                (service.source.apertureservice+'1.0.0/'+
                 service.source.id+'/'+
                 tile.level+'/'+
                 tile.xIndex+'/'+
                 tile.yIndex+'.json'),
                'GET',
                function( tileData ) {
                    var tile = tileData.index,
                        tilekey = tile.level + "," + tile.xIndex + "," + tile.yIndex;
                    if (tileData.tile !== undefined) {
                        callback( service.transformTileToBins( tileData.tile, tilekey ) );
                    }
                },
                // Add in the list of all needed tiles
                {'params': [] }
            );
        }

        for ( i=0; i<tiles.length; i++ ) {
            if ( !tilesInMemory[ tiles[i] ] ) {
                // request all given tiles not in memory
                requestTile( tiles[i] );
            }
        }

        _.forEach( tilesInMemory, function( value ) {
            if ( value.length > 0 ) {
                tilesWithData.push( value[0] );
            }

        });

        return tilesWithData;
    };

    return TileService;
});
