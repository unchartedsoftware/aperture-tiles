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
 * This module defines a ViewController class which provides delegation between multiple client renderers
 * and their respective data sources. Each view is composed of a ClientRenderer and DataService. Each view has
 * a TileTracker which delegates tile requests from the view and its DataService.
 *
 * Each DataService manages a unique set of data. Upon tile requests, if data for the tile is not held in memory, it is pulled from the server.
 * Each TileTracker manages the set of tiles that a currently visible within a particular view.
 * Each ClientRenderer is responsible for drawing only the elements currently visible within a specific view.
 *
 */
define(function (require) {
    "use strict";



    var Class        = require('../../../class'),
        TileTracker  = require('../../TileTracker'),
        MouseState   = require('./MouseState'),
        permData     = [],  // temporary aperture.js bug workaround //
		mapNodeLayer = {},
		mouseState = new MouseState(),	// global mouse state
        ViewController;



    ViewController = Class.extend({

        /**
         * Construct a ViewController
		 * @param spec The ViewController specification object of the form:
		 *				{
		 *					map : 	aperture.js map
		 *					views : array of views of the form:
		 *							[{
		 *								dataService : 	the DataService from which to pull the tile data
		 *								renderer : 		the ClientRenderer to draw the view data
		 *							}]
		 *
         */
        init: function (spec) {

            var that = this,
                i;

            function attachMap(map) {

                // add map and zoom/pan event handlers
                that.map = map;

                // mouse event handlers
                that.map.on('click', function() {
					// if click event has not been swallowed yet, clear mouse state and redraw
					that.mouseState.clearClickState();
					that.mapNodeLayer.all().redraw();
				});

                that.map.on('zoomend', function() {
					// clear click mouse state on zoom and call map update function
                    that.mouseState.clearClickState();
                    permData = [];  // reset temporary fix on zoom
                    that.onMapUpdate();
                });

                that.map.on('panend', function() {
					// cal map update on pan end
                    that.onMapUpdate();
                });
				
				// ensure only one mapNodeLayer is made for each unique map
				if (mapNodeLayer[that.map.getUid()] === undefined) {
					mapNodeLayer[that.map.getUid()] = that.map.addApertureLayer(aperture.geo.MapNodeLayer);
				}

                that.mapNodeLayer = mapNodeLayer[that.map.getUid()];
                that.mapNodeLayer.map('longitude').from('longitude');
                that.mapNodeLayer.map('latitude').from('latitude');
                // Necessary so that aperture won't place labels and texts willy-nilly
                that.mapNodeLayer.map('width').asValue(1);
                that.mapNodeLayer.map('height').asValue(1);
            }

            function addView(viewspec) {
                // construct and add view
                var view = {
                    // view id, used to map tile to view via getTileViewIndex()
                    id: viewspec.renderer.id,
                    // tracks the active tiles used per view at given moment
                    tileTracker: new TileTracker(viewspec.dataService),
                    // render layer for the view
                    renderer: viewspec.renderer
                };
                // attach the shared mouse state so that renderers can act in unison
                view.renderer.attachMouseState(that.mouseState);
                // create the render layer
                view.renderer.createLayer(that.mapNodeLayer);
                that.views.push(view);
            }

            // initialize attributes
            this.defaultViewIndex = 0;  	// if not specified, this is the default view of a tile
            this.tileViewMap = {};      	// maps a tile key to its view index
            this.views = [];				// array of all views
			this.mouseState = mouseState; 	// global mouse state to be shared by all views

            // attach map
            attachMap(spec.map);
            // add views
            for (i = 0; i < spec.views.length; i++) {
                addView(spec.views[i]);
            }

            // trigger callback to draw first frame
            this.onMapUpdate();
        },


        /**
         * Returns the view index for the specified tile key
         * @param tilekey 		tile identification key of the form: "level,x,y"
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
                oldTracker = this.views[oldViewIndex].tileTracker,
                newTracker = this.views[newViewIndex].tileTracker;

            // map tile to new view
            this.tileViewMap[tilekey] = newViewIndex;

            // un-select elements from this view
            if (tilekey === this.mouseState.clickState.tilekey) {
				this.mouseState.clearClickState();
            }

            // swap tile data, this function prevents data de-allocation if they share same data source
            oldTracker.swapTileWith(newTracker, tilekey, $.proxy(this.updateAndRedrawViews, this));

            // remove old renderer id from the data
            this.removeRendererIdFromData(oldViewIndex, oldTracker, tilekey);

            // always redraw immediately in case tile is already in memory (to draw new tile), or if
            // it isn't in memory (draw empty tile)
            this.updateAndRedrawViews();
        },


        /**
         * Map update callback, this function is called when the map view state is updating. Requests
         * and draws any new tiles
         */
        onMapUpdate: function() {

            var i,
                tiles,
                viewIndex,
                tilesByView = [];

            for (i=0; i<this.views.length; ++i) {
                tilesByView[i] = [];
            }

            // determine all tiles in view
            tiles = this.map.getTilesInView();

            // group tiles by view index
            for (i=0; i<tiles.length; ++i) {
                viewIndex = this.getTileViewIndex(tiles[i].level+','+
                                                  tiles[i].xIndex+','+
                                                  tiles[i].yIndex);
                tilesByView[viewIndex].push(tiles[i]);
            }

            for (i=0; i<this.views.length; ++i) {
                // find which tiles we need for each view from respective
                this.views[i].tileTracker.filterAndRequestTiles(tilesByView[i], {}, $.proxy(this.updateAndRedrawViews, this));
            }

            // always redraw immediately in case tiles are already in memory, or need to be drawn
            // as empty
            this.updateAndRedrawViews();
        },


        /** add views renderer id to node data
         *
         * @param viewIndex index of the view
         * @param data      data to add renderer id to
         */
        addRendererIdToData: function(viewIndex, data) {
            var i;
            for (i=0; i<data.length; i++ ) {
                if (data[i].renderer === undefined) {
                    data[i].renderer = {};
                }
                data[i].renderer[this.views[viewIndex].id] = true; // stamp tile data with renderer id
            }
            return data;
        },


        /** removes views renderer id from node data
         *
         * @param viewIndex     index of the view
         * @param oldTracker    previous tile tracker
         * @param tilekey       tile key of tile to remove renderer id from
         */
        removeRendererIdFromData: function(viewIndex, oldTracker, tilekey) {

            var i,
                tiles = oldTracker.getDataArray( tilekey );

            for (i=0; i<tiles.length; i++) {
                delete tiles[i].renderer[ this.views[ viewIndex ].id ];
            }

        },

        /**
         * Called upon receiving a tile. Updates the nodeLayer for each view and redraws
         * the layers
         */
        updateAndRedrawViews: function() {
            var i,
                data = [];

            for (i=0; i< this.views.length; i++ ) {
                $.merge(data, this.addRendererIdToData( i, this.views[i].tileTracker.getDataArray() ) );
            }

            /////////////////////////////////////////////
            // temporary aperture.js bug workaround //
            if (permData.length > 40) {
                // clear array in case it gets too big, null and redraw node data
                permData = [];
                this.mapNodeLayer.all([]).redraw();
            }
            for (i=0; i<data.length; i++) {
                if(permData.indexOf(data[i]) === -1) {
                    permData.push(data[i]);
                }
            }
            /////////////////////////////////////////////
			
            this.mapNodeLayer.all(permData).redraw();
        }


     });

    return ViewController;
});
