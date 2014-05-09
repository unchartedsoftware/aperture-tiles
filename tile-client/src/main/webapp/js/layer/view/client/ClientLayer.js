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
		mapNodeLayer = {},
		clientState = new ClientState(),	// global mouse state
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
        init: function ( id, map) {

            var that = this;

            function attachMap(map) {

                // add map and zoom/pan event handlers
                that.map = map;

                // mouse event handlers
                that.map.on('click', function() {
					// if click event has not been swallowed yet, clear mouse state and redraw
					that.clientState.clearClickState();
					that.mapNodeLayer.all().redraw();
				});

                that.map.on('zoomend', function() {
					// clear click mouse state on zoom and call map update function
                    that.clientState.clearClickState();
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

            // initialize attributes
            this.id = id;
            this.defaultViewIndex = 0;  	// if not specified, this is the default view of a tile
            this.tileViewMap = {};      	// maps a tile key to its view index
            this.views = [];				// array of all views
			this.clientState = clientState; 	// global mouse state to be shared by all views
            this.opacity = 1.0;
            this.isVisible = true;

            // attach map
            attachMap(map);
        },

        setViews: function( viewSpecs ) {

            var that =  this,
                i;

            // add views
            for (i = 0; i < viewSpecs.length; i++) {
                viewSpecs[i].clientState = that.clientState;
                viewSpecs[i].mapNodeLayer = that.mapNodeLayer;
                that.views.push( new View( viewSpecs[i] ) );
            }

            // trigger callback to draw first frame
            this.onMapUpdate();
        },

        setOpacity: function( opacity ) {

            this.clientState.setSharedState('opacity', opacity);
            this.mapNodeLayer.all().redraw();
        },


        setVisibility: function( visible ) {
            this.clientState.setSharedState('isVisible', visible);
            this.mapNodeLayer.all().redraw();
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
                oldView = this.views[oldViewIndex],
                newView = this.views[newViewIndex];

            // map tile to new view
            this.tileViewMap[tilekey] = newViewIndex;

            // un-select elements from this view
            if (tilekey === this.clientState.clickState.tilekey) {
				this.clientState.clearClickState();
            }

            if (oldView.getLayerId() === newView.getLayerId()) {
                // if both views share the same type of data source, swap tile data
                oldView.swapTileWith(newView, tilekey);
                this.updateAndRedrawViews();
                this.mapNodeLayer.all().where( newView.getTileData(tilekey) ).redraw();
            } else {
                // otherwise, release and request
                oldView.releaseTile( tilekey );
                newView.requestTile( tilekey, $.proxy(this.updateAndRedrawViews, this));
            }
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

            if (this.views === undefined || this.views.length === 0) {
                return;
            }

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
                this.views[i].filterAndRequestTiles(tilesByView[i],
                                                                this.map.getTileSetBoundsInView(),
                                                                $.proxy(this.updateAndRedrawViews, this));
            }
        },


        /**
         * Called upon receiving a tile. Updates the nodeLayer for each view and redraws
         * the layers
         */
        updateAndRedrawViews: function( tile ) {
            var i,
                that = this,
                data = [];

            for (i=0; i< this.views.length || i< 1; i++ ) {
                $.merge(data, this.views[i].getDataArray() );
            }

            this.mapNodeLayer.all([]).redraw(); // temporary aperture.js bug workaround
            this.mapNodeLayer.all(data).redraw();
            // bring selected node to the front, this allows any renderings to stay above other nodes
            this.mapNodeLayer.all().where( function() {
                return this.tilekey === that.clientState.clickState.tilekey;
            } ).toFront().redraw();
        }


     });

    return ClientLayer;
});
