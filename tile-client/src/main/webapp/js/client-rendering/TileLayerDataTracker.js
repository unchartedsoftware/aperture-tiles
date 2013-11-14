/**
 * Copyright (c) 2013 Oculus Info Inc.
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
 * This module collects a single layer's worth of tile data sent from the 
 * server, and keeps track of what a layer needs to know to display it.
 */
define(function (require) {
    "use strict";



    var Class = require('../profileclass'),
        WebPyramid = require('./WebTilePyramid'),
        TileLayerDataTracker,
        getArrayLength,
        webPyramid;



    webPyramid = new WebPyramid();



    /*
     * Get the length of this array property, if it is an array property.
     * If not, return false;
     */
    getArrayLength = function (binData) {
        var length = $(binData).length,
            i;
        if (0 === length) {
            return 0;
        }
        for (i=0; i<length; ++i) {
            if (!binData[i]) {
                return false;
            }
        }
        return length;
    };
        

    //
    // The data for each tile comes across from the server as follows:
    // {
    //   tileIndex: {
    //     x: <tile x coordinate>
    //     y: <tile y coordinate>
    //     level: <tile zoom level>
    //   },
    //   tile: { // Optional; absence means the tile wasn't in the pyramid
    //     // Properties from tile.avsc
    //     level: <tile zoom level>,
    //     xIndex: <tile x coordinate>,
    //     yIndex: <tile y coordinate>,
    //     xBinCount: <number of bins per tile along the x axis>,
    //     yBinCount: <number of bins per tile along the y axis>,
    //     default: <value to use as a default for missing bins>,
    //     meta: <creator-defined metadata>,
    //     values: [
    //       <bin contents>,
    //       <bin contents>,
    //       ....
    //       <bin contents>
    //     ]
    //   }
    // }


    TileLayerDataTracker = Class.extend({
        ClassName: "TileLayerDataTracker",
        init: function () {
            // An array of tile keys of each tile we've requested (whether or not
            // we've received them)
            this.tiles = [];
            // All the data we've recieved, put in one object, keyed by 
            // tile key (as defined by the createTileKey method).
            this.data = {};
            // The node layer for which we are tracking data
            this.nodeLayer = null;

            // The relative position within each bin at which visuals will 
            // be drawn
            this.position = {x: 'centerX', y: 'centerY'};
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
         * Sets the node layer for which this object is tracking data
         */
        setNodeLayer: function (nodeLayer) {
            this.nodeLayer = nodeLayer;
        },

        /**
         * Create a universal unique key for a given tile
         *
         * @param tile Some description of a tile.  This can be the tile
         *             index, or the tile itself.
         */
        createTileKey: function (tile) {
            return tile.level + "," + tile.xIndex + "," + tile.yIndex;
        },

        /*
         * Create a universal unique key for a given bin in a given tile
         *
         * @param tileKey The universal tile key, as calculated by the
         *                createTileKey method.
         * @param bin The bin; assumed to be in {x: #, y: #} format.
         */
        createBinKey: function (tileKey, bin) {
            return tileKey + ":" + bin.x + "," + bin.y;
        },




        //////////////////////////////////////////////////////////////////////
        // Section: Data-tracking methods
        // Methods used to track data as it comes in
        //
        /**
         * Given a set of tiles intersecting the visible region of the map,
         * determine for which we already have data.
         *
         * While we're at it, remove any old data we're storing that doesn't 
         * match the new region.
         *
         * @param visibleTiles An array of tile indices (not keys) for each 
         *                     and every tile currently visible on the map
         * @param An array containing the subset of visibleTiles that we
         *        don't already contain.
         */
        filterTiles: function (visibleTiles) {
            var usedTiles, defunctTiles,
                neededTiles,
                i, tile, tileKey;

            // Copy out all keys from the current data.  As we go through
            // making new requests, we won't bother with tiles we've 
            // already received, but we'll remove them from the defunct 
            // list.  When we're done requesting data, we'll then know 
            // which tiles are defunct, and can be removed from the data 
            // set.
            defunctTiles = {};
            usedTiles = [];
            for (i=0; i<this.tiles.length; ++i) {
                defunctTiles[this.tiles[i]] = true;
            }

            neededTiles = [];
            // Go through, seeing what we need.
            for (i=0; i<visibleTiles.length; ++i) {
                tile = visibleTiles[i];
                tileKey = this.createTileKey(tile);

                if (defunctTiles[tileKey]) {
                    // Already have the data.
                    // Make sure not to delete it
                    delete defunctTiles[tileKey];
                    // And mark it as meaningful
                    usedTiles[usedTiles.length] = tileKey;
                } else {
                    // New data.  Mark for fetch.
                    neededTiles[neededTiles.length] = tile;
                    // And mark it as meaningful
                    usedTiles[usedTiles.length] = tileKey;
                }
            }

            // Update our internal lists
            this.tiles = usedTiles;
            for (tileKey in defunctTiles) {
                if (defunctTiles.hasOwnProperty(tileKey)) {
                    delete this.data[tileKey];
                }
            }

            return neededTiles;
        },

        /**
         * Add tile data as returned from the server
         */
        addTileData: function (tileData) {
            var tileKey = this.createTileKey(tileData.index),
                binData = this.transformTileToBins(tileData.tile, tileKey);

            // Ignore data for tiles from previous, now defunct updates.
            if (-1 !== this.tiles.indexOf(tileKey)) {
                this.data[tileKey] = binData;

                if (this.nodeLayer) {
                    this.nodeLayer.join(this.mergeData(), "binkey");
                    this.nodeLayer.all().redraw();
                }
            }
        },
        mergeData: function () {
            var allData = [];
            $.each(this.data, function (index, value) {
                $.merge(allData, value);
            });
            return allData;
        },

        /**
         * Transforms a tile's worth of data into a series of bins of data
         *
         * This can be overridden; most of the result is use-specific - the
         * results of this transformation are passed to the layer(s) added 
         * to the node layer (in some subclassing of MapLayer).  There are,
         * however, a few properties used by the MapLayer and the 
         * TileLayerDataTracker themselves, which must be correctly set here 
         * in each bin object:
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
                        binRect = webPyramid.getBinBounds(tileData, bin);

                        binData = {
                            level: tileData.level,
                            binkey: this.createBinKey(tileKey, bin),
                            longitude: binRect[this.position.x],
                            latitude: binRect[this.position.y],
                            visible: true,
                            bin: tileData.values[binNum]
                        };
                        results[results.length] = binData;

                        ++binNum;
                    }
                }
            }

            return results;
        },


        //////////////////////////////////////////////////////////////////////
        // Section: Aperture Interpretation
        // Methods to interpret the data for Aperture drawing methods
        //

        /**
         * Get the total number of data points about which aperture will
         * care
         */
        getCount: function () {
            return this.tilesWithData.length * this.xBinCount * this.yBinCount;
        },

        /**
         * Calculates the number of values in the nth data point.
         */
        getNumValues: function (n) {
            var binsPerTile = this.xBinCount * this.yBinCount,
                tileNum = Math.floor(n / binsPerTile),
                tileKey = this.tilesWithData[tileNum],
                tileData = this.data[tileKey],
                indexInTile = tileNum * binsPerTile,
                binData = tileData.values[indexInTile],
                length = getArrayLength(binData);

            if (false === length) {
                return 1;
            }
            return length;
        },

        /**
         * Calculates the ith value of the nth data point
         */
        getValue: function (n, i) {
            var binsPerTile = this.xBinCount * this.yBinCount,
                tileNum = Math.floor(n / binsPerTile),
                tileKey = this.tilesWithData[tileNum],
                tileData = this.data[tileKey],
                indexInTile = tileNum * binsPerTile,
                binData = tileData.values[indexInTile],
                length = getArrayLength(binData);

            if (false === length) {
                return binData;
            }
            if (i < length) {
                return binData[i];
            }
            return null;
        },

        /**
         * Calculates the location of the nth data point.
         * This returns a rectangle object, with minX, minY, maxX, maxY,
         * centerX, and centerY values; it is up to the user to choose 
         * which to use.
         */
        getLocation: function (n) {
            var binsPerTile = this.xBinCount * this.yBinCount,
                tileNum = Math.floor(n / binsPerTile),
                tileKey = this.tilesWithData[tileNum],
                tileData = this.data[tileKey],
                indexInTile = tileNum * binsPerTile,
                bin = {
                    x: indexInTile % this.xBinCount,
                    y: Math.floor(indexInTile / this.xBinCount)
                },
                tile = {
                    xIndex:    tileData.xIndex,
                    yIndex:    tileData.yIndex,
                    level:     tileData.level,
                    xBinCount: tileData.xBinCount,
                    yBinCount: tileData.yBinCount
                };

            return this.webPyramid.getBinBounds(tile, bin);
        }
    });

    return TileLayerDataTracker;
});
