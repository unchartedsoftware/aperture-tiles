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
 * This module defines a TileTracker class which delegates tile requests between a
 * ViewController's individual views and a DataService. Maintains a collection of
 * relevant tiles for each view.
 */
define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        TileTracker;



    TileTracker = Class.extend({
        ClassName: "TileTracker",

        /**
         * Construct a TileTracker
         */
        init: function ( spec ) {

            this.renderer = spec.renderer;
            this.renderer.attachClientState(spec.clientState);    // attach the shared client state
            this.renderer.createLayer(spec.mapNodeLayer);   // create the render layer
            this.dataService = spec.dataService;
            this.tiles = [];
        },


        getRendererId: function() {
            return this.renderer.id;
        },


        getLayerId: function() {
            return this.dataService.layerInfo.layer;
        },

        /** add views renderer id to node data
         *
         * @param data      data to add renderer id to
         */
        addRendererIdToData: function( data ) {
            var i;
            if ( $.isArray( data ) ) {
                for (i=0; i<data.length; i++ ) {
                    data[i].rendererId = this.getRendererId();
                }
            } else {
                for (i in data) {
                    if (data.hasOwnProperty(i)) {
                        data[i].rendererId = this.getRendererId();
                    }
                }
            }
            return data;
        },

        /**
         * Given a list of tiles, determines which are already tracked, which need to be
         * requested from the dataService, and which need to be released from the dataService.
         * If a tile is already in memory, the callback function is ignored
         * @param visibleTiles an array of all visible tiles that will need to be displayed
         * @param callback tile receive callback function
         */
        filterAndRequestTiles: function(visibleTiles, tileSetBounds, callback) {

            var activeTiles = [],
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
                tileKey = this.dataService.createTileKey(tile);

                if (defunctTiles[tileKey]) {
                    // Already have the data, remove from defunct list
                    delete defunctTiles[tileKey];
                } else {
                    // New data.  Mark for fetch.
                    neededTiles.push(tileKey);
                }
                // And mark tile it as meaningful
                activeTiles.push(tileKey);
            }

            // Update our internal lists
            this.tiles = activeTiles;
            // Remove all old defunct tiles references
            for (tileKey in defunctTiles) {
                if (defunctTiles.hasOwnProperty(tileKey)) {
                    this.dataService.releaseData(tileKey);
                }
            }
            // Request needed tiles from dataService
            this.dataService.requestData(neededTiles, tileSetBounds, callback);
        },


        swapTileWith: function (otherTracker, tilekey) {
            otherTracker.tiles.push(tilekey);
            otherTracker.dataService.data[tilekey] = this.dataService.data[tilekey];

            this.tiles.splice(this.tiles.indexOf(tilekey), 1);
            this.dataService.releaseData(tilekey);
        },


        releaseTile: function(tilekey) {

            if (this.tiles.indexOf(tilekey) === -1) {
                // this tracker does not have requested tile, this function should not
                // have been called, return
                return;
            }
            // remove tile from tracked list
            this.tiles.splice(this.tiles.indexOf(tilekey), 1);
            // release data
            this.dataService.releaseData(tilekey);
        },


        requestTile: function(tilekey, callback) {
            if (this.tiles.indexOf(tilekey) === -1) {
                this.tiles.push(tilekey);
            }
            this.dataService.requestData([tilekey], {}, callback);
        },


        getTileData: function(tilekey) {
            this.dataService.data[tilekey].rendererId = this.getRendererId();
            return this.dataService.data[tilekey];
        },

        /**
         * Returns the data format of the tiles required by an aperture.geo.mapnodelayer
         */
        getDataArray: function ( tilekeys ) {

            var data;
            // if tilekeys are specified, return those
            if ( tilekeys !== undefined ) {
                if ( !$.isArray( tilekeys ) ) {
                    // only one tilekey specified, wrap in array
                    tilekeys = [tilekeys];
                }
                data =  this.dataService.getDataArray(tilekeys);
            } else {
                // otherwise, return all tiles currently tracked
                data =  this.dataService.getDataArray(this.tiles);
            }
            return this.addRendererIdToData( data );

        },


        /**
         * Returns the data format of the tiles required by an aperture.geo.mapnodelayer
         */
        getDataObject: function ( tilekeys ) {

            var data;
            // if tilekeys are specified, return those
            if ( tilekeys !== undefined ) {
                if ( !$.isArray( tilekeys ) ) {
                    // only one tilekey specified, wrap in array
                    tilekeys = [tilekeys];
                }
                data = this.dataService.getDataObject(tilekeys);
            } else {
                // otherwise, return all tiles currently tracked
                data = this.dataService.getDataObject(this.tiles);
            }
            return this.addRendererIdToData( data );

        }

    });

    return TileTracker;
});
