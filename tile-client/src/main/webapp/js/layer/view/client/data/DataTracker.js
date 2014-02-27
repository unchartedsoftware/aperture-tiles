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
 * This module defines a DataTracker class which manages all tile data from a
 * single dataset.
 */
define(function (require) {
    "use strict";

    var Class = require('../../../../class'),
        WebPyramid = require('../../../../binning/WebTilePyramid'),
        webPyramid,
        getArrayLength,
        DataTracker;

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


    webPyramid = new WebPyramid();


    DataTracker = Class.extend({
        ClassName: "DataTracker",

        /**
         * Construct a DataTracker
         */
        init: function (layerInfo) {

            // All the data we've received, put in one object, keyed by
            // tile key (as defined by the createTileKey method).
            this.layerInfo = layerInfo;
            this.data = {};
            this.referenceCount = {};
            this.dataStatus = {};
            this.dataCallbacks = [];
            this.memoryQueue = [];
            // The relative position within each bin at which visuals will 
            // be drawn
            this.position = {x: 'minX', y: 'centerY'}; //{x: 'centerX', y: 'centerY'};
            this.isMercatorProjected = true;        // set default to mercator projected
        },


        /**
         * Create a universal unique key for a given tile
         *
         * @param tilekeys Array of tile identification keys to merge into node data
         */
        getTileNodeData: function (tilekeys) {
            var allData = [];
            $.each(this.data, function (element, value) {
                // assemble only requested tiles
                if (tilekeys.indexOf(element) !== -1) {
                    $.merge(allData, value);
                }
            });
            return allData;
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
         * Increment reference count for specific tile data
         *
         * @param tilekey tile identification key for data
         */
        addReference: function(tilekey) {

            if (this.referenceCount[tilekey] === undefined) {
                this.referenceCount[tilekey] = 0;
            }

            // ensure fresh requests cause tile to be pushed onto the back of queue
            if (this.memoryQueue.indexOf(tilekey) !== -1) {
                this.memoryQueue.splice(this.memoryQueue.indexOf(tilekey), 1);
            }
            this.memoryQueue.push(tilekey);

            this.referenceCount[tilekey]++;
        },


        /**
         * Decrement reference count for specific tile data, if reference count hits 0,
         * call memory management function
         *
         * @param tilekey tile identification key for data
         */
        removeReference: function(tilekey) {

            this.referenceCount[tilekey]--;
            if (this.referenceCount[tilekey] === 0) {
                this.memoryManagement();
            }

        },


        /**
         * Returns current reference count of a tile
         *
         * @param tilekey tile identification key for data
         */
        getReferenceCount: function(tilekey) {
            if (this.referenceCount[tilekey] === undefined) {
                return 0;
            }
            return this.referenceCount[tilekey];
        },


        /**
         * Manages how defunct tile data is de-allocated, once max number
         * of in memory tiles is reached, de-allocates tiles that were
         * used longest ago. Current max tile count is 100
         */
        memoryManagement: function() {

            var i = 0,
                tilekey,
                MAX_NUMBER_OF_ENTRIES = 100;

            while (this.memoryQueue.length > MAX_NUMBER_OF_ENTRIES) {

                // while over limit of tiles in memory,
                // iterate from last used tile to most recent and remove them if
                // reference counts are 0
                tilekey = this.memoryQueue[i];
                if (this.getReferenceCount(tilekey) === 0) {
                    delete this.data[tilekey];
                    delete this.dataStatus[tilekey];
                    delete this.referenceCount[tilekey];
                    this.memoryQueue.splice(i, 1);
                }
                i++;
            }

        },


        /**
         * Requests tile data. If tile is not in memory, send GET request to server and
         * set individual callback function on receive. Callback is not called if tile
         * is already in memory
         *
         * @param requestedTiles array of requested tilekeys
         * @param callback callback function
         */
        requestTiles: function(requestedTiles, callback) {
            var i;
            // send request to respective coordinator
            for (i=0; i<requestedTiles.length; ++i) {
                this.getData(requestedTiles[i], callback);
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
        getData: function(tilekey, callback) {

            var parsedValues = tilekey.split(','),
                level = parseInt(parsedValues[0], 10),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10);

            if (this.dataStatus[tilekey] === undefined) {

                // flag tile as loading, add callback to list
                this.dataStatus[tilekey] = "loading";
                this.dataCallbacks[tilekey] = [];
                this.dataCallbacks[tilekey].push(callback);

                // request data from server
                aperture.io.rest(
                    (this.layerInfo.apertureservice+'1.0.0/'+
                     this.layerInfo.layer+'/'+
                     level+'/'+
                     xIndex+'/'+
                     yIndex+'.json'),
                     'GET',
                    $.proxy(this.receiveDataCallback, this)
                );
                this.addReference(tilekey);

            } else {

				
                this.addReference(tilekey);
                if (this.dataStatus[tilekey] === "loaded") {
                    return true;
                }
                // waiting on tile from server, add to callback list
                this.dataCallbacks[tilekey].push(callback);
            }

            return false;
        },


        /**
         * Called when a tile is received from server, flags tilekey as 'loaded' and
         * calls every callback function is respective array
         *
         * @param tileData received tile data from server
         */
        receiveDataCallback: function(tileData) {

            // create tile key: "level, xIndex, yIndex"
            var tilekey = this.createTileKey(tileData.index),
                i;

            this.data[tilekey] = this.transformTileToBins(tileData.tile, tilekey);
            this.dataStatus[tilekey] = "loaded"; // flag as loaded

            if (this.data[tilekey].length > 0) {
                if (this.dataCallbacks[tilekey] === undefined) {
                    console.log('ERROR: Received tile out of sync from server... ');
                    return;
                }
                for (i =0; i <this.dataCallbacks[tilekey].length; i++ ) {
                    this.dataCallbacks[tilekey][i](this.data[tilekey]);
                }
            }

            delete this.dataCallbacks[tilekey];
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
         * Returns the bin rectangle based on tiledata and bin x/y. Will mercator project
         * if isMercatorProjected is set to true
         *
         * @param tileData The data associated with this tile.
         * @param bin      The bin x and y values
         */
        getBinRect: function(tileData, bin) {
            var binRect;
            if (this.isMercatorProjected) {
                binRect = webPyramid.getBinBoundsMercator(tileData, bin);
            } else {
                binRect = webPyramid.getBinBounds(tileData, bin);
            }
            return binRect;
        },


        /**
         * Transforms a tile's worth of data into a series of bins of data
         *
         * This can be overridden; most of the result is use-specific - There are,
         * however, a few properties used by the ClientLayer and the
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

                        binRect = this.getBinRect(tileData, bin);
                        
                        binData = {
                            level: tileData.level,
                            binkey: this.createBinKey(tileKey, bin),
                            longitude: binRect[this.position.x],
                            latitude: binRect[this.position.y],
                            visible: true,
                            bin: tileData.values[binNum],
                            tilekey: tileKey
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
            return binRect = this.getBinRect(tileData, bin);
        }
    });

    return DataTracker;
});
