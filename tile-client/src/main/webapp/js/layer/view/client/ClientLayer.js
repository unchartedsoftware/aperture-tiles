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

/*global OpenLayers*/

/**
 * This module defines a ClientLayer class which provides delegation between multiple client renderers
 * and their respective data sources. Each view is composed of a ClientRenderer and DataService. Each view has
 * a View which delegates tile requests from the view and its DataService.
 *
 * Each DataService manages a unique set of data. Upon tile requests, if data for the tile is not held in memory, it is pulled from the server.
 * Each View manages the set of tiles that a currently visible within a particular view.
 * Each ClientRenderer is responsible for drawing only the elements currently visible within a specific view.
 *
 */
define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        View  = require('./View'),
        ClientState   = require('./ClientState'),
		clientState = new ClientState(),	// global client state
        ClientLayer;



    ClientLayer = Class.extend({

        /**
         * Construct a ClientLayer
		 * @param spec The ClientLayer specification object of the form:
		 *				{
		 *					map : 	aperture.js map
		 *					views : array of views of the form:
		 *							[{
		 *								dataService : 	the DataService from which to pull the tile data
		 *								renderer : 		the ClientRenderer to draw the view data
		 *							}]
		 *
         */
        init: function ( id, map ) {

            var that = this;

            // initialize attributes
            this.id = id;
            this.defaultViewIndex = 0;
            this.tileViewMap = {};      	// maps a tile key to its view index
            this.views = [];				// array of all views
			this.clientState = clientState; 	// global client state to be shared by all views
            this.opacity = 1.0;
            this.isVisible = true;

            this.map = map;
            this.map.on('move', function() {
                // cal map update on pan end
                that.onMapUpdate();
            });
        },


        setViews: function( viewSpecs ) {

            var that =  this,
                i;

            // add views
            for (i = 0; i < viewSpecs.length; i++) {
                viewSpecs[i].clientState = that.clientState;
                that.views.push( new View( viewSpecs[i] ) );
            }

            // trigger callback to draw first frame
            this.onMapUpdate();
        },


        setOpacity: function( opacity ) {

            //this.clientState.setSharedState('opacity', opacity);
            var i;
            for (i=0; i<this.views.length; ++i) {
                this.views[i].renderer.setOpacity( opacity );
            }
        },


        setVisibility: function( visible ) {
            //this.clientState.setSharedState('isVisible', visible);
            var i;
            for (i=0; i<this.views.length; ++i) {
                this.views[i].renderer.setVisibility( visible );
            }
        },


        /**
         * Maps a tilekey to its current view index. If none is specified, use default
         * @param tilekey tile identification key of the form: "level,x,y"
         */
        getTileViewIndex: function(tilekey) {
            // given a tile key "level + "," + xIndex + "," + yIndex"
            // return the view index
            var viewIndex;
            if ( this.tileViewMap[tilekey] === undefined ) {
                viewIndex = this.defaultViewIndex;
            } else {
                viewIndex = this.tileViewMap[tilekey];
            }
            return viewIndex;
        },


        /**
         * Tile change callback function
         * @param tilekey 		tile identification key of the form: "level,x,y"
         * @param newViewIndex  the new index to set the tilekey to
         */
        onTileViewChange: function(tilekey, newViewIndex) {

            var oldViewIndex = this.getTileViewIndex(tilekey),  // old view index for tile
                oldView = this.views[oldViewIndex],
                newView = this.views[newViewIndex];

            // map tile to new view
            this.tileViewMap[tilekey] = newViewIndex;
            // swap tile between views
            oldView.swapTileWith(newView, tilekey);
        },


        /**
         * Map update callback, this function is called when the map view state is updating. Requests
         * and draws any new tiles
         */
        onMapUpdate: function() {

            var i,
                tiles,
                viewIndex,
                tilesByView = [],
                tileViewBounds;

            if (this.views.length === 0) {
                return;
            }

            for (i=0; i<this.views.length; ++i) {
                tilesByView[i] = [];
            }

            // determine all tiles in view
            tiles = this.map.getTilesInView();
            tileViewBounds = this.map.getTileBoundsInView();

            // group tiles by view index
            for (i=0; i<tiles.length; ++i) {
                viewIndex = this.getTileViewIndex( tiles[i].level+','+
                                                   tiles[i].xIndex+','+
                                                   tiles[i].yIndex );
                tilesByView[viewIndex].push( tiles[i] );
            }

            for (i=0; i<this.views.length; ++i) {
                // find which tiles we need for each view from respective
                this.views[i].updateTiles( tilesByView[i], tileViewBounds );
            }
        }

     });

    return ClientLayer;
});
