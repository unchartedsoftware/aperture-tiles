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
 * This modules defines a basic layer class that can be added to maps.
 */
define(['class', 'datalayer', 'maplayer', 'aoitilepyramid', 'tileiterator'],
       function(Class, DataLayer, MapLayer, Pyramid, TileIterator) {
    "use strict";


    var ClientRenderedMapLayer, createTileKey;

    createTileKey = function (tileIndex) {
        return tileIndex.level + "-" + tileIndex.x + "-" + tileIndex.y;
    };

    ClientRenderedMapLayer = MapLayer.extend({
        init: function (id, layerSpec) {
            this._super(id);
            this.layerSpec = layerSpec;
            this.layerInfo = null;
            this.data = {};
            // Used to note if the map has ever received an update, so that 
            // when we get our layer information, if we have been updated,
            // we can display it.
            this.updated = false;

            this.dataListener = new DataLayer([layerSpec]);
            // Only one layer, so we don't have to worry about waiting until 
            // all requests are fulfilled
            this.dataListener.setRetrievedCallback($.proxy(this.useLayerInfo,
                                                           this));

            this.dataListener.retrieveLayerInfo();
        },

        /*
         * Called when data the basic information about the layer is recieved 
         * from the server.
         */
        useLayerInfo: function (dataListener, layerInfo) {
            this.layerInfo = layerInfo;
            if (this.updated) {
                this.updated = false;
                this.updateMap();
            }
        },

        createLayer: function (nodeLayer) {
            this.setUpdateFcn($.proxy(this.updateMap, this));
        },

        updateData: function (tile) {
            var tileKey = createTileKey(tile.index);
            this.data[tileKey] = tile.tile;
            // Data is:
            // .<tileid>.xIndex
            // .<tileid>.yIndex
            // .<tileid>.level
            // .<tileId>.values.<n>.value.<m>.key
            // .<tileId>.values.<n>.value.<m>.value
        },

        updateMap: function (event) {
            if (this.layerInfo) {
                var
                bounds = this.map.map.olMap_.getExtent(),
                level = this.map.map.getZoom(),
                // The bounds of the full OpenLayers map
                pyramid = new Pyramid(-20037500, -20037500,20037500, 20037500),
                tiles,
                i,
                defunctKeys,
                tileKey;

                // Copy out all keys from the current data.  As we go through
                // making new requests, we won't bother with tiles we've 
                // already received, but we'll remove them from the defunct 
                // list.  When we're done requesting data, we'll then know 
                // which tiles are defunct, and can be removed from the data 
                // set.
                defunctKeys = {};
                for (tileKey in this.data) {
                    if (this.data.hasOwnProperty(tileKey)) {
                        defunctKeys[tileKey] = true;
                    }
                }

                // Figure out what tiles we need
                tiles = new TileIterator(pyramid, level,
                                         bounds.left, bounds.bottom,
                                         bounds.right, bounds.top).getRest();

                // Request needed tiles
                for (i = 0; i < tiles.length; ++i) {
                    tileKey = createTileKey(tiles[i]);
                    if (defunctKeys[tileKey]) {
                        // Data exists already; remove it from the list of old 
                        // data to be removed
                        delete defunctKeys[tileKey];
                    } else {
                        // Data unknown; request it.
                        aperture.io.rest(
                            (this.layerInfo.apertureservice+'1.0.0/'+
                             this.layerInfo.layer+'/'+
                             tiles[i].level+'/'+tiles[i].x+'/'+tiles[i].y+'.json'),
                            'GET',
                            $.proxy(this.updateData, this)
                        );
                    }
                }

                // Remove old ata we no longer need
                for (tileKey in defunctKeys) {
                    if (defunctKeys.hasOwnProperty(tileKey)) {
                        delete this.data[tileKey];
                    }
                }
            } else {
                this.updated = true;
            }
        }
    });

    return ClientRenderedMapLayer;
});
