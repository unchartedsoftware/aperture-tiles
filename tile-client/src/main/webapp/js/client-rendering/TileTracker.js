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
        TileTracker;



    TileTracker = Class.extend({
        ClassName: "TileTracker",

        init: function (dataSet) {

            this.dataSet = dataSet;
            this.tiles = [];
        },


        filterAndRequestTiles: function(visibleTiles, callback) {

            var usedTiles = [],
                defunctTiles = {},
                neededTiles = [],
                i, tile, tileKey;

            // Copy out all keys from the current data.  As we go through
            // making new requests, we won't bother with tiles we've
            // already received, but we'll remove them from the defunct
            // list.  When we're done requesting data, we'll then know
            // which tiles are defunct, and can be removed from the data
            // set.
            for (i=0; i<this.tiles.length; ++i) {
                defunctTiles[this.tiles[i]] = true;
            }

            // Go through, seeing what we need.
            for (i=0; i<visibleTiles.length; ++i) {
                tile = visibleTiles[i];
                tileKey = this.dataSet.createTileKey(tile);

                if (defunctTiles[tileKey]) {
                    // Already have the data.
                    // Make sure not to delete it
                    delete defunctTiles[tileKey];
                    // And mark it as meaningful
                    usedTiles.push(tileKey);
                } else {
                    // New data.  Mark for fetch.
                    neededTiles.push(tileKey);
                    // And mark it as meaningful
                    usedTiles.push(tileKey);
                }
            }

            // Update our internal lists
            this.tiles = usedTiles;
            // Remove all old defunct tiles references
            for (tileKey in defunctTiles) {
                if (defunctTiles.hasOwnProperty(tileKey)) {
                    this.dataSet.removeReference(tileKey);
                }
            }

            this.dataSet.requestTiles(neededTiles, callback);
        },


        swapTileWith: function(newTracker, tilekey, callback) {

            // this function does extra reference count incrementing and decrementing
            // in order to prevent data de-allocation between releasing this trackers tile
            // and requesting the other trackers tile
            if (this.tiles.indexOf(tilekey) === -1) {
                // does not have requested tile, return
                return;
            }

            // prematurely increment reference in case other tracker shares data
            newTracker.dataSet.addReference(tilekey);
            // release tile, this decrements reference. If data is shared, the data
            // reference count is set to 1, preventing unnecessary de-allocation
            //this.releaseTile(tilekey);
            this.tiles.splice(this.tiles.indexOf(tilekey), 1);
            this.dataSet.removeReference(tilekey);

            // request tile, incrementing reference count again to 2
            newTracker.requestTile(tilekey, callback);

            // remove premature reference, resulting in proper count of 1
            newTracker.dataSet.removeReference(tilekey);
        },


        requestTile: function(tilekey, callback) {
            if (this.tiles.indexOf(tilekey) === -1) {
                this.tiles.push(tilekey);
            }
            this.dataSet.requestTiles([tilekey], callback);
        },


        getNodeData: function () {
            // request needed tiles
            return this.dataSet.getTileNodeData(this.tiles);
        }

    });

    return TileTracker;
});
