/**
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
 * and their respective data sources. Each view is composed of a renderer and DataTracker. Each view has
 * a TileTracker which delegates tile requests from the view and its DataTracker.
 */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        AoIPyramid = require('../client-rendering/AoITilePyramid'),
        TileIterator = require('../client-rendering/TileIterator'),
        TileTracker = require('../client-rendering/TileTracker'),
		MouseState = require('./MouseState'),
        permData = [],  // TEMPORARY BAND AID FIX /
        ViewController;



    ViewController = Class.extend({

        /**
         * Construct a ViewController
         */
        init: function (spec) {

            var that = this,
                i;

            function attachMap(map) {

                // add map and zoom/pan event handlers
                that.map = map;

                // mouse event handlers
                that.map.olMap_.events.register('click', that.map.olMap_, function() {
					that.mouseState.clearClickState();
					that.mapNodeLayer.all().redraw();
				});

                that.map.olMap_.events.register('zoomend', that.map.olMap_, function() {
                    that.mouseState.clearClickState();
                    permData = [];  // reset temporary fix on zoom
                    that.onMapUpdate();
                });

                that.map.on('panend', function() {
                    that.onMapUpdate();
                });

                that.mapNodeLayer = that.map.addLayer(aperture.geo.MapNodeLayer);
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
                    tileTracker: new TileTracker(viewspec.dataTracker, viewspec.renderer.id),
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
            this.defaultViewIndex = 0;  // if not specified, this is the default view of a tile
            this.tileViewMap = {};      // maps a tile key to its view index
            this.views = [];
			this.mouseState = new MouseState();

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
         * Returns the view index for specified tile key
         * @param tilekey tile identification key
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
         * @param tilekey tile identification key
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
                level = this.map.getZoom(),
                bounds = this.map.olMap_.getExtent(),
                mapExtents = this.map.olMap_.getMaxExtent(),
                mapPyramid = new AoIPyramid(mapExtents.left, mapExtents.bottom,
                                            mapExtents.right, mapExtents.top),
                viewIndex,
                tilesByView = [];

            for (i=0; i<this.views.length; ++i) {
                tilesByView[i] = [];
            }

            // determine all tiles in view
            tiles = new TileIterator(mapPyramid, level,
                                     bounds.left, bounds.bottom,
                                     bounds.right, bounds.top).getRest();

            // group tiles by view index
            for (i=0; i<tiles.length; ++i) {
                viewIndex = this.getTileViewIndex(tiles[i].level+','+
                                                  tiles[i].xIndex+','+
                                                  tiles[i].yIndex);
                tilesByView[viewIndex].push(tiles[i]);
            }

            for (i=0; i<this.views.length; ++i) {
                // find which tiles we need for each view from respective
                this.views[i].tileTracker.filterAndRequestTiles(tilesByView[i], $.proxy(this.updateAndRedrawViews, this));
            }

            // always redraw immediately in case tiles are already in memory, or need to be drawn
            // as empty
            this.updateAndRedrawViews();
        },


        /**
         * Called upon receiving a tile. Updates the nodeLayer for each view and redraws
         * the layers
         */
        updateAndRedrawViews: function() {
            var i,
                data = [];

            for (i=0; i< this.views.length; i++ ) {
                $.merge(data, this.views[i].tileTracker.getNodeData());
            }

            /////////////////////////////////////////////
            // TEMPORARY BAND AID FIX //
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
            //////////////////////////////////////////////
            this.mapNodeLayer.all(permData).redraw();
        }


     });

    return ViewController;
});
