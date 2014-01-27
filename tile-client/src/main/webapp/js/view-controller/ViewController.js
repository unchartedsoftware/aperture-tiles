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
        TileDataTracker = require('../client-rendering/TileDataTracker'),
        ViewController;



    ViewController = Class.extend({
        /**
         * Construct a carousel
         * @param spec Carousel specification object:
         */
        init: function () {

            this.map = null; // TODO: MAKE PRIVATE!
            this.views = [];
            this.defaultViewIndex = 0;  // if not specified, this is the default view of a tile
            this.tileViewMap = {}; // maps a tile key to its view index
        },


        addView: function( id, layerSpec, clientRenderer ) {

            var view = {
                id: id,
                layerSpec: layerSpec,
                renderer: clientRenderer
            };

            this.createViewNodeLayer(view);

            this.views.push( view );
        },

        attachToMap: function( map ) {

            var i;
            if (this.map) {
                return; // can only attach a single map to view controller
            }

            // set map
            this.map = map;

            // add map listeners
            this.map.on('zoom',   $.proxy(this.onMapUpdate, this));
            this.map.on('panend', $.proxy(this.onMapUpdate, this));

            for (i = 0; i < this.views.length; i++) {
                this.createViewNodeLayer(this.views[i]);
            }
        },


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


        createViewNodeLayer: function(view) {

            if (this.map) {
                // add the data tracker
                view.dataTracker = new TileDataTracker( view.layerSpec, $.proxy(this.onMapUpdate, this) );
                // create node layer off of map
                view.nodeLayer = this.map.addLayer(aperture.geo.MapNodeLayer);
                view.nodeLayer.map('longitude').from('longitude');
                view.nodeLayer.map('latitude').from('latitude');
                //this.nodeLayer.map('visible').asValue('visible');
                // Necessary so that aperture won't place labels and texts willy-nilly
                view.nodeLayer.map('width').asValue(1);
                view.nodeLayer.map('height').asValue(1);
                // create the renderer layer off of the shared view layer
                view.renderer.createLayer(view.nodeLayer);
            }
        },

        onTileViewChange: function(tilekey, newViewIndex) {

            var oldViewIndex = this.getTileViewIndex(tilekey),  // old view index for tile
                oldTracker = this.views[oldViewIndex].dataTracker,
                newTracker = this.views[newViewIndex].dataTracker,
                parsedValues = tilekey.split(','),
                level =  parseInt(parsedValues[0], 10),
                xIndex = parseInt(parsedValues[1], 10),
                yIndex = parseInt(parsedValues[2], 10);

            // remove tile from old tracker
            oldTracker.untrackTile(tilekey);

            this.tileViewMap[tilekey] = newViewIndex;

            // add tile to new tracker
            newTracker.trackTile(tilekey);

            // send request
            aperture.io.rest(
                (newTracker.layerInfo.apertureservice+'1.0.0/'+
                 newTracker.layerInfo.layer+'/'+
                 level+'/'+          // level
                 xIndex+'/'+         // xIndex
                 yIndex+'.json'),    // yIndex
                 'GET',
                 // Update will go to our tile tracker.
                 $.proxy(this.onReceiveTile, this)
            );
        },


        onMapUpdate: function() {

            var i, j,
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
                viewIndex = this.getTileViewIndex(tiles[i].level+','
                                                 +tiles[i].xIndex+','
                                                 +tiles[i].yIndex);
                tilesByView[viewIndex].push(tiles[i]);
            }


            for (i=0; i<this.views.length; ++i) {

                // find which tiles we need for each view from respective
                tilesByView[i] = this.views[i].dataTracker.filterTiles(tilesByView[i]);

                // send request to respective coordinator
                for (j=0; j<tilesByView[i].length; ++j) {
                    // Data unknown; request it.
                    aperture.io.rest(
                        (this.views[i].dataTracker.layerInfo.apertureservice+'1.0.0/'+
                         this.views[i].dataTracker.layerInfo.layer+'/'+
                         tilesByView[i][j].level+'/'+
                         tilesByView[i][j].xIndex+'/'+
                         tilesByView[i][j].yIndex+'.json'),
                        'GET',
                        // Update will go to our tile tracker
                        $.proxy(this.onReceiveTile, this)
                    );
                }
            }


        },

        onReceiveTile: function(tileData) {

            var i,
                viewIndex = this.getTileViewIndex(tileData.index.level + ','
                                                + tileData.index.xIndex + ','
                                                + tileData.index.yIndex);

            // get updated data
            this.views[viewIndex].dataTracker.addTileData(tileData);

            for (i=0; i< this.views.length; i++ ) {
                this.views[i].nodeLayer.join(this.views[i].dataTracker.getNodeData(), "binkey");
                this.views[i].nodeLayer.all().redraw();
                console.log("redraw");
            }
        }


     });

    return ViewController;
});
