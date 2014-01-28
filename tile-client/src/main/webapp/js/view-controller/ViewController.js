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

/*global OpenLayers*/

define(function (require) {
    "use strict";



    var Class = require('../class'),
        AoIPyramid = require('../client-rendering/AoITilePyramid'),
        TileIterator = require('../client-rendering/TileIterator'),
        TileTracker = require('../client-rendering/TileTracker'),
        ViewController;


    ViewController = Class.extend({

        /**
         * Constructor
         */
        init: function () {

            this.map = null;
            this.views = [];
            this.defaultViewIndex = 0;  // if not specified, this is the default view of a tile
            this.tileViewMap = {}; // maps a tile key to its view index
        },

        /**
         * Attach view controller to a map
         */
        attachToMap: function(map) {

            var i;

            if (this.map) {
                return; // can only attach a single map to view controller
            }

            // set map
            this.map = map;

            this.map.olMap_.events.register('zoomend', this.map.olMap_, $.proxy(this.onMapUpdate, this) );
            this.map.on('panend', $.proxy(this.onMapUpdate, this));

            for (i = 0; i < this.views.length; i++) {
                this.createViewNodeLayer(this.views[i]);
            }

            this.onMapUpdate();
        },

        /**
         * Add view to view controller
         */
        addView: function(id, dataSet, clientRenderer) {

            var view = {
                id: id,
                tileTracker: new TileTracker(dataSet),
                renderer: clientRenderer
            };

            this.createViewNodeLayer(view);

            this.views.push(view);
        },

        /**
         * If attached to a map, create node layer for view rendering
         */
        createViewNodeLayer: function(view) {

            if (this.map) {
                // create node layer off of map
                view.nodeLayer = this.map.addLayer(aperture.geo.MapNodeLayer);
                view.nodeLayer.map('longitude').from('longitude');
                view.nodeLayer.map('latitude').from('latitude');
                // Necessary so that aperture won't place labels and texts willy-nilly
                view.nodeLayer.map('width').asValue(1);
                view.nodeLayer.map('height').asValue(1);
                // create the renderer layer off of the shared view layer
                view.renderer.createLayer(view.nodeLayer);
            }
        },

        /**
         * Returns view index for specified tile key
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
         */
        onTileViewChange: function(tilekey, newViewIndex) {

            var that = this,
                oldViewIndex = this.getTileViewIndex(tilekey),  // old view index for tile
                oldTracker = this.views[oldViewIndex].tileTracker,
                newTracker = this.views[newViewIndex].tileTracker;

            this.tileViewMap[tilekey] = newViewIndex;

            oldTracker.swapTileWith(newTracker, tilekey, function() {
                that.views[oldViewIndex].nodeLayer.all(that.views[oldViewIndex].tileTracker.getNodeData());
                that.views[newViewIndex].nodeLayer.all(that.views[newViewIndex].tileTracker.getNodeData());
                that.map.all().redraw();
            });
        },

        /**
         * Map update callback
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
                this.views[i].tileTracker.filterAndRequestTiles(tilesByView[i], $.proxy(this.onReceiveTile, this));
            }
        },

        onReceiveTile: function() {

            var that = this,
                i;

            for (i=0; i< that.views.length; i++ ) {
                that.views[i].nodeLayer.all(that.views[i].tileTracker.getNodeData());
            }
            that.map.all().redraw();
        }

     });

    return ViewController;
});
