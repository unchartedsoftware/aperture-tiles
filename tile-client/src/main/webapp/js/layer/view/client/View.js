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
        ClassName: "View",

        /**
         * Construct a View
         */
        init: function ( spec ) {

            this.renderer = spec.renderer;
            this.renderer.attachClientState(spec.clientState);    // attach the shared client state
            this.dataService = spec.dataService;
            this.tiles = [];
        },


        getLayerId: function() {
            return this.dataService.layerInfo.layer;
        },


        /**
         * Given a list of tiles, determines which are already tracked, which need to be
         * requested from the dataService, and which need to be released from the dataService.
         * If a tile is already in memory, the callback function is ignored
         * @param visibleTiles an array of all visible tiles that will need to be displayed
         * @param callback tile receive callback function
         */
        updateTiles: function( visibleTiles, tileSetBounds ) {

            var activeTiles = [],
                defunctTiles = {},
                neededTiles = [],
                i, tile, tilekey;

            // keep track of current tiles to ensure we know
            // which ones no longer exist
            for (i=0; i<this.tiles.length; ++i) {
                defunctTiles[this.tiles[i]] = true;
            }

            // Go through, seeing what we need.
            for (i=0; i<visibleTiles.length; ++i) {
                tile = visibleTiles[i];
                tilekey = this.dataService.createTileKey(tile);

                if (defunctTiles[tilekey]) {
                    // Already have the data, remove from defunct list
                    delete defunctTiles[tilekey];
                } else {
                    // New data.  Mark for fetch.
                    neededTiles.push(tilekey);
                }
                // And mark tile it as meaningful
                activeTiles.push(tilekey);
            }

            // Update our internal lists
            this.tiles = activeTiles;
            // Remove all old defunct tiles references
            for (tilekey in defunctTiles) {
                if (defunctTiles.hasOwnProperty(tilekey)) {
                    this.dataService.releaseData(tilekey);
                }
            }
            // Request needed tiles from dataService
            this.dataService.requestData( neededTiles, tileSetBounds, $.proxy( this.redraw, this ) );
        },


        swapTileWith: function( newView, tilekey ) {

            if ( this.getLayerId() === newView.getLayerId() ) {

                // if both views share the same type of data source, swap tile data
                // give tile to new view
                newView.giveTile( tilekey, this.dataService.data[tilekey] );
                newView.redraw( tilekey );

            } else {
                // otherwise release and request new data
                if (tilekey === this.renderer.clientState.clickState.tilekey ) {
                    // if same tile as clicked tile, un-select elements from this view
                    this.renderer.clientState.clickState = {};
                }
                newView.requestTile( tilekey );
            }
            this.releaseTile( tilekey ); // release tile from this view
            this.redraw( tilekey );      // redraw tile

        },


        releaseTile: function(tilekey) {

            if (this.tiles.indexOf(tilekey) === -1) {
                // this view does not have requested tile, this function should not
                // have been called, return
                return;
            }
            this.tiles.splice(this.tiles.indexOf(tilekey), 1); // remove tile from tracked list
            this.dataService.releaseData(tilekey); // release data
        },


        requestTile: function(tilekey, callback) {

            if (this.tiles.indexOf(tilekey) !== -1) {
                // this view already has the requested tile, this function should not
                // have been called, return
                return;
            }
            this.tiles.push(tilekey); // add tile
            this.dataService.requestData( [tilekey], {}, $.proxy( this.redraw, this ) ); // request data
        },


        giveTile: function( tilekey, data ) {

            if (this.tiles.indexOf(tilekey) !== -1) {
                // this view already has the gifted tile, this function should not
                // have been called, return
                return;
            }
            this.tiles.push(tilekey); // add tile
            this.dataService.data[tilekey] = data; // set data
        },


        redraw: function( tilekey ) {
            this.renderer.redraw( this.getDataArray() );
        },


        /**
         * Returns the data of the tiles in an array
         */
        getDataArray: function ( tilekeys ) {

            if ( tilekeys === undefined ) {
                tilekeys = this.tiles;
            } else if ( !$.isArray( tilekeys ) ) {
                tilekeys = [ tilekeys ];
            }
            return this.dataService.getDataArray( tilekeys );
        },


        /**
         * Returns the data of the tiles as an object keyed by tilekey
         */
        getDataObject: function ( tilekeys ) {

            if ( tilekeys === undefined ) {
                tilekeys = this.tiles;
            } else if ( !$.isArray( tilekeys ) ) {
                tilekeys = [ tilekeys ];
            }
            return this.dataService.getDataObject( tilekeys );
        }

    });

    return TileTracker;
});
