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



    var Class = require('../../../../class'),
        TileService;



    TileService = Class.extend({
        ClassName: "TileService",

        /**
         * Construct a TileService
         */
        init: function (layerInfo, tilepyramid) {

            // current tile data
            this.data = {};
            // tiles flagged as actively requested and waiting on
            this.waitingOnTile = {};
            // callbacks
            this.dataCallback = {};
            // layer info
            this.layerInfo = layerInfo;

            // The relative position within each bin at which visuals will 
            // be drawn
            this.position = {x: 'minX', y: 'centerY'}; //{x: 'centerX', y: 'centerY'};

            // set tile pyramid type
            this.tilePyramid = tilepyramid;
        },


        getDataArray: function ( tilekeys ) {
            var i,
                allData = [];

            for(i=0; i<tilekeys.length; i++) {
                // if data exists in tile
                if ( this.data[ tilekeys[i] ] !== undefined ) {
                    // check format of data
                    if ( $.isArray( this.data[ tilekeys[i] ] ) ) {
                        // for each tile, data is an array, merge it together
                        $.merge( allData, this.data[ tilekeys[i] ] );
                    } else {
                        // for each tile, data is an object
                        allData.push( this.data[ tilekeys[i] ] );
                    }
                }
            }
            return allData;
        },


        getDataObject: function ( tilekeys ) {
            var i,
                allData = {};

            for(i=0; i<tilekeys.length; i++) {
                // if data exists in tile
                if ( this.data[ tilekeys[i] ] !== undefined ) {
                    allData[ tilekeys[i] ] = this.data[ tilekeys[i] ];
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
            delete this.waitingOnTile[tilekey];
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
        requestData: function(requestedTiles, tileSetBounds, callback) {
            var i;
            // send request to respective coordinator
            for (i=0; i<requestedTiles.length; ++i) {
                this.getRequest(requestedTiles[i], tileSetBounds, callback);
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
        getRequest: function(tilekey, tileSetBounds, callback) {

            var parsedValues = tilekey.split(','),
                level = parseInt(parsedValues[0], 10),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10);

            // ensure we only send a request once
            if (this.waitingOnTile[tilekey] === undefined) {

                // flag tile as loading and stash callback
                this.waitingOnTile[tilekey] = true;
                this.dataCallback[tilekey] = callback;

                // request data from server
                aperture.io.rest(
                    (this.layerInfo.apertureservice+'1.0.0/'+
                     this.layerInfo.layer+'/'+
                     level+'/'+
                     xIndex+'/'+
                     yIndex+'.json'),
                     'GET',
                    $.proxy(this.getCallback, this),
                    // Add in the list of all needed tiles
                    tileSetBounds
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
            var tilekey = this.createTileKey(tileData.index);

            // ensure we still need the tile
            if (this.waitingOnTile[tilekey] === true) {

                this.data[tilekey] = this.transformTileToBins(tileData.tile, tilekey);
                this.dataCallback[tilekey]( tileData );

                // clear callbacks and 'waiting on' status
                delete this.waitingOnTile[tilekey];
                delete this.dataCallback[tilekey];
            }
        },


        /**
         * Set the position of visuals on this layer relative to the tile
         * location.
         *
         * @param position One of ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW',
         *                 'Center'], indicating the corner, side center, or
         *                 tile center, as appropriate.
         */
        setPosition: function (position) {
            var stdpos = position.toLowerCase();

            switch (stdpos) {
                case 'n':
                case 'north':
                    this.position = {x: 'centerX', y: 'maxY'};
                    break;
                case 'ne':
                case 'northeast':
                    this.position = {x: 'maxX', y: 'maxY'};
                    break;
                case 'e':
                case 'east':
                    this.position = {x: 'maxX', y: 'centerY'};
                    break;
                case 'se':
                case 'southeast':
                    this.position = {x: 'maxX', y: 'minY'};
                    break;
                case 's':
                case 'south':
                    this.position = {x: 'centerX', y: 'minY'};
                    break;
                case 'sw':
                case 'southwest':
                    this.position = {x: 'minX', y: 'minY'};
                    break;
                case 'w':
                case 'west':
                    this.position = {x: 'minX', y: 'centerY'};
                    break;
                case 'nw':
                case 'northwest':
                    this.position = {x: 'minX', y: 'maxY'};
                    break;
                case 'c':
                case 'center':
                case 'centre':
                    this.position = {x: 'centerX', y: 'centerY'};
                    break;
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
        transformTileToBins: function (tileData, tileKey) {

            var x, y, binNum, bin, binRect, binData, results;

            results = [];
            if (tileData) {
                binNum = 0;
                for (y=0; y<tileData.yBinCount; ++y) {
                    for (x=0; x<tileData.xBinCount; ++x) {
                        bin =  {x: x, y: y};

                        binRect = this.tilePyramid.getBinBounds(tileData, bin);
                        
                        binData = {
                            level: tileData.level,
                            binkey: this.createBinKey(tileKey, bin),
                            longitude: binRect[this.position.x],
                            latitude: binRect[this.position.y],
                            visible: true,
                            bin: tileData.values[binNum],
                            tilekey: tileKey,
                            layerId: this.layerInfo.layer
                        };
                        results.push( binData );
                        ++binNum;
                    }
                }
            }
            return results;
        }

    });

    return TileService;
});
