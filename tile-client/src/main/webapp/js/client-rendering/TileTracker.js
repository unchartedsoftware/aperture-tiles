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
 * This module defines a TileTracker class which delegates tile requests between a
 * ViewController's individual views and a DataTracker. Maintains a collection of
 * relevant tiles for each view.
 */
define(function (require) {
    "use strict";



    var Class = require('../profileclass'),
        TileTracker;



    TileTracker = Class.extend({
        ClassName: "TileTracker",

        /**
         * Construct a TileTracker
         */
        init: function (dataTracker) {

            this.dataTracker = dataTracker;
            this.tiles = [];
        },


        /**
         * Given a list of tiles, determines which are already tracked, which need to be
         * requested from the DataTracker, and which need to be released from the DataTracker. Calls
         * callback function once for each tile received from the server, and once if there is at least
         * one tile already in memory
         * @param visibleTiles an array of all visible tiles that will need to be displayed
         * @param callback tile receive callback function
         */
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
                tileKey = this.dataTracker.createTileKey(tile);

                if (defunctTiles[tileKey]) {
                    // Already have the data, remove from defunct list
                    delete defunctTiles[tileKey];
                } else {
                    // New data.  Mark for fetch.
                    neededTiles.push(tileKey);
                }
                // And mark tile it as meaningful
                usedTiles.push(tileKey);
            }

            // Update our internal lists
            this.tiles = usedTiles;
            // Remove all old defunct tiles references
            for (tileKey in defunctTiles) {
                if (defunctTiles.hasOwnProperty(tileKey)) {
                    this.dataTracker.removeReference(tileKey);
                }
            }
            // Request needed tiles from dataTracker
            this.dataTracker.requestTiles(neededTiles, callback);
        },


        /**
         * Given another TileTracker and tilekey, swaps the tiles. This function ensures that
         * if the DataTracker is shared between trackers, the data will not be de-allocated
         * from memory during the swap.
         * @param newTracker the tracker to gain ownership of the tile
         * @param tilekey the tile indentification key
         * @param callback the callback function after the swap is complete
         */
        swapTileWith: function(newTracker, tilekey, callback) {

            // this function does extra reference count incrementing and decrementing
            // in order to prevent data de-allocation between releasing this trackers tile
            // and requesting the other trackers tile
            if (this.tiles.indexOf(tilekey) === -1) {
                // does not have requested tile, return
                return;
            }

            // prematurely increment reference in case other tracker shares data
            newTracker.dataTracker.addReference(tilekey);
            // release tile, this decrements reference. If data is shared, the data
            // reference count is set to 1, preventing unnecessary de-allocation
            //this.releaseTile(tilekey);
            this.tiles.splice(this.tiles.indexOf(tilekey), 1);
            this.dataTracker.removeReference(tilekey);

            // request tile, incrementing reference count again to 2
            newTracker.requestTile(tilekey, callback);

            // remove premature reference, resulting in proper count of 1
            newTracker.dataTracker.removeReference(tilekey);
        },


        /**
         * Request a tile from the dataTracker
         * @param tilekey the tile indentification key
         * @param callback the callback function after the tile is received
         */
        requestTile: function(tilekey, callback) {
            if (this.tiles.indexOf(tilekey) === -1) {
                this.tiles.push(tilekey);
            }
            this.dataTracker.requestTiles([tilekey], callback);
        },


        /**
         * Returns the data format of the tiles required by an aperture.nodelayer
         */
        getNodeData: function () {
            // request needed tiles
            return this.dataTracker.getTileNodeData(this.tiles);
        }

    });

    return TileTracker;
});
