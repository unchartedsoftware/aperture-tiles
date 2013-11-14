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
 * This module handles listening to a (single) map, requesting needed tiles 
 * from the server, and handling the responses.
 */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        DataLayer = require('../datalayer'),
        TileIterator = require('./TileIterator'),
        AoIPyramid = require('./AoITilePyramid'),

        MapServerCoordinator;



    MapServerCoordinator = Class.extend({
        ClassName: "MapServerCoordinator",
        init: function (tracker, layerSpec) {
            var layerInfoListener, onLayerInfoRetrieved;

            this.map = null;
            this.tracker = tracker;
            this.layerSpec = layerSpec;
            this.layerInfo = null;
            this.mapPyramid = new AoIPyramid(-20037500, -20037500,
                                              20037500,  20037500);
            this.mapUpdateFcn = $.proxy(this.onMapUpdate, this);

            // When we get our layer info, store it, and do a map udpate if
            // necessary
            onLayerInfoRetrieved = $.proxy(function (dataListerner, layerInfo) {
                this.layerInfo = layerInfo;
                if (this.map) {
                    this.onMapUpdate();
                }
            }, this);

            // Fetch our layer info
            layerInfoListener = new DataLayer([layerSpec]);
            // Only one layer, so we don't have to worry about waiting until 
            // all requests are fulfilled
            layerInfoListener.setRetrievedCallback(onLayerInfoRetrieved);
            layerInfoListener.retrieveLayerInfo();
        },

        setMap: function (map) {
            if (this.map === map) {
                return;
            }

            if (this.map) {
                this.map.map.off('zoom',   this.mapUpdateFcn);
                this.map.map.off('panend', this.mapUpdateFcn);
            }

            this.map = map;

            if (this.map) {
                this.map.map.on('zoom',   this.mapUpdateFcn);
                this.map.map.on('panend', this.mapUpdateFcn);
            }
        },

        onMapUpdate: function () {
            if (!this.layerInfo || !this.map) {
                return;
            }

            var i, tiles,
                level = this.map.map.getZoom(),
                bounds = this.map.map.olMap_.getExtent();

            // Figure out what tiles we need
            tiles = new TileIterator(this.mapPyramid, level,
                                     bounds.left, bounds.bottom,
                                     bounds.right, bounds.top).getRest();

            tiles = this.tracker.filterTiles(tiles);

            for (i=0; i<tiles.length; ++i) {
                // Data unknown; request it.
                aperture.io.rest(
                    (this.layerInfo.apertureservice+'1.0.0/'+
                     this.layerInfo.layer+'/'+
                     tiles[i].level+'/'+
                     tiles[i].xIndex+'/'+
                     tiles[i].yIndex+'.json'),
                    'GET',
                    // Update will go to our tile tracker.
                    $.proxy(this.tracker.addTileData, this.tracker)
                );
            }
        }
    });

    return MapServerCoordinator;
});
